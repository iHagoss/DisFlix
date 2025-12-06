# Stremio Android APK Project

## Project Overview
This is an **Android APK project** for Stremio, a media streaming application. The app is designed to run on Android TV, Firestick, and mobile Android devices.

## Important: This Project Cannot Run on Replit
This is an Android APK project that requires the Android SDK to compile. It **cannot be run as a web service or console application on Replit**.

### How to Build
The APK is built using **GitHub Actions**. When you push code to the repository:
1. GitHub Actions runs `.github/workflows/build-apk.yml`
2. Builds three variants: TV, Firestick, and Mobile
3. Outputs APK files as downloadable artifacts

To trigger a manual build:
1. Go to the GitHub repository
2. Navigate to Actions > Build Android App
3. Click "Run workflow"
4. Download the APK artifacts when complete

## Project Structure
```
app/
├── build.gradle          # Android app build configuration
├── src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/stremio/app/    # Kotlin/Java source files
│   │   ├── MainActivity.kt
│   │   ├── PlayerActivity.kt
│   │   ├── StremioApplication.kt
│   │   ├── tv/                  # TV-specific activities
│   │   │   ├── TvDetailActivity.kt
│   │   │   ├── TvPlayerActivity.kt
│   │   │   └── TvSearchActivity.kt
│   │   └── ui/                  # UI screens
│   └── res/                     # Android resources
│       ├── layout/              # XML layouts
│       ├── values/              # Colors, strings, styles
│       └── drawable/            # Icons and graphics
build.gradle              # Root build configuration
settings.gradle           # Project settings
.github/workflows/        # GitHub Actions CI/CD
```

## Build Variants (Flavors)
- **tv**: For Android TV devices
- **firestick**: For Amazon Fire TV Stick
- **mobile**: For phones and tablets

## Key Dependencies
- VLC (LibVLC 3.6.0) for video playback
- Media3 (ExoPlayer) for streaming
- Leanback for TV UI
- Picasso for image loading
- OkHttp for networking
- Kotlin Coroutines for async operations

## Development Workflow
1. Edit source files in Replit
2. Push changes to GitHub
3. GitHub Actions builds the APK
4. Download and install APK on Android device for testing

## Recent Changes
- Added TV activities (TvDetailActivity, TvSearchActivity, TvPlayerActivity)
- Added ServerService and StreamSelectActivity
- Added missing color resources and Stremio themes
- Updated GitHub Actions to build all flavors (tv, firestick, mobile)
- Created ic_launcher drawable
