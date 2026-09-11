#!/bin/bash
set -e
export PATH="/usr/lib/jvm/jdk-11/bin:/tmp/android/build-tools/android-14:$PATH"
ANDROID_JAR=/tmp/android/platform/android-34/android.jar
BASE="$(cd "$(dirname "$0")" && pwd)"
WORK=/tmp/apkwork
rm -rf "$WORK" && mkdir -p "$WORK/obj"

echo "== 1) compile resources =="
aapt2 compile --dir "$BASE/res" -o "$WORK/res.zip"

echo "== 2) link =="
aapt2 link -o "$WORK/base.apk" -I "$ANDROID_JAR" --manifest "$BASE/AndroidManifest.xml" "$WORK/res.zip"

echo "== 3) javac =="
javac -source 1.8 -target 1.8 -bootclasspath "$ANDROID_JAR" -classpath "$ANDROID_JAR" \
  -d "$WORK/obj" "$BASE/src/com/jarvis/assistant/MainActivity.java"

echo "== 4) d8 =="
d8 --release --lib "$ANDROID_JAR" --output "$WORK" "$WORK/obj/com/jarvis/assistant/"*.class
ls -la "$WORK/classes.dex"

echo "== 5) add classes.dex =="
cd "$WORK"
zip -j base.apk classes.dex

echo "== 6) zipalign =="
zipalign -f 4 base.apk aligned.apk

echo "== 7) keystore =="
if [ ! -f "$BASE/jarvis.keystore" ]; then
  keytool -genkeypair -v -keystore "$BASE/jarvis.keystore" -alias jarvis \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -storepass jarvis123 -keypass jarvis123 \
    -dname "CN=JARVIS, OU=Dev, O=JARVIS, L=Local, S=Local, C=US" >/dev/null 2>&1
fi

echo "== 8) sign =="
apksigner sign --ks "$BASE/jarvis.keystore" --ks-key-alias jarvis \
  --ks-pass pass:jarvis123 --key-pass pass:jarvis123 \
  --out "$BASE/JARVIS.apk" aligned.apk

echo "== 9) verify =="
apksigner verify --verbose "$BASE/JARVIS.apk" | head -6
ls -la "$BASE/JARVIS.apk"
