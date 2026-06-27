package com.instadrop.app.data.history

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Room row for a completed download. URI/type stored as strings. */
@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val displayName: String,
    val uri: String,
    val type: String,
    val thumbnailUrl: String?,
    val sizeBytes: Long,
    val savedAt: Long,
)
