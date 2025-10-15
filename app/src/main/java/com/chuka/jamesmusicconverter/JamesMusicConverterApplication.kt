package com.chuka.jamesmusicconverter

import android.app.Application
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepository
import com.chuka.jamesmusicconverter.domain.repository.ConversionRepositoryImpl

class JamesMusicConverterApplication : Application() {

    // Simple DI container
    val conversionRepository: ConversionRepository by lazy {
        ConversionRepositoryImpl(this)
    }
}
