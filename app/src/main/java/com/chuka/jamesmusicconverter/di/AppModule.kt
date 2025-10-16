package com.chuka.jamesmusicconverter.di

import android.content.Context
import com.chuka.jamesmusicconverter.data.service.AudioExtractor
import com.chuka.jamesmusicconverter.data.service.DownloadNotificationService
import com.chuka.jamesmusicconverter.data.service.VideoDownloader
import com.chuka.jamesmusicconverter.data.service.YtDlpDownloader
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepositoryImpl
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
        videoDownloader: VideoDownloader,
        audioExtractor: AudioExtractor,
        notificationService: DownloadNotificationService
    ): ConversionRepository {
        return ConversionRepositoryImpl(
            videoDownloader = videoDownloader,
            audioExtractor = audioExtractor,
            notificationService = notificationService
        )
    }
}
