# Repository Guidelines

## Project Structure & Module Organization
This solo-maintained app ships from `SmartSalesAssistant_Final_Delivery/`, which includes the Gradle wrapper, settings, and the single `app` module. All runtime Kotlin sources sit in `SmartSalesAssistant_Final_Delivery/app/src/main/java/com/smartsales`, where domain logic (BLE, repositories, Room entities) currently shares the root package; add new files by clustering related code with clear suffixes such as `Repository`, `UseCase`, or `Screen`. Delivery artefacts and specs remain in `Documentation/`, while quality reports (for audit use) live beside this guide at the repo root.

## Build, Test, and Development Commands
- `cd SmartSalesAssistant_Final_Delivery && ./gradlew assembleDebug` — compile the app and verify the project still builds (requires Android SDK).
- `./gradlew testDebugUnitTest` — run JVM unit tests; create them in `app/src/test/kotlin`.
- `./gradlew connectedDebugAndroidTest` — execute instrumentation tests on a connected device or emulator.
- `./gradlew lint` — run Android lint and Compose checks before committing or tagging a release.

## Coding Style & Naming Conventions
The codebase targets Kotlin 1.9 with Jetpack Compose, Hilt, and Room. Use four-space indentation, trailing commas where Compose benefits, and prefer expression-bodied functions for simple delegates. View-models, repositories, and use cases follow PascalCase with the role in the suffix (`BleManager`, `ConversationRepository`). Keep Compose functions stateless where possible and suffix previews with `Preview`. Run `./gradlew lint` before committing if the IDE does not auto-format.

## Testing Guidelines
Add JVM tests beneath `SmartSalesAssistant_Final_Delivery/app/src/test/kotlin`, mirroring the production package path (`com.smartsales`). Instrumented tests belong in `app/src/androidTest/kotlin`. Name tests with the unit or flow under test (e.g., `ConversationRepositoryTest`). When adding BLE or network features, provide coroutine test coverage and fake dependencies so CI can run without hardware.

## Commit & Branch Guidelines
Even as a solo maintainer, follow the conventional commit style used in history (`type: short summary`) to keep the log searchable. Keep the subject under 72 characters and note intent plus testing in the body. When experimenting, open feature branches so you can review changes before merging to `main`; if you do create self-review pull requests, document steps taken and capture emulator screenshots for UI work.

## Configuration & Security Notes
Secrets stay out of version control: add `DASHSCOPE_API_KEY` and `TINGWU_API_KEY` to `SmartSalesAssistant_Final_Delivery/local.properties`. Never log raw keys or transcripts; prefer masking helpers already in `BleConstants.kt`. Remove temporary debug toggles before committing snapshots you plan to keep.

- See `DEPENDENCY_VERSIONING.md` for the detailed dependency/version pinning policy, shared constants, and toolchain baselines.
- Whenever you change dependencies, SDK/JDK versions, Gradle/AGP versions, or other tooling noted in that document, explicitly call it out in your status update so the maintainer stays informed.

## China Network Considerations
- Gradle already declares Aliyun mirrors in `settings.gradle.kts`; if sync still times out, comment out the default `google()`/`mavenCentral()` lines temporarily.
- Keep the bundled JDK 17 (`./jdk-17.0.9+9`) and set `org.gradle.java.home=./jdk-17.0.9+9` to avoid pulling toolchains from blocked hosts.
- Create `local.properties` from `local.properties.example`, pointing `sdk.dir` to a locally installed Android SDK mirror (e.g., from Tencent Cloud).
- Pre-cache dependencies on a fast network with `./gradlew --refresh-dependencies assembleDebug`, then carry the `.gradle-cache/` directory for offline work.
- Configure corporate or system-wide proxies via `gradle.properties` (`systemProp.https.proxyHost`, etc.) when interacting with Play Services artifacts.
