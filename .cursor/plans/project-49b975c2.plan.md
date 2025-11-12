<!-- 49b975c2-b350-4a44-9c04-0fd06ba54ced a56fc72b-7c8b-4a7d-aedf-558915b83adb -->
# AI Ports TDD Retrofit

1. **Rollback Spike Changes** – remove direct DashScope SDK wiring so the codebase returns to the previous Retrofit-based behavior.
2. **Define Ports & Tests** – add `ChatPort`, `TranscriptionPort`, and `ObjectStorePort` interfaces in `:ai-core` with failing unit tests covering success/error/timeout/cancel.
3. **Adapter Implementations** – implement DashScope/Tingwu/OSS adapters behind those ports, driven by the new tests, isolating SDK classes.
4. **Contract & Smoke Prep** – set up record/replay scaffolding, adjust docs/dependency locks, and outline release smoke steps for later execution.