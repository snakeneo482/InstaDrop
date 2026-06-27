package com.instadrop.app.data.download

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.instadrop.app.domain.model.MediaItem
import com.instadrop.app.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.OutputStream
import kotlin.coroutines.coroutineContext

/** Result of a completed save: where it landed and how big it was. */
data class SavedMedia(val uri: Uri, val displayName: String, val sizeBytes: Long)

/**
 * Streams a remote [MediaItem] to the device gallery.
 *
 * Files go to /Movies/InstaDrop (video) or /Pictures/InstaDrop (image) using
 * MediaStore, so they show up in the gallery with no storage permission on
 * Android 10+. Below 10, the caller must hold WRITE_EXTERNAL_STORAGE.
 */
class MediaDownloader(private val context: Context) {

    private val client = OkHttpClient()

    /**
     * @param onProgress invoked on the IO thread with (percent 0..100, bytesPerSecond).
     *        Percent is -1 when total size is unknown.
     */
    suspend fun download(
        item: MediaItem,
        displayName: String,
        onProgress: (percent: Int, bytesPerSecond: Long) -> Unit,
    ): SavedMedia = withContext(Dispatchers.IO) {
        val request = Request.Builder().url(item.downloadUrl).build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Server returned HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty response body")
            val total = body.contentLength()

            val target = createTarget(item.type, displayName)
            var written = 0L
            try {
                context.contentResolver.openOutputStream(target.uri)?.use { out ->
                    written = body.byteStream().use { input ->
                        copyStreaming(input, out, total, onProgress)
                    }
                } ?: throw IOException("Could not open output stream")
            } catch (t: Throwable) {
                // Roll back the half-written MediaStore row so it doesn't linger.
                runCatching { context.contentResolver.delete(target.uri, null, null) }
                throw t
            }

            publish(target.uri)
            SavedMedia(target.uri, target.displayName, written)
        }
    }

    private data class Target(val uri: Uri, val displayName: String)

    private fun createTarget(type: MediaType, displayName: String): Target {
        val isVideo = type == MediaType.VIDEO
        val mime = if (isVideo) "video/mp4" else "image/jpeg"
        val ext = if (isVideo) ".mp4" else ".jpg"
        val name = if (displayName.contains('.')) displayName else displayName + ext

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (isVideo) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            }
        } else {
            if (isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val base = if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES
                put(MediaStore.MediaColumns.RELATIVE_PATH, "$base/InstaDrop")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val uri = context.contentResolver.insert(collection, values)
            ?: throw IOException("Failed to create gallery entry")
        return Target(uri, name)
    }

    /** Clears IS_PENDING so the file becomes visible in the gallery (Q+). */
    private fun publish(uri: Uri) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            context.contentResolver.update(uri, values, null, null)
        }
    }

    private suspend fun copyStreaming(
        input: java.io.InputStream,
        out: OutputStream,
        total: Long,
        onProgress: (Int, Long) -> Unit,
    ): Long {
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var written = 0L
        var lastTick = System.currentTimeMillis()
        var bytesSinceTick = 0L
        var read: Int
        while (input.read(buffer).also { read = it } >= 0) {
            coroutineContext.ensureActive() // cancellation support
            out.write(buffer, 0, read)
            written += read
            bytesSinceTick += read

            val now = System.currentTimeMillis()
            val elapsed = now - lastTick
            if (elapsed >= PROGRESS_INTERVAL_MS) {
                val bps = if (elapsed > 0) bytesSinceTick * 1000 / elapsed else 0
                val percent = if (total > 0) ((written * 100) / total).toInt() else -1
                onProgress(percent, bps)
                lastTick = now
                bytesSinceTick = 0
            }
        }
        out.flush()
        onProgress(if (total > 0) 100 else -1, 0)
        return written
    }

    private companion object {
        const val PROGRESS_INTERVAL_MS = 150L
    }
}
