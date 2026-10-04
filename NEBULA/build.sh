#!/data/data/com.termux/files/usr/bin/bash
# Build NEBULA.apk langsung di Termux. Butuh: openjdk-17 aapt2 d8 apksigner zip
set -e; cd "$(dirname "$0")"
AJ="${ANDROID_JAR:-$(find "$PREFIX/share" "$HOME" -maxdepth 4 -name android.jar 2>/dev/null | head -1)}"
[ -f "$AJ" ] || { echo "android.jar tidak ketemu. Pakai: ANDROID_JAR=/path/android.jar bash build.sh"; exit 1; }
JC=$(command -v javac || command -v ecj)
rm -rf build; mkdir -p build/gen build/cls build/dex
aapt2 link -o build/base.apk -I "$AJ" --manifest AndroidManifest.xml -A assets --java build/gen \
  --min-sdk-version 26 --target-sdk-version 28 --version-code 1 --version-name 1.0
"$JC" -source 1.8 -target 1.8 -cp "$AJ" -d build/cls $(find src build/gen -name '*.java')
if command -v d8 >/dev/null; then d8 --min-api 26 --lib "$AJ" --output build/dex $(find build/cls -name '*.class')
else dx --dex --output=build/dex/classes.dex build/cls; fi
(cd build/dex && zip -q ../base.apk classes.dex)
[ -f nebula.jks ] || keytool -genkeypair -keystore nebula.jks -alias nebula -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass nebula123 -keypass nebula123 -dname "CN=NEBULA"
apksigner sign --ks nebula.jks --ks-pass pass:nebula123 --out NEBULA.apk build/base.apk
cp NEBULA.apk /sdcard/Download/NEBULA.apk 2>/dev/null && echo "Disalin ke /sdcard/Download/NEBULA.apk"
echo "OK -> $(pwd)/NEBULA.apk"
