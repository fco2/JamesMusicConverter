# yt-dlp Setup Guide for Android

## What is yt-dlp?

yt-dlp is a command-line program to download videos from YouTube and 1000+ other sites. It's a fork of youtube-dl with additional features, better performance, and active maintenance.

**Supported platforms**: YouTube, Vimeo, Dailymotion, Twitch, Facebook, Instagram, TikTok, Twitter/X, and many more.

## Why Use yt-dlp?

The app now **automatically detects** if a URL needs yt-dlp:

- **YouTube URLs** → Uses yt-dlp
- **Vimeo, TikTok, etc.** → Uses yt-dlp
- **Direct video URLs** (.mp4, .webm) → Uses OkHttp (no yt-dlp needed)

## Setup Options

### Option 1: Build yt-dlp for Android (Recommended for Production)

yt-dlp is a Python program that needs to be packaged for Android.

#### Using Termux (Easiest)

1. **Install Termux** on Android device or emulator
2. **Install yt-dlp**:
   ```bash
   pkg install python
   pip install yt-dlp
   ```

3. **Create standalone binary** (using PyInstaller):
   ```bash
   pkg install binutils
   pip install pyinstaller
   pyinstaller --onefile $(which yt-dlp)
   ```

4. **Copy binary**:
   ```bash
   # Binary will be in dist/yt-dlp
   cp dist/yt-dlp /sdcard/Download/
   ```

5. **Add to Android project**:
   ```
   app/src/main/assets/
     └── yt-dlp  (the binary from Termux)
   ```

#### Using Docker (For CI/CD)

```dockerfile
FROM python:3.11-slim
RUN pip install yt-dlp pyinstaller
RUN pyinstaller --onefile /usr/local/bin/yt-dlp
# Binary in /dist/yt-dlp
```

### Option 2: Use Python for Android (Chaquopy)

Instead of bundling a binary, embed Python runtime in your app.

#### Step 1: Add Chaquopy Plugin

**build.gradle.kts (project level)**:
```kotlin
plugins {
    id("com.chaquo.python") version "15.0.1" apply false
}
```

**build.gradle.kts (app level)**:
```kotlin
plugins {
    id("com.chaquo.python")
}

chaquopy {
    defaultConfig {
        version = "3.11"
        pip {
            install("yt-dlp")
        }
    }
}
```

#### Step 2: Use Python from Kotlin

```kotlin
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

class YtDlpPythonWrapper(context: Context) {
    init {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    fun downloadVideo(url: String, outputPath: String) {
        val py = Python.getInstance()
        val ytdlp = py.getModule("yt_dlp")

        // Call yt-dlp from Python
        val opts = mapOf(
            "outtmpl" to outputPath,
            "format" to "best"
        )

        ytdlp.callAttr("download", url, opts)
    }
}
```

**Pros**:
- Easy to update yt-dlp
- Access to full Python ecosystem

**Cons**:
- Larger APK size (+50-80MB)
- Python runtime overhead

### Option 3: Download Binary at Runtime

Download yt-dlp binary on first app launch.

```kotlin
suspend fun downloadYtDlp() {
    val url = "https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux"
    val outputFile = File(context.filesDir, "yt-dlp")

    // Download binary
    val client = OkHttpClient()
    val request = Request.Builder().url(url).build()
    client.newCall(request).execute().use { response ->
        outputFile.outputStream().use { output ->
            response.body?.byteStream()?.copyTo(output)
        }
    }

    // Make executable
    outputFile.setExecutable(true)
}
```

**Pros**:
- Smaller initial APK
- Easy to update

**Cons**:
- Requires internet on first launch
- Might not work on all Android versions

### Option 4: Use yt-dlp API Service (Backend)

Run yt-dlp on your own server and provide an API.

#### Server (Node.js example):

```javascript
const express = require('express');
const ytdlp = require('yt-dlp-exec');

app.post('/download', async (req, res) => {
    const { url } = req.body;

    const info = await ytdlp(url, {
        dumpSingleJson: true,
        noWarnings: true
    });

    res.json({
        title: info.title,
        duration: info.duration,
        formats: info.formats
    });
});
```

#### Android App:

```kotlin
suspend fun getVideoInfo(url: String): VideoInfo {
    val response = httpClient.post("https://yourserver.com/download") {
        setBody(mapOf("url" to url))
    }
    return response.body()
}
```

**Pros**:
- No app-side complexity
- Centralized updates
- Can add features like caching

**Cons**:
- Requires server hosting
- API costs and maintenance
- Potential rate limiting

## Current Implementation

The app includes `YtDlpDownloader.kt` which uses **ProcessBuilder** to execute yt-dlp binary.

### How It Works

