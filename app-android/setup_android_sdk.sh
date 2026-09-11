#!/bin/bash
# Sets up the Android command-line build toolchain (no Android Studio needed).
# Downloads build-tools 34 + platform android-34 into /tmp/android.
# Usage: bash setup_android_sdk.sh
set -e
SDK=/tmp/android
mkdir -p "$SDK"
cd /tmp

if [ ! -x "$SDK/build-tools/android-14/aapt2" ]; then
  echo "== downloading build-tools r34 =="
  curl -fsSL -o /tmp/bt.zip https://dl.google.com/android/repository/build-tools_r34-linux.zip
  rm -rf /tmp/btx && mkdir /tmp/btx
  unzip -q /tmp/bt.zip -d /tmp/btx
  mkdir -p "$SDK/build-tools"
  # zip extracts to a folder named android-14 (or android-14.0.0); normalise it
  d=$(find /tmp/btx -maxdepth 1 -type d -name 'android-*' | head -1)
  rm -rf "$SDK/build-tools/android-14"
  mv "$d" "$SDK/build-tools/android-14"
  echo "build-tools: $SDK/build-tools/android-14"
fi

if [ ! -f "$SDK/platform/android-34/android.jar" ]; then
  echo "== downloading platform android-34 =="
  curl -fsSL -o /tmp/plat.zip https://dl.google.com/android/repository/platform-34-ext7_r03.zip
  rm -rf /tmp/plx && mkdir /tmp/plx
  unzip -q /tmp/plat.zip -d /tmp/plx
  mkdir -p "$SDK/platform"
  rm -rf "$SDK/platform/android-34"
  mv /tmp/plx/android-34 "$SDK/platform/android-34"
  echo "platform: $SDK/platform/android-34"
fi

echo "== verify =="
ls "$SDK/build-tools/android-14/aapt2" "$SDK/platform/android-34/android.jar"
"$SDK/build-tools/android-14/aapt2" version 2>&1 | head -1
