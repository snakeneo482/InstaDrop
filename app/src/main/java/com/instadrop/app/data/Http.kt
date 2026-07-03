package com.instadrop.app.data

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Shared, hardened [OkHttpClient] used by both the resolver and the downloader.
 *
 * A browser-like User-Agent and generous timeouts make Instagram's public CDN
 * far more likely to serve us the page / asset instead of a bot wall.
 */
object Http {

    /** A realistic desktop User-Agent. Instagram serves richer Open Graph data to "browsers". */
    const val USER_AGENT =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
}
