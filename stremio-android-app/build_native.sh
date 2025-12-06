#!/bin/bash
set -e

# Build stremio-core for Android NDK targets
ANDROID_NDK=${ANDROID_NDK_HOME:-/opt/android-ndk}
CARGO_NDK=${CARGO_NDK:-cargo-ndk}

cd ../stremio-core

# Install rustup targets for Android
rustup target add aarch64-linux-android armv7-linux-androideabi x86_64-linux-android x86-linux-android

# Build for each Android target
echo "Building stremio-core for Android targets..."

# Using cargo with Android targets (if cargo-ndk available)
if command -v $CARGO_NDK &> /dev/null; then
    $CARGO_NDK build --release --target aarch64-linux-android
    $CARGO_NDK build --release --target armv7-linux-androideabi
    $CARGO_NDK build --release --target x86_64-linux-android
    $CARGO_NDK build --release --target x86-linux-android
else
    # Fallback: use standard cargo with NDK environment
    export NDK_ROOT=$ANDROID_NDK
    export PATH=$ANDROID_NDK/toolchains/llvm/prebuilt/linux-x86_64/bin:$PATH
    
    cargo build --release --target aarch64-linux-android
    cargo build --release --target armv7-linux-androideabi
    cargo build --release --target x86_64-linux-android
    cargo build --release --target x86-linux-android
fi

echo "Native libraries built successfully"
