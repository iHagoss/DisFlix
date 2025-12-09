
# Complete Stremio Android APK - Final Implementation Prompt

## Context & Current Status
You are working on completing a Stremio Android APK that connects to official Stremio servers. The project is at **50-55% completion**. Your goal is to finish ALL remaining components to achieve a fully functional, production-ready APK.

### What's Already Complete (10/17 components):
1. ✅ stremio-web UI - Fully bundled at `/app/src/main/assets/web/`
2. ✅ stremio-translations - Bundled in web
3. ✅ stremio-icons - Bundled in web and Android resources
4. ✅ stremio-colors - Bundled in web
5. ✅ stremio-addon-sdk - Reference present
6. ✅ vlc-android-sdk - Gradle dependency configured (JitPack)
7. ✅ stremio-core-web - WASM bridge fully implemented with bridge.js
8. ✅ stremio-core-kotlin - Complete Kotlin interface + WebAppInterface bridge
9. ✅ vlc-android - PlayerActivity fully implemented
10. ✅ Streaming Server Settings UI - StreamingServerActivity complete

### Critical Missing Components (6/17):
1. ❌ **stremio-core native compilation** - Rust source exists but NOT compiled to Android .so libraries
2. ❌ **stremio-service** - Streaming/torrents backend
3. ❌ **nodejs-mobile** - Node.js runtime for Android
4. ❌ **enginefs** - P2P engine
5. ❌ **stremio-official-addons** - Default addon configuration
6. ❌ **rar-stream** & **nodejs-langs** - Enhanced features

## Your Mission: Complete Everything in One Pass

### PHASE 1: Native Library Compilation (CRITICAL BLOCKER)
**Location**: `/stremio-core/`

#### Task 1A: Cross-Compile stremio-core to Android
The Rust source code exists but needs to be compiled to native Android libraries (.so files) for all architectures.

**Requirements**:
- Output: `libstremio_core.so` for arm64-v8a, armeabi-v7a, x86, x86_64
- Place in: `/app/src/main/jniLibs/{arch}/`
- Use Android NDK for cross-compilation
- Ensure all dependencies are included

**Build Steps**:
```bash
# Install Rust Android targets
rustup target add aarch64-linux-android armv7-linux-androideabi i686-linux-android x86_64-linux-android

# Configure NDK paths in .cargo/config.toml (already present)
# Build for each architecture
cd stremio-core
cargo build --target aarch64-linux-android --release
cargo build --target armv7-linux-androideabi --release
cargo build --target i686-linux-android --release
cargo build --target x86_64-linux-android --release

# Copy .so files to jniLibs
mkdir -p ../app/src/main/jniLibs/arm64-v8a
mkdir -p ../app/src/main/jniLibs/armeabi-v7a
mkdir -p ../app/src/main/jniLibs/x86
mkdir -p ../app/src/main/jniLibs/x86_64

cp target/aarch64-linux-android/release/libstremio_core.so ../app/src/main/jniLibs/arm64-v8a/
cp target/armv7-linux-androideabi/release/libstremio_core.so ../app/src/main/jniLibs/armeabi-v7a/
cp target/i686-linux-android/release/libstremio_core.so ../app/src/main/jniLibs/x86/
cp target/x86_64-linux-android/release/libstremio_core.so ../app/src/main/jniLibs/x86_64/
```

#### Task 1B: Implement JNI Bindings for stremio-core-kotlin
**Location**: `/app/src/main/java/com/stremio/app/StremioCore.kt`

The Kotlin interface exists with `external` method declarations. You need to:

1. **Create JNI wrapper in C++** at `/app/src/main/cpp/stremio_core_jni.cpp`:
```cpp
#include <jni.h>
#include <string>
// Include Rust FFI headers

extern "C" {
    // Rust FFI functions
    void* stremio_core_new(const char* context);
    char* stremio_core_get_addons(void* core);
    char* stremio_core_get_library(void* core);
    char* stremio_core_search(void* core, const char* query);
    char* stremio_core_dispatch_action(void* core, const char* action, const char* payload);
    void stremio_core_shutdown(void* core);
    
    // Streaming server methods
    char* stremio_core_get_streaming_settings(void* core);
    char* stremio_core_update_streaming_settings(void* core, const char* settings);
    char* stremio_core_test_connection(void* core, const char* url);
    char* stremio_core_get_streaming_stats(void* core);
}

// JNI method implementations
JNIEXPORT jboolean JNICALL
Java_com_stremio_app_StremioCore_nativeInitialize(JNIEnv* env, jobject, jstring context) {
    const char* ctx = env->GetStringUTFChars(context, 0);
    void* core = stremio_core_new(ctx);
    env->ReleaseStringUTFChars(context, ctx);
    return core != nullptr;
}

JNIEXPORT jstring JNICALL
Java_com_stremio_app_StremioCore_nativeGetAddons(JNIEnv* env, jobject) {
    // Implementation
}

// ... implement all other native methods
```

