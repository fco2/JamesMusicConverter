package com.chuka.jamesmusicconverter.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.chuka.jamesmusicconverter.data.local.AppDatabase
import com.chuka.jamesmusicconverter.data.local.DownloadHistoryDao
import com.chuka.jamesmusicconverter.data.local.DownloadQueueDao
import com.chuka.jamesmusicconverter.data.service.AudioExtractor
import com.chuka.jamesmusicconverter.data.service.DownloadNotificationService
import com.chuka.jamesmusicconverter.data.service.VideoDownloader
import com.chuka.jamesmusicconverter.data.service.YtDlpDownloader
import com.chuka.jamesmusicconverter.data.util.ThumbnailManager
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepositoryImpl
import com.chuka.jamesmusicconverter.domain.repository.DownloadHistoryRepository
import com.chuka.jamesmusicconverter.domain.repository.DownloadQueueRepository
import com.chuka.jamesmusicconverter.worker.DownloadWorkManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideYtDlpDownloader(
        @ApplicationContext context: Context
    ): YtDlpDownloader {
        return YtDlpDownloader(context)
    }

    @Provides
    @Singleton
    fun provideVideoDownloader(
        @ApplicationContext context: Context
    ): VideoDownloader {
        return VideoDownloader(context)
    }

    @Provides
    @Singleton
    fun provideAudioExtractor(
        @ApplicationContext context: Context
    ): AudioExtractor {
        return AudioExtractor(context)
    }

    @Provides
    @Singleton
    fun provideDownloadNotificationService(
        @ApplicationContext context: Context
    ): DownloadNotificationService {
        return DownloadNotificationService(context)
    }

    @Provides
    @Singleton
    fun provideConversionRepository(
        @ApplicationContext context: Context,
        videoDownloader: VideoDownloader,
        audioExtractor: AudioExtractor,
        notificationService: DownloadNotificationService,
        downloadHistoryRepository: DownloadHistoryRepository,
        thumbnailManager: ThumbnailManager
    ): ConversionRepository {
        return ConversionRepositoryImpl(
            context = context,
            videoDownloader = videoDownloader,
            audioExtractor = audioExtractor,
            notificationService = notificationService,
            downloadHistoryRepository = downloadHistoryRepository,
            thumbnailManager = thumbnailManager
        )
    }

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "james_music_converter_db"
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
    }

    @Provides
    @Singleton
    fun provideDownloadHistoryDao(
        database: AppDatabase
    ): DownloadHistoryDao {
        return database.downloadHistoryDao()
    }

    @Provides
    @Singleton
    fun provideDownloadQueueDao(
        database: AppDatabase
    ): DownloadQueueDao {
        return database.downloadQueueDao()
    }

    @Provides
    @Singleton
    fun provideDownloadHistoryRepository(
        dao: DownloadHistoryDao
    ): DownloadHistoryRepository {
        return DownloadHistoryRepository(dao)
    }

    @Provides
    @Singleton
    fun provideDownloadQueueRepository(
        dao: DownloadQueueDao
    ): DownloadQueueRepository {
        return DownloadQueueRepository(dao)
    }

    @Provides
    @Singleton
    fun provideThumbnailManager(
        @ApplicationContext context: Context
    ): ThumbnailManager {
        return ThumbnailManager(context)
    }

    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }

    @Provides
    @Singleton
    fun provideDownloadWorkManager(
        workManager: WorkManager
    ): DownloadWorkManager {
        return DownloadWorkManager(workManager)
    }
}
