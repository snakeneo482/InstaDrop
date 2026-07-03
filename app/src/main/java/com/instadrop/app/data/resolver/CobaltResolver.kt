package com.instadrop.app.data.resolver

import com.instadrop.app.data.Http
import com.instadrop.app.data.settings.SettingsRepository
import com.instadrop.app.domain.Urls
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import com.instadrop.app.domain.model.PostKind
import com.instadrop.app.domain.model.SourcePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Universal resolver backed by a self-hosted **Cobalt** instance
 * (https://github.com/imputnet/cobalt) — the mature open-source engine that can
 * extract media from YouTube, Instagram (incl. Stories when the server holds a
 * session), TikTok, X, Facebook, Reddit, Twitch, Vimeo, SoundCloud and ~1000
 * more sites.
 *
 * The app just POSTs the shared URL to the instance you configure in Settings
 * and downloads whatever direct/tunnelled media URL(s) it returns — so all the
 * brittle, per-site extraction logic lives on a server you control and update,
 * not baked into the APK.
 *
 * Supports both the current Cobalt API (v10: `tunnel` / `redirect` / `picker` /
 * `local-processing`) and the older `stream` response shape.
 */
class CobaltResolver(
    private val settings: SettingsRepository,
    private val client: OkHttpClient = Http.client,
) : MediaResolver {

    override suspend fun resolve(url: String): InstaMedia = withContext(Dispatchers.IO) {
        val clean = Urls.extract(url) ?: throw ResolveException("That doesn't look like a valid link.")
        val base = settings.backendUrl.value.trim().trimEnd('/')
        if (base.isBlank()) {
            throw BackendNotConfigured("No downloader server set. Add one in Settings to use this site.")
        }

        val payload = JSONObject()
            .put("url", clean)
            .put("videoQuality", "1080")
            .put("filenameStyle", "basic")
            .put("downloadMode", "auto")
            .toString()

        val builder = Request.Builder()
            .url(base)
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept", "application/json")
            .post(payload.toRequestBody(JSON))
        val key = settings.apiKey.value.trim()
        if (key.isNotBlank()) builder.header("Authorization", "Api-Key $key")

        val json = client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (body.isBlank()) throw ResolveException("Empty response from the downloader server.")
            try {
                JSONObject(body)
            } catch (e: Exception) {
                throw ResolveException("Couldn't read the downloader server response.", e)
            }
        }

        val items = parseItems(json)
        if (items.isEmpty()) throw ResolveException(errorMessage(json))

        InstaMedia(
            sourceUrl = clean,
            kind = if (items.first().type == MediaType.VIDEO) PostKind.VIDEO else PostKind.POST,
            items = items,
            platform = Urls.platformOf(clean),
        )
    }

    private fun parseItems(json: JSONObject): List<MediaItem> {
        return when (json.optString("status")) {
            "tunnel", "redirect", "stream" -> {
                val url = json.optString("url").ifBlank { return emptyList() }
                listOf(item(url, json.optString("filename")))
            }
            "picker" -> pickerItems(json.optJSONArray("picker"))
            "local-processing" -> {
                // v10 client-side muxing: take the first tunnel URL as-is.
                val tunnels = json.optJSONArray("tunnel")
                val url = tunnels?.optString(0).orEmpty().ifBlank { return emptyList() }
                val name = json.optJSONObject("output")?.optString("filename").orEmpty()
                listOf(item(url, name))
            }
            else -> emptyList()
        }
    }

    private fun pickerItems(arr: JSONArray?): List<MediaItem> {
        arr ?: return emptyList()
        val out = mutableListOf<MediaItem>()
        for (i in 0 until arr.length()) {
            val node = arr.optJSONObject(i)
            val url = node?.optString("url").orEmpty()
            if (url.isBlank()) continue
            val type = if (node?.optString("type") == "photo") MediaType.IMAGE else MediaType.VIDEO
            out += MediaItem(
                type = type,
                downloadUrl = url,
                thumbnailUrl = if (type == MediaType.IMAGE) url else null,
                isHd = true,
            )
        }
        return out
    }

    private fun item(url: String, filename: String): MediaItem {
        val type = if (looksLikeImage(filename.ifBlank { url })) MediaType.IMAGE else MediaType.VIDEO
        return MediaItem(
            type = type,
            downloadUrl = url,
            thumbnailUrl = if (type == MediaType.IMAGE) url else null,
            isHd = true,
        )
    }

    private fun errorMessage(json: JSONObject): String {
        val code = json.optJSONObject("error")?.optString("code").orEmpty()
        return when {
            code.contains("auth", ignoreCase = true) -> "The server needs a valid API key (set it in Settings)."
            code.contains("login", ignoreCase = true) || code.contains("private", ignoreCase = true) ->
                "This post needs a logged-in session on your server (e.g. for Stories/private posts)."
            code.isNotBlank() -> "Downloader server error: $code"
            else -> "The server couldn't fetch media from that link."
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
        private val IMAGE_EXT = Regex("""\.(jpe?g|png|webp|gif|heic|bmp)(\?|$)""", RegexOption.IGNORE_CASE)
        fun looksLikeImage(s: String): Boolean = IMAGE_EXT.containsMatchIn(s)
    }
}
