package com.chuka.jamesmusicconverter.ui.urlinput

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class UrlInputViewModel @Inject constructor() : ViewModel() {

    /**
     * Validates if the provided URL is valid
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val urlPattern = Regex(
                "^(https?://)?(www\\.)?" +
                        "[-a-zA-Z0-9@:%._+~#=]{1,256}\\.[a-zA-Z0-9()]{1,6}\\b" +
                        "([-a-zA-Z0-9()@:%_+.~#?&/=]*)\$"
            )
            urlPattern.matches(url)
        } catch (e: Exception) {
            false
        }
    }
}
