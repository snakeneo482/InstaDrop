package com.instadrop.app.data.resolver

import android.util.Log
import com.instadrop.app.domain.model.InstaMedia

/**
 * Tries each [MediaResolver] in order and returns the first success.
 *
 * In practice: attempt the real [OpenGraphResolver] first (real public media),
 * and fall back to a safe stand-in (e.g. [StubMediaResolver]) so the app always
 * has something to preview/download instead of dead-ending on a transient failure
 * or a login-walled post. If *every* resolver fails, the last error is rethrown.
 */
class ResolverChain(
    private val resolvers: List<MediaResolver>,
) : MediaResolver {

    init {
        require(resolvers.isNotEmpty()) { "ResolverChain needs at least one resolver" }
    }

    override suspend fun resolve(url: String): InstaMedia {
        var first: Throwable? = null
        for (resolver in resolvers) {
            try {
                return resolver.resolve(url)
            } catch (e: BackendNotConfigured) {
                Log.i(TAG, "${resolver::class.simpleName} skipped: ${e.message}")
            } catch (e: Throwable) {
                if (first == null) first = e // keep the primary resolver's (most relevant) error
                Log.w(TAG, "${resolver::class.simpleName} failed for $url: ${e.message}")
            }
        }
        throw first ?: ResolveException("No resolver could handle this link.")
    }

    private companion object {
        const val TAG = "ResolverChain"
    }
}
