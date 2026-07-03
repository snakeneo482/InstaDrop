package com.instadrop.app.data.history

import android.net.Uri
import androidx.core.net.toUri
import com.instadrop.app.domain.model.MediaType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** UI-facing model for a completed download (URI resolved). */
data class DownloadEntry(
    val id: Long,
    val displayName: String,
    val uri: Uri,
    val type: MediaType,
    val thumbnailUrl: String?,
    val sizeBytes: Long,
    val savedAt: Long,
)

/**
 * Persistent history of completed downloads, backed by Room. Survives app
 * restarts. The rest of the app sees only [DownloadEntry]; the entity mapping
 * stays here.
 */
class DownloadHistory(private val dao: DownloadDao) {

    val entries: Flow<List<DownloadEntry>> =
        dao.observeAll().map { rows -> rows.map { it.toEntry() } }

    suspend fun add(
        displayName: String,
        uri: Uri,
        type: MediaType,
        thumbnailUrl: String?,
        sizeBytes: Long,
    ) {
        dao.insert(
            DownloadEntity(
                displayName = displayName,
                uri = uri.toString(),
                type = type.name,
                thumbnailUrl = thumbnailUrl,
                sizeBytes = sizeBytes,
                savedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun remove(id: Long) = dao.delete(id)

    suspend fun clear() = dao.deleteAll()

    private fun DownloadEntity.toEntry() = DownloadEntry(
        id = id,
        displayName = displayName,
        uri = uri.toUri(),
        type = runCatching { MediaType.valueOf(type) }.getOrDefault(MediaType.VIDEO),
        thumbnailUrl = thumbnailUrl,
        sizeBytes = sizeBytes,
        savedAt = savedAt,
    )
}
