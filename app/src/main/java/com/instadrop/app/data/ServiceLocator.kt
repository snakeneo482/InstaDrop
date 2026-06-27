package com.instadrop.app.data

import android.content.Context
import com.instadrop.app.data.download.MediaDownloader
import com.instadrop.app.data.history.DownloadHistory
import com.instadrop.app.data.history.InstaDropDatabase
import com.instadrop.app.data.resolver.MediaResolver
import com.instadrop.app.data.resolver.StubMediaResolver

/**
 * Tiny manual dependency container — enough for an MVP without dragging in a
 * full DI framework. Swap [resolver] here to wire in a real backend.
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

    fun init(context: Context) {
        val app = context.applicationContext
        val db = InstaDropDatabase.build(app)
        // TODO: replace StubMediaResolver with your real extraction backend.
        resolver = StubMediaResolver()
        downloader = MediaDownloader(app)
        history = DownloadHistory(db.downloadDao())
        notifier = Notifier(app)
    }
}
