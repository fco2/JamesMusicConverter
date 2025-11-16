# James Music Converter 🎵

A fully functional Android application that downloads videos from YouTube and other platforms and converts them to high-quality MP3 audio files (320kbps).

## ✨ Features

- **🎬 Multi-Platform Support**: YouTube, Vimeo, TikTok, Instagram, Twitter/X, and 1000+ more platforms via yt-dlp
- **🎵 High-Quality MP3**: Converts to 320kbps MP3 using FFmpeg
- **📱 Smart Filename**: Uses actual video title as filename automatically
- **📊 Real-Time Progress**: Live progress tracking from 0-100% during download and conversion
- **🔔 Notifications**: Get notified when conversion completes, tap to play the MP3
- **▶️ Built-in Player**: Play MP3 directly from the app using your default music player
- **📂 File Management**: Open file location, share files with other apps
- **🎨 Modern UI**: Built with Jetpack Compose and Material 3 with white status bar icons
- **🎶 Custom Snackbars**: Beautiful snackbar notifications with music icon for all user feedback
- **🎨 Music-Themed Icon**: Gorgeous blue-to-purple gradient app icon with music notes and sound waves
- **♻️ Multiple Conversions**: Convert multiple videos consecutively without restarting the app
- **⚡ Direct URL Support**: Also supports direct video file URLs (.mp4, .webm, etc.)

## 📸 Screenshots

> **Note**: To add screenshots to this README, take screenshots of the app on your device/emulator and save them in a `screenshots/` folder, then update the image paths below.

<table>
  <tr>
    <td><img src="screenshots/Screenshot%202025-10-17%20at%2011.54.15%E2%80%AFPM.png" alt="URL Input Screen" width="200"/><br/><b>URL Input</b></td>
    <td><img src="screenshots/Screenshot%202025-10-17%20at%2011.55.00%E2%80%AFPM.png" alt="Progress Screen" width="200"/><br/><b>Conversion Progress</b></td>
    <td><img src="screenshots/Screenshot%202025-10-17%20at%2011.56.02%E2%80%AFPM.png" alt="Completed Screen" width="200"/><br/><b>Conversion Complete</b></td>
  </tr>
</table>

### How to capture screenshots:
1. Run the app on your device/emulator
2. Use Android Studio's screenshot tool or device screenshot button
3. Create a `screenshots/` folder in the project root
4. Save images as: `url_input.png`, `progress.png`, `completed.png`, `notification.png`

## 📱 How It Works

### Simple 3-Step Process:

1. **Enter URL**
   - Paste any YouTube, Vimeo, TikTok, or other supported video URL
   - Quick paste button for clipboard content

2. **Watch Progress**
   - Real-time download and conversion progress (0-100%)
   - Status updates: "Fetching video information...", "Downloading audio...", "Converting to MP3..."
   - Beautiful animated circular progress indicator

3. **Enjoy Your MP3**
   - Get notification when complete
   - Play immediately with built-in player integration
   - Share with messaging apps or other devices
   - Access file location for manual management

### Screens Overview

The app includes 4 main screens:

1. **🏠 URL Input Screen**
   - Clean text input with paste from clipboard support
   - URL validation
   - Material 3 design with primary color top bar

2. **⏳ Conversion Progress Screen**
   - Large animated circular progress indicator
   - Real-time percentage display
   - Status message updates
   - Smooth progress animation from 0-100%

3. **✅ Conversion Complete Screen**
   - Success confirmation with checkmark icon
   - File information card showing:
     - Video title
     - File name (uses actual video title)
     - File size (formatted in KB/MB/GB)
   - Action buttons:
     - **Play MP3** - Opens in your music player
     - **Open File Location** - View file in file manager
     - **Share File** - Share via other apps
     - **Convert Another Video** - Return to start

4. **❌ Error Screen**
   - Clear error messages with details
   - Helpful troubleshooting suggestions
   - Try Again button to return to URL input

## 🛠️ Technology Stack

### Core Technologies
- **Language**: Kotlin 2.1.0
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Custom NavDisplay system (type-safe navigation with Kotlin Serialization)
- **Dependency Injection**: Hilt 2.56.2 with KSP 2.1.0-1.0.29
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Android 36
- **Build System**: Gradle 8.13 with Kotlin DSL

### Download & Conversion Libraries
- **yt-dlp Integration**: `youtubedl-android` (v0.18.+) - YouTube and 1000+ platform support
- **FFmpeg**: `youtubedl-android-ffmpeg` (v0.18.+) - Audio format conversion to MP3
- **HTTP Client**: OkHttp 4.12.0 - Direct URL downloads
- **Coroutines**: Kotlin Flow for async progress tracking

