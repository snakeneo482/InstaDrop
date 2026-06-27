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
        val path = url.substringAfter("instagram.com", "")
            .substringAfter("instagr.am", "")
            .lowercase()
        return when {
            path.contains("/reel/") || path.contains("/reels/") -> PostKind.REEL
            path.contains("/stories/") -> PostKind.STORY
            path.contains("/p/") -> PostKind.POST
            // /username/ with nothing else is most likely a profile.
            path.trim('/').count { it == '/' } == 0 && path.length > 1 -> PostKind.PROFILE_PIC
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
