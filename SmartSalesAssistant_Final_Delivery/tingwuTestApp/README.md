<!-- 文件路径: tingwuTestApp/README.md -->
<!-- 文件作用: 介绍 Tingwu 专用测试 App -->
<!-- 文件目的: 指导如何调试签名串、参数与官方文档保持一致 -->
<!-- 相关文件: docs/current-state.md, aiFeatureTestApp/README.md, reference-source/relevant_map.json -->

# Tingwu Test App

该模块与 `:aiFeatureTestApp` 并列，专注验证 Tingwu HTTP API 与签名流程，完全遵循阿里云官方 SDK（`V2.7.0-039-20251010_Android_OpenSSL/example/.../token/HttpRequest.java`）的最佳实践：

- **唯一职责**：在真机上完成“选择本地音频→上传 Aliyun OSS→提交 Tingwu 转写”的两步管线，并校验 ROA 签名。
- **操作流程**：
  1. 点击“选择音频并上传 OSS”，应用会缓存本地文件、走 `RealOssUploadClient` 上传，并生成可复制的临时 URL。
  2. 选择语言/TaskKey，直接点击“提交转写任务”即可触发 `RealTingwuCoordinator`，无需再手动填写 URL。
- **可视化信息**：新增“运行诊断”面板，以及分级的“详细日志”视图，对凭据加载、OSS 上传、Tingwu 请求、Job 轮询、URL 有效期等关键点做 SUCCESS/WARNING/ERROR 标记；任务卡会展示实时状态、最近的 Markdown 段落，并支持一键复制完整转写内容；继续输出 `baseUrl`、脱敏后的 `AppKey` 和签名日志，方便对照 Tingwu 后台的 "string to sign"。
- **默认配置**：`TingwuTestAiCoreOverrides` 强制 `preferFakeTingwu=false`、开启 `enableTingwuHttpLogging` 与 `tingwuVerboseLogging`，并将轮询窗口调大（120s），便于真机测试长音频。
- **权限/网络**：`AndroidManifest.xml` + `networkSecurityConfig` 允许 HTTP 明文，便于调试内网 OSS Gateway；生产环境务必切换回 HTTPS。

## 构建与运行
- 构建命令：`./gradlew :tingwuTestApp:assembleDebug`
- 安装后直接启动 `TingwuTestActivity`，确保 `local.properties` 配置了 `TINGWU_APP_KEY`、`TINGWU_ACCESS_KEY_ID`、`TINGWU_ACCESS_KEY_SECRET`、`TINGWU_BASE_URL`。
- Logcat 过滤 `SmartSalesAi/Tingwu` 或 `SmartSalesTest/Tingwu` Tag，可同步看到拦截器输出的签名串。

## 后续增强
- 支持多段音频排队上传 / 并行提交，方便回归测试。
- 提供 "官方示例参数" 预设，覆盖更多语言、参数组合（如 `TranscriptionEnabled=false`）。
- 将诊断日志导出为 Markdown，方便 QA 附件复现。
