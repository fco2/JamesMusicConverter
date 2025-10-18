package com.chuka.jamesmusicconverter.ui.urlinput

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class UrlInputUiState(
    val urlTextFieldValue: TextFieldValue = TextFieldValue(""),
    val isError: Boolean = false,
    val showAdvancedOptions: Boolean = false,
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val selectedBrowser: String = "",
    val useBrowserCookies: Boolean = false
)

@HiltViewModel
class UrlInputViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(UrlInputUiState())
    val uiState: StateFlow<UrlInputUiState> = _uiState.asStateFlow()

    /**
     * Updates the URL text field value
     */
    fun updateUrl(textFieldValue: TextFieldValue) {
        _uiState.value = _uiState.value.copy(
            urlTextFieldValue = textFieldValue,
            isError = false
        )
    }

    /**
     * Clears the URL input field
     */
    fun clearUrl() {
        _uiState.value = _uiState.value.copy(
            urlTextFieldValue = TextFieldValue(""),
            isError = false
        )
    }

    /**
     * Pastes text from clipboard
     */
    fun pasteFromClipboard(text: String) {
        _uiState.value = _uiState.value.copy(
            urlTextFieldValue = TextFieldValue(
                text = text,
                selection = androidx.compose.ui.text.TextRange(text.length)
            ),
            isError = false
        )
    }

    /**
     * Toggles advanced options visibility
     */
    fun toggleAdvancedOptions() {
        _uiState.value = _uiState.value.copy(
            showAdvancedOptions = !_uiState.value.showAdvancedOptions
        )
    }

    /**
     * Updates username
     */
    fun updateUsername(username: String) {
        _uiState.value = _uiState.value.copy(username = username)
    }

    /**
     * Updates password
     */
    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password)
    }

    /**
     * Toggles password visibility
     */
    fun togglePasswordVisibility() {
        _uiState.value = _uiState.value.copy(
            passwordVisible = !_uiState.value.passwordVisible
        )
    }

    /**
     * Updates browser cookies setting
     */
    fun updateUseBrowserCookies(use: Boolean) {
        _uiState.value = _uiState.value.copy(useBrowserCookies = use)
    }

    /**
     * Updates selected browser
     */
    fun updateSelectedBrowser(browser: String) {
        _uiState.value = _uiState.value.copy(selectedBrowser = browser)
    }

    /**
     * Validates the current URL and returns authentication data if valid
     * Returns null if URL is invalid
     */
    fun validateAndGetAuthData(): Triple<String, AuthData?, Boolean>? {
        val currentState = _uiState.value
        val url = currentState.urlTextFieldValue.text.trim()

        if (url.isBlank() || !isValidUrl(url)) {
            _uiState.value = currentState.copy(isError = true)
            return null
        }

        val authData = if (currentState.username.isNotBlank() ||
                          currentState.password.isNotBlank() ||
                          (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())) {
            AuthData(
                username = currentState.username.ifBlank { null },
                password = currentState.password.ifBlank { null },
                browser = if (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())
                    currentState.selectedBrowser else null
            )
        } else null

        return Triple(url, authData, true)
    }

    /**
     * Validates if the provided URL is valid
     */
    private fun isValidUrl(url: String): Boolean {
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

/**
 * Data class to hold authentication information
 */
data class AuthData(
    val username: String?,
    val password: String?,
    val browser: String?
)
