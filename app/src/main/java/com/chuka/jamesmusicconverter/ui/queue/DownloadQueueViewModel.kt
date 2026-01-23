package com.chuka.jamesmusicconverter.ui.queue

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.domain.model.QueueItem
import com.chuka.jamesmusicconverter.domain.model.QueueItemStatus
import com.chuka.jamesmusicconverter.domain.repository.DownloadQueueRepository
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import com.chuka.jamesmusicconverter.worker.DownloadWorkManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the download queue screen
 */
data class DownloadQueueUiState(
    val queueItems: List<QueueItem> = emptyList(),
    val activeCount: Int = 0,
    val pendingCount: Int = 0,
    val completedCount: Int = 0,
    val failedCount: Int = 0,
    val isLoading: Boolean = true
)

/**
 * ViewModel for managing download queue state and operations
 */
@HiltViewModel
class DownloadQueueViewModel @Inject constructor(
    private val queueRepository: DownloadQueueRepository,
    private val workManager: DownloadWorkManager
) : ViewModel() {

    companion object {
        private const val TAG = "DownloadQueueViewModel"
    }

    private val _uiState = MutableStateFlow(DownloadQueueUiState())
    val uiState: StateFlow<DownloadQueueUiState> = _uiState.asStateFlow()

    init {
        observeQueue()
    }

    /**
     * Observe queue items and update UI state
     */
    private fun observeQueue() {
        viewModelScope.launch {
            queueRepository.getAllQueueItems().collect { items ->
                val activeCount = items.count { it.status == QueueItemStatus.ACTIVE }
                val pendingCount = items.count { it.status == QueueItemStatus.PENDING }
                val completedCount = items.count { it.status == QueueItemStatus.COMPLETED }
                val failedCount = items.count { it.status == QueueItemStatus.FAILED || it.status == QueueItemStatus.CANCELLED }

                _uiState.value = DownloadQueueUiState(
                    queueItems = items,
                    activeCount = activeCount,
                    pendingCount = pendingCount,
                    completedCount = completedCount,
                    failedCount = failedCount,
                    isLoading = false
                )
            }
        }
    }

    /**
     * Add a URL to the queue and start download
     */
    fun addToQueue(
        url: String,
        title: String?,
        thumbnailUrl: String?,
        platform: String,
        downloadMode: DownloadMode,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ) {
        viewModelScope.launch {
            Log.d(TAG, "Adding to queue: url=$url, mode=$downloadMode")

            // Check if URL is already in active queue
            if (queueRepository.isUrlInActiveQueue(url)) {
                Log.d(TAG, "URL already in queue, skipping")
                return@launch
            }

            // Add to queue database
            val queueId = queueRepository.addToQueue(
                url = url,
                title = title,
                thumbnailUrl = thumbnailUrl,
                localThumbnailPath = null,
                platform = platform,
                downloadMode = downloadMode
            )

            Log.d(TAG, "Added to queue with ID: $queueId")

            // Enqueue WorkManager job
            workManager.enqueueDownload(
                queueId = queueId,
                url = url,
                downloadMode = downloadMode,
                username = username,
                password = password,
                cookiesFromBrowser = cookiesFromBrowser
            )
        }
    }

    /**
     * Cancel a download
     */
    fun cancelItem(item: QueueItem) {
        viewModelScope.launch {
            Log.d(TAG, "Cancelling item: ${item.id}")
            workManager.cancelDownload(item.id)
            queueRepository.cancelItem(item.id)
        }
    }

    /**
     * Retry a failed or cancelled download
     */
    fun retryItem(item: QueueItem) {
        viewModelScope.launch {
            Log.d(TAG, "Retrying item: ${item.id}")

            // Reset in database
            queueRepository.retryItem(item.id)

            // Re-enqueue with WorkManager
            workManager.retryDownload(
                queueId = item.id,
                url = item.url,
                downloadMode = item.downloadMode
            )
        }
    }

    /**
     * Remove an item from the queue
     */
    fun removeItem(item: QueueItem) {
        viewModelScope.launch {
            Log.d(TAG, "Removing item: ${item.id}")

            // Cancel if still running
            if (!item.isFinished) {
                workManager.cancelDownload(item.id)
            }

            // Remove from database
            queueRepository.removeItem(item.id)
        }
    }

    /**
     * Cancel all active downloads
     */
    fun cancelAll() {
        viewModelScope.launch {
            Log.d(TAG, "Cancelling all downloads")
            workManager.cancelAllDownloads()

            // Update database for all active/pending items
            _uiState.value.queueItems
                .filter { it.canCancel }
                .forEach { item ->
                    queueRepository.cancelItem(item.id)
                }
        }
    }

    /**
     * Clear all completed items
     */
    fun clearCompleted() {
        viewModelScope.launch {
            Log.d(TAG, "Clearing completed items")
            queueRepository.clearCompleted()
        }
    }

    /**
     * Clear all finished items (completed, cancelled, failed)
     */
    fun clearFinished() {
        viewModelScope.launch {
            Log.d(TAG, "Clearing finished items")
            queueRepository.clearFinished()
        }
    }
}