### UI & UX
- **Material 3 Components**: TopAppBar, Cards, Buttons, Progress Indicators, Custom Snackbars
- **Compose BOM**: 2025.08.00
- **Extended Material Icons**: Additional icon set with music note icon
- **Custom Snackbar System**: Singleton SnackbarController with music icon for all notifications
- **Music-Themed App Icon**: Vector drawable with gradient background and music notes
- **Coil**: Image loading library (2.7.0)
- **Hilt Navigation Compose**: 1.2.0 for ViewModel injection
- **File Sharing**: AndroidX FileProvider for secure file access

## 🏗️ Architecture

### App Architecture
The app follows modern Android architecture with clean separation of concerns and **Hilt dependency injection**:

#### 1. **Presentation Layer** (`ui/`)
- Jetpack Compose screens with Material 3 components
- Hilt-injected ViewModels using `@HiltViewModel` annotation
- Type-safe navigation with custom NavDisplay system
- ViewModels injected via `hiltViewModel()` in Composables

#### 2. **Domain Layer** (`domain/`)
- `ConversionRepository`: Interface for conversion operations
- `ConversionRepositoryImpl`: Implementation with injected dependencies
- `ConversionProgress`: Data class for progress updates (0-100%)
- `ConversionResult`: Data class for final MP3 output

#### 3. **Data Layer** (`data/service/`)
- `VideoDownloader`: Routes downloads (direct vs platform URLs)
- `YtDlpDownloader`: Handles yt-dlp integration for YouTube/platforms
- `AudioExtractor`: Manages audio file processing
- `DownloadNotificationService`: Notification management
- All services provided as singletons via Hilt

#### 4. **Dependency Injection** (`di/`)
- `AppModule`: Hilt module providing all singleton dependencies
- Application-scoped providers for services and repositories
- Context injection using `@ApplicationContext` qualifier

### Conversion Flow
```
┌─────────────────┐
│   URL Input     │  User enters YouTube/video URL
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ VideoDownloader │  Detects URL type (platform vs direct)
└────────┬────────┘
         │
    ┌────┴────┐
    ▼         ▼
┌─────────┐  ┌──────────┐
│ YtDlp   │  │ OkHttp   │  Platform URLs → yt-dlp
│ (0-80%) │  │ (Direct) │  Direct URLs → OkHttp
└────┬────┘  └────┬─────┘
     │            │
     └──────┬─────┘
            ▼
     ┌──────────────┐
     │ YtDlp FFmpeg │  Convert to MP3 using -x --audio-format mp3
     │   (80-100%)  │  Output: 320kbps MP3 with video title as filename
     └──────┬───────┘
            ▼
     ┌──────────────┐
     │ Notification │  Notify user with tap-to-play action
     │   + Result   │  Store ConversionResult with file details
     └──────────────┘
```

### Navigation Flow
```
UrlInputRoute → ConversionProgressRoute → ConversionCompletedRoute
                                      ↓
                               ConversionErrorRoute
```

### Key Design Patterns
- **Dependency Injection (Hilt)**: All dependencies managed by Hilt for testability and modularity
- **Repository Pattern**: `ConversionRepository` abstracts data sources with constructor injection
- **Flow-based Progress**: Kotlin Flow for reactive progress updates
- **FileProvider**: Secure file sharing between apps
- **Singleton Services**: Reused yt-dlp instance for efficiency via `@Singleton` scope
- **MVVM Architecture**: ViewModels with Hilt injection, no Context in ViewModels

## 🚀 Building the Project

### Prerequisites
- Android Studio Hedgehog or later
- JDK 11 or later
- Android SDK with API 29+ (Android 10+)

### Quick Start

```bash
# Clean build (recommended for first build)
./gradlew clean build

# Install debug version to device/emulator
./gradlew installDebug

# Build without lint (faster for development)
./gradlew assembleDebug -x lint

# Run tests
./gradlew test
```

### Important Notes
- **First build**: May take longer as Hilt annotation processing runs
- **Kotlin version**: 2.1.0 with KSP 2.1.0-1.0.29
- **Gradle version**: 8.13 required
- **Hilt**: Automatic dependency injection setup via annotations

## Project Structure

```
app/src/main/java/com/chuka/jamesmusicconverter/
├── MainActivity.kt                          # Entry point
├── JamesMusicConverterApplication.kt        # Application class
├── navigation/
│   ├── BackstackNavigation.kt              # Custom navigation system
│   ├── Routes.kt                            # Navigation routes
│   └── MusicConverterNavGraph.kt            # Navigation graph
├── ui/
│   ├── urlinput/
│   │   └── UrlInputScreen.kt               # URL entry screen
│   ├── progress/
│   │   └── ConversionProgressScreen.kt     # Progress animation
│   ├── completed/
│   │   └── ConversionCompletedScreen.kt    # Success screen
│   ├── error/
│   │   └── ConversionErrorScreen.kt        # Error screen
│   └── theme/                               # Material 3 theming
└── ...
```

