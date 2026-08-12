# Final Build Readiness Report

Audit date: 2026-08-11

This report separates source-level readiness from commands that were physically executable in the Codex container. It does not claim that an APK was produced locally.

## Build configuration — PASS

- The project uses AGP 8.7.3, Gradle Wrapper 8.9, Kotlin/Compose plugin 2.0.21, JDK 17 bytecode, compile/target SDK 35, and Build-Tools 35.0.0.
- Namespace and application ID are both `com.jolttime.game`; minimum SDK is 26.
- Compose, Material 3, DataStore Preferences, coroutines, and kotlinx.serialization dependencies and plugins are declared.
- The text wrapper scripts and wrapper properties are checked in. The binary wrapper JAR is intentionally omitted from the initial Codex Web pull request; CI installs Gradle 8.9 with the official Gradle action, and the standard wrapper can be regenerated after clone.

## Kotlin — PASS

- Packages and source paths align, domain and game-engine sources compile with the Kotlin 2.0.21 compiler under JDK 17, and the game-engine JUnit suite executes successfully using the locally available Kotlin/JUnit runtime.
- Saturating arithmetic and bounded XP calculation protect large values and avoid a potentially unbounded level-up loop.
- Full Android Kotlin compilation remains dependent on the CI/local Android toolchain described below.

## Compose — PASS

- Every bottom-navigation destination resolves to a Compose screen.
- Progress values are clamped, fragment denominators cannot be zero, expedition remaining time cannot become negative, and empty artifact collections have a safe museum progress value.
- The activity remains portrait locked and the ViewModel survives normal configuration recreation.

## Game engine — PASS

- Taps, purchases, XP, multi-level progression, currency floors, fragment bounds, one-time expedition claims, and one-time daily claims are covered by pure JVM tests.
- Large XP values use a bounded binary search rather than repeated per-level iteration.

## DataStore — PASS

- The complete serializable `GameState` contains currencies, XP/level, upgrades, artifacts, expedition timestamps, daily state, settings, and last-seen time.
- DataStore uses a file-corruption replacement handler. Invalid/unknown JSON safely falls back to a new game, and decoded impossible values are sanitized.
- ViewModel writes are sent through one conflated persistence channel so older asynchronous writes cannot complete after newer state. Reset flags the ordered writer to clear DataStore before writing initial state, so deleted progress cannot reappear after restart.

## Expeditions — PASS

- An expedition persists absolute start/end timestamps. Restore immediately applies `updateTime`, and tests cover both sides of the exact completion boundary and duplicate claims.

## Offline income — PASS

- Negative/zero elapsed time earns zero. Seven-hour, ten-hour-equivalent, and 100-hour absences are capped at 21,600 seconds (six hours).
- Active ticks refresh `lastSeenAt`, preventing active play time from being paid again after process termination.

## Daily reward — PASS

- ISO dates persist across restart. A claim is accepted only when the local date is strictly after the saved date, preventing same-day duplicate claims and rewards caused by moving the clock backward.
- Tests cover all seven cycle positions and duplicate/backdated attempts.

## Tests — PASS (game-engine JVM); NOT VERIFIED (Android Gradle task)

- The manually invoked Kotlin 2.0.21 compiler compiled the domain/game-engine sources under JDK 17.
- JUnit executed all 15 `GameEngineTest` tests successfully.
- `GameStateCodecTest` is included for Gradle/CI execution because serialization code generation requires the Gradle serialization compiler plugin.
- The earlier `./gradlew testDebugUnitTest` attempt could not download Gradle 8.9 because the environment proxy returned HTTP 403. CI now installs Gradle 8.9 through `gradle/actions/setup-gradle` and invokes the installed `gradle` executable.

## CI — PASS (configured); NOT VERIFIED (remote run)

- `.github/workflows/android-build.yml` triggers on push, pull request, and manual dispatch.
- It provisions JDK 17, SDK Platform 35, Build-Tools 35.0.0, and Gradle 8.9; runs clean, unit tests, lint, and `assembleDebug`; and has no error suppression.
- A successful run extracts version metadata from the built APK and uploads a uniquely named artifact such as `Jolt-Time-v0.4.5-dev-b1786535901-f2943ad`. No remote run was available from this repository environment, so the workflow result is not claimed as verified.

## APK — FAIL / NOT VERIFIED LOCALLY

- The earlier `./gradlew assembleDebug` attempt could not download Gradle 8.9 because the Codex proxy returned HTTP 403. No APK was generated or committed; the text-only CI path uses the installed Gradle 8.9 executable instead.
- On a normal network, the local Gradle output remains `app/build/outputs/apk/debug/app-debug.apk`; CI copies the validated APK into `dist/Jolt-Time-v<VERSION_NAME>-b<VERSION_CODE>-<SHORT_GIT_SHA>.apk` and uploads it under the matching versioned artifact name.
