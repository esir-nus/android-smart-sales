# AI Feature Test Child App Guide

## Overview
`aiFeatureTestApp` is a standalone Android application dedicated to validating the Dashscope (Qwen) chat workflow and Tingwu transcription pipeline in isolation from the production app. It consumes the shared `:ai-core` module so that QA teams can exercise cloud integrations without running the full Smart Sales Assistant experience.

## Build & Run
```bash
cd SmartSalesAssistant_Final_Delivery
./gradlew :aiFeatureTestApp:assembleDebug
```
The generated APK is located at `aiFeatureTestApp/build/outputs/apk/debug/aiFeatureTestApp-debug.apk`.

### Prerequisites
- Configure `DASHSCOPE_API_KEY` and `TINGWU_API_KEY` in `local.properties` (copy from `local.properties.example`).
- Provide a publicly reachable audio file URL when testing Tingwu. The default placeholder (`https://example.com/audio/sample.mp3`) must be replaced with a real asset.

## Features
- **Qwen Chat Playground**
  - Edit the system prompt and send multi-turn messages.
  - Displays assistant replies using the default `qwen-turbo` model.
  - Conversation reset capability for repeated test scenarios.
- **Tingwu Transcription Harness**
  - Submit remote audio files for offline transcription.
  - Configure speaker diarization and speaker count.
  - Polls Tingwu until completion and renders formatted results with timestamps and speaker labels.

## Error Handling & Status Messages
- Chat and transcription errors raise a snackbar and keep detail text visible in the UI.
- Transcription status updates remain within the Tingwu card for quick diagnostics.
- All network operations run within `AiTestViewModel`, which exposes state via `StateFlow` for deterministic UI testing.

## Shared Module Dependencies
- Relies on `:ai-core` for Hilt network modules, Retrofit interfaces, and data models.
- No connectivity or storage permissions beyond `INTERNET` are required.

## Testing Notes
- Mock responses can be injected by swapping `DashscopeApi`/`TingwuApi` with test doubles via Hilt's `@TestInstallIn` mechanism.
- The UI uses Jetpack Compose and can be inspected with `ui-tooling` previews; add sample state in previews if required.

## Next Steps
- Hook telemetry events or log capture if long-running Tingwu tasks need auditing.
- Extend the playground with streaming chat (SSE) or advanced Tingwu features (translation, summarisation) as product scope expands.
