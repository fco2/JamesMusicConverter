# Troubleshooting Guide

## Common Issues and Solutions

### YoutubeDL-Android Initialization Error

**Error Messages:**

```
CANNOT LINK EXECUTABLE "/data/app/.../libffmpeg.so": library "libavdevice.so.61" not found
ERROR: 'NoneType' object has no attribute 'lower'
```

**Cause:**
The `youtubedl-android` library failed to initialize properly due to:

1. Missing native library (ABI) filters in `build.gradle.kts`
2. FFmpeg native libraries not loading correctly
3. Device architecture compatibility issues

**Solution:**

1. **Add ABI Filters** (Already applied):
   ```kotlin
   android {
       defaultConfig {
           ndk {
               abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
           }
       }
   }
   ```

2. **Clean and Rebuild**:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```

3. **Install Fresh**:
    - Uninstall the app from your device/emulator
    - Reinstall using Android Studio or:
      ```bash
      adb uninstall com.chuka.jamesmusicconverter
      ./gradlew installDebug
      ```

4. **Verify extractNativeLibs** (Already set in AndroidManifest.xml):
   ```xml
   <application
       android:extractNativeLibs="true"
       ...>
   ```

**Workaround if Still Failing:**

If the youtubedl-android library continues to fail on your device, you have these options:

1. **Use Direct Video URLs Only**:
    - The app falls back to OkHttp for direct video URLs
    - Works for URLs ending in `.mp4`, `.webm`, etc.
    - No yt-dlp needed for these

2. **Test on Different Device/Emulator**:
    - The library may have compatibility issues with certain devices
    - Try a different Android version or emulator

3. **Check Logcat for Details**:
   ```bash
   adb logcat | grep -E "YtDlpDownloader|VideoDownloader|YoutubeDL"
   ```

**Prevention:**
The app now includes enhanced error handling that:

- Provides clear error messages when yt-dlp fails
- Suggests using direct video URLs as an alternative
- Caches initialization failure to avoid repeated attempts

### Flow Invariant Violation Error

**Error Message:**
```
Flow invariant is violated: flow was collected in StandaloneCoroutine{Active}
but emission happened in DispatchedCoroutine{Active}.
Please refer to flow documentation or use flowOn instead
```

**Cause:**
This error occurs when you emit from a Flow in a different coroutine context than where it was collected. This typically happens when using `withContext()` inside a `flow {}` builder.

**Solution:**
Use `flowOn()` instead of `withContext()` to change the execution context:

❌ **Wrong (causes error):**
```kotlin
fun downloadVideo(url: String): Flow<DownloadProgress> = flow {
    emit(DownloadProgress(0f, "Starting..."))

    withContext(Dispatchers.IO) {  // ❌ Changes context inside flow
        // Network operation
        emit(DownloadProgress(0.5f, "Downloading..."))  // ❌ Error!
    }
}
```

✅ **Correct:**
```kotlin
fun downloadVideo(url: String): Flow<DownloadProgress> = flow {
    emit(DownloadProgress(0f, "Starting..."))

    // Network operation - no withContext needed
    emit(DownloadProgress(0.5f, "Downloading..."))

}.flowOn(Dispatchers.IO)  // ✅ Apply context to entire flow
```

**Files Fixed:**
- `VideoDownloader.kt` - Line 85
- `AudioExtractor.kt` - Lines 80, 122
- `MediaCodecAudioExtractor.kt` - Line 152

### Build Errors

#### FFmpeg Library Not Found

**Error:**
```
Could not find com.arthenica:ffmpeg-kit-full:X.X
```

**Solution:**
FFmpeg libraries were retired from Maven Central in January 2025. Use one of these alternatives:

1. **Simulated conversion** (current default) - No action needed
2. **MediaCodec** - Native Android, no dependencies
3. **FFmpeg binary** - Bundle FFmpeg executable

See [QUICK_START.md](QUICK_START.md) for setup instructions.

#### Kotlin Coroutines Version Conflict

**Error:**
```
Module was compiled with an incompatible version of Kotlin
```

**Solution:**
```bash
# Clean build
./gradlew clean

# Update Kotlin version in gradle/libs.versions.toml
kotlin = "2.0.21"

# Rebuild
./gradlew assembleDebug
```

### Runtime Issues

#### Conversion Never Completes

**Symptoms:**
- Progress stuck at certain percentage
- App not responding
- No error messages

**Possible Causes & Solutions:**

1. **Flow not collected:**
```kotlin
// ❌ Wrong - Flow not collected
viewModel.convertVideo(url)

