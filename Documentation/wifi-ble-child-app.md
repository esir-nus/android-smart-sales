# WiFi/BLE Child Project Playbook

This document captures the agreed approach for extracting the WiFi and BLE functionality into a detachable child project that produces its own APK while remaining easy to merge back into the main `android-smart-sales` app.

---

## 1. High-Level Goals
- **Shared Core Logic**: Move BLE and WiFi code into a reusable Android library module (`:device-connectivity`) so both the main app and the child app compile against the same sources.
- **Standalone Test App**: Introduce a lightweight application module (`:wifiBleTestApp`) that depends only on the connectivity library to exercise BLE scanning/connection and WiFi provisioning workflows on-device.
- **Seamless Reintegration**: Keep module boundaries clear, avoid package moves that break existing imports, and document Gradle/settings updates so the child app can be included or removed without churn.
- **Branch Strategy**: Perform work on `wifi/ble`, preserving history and making it easy to review/merge.

---

## 2. Module Layout After Extraction

```
SmartSalesAssistant_Final_Delivery/
├─ settings.gradle.kts
├─ app/                         (main production app)
├─ device-connectivity/         (new Android library)
│  ├─ src/main/java/com/smartsales/business/bluetooth/...
│  ├─ src/main/java/com/smartsales/data/local/{entity,dao,repository}/...
│  ├─ src/main/java/com/smartsales/data/network/api/GadgetApi.kt
│  ├─ src/main/java/com/smartsales/data/network/model/... (WiFi/BLE-related)
│  ├─ src/main/java/com/smartsales/di/{BleModule,RepositoryModule,DatabaseModule}
│  └─ build.gradle.kts
└─ wifiBleTestApp/              (new standalone Compose app)
   ├─ src/main/AndroidManifest.xml
   ├─ src/main/java/com/smartsales/wifibletest/MainActivity.kt
   ├─ src/main/java/com/smartsales/wifibletest/ui/WifiBleTestScreen.kt
   ├─ src/main/res/...
   └─ build.gradle.kts
```

**Key notes**
- Keep package names identical to existing classes so DI bindings and Room schemas migrate with minimal refactors.
- Expose only the components the child app needs (BLE manager, Device repository, Room database, Retrofit API). Broader application logic stays inside `:app`.

---

## 3. Implementation Checklist

### A. Preparation
- [ ] Branch off `main` into `wifi/ble`.
- [ ] Snapshot current builds (`./gradlew :app:assembleDebug`) as a baseline.

### B. Create Shared Library `:device-connectivity`
- [ ] Add new Android library module (API level + Kotlin version aligned with `:app`).
- [ ] Move BLE sources (`business/bluetooth/*`) into the library.
- [ ] Relocate WiFi entities, DAO, and repository implementations used by BLE workflows.
- [ ] Relocate Hilt modules (`BleModule`, relevant parts of `RepositoryModule`, `DatabaseModule`) or split them to keep unrelated bindings in `:app`.
- [ ] Update package imports in `:app` if paths change.
- [ ] Add public-facing API surface documentation inside the library README.

### C. Child App `:wifiBleTestApp`
- [ ] Generate an Android application module using Compose + Hilt.
- [ ] Declare dependency on `:device-connectivity`.
- [ ] Provide minimal DI bootstrap (Application class with `@HiltAndroidApp`, module wiring).
- [ ] Implement Compose UI for:
  - BLE scanning/connection state display
  - WiFi credential input + submission via `BleManager`
  - Basic log/status presentation
- [ ] Ensure manifest includes required permissions (Bluetooth, location, WiFi).
- [ ] Provide localized strings/resources as needed for testing.

### D. Gradle & Configuration
- [ ] Update `settings.gradle.kts` to include both new modules.
- [ ] Align `build.gradle.kts` plugin versions, Kotlin options, and compose compiler settings.
- [ ] Confirm Room schema location updates (shared library) to avoid annotation processor errors.
- [ ] Adjust Hilt aggregating task outputs if necessary.

