#!/bin/bash
# Quick yt-dlp setup script for Android
# Run this from the project root directory

set -e

echo "🎬 Quick yt-dlp Setup for Android"
echo "=================================="
echo ""

# Create assets directory if it doesn't exist
ASSETS_DIR="app/src/main/assets"
mkdir -p "$ASSETS_DIR"

echo "📁 Assets directory: $ASSETS_DIR"
echo ""

# Download yt-dlp for Linux (works on Android)
echo "⬇️  Downloading yt-dlp for Android..."
curl -L https://github.com/yt-dlp/yt-dlp/releases/latest/download/yt-dlp_linux \
    -o "$ASSETS_DIR/yt-dlp"

# Make it executable
chmod +x "$ASSETS_DIR/yt-dlp"

echo ""
echo "✅ yt-dlp downloaded successfully!"
echo ""
echo "📝 File location: $ASSETS_DIR/yt-dlp"
echo "📦 File size: $(ls -lh "$ASSETS_DIR/yt-dlp" | awk '{print $5}')"
echo ""
echo "🔨 Next steps:"
echo "1. Rebuild the app: ./gradlew assembleDebug"
echo "2. Install on device: ./gradlew installDebug"
echo "3. Try a YouTube URL!"
echo ""
echo "📺 Test with: https://www.youtube.com/watch?v=dQw4w9WgXcQ"
echo ""
