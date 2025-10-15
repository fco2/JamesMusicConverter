package com.chuka.jamesmusicconverter.domain.model

/**
 * Represents the progress of an ongoing conversion
 */
data class ConversionProgress(
    val percentage: Float,
    val statusMessage: String
)