### E. Verification
- [ ] Run `./gradlew :device-connectivity:assemble` (or `assembleDebug`) to ensure the library compiles.
- [ ] Run `./gradlew :app:assembleDebug` to ensure the main app still builds.
- [ ] Run `./gradlew :wifiBleTestApp:assembleDebug` to produce the standalone APK.
- [ ] (Optional) Execute targeted unit tests for BLE/WiFi logic (`:device-connectivity:testDebugUnitTest`).
- [ ] Document APK output path (likely `wifiBleTestApp/build/outputs/apk/debug/wifiBleTestApp-debug.apk`).

### F. Documentation & Handoff
- [ ] Update/verify this playbook with actual locations and commands.
- [ ] Add README snippet or link inside `README_DELIVERY.md` referencing the child project steps.
- [ ] Push branch `wifi/ble` with commit history and open PR if needed.

---

## 4. Reintegration Guide

1. **Including the Test App**
   - Ensure `include(":wifiBleTestApp")` remains in `settings.gradle.kts`.
   - Keep the module listed in the root `build.gradle.kts` dependency graph only when test builds are required. Removing the child app simply involves removing the include line and any referencing dependencies.

2. **Sharing Connectivity Logic**
   - Both `:app` and `:wifiBleTestApp` depend on `:device-connectivity`.
   - Any updates to BLE/WiFi flows should happen inside the shared module to avoid drift.
   - When introducing new dependencies, update the library Gradle file and confirm the main app mirrors anything it requires.

3. **Feature Development Workflow**
   - Develop and validate connectivity features inside `:device-connectivity`.
   - Use `:wifiBleTestApp` to verify device interactions quickly.
   - Once stable, integrate UI hooks inside the main `:app` module as needed.

4. **Merging Back to `main`**
   - Run full lint/test suite (`./gradlew lint testDebugUnitTest :wifiBleTestApp:assembleDebug`).
   - Prepare a PR summary referencing this document and outline manual test results.
   - After merge, teammates can either keep the child app included for regression coverage or comment out its `include` entry if not required.

---

## 5. Future Enhancements (Optional)
- Instrumentation tests using fake BLE devices or dependency injection for headless validation.
- Telemetry hooks in the child app to record test results, stored locally or exported.
- Script to toggle child app inclusion (e.g., Gradle property `-PincludeWifiBleTestApp=false`).

---

## 6. Quick Commands Reference
- Assemble main app: `cd SmartSalesAssistant_Final_Delivery && ./gradlew :app:assembleDebug`
- Assemble child APK: `cd SmartSalesAssistant_Final_Delivery && ./gradlew :wifiBleTestApp:assembleDebug`
- Run library unit tests: `cd SmartSalesAssistant_Final_Delivery && ./gradlew :device-connectivity:testDebugUnitTest`
- Clean: `cd SmartSalesAssistant_Final_Delivery && ./gradlew clean`

Keep this playbook updated alongside implementation to ensure context remains accurate for future AI or human contributors revisiting the `wifi/ble` branch.

---

## 7. Current Branch Snapshot (`wifi/ble`, in-progress)
- Shared library module `:device-connectivity` now owns BLE APIs plus the gadget Retrofit client and Hilt bindings; `:app`, `:wifiBleTestApp`, and `:aiFeatureTestApp` consume it directly.
- `:wifiBleTestApp` includes BLE scanning, Wi-Fi provisioning, and HTTP file browsing with dynamic IP/port entry and status reporting.
- Project settings include the new modules; `:app` depends on `:device-connectivity` and `:ai-core` for consolidated logic.
- Pending manual verification: execute `./gradlew :device-connectivity:assembleDebug :wifiBleTestApp:assembleDebug` on a workstation with BLE hardware access and configured gadget endpoints.
