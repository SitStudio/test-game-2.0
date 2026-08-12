# Jolt Time

Jolt Time is an offline-first historical action RPG for Android. The Ancient Egypt vertical slice combines a node campaign, a three-hero team, directly controlled real-time battles, hero growth, artifacts, and the Temporal Fracture story.

The app uses Kotlin, Jetpack Compose, Material 3, MVVM, coroutines, Preferences DataStore, and kotlinx.serialization. It has no backend, login, ads, payments, or network requirement.

## Requirements and build

- Android Studio with Android Gradle Plugin 8.7.3 support
- JDK 17
- Android SDK Platform 35 and Build-Tools 35.0.0
- Android 8.0 / API 26 or newer device or emulator

Open the repository root in Android Studio, sync Gradle, select a landscape-capable phone/emulator, and run `app`. The activity is landscape-locked and composed for a phone 16:9 play area.

The initial text-only repository transport omits the binary wrapper JAR. Regenerate it once with `gradle wrapper --gradle-version 8.9`, then use:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`. GitHub Actions installs Gradle 8.9, runs the same checks, validates the package, and uploads **Jolt-Time-debug-apk**.

For one-command Mac → phone updates, persistent development signing, USB/Wireless ADB setup, and save-preserving installs, see [Local Android development](docs/LOCAL_ANDROID_DEVELOPMENT.md). The normal loop is `./scripts/update-android.sh`; it builds, updates in place, and launches the game.

## Architecture

- `domain/model/GameModels.kt` — persisted campaign, hero, mission, ability, and artifact models.
- `domain/model/GameContent.kt` — data-driven Ancient Egypt heroes, enemies, missions, artifacts, and dialogue.
- `game/ActionBattleEngine.kt` — fixed-step movement, targeting, cooldowns, attacks, skills, ultimates, ally assistance, enemy AI, and boss stages.
- `game/GameBattleView.kt` — Android Canvas battlefield renderer and multitouch joystick/action controls. Compose remains responsible for menus and story UI.
- `game/RpgEngine.kt` — campaign rewards, hero progression, team selection, artifact equipment, and chapter completion.
- `data/local` — corruption-tolerant DataStore JSON persistence and old-save fallback.
- `ui/GameApp.kt` — landscape Archive, Egypt map, team/heroes, battle, story, artifact reveal, and museum UI.
- `res/values` and `res/values-uk` — complete English and Ukrainian resources with stable internal IDs.

Adding a future epoch means supplying another content catalog of missions, enemy templates, dialogue, and artifacts without changing the battle engine. Only Ancient Egypt is intentionally playable in this release.
