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
- **Coil**: Image loading (for future thumbnail support)
- **OkHttp**: HTTP client for network requests
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
- `WRITE_EXTERNAL_STORAGE`: For devices API 28 and below
- `READ_MEDIA_AUDIO`: For accessing converted audio files

### Testing Setup
- Unit tests: JUnit 4.13.2
- UI tests: Compose UI Test (junit4)
- Instrumented tests: AndroidX Test with Espresso

## Implementation Notes

### Current Conversion Logic
The conversion progress is currently **simulated** in `ConversionProgressScreen.kt`. In a real implementation, you would:
1. Integrate a library like `yt-dlp` or use a backend service
2. Handle actual video download and audio extraction
3. Implement proper file storage using Android's storage APIs
4. Handle runtime permissions for storage access

### Future Enhancements
- Implement actual video-to-MP3 conversion using external libraries
- Add ViewModel layer for business logic separation
- Implement proper error handling for different error types
- Add file management and history features
- Support for batch conversions
