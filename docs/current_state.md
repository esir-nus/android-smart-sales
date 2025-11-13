# Current State

## Modules
- `:app` – Compose UI + Hilt view-models, houses AI session Room DB and crash handler.
- `:device-connectivity` – BLE manager, Room DAOs/entities, Retrofit gadget API clients.
- `:ai-core` – Dashscope/OpenAI clients, OSS upload helpers.
- `:core:util` – logging/redaction utilities shared across modules.
- `:wifiBleTestApp` – standalone BLE/Wi-Fi provisioning demo with shared prefs.
- `:aiFeatureTestApp` – sandbox for AI flows (audio analysis + OSS smoke tests).

## Tests on record
- `:app` unit tests: BLE constants, API config/interceptor, DevicePairingViewModel; UI tests under `app/src/androidTest`.
- `:device-connectivity` currently lacks JVM tests (new failures added in this retrofit).
- `:wifiBleTestApp` has `WifiBleTestViewModelTest` covering permission gating only.
- `:ai-core` offers `DefaultDashscopeChatClientTest` (network retries); `:core:util` has `RedactorTest`.
- `:aiFeatureTestApp` contains three JVM tests for OSS upload + audio analysis guards.

## Room schemas
- `SmartSalesDatabase` (`device-connectivity`, version 1, 6 entities) uses `fallbackToDestructiveMigration()` and exports no schema; DAOs cover conversations, wifi configs, CRM exports.
- `AiSessionDatabase` (`app`, version 1) stores AI session transcripts without migrations or encryption.

## Direct `Log.*` call sites
- `app/src/main/java/com/smartsales/SmartSalesApplication.kt:34` falls back to `Log.e` inside crash handler.
- `device-connectivity/src/main/java/com/smartsales/business/bluetooth/BleManager.kt` mixes `Log.d/w/e` with `Loggers`.
- `core/util/src/main/java/com/smartsales/core/util/logging/AndroidLogger.kt` still bridges to `Log.*` (wrapped but unredacted call arguments would leak if `Redactor` missed fields).

## Obvious risk spots
- Infinite BLE scan windows (`BleManager.startScan`) never stop when target absent; `SCAN_SERVICE_UUIDS` is empty so every advertisement is processed.
- `WifiBleTestViewModel.detectServerType` performs blocking `HttpURLConnection` work on `viewModelScope` (Main dispatcher) and lacks captive-portal heuristics.
- Room layer ships with destructive migrations only; credentials/history are wiped on upgrade and there is no schema dump to validate future changes.
- Saved Wi-Fi configs persist plaintext passwords in shared prefs via `WifiConfigRepository`, no Android keystore tie-in.
