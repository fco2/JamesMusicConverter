# Test YouTube Downloads - Quick Guide

## ✅ Setup Complete!

yt-dlp binary has been installed in your app. You can now download from YouTube and other platforms!

## 🎬 Test URLs

### YouTube (Short Videos - Good for Testing)

```
# Rick Astley - Never Gonna Give You Up (3:33)
https://www.youtube.com/watch?v=dQw4w9WgXcQ

# Big Buck Bunny - Animated Short (10:00)
https://www.youtube.com/watch?v=aqz-KE-bpKQ

# YouTube Test Video (Short)
https://www.youtube.com/watch?v=jNQXAC9IVRw

https://www.youtube.com/watch?v=_7GnilUOUhw&list=RD_7GnilUOUhw&start_radio=1

https://www.instagram.com/p/DP4A-NaDnKn/?utm_source=ig_web_copy_link
```

### Vimeo

```
# Staff Pick Videos
https://vimeo.com/148751763
https://vimeo.com/336892869
```

### Direct URLs (Don't Need yt-dlp)

```
# Sample Videos - Instant download
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
https://sample-videos.com/video123/mp4/720/big_buck_bunny_720p_1mb.mp4
```

## 📱 How to Test

### 1. Install the App

```bash
# Install on connected device/emulator
./gradlew installDebug

# Or manually install
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 2. Test YouTube URL

1. **Open app**
2. **Enter YouTube URL**: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
3. **Click "Convert to MP3"**
4. **Watch progress**:
   - "Initializing yt-dlp..."
   - "Fetching video information..."
   - "Downloading... 25%"
   - "Downloading... 50%"
   - "Download complete"
   - "Extracting audio..."
   - "Conversion complete!"

### 3. Verify Download

#### Using ADB:

```bash
# Check if yt-dlp was extracted
adb shell ls -lh /data/data/com.chuka.jamesmusicconverter/files/yt-dlp

# Check downloaded video
adb shell ls -lh /data/data/com.chuka.jamesmusicconverter/files/Downloads/JamesMusicConverter/

# Check converted MP3
adb shell ls -lh /data/data/com.chuka.jamesmusicconverter/files/Music/JamesMusicConverter/
```

#### Using Android Studio:

1. **View → Tool Windows → Device File Explorer**
2. Navigate to: `/data/data/com.chuka.jamesmusicconverter/files/`
3. Check folders:
   - `Downloads/JamesMusicConverter/` - Downloaded video
   - `Music/JamesMusicConverter/` - Converted MP3

## 📊 Expected Behavior

### First Run (yt-dlp extraction)

```
App starts
  ↓
User enters YouTube URL
  ↓
App detects it needs yt-dlp
  ↓
Checks if yt-dlp exists in /data/data/.../files/yt-dlp
  ↓
Not found - extracts from assets
  ↓
Makes it executable
  ↓
Proceeds with download
```

**Time**: First run adds ~2-3 seconds for extraction

### Subsequent Runs

```
App starts
  ↓
User enters YouTube URL
  ↓
yt-dlp already extracted - use immediately
  ↓