## 📂 Project Structure

```
app/src/main/java/com/chuka/jamesmusicconverter/
├── MainActivity.kt                          # Entry point (@AndroidEntryPoint for Hilt)
├── JamesMusicConverterApplication.kt        # Application class (@HiltAndroidApp)
├── di/
│   └── AppModule.kt                         # Hilt dependency injection module
├── navigation/
│   ├── BackstackNavigation.kt              # Custom navigation system
│   ├── Routes.kt                            # Serializable navigation routes
│   └── MusicConverterNavGraph.kt            # Main navigation graph
├── domain/
│   ├── model/
│   │   ├── ConversionProgress.kt           # Progress data (0-100%)
│   │   └── ConversionResult.kt             # Final MP3 result
│   └── repository/
│       └── ConversionRepository.kt          # Repository interface & implementation
├── data/
│   └── service/
│       ├── VideoDownloader.kt               # URL routing logic (Hilt injected)
│       ├── YtDlpDownloader.kt              # yt-dlp wrapper (Hilt injected)
│       ├── AudioExtractor.kt                # Audio file management (Hilt injected)
│       └── DownloadNotificationService.kt   # Notification handling (Hilt injected)
├── ui/
│   ├── components/
│   │   ├── SnackbarController.kt           # Global snackbar controller (Singleton)
│   │   ├── SnackbarViewModel.kt            # ViewModel for snackbar injection
│   │   └── MusicSnackbar.kt                # Custom snackbar with music icon
│   ├── urlinput/
│   │   ├── UrlInputScreen.kt               # URL entry screen
│   │   └── UrlInputViewModel.kt            # Input validation (@HiltViewModel)
│   ├── progress/
│   │   ├── ConversionProgressScreen.kt     # Animated progress
│   │   └── ConversionProgressViewModel.kt  # Progress state (@HiltViewModel)
│   ├── completed/
│   │   ├── ConversionCompletedScreen.kt    # Success screen
│   │   └── ConversionCompletedViewModel.kt # Play/share actions (@HiltViewModel)
│   ├── error/
│   │   └── ConversionErrorScreen.kt        # Error handling
│   └── theme/
│       ├── Color.kt                         # Material 3 colors
│       ├── Type.kt                          # Typography
│       └── Theme.kt                         # Theme configuration
└── res/
    └── xml/
        └── file_paths.xml                   # FileProvider paths
```

## ✅ Current Implementation Status

This app is **fully functional** and production-ready with yt-dlp integration:

### What's Working:
- ✅ YouTube video downloading and MP3 conversion (320kbps)
- ✅ Support for 1000+ platforms via yt-dlp (Vimeo, TikTok, Instagram, etc.)
- ✅ Real-time progress tracking (0-100%) with proper handling of unknown file sizes
- ✅ Automatic filename using video title
- ✅ Download completion notifications with tap-to-play
- ✅ Play, share, and open file location features
- ✅ Error handling with user-friendly messages
- ✅ Custom snackbar system with music icon for all user feedback
- ✅ Beautiful music-themed app icon with gradient background
- ✅ Multiple consecutive conversions without app restart
- ✅ Proper conversion cancellation with state cleanup
- ✅ Generation-based state management preventing race conditions
- ✅ White status bar icons (works on light/dark themes)
- ✅ FileProvider for secure file sharing

### Key Files:
- `YtDlpDownloader.kt` - Main downloader using yt-dlp library with FFmpeg (fixed negative progress)
- `VideoDownloader.kt` - Smart routing (detects platform URLs vs direct URLs)
- `ConversionRepository.kt` - Orchestrates the entire conversion flow (preserves CancellationException)
- `ConversionProgressViewModel.kt` - Generation-based state management for multiple conversions
- `SnackbarController.kt` - Global snackbar management system
- `MusicSnackbar.kt` - Custom snackbar with music note icon
- `DownloadNotificationService.kt` - Handles completion notifications
- `ic_launcher_foreground.xml` - Music-themed app icon (notes + sound waves)
- `ic_launcher_background.xml` - Blue-to-purple gradient background

### Recent Improvements (Latest Updates):

