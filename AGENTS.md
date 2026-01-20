# Repository Guidelines

## Project Structure & Module Organization
- `app/` is the single Android application module.
- Kotlin/Java sources live in `app/src/main/java/com/celdy/groufr/` with feature areas like `data/`, `di/`, and `ui/`.
- Android resources live in `app/src/main/res/` (layouts, drawables, values, etc.).
- Unit tests are in `app/src/test/java/`; instrumentation tests are in `app/src/androidTest/java/`.
- Gradle configuration is in `build.gradle.kts`, `app/build.gradle.kts`, and `gradle/libs.versions.toml`.

## Build, Test, and Development Commands
- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew installDebug` installs the debug build to a connected device/emulator.
- `./gradlew testDebugUnitTest` runs local JUnit unit tests.
- `./gradlew connectedDebugAndroidTest` runs Espresso instrumentation tests.
- `./gradlew lintDebug` runs Android Lint.

On Windows PowerShell, use `./gradlew.bat` (for example, `./gradlew.bat testDebugUnitTest`).

## Coding Style & Naming Conventions
- Kotlin uses the official style (`kotlin.code.style=official`) with 4-space indentation.
- Class/object names use PascalCase; functions/variables use camelCase.
- Package names are lowercase; keep app code under `com.celdy.groufr`.
- Resource names are lowercase_with_underscores (for example, `activity_main.xml`, `ic_logo.png`).

## Testing Guidelines
- Unit tests use JUnit4 (`testImplementation(libs.junit)`).
- Instrumentation tests use AndroidX JUnit and Espresso.
- Name test classes with the `*Test` suffix and place them alongside the corresponding package structure.
- No coverage thresholds are configured; add tests for new logic and UI flows.

## Commit & Pull Request Guidelines
- Git history is not available in this workspace, so commit message conventions could not be verified.
- Until confirmed, keep commits short and imperative, and reference issue IDs when applicable.
- PRs should include a clear description, test commands run, and screenshots for UI changes.

## Security & Configuration Tips
- `local.properties` is developer-specific (SDK paths); never commit secrets.
- Keep API keys and tokens out of source control; use local Gradle properties or CI secrets.
