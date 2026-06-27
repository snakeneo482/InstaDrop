package com.instadrop.app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.instadrop.app.R
import com.instadrop.app.domain.model.MediaType

/**
 * Posts download progress and "download complete" notifications.
 *
 * If POST_NOTIFICATIONS isn't granted (Android 13+), NotificationManagerCompat
 * silently no-ops, so callers don't need to guard every call.
 */
class Notifier(private val context: Context) {

    private val manager = NotificationManagerCompat.from(context)

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Downloads",
                NotificationManager.IMPORTANCE_LOW,
            ).apply { description = "Download progress and completion" }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    fun showProgress(id: Int, title: String, percent: Int) {
        val builder = base(title)
            .setContentText(if (percent >= 0) "$percent%" else "Downloading…")
            .setOngoing(true)
            .setProgress(100, percent.coerceAtLeast(0), percent < 0)
        notify(id, builder)
    }

    fun showComplete(id: Int, title: String, uri: Uri, type: MediaType) {
        val mime = if (type == MediaType.VIDEO) "video/*" else "image/*"
        val openIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val pending = PendingIntent.getActivity(context, id, openIntent, flags)

        val builder = base("Download complete")
            .setContentText(title)
            .setOngoing(false)
            .setAutoCancel(true)
            .setContentIntent(pending)
        notify(id, builder)
    }

    fun cancel(id: Int) = manager.cancel(id)

    private fun base(title: String) =
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

    private fun notify(id: Int, builder: NotificationCompat.Builder) {
        runCatching { manager.notify(id, builder.build()) } // ignore if no permission
    }

    companion object {
        private const val CHANNEL_ID = "downloads"
    }
}
