package com.chuka.jamesmusicconverter.worker

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper class for WorkManager to manage download work requests
 */
@Singleton
class DownloadWorkManager @Inject constructor(
    private val workManager: WorkManager
) {
    companion object {
        private const val TAG = "DownloadWorkManager"
        private const val DOWNLOAD_WORK_TAG = "download_work"
    }

    /**
     * Enqueue a download work request
     *
     * @param queueId Unique ID for this download (used for tracking and cancellation)
     * @param url Video URL to download
     * @param downloadMode AUDIO or VIDEO mode
     * @param username Optional username for authentication
     * @param password Optional password for authentication
     * @param cookiesFromBrowser Optional browser name to extract cookies from
     */
    fun enqueueDownload(
        queueId: String,
        url: String,
        downloadMode: DownloadMode,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ) {
        Log.d(TAG, "Enqueueing download: queueId=$queueId, url=$url, mode=$downloadMode")

        val inputData = workDataOf(
            DownloadWorker.KEY_QUEUE_ID to queueId,
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_DOWNLOAD_MODE to downloadMode.name,
            DownloadWorker.KEY_USERNAME to username,
            DownloadWorker.KEY_PASSWORD to password,
            DownloadWorker.KEY_COOKIES_BROWSER to cookiesFromBrowser
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(DOWNLOAD_WORK_TAG)
            .addTag(queueId)  // Add queueId as tag for easy cancellation
            .build()

        // Use unique work name to allow tracking and replacement
        workManager.enqueueUniqueWork(
            queueId,
            ExistingWorkPolicy.KEEP,  // Keep existing if already enqueued
            workRequest
        )

        Log.d(TAG, "Work request enqueued: ${workRequest.id}")
    }

    /**
     * Cancel a specific download by queue ID
     */
    fun cancelDownload(queueId: String) {
        Log.d(TAG, "Cancelling download: $queueId")
        workManager.cancelUniqueWork(queueId)
    }

    /**
     * Cancel all downloads
     */
    fun cancelAllDownloads() {
        Log.d(TAG, "Cancelling all downloads")
        workManager.cancelAllWorkByTag(DOWNLOAD_WORK_TAG)
    }

    /**
     * Get work info for a specific download
     */
    fun getWorkInfoLiveData(queueId: String): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosForUniqueWorkLiveData(queueId)
    }

    /**
     * Get work info for all downloads
     */
    fun getAllDownloadsLiveData(): LiveData<List<WorkInfo>> {
        return workManager.getWorkInfosByTagLiveData(DOWNLOAD_WORK_TAG)
    }

    /**
     * Check if a download is currently running or enqueued
     */
    suspend fun isDownloadActive(queueId: String): Boolean {
        val workInfos = workManager.getWorkInfosForUniqueWork(queueId).get()
        return workInfos.any { info ->
            info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
        }
    }

    /**
     * Get the number of active/enqueued downloads
     */
    suspend fun getActiveDownloadCount(): Int {
        val workInfos = workManager.getWorkInfosByTag(DOWNLOAD_WORK_TAG).get()
        return workInfos.count { info ->
            info.state == WorkInfo.State.RUNNING || info.state == WorkInfo.State.ENQUEUED
        }
    }

    /**
     * Retry a failed download by re-enqueueing it
     */
    fun retryDownload(
        queueId: String,
        url: String,
        downloadMode: DownloadMode,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ) {
        Log.d(TAG, "Retrying download: $queueId")

        // Cancel any existing work with this ID first
        workManager.cancelUniqueWork(queueId)

        // Then enqueue a new work request
        val inputData = workDataOf(
            DownloadWorker.KEY_QUEUE_ID to queueId,
            DownloadWorker.KEY_URL to url,
            DownloadWorker.KEY_DOWNLOAD_MODE to downloadMode.name,
            DownloadWorker.KEY_USERNAME to username,
            DownloadWorker.KEY_PASSWORD to password,
            DownloadWorker.KEY_COOKIES_BROWSER to cookiesFromBrowser
        )

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<DownloadWorker>()
            .setInputData(inputData)
            .setConstraints(constraints)
            .addTag(DOWNLOAD_WORK_TAG)
            .addTag(queueId)
            .build()

        workManager.enqueueUniqueWork(
            queueId,
            ExistingWorkPolicy.REPLACE,  // Replace existing for retry
            workRequest
        )
    }
}
