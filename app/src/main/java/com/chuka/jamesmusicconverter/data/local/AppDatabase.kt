package com.chuka.jamesmusicconverter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main Room database for the application
 */
@Database(
    entities = [DownloadHistoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao
}
