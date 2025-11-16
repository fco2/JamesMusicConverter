package com.chuka.jamesmusicconverter.ui.urlinput

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.data.service.YtDlpDownloader
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Video preview information
 */
data class VideoPreview(
    val title: String,
    val thumbnail: String?,
    val duration: Long,
    val uploader: String?,
    val platform: VideoPlatform
)

/**
 * Supported video platforms for custom UI
 */
enum class VideoPlatform {
    YOUTUBE,
    TIKTOK,
    INSTAGRAM,
    TWITTER,
    FACEBOOK,
    DAILYMOTION,
    OTHER;

    fun getDisplayName(): String = when (this) {
        YOUTUBE -> "YouTube"
        TIKTOK -> "TikTok"
        INSTAGRAM -> "Instagram"
        TWITTER -> "Twitter/X"
        FACEBOOK -> "Facebook"
        DAILYMOTION -> "Dailymotion"
        OTHER -> "Video"
    }
}

data class UrlInputUiState(
    val urlTextFieldValue: TextFieldValue = TextFieldValue(""),
    val isError: Boolean = false,
    val errorMessage: String? = null,
    val showAdvancedOptions: Boolean = false,
    val username: String = "",
    val password: String = "",
    val passwordVisible: Boolean = false,
    val selectedBrowser: String = "",
    val useBrowserCookies: Boolean = false,
    val downloadMode: DownloadMode = DownloadMode.VIDEO,  // Default to video mode
    val videoPreview: VideoPreview? = null,
    val isLoadingPreview: Boolean = false,
    val previewError: String? = null
)

@HiltViewModel
class UrlInputViewModel @Inject constructor(
    private val ytDlpDownloader: YtDlpDownloader
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlInputUiState())
    val uiState: StateFlow<UrlInputUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    /**
     * Updates the URL text field value and fetches video preview
     */
    fun updateUrl(textFieldValue: TextFieldValue) {
        _uiState.value = _uiState.value.copy(
            urlTextFieldValue = textFieldValue,
            isError = false,
            errorMessage = null
        )

        // Fetch preview if URL looks valid
        val url = textFieldValue.text.trim()
        if (isValidUrl(url)) {
            fetchVideoPreview(url)
        } else {
            // Clear preview if URL is invalid
            _uiState.value = _uiState.value.copy(
                videoPreview = null,
                previewError = null
            )
        }
    }

    /**
     * Clears the URL input field
     */
    fun clearUrl() {
        previewJob?.cancel()
        _uiState.value = _uiState.value.copy(
            urlTextFieldValue = TextFieldValue(""),
            isError = false,
            errorMessage = null,
            videoPreview = null,
            previewError = null,
            isLoadingPreview = false
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
            isError = false,
            errorMessage = null
        )

        // Fetch preview if URL is valid
        if (isValidUrl(text)) {
            fetchVideoPreview(text)
        }
    }

    /**
     * Fetches video preview information from the URL
     */
    private fun fetchVideoPreview(url: String) {
        // Cancel any existing preview fetch
        previewJob?.cancel()

        previewJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPreview = true,
                previewError = null
            )

            try {
                val videoInfo = ytDlpDownloader.getVideoInfo(url)

                if (videoInfo != null) {
                    val platform = detectPlatform(url)
                    val preview = VideoPreview(
                        title = videoInfo.title,
                        thumbnail = videoInfo.thumbnail,
                        duration = videoInfo.duration,
                        uploader = videoInfo.uploader,
                        platform = platform
                    )

                    _uiState.value = _uiState.value.copy(
                        videoPreview = preview,
                        isLoadingPreview = false,
                        previewError = null
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        videoPreview = null,
                        isLoadingPreview = false,
                        previewError = "Could not fetch video information"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("UrlInputViewModel", "Error fetching video preview", e)
                _uiState.value = _uiState.value.copy(
                    videoPreview = null,
                    isLoadingPreview = false,
                    previewError = null  // Don't show error, just silently fail
                )
            }
        }
    }

    /**
     * Detects the video platform from URL
     */
    private fun detectPlatform(url: String): VideoPlatform {
        return when {
            url.contains("youtube.com", ignoreCase = true) ||
                    url.contains("youtu.be", ignoreCase = true) -> VideoPlatform.YOUTUBE

            url.contains("tiktok.com", ignoreCase = true) -> VideoPlatform.TIKTOK

            url.contains("instagram.com", ignoreCase = true) -> VideoPlatform.INSTAGRAM

            url.contains("twitter.com", ignoreCase = true) ||
                    url.contains("x.com", ignoreCase = true) -> VideoPlatform.TWITTER

            url.contains("facebook.com", ignoreCase = true) ||
                    url.contains("fb.watch", ignoreCase = true) -> VideoPlatform.FACEBOOK

            url.contains("dailymotion.com", ignoreCase = true) -> VideoPlatform.DAILYMOTION

            else -> VideoPlatform.OTHER
        }
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
     * Updates download mode (audio or video)
     */
    fun updateDownloadMode(mode: DownloadMode) {
        _uiState.value = _uiState.value.copy(downloadMode = mode)
    }

    /**
     * Validates the current URL and returns authentication data if valid
     * Returns null if URL is invalid
     */
    fun validateAndGetAuthData(): Triple<String, AuthData?, Boolean>? {
        val currentState = _uiState.value
        val url = currentState.urlTextFieldValue.text.trim()

        if (url.isBlank()) {
            _uiState.value = currentState.copy(
                isError = true,
                errorMessage = "Please enter a video URL"
            )
            return null
        }

        if (!isValidUrl(url)) {
            _uiState.value = currentState.copy(
                isError = true,
                errorMessage = "Please enter a valid URL"
            )
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
