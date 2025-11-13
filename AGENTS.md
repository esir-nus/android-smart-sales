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


Perfect — below are the two final deliverables you requested:
short, production-ready, under-500-word rulesets for **`agent.md`** and **`tasklist.md`** that align with the **Vibe + TDD** principle.

---

## 🧭 `agent.md` — *Vibe + TDD Core Principles*  *(≈ 480 words)*

**Purpose**
Guide Android agents working on Wi-Fi, Bluetooth, and AI features to balance *creative velocity (vibe)* with *technical discipline (TDD)*.

---

### 1. Core Ethos

* **Tests are the spec.** All behavior is proven by tests; nothing ships untested.
* **Vibe ≠ chaos.** Rapid iteration is allowed only inside a green safety net.
* **Small steps.** Deliver one verified behavior per commit.
* **Refactor safely.** Only refactor on a full-green suite.

---

### 2. Operating Loop — Red → Green → Refactor → Ship

1. **Red:** Write or select a failing test that defines intent.
2. **Green:** Implement the minimal code to pass it.
3. **Refactor:** Simplify structure without altering behavior.
4. **Ship:** Commit when tests, lint, and static checks are all green.

Repeat the loop continuously. Every cycle must leave the code base deployable.

---

### 3. Design & Architecture

* **Clean + MVVM:** separate UI / Domain / Data.
* **Hilt for DI**, **Room for DB**, **Coroutines + Flow** for concurrency.
* **Lifecycle-aware:** cancel scans, flows, or coroutines on `onStop()`.
* **No magic:** Explicit dependency graphs and constructor injection only.

---

### 4. Scope Discipline

* ≤ 1 behavior / commit; ≤ ~20 LOC per change.
* If wider impact, split into sub-steps each proven by its own test.
* Each bug fix requires a regression test that fails before and passes after.

---

### 5. AI, BT, Wi-Fi Specifics

* **AI:** Prefer on-device models; seed fixed; offline fallback required.
* **Bluetooth:** Permission-gated, bounded scans, lifecycle-aware cancel.
* **Wi-Fi:** Use modern APIs (`NetworkSpecifier`/`Suggestion`), handle captive portals gracefully.
* **Privacy:** No PII in logs; encrypted storage only.

---

### 6. Network & China Constraints

* Use mirrored Maven repos; failover endpoints; adaptive timeouts.
* Assume partial connectivity; design offline-first flows.
* Avoid hard-dependency on Google services; use optional shims.

---

### 7. Test Policy

* **Unit → Instrumented → UI**: escalate outward only when needed.
* Each schema migration, permission flow, or AI inference path must have a test.
* Flaky tests = bugs; stabilize before merge.
* Green = ready to release.

---

### 8. Workflow Integration

* Daily: break features into test-driven micro-tasks listed in `tasklist.md`.
* The agent reads `tasklist.md`, executes each failing test → passes → refactors → commits.
* Never modify or remove tests unless the task explicitly begins with `TEST_CHANGE:`.

---

### 9. Definition of Done

✅ All tests green ✅ Lint + Detekt clean ✅ Perf gates passed ✅ Commit isolated and descriptive

---

**Pointer:**

> Granular task creation, sequencing, and execution live in [`tasklist.md`](./tasklist.md).

---



