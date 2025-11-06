# Smart Sales Assistant - Android Application

## Overview
Smart Sales Assistant is a modern Android application featuring AI-powered chat, Bluetooth Low Energy device pairing, file synchronization, and data export capabilities.

## Features
- ✅ AI Chat Integration (Qwen)
- ✅ Conversation Management
- ✅ BLE Device Pairing
- ✅ File Synchronization
- ✅ PDF/CSV Export
- ✅ Multi-language Support

## Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- JDK 11 or 17
- Gradle 8.2+
- Android SDK 26+ (Target: 34)

## Quick Start
1. Open in Android Studio
2. Configure API keys in `local.properties`
3. Sync Gradle
4. Build and run

## China Region Setup Tips
- Use the bundled JDK (`./jdk-17.0.9+9`) by keeping `org.gradle.java.home=./jdk-17.0.9+9` in `gradle.properties`; no external download required.
- Android Maven mirrors for mainland China (Aliyun) are pre-registered in `settings.gradle.kts`. If sync still times out, temporarily disable the default `google()`/`mavenCentral()` entries or add a VPN.
- Prime the Gradle cache before travelling by running `./gradlew --refresh-dependencies assembleDebug` on a fast network, then keep `GRADLE_USER_HOME` pointed at a persistent disk.
- Configure system proxies when needed via `gradle.properties` (e.g., `systemProp.https.proxyHost`) so Hilt/Compose artifacts resolve.
- Dashscope/Tingwu endpoints are mainland-friendly, but third-party crash reporting or analytics should be reviewed before enabling them in China.

## Architecture
- MVVM Pattern
- Clean Architecture
- Jetpack Compose UI
- Hilt Dependency Injection
- Room Database
- Retrofit + OkHttp

## Modules
- `app` — Mother application combining AI chat, device connectivity, and data workflows.
- `device-connectivity` — Shared BLE/Wi-Fi connectivity toolkit consumed by the app and connectivity tester.
- `ai-core` — Shared Dashscope/Tingwu networking layer reused across AI surfaces.
- `wifiBleTestApp` — Standalone connectivity tester for BLE pairing and gadget file browsing.
- `aiFeatureTestApp` — Standalone AI playground covering Qwen chat and Tingwu transcription.

## Version
1.0.0 - Initial Release

For detailed documentation, see the Documentation folder.
