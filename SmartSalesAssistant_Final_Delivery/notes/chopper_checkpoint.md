## WiFi/BLE Tester – Chopper Checkpoint

> Scope Note: this checkpoint file is strictly for coding assistance and project development. Non-development requests (learning exercises, copywriting, etc.) should be handled elsewhere.

**Context (BLE + WiFi workflow)**  
- BleManager now supports multiple service profiles (legacy SmartSales, Nordic UART, HM-10) and emits transport/network info streams while limiting scans strictly to the `BT311` device. The scanner retries automatically in short windows, and state messaging collapses to “Searching” vs “BT311 已找到”.  
- When `BT311` appears, the UI surfaces a single connect button for that device; once connected a dedicated Stop button can terminate the BLE link, and the Bluetooth icon turns blue whenever a session is active.  
- Credential and query payloads switched to the new protocol: the app sends `wifi#connect#<name>#<password>` when pushing WiFi credentials, issues a fixed `wifi#address#ip#name` query string, and expects the reply payload to be `wifi#address#<ip>#<name>` (gadget populates the fields). The returned `<ip>` (which can include schemes/ports) is normalized to `http://<host>:8000/` automatically for HTTP actions, while `<name>` is treated as the user-friendly Wi-Fi name for matching.  
- BLE traffic (sent/received) remains logged via `BleTransportEvent` for UI inspection and debugging.

**UI / ViewModel updates**  
- WifiBleTestViewModel observes transport/network flows, stores gadget IP/name, keeps the phone’s current Wi-Fi name, and surfaces a match flag (mismatch triggers a prompt to re-enter credentials).  
- WifiBleTestScreen keeps the BT311-focused workflow but now launches a dedicated Web Console route when the user taps “打开全屏 Web 控制台.” It still shows network tools, Wi-Fi config chips, and BLE logs inline.  
- WebConsoleScreen renders the gadget portal full screen with navigation chrome, refresh, and “open in browser” actions. The ViewModel persists the last URL so the dashboard card can show/clear it.

**Key files touched**  
- `device-connectivity/src/main/java/com/smartsales/business/bluetooth/BleManager.kt` (new helpers, streams).  
- `device-connectivity/src/main/java/com/smartsales/business/bluetooth/BleConstants.kt`, `BleTransportEvent.kt`, `BleNetworkInfo.kt`.  
- `wifiBleTestApp/src/main/java/com/smartsales/wifibletest/ui/WifiBleTestViewModel.kt`, `WifiBleTestScreen.kt`, and `NetworkSections.kt`.  
- `wifiBleTestApp/src/main/java/com/smartsales/wifibletest/data/WifiConfigRepository.kt` auto-updates IP from network responses.

**Next actions**  
1. Build & smoke-test on device (`./gradlew wifiBleTestApp:assembleDebug`, reinstall).  
2. Verify BLE scan (only `BT311`, auto-retry) → connect button → Wi-Fi credential send `wifi#connect#<name>#<password>` → fixed `wifi#address#ip#name` query → parse `wifi#address#<ip>#<name>` and confirm phone/gadget Wi-Fi names match → open the full-screen Web 控制台 (loads `http://<ip>:8000/`).  
3. Adjust parsing/UI once real gadget responses are known (e.g., different delimiters, extra fields).  
4. Consider persisting BLE logs or filtering to avoid noise if logs become too chatty.  
5. Full-screen Web 控制台 now lives on its own navigation route inside `wifiBleTestApp`; use the dashboard card to open/clear the URL and verify the WebConsole screen (`WebConsoleScreen.kt`) renders correctly.  

---

### Latest checkpoint (BLE permission guard + data fixes)

**What changed this round**
- `BleManager.startScan()` now hard-checks `BLUETOOTH_SCAN` (or fine location on <S); without it we raise `BleConnectionState.Error` instead of letting the platform throw. `DevicePairingViewModel` and the WiFi/BLE tester show “请先授予蓝牙扫描权限” if the guard fails.  
- Chat/conversation ViewModels cancel prior Flow collectors and Chat now includes the freshly sent user message when building the Dashscope history (prevents blank AI replies on first turn).  
- `DeviceRepository` now encrypts saved Wi-Fi passwords (uses `WifiConfigEntity.create`), updates the exact record in `markWifiConfigAsUsed`, and persists synced file IDs via `SharedPreferences` so File Sync doesn’t redownload after process death.  
- Added unit scaffolding (kotlinx-coroutines-test + inline Mockito) and a new `DevicePairingViewModel` test asserting the permission gate behaviour.

**Action items for next session**
1. Set `JAVA_HOME=/home/cslh-frank/jdks/jdk-17.0.11+9` (or update `org.gradle.java.home`) before running `./gradlew testDebugUnitTest`; the previous attempt failed because no JDK was visible.  
2. Re-run `./gradlew testDebugUnitTest` to confirm the new ViewModel test passes.  
3. Manual sanity pass on device/emulator: trigger scan without permissions to ensure the new error surfaces, then grant permissions and confirm scanning still works.  
4. Verify Device Sync still marks files as synced across app restarts (check the SharedPreferences-backed cache).  
5. If time allows, add coverage for the Chat/Conversation Flow-job fixes (e.g., ensure only one collector at a time).  

Keep using this checkpoint file to brief “Chopper” on outstanding items before starting a new session.
