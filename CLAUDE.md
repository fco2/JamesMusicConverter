# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

JamesMusicConverter is an Android application that converts video URLs (like YouTube) to MP3 files. Built with Kotlin and Jetpack Compose using modern Android development practices.

- **Package**: `com.chuka.jamesmusicconverter`
- **Min SDK**: 29 (Android 10)
- **Target SDK**: 36
- **Build System**: Gradle with Kotlin DSL (.kts)
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Custom NavDisplay system (based on NavKey pattern)

## Build Commands

### Building the Project
```bash
./gradlew build
```

### Running Tests
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator/device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests com.chuka.jamesmusicconverter.ExampleUnitTest
```

### Installing to Device/Emulator
```bash
# Debug build
./gradlew installDebug

# Release build
./gradlew installRelease
```

### Cleaning Build
```bash
./gradlew clean
```

### Linting
```bash
./gradlew lint
```

## Architecture

### App Flow
1. **URL Input Screen** (`ui/urlinput/UrlInputScreen.kt`): User enters video URL
2. **Conversion Progress Screen** (`ui/progress/ConversionProgressScreen.kt`): Shows animated progress during conversion
3. **Conversion Completed Screen** (`ui/completed/ConversionCompletedScreen.kt`): Displays result with download option
4. **Error Screen** (`ui/error/ConversionErrorScreen.kt`): Shows error messages if conversion fails

### Source Structure
- **Main source**: `app/src/main/java/com/chuka/jamesmusicconverter/`
- **Navigation**: `navigation/` - Contains NavDisplay system, Routes, and NavGraph
- **UI Screens**: `ui/urlinput/`, `ui/progress/`, `ui/completed/`, `ui/error/`
- **Theme**: `ui/theme/` - Material 3 theming (Color.kt, Type.kt, Theme.kt)
- **Unit tests**: `app/src/test/java/com/chuka/jamesmusicconverter/`
- **Instrumented tests**: `app/src/androidTest/java/com/chuka/jamesmusicconverter/`

### Navigation System
The app uses a custom **NavDisplay** navigation system (not Jetpack Navigation):
- **NavKey**: Sealed interface that all routes implement (using Kotlin serialization)
- **NavBackStack**: Manages navigation backstack with methods like `navigate()`, `navigateUp()`, `popTo()`, `replace()`
- **NavDisplay**: Composable that handles animated transitions and back press
- **Routes**: Defined in `navigation/Routes.kt`
  - `UrlInputRoute` (object)
  - `ConversionProgressRoute(videoUrl: String)`
  - `ConversionCompletedRoute(videoTitle, thumbnailUrl, fileName, fileSize, filePath)`
  - `ConversionErrorRoute(errorMessage: String)`

### Key Dependencies
- **Compose BOM** (2025.08.00): Manages all Compose library versions
- **Kotlin Serialization**: Type-safe navigation with serializable routes
- **youtubedl-android** (v0.18.+): yt-dlp integration for YouTube and 1000+ platforms
- **youtubedl-android-ffmpeg** (v0.18.+): FFmpeg for audio conversion to MP3
- **Coil**: Image loading
- **OkHttp**: HTTP client for direct URL downloads
- **Material Icons Extended**: Additional Material Design icons

### Dependencies Management
Dependencies are managed using Gradle version catalogs in `gradle/libs.versions.toml`:
1. Add version in `[versions]` section
2. Define library in `[libraries]` section
3. Reference in `app/build.gradle.kts` using `libs.` prefix

### Compose Configuration
- Uses Kotlin Compose Compiler Plugin (version 2.0.21)
- Material 3 design system
- Compose BOM version: 2025.08.00
- Extended Material Icons for additional icons

### Java/Kotlin Configuration
- Java 11 compatibility (source and target)
- Kotlin JVM target: 11
- Kotlin Serialization plugin enabled

### Permissions
The app requires these permissions (defined in AndroidManifest.xml):
- `INTERNET`: For fetching video data
- `ACCESS_NETWORK_STATE`: Check network connectivity
- `POST_NOTIFICATIONS`: Show completion notifications (Android 13+)
- `READ_MEDIA_AUDIO`: For accessing converted audio files (Android 13+)
- `WRITE_EXTERNAL_STORAGE`: For devices API 28 and below (maxSdkVersion="32")
- `READ_EXTERNAL_STORAGE`: For devices API 29-32 (maxSdkVersion="32")

### Testing Setup
- Unit tests: JUnit 4.13.2
- UI tests: Compose UI Test (junit4)
- Instrumented tests: AndroidX Test with Espresso

## Implementation Notes

### Current Implementation - FULLY FUNCTIONAL ✅

The app is **production-ready** with real YouTube/platform video downloading and MP3 conversion:

#### What's Implemented:
1. **yt-dlp Integration** (`YtDlpDownloader.kt`)
   - Uses `youtubedl-android` library (v0.18.+) with bundled Python runtime
   - Supports YouTube and 1000+ platforms (Vimeo, TikTok, Instagram, Twitter/X, etc.)
   - Downloads audio with `-x --audio-format mp3 --audio-quality 0` flags
   - Automatic filename using video title template `%(title)s.%(ext)s`
   - First-time initialization (5-10 seconds) - subsequent calls are fast

2. **FFmpeg Audio Conversion** (bundled in youtubedl-android-ffmpeg)
   - Converts downloaded audio to 320kbps MP3
   - Handles all audio formats (Opus, Vorbis, M4A, AAC, etc.)
   - Progress tracking integrated with yt-dlp callback

3. **Smart Download Routing** (`VideoDownloader.kt`)
   - Auto-detects platform URLs (YouTube, Vimeo, etc.) → uses yt-dlp
   - Direct video URLs (.mp4, .webm) → uses OkHttp for faster download
   - Fallback error handling for unsupported platforms

4. **Notification System** (`DownloadNotificationService.kt`)
   - Shows notification when conversion completes
   - Tap notification to play MP3 in default music player
   - Uses FileProvider for secure file URI access
   - Auto-cancels on tap

5. **File Management** (`ConversionCompletedViewModel.kt`)
   - Play MP3 with built-in music player integration
   - Share files with other apps (WhatsApp, Telegram, etc.)
   - Open file location (shows toast if file manager not available)
   - Files saved with actual video title as filename

6. **Progress Tracking** (`ConversionRepository.kt`)
   - Download phase: 0-80% (yt-dlp reports download progress)
   - Conversion phase: 80-100% (FFmpeg audio extraction)
   - Progress normalized with `.coerceAtLeast(0f)` to prevent negatives
   - Real-time status messages throughout

7. **UI Enhancements** (`MainActivity.kt`)
   - White status bar icons (works on light/dark themes)
   - `isAppearanceLightStatusBars = false` for white icons
   - Transparent status bar with edge-to-edge display

#### Architecture Pattern:
```
UI Layer (Compose Screens + ViewModels)
    ↓