1. **Check if yt-dlp needed**:
   ```kotlin
   if (url.contains("youtube.com")) {
       // Use yt-dlp
   } else {
       // Use direct download
   }
   ```

2. **Execute yt-dlp**:
   ```kotlin
   val command = listOf(
       "/path/to/yt-dlp",
       url,
       "-o", outputPath,
       "-f", "bestvideo+bestaudio/best"
   )
   ProcessBuilder(command).start()
   ```

3. **Parse progress**:
   ```kotlin
   // yt-dlp outputs: [download]  50.0% of 10.00MiB at 1.00MiB/s
   val progress = parseProgress(output)
   emit(DownloadProgress(progress, "Downloading..."))
   ```

## Testing yt-dlp Integration

### 1. Check if yt-dlp is available

```kotlin
val downloader = YtDlpDownloader(context)
if (downloader.isAvailable()) {
    Log.d("Test", "yt-dlp is ready")
    val version = downloader.getVersion()
    Log.d("Test", "yt-dlp version: $version")
} else {
    Log.e("Test", "yt-dlp not found")
}
```

### 2. Test with YouTube URL

```kotlin
val url = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

downloader.downloadVideo(url).collect { progress ->
    Log.d("Test", "Progress: ${progress.progress * 100}%")
    Log.d("Test", "Message: ${progress.message}")
}
```

### 3. Test with audio-only

```kotlin
// Faster download, smaller file
downloader.downloadAudioOnly(url).collect { progress ->
    // Progress updates
}
```

## Troubleshooting

### Binary Not Found

**Error**: "yt-dlp binary not found"

**Check**:
```bash
# Using ADB
adb shell ls -l /data/data/com.chuka.jamesmusicconverter/files/yt-dlp

# Should show:
# -rwxr-xr-x 1 u0_a123 u0_a123 12345678 2025-01-15 yt-dlp
```

**Fix**:
- Ensure binary is in `app/src/main/assets/yt-dlp`
- Check file permissions (should be executable)
- Verify binary is for correct architecture

### Permission Denied

**Error**: "java.io.IOException: Cannot run program"

**Fix**:
```kotlin
val ytdlpFile = File(context.filesDir, "yt-dlp")
ytdlpFile.setExecutable(true, false)  // Make executable
ytdlpFile.setReadable(true, false)
```

### Wrong Architecture

**Error**: "Exec format error"

**Cause**: Binary built for wrong CPU architecture

**Fix**:
- Build for all architectures: arm64-v8a, armeabi-v7a, x86, x86_64
- Or detect architecture at runtime:
  ```kotlin
  val abi = Build.SUPPORTED_ABIS[0]  // "arm64-v8a", etc.
  val binaryName = "yt-dlp-$abi"
  ```

### Python Not Found

**Error**: "/usr/bin/python: not found"

**Cause**: yt-dlp requires Python but it's not in PATH

**Fix**:
- Use standalone binary (PyInstaller)
- Or bundle Python with Chaquopy

### SSL/Certificate Errors

**Error**: "SSL: CERTIFICATE_VERIFY_FAILED"

**Fix**:
Add to yt-dlp command:
```kotlin
"--no-check-certificate"  // Not recommended for production
```

Or update certificates:
```kotlin
"--update"  // Update yt-dlp to latest version
```

## Recommended Setup

For **production**:

1. Use **Option 2 (Chaquopy)** if APK size isn't critical
2. Or **Option 4 (API Service)** for best user experience

For **development/testing**:

1. Use **Option 3 (Runtime Download)** for quick testing
2. Download once and reuse

## File Size Comparison

| Method            | APK Size Increase       |
|-------------------|-------------------------|
| Standalone binary | +15-25 MB               |
| Chaquopy (Python) | +50-80 MB               |
| Runtime download  | 0 MB (downloads ~15 MB) |
| API Service       | 0 MB                    |

## Performance

| Method           | Speed             | Reliability       |
|------------------|-------------------|-------------------|
| Local binary     | Fast              | Offline           |
| Chaquopy         | Medium            | Offline           |
| Runtime download | Fast              | Requires setup    |
| API Service      | Depends on server | Requires internet |

## Next Steps

1. **Choose setup method** based on your needs
2. **Bundle yt-dlp binary** or configure Chaquopy
3. **Test with YouTube URL**
4. **Handle errors gracefully**
5. **Add progress notifications**

## Resources

- **yt-dlp GitHub**: https://github.com/yt-dlp/yt-dlp
- **Chaquopy**: https://chaquo.com/chaquopy/
- **PyInstaller**: https://pyinstaller.org/
- **Supported Sites**: https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md

## Legal Notice

Be aware of:
- YouTube Terms of Service
- Copyright laws in your jurisdiction
- Platform-specific restrictions

Always respect content creators' rights and platform policies.
