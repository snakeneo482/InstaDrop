package com.instadrop.app.data.history

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DownloadEntity::class], version = 1, exportSchema = false)
abstract class InstaDropDatabase : RoomDatabase() {
    abstract fun downloadDao(): DownloadDao

    companion object {
        fun build(context: Context): InstaDropDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                InstaDropDatabase::class.java,
                "instadrop.db",
            ).build()
    }
}