// ✅ Correct - Collect the flow
viewModel.convertVideo(url).collect { progress ->
    // Handle progress
}
```

2. **Coroutine scope cancelled:**
```kotlin
// Use appropriate scope
viewModelScope.launch {  // ✅ Survives configuration changes
    repository.convert(url).collect { ... }
}
```

3. **Network timeout:**
```kotlin
// Increase OkHttp timeout
val client = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()
```

#### Files Not Found After Conversion

**Issue:**
Converted files created but can't be accessed.

**Solution:**

1. **Check output directory:**
```kotlin
val outputDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
Log.d("FilePath", "Output: ${outputDir?.absolutePath}")
```

2. **Use Device File Explorer:**
   - Android Studio → View → Tool Windows → Device File Explorer
   - Navigate to: `/data/data/com.chuka.jamesmusicconverter/files/Music/`

3. **Storage permissions:**
   - For API 29+, using `getExternalFilesDir()` doesn't require permissions
   - For saving to public directories, add runtime permission requests

#### MediaCodec Format Not Supported

**Error:**
```
No encoder found for audio/mp3
```

**Solution:**
MediaCodec doesn't support MP3 encoding. Use AAC instead:

```kotlin
// Change from:
extractAudioToMp3(input, "output.mp3")

// To:
extractAudioToAAC(input, "output.m4a")
```

Or use FFmpeg implementation for MP3 support.

### UI Issues

#### Progress Not Updating

**Check:**

1. **Flow collection in ViewModel:**
```kotlin
fun startConversion(url: String) {
    viewModelScope.launch {
        repository.convert(url).collect { progress ->
            _progress.value = progress  // ✅ Update state
        }
    }
}
```

2. **State observation in Composable:**
```kotlin
val progress by viewModel.progress.collectAsState()
CircularProgressIndicator(progress = progress.percentage)
```

#### Navigation Not Working

**Check custom navigation:**

```kotlin
// Ensure NavBackStack is initialized
val navBackStack = remember { NavBackStack(UrlInputRoute) }

// Navigate with correct route
navBackStack.push(ConversionProgressRoute(url))
```

### Performance Issues

#### App Slow/Laggy During Conversion

**Solutions:**

1. **Use flowOn for background processing:**
```kotlin
fun convert() = flow {
    // Heavy work here
}.flowOn(Dispatchers.IO)  // ✅ Run on IO thread
```

2. **Reduce progress update frequency:**
```kotlin
if (sampleCount % 100 == 0) {  // Update every 100 samples, not every sample
    emit(progress)
}
```

3. **Use background service for long operations:**
```kotlin
class ConversionService : Service() {
    // Long-running conversion in service
}
```

#### Out of Memory Errors

**Solutions:**

1. **Reduce buffer size:**
```kotlin
// Change from:
val buffer = ByteArray(1024 * 1024)  // 1MB

// To:
val buffer = ByteArray(256 * 1024)   // 256KB
```

2. **Stream processing instead of loading entire file**
3. **Add to AndroidManifest.xml:**
```xml
<application
    android:largeHeap="true"
    ...>
```

### Debugging Tips

#### Enable Detailed Logging

```kotlin
// In your service classes
private val TAG = "VideoDownloader"
Log.d(TAG, "Starting download: $url")
Log.d(TAG, "Progress: $progress")
Log.e(TAG, "Error occurred", exception)
```

#### View Logs

```bash
# Filter by tag
adb logcat | grep VideoDownloader

# Filter by app
adb logcat | grep com.chuka.jamesmusicconverter

# Clear and watch
adb logcat -c && adb logcat
```

#### Inspect Database/Files

```bash
# Access device shell
adb shell

# Navigate to app files
cd /data/data/com.chuka.jamesmusicconverter/files

# List files
ls -la
```

## Still Having Issues?

1. Check [IMPLEMENTATION_NOTES.md](IMPLEMENTATION_NOTES.md) for architecture details
2. Review [QUICK_START.md](QUICK_START.md) for setup instructions
3. Clean and rebuild:
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   ```
4. Check Android Studio's Build output and Logcat for error details

## Reporting Issues

When reporting issues, include:
- Error message (full stack trace)
- Steps to reproduce
- Android version and device
- Relevant code snippets
- Logcat output
