# James Music Converter

An Android application that converts video URLs (like YouTube) to MP3 audio files.

## Features

- **URL Input**: Enter any supported video URL to convert
- **Animated Progress**: Beautiful animated progress indicator during conversion
- **Download & Share**: Download converted MP3 files or share them directly
- **Error Handling**: Clear error messages when conversion fails
- **Modern UI**: Built with Jetpack Compose and Material 3

## Screenshots

The app includes 4 main screens:
1. **URL Input Screen** - Enter video URL with paste from clipboard support
2. **Conversion Progress** - Animated circular progress with percentage and status updates
3. **Conversion Complete** - File information display with download and share buttons
4. **Error Screen** - Clear error messages with retry functionality

## Technology Stack

- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose with Material 3
- **Navigation**: Custom NavDisplay system (type-safe navigation with Kotlin Serialization)
- **Min SDK**: Android 10 (API 29)
- **Target SDK**: Android 36

## Architecture

The app follows modern Android architecture:
- **Custom Navigation**: NavDisplay system with backstack management and animated transitions
- **Type-Safe Routes**: Serializable navigation routes using Kotlin Serialization
- **Modular UI**: Separate composable screens for each feature
- **Clean Structure**: Organized packages for navigation, UI screens, and theming

### Navigation Flow
```
UrlInputRoute → ConversionProgressRoute → ConversionCompletedRoute
                                      ↓
                               ConversionErrorRoute
```

## Building the Project

```bash
# Build the app
./gradlew build

# Install debug version to device/emulator
./gradlew installDebug

# Run tests
./gradlew test
```

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

## Current Implementation

**Note**: The video conversion is currently **simulated** for demonstration purposes. However, the app includes **three production-ready conversion implementations**:

### 🎯 1. Simulated (Default - Demo Only)
- Shows UI/UX flow without actual conversion
- Perfect for testing and demonstration
- **Currently active** - just build and run!

### 📱 2. MediaCodec (Native Android)
- Uses Android's built-in MediaCodec API
- **Output**: AAC/M4A format (MP3 not supported)
- **Pros**: No dependencies, small APK, hardware acceleration
- **Best for**: Quick production deployment with AAC output

### 🎵 3. FFmpeg (Full Features)
- Invokes FFmpeg binary as external process
- **Output**: MP3 and all formats
- **Pros**: Industry standard, all codecs and formats
- **Requires**: Bundling FFmpeg binary for Android

See **[QUICK_START.md](QUICK_START.md)** for step-by-step setup of each option.
See **[IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)** for detailed comparison and architecture.

### Files Included
- `AudioExtractor.kt` - Simulated (current)
- `MediaCodecAudioExtractor.kt` - Native Android implementation
- `FFmpegExecutor.kt` - FFmpeg process execution
- `VideoDownloader.kt` - Smart downloader (auto-detects direct URL vs platform)
- `YtDlpDownloader.kt` - YouTube/Vimeo/TikTok downloader via yt-dlp

## Dependencies

Key dependencies managed via Gradle version catalogs:
- Jetpack Compose BOM (2025.08.00)
- Kotlin Serialization (1.8.1)
- Coil for image loading (2.7.0)
- OkHttp for networking (4.12.0)
- Material Icons Extended

## Documentation

- **[QUICK_START.md](QUICK_START.md)** - Choose and set up your conversion implementation
- **[YT_DLP_SETUP.md](YT_DLP_SETUP.md)** - Setup yt-dlp for YouTube/platform downloads
- **[TESTING_GUIDE.md](TESTING_GUIDE.md)** - Test URLs and verification steps
- **[IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md)** - Architecture details and production notes
- **[TROUBLESHOOTING.md](TROUBLESHOOTING.md)** - Common errors and solutions

## License

This project is based on the NavDisplay navigation pattern from the NavDisplayDemo project.