Repository Layer (ConversionRepository)
    ↓
Data Layer (VideoDownloader → YtDlpDownloader → FFmpeg)
    ↓
File System + Notifications
```

#### Key Files:
- `YtDlpDownloader.kt` - Main downloader with FFmpeg integration (407 lines)
- `VideoDownloader.kt` - Smart URL routing logic (221 lines)
- `ConversionRepository.kt` - Orchestrates conversion flow with progress (116 lines)
- `DownloadNotificationService.kt` - Handles completion notifications (124 lines)
- `ConversionCompletedViewModel.kt` - Play/share/open file actions
- `MainActivity.kt` - Status bar configuration (white icons)
- `AudioExtractor.kt` - Audio file management (preserves original filename)

#### File Output:
- **Format**: MP3 (320kbps, best quality)
- **Location**: `Android/data/com.chuka.jamesmusicconverter/files/Download/JamesMusicConverter/`
- **Filename**: Actual video title (e.g., "Best Song Ever.mp3" not "converted_12345.mp3")

### Testing
Run the app and test with any YouTube URL:
```
https://www.youtube.com/watch?v=dQw4w9WgXcQ
```

Expected flow:
1. Enter URL → "Start Conversion"
2. Progress: "Initializing yt-dlp..." → "Fetching video information..." → "Downloading audio... X%" → "Audio ready"
3. Notification appears: "Download Complete - {filename}.mp3"
4. Completion screen shows: file name, size, with Play/Share/Open buttons

### Known Issues & Workarounds

1. **Emulator Storage Issues**
   - Problem: `INSTALL_FAILED_INSUFFICIENT_STORAGE`
   - Solution: Always use `./gradlew uninstallDebug && ./gradlew installDebug`

2. **First Launch Slow**
   - Problem: First yt-dlp initialization takes 5-10 seconds
   - Reason: Extracting Python runtime and yt-dlp scripts
   - Solution: Normal behavior, subsequent downloads are fast

3. **Architecture Compatibility**
   - Problem: Some devices may not support yt-dlp (ARM vs x86)
   - Solution: App checks `ytDlpDownloader.isAvailable()` and shows helpful error
   - Fallback: User can still use direct video URLs (OkHttp path)

4. **Deprecated APIs** (warnings only, not errors)
   - `window.statusBarColor` - Still works, just deprecated
   - `Divider` → use `HorizontalDivider` in future
   - `Icons.Filled.ArrowBack` → use `Icons.AutoMirrored.Filled.ArrowBack`
   - `LocalClipboardManager` → use `LocalClipboard` in future

### Future Enhancement Ideas
- Add download history with SQLite/Room
- Support for batch/queue conversions
- Custom output quality selection (128/192/320 kbps)
- Playlist support
- Dark theme toggle
- Progress notification (not just completion)
