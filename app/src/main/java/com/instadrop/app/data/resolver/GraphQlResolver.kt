package com.instadrop.app.data.resolver

import com.instadrop.app.data.Http
import com.instadrop.app.domain.InstagramUrl
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import com.instadrop.app.domain.model.SourcePlatform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder

/**
 * Resolves a **public** Instagram Reel / video / photo / carousel via Instagram's
 * own web GraphQL endpoint — the same request instagram.com makes in a browser
 * to render a post page.
 *
 * Flow:
 *  1. Prime a session against instagram.com to pick up the `csrftoken` cookie.
 *  2. GET `/graphql/query/` with the public web `X-IG-App-ID` and that CSRF
 *     token, asking for the post by shortcode.
 *  3. Read the real CDN media URLs (`video_url` / `display_url`) out of the JSON.
 *
 * No login and no private credentials are used — only public content resolves;
 * private posts / Stories return nothing and we throw, so the [ResolverChain]
 * can surface a clear error.
 *
 * > Instagram has no official download API and changes this endpoint without
 * > notice, so treat extraction as best-effort. Only download content you have
 * > the rights to, and respect Instagram's Terms of Service.
 */
class GraphQlResolver(
    private val client: OkHttpClient = Http.client,
) : MediaResolver {

    override suspend fun resolve(url: String): InstaMedia = withContext(Dispatchers.IO) {
        val clean = InstagramUrl.extract(url)
            ?: throw ResolveException("That doesn't look like an Instagram link.")
        val shortcode = SHORTCODE.find(clean)?.groupValues?.get(1)
            ?: throw ResolveException("Couldn't read the post id from that link.")

        val (cookie, csrf) = primeSession()
        val json = queryGraphQl(shortcode, cookie, csrf)

        val media = json.optJSONObject("data")?.optJSONObject("xdt_shortcode_media")
            ?: throw ResolveException("This post isn't publicly available (private, a Story, or removed).")

        val items = buildList {
            val children = media.optJSONObject("edge_sidecar_to_children")?.optJSONArray("edges")
            if (children != null && children.length() > 0) {
                for (i in 0 until children.length()) {
                    children.getJSONObject(i).optJSONObject("node")?.let { node -> toItem(node)?.let(::add) }
                }
            } else {
                toItem(media)?.let(::add)
            }
        }
        if (items.isEmpty()) throw ResolveException("Couldn't find any downloadable media in that post.")

        InstaMedia(
            sourceUrl = clean,
            kind = InstagramUrl.classify(clean),
            author = media.optJSONObject("owner")?.optString("username")?.takeIf { it.isNotBlank() },
            caption = caption(media),
            items = items,
            platform = SourcePlatform.INSTAGRAM,
        )
    }

    /** GET instagram.com once to collect the cookies (incl. csrftoken) the API expects. */
    private fun primeSession(): Pair<String, String?> {
        val request = Request.Builder()
            .url("https://www.instagram.com/")
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept", "text/html,application/xhtml+xml")
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        return client.newCall(request).execute().use { response ->
            val pairs = response.headers("set-cookie")
                .map { it.substringBefore(';').trim() }
                .filter { it.contains('=') }
            val cookie = pairs.joinToString("; ")
            val csrf = pairs.firstOrNull { it.startsWith("csrftoken=") }?.substringAfter("=")
            cookie to csrf
        }
    }

    private fun queryGraphQl(shortcode: String, cookie: String, csrf: String?): JSONObject {
        val variables = URLEncoder.encode("{\"shortcode\":\"$shortcode\"}", "UTF-8")
        val builder = Request.Builder()
            .url("https://www.instagram.com/graphql/query/?doc_id=$DOC_ID&variables=$variables")
            .header("User-Agent", Http.USER_AGENT)
            .header("X-IG-App-ID", IG_APP_ID)
            .header("Accept", "*/*")
            .header("Referer", "https://www.instagram.com/p/$shortcode/")
        if (cookie.isNotBlank()) builder.header("Cookie", cookie)
        if (csrf != null) builder.header("X-CSRFToken", csrf)

        client.newCall(builder.build()).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw ResolveException("Instagram blocked the request (HTTP ${response.code}). Try again shortly.")
            }
            return try {
                JSONObject(body)
            } catch (e: Exception) {
                throw ResolveException("Couldn't read Instagram's response (the post may be login-only).", e)
            }
        }
    }

    private companion object {
        // Public web client id Instagram's own site sends; required by the API.
        const val IG_APP_ID = "936619743392459"
        // The PolarisPostActionLoadPostQueryQuery doc id. Instagram may rotate this.
        const val DOC_ID = "10015901848480474"

        val SHORTCODE = Regex("""/(?:reel|reels|p|tv)/([A-Za-z0-9_-]+)""")

        fun toItem(node: JSONObject): MediaItem? {
            val isVideo = node.optBoolean("is_video", false)
            val downloadUrl = if (isVideo) node.optString("video_url") else node.optString("display_url")
            if (downloadUrl.isNullOrBlank()) return null
            val duration = node.optDouble("video_duration", 0.0).takeIf { it > 0 }?.toInt()
            return MediaItem(
                type = if (isVideo) MediaType.VIDEO else MediaType.IMAGE,
                downloadUrl = downloadUrl,
                thumbnailUrl = node.optString("display_url").takeIf { it.isNotBlank() },
                durationSeconds = duration,
                isHd = true,
            )
        }

        fun caption(media: JSONObject): String? =
            media.optJSONObject("edge_media_to_caption")
                ?.optJSONArray("edges")
                ?.optJSONObject(0)
                ?.optJSONObject("node")
                ?.optString("text")
                ?.takeIf { it.isNotBlank() }
    }
}
