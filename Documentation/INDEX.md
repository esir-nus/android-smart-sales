# Smart Sales Assistant - 文档索引

## 📚 完整文档列表

本项目包含8个核心文档，总计约 **188 KB** 的详细技术文档。

---

## 📖 主要文档

### 1. **README.md** (15 KB) - 📋 项目总览
**这是您应该首先阅读的文档！**

包含内容：
- 项目概述和核心特性
- 完整的技术栈说明
- 环境要求和安装步骤
- 实现指南（从数据层到UI层）
- 关键功能实现示例
- API配置说明
- 硬件设备集成指南
- 常见问题解答
- 性能优化建议
- 安全考虑
- 扩展功能建议

**适合**: 项目管理者、新加入的开发者、想要快速了解项目全貌的人

**开始阅读**: [README.md](README.md)

---

### 2. **QUICK_REFERENCE.md** (16 KB) - ⚡ 快速参考
**开发过程中最常查阅的文档！**

包含内容：
- 核心代码片段（复制即用）
- 数据库操作示例
- BLE连接代码
- WiFi文件同步代码
- AI聊天实现
- 音频转写流程
- PDF/CSV导出代码
- Compose UI模式
- 实用工具函数
- 常用数据结构
- 设计模式应用
- 性能优化技巧
- 调试技巧
- 生产环境检查清单

**适合**: 正在编码的开发者、需要快速查找API用法的人

**开始阅读**: [QUICK_REFERENCE.md](QUICK_REFERENCE.md)

---

## 🔧 技术模块文档

### 3. **smart_sales_data_layer.md** (25 KB) - 💾 数据持久化层
**第一个应该实现的模块！**

包含内容：
- Room数据库完整设计
- 6个实体表的定义
- DAO接口实现（6个）
- Room数据库配置
- Repository层实现
- 数据模型设计

**关键类**:
- `ConversationEntity`, `MessageEntity`, `AttachmentEntity`
- `WifiConfigEntity`, `DeviceSettingEntity`, `CrmExportEntity`
- `ConversationDao`, `MessageDao`, etc.
- `ConversationRepository`, `DeviceRepository`, `ExportRepository`

**适合**: 负责数据层的开发者

**开始阅读**: [smart_sales_data_layer.md](smart_sales_data_layer.md)

---

### 4. **smart_sales_wifi_sync.md** (25 KB) - 🔗 连接与同步
**实现设备通信的核心模块！**

包含内容：
- BLE服务定义和UUID
- BLE管理器实现
- WiFi配置传输协议
- HTTP API端点定义
- HTTP客户端实现
- 文件同步管理器
- 图片处理工具

**关键类**:
- `BleConstants`, `BleCommand`
- `BleManager`, `BleConnectionState`
- `GadgetHttpClient`, `FileSyncManager`
- `ImageProcessor`, `ImageValidationResult`

**适合**: 负责设备连接和文件同步的开发者

**开始阅读**: [smart_sales_wifi_sync.md](smart_sales_wifi_sync.md)

---

### 5. **smart_sales_ai_integration.md** (26 KB) - 🤖 AI服务集成
**实现智能功能的关键模块！**

包含内容：
- Qwen API配置
- Dashscope聊天客户端
- Tingwu转写客户端
- AI服务管理器
- 提示词模板系统
- 流式响应处理

**关键类**:
- `QwenConfig`
- `QwenChatClient`, `QwenTingwuClient`
- `AiServiceManager`, `ChatState`, `ChatMode`
- `PromptTemplates`

**关键功能**:
- 普通聊天 & 流式聊天
- 音频转写 & 说话人分离
- 客户分析
- CRM数据提取
- 会议摘要生成

**适合**: 负责AI集成的开发者

**开始阅读**: [smart_sales_ai_integration.md](smart_sales_ai_integration.md)

---

### 6. **smart_sales_export.md** (28 KB) - 📄 文档导出
**实现报告和数据导出功能！**

包含内容：
- PDF生成器实现
- CSV生成器实现
- 导出管理器
- 多种CRM格式支持
- FileProvider配置
- 文件分享功能

**关键类**:
- `PdfGenerator`, `PdfLine`, `LineType`
- `CsvGenerator`, `CrmFormat`
- `ExportManager`
- `CustomerData`

