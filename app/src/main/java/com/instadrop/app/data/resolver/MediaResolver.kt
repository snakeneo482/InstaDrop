package com.instadrop.app.data.resolver

import com.instadrop.app.domain.model.InstaMedia

/**
 * Turns an Instagram URL into concrete, downloadable media.
 *
 * This is the single seam where the actual extraction backend plugs in. The
 * rest of the app (UI, downloader, history) depends only on this interface, so
 * you can swap [StubMediaResolver] for a real implementation — your own backend
 * API, a yt-dlp service, etc. — without touching anything else.
 */
interface MediaResolver {
    /**
     * @throws ResolveException if the URL is unsupported or extraction fails.
     */
    suspend fun resolve(url: String): InstaMedia
}

class ResolveException(message: String, cause: Throwable? = null) : Exception(message, cause)
