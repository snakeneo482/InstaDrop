package com.instadrop.app.data.resolver

import com.instadrop.app.domain.model.InstaMedia

/**
 * Turns a shared link into concrete, downloadable media.
 *
 * This is the single seam where extraction backends plug in. The rest of the
 * app (UI, downloader, history) depends only on this interface, so resolvers —
 * the Cobalt backend, the Instagram GraphQL reader, etc. — can be swapped or
 * chained without touching anything else.
 */
interface MediaResolver {
    /**
     * @throws ResolveException if the URL is unsupported or extraction fails.
     */
    suspend fun resolve(url: String): InstaMedia
}

open class ResolveException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Thrown by a backend resolver when it has nothing configured to work with (no
 * server URL). The [ResolverChain] treats this as "skip me", not as a failure,
 * so a later resolver's more specific error is what surfaces to the user.
 */
class BackendNotConfigured(message: String) : ResolveException(message)
