package com.instadrop.app.data.resolver

import com.instadrop.app.data.Http
import com.instadrop.app.domain.InstagramUrl
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

/**
 * Resolves a **public** Instagram post by reading the Open Graph (`og:`) meta
 * tags Instagram embeds in the page for link unfurling — the very same metadata
 * Messenger, WhatsApp and Slack use to render a preview card.
 *
 * This only ever reads publicly available preview metadata; it does not log in,
 * use private APIs, or bypass any access control. Private posts, or posts behind
 * Instagram's login wall, simply yield no `og:video`/`og:image` and we throw —
 * the [ResolverChain] then falls back to the safe stub so the app never dead-ends.
 *
 * > Only download content you have the rights to, and respect Instagram's Terms.
 */
class OpenGraphResolver(
    private val client: OkHttpClient = Http.client,
) : MediaResolver {

    override suspend fun resolve(url: String): InstaMedia = withContext(Dispatchers.IO) {
        val clean = InstagramUrl.extract(url)
            ?: throw ResolveException("That doesn't look like an Instagram link.")

        val html = fetch(clean)

        val videoUrl = meta(html, "og:video:secure_url") ?: meta(html, "og:video")
        val imageUrl = meta(html, "og:image")
        val title = meta(html, "og:title")
        val description = meta(html, "og:description")

        val item = when {
            videoUrl != null -> MediaItem(
                type = MediaType.VIDEO,
                downloadUrl = videoUrl,
                thumbnailUrl = imageUrl,
                isHd = true,
            )
            imageUrl != null -> MediaItem(
                type = MediaType.IMAGE,
                downloadUrl = imageUrl,
                thumbnailUrl = imageUrl,
                isHd = true,
            )
            else -> throw ResolveException(
                "This post isn't publicly accessible (it may be private or login-only).",
            )
        }

        InstaMedia(
            sourceUrl = clean,
            kind = InstagramUrl.classify(clean),
            author = author(title),
            caption = caption(description, title),
            items = listOf(item),
            platform = com.instadrop.app.domain.model.SourcePlatform.INSTAGRAM,
        )
    }

    private fun fetch(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", Http.USER_AGENT)
            .header("Accept-Language", "en-US,en;q=0.9")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw ResolveException("Instagram returned HTTP ${response.code}.")
            }
            return response.body?.string() ?: throw IOException("Empty page body")
        }
    }

    private companion object {
        /** Pulls the `content` of a `<meta property="og:..." content="...">` tag, order-agnostic. */
        fun meta(html: String, property: String): String? {
            val p = Regex.escape(property)
            val patterns = listOf(
                Regex("""<meta[^>]+property=["']$p["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
                Regex("""<meta[^>]+content=["']([^"']+)["'][^>]+property=["']$p["']""", RegexOption.IGNORE_CASE),
            )
            val raw = patterns.firstNotNullOfOrNull { it.find(html)?.groupValues?.get(1) } ?: return null
            return decodeEntities(raw).takeIf { it.isNotBlank() }
        }

        fun author(title: String?): String? {
            title ?: return null
            // og:title looks like:  "Jane Doe (@jane) • Instagram photos and videos"
            Regex("""\(@([A-Za-z0-9._]+)\)""").find(title)?.let { return it.groupValues[1] }
            // …or:  "jane on Instagram: \"caption\""
            Regex("""^(.*?)\s+on Instagram""", RegexOption.IGNORE_CASE).find(title)?.let {
                return it.groupValues[1].trim().removePrefix("@").ifBlank { null }
            }
            return null
        }

        // Matches the first "quoted" segment, e.g.  username on Instagram: "the caption"
        private val QUOTED = Regex("\"([^\"]{2,})\"", RegexOption.DOT_MATCHES_ALL)

        fun caption(description: String?, title: String?): String? {
            // Prefer the quoted caption embedded in og:title or og:description.
            for (source in listOf(title, description)) {
                source ?: continue
                QUOTED.find(source)?.groupValues?.get(1)?.trim()
                    ?.takeIf { it.isNotBlank() }?.let { return it }
            }
            return description?.trim()?.takeIf { it.isNotBlank() }
        }

        fun decodeEntities(s: String): String = s
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&#x27;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
