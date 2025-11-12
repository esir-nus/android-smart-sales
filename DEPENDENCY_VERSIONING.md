# Dependency & Versioning Rules

These guidelines keep every module reproducible, including standalone tester apps (`wifiBleTestApp`, `aiFeatureTestApp`), so hardware debugging and QA builds run with known library baselines.

## Toolchain Baseline
- **JDK:** 17.0.11+9 (or the bundled `./jdk-17.0.9+9` when offline). Point `JAVA_HOME`/`org.gradle.java.home` here.
- **Gradle:** Wrapper locked to **8.13**. Pre-download the distribution into `.gradle-cache/wrapper/dists` for air‑gapped work.
- **Android Gradle Plugin:** 8.2.x (see root `build.gradle.kts`).

Keep this section updated whenever a toolchain upgrade lands so downstream testers know what to install.

## Pin Everything
- Declare explicit versions for **every** Gradle dependency instead of relying on BOM defaults or transitive pulls. Use shared constants (e.g., `val composeUiVersion = "1.6.7"`) when multiple artifacts must stay in sync.
- Tester apps follow the same rule even if they only wrap internal libraries.
- When introducing shared constants, add them near the top of the module’s `build.gradle.kts` and mention them here.

### Current Shared Constants (2024-11)
| Constant | Version | Notes |
| --- | --- | --- |
| `composeUiVersion` | `1.6.7` | Used across `app`, `wifiBleTestApp`, `aiFeatureTestApp` for all `androidx.compose.ui` artifacts, including `foundation` so `nestedScroll` APIs resolve. |
| `composeMaterial3Version` | `1.2.1` | Primary Material 3 dependency. |
| `kotlinCompilerExtensionVersion` | `1.5.14` | Configured in `composeOptions` for all Compose-enabled modules. |
| `navigationComposeVersion` | `2.7.5` | Compose Navigation for in-app routing (now used by tester app for the Web Console screen). |
| `coroutinesVersion` | `1.7.3` | Used for async operations in mock implementations (`kotlinx-coroutines-core`, `kotlinx-coroutines-android`). |

Update this table whenever you bump a shared constant so reviewers have a single reference point.

## Document Deviations
- If a module needs a version that differs from the main `app`, add a brief inline comment in that module’s `build.gradle.kts` explaining the reason (e.g., requires a preview API or hardware‑specific fix).
- Record the deviation in this file under a dedicated subsection:

```
### Module Deviations
- `ai-core`: Alibaba SDK dependencies temporarily replaced with mock implementations due to version conflicts (2025-11-11). Real SDK coordinates: `com.alibaba:dashscope-sdk-java`, `com.aliyun:tingwu20220930`, `com.aliyun.dpa:oss-android-sdk`.
- `ai-core`: Commented out problematic Alibaba SDKs in `build.gradle.kts:44-49` and created mock implementations (`MockOssClient.kt`, `MockDashscopeChatClient.kt`, `MockDashscopeApiHelper.kt`) to maintain build functionality while preserving API interfaces for future migration back to real SDKs.
- `wifiBleTestApp`: `retrofit` pinned to 2.9.1 to consume experimental endpoint (2024-11-15).
```

## When Adding Libraries
1. Choose the version that matches the project’s Kotlin/AGP matrix.
2. Update the relevant `build.gradle.kts` file(s) with explicit numbers.
3. Mention any new constants or deviations in this document.
4. For tester apps, verify the dependency is still aligned with the main modules or explain why not.

Keeping this file current helps future agents know where a version was introduced, what toolchains to install, and how to resolve conflicts when Gradle upgrades are required. Feel free to expand it with module-specific notes (e.g., BLE SDK constraints) as dependencies evolve.

### AI SDK Baselines
- `com.alibaba.dashscope:dashscope-sdk-java:2.14.0` – shared via `:ai-core` for official Qwen chat streaming.
- `com.aliyun:tingwu-android-sdk:1.0.3` – `:ai-core` dependency for Tingwu transcription orchestration.
- `com.aliyun.oss:oss-android-sdk:2.9.13` – `:ai-core` dependency powering file uploads for Tingwu/AI attachments.

**Note**: Due to dependency resolution conflicts during build, mock implementations are currently used for testing. The actual SDK dependencies are temporarily commented out in `ai-core/build.gradle.kts` but the API interfaces remain consistent for easy migration back to real SDKs when dependency issues are resolved.

### Module Deviations