**支持导出**:
- 客户分析报告 (PDF)
- 会议纪要 (PDF)
- CRM数据 (CSV) - Salesforce/HubSpot/通用格式
- 对话记录 (CSV)
- 批量导出

**适合**: 负责数据导出功能的开发者

**开始阅读**: [smart_sales_export.md](smart_sales_export.md)

---

### 7. **smart_sales_ui.md** (32 KB) - 🎨 用户界面
**最大的模块，实现所有UI界面！**

包含内容：
- 主界面导航结构
- 聊天主界面
- 历史记录界面
- 设备管理界面
- 文件查看器
- ViewModel实现
- Compose最佳实践

**关键组件**:
- `SmartSalesApp`, `BottomNavigationBar`
- `ChatScreen`, `MessageBubble`, `ActionButtonRow`
- `HistoryScreen`, `ConversationItem`
- `DeviceManagementScreen`, `CurrentDeviceCard`
- `ChatViewModel`, `HistoryViewModel`, `DeviceViewModel`

**界面包括**:
- 5个主要标签页
- Material Design 3风格
- 深色模式支持
- 响应式布局
- 流畅动画

**适合**: 负责UI开发的前端开发者

**开始阅读**: [smart_sales_ui.md](smart_sales_ui.md)

---

### 8. **smart_sales_config.md** (21 KB) - ⚙️ 项目配置
**配置项目所需的所有文件！**

包含内容：
- Gradle配置文件
- AndroidManifest.xml
- Application类
- MainActivity实现
- Hilt依赖注入配置
- ProGuard规则
- 资源文件配置

**配置文件**:
- `build.gradle.kts` (Project & App)
- `AndroidManifest.xml`
- `SmartSalesApplication.kt`
- `MainActivity.kt`
- `AppModule.kt` (Hilt)
- `gradle.properties`
- `proguard-rules.pro`
- XML资源文件

**包含全部**:
- 依赖项配置
- 权限声明
- FileProvider设置
- 主题配置
- 字符串资源

**适合**: 项目初始化、配置管理

**开始阅读**: [smart_sales_config.md](smart_sales_config.md)

---

## 🎯 按角色推荐阅读顺序

### 👨‍💼 项目管理者 / 产品经理
1. **README.md** - 了解项目全貌
2. 快速浏览其他文档的"关键功能"部分

### 👨‍💻 后端开发者 / 全栈开发者
1. **README.md** - 项目概览
2. **smart_sales_data_layer.md** - 数据库设计
3. **smart_sales_ai_integration.md** - AI集成
4. **smart_sales_export.md** - 数据导出
5. **QUICK_REFERENCE.md** - 常用代码
6. **smart_sales_config.md** - 项目配置

### 📱 Android开发者 / 移动端开发者
1. **README.md** - 项目概览
2. **smart_sales_config.md** - 项目配置
3. **smart_sales_ui.md** - UI实现
4. **smart_sales_data_layer.md** - 数据层
5. **QUICK_REFERENCE.md** - 开发参考

### 🔧 硬件工程师 / 嵌入式开发者
1. **README.md** - 项目概览
2. **smart_sales_wifi_sync.md** - 通信协议
3. 查看 README.md 中的"硬件设备集成"章节

### 🧪 测试工程师 / QA
1. **README.md** - 了解功能
2. **QUICK_REFERENCE.md** - 查看API用法
3. 每个模块文档末尾的测试建议

### 🆕 新手开发者
1. **README.md** - 从头到尾阅读
2. **QUICK_REFERENCE.md** - 收藏以便查阅
3. 按实现顺序阅读技术文档
4. 遇到问题时查阅对应模块文档

---

## 📊 文档统计

| 文档 | 大小 | 行数 | 代码块数 | 主要内容 |
|------|------|------|----------|----------|
| README.md | 15 KB | ~550 | 20+ | 项目总览 |
| QUICK_REFERENCE.md | 16 KB | ~700 | 60+ | 代码片段 |
| data_layer.md | 25 KB | ~900 | 30+ | 数据库 |
| wifi_sync.md | 25 KB | ~850 | 25+ | 连接同步 |
| ai_integration.md | 26 KB | ~900 | 30+ | AI服务 |
| export.md | 28 KB | ~950 | 25+ | 文档导出 |
| ui.md | 32 KB | ~1100 | 40+ | UI界面 |
| config.md | 21 KB | ~800 | 35+ | 项目配置 |
| **总计** | **188 KB** | **~6750** | **265+** | - |

