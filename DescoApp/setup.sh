#!/bin/bash
echo "=== DESCO App Setup ==="

# Check Java
java -version 2>/dev/null || { echo "ERROR: Java নেই! sudo apt install openjdk-17-jdk"; exit 1; }

# Download Android SDK if not present
SDK_DIR="$HOME/android-sdk"
if [ ! -d "$SDK_DIR/platforms/android-34" ]; then
    echo "Android SDK ডাউনলোড হচ্ছে..."
    mkdir -p "$SDK_DIR/cmdline-tools"
    curl -L "https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip" -o /tmp/tools.zip
    unzip -q /tmp/tools.zip -d /tmp/cmdtools
    mkdir -p "$SDK_DIR/cmdline-tools/latest"
    cp -r /tmp/cmdtools/cmdline-tools/* "$SDK_DIR/cmdline-tools/latest/"
    export ANDROID_HOME="$SDK_DIR"
    export PATH="$SDK_DIR/cmdline-tools/latest/bin:$PATH"
    yes | sdkmanager --licenses
    sdkmanager "platforms;android-34" "build-tools;34.0.0"
fi

echo "sdk.dir=$SDK_DIR" > local.properties
echo "=== Setup সম্পন্ন! এখন: ./gradlew assembleDebug ==="