Download starts
```

**Time**: Instant (no extraction needed)

## 🎯 Success Indicators

### In Logcat

```bash
adb logcat | grep -E "VideoDownloader|YtDlpDownloader"
```

**Look for**:

```
VideoDownloader: Using yt-dlp for: https://www.youtube.com/...
YtDlpDownloader: yt-dlp extracted to: /data/data/.../files/yt-dlp
YtDlpDownloader: Executing yt-dlp: /data/data/.../files/yt-dlp [url] -o [output]
YtDlpDownloader: [download]  50.0% of 10.00MiB at 1.00MiB/s ETA 00:05
YtDlpDownloader: Download completed successfully
```

### In UI

- Progress bar updates smoothly
- Percentage shows: 0% → 10% → 25% → 50% → 75% → 100%
- Messages change appropriately
- File appears in completed screen

## 🐛 Troubleshooting

### Error: "yt-dlp binary not found"

**Check**:
```bash
ls -lh app/src/main/assets/yt-dlp
```

**Should show**: ~36MB file

**Fix**:
```bash
./QUICK_YTDLP_SETUP.sh
./gradlew clean assembleDebug
```

### Error: "Cannot run program... error=13, Permission denied"

**Cause**: yt-dlp not executable

**Fix**: Already handled in code - file is made executable automatically

**Verify**:
```bash
adb shell ls -l /data/data/com.chuka.jamesmusicconverter/files/yt-dlp
```

Should show: `-rwxr-xr-x` (executable permissions)

### Error: "yt-dlp failed with exit code: 1"

**Check logs**:
```bash
adb logcat | grep YtDlpDownloader
```

**Common causes**:
- Invalid YouTube URL
- Video unavailable/private
- Age-restricted content
- Geo-blocked video

**Test with known working URL**:
```
https://www.youtube.com/watch?v=jNQXAC9IVRw
```

### Download is Slow

**Normal speeds**:
- YouTube SD: 1-5 MB/s
- YouTube HD: 5-15 MB/s

**Depends on**:
- Internet connection
- YouTube server location
- Video quality

**Tip**: Use audio-only for faster downloads (see below)

## ⚡ Advanced Features

### Audio-Only Download (Faster)

Currently, the app downloads full video then extracts audio. For faster downloads, modify `YtDlpDownloader.kt`:

```kotlin
// In VideoDownloader.kt, change:
ytDlpDownloader.downloadVideo(url)

// To:
ytDlpDownloader.downloadAudioOnly(url)  // ⚡ Faster, smaller file
```

**Benefits**:
- 5-10x faster download
- Smaller file size
- Less storage used
- No need to extract audio (already audio-only)

### Get Video Info First

Before downloading, get video information:

```kotlin
val info = ytDlpDownloader.getVideoInfo(url)
Log.d("Test", "Title: ${info?.title}")
Log.d("Test", "Duration: ${info?.duration}")
```

## 📈 Performance

### File Sizes (Approximate)

| Video Length | Video (Full) | Audio Only |
|--------------|--------------|------------|
| 3 min        | 30-100 MB    | 3-5 MB     |
| 10 min       | 100-300 MB   | 10-15 MB   |
| 30 min       | 300-900 MB   | 30-45 MB   |

### Download Times (on WiFi)

| File Size | Typical Time |
|-----------|--------------|
| 5 MB      | 5-10 seconds |
| 50 MB     | 30-60 seconds |
| 100 MB    | 1-2 minutes |

### Conversion Times (Simulated)

Currently using simulated conversion (~3 seconds).

For real conversion times with FFmpeg, see QUICK_START.md

## 🔍 Debug Commands

### Check yt-dlp Version

```bash
adb shell /data/data/com.chuka.jamesmusicconverter/files/yt-dlp --version
```

### Test yt-dlp Manually

```bash
# Get video info
adb shell /data/data/com.chuka.jamesmusicconverter/files/yt-dlp \
  --dump-json \
  "https://www.youtube.com/watch?v=dQw4w9WgXcQ"

# Download test
adb shell /data/data/com.chuka.jamesmusicconverter/files/yt-dlp \
  -o "/sdcard/Download/test.mp4" \
  "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
```

### Watch Logs in Real-Time

```bash
# All app logs
adb logcat | grep jamesmusicconverter

# Just download progress
adb logcat | grep "download"

# Just errors
adb logcat *:E | grep jamesmusicconverter
```

## ✅ Success Checklist

- [ ] App builds successfully
- [ ] yt-dlp appears in assets (36MB)
- [ ] App installs on device
- [ ] YouTube URL is recognized
- [ ] Progress updates appear
- [ ] Download completes
- [ ] File exists with size > 0
- [ ] Conversion completes
- [ ] MP3 file created

## 🎉 Next Steps

Once YouTube downloads work:

1. **Optimize**: Use audio-only downloads
2. **Enhance**: Show video thumbnail and title
3. **Improve**: Add format selection (quality options)
4. **Extend**: Support playlists
5. **Polish**: Add download history

## 📚 More Info

- **YT_DLP_SETUP.md** - Advanced setup options
- **TESTING_GUIDE.md** - More test scenarios
- **TROUBLESHOOTING.md** - Common issues

---

**Ready to test!** 🚀

Try this URL: `https://www.youtube.com/watch?v=dQw4w9WgXcQ`
