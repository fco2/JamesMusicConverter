# Testing Guide - James Music Converter

## Quick Testing URLs

### Direct Video URLs (Will Work)

Use these direct video URLs for testing:

```
# Sample Videos (Small sizes, good for testing)
https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4
https://file-examples.com/storage/fe5b2af28fc1da07ecb5e4a/2017/04/file_example_MP4_480_1_5MG.mp4

# Test Videos from Commons
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4
```

### URLs That Won't Work (Yet)

These require yt-dlp integration (not yet implemented):

```
❌ https://www.youtube.com/watch?v=VIDEO_ID
❌ https://vimeo.com/123456789
❌ https://www.dailymotion.com/video/x12345
```

**Why?** These platforms don't provide direct video URLs. They require:
- yt-dlp for YouTube
- Platform-specific APIs for Vimeo/Dailymotion
- Or a backend service to handle the download

## Testing Steps

### 1. Install the App

```bash
# Build and install
./gradlew installDebug

# Or install from APK
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Grant Permissions

When you first click "Convert to MP3":
- **Android 13+**: No permission needed (automatically uses app-specific storage)
- **Android 10-12**: Grant storage permission when prompted
- **Android 9 or below**: Not supported (minSdk is 29)

### 3. Test Conversion

1. **Enter URL**: Paste a direct video URL (see above)
2. **Click Convert**: Permission dialog may appear
3. **Watch Progress**: Should see:
   - "Connecting to server..."
   - "Downloading video... X MB / Y MB"
   - "Extracting audio..."
   - "Converting to MP3..."
   - "Conversion complete"

### 4. Verify Output

#### Using Android Studio Device File Explorer

1. View → Tool Windows → Device File Explorer
2. Navigate to: `/data/data/com.chuka.jamesmusicconverter/files/`
3. Check directories:
   ```
   Downloads/JamesMusicConverter/  → Downloaded video
   Music/JamesMusicConverter/      → Converted MP3
   ```

#### Using ADB

```bash
# List downloaded files
adb shell ls -lh /data/data/com.chuka.jamesmusicconverter/files/Downloads/JamesMusicConverter/

# List converted files
adb shell ls -lh /data/data/com.chuka.jamesmusicconverter/files/Music/JamesMusicConverter/

# Pull a file to your computer
adb pull /data/data/com.chuka.jamesmusicconverter/files/Music/JamesMusicConverter/converted_*.mp3 ./
```

### 5. Check Logs

```bash
# Real-time logs
adb logcat | grep -E "VideoDownloader|AudioExtractor|ConversionRepository"

# Filter by tag
adb logcat VideoDownloader:D AudioExtractor:D *:S

# Save logs to file
adb logcat > conversion_test.log
```

## Expected Behavior

### Successful Conversion

1. **Download Phase** (0-50%):
   ```
   Connecting to server...
   Downloading video... 1.2 MB / 5.0 MB (24%)
   Downloading video... 2.5 MB / 5.0 MB (50%)
   ...
   Download complete
   ```

2. **Extraction Phase** (50-100%):
   ```
   Extracting audio...
   Converting to MP3...
   Processing audio...
   Finalizing...
   Conversion complete
   ```

3. **Result**:
   - File created in `Music/JamesMusicConverter/`
   - File size > 0 bytes
   - Can play the audio file

### Common Issues

#### Issue: 0-byte File

**Problem**: File created but size is 0 bytes

**Causes**:
1. URL is not a direct video link (e.g., YouTube URL)
2. Server returned HTML instead of video
3. Network error during download

**Check Logs**:
```bash
adb logcat | grep VideoDownloader
```

Look for:
- `Content-Type: video/mp4` (good)
- `Content-Type: text/html` (bad - not a video)
- `Content-Length: 0` (bad - empty response)

**Solution**:
- Use a direct video URL (.mp4, .webm, .mkv)
- Check the error message in the app

#### Issue: "Failed to download: HTTP 403"

**Problem**: Server rejected the request

**Causes**:
- URL requires authentication
- URL expired (common with shared links)
- Server blocks bot/app requests

**Solution**:
- Try a different URL
- Use publicly accessible test videos

#### Issue: "Invalid content length"

**Problem**: Server didn't provide content length

**Causes**:
- Streaming URL (not downloadable file)
- Server doesn't support range requests

**Solution**:
- Use a different URL that supports direct download

#### Issue: Permissions Denied

**Problem**: Can't save files

**Solution**:
```bash
# Check current permissions
adb shell dumpsys package com.chuka.jamesmusicconverter | grep permission