#### 🎨 UI/UX Enhancements
- **Custom Snackbar System**: Replaced all Android Toasts with beautiful Material 3 snackbars featuring a music note icon
- **Music-Themed App Icon**:
  - Foreground: White music notes with sound waves on transparent background
  - Background: Gorgeous blue (#2575FC) to purple (#6A11CB) gradient with subtle music staff lines
  - Professional, modern design that stands out

#### 🔧 Technical Improvements
- **Generation-Based State Management**: Each conversion is tagged with a generation number, preventing race conditions when starting multiple conversions consecutively
- **Fixed Negative Progress**: yt-dlp reports -1.0% when file size is unknown; now displays "Downloading audio..." without percentage until known
- **Proper Cancellation Handling**:
  - CancellationException is preserved through the repository layer
  - Correct generation tracking prevents old cancelled conversions from interfering with new ones
  - Cancel button always appears during active conversions
- **Multiple Consecutive Conversions**: Users can convert multiple videos back-to-back without restarting the app
- **Race Condition Prevention**: Old coroutine cleanup no longer interferes with new conversion jobs

### Output:
- **Format**: MP3 (320kbps, best quality)
- **Location**: `/storage/emulated/0/Download/JamesMusicConverter/` (Public Downloads folder)
- **Filename**: Actual video title (e.g., "Best Song Ever.mp3")
- **Access**: Files are visible in system file manager and music player apps

### Architecture Highlights:
- **Hilt Dependency Injection**: All services and repositories use constructor injection
- **No Context in ViewModels**: ViewModels are properly scoped and testable
- **Singleton Scope**: Heavy services like yt-dlp are reused across the app
- **Type-Safe Navigation**: Kotlin serialization for route parameters

## 🧪 Testing

### Test with a YouTube URL:
```
https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

Or any other supported platform URL:
- YouTube: `youtube.com/watch?v=...`
- YouTube Shorts: `youtube.com/shorts/...`
- Vimeo: `vimeo.com/...`
- TikTok: `tiktok.com/@.../video/...`
- Twitter/X: `twitter.com/.../status/...`

### Expected Behavior:
1. Enter URL and tap "Start Conversion"
2. Progress shows: "Initializing yt-dlp..." → "Fetching video information..." → "Downloading audio..." → "Audio ready"
3. Notification appears: "Download Complete" with file name
4. Completion screen shows file details with Play button
5. Tap Play to open in your music player

## 🔐 Instagram & Private Content Authentication

### Problem: "Authentication Required" Error

If you get an error like:

```
Instagram Authentication Required
This content requires login...
```

This happens because the content requires login (private accounts, follower-only content, stories,
etc.).

### Quick Solution (2 Minutes)

1. **Login to Instagram in Chrome/Firefox**
    - Open your browser
    - Go to instagram.com
    - Log in normally

2. **Configure the app**
    - Paste your Instagram URL
    - Tap **"Advanced Options"**
    - Enable ☑️ **"Extract cookies from browser"**
    - Type: `chrome` (or `firefox`, `edge`, `safari`)

3. **Download** - Success! ✨

### Supported Browsers

- `chrome` - Google Chrome
- `firefox` - Mozilla Firefox
- `edge` - Microsoft Edge
- `safari` - Safari (Mac)

### Full Documentation

- **Quick Guide**: [`QUICK_FIX_INSTAGRAM_AUTH.md`](QUICK_FIX_INSTAGRAM_AUTH.md)
- **Comprehensive Guide**: [`INSTAGRAM_AUTHENTICATION_GUIDE.md`](INSTAGRAM_AUTHENTICATION_GUIDE.md)
- **Technical Details**: [`INSTAGRAM_AUTH_FIX_SUMMARY.md`](INSTAGRAM_AUTH_FIX_SUMMARY.md)

### Why This Works

The app extracts your browser's Instagram cookies (locally) and uses them to authenticate downloads.
It's:

- ✅ Secure (all local, no data sent anywhere)
- ✅ Works with 2FA
- ✅ No password storage needed
- ✅ Same as using your browser

### Other Platforms

This also works for:

- Twitter/X protected accounts
- Facebook private videos
- TikTok private accounts
- Any platform requiring authentication

---

## 📝 Permissions

The app requires these permissions (automatically requested at runtime):
- `INTERNET` - Download videos from online platforms
- `ACCESS_NETWORK_STATE` - Check network connectivity
- `POST_NOTIFICATIONS` - Show completion notifications (Android 13+)
- `READ_MEDIA_AUDIO` - Access downloaded MP3 files (Android 13+)
- `WRITE_EXTERNAL_STORAGE` - Save files to Downloads folder (API 29-32, maxSdkVersion="32")
- `READ_EXTERNAL_STORAGE` - Access files on Android 10-12 (API 29-32, maxSdkVersion="32")
- `FOREGROUND_SERVICE` - Background download operations
- `FOREGROUND_SERVICE_DATA_SYNC` - Download data syncing

### Permission Scoping
Permissions are properly scoped by Android version:
- **Android 13+** (API 33+): Only requests `POST_NOTIFICATIONS` and `READ_MEDIA_AUDIO`
- **Android 10-12** (API 29-32): Requests storage permissions for public Downloads access
- **Android 9 and below**: Not supported (min SDK 29)

## License

This project is based on the NavDisplay navigation pattern from the NavDisplayDemo project.
