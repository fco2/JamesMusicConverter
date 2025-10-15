package com.chuka.jamesmusicconverter.ui.progress

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.domain.model.ConversionProgress
import com.chuka.jamesmusicconverter.domain.model.ConversionResult
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConversionUiState {
    data object Idle : ConversionUiState()
    data class Converting(val progress: ConversionProgress) : ConversionUiState()
    data class Success(val result: ConversionResult) : ConversionUiState()
    data class Error(val message: String) : ConversionUiState()
}

class ConversionProgressViewModel(
    private val repository: ConversionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversionUiState>(ConversionUiState.Idle)
    val uiState: StateFlow<ConversionUiState> = _uiState.asStateFlow()

    /**
     * Starts the conversion process for the given video URL
     */
    fun startConversion(videoUrl: String) {
        viewModelScope.launch {
            try {
                // Collect progress updates
                repository.convertVideoToMp3(videoUrl).collect { progress ->
                    _uiState.value = ConversionUiState.Converting(progress)
                }

                // Get final result
                val result = repository.getConversionResult(videoUrl)
                result.fold(
                    onSuccess = { conversionResult ->
                        _uiState.value = ConversionUiState.Success(conversionResult)
                    },
                    onFailure = { exception ->
                        _uiState.value = ConversionUiState.Error(
                            exception.message ?: "An error occurred during conversion"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ConversionUiState.Error(
                    e.message ?: "An error occurred during conversion"
                )
            }
        }
    }
}
