# Smart Sales Assistant Testing Plan

## 1. Scope & Objectives
- Validate stability, performance, and resilience of all runtime modules (`business`, `data`, `ui`, application entry points).
- Capture regressions quickly through layered automation (static analysis → unit → integration → instrumentation → manual QA).
- Report quality metrics (coverage, lint debt, open defects) to sustain release readiness.

## 2. Preconditions
- Android SDK 34 (local mirror as per `local.properties.example`), JDK `./jdk-17.0.9+9`.
- Secrets configured in `local.properties` (`DASHSCOPE_API_KEY`, `TINGWU_API_KEY` masked in logs).
- Gradle cache hydrated (`./gradlew --refresh-dependencies assembleDebug`) before offline work.
- Baseline artifacts archived: latest `./gradlew lint` output, `Documentation/` specs, `SOFTWARE_QUALITY_METRICS_REPORT.md`.

## 3. Environment Matrix
- **Build variants**: `debug` for automation, `release` for smoke checks.
- **Devices**: Pixel 5 API 34 emulator, Xiaomi hardware running API 33 (China network), 7" tablet profile (layout validation).
- **Network profiles**: Online, proxy-intercepted (to confirm TLS), offline/airplane.

## 4. Automation Pipeline
1. `./gradlew lint` — Kotlin/Compose lint, accessibility, dependency checks (gate CI).
2. `./gradlew testDebugUnitTest jacocoTestReport` — JVM coverage published to `Documentation/coverage/`.
3. `./gradlew detekt ktlintCheck` — optional static linters (fail on violation).
4. `./gradlew connectedDebugAndroidTest` — instrumentation suite on emulator farm.
5. `./gradlew benchmarkReport` — macrobenchmark module (optional) for Compose performance tracking.

CI integrates stages 1–4 with caching, fails fast on lint/test errors, archives reports as build artifacts.

## 5. Module Coverage Map
### business/bluetooth
- **Unit**: `BleManagerTest`, `BleModuleTest`, `BleCommandTest` using fake GATT, coroutine test dispatcher; verify connect/disconnect state machine, retry policy, telemetry masking.
- **Integration**: Instrumented BLE flow with mocked `BluetoothAdapter`, verifying notification subscription, command dispatch, failure surfaces (timeouts, permissions).
- **Non-functional**: Stress test command queue ordering under rapid sends; ensure logs redact keys (`BleConstants`).

### data/network
- **Unit**: Retrofit builder configuration, timeout/backoff, interceptors injecting headers from `ApiConfig`.
- **Integration**: Instrumented test with MockWebServer asserting request sequencing, proxy toggles, offline caching behavior.
- **Resilience**: Network failure drill (HTTP 500/429) verifying retry and user messaging.

### ui/components & navigation
- **Unit/Compose**: Snapshot tests for `MessageBubble`, `ErrorState`, `EmptyState`, ensuring theming alignment (light/dark).
- **Navigation**: Compose rule to traverse `NavGraph` start → detail → settings, confirm state persistence and deep link handling.
- **Accessibility**: Automated checks enabled via `AccessibilityChecks.enable()` during instrumentation.

### Application core
- **Unit**: `SmartSalesApplicationTest` (Hilt entry point init), `MainActivityTest` (navigation host, permission prompts).
- **Integration**: End-to-end instrumentation from onboarding to first BLE scan, including permission grant/deny paths.

## 6. Manual Validation Checklist
- BLE pairing success/failure, reconnection after app kill, device rename persistence.
- Offline mode: cached conversations accessible, write operations queued.
- API failure recovery: display error surfaces, retry/backoff messaging.
- Localization: Chinese/English strings, currency formats.
- Permissions & privacy: runtime prompts, denied states, log scrubbing.
- Release readiness: `assembleRelease`, Play Store bundle sanity, screenshots for Documentation.

## 7. Reporting & Governance
- Update `SOFTWARE_QUALITY_METRICS_REPORT.md` with coverage %, lint debt, flaky tests each sprint.
- File bugs in audit log with reproduction steps, attach logs/tests.
- Maintain `VERIFICATION_CHECKLIST.md` per release, sign-off after manual QA.
- Host weekly quality review to triage failures, assign owners, and adjust plan.

## 8. Continuous Improvement
- Add property-based tests for BLE payload parsing.
- Instrument analytics for crash-free users; feed back into tests.
- Review dependencies quarterly; lock versions and rerun regression plan after upgrades.

