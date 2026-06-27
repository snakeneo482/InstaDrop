package com.instadrop.app.domain.model

/** What kind of Instagram content the shared URL points at. */
enum class PostKind(val label: String) {
    REEL("Reel"),
    POST("Post"),
    STORY("Story"),
    CAROUSEL("Carousel"),
    PROFILE_PIC("Profile photo"),
    UNKNOWN("Media"),
}

enum class MediaType { VIDEO, IMAGE }

/** A single downloadable asset within a post (a carousel has several). */
data class MediaItem(
    val type: MediaType,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int? = null,
    val isHd: Boolean = false,
)

/** The fully-resolved result for one shared Instagram URL. */
data class InstaMedia(
    val sourceUrl: String,
    val kind: PostKind,
    val items: List<MediaItem>,
    val author: String? = null,
    val caption: String? = null,
) {
    val primary: MediaItem get() = items.first()
    val isCarousel: Boolean get() = items.size > 1
}
