#!/bin/sh
set -eu

PACKAGE="com.jolttime.game"
ACTIVITY="$PACKAGE/.MainActivity"
CLEAN=0
[ "${1:-}" = "--clean" ] && CLEAN=1
[ $# -le 1 ] || { echo "Usage: $0 [--clean]" >&2; exit 2; }

command -v adb >/dev/null 2>&1 || { echo "ERROR: adb was not found. Install Android SDK Platform Tools and add platform-tools to PATH." >&2; exit 1; }
LINES=$(adb devices | sed '1d;/^[[:space:]]*$/d')
UNAUTHORIZED=$(printf '%s\n' "$LINES" | awk '$2=="unauthorized"{print $1}')
[ -z "$UNAUTHORIZED" ] || { echo "ERROR: Device unauthorized: $UNAUTHORIZED. Unlock the phone and accept the RSA debugging prompt." >&2; exit 1; }
DEVICES=$(printf '%s\n' "$LINES" | awk '$2=="device"{print $1}')
COUNT=$(printf '%s\n' "$DEVICES" | sed '/^$/d' | wc -l | tr -d ' ')
[ "$COUNT" -gt 0 ] || { echo "ERROR: No Android device connected. Check USB/Wireless debugging and run 'adb devices'." >&2; exit 1; }
if [ -n "${ANDROID_SERIAL:-}" ]; then SERIAL=$ANDROID_SERIAL
else
  [ "$COUNT" -eq 1 ] || { echo "ERROR: Multiple devices connected. Disconnect extras or set ANDROID_SERIAL." >&2; printf '%s\n' "$DEVICES"; exit 1; }
  SERIAL=$DEVICES
fi

if [ "$CLEAN" -eq 1 ]; then
  echo "WARNING: --clean removes Jolt Time and its local save data."
  adb -s "$SERIAL" uninstall "$PACKAGE" || true
fi

if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then GRADLE=./gradlew
elif command -v gradle >/dev/null 2>&1; then GRADLE=gradle
else echo "ERROR: Gradle wrapper JAR is absent and global Gradle was not found." >&2; exit 1
fi

echo "Building Jolt Time development APK..."
"$GRADLE" assembleDebug
APK="app/build/outputs/apk/debug/app-debug.apk"
[ -s "$APK" ] || { echo "ERROR: Build completed but $APK is missing." >&2; exit 1; }

echo "Installing in place on $SERIAL (app data is preserved)..."
set +e
OUTPUT=$(adb -s "$SERIAL" install -r "$APK" 2>&1)
STATUS=$?
set -e
printf '%s\n' "$OUTPUT"
if [ "$STATUS" -ne 0 ]; then
  case "$OUTPUT" in
    *INSTALL_FAILED_UPDATE_INCOMPATIBLE*) echo "ERROR: Signing certificate mismatch. A ONE-TIME manual uninstall is required when moving to the persistent Jolt Time development key. This deletes current local data; export/accept that loss before running './scripts/update-android.sh --clean'." >&2;;
    *INSTALL_FAILED_VERSION_DOWNGRADE*) echo "ERROR: Installed versionCode is newer. Increase versionCode, or explicitly use --clean only if losing local data is acceptable." >&2;;
    *) echo "ERROR: ADB install failed; raw output is shown above." >&2;;
  esac
  exit "$STATUS"
fi

echo "Launching Jolt Time..."
adb -s "$SERIAL" shell am start -n "$ACTIVITY" >/dev/null || { echo "ERROR: Install succeeded but app launch failed. Run: adb shell am start -n $ACTIVITY" >&2; exit 1; }
echo "Done: Jolt Time updated and launched without clearing app data."
