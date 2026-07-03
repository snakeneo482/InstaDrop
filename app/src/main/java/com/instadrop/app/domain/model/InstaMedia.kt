package com.instadrop.app.domain.model

/** Which site a shared link points at. Drives labels and routing. */
enum class SourcePlatform(val label: String) {
    INSTAGRAM("Instagram"),
    YOUTUBE("YouTube"),
    TIKTOK("TikTok"),
    TWITTER("X"),
    FACEBOOK("Facebook"),
    REDDIT("Reddit"),
    TWITCH("Twitch"),
    VIMEO("Vimeo"),
    PINTEREST("Pinterest"),
    SOUNDCLOUD("SoundCloud"),
    TUMBLR("Tumblr"),
    SNAPCHAT("Snapchat"),
    BLUESKY("Bluesky"),
    LOOM("Loom"),
    OTHER("Media"),
}

/** What kind of content the shared URL points at (Instagram-specific detail). */
enum class PostKind(val label: String) {
    REEL("Reel"),
    POST("Post"),
    STORY("Story"),
    CAROUSEL("Carousel"),
    PROFILE_PIC("Profile photo"),
    VIDEO("Video"),
    UNKNOWN("Media"),
}

enum class MediaType { VIDEO, IMAGE }

/** A single downloadable asset within a post (a carousel / picker has several). */
data class MediaItem(
    val type: MediaType,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int? = null,
    val isHd: Boolean = false,
)

/** The fully-resolved result for one shared link. */
data class InstaMedia(
    val sourceUrl: String,
    val kind: PostKind,
    val items: List<MediaItem>,
    val author: String? = null,
    val caption: String? = null,
    val platform: SourcePlatform = SourcePlatform.OTHER,
) {
    val primary: MediaItem get() = items.first()
    val isCarousel: Boolean get() = items.size > 1

    /** Human title for the preview header, e.g. "YouTube video" / "Instagram carousel". */
    val headline: String
        get() {
            val what = when {
                isCarousel -> "carousel"
                primary.type == MediaType.IMAGE -> "photo"
                else -> "video"
            }
            return "${platform.label} $what"
        }
}