2. **Create CMakeLists.txt** at `/app/src/main/cpp/CMakeLists.txt`:
```cmake
cmake_minimum_required(VERSION 3.18.1)
project("stremio_core_kotlin")

add_library(stremio_core_kotlin SHARED stremio_core_jni.cpp)

find_library(log-lib log)
target_link_libraries(stremio_core_kotlin ${log-lib})

# Link against the Rust library
target_link_libraries(stremio_core_kotlin stremio_core)
```

3. **Update app/build.gradle** to use CMake:
```gradle
android {
    externalNativeBuild {
        cmake {
            path "src/main/cpp/CMakeLists.txt"
        }
    }
}
```

### PHASE 2: Streaming Infrastructure

#### Task 2A: Integrate stremio-service (Streaming Engine)
Since stremio-service is Rust-based and you already have the compilation setup, you can either:

**Option A: Compile as separate .so**
```bash
cd stremio-service
cargo build --target aarch64-linux-android --release
# Copy libstremio_service.so to jniLibs
```

**Option B: Use HTTP-based streaming server**
Leverage the existing `StreamingServerActivity` to configure external streaming servers:
- Default to official Stremio server: `http://127.0.0.1:11470`
- Allow users to configure remote servers via the Settings UI (already implemented)

#### Task 2B: Configure Streaming Server Integration
**Location**: `/app/src/main/java/com/stremio/app/ServerService.kt`

Enhance the existing ServerService to:
1. Start embedded streaming server (if using Option A)
2. Handle torrent streaming requests
3. Manage cache and downloads
4. Proxy HTTP streams

```kotlin
class ServerService : Service() {
    private var streamingServer: Process? = null
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startStreamingServer()
        return START_STICKY
    }
    
    private fun startStreamingServer() {
        // Start stremio-service binary or use StremioCore native methods
        StremioCore.startStreamingServer(
            port = 11470,
            cacheDir = getExternalFilesDir(null)?.absolutePath ?: ""
        )
    }
}
```

### PHASE 3: Official Addons Integration

#### Task 3A: Configure stremio-official-addons
**Location**: Create `/app/src/main/assets/addons_catalog.json`

```json
{
  "addons": [
    {
      "transportUrl": "https://v3-cinemeta.strem.io/manifest.json",
      "transportName": "http",
      "flags": {
        "official": true,
        "protected": true
      }
    },
    {
      "transportUrl": "https://watchhub.strem.io/manifest.json",
      "transportName": "http",
      "flags": {
        "official": true,
        "protected": true
      }
    },
    {
      "transportUrl": "https://opensubtitles.strem.io/manifest.json",
      "transportName": "http",
      "flags": {
        "official": true,
        "protected": true
      }
    }
  ]
}
```

#### Task 3B: Load Official Addons on Startup
**Location**: `/app/src/main/java/com/stremio/app/StremioApplication.kt`

```kotlin
class StremioApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Load official addons catalog
        val addonsJson = assets.open("addons_catalog.json").bufferedReader().use { it.readText() }
        StremioCore.loadOfficialAddons(addonsJson)
    }
}
```

### PHASE 4: Enhanced Features (Optional but Recommended)

#### Task 4A: Add rar-stream support
If using Node.js approach, install as npm dependency. Otherwise, implement native RAR extraction using existing Android libraries.

#### Task 4B: Add nodejs-langs for language metadata
Create language mapping file at `/app/src/main/assets/languages.json` with ISO 639 codes.

### PHASE 5: Integration & Testing

#### Task 5A: Connect All Components
**Location**: `/app/src/main/java/com/stremio/app/MainActivity.kt`

