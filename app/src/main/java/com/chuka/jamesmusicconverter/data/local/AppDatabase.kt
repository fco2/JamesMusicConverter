package com.chuka.jamesmusicconverter.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Main Room database for the application
 */
@Database(
    entities = [DownloadHistoryEntity::class, DownloadQueueEntity::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadHistoryDao(): DownloadHistoryDao
    abstract fun downloadQueueDao(): DownloadQueueDao

    companion object {
        /**
         * Migration from version 1 to 2: Add download_queue table
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS download_queue (
                        id TEXT PRIMARY KEY NOT NULL,
                        url TEXT NOT NULL,
                        title TEXT,
                        thumbnailUrl TEXT,
                        localThumbnailPath TEXT,
                        platform TEXT NOT NULL,
                        downloadMode TEXT NOT NULL,
                        status TEXT NOT NULL,
                        progress REAL NOT NULL,
                        statusMessage TEXT NOT NULL,
                        errorMessage TEXT,
                        filePath TEXT,
                        fileSize INTEGER NOT NULL,
                        addedAt INTEGER NOT NULL,
                        startedAt INTEGER,
                        completedAt INTEGER,
                        retryCount INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        /**
         * Migration from version 2 to 3: Add durationMillis field to download_queue
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE download_queue ADD COLUMN durationMillis INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Migration from version 3 to 4: Add carousel/playlist tracking fields
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add carousel fields to download_queue
                db.execSQL("ALTER TABLE download_queue ADD COLUMN isPartOfCarousel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE download_queue ADD COLUMN carouselGroupId TEXT")
                db.execSQL("ALTER TABLE download_queue ADD COLUMN carouselPosition INTEGER")
                db.execSQL("ALTER TABLE download_queue ADD COLUMN carouselTotal INTEGER")

                // Add carousel fields to download_history
                db.execSQL("ALTER TABLE download_history ADD COLUMN isPartOfCarousel INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE download_history ADD COLUMN carouselGroupId TEXT")
                db.execSQL("ALTER TABLE download_history ADD COLUMN carouselPosition INTEGER")
                db.execSQL("ALTER TABLE download_history ADD COLUMN carouselTotal INTEGER")
            }
        }
    }
}
