package com.chuka.jamesmusicconverter.ui.urlinput

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.chuka.jamesmusicconverter.data.service.YtDlpDownloader
import com.chuka.jamesmusicconverter.domain.repository.DownloadQueueRepository
import com.chuka.jamesmusicconverter.navigation.DownloadMode
import com.chuka.jamesmusicconverter.worker.DownloadWorkManager
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
    val previewError: String? = null,
    val activeQueueCount: Int = 0,
    val addedToQueue: Boolean = false,
    val isPlaylist: Boolean = false,
    val playlistVideoCount: Int = 0,
    val playlistVideos: List<com.chuka.jamesmusicconverter.data.service.PlaylistVideoInfo> = emptyList()
)

@HiltViewModel
class UrlInputViewModel @Inject constructor(
    private val ytDlpDownloader: YtDlpDownloader,
    private val queueRepository: DownloadQueueRepository,
    private val downloadWorkManager: DownloadWorkManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(UrlInputUiState())
    val uiState: StateFlow<UrlInputUiState> = _uiState.asStateFlow()

    private var previewJob: Job? = null

    init {
        // Observe active queue count
        viewModelScope.launch {
            queueRepository.getActiveCountFlow().collect { count ->
                _uiState.value = _uiState.value.copy(activeQueueCount = count)
            }
        }
    }

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
            isLoadingPreview = false,
            isPlaylist = false,
            playlistVideoCount = 0,
            playlistVideos = emptyList()
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
     * Automatically detects playlists and carousels
     */
    private fun fetchVideoPreview(url: String) {
        // Cancel any existing preview fetch
        previewJob?.cancel()

        previewJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoadingPreview = true,
                previewError = null,
                isPlaylist = false,
                playlistVideoCount = 0,
                playlistVideos = emptyList()
            )

            try {
                // First, check if it's a playlist/carousel
                val isPlaylist = ytDlpDownloader.isPlaylist(url)
                
                if (isPlaylist) {
                    // Fetch playlist info
                    val playlistInfo = ytDlpDownloader.getPlaylistInfo(url)
                    
                    if (playlistInfo != null && playlistInfo.videoCount > 1) {
                        // Multiple videos found - show playlist preview
                        val platform = detectPlatform(url)
                        val firstVideo = playlistInfo.videos.firstOrNull()
                        
                        val preview = VideoPreview(
                            title = playlistInfo.title,
                            thumbnail = firstVideo?.thumbnail,
                            duration = firstVideo?.duration ?: 0L,
                            uploader = null,
                            platform = platform
                        )

                        _uiState.value = _uiState.value.copy(
                            videoPreview = preview,
                            isLoadingPreview = false,
                            previewError = null,
                            isPlaylist = true,
                            playlistVideoCount = playlistInfo.videoCount,
                            playlistVideos = playlistInfo.videos
                        )
                    } else {
                        // Single video, fall back to normal preview
                        fetchSingleVideoPreview(url)
                    }
                } else {
                    // Single video
                    fetchSingleVideoPreview(url)
                }
            } catch (e: Exception) {
                android.util.Log.e("UrlInputViewModel", "Error fetching video preview", e)
                _uiState.value = _uiState.value.copy(
                    videoPreview = null,
                    isLoadingPreview = false,
                    previewError = null,  // Don't show error, just silently fail
                    isPlaylist = false,
                    playlistVideoCount = 0,
                    playlistVideos = emptyList()
                )
            }
        }
    }

    /**
     * Fetch preview for a single video
     */
    private suspend fun fetchSingleVideoPreview(url: String) {
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
                    previewError = null,
                    isPlaylist = false,
                    playlistVideoCount = 0,
                    playlistVideos = emptyList()
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    videoPreview = null,
                    isLoadingPreview = false,
                    previewError = "Could not fetch video information",
                    isPlaylist = false,
                    playlistVideoCount = 0,
                    playlistVideos = emptyList()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("UrlInputViewModel", "Error fetching single video preview", e)
            _uiState.value = _uiState.value.copy(
                videoPreview = null,
                isLoadingPreview = false,
                previewError = null,
                isPlaylist = false,
                playlistVideoCount = 0,
                playlistVideos = emptyList()
            )
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

    /**
     * Add current URL to download queue (single video)
     * Returns true if added successfully, false otherwise
     */
    fun addToQueue(): Boolean {
        val currentState = _uiState.value
        val url = currentState.urlTextFieldValue.text.trim()

        if (url.isBlank() || !isValidUrl(url)) {
            _uiState.value = currentState.copy(
                isError = true,
                errorMessage = "Please enter a valid URL"
            )
            return false
        }

        viewModelScope.launch {
            // Check if URL is already in queue
            if (queueRepository.isUrlInActiveQueue(url)) {
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = "This URL is already in the queue"
                )
                return@launch
            }

            val preview = currentState.videoPreview
            val platform = preview?.platform?.getDisplayName() ?: detectPlatform(url).getDisplayName()

            // Add to queue database (single video, not part of carousel)
            val queueId = queueRepository.addToQueue(
                url = url,
                title = preview?.title,
                thumbnailUrl = preview?.thumbnail,
                localThumbnailPath = null,
                platform = platform,
                downloadMode = currentState.downloadMode,
                durationMillis = (preview?.duration ?: 0L) * 1000,  // Convert seconds to millis
                isPartOfCarousel = false,
                carouselGroupId = null,
                carouselPosition = null,
                carouselTotal = null
            )

            // Get auth data
            val authData = if (currentState.username.isNotBlank() ||
                currentState.password.isNotBlank() ||
                (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())
            ) {
                AuthData(
                    username = currentState.username.ifBlank { null },
                    password = currentState.password.ifBlank { null },
                    browser = if (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())
                        currentState.selectedBrowser else null
                )
            } else null

            // Enqueue WorkManager job
            downloadWorkManager.enqueueDownload(
                queueId = queueId,
                url = url,
                downloadMode = currentState.downloadMode,
                username = authData?.username,
                password = authData?.password,
                cookiesFromBrowser = authData?.browser
            )

            // Update state to show success
            _uiState.value = _uiState.value.copy(
                addedToQueue = true
            )
        }

        return true
    }

    /**
     * Reset the addedToQueue flag
     */
    fun resetAddedToQueue() {
        _uiState.value = _uiState.value.copy(addedToQueue = false)
    }

    /**
     * Add all videos from a playlist/carousel to the download queue
     * Returns true if added successfully, false otherwise
     */
    fun addAllToQueue(): Boolean {
        val currentState = _uiState.value

        if (!currentState.isPlaylist || currentState.playlistVideos.isEmpty()) {
            // Not a playlist or no videos - fall back to single video add
            return addToQueue()
        }

        viewModelScope.launch {
            try {
                val platform = currentState.videoPreview?.platform?.getDisplayName()
                    ?: detectPlatform(currentState.urlTextFieldValue.text.trim()).getDisplayName()

                // Get auth data
                val authData = if (currentState.username.isNotBlank() ||
                    currentState.password.isNotBlank() ||
                    (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())
                ) {
                    AuthData(
                        username = currentState.username.ifBlank { null },
                        password = currentState.password.ifBlank { null },
                        browser = if (currentState.useBrowserCookies && currentState.selectedBrowser.isNotBlank())
                            currentState.selectedBrowser else null
                    )
                } else null

                // Generate a unique carousel group ID for this batch
                val carouselGroupId = java.util.UUID.randomUUID().toString()
                val totalVideos = currentState.playlistVideos.size

                // Add each video to the queue with carousel info
                var addedCount = 0
                currentState.playlistVideos.forEachIndexed { index, video ->
                    // Check if URL is already in queue
                    if (!queueRepository.isUrlInActiveQueue(video.url)) {
                        val queueId = queueRepository.addToQueue(
                            url = video.url,
                            title = video.title,
                            thumbnailUrl = video.thumbnail,
                            localThumbnailPath = null,
                            platform = platform,
                            downloadMode = currentState.downloadMode,
                            durationMillis = video.duration * 1000,  // Convert seconds to millis
                            isPartOfCarousel = true,
                            carouselGroupId = carouselGroupId,
                            carouselPosition = index + 1,  // 1-indexed position
                            carouselTotal = totalVideos
                        )

                        // Enqueue WorkManager job
                        downloadWorkManager.enqueueDownload(
                            queueId = queueId,
                            url = video.url,
                            downloadMode = currentState.downloadMode,
                            username = authData?.username,
                            password = authData?.password,
                            cookiesFromBrowser = authData?.browser
                        )

                        addedCount++
                    }
                }

                android.util.Log.d("UrlInputViewModel", "Added $addedCount/$totalVideos videos to queue (carousel: $carouselGroupId)")

                // Update state to show success
                _uiState.value = _uiState.value.copy(
                    addedToQueue = true
                )
            } catch (e: Exception) {
                android.util.Log.e("UrlInputViewModel", "Error adding playlist to queue", e)
                _uiState.value = _uiState.value.copy(
                    isError = true,
                    errorMessage = "Failed to add videos to queue: ${e.message}"
                )
            }
        }

        return true
    }

    /**
     * Smart download function that handles routing based on content type:
     * - Single video: Returns true to proceed with direct download via progress screen
     * - Playlist (2+ videos): Auto-queues all, returns false (caller should navigate to queue)
     *
     * @return true if single video (proceed to progress), false if playlist (queued, go to queue)
     */
    fun performSmartDownload(): Boolean {
        val currentState = _uiState.value

        // For playlists with 2+ videos, auto-queue and return false
        if (currentState.isPlaylist && currentState.playlistVideoCount > 1) {
            addAllToQueue()
            return false  // Signal caller to navigate to queue
        }

        // Single video - return true to proceed with direct download
        return true
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
