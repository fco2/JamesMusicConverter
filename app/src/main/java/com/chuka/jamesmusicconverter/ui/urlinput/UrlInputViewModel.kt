package com.chuka.jamesmusicconverter.ui.urlinput

import androidx.lifecycle.ViewModel

class UrlInputViewModel : ViewModel() {

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
