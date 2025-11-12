Here’s an **enhanced TDD plan** with room for flexibility while enforcing consistency and accuracy.

## Principles (overarching rules)

* **Ports-first**: Code to **stable interfaces**; vendors live behind adapters.
* **Red–Green–Refactor**: Each change starts with a failing test, ends with cleanup.
* **Android-first reality**: Prove **release build + device** early and often.
* **Determinism**: Virtual time for polling/streams; no sleeps in tests.
* **Minimal surface**: Only expose domain types; never leak SDK classes.

## Scope & Interfaces (fixed)

Define ports in `:ai-core`:

* `ChatPort.stream(prompt): Flow<Delta>`
* `TranscriptionPort.start()/poll(id): Result`
* `ObjectStorePort.put(key, bytes): PutResult`

Adapters (DashScope/Tingwu/OSS) implement these ports; app/ViewModel depends on ports only.

## Phases (flexible inside each)

**0) Compat Spike (1 day, mandatory)**

* Add correct Maven coords; compile **Release**; run a tiny call on emulator.
* Add minimal keep rules/excludes if needed.

**1) TDD Baseline (unit)**

* Write failing tests for each port: success, timeout, retriable vs fatal, cancellation, progress.
* Provide fakes (no SDKs). Make tests pass.

**2) Adapter Integration**

* Implement SDK adapters mapping to domain; centralize retry/backoff/timeouts.
* Add adapter-level tests (using SDK facades or thin stubs).

**3) Contract Tests (record–replay)**

* Record sanitized real responses once; replay in CI to catch schema drift.
* Keep fixtures small and explicit.

**4) Android Smoke (Release)**

* Instrumented/smoke tests: create client, perform minimal call per SDK in **Release** (R8 on).
* One UI test per flow: stream updates, polling transitions, upload progress/cancel.

**5) CI & Docs**

* CI jobs: `lint`, unit, replay, assembleRelease, smoke.
* Lock dependencies; document coords/mirrors/keeps/excludes and a flatDir AAR fallback.

## Guardrails (necessary limitations)

* No SDK types outside adapters.
* No real-network in unit tests; contract tests only record/replay.
* No sleeps; use `kotlinx.coroutines.test` virtual time.
* Secrets only via env/`local.properties`/Keystore; never in repo.
* Break build on new dependency without lock update.

## Flex points (where spontaneity lives)

* Adapter internals (e.g., chunking, progress fan-out) can evolve if port tests stay green.
* Retry/backoff policy tunables via config.
* Choice of mocking library, test data builders, and fixture layout.

## Definition of Done

* Release build runs on device; minimal real call per SDK works.
* Ports have full unit coverage across success/timeout/retry/cancel.
* Contract tests replay green; fixtures curated.
* ViewModels react correctly to stream/poll/upload; progress/cancel visible.
* Dependencies locked; rules/exceptions documented.
