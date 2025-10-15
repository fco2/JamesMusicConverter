# Quick Start Guide - James Music Converter

## Choose Your Implementation

The app currently has **three conversion implementations**. Choose based on your requirements:

### 1. 🎯 Simulated (Current - Demo Only)
**Location**: `AudioExtractor.kt` (current implementation)

**Use for**: Testing UI/UX flow without actual conversion

**How to use**: Already active - just build and run!

```bash
./gradlew installDebug
```

### 2. 📱 MediaCodec (Recommended for Quick Start)
**Location**: `MediaCodecAudioExtractor.kt`

**Use for**: Production app with AAC/M4A output (no MP3)

**Pros**: No external dependencies, small APK size, uses Android native APIs

**How to enable**:

1. Open `ConversionRepository.kt`
2. Replace `AudioExtractor` with `MediaCodecAudioExtractor`:

```kotlin
class ConversionRepositoryImpl(
    private val context: Context
) : ConversionRepository {
    private val videoDownloader = VideoDownloader(context)
    private val audioExtractor = MediaCodecAudioExtractor(context)  // ← Change this

    // ... rest of implementation
}
```

3. Update the extraction call:
```kotlin
// Change from:
audioExtractor.extractAudioToMp3(downloadedFilePath!!)

// To:
audioExtractor.extractAudioToAAC(downloadedFilePath!!)
```

4. Build and run:
```bash
./gradlew assembleDebug
```

**Output**: Files will be saved as `.m4a` (AAC audio) instead of `.mp3`

### 3. 🎵 FFmpeg Process (Full MP3 Support)
**Location**: `FFmpegExecutor.kt`

**Use for**: Production app requiring MP3 output

**Requires**: FFmpeg binary for Android

**How to enable**:

#### Step 1: Get FFmpeg Binary

**Option A - Build from source**:
```bash
git clone https://github.com/arthenica/ffmpeg-kit
cd ffmpeg-kit/android
./android.sh --enable-gpl --enable-libmp3lame
```

**Option B - Download prebuilt** (easier):
- Download from: https://github.com/arthenica/ffmpeg-kit/releases
- Extract `ffmpeg` binary for your architecture

#### Step 2: Add to Project

Place the FFmpeg binary in one of these locations:

**Method 1 - Assets** (recommended):
```
app/src/main/assets/
  └── ffmpeg  (executable binary)
```

**Method 2 - jniLibs**:
```
app/src/main/jniLibs/
  ├── arm64-v8a/libffmpeg.so
  ├── armeabi-v7a/libffmpeg.so
  ├── x86/libffmpeg.so
  └── x86_64/libffmpeg.so
```

#### Step 3: Update AudioExtractor

Replace the simulated implementation in `AudioExtractor.kt`:

```kotlin
class AudioExtractor(private val context: Context) {
    private val ffmpegExecutor = FFmpegExecutor(context)

    fun extractAudioToMp3(
        videoFilePath: String,
        outputFileName: String = "converted_${System.currentTimeMillis()}.mp3"
    ): Flow<ExtractionProgress> = flow {
        emit(ExtractionProgress(0f, "Preparing audio extraction..."))

        try {
            val musicDir = File(
                context.getExternalFilesDir(Environment.DIRECTORY_MUSIC),
                "JamesMusicConverter"
            )
            musicDir.mkdirs()
            val outputFile = File(musicDir, outputFileName)

            emit(ExtractionProgress(0.1f, "Extracting audio..."))

            val exitCode = ffmpegExecutor.execute(
                arrayOf(
                    "-i", videoFilePath,
                    "-vn",
                    "-codec:a", "libmp3lame",
                    "-q:a", "2",
                    "-y", outputFile.absolutePath
                )
            ) { progress ->
                Log.d("AudioExtractor", progress)
                // Parse FFmpeg output for progress percentage
            }

            if (exitCode == 0) {
                emit(ExtractionProgress(0.9f, "Finalizing..."))
                File(videoFilePath).delete()
                emit(ExtractionProgress(1f, "Conversion complete", outputFile.absolutePath, outputFile.length()))
            } else {
                throw Exception("FFmpeg failed with exit code: $exitCode")
            }
        } catch (e: Exception) {
            throw Exception("Audio extraction failed: ${e.message}")
        }
    }
}
```

#### Step 4: Build and Run
```bash
./gradlew assembleDebug
```

---

## Testing the App

### 1. Test with Direct Video URL

Use a direct link to a video file:
```
https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4
```

### 2. Test with Local File

For testing without network:
1. Place a video in your device storage
2. Modify `VideoDownloader.kt` to accept local file paths
3. Or use: `file:///sdcard/Download/test_video.mp4`

### 3. Check Output

Files are saved to:
```
/data/data/com.chuka.jamesmusicconverter/files/Music/JamesMusicConverter/
```

Use Android Studio's Device File Explorer to view:
- View → Tool Windows → Device File Explorer
- Navigate to the path above

---

## Common Issues

### FFmpeg Binary Not Found

**Error**: "FFmpeg binary not found"

**Solution**:
1. Check binary is in `assets/` or `jniLibs/`
2. Verify it's executable: `File.setExecutable(true)`
3. Check logs: `adb logcat | grep FFmpegExecutor`

### MediaCodec Format Not Supported

**Error**: "No encoder found for audio/mp3"

**Solution**: MediaCodec doesn't support MP3 encoding. Use AAC instead:
```kotlin
extractAudioToAAC(input, "output.m4a")
```

### Out of Memory

**Error**: OOM during conversion

**Solution**:
- Use streaming instead of loading entire file
- Reduce buffer sizes in MediaCodec implementation
- Process in background service

### YouTube URLs Don't Work

**Current**: Only direct video URLs are supported

**Solution**: Integrate yt-dlp (see IMPLEMENTATION_NOTES.md)

---

## Next Steps

1. **Choose implementation** based on your requirements
2. **Test with sample videos**
3. **Add error handling** for production
4. **Implement background service** for long conversions
5. **Add notifications** for completion
6. **Integrate yt-dlp** for YouTube support

For detailed architecture and production notes, see `IMPLEMENTATION_NOTES.md`.
