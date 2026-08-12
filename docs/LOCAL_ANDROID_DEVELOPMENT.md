# Local Android development on macOS

## First setup

1. Install Android Studio and **Android SDK Platform Tools**. `adb` normally lives in `~/Library/Android/sdk/platform-tools`; add that directory to your shell `PATH`.
2. On the phone, enable **Developer options** and **USB debugging**. Connect by USB, run `adb devices`, unlock the phone, and accept the RSA authorization prompt.
3. For wireless use on Android 11+, enable **Wireless debugging**, choose **Pair device with pairing code**, then run `adb pair HOST:PAIR_PORT` and `adb connect HOST:ADB_PORT`.
4. Generate one persistent development key outside Git and create ignored `keystore.properties` in the repository root:

```properties
storeFile=/absolute/path/to/jolt-time-development.jks
storePassword=your-secret
keyAlias=jolt-time-development
keyPassword=your-secret
```

Generate it with Android Studio's key tool or `keytool -genkeypair -keystore ~/jolt-time-development.jks -alias jolt-time-development -keyalg RSA -keysize 2048 -validity 10000`. Never commit the key or passwords.

If configuration is absent, Gradle uses the normal machine-specific debug key. APKs signed by different keys cannot update one another. The currently installed CI/debug APK may therefore require **one final uninstall** when moving to the persistent key. This one-time uninstall loses local app data. After installing with the persistent key, normal updates preserve DataStore and all progress.

To make CI APKs update the same installation, configure the repository secrets `JOLT_DEV_KEYSTORE_BASE64`, `JOLT_DEV_STORE_PASSWORD`, `JOLT_DEV_KEY_ALIAS`, and `JOLT_DEV_KEY_PASSWORD` from this same key. The workflow reconstructs it only in the temporary runner and never commits it. Without these secrets, CI intentionally falls back to its temporary debug certificate and that artifact may not update a locally signed build.

## Every next update

1. Connect the phone.
2. Run `./scripts/update-android.sh`.
3. Test the game.

The default path builds, uses `adb install -r` for an in-place update, preserves internal app data, and launches `com.jolttime.game/.MainActivity`. It never uninstalls or runs `pm clear`.

Use `./scripts/update-android.sh --clean` only when you explicitly intend to uninstall and reset the development game, such as the one-time signing migration.

## Android Studio

Open the project, select JDK 17, connect and authorize the phone, select it in the device menu, and press **Run ▶**. Android Studio installs over the prior build when package and certificate match. For wireless debugging, pair/connect first and then select the device.

## Troubleshooting

- **No device:** check cable mode, Developer Options, USB debugging, and `adb devices`.
- **Unauthorized:** unlock the phone and accept RSA authorization; revoke USB debugging authorizations and reconnect if necessary.
- **Multiple devices:** disconnect extras or set `ANDROID_SERIAL`.
- **UPDATE_INCOMPATIBLE:** certificate mismatch; perform the documented one-time clean migration only when ready to lose old local data.
- **VERSION_DOWNGRADE:** increase `versionCode`; do not erase data as the normal workaround.
- **Build failure:** the script exits and retains full Gradle output.