# Grant manually
adb shell pm grant com.chuka.jamesmusicconverter android.permission.READ_EXTERNAL_STORAGE
```

## Testing Simulated Mode

The default implementation uses **simulated conversion**:

1. Enter any URL (validation still required)
2. Click Convert
3. Watch fake progress animation
4. Result: 0-byte placeholder file

**Purpose**: Test UI/UX without actual conversion

## Testing Real Conversion

To test actual conversion, you need either:

### Option 1: Direct URLs (Current)

✅ Works now - just use direct video URLs

### Option 2: YouTube URLs (Requires Setup)

Need to integrate yt-dlp:
1. See [QUICK_START.md](QUICK_START.md#youtube-download-integration)
2. Bundle yt-dlp binary
3. Update VideoDownloader to use yt-dlp

## Performance Testing

### Small Files (Recommended for Testing)

```
# 1-5 MB files - Quick to download
https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4
```

**Expected time**: 5-15 seconds total

### Medium Files

```
# 10-50 MB files
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
```

**Expected time**: 30-60 seconds total

### Large Files (Not Recommended)

```
# 100+ MB files
```

**Issues**:
- Long download time
- High memory usage
- May timeout on slow networks

## Debugging Tips

### Enable Verbose Logging

Add to `VideoDownloader.kt` and `AudioExtractor.kt`:

```kotlin
Log.d(TAG, "Starting download for: $url")
Log.d(TAG, "Content-Type: $contentType")
Log.d(TAG, "Content-Length: $contentLength")
Log.d(TAG, "Downloaded $totalBytesRead bytes")
Log.d(TAG, "File created: ${outputFile.absolutePath} (${outputFile.length()} bytes)")
```

### Monitor Network Traffic

```bash
# Using tcpdump
adb shell tcpdump -i any -s 0 -w - | wireshark -k -i -

# Using Android Studio
Tools → App Inspection → Network Inspector
```

### Check File Integrity

```bash
# Pull file and check with ffprobe
adb pull /data/data/.../file.mp4 ./test.mp4
ffprobe test.mp4

# Or with file command
file test.mp4
```

## Automated Testing

### Unit Tests

Test URL validation:

```kotlin
@Test
fun testValidUrl() {
    val viewModel = UrlInputViewModel()
    assertTrue(viewModel.isValidUrl("https://example.com/video.mp4"))
    assertFalse(viewModel.isValidUrl("not a url"))
}
```

### Integration Tests

Test full conversion flow (requires test video):

```kotlin
@Test
fun testVideoDownload() = runTest {
    val downloader = VideoDownloader(context)
    val testUrl = "https://test.com/video.mp4"

    downloader.downloadVideo(testUrl).collect { progress ->
        assertTrue(progress.progress in 0f..1f)
    }
}
```

## Success Criteria

✅ **Download Test Passed**:
- File size > 0 bytes
- File exists in correct directory
- Content-Type is video/*

✅ **Conversion Test Passed** (Simulated):
- Progress updates received
- Completion reached (100%)
- Output file created

✅ **UI Test Passed**:
- All screens render correctly
- Navigation works
- Error messages display properly

## Next Steps

After basic testing:

1. **Add Real Conversion**: See [QUICK_START.md](QUICK_START.md)
2. **Add YouTube Support**: Integrate yt-dlp
3. **Add Background Service**: For long conversions
4. **Add Notifications**: Progress notifications

## Reporting Issues

When reporting test failures, include:

1. **Device Info**:
   ```bash
   adb shell getprop ro.build.version.release  # Android version
   adb shell getprop ro.product.model           # Device model
   ```

2. **Test URL**: The URL you tried to convert

3. **Logs**:
   ```bash
   adb logcat -d > test_failure.log
   ```

4. **Screenshots**: Of any error messages

5. **File Info**:
   ```bash
   adb shell ls -lh /path/to/file
   ```