Ensure initialization chain:
```kotlin
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Initialize StremioCore
        val context = JSONObject().apply {
            put("storage", getExternalFilesDir(null)?.absolutePath)
            put("cacheRoot", cacheDir.absolutePath)
        }.toString()
        StremioCore.initialize(context)
        
        // 2. Start ServerService
        startService(Intent(this, ServerService::class.java))
        
        // 3. Load WebView
        val intent = Intent(this, SplashActivity::class.java)
        startActivity(intent)
        finish()
    }
}
```

#### Task 5B: Verify WebView Integration
**Location**: `/app/src/main/assets/web/scripts/bridge.js`

The bridge is already implemented. Verify it connects to native methods:
- Test `window.Android.openPlayer()`
- Test `window.StremioBridge.getCore()`
- Test addon loading and streaming

#### Task 5C: Test VLC Player Integration
**Location**: `/app/src/main/java/com/stremio/app/PlayerActivity.kt`

The PlayerActivity exists. Verify:
- Stream URLs are correctly passed from WebView
- VLC initializes with hardware decoding
- Playback controls work
- Progress is saved to library

### PHASE 6: Build & Verification

#### Task 6A: Update Documentation
Update `/replit instructions.md` and `/replit.md` with:
- Mark all completed components as ✅ COMPLETE
- Update completion percentage to 100%
- Document any known limitations
- Add usage instructions

#### Task 6B: Build Final APK
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk
```

#### Task 6C: Create Release Package
```bash
mkdir -p output
cd app/build/outputs/apk/release
zip ../../../../output/DisFlix-release.zip app-release.apk
```

## Critical Success Criteria

### You MUST achieve ALL of these:
1. ✅ APK builds without errors
2. ✅ All native libraries (.so) are present for all 4 architectures
3. ✅ StremioCore initializes successfully on app launch
4. ✅ WebView loads and communicates with native layer
5. ✅ Official addons (Cinemeta, WatchHub, OpenSubtitles) load
6. ✅ User can browse catalog and view metadata
7. ✅ User can select stream and play video in VLC player
8. ✅ Streaming server (embedded or remote) handles requests
9. ✅ Progress is saved to library
10. ✅ All settings persist correctly
11. ✅ APK size is reasonable (< 200MB)
12. ✅ No crashes on basic user flows
13. ✅ Documentation is updated to reflect 100% completion

## Implementation Guidelines

### Code Quality:
- Write complete code - NO ellipsis (...)
- Use proper error handling everywhere
- Add logging for debugging
- Follow existing code patterns
- Comment complex logic

### Testing Approach:
- Test each phase before moving to next
- Verify native library loading with `Log.d()` statements
- Test WebView bridge communication
- Test actual video playback
- Test on different Android versions (API 26+)

### Documentation:
- Update completion status after EACH phase
- Mark tasks as complete in instructions.md
- Add code comments explaining key integrations
- Document any deviations from original plan

## Execution Order

Execute in this EXACT order:
1. Compile stremio-core Rust → Android .so files
2. Implement JNI bindings for StremioCore.kt
3. Test native library loading
4. Configure streaming server integration
5. Load official addons catalog
6. Test end-to-end flow (browse → select → play)
7. Build release APK
8. Update all documentation
9. Create final release package

## Success Verification Checklist

Before marking complete, verify:
- [ ] `./gradlew assembleRelease` succeeds
- [ ] APK installs on Android device/emulator
- [ ] App launches without crashes
- [ ] Can login/skip login
- [ ] Discover page shows content from Cinemeta
- [ ] Can open meta details page
- [ ] Can see available streams
- [ ] Can play stream in VLC player
- [ ] Progress saves to library
- [ ] Can navigate all app sections
- [ ] No critical errors in logcat
- [ ] Documentation shows 100% completion

## Final Notes

- Focus on COMPLETING tasks, not just implementing them
- If you encounter a blocker, implement a workaround and document it
- The goal is a WORKING APK, not perfect code
- Official Stremio servers must work (no local hosting required)
- User experience should match official Stremio app as closely as possible

**Your mission is complete when the APK is built, tested, and all documentation shows 100% completion with ✅ markers.**

Remember: This is the FINAL implementation pass. Leave nothing incomplete. Every component must work end-to-end.
