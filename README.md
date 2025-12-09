
# DisFlix - Stremio Android APK

[![Build Android App](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/build-apk.yml/badge.svg)](https://github.com/YOUR_USERNAME/YOUR_REPO/actions/workflows/build-apk.yml)

## Overview

DisFlix is a universal Android APK fork of Stremio designed to work on Firestick, Android TV, and mobile devices. The app connects to official Stremio servers for authentication, library sync, addons, and content discovery.

## Features

- ✅ Full Stremio Core integration (Rust + WASM)
- ✅ Complete Web UI (stremio-web)
- ✅ VLC player integration for video playback
- ✅ Official Stremio API authentication and addon sync
- ✅ WebView-based hybrid architecture
- ✅ Support for all Android architectures (arm64-v8a, armeabi-v7a, x86, x86_64)

## Building from Source

### Prerequisites

- Java 17
- Android SDK Platform 34/35
- Android Build Tools 34.0.0
- Node.js 20+
- pnpm 8+

### Local Build

1. Clone the repository:
```bash
git clone https://github.com/YOUR_USERNAME/YOUR_REPO.git
cd YOUR_REPO
```

2. Build using Gradle:
```bash
export GRADLE_OPTS="-Xmx4096m -XX:MaxMetaspaceSize=512m"
./gradlew assembleDebug --no-daemon --stacktrace
```

3. Find the APK:
```
app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions Build

The repository includes automated GitHub Actions workflows:

- **Manual Build**: Go to Actions → Build Android App → Run workflow
- **Automatic Build**: Pushes to `main` branch trigger builds automatically

After the workflow completes, download the APK from the Artifacts section.

## Project Structure

```
├── app/                          # Android application
│   ├── src/main/
│   │   ├── assets/web/          # Stremio web UI bundle
│   │   ├── java/                # Kotlin/Java source
│   │   ├── jni/                 # Native library config
│   │   └── res/                 # Android resources
│   └── build.gradle             # App dependencies
├── stremio-core/                # Rust core library (source)
├── stremio-web/                 # React web UI (source)
├── vlc-android-sdk/             # VLC player integration
└── .github/workflows/           # CI/CD workflows
```

## Configuration

### App Details
- **Application ID**: `com.stremio.app`
- **Version**: 1.0 (versionCode: 1)
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

### Stremio Integration
- **API Endpoint**: `https://api.strem.io/api/`
- **Addon Catalog**: Official Stremio addon repository
- **Authentication**: OAuth via official Stremio servers

## Testing

The APK has been verified with:
- [x] Successful build and installation
- [x] WebView loads Stremio UI
- [x] VLC player integration
- [x] User authentication flow
- [x] Settings and configuration

## Known Limitations

- Torrent streaming requires backend service integration
- Some advanced features may require additional configuration

## License

This project is a fork of open-source Stremio components. Please refer to individual component licenses.

## Acknowledgments

- **Stremio** - Original application and core libraries
- **VLC** - Video player library
- **React** - Web UI framework

## Support

For issues and feature requests, please use the GitHub Issues tab.

---

**Note**: This is an unofficial fork. For official support, please visit [stremio.com](https://www.stremio.com).
