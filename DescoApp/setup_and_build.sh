#!/bin/bash
echo "========================================="
echo "  DESCO APK Builder - Auto Setup"
echo "========================================="

# Install Java if needed
java -version 2>/dev/null || { echo "Installing Java..."; sudo apt-get install -y openjdk-17-jdk; }

# Download Gradle
GRADLE_VERSION="8.4"
GRADLE_DIR="$HOME/.gradle_dist"
if [ ! -f "$GRADLE_DIR/gradle-$GRADLE_VERSION/bin/gradle" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    mkdir -p "$GRADLE_DIR"
    cd "$GRADLE_DIR"
    curl -L "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" -o gradle.zip
    unzip -q gradle.zip
    rm gradle.zip
fi

export PATH="$GRADLE_DIR/gradle-$GRADLE_VERSION/bin:$PATH"

# Download Android SDK command line tools
SDK_DIR="$HOME/android-sdk"
if [ ! -d "$SDK_DIR" ]; then
    echo "Downloading Android SDK..."
    mkdir -p "$SDK_DIR"
    cd "$SDK_DIR"
    curl -L "https://dl.google.com/android/repository/commandlinetools-linux-10406996_latest.zip" -o tools.zip
    unzip -q tools.zip
    rm tools.zip
    mkdir -p cmdline-tools/latest
    mv cmdline-tools/* cmdline-tools/latest/ 2>/dev/null || true
fi

export ANDROID_HOME="$SDK_DIR"
export PATH="$SDK_DIR/cmdline-tools/latest/bin:$SDK_DIR/platform-tools:$PATH"

# Accept licenses and install required packages
yes | sdkmanager --licenses 2>/dev/null
sdkmanager "platforms;android-34" "build-tools;34.0.0" 2>/dev/null

# Go to project directory
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$SCRIPT_DIR"

# Create local.properties
echo "sdk.dir=$SDK_DIR" > local.properties

# Build
echo ""
echo "Building APK..."
gradle assembleDebug

echo ""
echo "========================================="
echo "APK Location:"
find . -name "*.apk" 2>/dev/null
echo "========================================="