---

## 🔍 快速查找

### 我想实现某个功能，应该看哪个文档？

| 功能需求 | 推荐文档 |
|----------|----------|
| 数据库增删改查 | data_layer.md |
| BLE设备连接 | wifi_sync.md |
| 文件上传下载 | wifi_sync.md |
| AI聊天对话 | ai_integration.md |
| 音频转写 | ai_integration.md |
| 客户分析 | ai_integration.md |
| 生成PDF报告 | export.md |
| 导出CSV数据 | export.md |
| 聊天界面 | ui.md |
| 历史记录 | ui.md |
| 设备管理 | ui.md |
| 项目初始化 | config.md |
| 依赖配置 | config.md |
| 权限管理 | config.md |
| 代码示例 | QUICK_REFERENCE.md |
| 常见问题 | README.md |

### 我遇到了问题，应该看哪里？

| 问题类型 | 查找位置 |
|----------|----------|
| 无法编译 | config.md → Gradle配置 |
| 数据库错误 | data_layer.md → Room配置 |
| BLE连不上 | wifi_sync.md → BLE管理器 + README FAQ |
| AI调用失败 | ai_integration.md + README FAQ |
| PDF生成错误 | export.md → PDF生成器 |
| UI显示问题 | ui.md → Compose实现 |
| 权限问题 | config.md → AndroidManifest |
| 性能问题 | QUICK_REFERENCE.md → 性能优化 |
| 调试技巧 | QUICK_REFERENCE.md → 调试技巧 |

---

## 💡 使用建议

### 第一次接触项目
1. 先读 **README.md** 获得全局视角
2. 根据你的角色选择阅读路径
3. 收藏 **QUICK_REFERENCE.md** 以便开发时查阅

### 开始实际开发
1. 从 **config.md** 开始配置项目
2. 按照 README 中的实现顺序逐步开发
3. 遇到具体问题时查阅对应的技术文档
4. 使用 **QUICK_REFERENCE.md** 复制粘贴代码示例

### 代码审查 / 重构
1. 参考 **QUICK_REFERENCE.md** 中的设计模式
2. 检查是否遵循各文档中的最佳实践
3. 对照 **生产环境检查清单**

### 维护和扩展
1. 先查阅相关模块的技术文档
2. 参考 README 中的"扩展功能建议"
3. 保持代码风格与现有实现一致

---

## 📞 技术支持

### 在线资源
- **Android官方文档**: https://developer.android.com/
- **Jetpack Compose**: https://developer.android.com/jetpack/compose
- **Room数据库**: https://developer.android.com/training/data-storage/room
- **Qwen API**: https://dashscope.aliyun.com/
- **Kotlin协程**: https://kotlinlang.org/docs/coroutines-overview.html

### 相关技术栈
- **Hilt**: https://developer.android.com/training/dependency-injection/hilt-android
- **OkHttp**: https://square.github.io/okhttp/
- **Kotlinx Serialization**: https://github.com/Kotlin/kotlinx.serialization

---

## 🔄 文档更新

**当前版本**: 1.0.0  
**最后更新**: 2025年11月03日  
**状态**: ✅ 完整发布

### 版本历史
- **v1.0.0** (2025-11-03) - 初始发布，完整的8个文档

---

## ✅ 下一步行动

1. ✅ 已完成：全部8个核心文档
2. ⏭️ 建议：开始实际编码实现
3. 📋 TODO：根据实际硬件调整API接口
4. 🔧 TODO：配置Qwen API密钥
5. 🎨 TODO：根据设计稿调整UI主题

---

**开始您的开发之旅！** 🚀

选择一个文档开始阅读，或者直接查看 [README.md](README.md) 开始！

---

**文档索引制作日期**: 2025年11月03日  
**总文档大小**: 188 KB  
**总代码示例**: 265+  
**预计阅读时间**: 4-6小时（全部文档）
