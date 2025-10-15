package com.chuka.jamesmusicconverter.domain.model

/**
 * Sealed class representing the state of a conversion process
 */
sealed class ConversionState {
    data object Idle : ConversionState()
    data class InProgress(val progress: ConversionProgress) : ConversionState()
    data class Success(val result: ConversionResult) : ConversionState()
    data class Error(val message: String) : ConversionState()
}
