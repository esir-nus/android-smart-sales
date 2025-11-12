## AI Child Project – Checkpoint

**Scope reminder:** This note tracks the AI-focused child project. It complements `notes/chopper_checkpoint.md` (WiFi/BLE) so we can merge both streams into the mother project later.

### What’s in place
- **Contracts:** Core interfaces for chat, Tingwu orchestration, Ali OSS uploads, structured markdown export, media preprocessing, and session persistence now live under `app/src/main/java/com/smartsales/ai`.
- **Navigation + UI skeleton:** Chat, structured-output, history, and drawer screens mirror `AI_UI_specifications.md`, with ViewModels emitting the state needed for skill chips, attachment flow, sticky headers, and export buttons.
- **State model:** `AiChatViewModel` tracks task modes, attachments, streaming responses, and structured-output triggers while persisting sessions through `AiSessionRepository`.
- **Service scaffolding:** `DashscopeAiChatService` streams markdown chunks from the existing Dashscope Retrofit client, `DefaultMediaPreprocessor` enforces the mp3/image whitelist, `SimpleExportManager` writes placeholder PDF/CSV/mind-map files, and the DI module now exposes everything via `AiFeatureGraph`. Tingwu/OSS routes are stubbed (`NoopTingwuCoordinator`, `NoopOssRepository`) until we wire the official Ali SDKs.
- **Persistence + navigation:** AI chat/history now live on their own Room-backed repository (`AiSessionDatabase` + `RoomAiSessionRepository`), the conversation list exposes an “AI 助手” shortcut, and the nav graph hosts chat, structured, and history destinations plus the Compose drawer defined in `AI_UI_specifications.md`.

### Pending integrations
1. **Official Ali SDKs**
   - *DashScope (Qwen)*: Use Ali’s official Android/JVM client so `AiChatService` can stream markdown chunks with reliable token accounting.
   - *Tingwu*: Adopt the Tingwu job SDK for transcription/mind-map outputs; we’ll pipe progress into `TingwuCoordinator`.
   - *Ali OSS*: Use the OSS Android SDK for uploads/download URLs in `OssRepository`.
2. **File picker / media prep**
   - Hook the upload button to the platform picker (mp3 + images). Once a URI is returned, call `MediaPreprocessor.prepare(uri)` so we can compress/convert before uploading or sending to DashScope.
3. **Export pipelines**
   - Replace the placeholder `SimpleExportManager` with real PDF (Compose-to-PDF), CSV, and mind map generators once we finalize the markdown schema.
4. **Session storage**
   - ✅ Done via `AiSessionDatabase`. Future work: add migrations + backup/export support if other modules need to query the same data.

### Collaboration checklist
- I’ll code against the official SDK artifacts; if they aren’t mirrored in the workspace, please download/provide them (Maven coordinates or local AARs) so Gradle can resolve them offline.
- Provide `DASHSCOPE_API_KEY`, `TINGWU_API_KEY`, and Ali OSS credentials in `local.properties` as already documented; let me know if a different secrets management flow is required.
- When Tingwu or OSS endpoints require whitelisting (IP/domain), share the constraints ahead of time so we can design retries and proxy settings before wiring the network layer.

Use this file to brief future sessions ("Chopper-AI") the same way we use `chopper_checkpoint.md` for connectivity work.
