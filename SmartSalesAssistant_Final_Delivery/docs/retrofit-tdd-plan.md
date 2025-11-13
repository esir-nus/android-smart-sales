# Retrofit TDD Plan (KISS)

## Module map
- `:app` – Compose UI, pairing + chat flows, thin repositories wired into BLE/AI.
- `:device-connectivity` – BLE orchestration, Room storage, gadget Retrofit clients.
- `:ai-core` – Dashscope/OpenAI adapters + OSS utilities.
- `:core:util` – logging, telemetry, and shared coroutine tools.
- `:wifiBleTestApp` – provisioning sandbox that exercises BLE + Wi-Fi hand-off.
- `:aiFeatureTestApp` – experimental AI scenarios / smoke apps.

## Domain ports to introduce
- **BleBoundedScanPort** – `suspend fun boundedScan(request: ScanRequest, timeoutMs: Long): ScanOutcome`, `fun stop(reason: ScanStopReason): Unit`.
- **WifiConnectionPort** – `suspend fun connectAndVerify(ssid: String, password: String, deadlineMs: Long): WifiConnectionResult`, `suspend fun detectCaptivePortal(host: String): CaptivePortalState`.
- **AiOrchestratorPort** – `suspend fun runSalesAssistant(prompt: AiPrompt): AiReply`, `suspend fun summarizeConversation(messages: List<AiMessage>): AiSummary`.

## Top 10 test-first tasks (module · test path)
1. `:device-connectivity · device-connectivity/src/test/java/com/smartsales/business/bluetooth/BleBoundedScanTest.kt` – prove scan job stops after N windows without target.
2. `:device-connectivity · device-connectivity/src/test/java/com/smartsales/business/bluetooth/BlePermissionFallbackTest.kt` – assert permission denial surfaces `BleError.PermissionDenied`.
3. `:device-connectivity · device-connectivity/src/test/java/com/smartsales/data/network/ConnectivityTimeoutTest.kt` – ensure Wi-Fi connect attempts fail fast (<5s) when captive portal suspected.
4. `:device-connectivity · device-connectivity/src/test/java/com/smartsales/data/local/database/SmartSalesDatabaseMigrationTest.kt` – cover 1→2 migration retaining wifi configs.
5. `:wifiBleTestApp · wifiBleTestApp/src/test/java/com/smartsales/wifibletest/ui/CaptivePortalViewModelTest.kt` – flag captive portal mismatch in `WifiBleTestViewModel`.
6. `:app · app/src/test/java/com/smartsales/ui/screens/device/DevicePairingStateTest.kt` – lock regression where error banner clears once scan resumes.
7. `:app · app/src/test/java/com/smartsales/ui/screens/chat/ChatViewModelAiAnalysisTest.kt` – require AI insights list to be populated instead of TODO.
8. `:ai-core · ai-core/src/test/kotlin/com/smartsales/data/network/dashscope/DashscopeBackoffTest.kt` – capture retry/backoff contract for rate limits.
9. `:core:util · core/util/src/test/java/com/smartsales/core/util/logging/AndroidLoggerLeakTest.kt` – verify `Redactor` scrubs Wi-Fi credentials before `Log`.
10. `:aiFeatureTestApp · aiFeatureTestApp/src/test/java/com/smartsales/aitest/OfflineInferenceFallbackTest.kt` – pin behavior when Tingwu API key missing.

## Bug→test SOP
- Capture repro + logs, restate as single assertion, and freeze it in the correct module’s test tree before touching prod code.
- Make the new test red by modeling the failing state with fakes/stubs; no direct device toggles in tests.
- Only start implementation once the test names the bug; keep the red test in history even if the bug feels “obvious”.

## Definition of done
- Targeted unit/integration tests red→green, plus surrounding regression suite still green.
- Lint/Detekt/ktlint baselines untouched; no new warnings.
- Feature flagged/telemetry hooks documented in tasklist with reviewer notes.
- Observability (logs, metrics, state exposure) updated to explain failures in-field.

## Success metrics
- ≥80% of new BLE/Wi-Fi code paths covered by deterministic JVM tests.
- Scan-to-connect median time reduced by >20% (bounded scans + fast Wi-Fi timeout).
- Zero destructive Room migrations after retrofit; schema diffs reviewed via tests.
- ≤1% of crash reports reference `Log.*` fallback (AndroidLogger in use).
- Bug queue burn-down: at least one red item closed per iteration.

### What I inferred from the code
- BLE logic still runs on a long-lived IO scope unattached to lifecycle; needs port abstraction.
- Wi-Fi provisioning relies on shared prefs and blocking network calls directly in the ViewModel.
- AI features lean on Dashscope clients but UI still ships TODO placeholders for insights.
- Room layer is centralized in `device-connectivity`, so migrations there unblock all modules.
- Quality gates already wired in root `build.gradle.kts`; keep toolchain stable per Phase 0.

### Unknowns / need confirmation
- Do we have hardware constraints that force `SCAN_SERVICE_UUIDS` to stay empty?
- Is there an approved captive-portal detection heuristic from product?
- Should Wi-Fi credentials transition to Android Keystore or stay in shared prefs for now?
- Are AI requests expected to run offline via on-device model fallback?
- Can we introduce new gradle test fixtures for BLE fakes without violating toolchain freeze?
