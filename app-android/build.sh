#!/bin/bash
# Builds the complete native JARVIS app into JARVIS.apk using only the Android
# SDK command-line tools (no Gradle needed). Also works in CI (GitHub Actions)
# where ANDROID_HOME points at the SDK.
#
# Requirements: JDK 11+, Android build-tools 34 (aapt2/d8/apksigner/zipalign),
#               platform-34 (android.jar).
set -e

# ---- locate JDK ----
JAVAC_BIN=""
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
  JAVAC_BIN="$JAVA_HOME/bin/"
elif [ -x /usr/lib/jvm/jdk-11/bin/javac ]; then
  JAVAC_BIN="/usr/lib/jvm/jdk-11/bin/"
elif command -v javac >/dev/null 2>&1; then
  JAVAC_BIN=""
fi
export PATH="$JAVAC_BIN:$PATH"

# ---- locate Android build-tools + android.jar ----
ANDROID_HOME="${ANDROID_HOME:-/tmp/android}"
BT_DIR=""
for cand in "$ANDROID_HOME"/build-tools/* /tmp/android/build-tools/*; do
  if [ -x "$cand/aapt2" ]; then BT_DIR="$cand"; break; fi
done
if [ -z "$BT_DIR" ]; then
  echo "ERROR: Android build-tools (aapt2) not found — set ANDROID_HOME." >&2
  exit 1
fi
export PATH="$BT_DIR:$PATH"

JAR=""
for cand in "$ANDROID_HOME"/platforms/android-34/android.jar \
            "$ANDROID_HOME"/platform/android-34/android.jar \
            /tmp/android/platform/android-34/android.jar; do
  if [ -f "$cand" ]; then JAR="$cand"; break; fi
done
if [ -z "$JAR" ]; then
  echo "ERROR: android.jar (platform-34) not found." >&2
  exit 1
fi

BASE="$(cd "$(dirname "$0")" && pwd)"
SRC="$BASE/app/src/main"
WORK=/tmp/nativebuild
rm -rf "$WORK"; mkdir -p "$WORK/obj"

echo "== 1) compile resources =="
aapt2 compile --dir "$SRC/res" -o "$WORK/res.zip"
aapt2 link -o "$WORK/base.apk" -I "$JAR" --manifest "$SRC/AndroidManifest.xml" "$WORK/res.zip"

echo "== 2) javac (all sources) =="
find "$SRC/java" -name '*.java' > "$WORK/sources.txt"
javac -encoding UTF-8 -source 1.8 -target 1.8 -bootclasspath "$JAR" -classpath "$JAR" \
  -d "$WORK/obj" @"$WORK/sources.txt"

echo "== 3) d8 =="
d8 --release --lib "$JAR" --output "$WORK" $(find "$WORK/obj" -name '*.class')
ls -la "$WORK/classes.dex"

echo "== 4) package + align =="
cd "$WORK"
zip -j base.apk classes.dex
zipalign -f 4 base.apk aligned.apk

echo "== 5) keystore (auto-generated if missing) =="
KS="$BASE/jarvis.keystore"
if [ ! -f "$KS" ]; then
  keytool -genkeypair -v -keystore "$KS" -alias jarvis -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass jarvis123 -keypass jarvis123 \
    -dname "CN=JARVIS, OU=Dev, O=JARVIS, L=Local, S=Local, C=US" >/dev/null 2>&1
fi

echo "== 6) sign =="
apksigner sign --ks "$KS" --ks-key-alias jarvis --ks-pass pass:jarvis123 --key-pass pass:jarvis123 \
  --out "$BASE/JARVIS.apk" aligned.apk

echo "== 7) verify =="
apksigner verify --verbose "$BASE/JARVIS.apk" | head -6
ls -la "$BASE/JARVIS.apk"
