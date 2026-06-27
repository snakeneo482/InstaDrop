package com.instadrop.app.data.resolver

import com.instadrop.app.domain.InstagramUrl
import com.instadrop.app.domain.model.InstaMedia
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import com.instadrop.app.domain.model.PostKind
import kotlinx.coroutines.delay

/**
 * A self-contained, network-free stand-in for a real extraction backend.
 *
 * It validates the URL, simulates a short "fetching" delay, and returns a
 * publicly-hosted sample video so the ENTIRE flow — preview, download, save to
 * gallery, history — works end-to-end on a real device today. Replace this with
 * your real [MediaResolver] when the backend is ready; nothing else changes.
 */
class StubMediaResolver : MediaResolver {

    override suspend fun resolve(url: String): InstaMedia {
        val clean = InstagramUrl.extract(url)
            ?: throw ResolveException("That doesn't look like an Instagram link.")

        delay(900) // pretend we're talking to a server

        val kind = InstagramUrl.classify(clean)

        return when (kind) {
            PostKind.PROFILE_PIC -> InstaMedia(
                sourceUrl = clean,
                kind = kind,
                author = "instagram_user",
                caption = null,
                items = listOf(
                    MediaItem(
                        type = MediaType.IMAGE,
                        downloadUrl = SAMPLE_IMAGE,
                        thumbnailUrl = SAMPLE_IMAGE,
                        isHd = true,
                    ),
                ),
            )

            PostKind.CAROUSEL, PostKind.POST -> InstaMedia(
                sourceUrl = clean,
                kind = kind,
                author = "sample_account",
                caption = "Sample post caption — swap StubMediaResolver for your backend.",
                items = listOf(
                    MediaItem(MediaType.VIDEO, SAMPLE_VIDEO, SAMPLE_THUMB, durationSeconds = 15, isHd = true),
                ),
            )

            else -> InstaMedia(
                sourceUrl = clean,
                kind = kind,
                author = "sample_account",
                caption = "Sample ${kind.label.lowercase()} — this is stub data.",
                items = listOf(
                    MediaItem(MediaType.VIDEO, SAMPLE_VIDEO, SAMPLE_THUMB, durationSeconds = 28, isHd = true),
                ),
            )
        }
    }

    private companion object {
        // Small, reliably-hosted Creative Commons sample assets.
        const val SAMPLE_VIDEO =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4"
        const val SAMPLE_THUMB =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerBlazes.jpg"
        const val SAMPLE_IMAGE =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/images/ForBiggerMeltdowns.jpg"
    }
}
