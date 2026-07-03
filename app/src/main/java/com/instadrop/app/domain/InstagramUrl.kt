package com.instadrop.app.domain

import com.instadrop.app.domain.model.PostKind

/**
 * Pulls a clean Instagram URL out of arbitrary shared text and classifies it.
 *
 * Instagram's share sheet sends plain text that is usually just the URL, but
 * can include surrounding text ("Check this out: https://..."). We extract the
 * first instagram.com link and strip tracking query params.
 */
object InstagramUrl {

    private val URL_REGEX =
        Regex("""https?://(?:www\.)?(?:instagram\.com|instagr\.am)/\S+""", RegexOption.IGNORE_CASE)

    /** Returns the normalized Instagram URL found in [text], or null. */
    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val match = URL_REGEX.find(text)?.value ?: return null
        return normalize(match)
    }

    fun isInstagramUrl(text: String?): Boolean = extract(text) != null

    /** Best-effort classification from the URL path. */
    fun classify(url: String): PostKind {
        // Everything after the host, without a leading slash, e.g. "reel/Cabc".
        val path = url.substringAfter("://", url)
            .substringAfter('/', "")
            .lowercase()
        return when {
            path.startsWith("reel/") || path.startsWith("reels/") -> PostKind.REEL
            path.startsWith("stories/") -> PostKind.STORY
            path.startsWith("p/") || path.startsWith("tv/") -> PostKind.POST
            // "username" with nothing after it is most likely a profile.
            path.isNotEmpty() && !path.trimEnd('/').contains('/') -> PostKind.PROFILE_PIC
            else -> PostKind.UNKNOWN
        }
    }

    private fun normalize(raw: String): String {
        // Drop trailing punctuation the regex may have greedily captured, and
        // strip query string / fragment (tracking params like ?igsh=...).
        val trimmed = raw.trimEnd('.', ',', ')', '"', '\'', '>')
        return trimmed.substringBefore('?').substringBefore('#')
    }
}
