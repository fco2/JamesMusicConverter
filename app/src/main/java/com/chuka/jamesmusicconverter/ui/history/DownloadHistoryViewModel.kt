package com.chuka.jamesmusicconverter.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.domain.model.DownloadHistory
import com.chuka.jamesmusicconverter.domain.repository.DownloadHistoryRepository
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for download history
 */
data class DownloadHistoryUiState(
    val downloads: List<DownloadHistory> = emptyList(),
    val isLoading: Boolean = false,
    val filterMode: DownloadMode? = null,  // null = all
    val searchQuery: String = "",
    val totalCount: Int = 0,
    val totalSize: Long = 0L,
    val isSelectionMode: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

/**
 * ViewModel for download history screen
 */
@HiltViewModel
class DownloadHistoryViewModel @Inject constructor(
    private val historyRepository: DownloadHistoryRepository,
    val fileActionHandler: com.chuka.jamesmusicconverter.domain.FileActionHandler
) : ViewModel() {

    private val _uiState = MutableStateFlow(DownloadHistoryUiState())
    val uiState: StateFlow<DownloadHistoryUiState> = _uiState.asStateFlow()

    init {
        loadHistory()
        loadStats()
    }

    /**
     * Load download history
     */
    private fun loadHistory() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val currentState = _uiState.value

                when {
                    currentState.searchQuery.isNotBlank() -> {
                        historyRepository.searchDownloads(currentState.searchQuery)
                            .collect { downloads ->
                                _uiState.value = _uiState.value.copy(
                                    downloads = downloads,
                                    isLoading = false
                                )
                            }
                    }

                    currentState.filterMode != null -> {
                        historyRepository.getDownloadsByMode(currentState.filterMode)
                            .collect { downloads ->
                                _uiState.value = _uiState.value.copy(
                                    downloads = downloads,
                                    isLoading = false
                                )
                            }
                    }

                    else -> {
                        historyRepository.getAllDownloads()
                            .collect { downloads ->
                                _uiState.value = _uiState.value.copy(
                                    downloads = downloads,
                                    isLoading = false
                                )
                            }
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            }
        }
    }

    /**
     * Load statistics
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val count = historyRepository.getTotalDownloadCount()
                val size = historyRepository.getTotalFileSize()

                _uiState.value = _uiState.value.copy(
                    totalCount = count,
                    totalSize = size
                )
            } catch (e: Exception) {
                // Ignore stats errors
            }
        }
    }

    /**
     * Search downloads
     */
    fun searchDownloads(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadHistory()
    }

    /**
     * Filter by mode
     */
    fun filterByMode(mode: DownloadMode?) {
        _uiState.value = _uiState.value.copy(filterMode = mode)
        loadHistory()
    }

    /**
     * Delete a download
     */
    fun deleteDownload(id: Long) {
        viewModelScope.launch {
            try {
                historyRepository.deleteDownload(id)
                loadStats()  // Refresh stats after deletion
            } catch (e: Exception) {
                android.util.Log.e("DownloadHistoryVM", "Error deleting download", e)
            }
        }
    }

    /**
     * Clear all history
     */
    fun clearAllHistory() {
        viewModelScope.launch {
            try {
                historyRepository.clearAllHistory()
                loadStats()  // Refresh stats
            } catch (e: Exception) {
                android.util.Log.e("DownloadHistoryVM", "Error clearing history", e)
            }
        }
    }

    /**
     * Refresh history
     */
    fun refresh() {
        loadHistory()
        loadStats()
    }

    /**
     * Enable selection mode
     */
    fun enableSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = true,
            selectedIds = emptySet()
        )
    }

    /**
     * Disable selection mode
     */
    fun disableSelectionMode() {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = false,
            selectedIds = emptySet()
        )
    }

    /**
     * Toggle item selection
     */
    fun toggleSelection(id: Long) {
        val currentSelected = _uiState.value.selectedIds
        _uiState.value = _uiState.value.copy(
            selectedIds = if (currentSelected.contains(id)) {
                currentSelected - id
            } else {
                currentSelected + id
            }
        )
    }

    /**
     * Select all items
     */
    fun selectAll() {
        _uiState.value = _uiState.value.copy(
            selectedIds = _uiState.value.downloads.map { it.id }.toSet()
        )
    }

    /**
     * Delete selected items
     */
    fun deleteSelected() {
        viewModelScope.launch {
            try {
                _uiState.value.selectedIds.forEach { id ->
                    historyRepository.deleteDownload(id)
                }
                disableSelectionMode()
                loadStats()
            } catch (e: Exception) {
                android.util.Log.e("DownloadHistoryVM", "Error deleting selected", e)
            }
        }
    }
}
