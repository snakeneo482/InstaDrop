package com.instadrop.app.domain

import com.instadrop.app.domain.model.SourcePlatform

/**
 * Generic link handling for any supported site (not just Instagram).
 *
 * Pulls the first http(s) URL out of arbitrary shared text — Android share
 * sheets from YouTube / TikTok / X / etc. all send the link as `text/plain`,
 * sometimes wrapped in extra words — and classifies it by host.
 */
object Urls {

    private val URL_REGEX = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    /** First URL in [text], trimmed of trailing punctuation. Null if none. */
    fun extract(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val raw = URL_REGEX.find(text)?.value ?: return null
        return raw.trimEnd('.', ',', ')', ']', '"', '\'', '>', '\\')
    }

    fun isSupportedLink(text: String?): Boolean = extract(text) != null

    /** Best-effort platform detection from the URL host. */
    fun platformOf(url: String): SourcePlatform {
        val host = host(url)
        return when {
            host.contains("instagram.") || host.contains("instagr.am") || host.contains("cdninstagram") -> SourcePlatform.INSTAGRAM
            host.contains("youtube.") || host == "youtu.be" || host.endsWith(".youtu.be") -> SourcePlatform.YOUTUBE
            host.contains("tiktok.") -> SourcePlatform.TIKTOK
            host.contains("twitter.") || host == "x.com" || host.endsWith(".x.com") || host.contains("t.co") -> SourcePlatform.TWITTER
            host.contains("facebook.") || host.contains("fb.watch") || host.contains("fb.com") -> SourcePlatform.FACEBOOK
            host.contains("reddit.") || host.contains("redd.it") -> SourcePlatform.REDDIT
            host.contains("twitch.") -> SourcePlatform.TWITCH
            host.contains("vimeo.") -> SourcePlatform.VIMEO
            host.contains("pinterest.") || host.contains("pin.it") -> SourcePlatform.PINTEREST
            host.contains("soundcloud.") -> SourcePlatform.SOUNDCLOUD
            host.contains("tumblr.") -> SourcePlatform.TUMBLR
            host.contains("snapchat.") -> SourcePlatform.SNAPCHAT
            host.contains("bsky.") -> SourcePlatform.BLUESKY
            host.contains("loom.com") -> SourcePlatform.LOOM
            else -> SourcePlatform.OTHER
        }
    }

    private fun host(url: String): String =
        url.substringAfter("://", url)
            .substringBefore('/')
            .substringBefore('?')
            .substringBefore('#')
            .lowercase()
}
