# Jolt Time

Jolt Time is a standalone, portrait-native Android idle collection RPG. It is built entirely with Kotlin, Jetpack Compose, Material 3, coroutines, MVVM, and Preferences DataStore. It has no account, backend, ads, payment system, web runtime, or network requirement.

## Requirements

- **Android Studio:** a current stable release capable of using AGP 8.7.3.
- **JDK:** 17. Select it under **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.
- **Android SDK:** SDK Platform 35 and Android SDK Build-Tools 35.0.0. Install both from **Tools → SDK Manager**. The app runs on Android 8.0/API 26 or newer.
- **Emulator:** in **Tools → Device Manager**, create a phone, download an API 35 system image, and start the virtual device. A physical API 26+ device with USB debugging enabled also works.

The repository keeps the text wrapper scripts and Gradle 8.9 configuration, but temporarily omits the binary `gradle-wrapper.jar` so the initial Codex Web pull request remains text-only. GitHub Actions installs Gradle 8.9 through the official Gradle action. After the initial clone, regenerate the standard local wrapper once with `gradle wrapper --gradle-version 8.9`; subsequent local commands can use `./gradlew` normally.

## Open and run in Android Studio

1. Choose **Open** and select this repository's root directory.
2. Run **File → Sync Project with Gradle Files** and allow dependency resolution to finish.
3. Select a running emulator or connected Android device from the deployment target list.
4. Press **Run** for the `app` configuration.

The layouts are responsive for common portrait widths including 360, 390, and 412 dp. The activity is portrait-locked.

## Build an APK

From Android Studio use **Build → Build Bundle(s) / APK(s) → Build APK(s)**, or run from the repository root:

```bash
gradle wrapper --gradle-version 8.9 # one-time, until wrapper.jar is restored after the initial PR
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. For a release APK, configure signing in Android Studio and run `assembleRelease`.

Useful verification commands are:

```bash
./gradlew clean
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

## Continuous integration and APK artifact

`.github/workflows/android-build.yml` runs on pushes, pull requests, and manual dispatch. It provisions JDK 17, Android SDK Platform 35, Build-Tools 35.0.0, and Gradle 8.9, then performs clean, unit tests, Android lint, and `assembleDebug` without hiding failures. A successful run validates and uploads `app-debug.apk` as the downloadable **Jolt-Time-debug-apk** workflow artifact.

## Project map

- `domain/model/GameModels.kt` defines the saved state and economy models.
- `domain/model/GameContent.kt` is the single catalog for epochs, upgrades, and the 25 artifacts.
- `game/GameEngine.kt` contains pure tap, progression, reward, expedition, and offline-income rules.
- `data/local/GameStore.kt` serializes the complete game state into DataStore.
- `ui/GameViewModel.kt` coordinates persistence, state, and one-second game ticks.
- `ui/GameApp.kt` contains the Compose navigation shell and playable screens.

## Extend content

To add a civilization, add an `Epoch` to `GameContent.epochs`, add its five artifact names to `names`, and give it a unique ID and unlock level. The artifact catalog is generated from those entries. To add an individual artifact with custom rules, replace or append to the generated `GameContent.artifacts` list while preserving a unique ID. DataStore's tolerant JSON decoder keeps older saves readable when new fields are introduced with defaults.

## Reset progress

Open **Settings** in the bottom bar, tap **Reset all progress**, and confirm. During development, clearing the app's storage or uninstalling it has the same effect.

See [the game design](docs/GAME_DESIGN.md) for the MVP's balancing and roadmap.
