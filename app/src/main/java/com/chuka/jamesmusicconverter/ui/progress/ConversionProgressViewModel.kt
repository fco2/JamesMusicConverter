package com.chuka.jamesmusicconverter.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.domain.model.ConversionProgress
import com.chuka.jamesmusicconverter.domain.model.ConversionResult
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConversionUiState {
    data object Idle : ConversionUiState()
    data class Converting(val progress: ConversionProgress, val isCancellable: Boolean = true, val generation: Int = 0) : ConversionUiState()
    data class Success(val result: ConversionResult, val generation: Int = 0) : ConversionUiState()
    data class Error(val message: String, val generation: Int = 0) : ConversionUiState()
    data class Cancelled(val message: String = "Conversion cancelled", val generation: Int = 0) : ConversionUiState()
}

@HiltViewModel
class ConversionProgressViewModel @Inject constructor(
    private val repository: ConversionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversionUiState>(ConversionUiState.Idle)
    val uiState: StateFlow<ConversionUiState> = _uiState.asStateFlow()

    private var currentConversionUrl: String? = null
    private var conversionJob: Job? = null

    // Track conversion generation to prevent stale state navigation
    private var conversionGeneration = 0

    /**
     * Gets the current conversion generation
     */
    fun getCurrentGeneration(): Int = conversionGeneration

    /**
     * Starts the conversion process for the given video URL
     */
    fun startConversion(
        videoUrl: String,
        username: String? = null,
        password: String? = null,
        cookiesFromBrowser: String? = null
    ) {
        android.util.Log.d("CHUKA_ViewModel", "=== startConversion called ===")
        android.util.Log.d("CHUKA_ViewModel", "URL: $videoUrl")
        android.util.Log.d("CHUKA_ViewModel", "Current state: ${_uiState.value}")

        // Silently cancel any ongoing conversion without setting Cancelled state
        conversionJob?.cancel()
        conversionJob = null

        // Increment generation for this new conversion
        conversionGeneration++
        val thisGeneration = conversionGeneration

        android.util.Log.d("CHUKA_ViewModel", "Starting conversion generation $thisGeneration")

        // Prevent re-triggering conversion for the same URL if already converting
        if (currentConversionUrl == videoUrl &&
            _uiState.value is ConversionUiState.Converting) {
            android.util.Log.d("CHUKA_ViewModel", "SKIPPING - Same URL already converting")
            return
        }

        currentConversionUrl = videoUrl
        _uiState.value = ConversionUiState.Idle

        android.util.Log.d("CHUKA_ViewModel", "Starting coroutine for conversion...")

        conversionJob = viewModelScope.launch {
            try {
                android.util.Log.d("CHUKA_ViewModel", "Calling repository.convertVideoToMp3...")
                // Collect progress updates
                repository.convertVideoToMp3(
                    videoUrl = videoUrl,
                    username = username,
                    password = password,
                    cookiesFromBrowser = cookiesFromBrowser
                ).collect { progress ->
                    //android.util.Log.d("CHUKA_ViewModel", "Progress: ${progress.percentage * 100}% - ${progress.statusMessage}")
                    _uiState.value = ConversionUiState.Converting(progress, isCancellable = true, generation = thisGeneration)
                }

                android.util.Log.d("CHUKA_ViewModel", "Flow completed, getting final result...")
                // Get final result
                val result = repository.getConversionResult(videoUrl)
                result.fold(
                    onSuccess = { conversionResult ->
                        android.util.Log.d("CHUKA_ViewModel", "SUCCESS: ${conversionResult.fileName} (generation $thisGeneration)")
                        _uiState.value = ConversionUiState.Success(conversionResult, generation = thisGeneration)
                        conversionJob = null
                    },
                    onFailure = { exception ->
                        android.util.Log.e("CHUKA_ViewModel", "ERROR: ${exception.message} (generation $thisGeneration)")
                        _uiState.value = ConversionUiState.Error(
                            exception.message ?: "An error occurred during conversion",
                            generation = thisGeneration
                        )
                        conversionJob = null
                    }
                )
            } catch (_: kotlinx.coroutines.CancellationException) {
                // Flow was cancelled - this is normal when navigating away or when user clicks cancel
                // Only update state if we're still in Converting state with the correct generation
                if (_uiState.value is ConversionUiState.Converting) {
                    val currentState = _uiState.value as? ConversionUiState.Converting
                    if (currentState?.generation == thisGeneration) {
                        _uiState.value = ConversionUiState.Cancelled("Conversion cancelled", generation = thisGeneration)
                    }
                }
                // DON'T clear conversionJob here - it might have been replaced by a new conversion
                // Let the new conversion or cancelConversion() handle clearing
                // Don't re-throw - let it cancel gracefully
            } catch (e: Exception) {
                _uiState.value = ConversionUiState.Error(
                    e.message ?: "An error occurred during conversion",
                    generation = thisGeneration
                )
                conversionJob = null
            }
        }
    }

    /**
     * Cancels the ongoing conversion
     */
    fun cancelConversion() {
        try {
            conversionJob?.cancel()
            conversionJob = null

            if (_uiState.value is ConversionUiState.Converting) {
                // Use current generation so the UI can properly handle this cancellation
                _uiState.value = ConversionUiState.Cancelled(
                    message = "Conversion cancelled",
                    generation = conversionGeneration
                )
            }
        } catch (_: Exception) {
            // Ignore cancellation errors - just ensure cleanup
            conversionJob = null
        }
    }

    /**
     * Check if conversion can be cancelled
     */
    fun isCancellable(): Boolean {
        return _uiState.value is ConversionUiState.Converting && conversionJob?.isActive == true
    }

    /**
     * Resets the ViewModel state for a fresh conversion
     */
    fun reset() {
        android.util.Log.d("CHUKA_ViewModel", "=== reset() called ===")
        android.util.Log.d("CHUKA_ViewModel", "State before reset: ${_uiState.value}")

        // Silently cancel any active conversion without triggering Cancelled state
        conversionJob?.cancel()
        conversionJob = null
        currentConversionUrl = null
        _uiState.value = ConversionUiState.Idle

        android.util.Log.d("CHUKA_ViewModel", "State after reset: ${_uiState.value}")
    }

    /**
     * Cleanup when ViewModel is destroyed
     */
    override fun onCleared() {
        super.onCleared()
        cancelConversion()  // Ensure cleanup
    }
}
