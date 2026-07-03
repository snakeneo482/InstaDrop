package com.instadrop.app.data

import android.content.Context
import com.instadrop.app.data.download.MediaDownloader
import com.instadrop.app.data.history.DownloadHistory
import com.instadrop.app.data.history.InstaDropDatabase
import com.instadrop.app.data.resolver.CobaltResolver
import com.instadrop.app.data.resolver.GraphQlResolver
import com.instadrop.app.data.resolver.MediaResolver
import com.instadrop.app.data.resolver.OpenGraphResolver
import com.instadrop.app.data.resolver.ResolverChain
import com.instadrop.app.data.settings.SettingsRepository

/**
 * Tiny manual dependency container — enough for an MVP without dragging in a
 * full DI framework. Swap [resolver] here to wire in a different backend.
 */
object ServiceLocator {
    lateinit var resolver: MediaResolver
        private set
    lateinit var downloader: MediaDownloader
        private set
    lateinit var history: DownloadHistory
        private set
    lateinit var notifier: Notifier
        private set
    lateinit var settings: SettingsRepository
        private set

    fun init(context: Context) {
        val app = context.applicationContext
        val db = InstaDropDatabase.build(app)
        settings = SettingsRepository(app)

        // Resolution order:
        //  1) Cobalt backend (self-hosted) — handles every supported site incl.
        //     YouTube / TikTok / X / IG Stories. Skips itself if no URL is set.
        //  2) Instagram web GraphQL — works for public IG posts with no server.
        //  3) Open Graph meta tags — last-ditch fallback.
        // The chain surfaces the primary (first) resolver's error if all fail.
        resolver = ResolverChain(
            listOf(
                CobaltResolver(settings, Http.client),
                GraphQlResolver(Http.client),
                OpenGraphResolver(Http.client),
            ),
        )
        downloader = MediaDownloader(app, Http.client)
        history = DownloadHistory(db.downloadDao())
        notifier = Notifier(app)
    }
}
