# 占位符管理清单 | Placeholders Tracker

## 📋 文档说明
本文档追踪项目中所有使用占位符的地方，便于后续替换为真实值。

**更新时间**: 2025-11-03

---

## ✅ 已配置项 (Configured)

| 项目 | 值 | 位置 | 状态 |
|------|-----|------|------|
| **App名称** | 智能销冠 | `strings.xml` | ✅ 已配置 |
| **Qwen Dashscope API Key** | `sk-3617a0cf4daa4428a17c990e8307f09c` | `BuildConfig` / `local.properties` | ✅ 已配置 |
| **Qwen Tingwu APP Key** | `DVi4nadRWfUuGj2g` | `BuildConfig` / `local.properties` | ✅ 已配置 |
| **主题色** | Blue系 | `Color.kt` | ✅ 已配置 |
| **设计风格** | Neumorphism + Material | `Theme.kt` | ✅ 已配置 |

---

## ⚠️ 待替换占位符 (Pending Placeholders)

### 1. BLE相关

#### 1.1 BLE Service UUID
```kotlin
// 文件: data/remote/ble/BleConstants.kt
object BleConstants {
    // ⚠️ PLACEHOLDER: 需要从硬件团队获取真实UUID
    const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"
    
    // ⚠️ PLACEHOLDER: WiFi配置特征UUID
    const val WIFI_CONFIG_CHAR = "0000fff1-0000-1000-8000-00805f9b34fb"
    
    // ⚠️ PLACEHOLDER: 状态通知特征UUID
    const val STATUS_CHAR = "0000fff2-0000-1000-8000-00805f9b34fb"
}
```

**如何替换**:
1. 联系硬件团队获取真实UUID
2. 在BLE调试工具中扫描gadget查看服务
3. 替换上述三个常量

**优先级**: 🔴 高 (BLE功能无法工作)

---

#### 1.2 BLE设备名称前缀
```kotlin
// 文件: data/remote/ble/BleManager.kt
private const val DEVICE_NAME_PREFIX = "SalesGadget"  // ⚠️ PLACEHOLDER

fun filterGadgetDevices(devices: List<BleDevice>): List<BleDevice> {
    return devices.filter { 
        it.name?.startsWith(DEVICE_NAME_PREFIX) == true 
    }
}
```

**如何替换**:
- 确认gadget蓝牙广播的设备名称
- 更新`DEVICE_NAME_PREFIX`常量

**优先级**: 🟡 中 (影响设备过滤)

---

### 2. HTTP API相关

#### 2.1 Gadget API端点
```kotlin
// 文件: data/remote/gadget/GadgetApiService.kt
interface GadgetApiService {
    // ⚠️ PLACEHOLDER: 以下所有端点路径需要确认
    
    @GET("/api/files/list")
    suspend fun getFileList(): FileListResponse
    
    @GET("/api/files/download")
    suspend fun downloadFile(@Query("name") fileName: String): ResponseBody
    
    @POST("/api/display/image")
    suspend fun uploadImage(@Part image: MultipartBody.Part): UploadResponse
    
    @POST("/api/display/text")
    suspend fun updateText(@Body request: TextUpdateRequest): StatusResponse
    
    @GET("/api/status")
    suspend fun getStatus(): GadgetHealthResponse
}
```

**如何替换**:
1. 获取gadget HTTP服务器的API文档
2. 确认每个端点的:
   - 路径 (URL path)
   - HTTP方法 (GET/POST/PUT等)
   - 请求参数格式
   - 响应数据结构
3. 更新接口定义和数据模型

**优先级**: 🔴 高 (文件同步/编辑器功能依赖)

---

#### 2.2 Gadget默认IP和端口
```kotlin
// 文件: data/remote/gadget/GadgetHttpClient.kt
object GadgetDefaults {
    // ⚠️ PLACEHOLDER: gadget作为AP时的默认IP
    const val DEFAULT_AP_IP = "192.168.4.1"
    
    // ⚠️ PLACEHOLDER: HTTP服务器端口
    const val DEFAULT_PORT = 8080
    
    fun buildBaseUrl(ip: String? = null, port: Int = DEFAULT_PORT): String {
        val actualIp = ip ?: DEFAULT_AP_IP
        return "http://$actualIp:$port"
    }
}
```

**如何替换**:
- 确认gadget WiFi AP模式的IP地址
- 确认HTTP服务器监听端口
- 更新常量

**优先级**: 🟡 中 (影响初次连接)

---

#### 2.3 API响应数据结构
```kotlin
// 文件: data/remote/gadget/model/FileListResponse.kt
// ⚠️ PLACEHOLDER: 以下数据结构需要根据实际API响应调整

data class FileListResponse(
    val audio: List<AudioFileInfo>,
    val images: List<ImageFileInfo>
)

data class AudioFileInfo(
    val name: String,
    val size: Long,
    val timestamp: String,      // ⚠️ 格式待确认: ISO8601? Unix timestamp?
    val location: String?       // ⚠️ 可选字段，可能不存在
)

data class TextUpdateRequest(
    val text: String,
    val displayTime: Int?       // ⚠️ 单位待确认: 秒? 毫秒?
)
```

**如何替换**:
1. 使用Postman/curl测试gadget API
2. 查看实际返回的JSON结构
3. 调整数据类字段名、类型、可空性

**优先级**: 🔴 高 (数据解析失败)

---

### 3. 认证相关

#### 3.1 Gadget HTTP认证
```kotlin
// 文件: data/remote/gadget/GadgetHttpClient.kt
// ⚠️ PLACEHOLDER: 认证机制待实现

private fun buildAuthHeader(): String? {
    // TODO: 实现认证逻辑
    // 可能的方式:
    // 1. Basic Auth: "Basic ${Base64.encode("user:pass")}"
    // 2. Bearer Token: "Bearer xxx"
    // 3. API Key: "X-Api-Key: xxx"
    // 4. 无需认证
    return null  // 当前无认证
}
```

**如何替换**:
1. 确认gadget是否需要HTTP认证
2. 获取认证方式和凭证
3. 实现`buildAuthHeader()`方法

**优先级**: 🟢 低 (第一版暂不需要)

---

#### 3.2 BLE配对PIN码
```kotlin
// 文件: data/remote/ble/BleManager.kt
// ⚠️ PLACEHOLDER: BLE配对密码

private const val BLE_PAIRING_PIN = "000000"  // ⚠️ 6位数字PIN

fun handlePairingRequest(device: BleDevice) {
    // 如果gadget需要配对，自动输入PIN
    device.setPairingConfirmation(true)
    device.setPin(BLE_PAIRING_PIN)
}
```

**如何替换**:
- 确认gadget是否需要配对
- 获取默认PIN码或配对方式
- 更新常量

**优先级**: 🟡 中 (部分Android版本需要)

---

### 4. Qwen API相关

#### 4.1 模型版本
```kotlin
// 文件: data/remote/qwen/QwenApiService.kt
object QwenModels {
    // ⚠️ PLACEHOLDER: 可能需要调整模型版本以平衡成本和性能
    
    const val CHAT_MODEL = "qwen-max"           // 最强模型，成本高
    // 可选: "qwen-plus", "qwen-turbo"
    
    const val TRANSCRIPTION_MODEL = "tingwu-v1" // ⚠️ 待确认版本号
}
```

**如何替换**:
1. 测试不同模型的效果和速度
2. 根据预算和需求选择合适模型
3. 更新常量

**优先级**: 🟡 中 (影响成本和效果)

---

#### 4.2 提示词模板
```kotlin
// 文件: domain/usecase/ai/PromptTemplates.kt
object PromptTemplates {
    // ⚠️ PLACEHOLDER: 需要根据实际测试效果优化提示词
    
    const val SYSTEM_PROMPT = """
你是一位资深的销售顾问AI助手，名叫智能销冠。
你的任务是帮助销售人员分析客户需求，制定个性化的销售策略。
"""
    
    fun customerAnalysisPrompt(...) = """
根据以下信息分析客户：
...
"""
}
```

**如何替换**:
1. 进行prompt engineering测试
2. 收集实际使用反馈
3. 迭代优化提示词

**优先级**: 🟡 中 (持续优化)

---

### 5. CSV导出相关

#### 5.1 HubSpot字段映射
```kotlin
// 文件: domain/usecase/export/CsvGenerator.kt
object HubSpotFields {
    // ⚠️ PLACEHOLDER: 需要确认HubSpot实际字段名
    
    const val FIELD_NAME = "姓名"
    const val FIELD_COMPANY = "公司"
    const val FIELD_CONTACT = "联系方式"
    const val FIELD_NEEDS = "需求"
    const val FIELD_STAGE = "阶段"
    const val FIELD_NOTES = "备注"
    
    // ⚠️ TODO: 可能需要英文字段名
    // const val FIELD_NAME_EN = "name"
    // const val FIELD_COMPANY_EN = "company"
}
```

**如何替换**:
1. 登录HubSpot查看联系人属性字段
2. 确认中文/英文字段名
3. 确认必填字段和可选字段
4. 更新常量和CSV生成逻辑

**优先级**: 🟡 中 (影响CRM导入)

---

### 6. UI相关

#### 6.1 Logo和品牌图标
```
⚠️ PLACEHOLDER: 需要设计和添加的资源文件

res/drawable/
├── ic_logo.xml              // App logo (SVG格式)
├── ic_logo_splash.xml       // 启动页logo
├── ic_gadget_placeholder.png // Gadget设备默认图标
└── img_empty_state.xml      // 空状态插图

res/mipmap-xxxhdpi/
└── ic_launcher.png          // 应用图标 (各尺寸)
```

**如何替换**:
1. 设计品牌logo和图标
2. 使用Android Asset Studio生成各尺寸
3. 替换占位图标

**优先级**: 🟢 低 (功能不受影响)

---

#### 6.2 示例数据和截图
```kotlin
// 文件: presentation/preview/PreviewData.kt
// ⚠️ PLACEHOLDER: 用于Compose Preview的示例数据

object PreviewData {
    val sampleMessages = listOf(
        Message(role = "user", content = "今天见了一个客户..."),
        Message(role = "assistant", content = "根据您的描述...")
    )
    
    val sampleCustomer = CustomerProfile(
        name = "张三",  // ⚠️ 使用虚构数据
        company = "XX公司",
        needs = "换车需求"
    )
}
```

**如何替换**:
- 在生产环境移除示例数据
- 或使用更真实的测试案例

**优先级**: 🟢 低 (仅开发使用)

---

### 7. 配置文件相关

#### 7.1 ProGuard规则
```proguard
# 文件: proguard-rules.pro
# ⚠️ PLACEHOLDER: 需要根据实际使用的库添加规则

# Retrofit
-keepattributes Signature
-keep class retrofit2.** { *; }

# Gson
-keep class com.salesassistant.data.remote.**.model.** { *; }

# ⚠️ TODO: 添加更多第三方库的混淆规则
# - FastBle
# - Room
# - Coil
```

**如何替换**:
1. 测试Release构建
2. 查看混淆后的崩溃日志
3. 添加必要的keep规则

**优先级**: 🟡 中 (Release构建需要)

---

#### 7.2 签名配置
```gradle
// 文件: app/build.gradle.kts
android {
    signingConfigs {
        create("release") {
            // ⚠️ PLACEHOLDER: 需要生成密钥库
            storeFile = file("../keystore/release.jks")
            storePassword = "PLACEHOLDER_PASSWORD"
            keyAlias = "release_key"
            keyPassword = "PLACEHOLDER_PASSWORD"
        }
    }
}
```

**如何替换**:
1. 生成密钥库: `keytool -genkey -v -keystore release.jks ...`
2. 将密钥库信息保存到`local.properties`
3. 更新签名配置

**优先级**: 🔴 高 (发布需要)

---

## 📝 占位符替换检查清单

发布前必须检查:

### 🔴 Critical (必须替换，否则功能不可用)
- [ ] BLE Service UUID (3个)
- [ ] Gadget HTTP API端点
- [ ] API响应数据结构
- [ ] 签名密钥库

### 🟡 Important (应该替换，影响用户体验)
- [ ] BLE设备名称前缀
- [ ] Gadget默认IP/端口
- [ ] Qwen模型版本选择
- [ ] HubSpot字段映射
- [ ] ProGuard混淆规则

### 🟢 Optional (可选替换，不影响核心功能)
- [ ] BLE配对PIN码 (如果需要)
- [ ] HTTP认证机制 (第二版)
- [ ] Logo和品牌图标
- [ ] 提示词优化 (持续迭代)

---

## 🔍 如何查找占位符

在项目中搜索以下标记:

```bash
# 搜索所有占位符
grep -r "⚠️ PLACEHOLDER" --include="*.kt" --include="*.xml"

# 搜索TODO注释
grep -r "TODO:" --include="*.kt"

# 搜索需要配置的常量
grep -r "PLACEHOLDER_" --include="*.kt"
```

---

## 📞 联系方式

如果需要占位符的真实值，联系:

- **BLE相关**: 硬件团队 (gadget开发者)
- **HTTP API**: 后端团队 或查看gadget文档
- **Qwen API**: 阿里云控制台查看配额和模型
- **HubSpot**: CRM管理员或销售运营团队

---

## 🔄 变更记录

| 日期 | 项目 | 操作 | 备注 |
|------|------|------|------|
| 2025-11-03 | API Keys | ✅ 已配置 | Dashscope & Tingwu |
| 2025-11-03 | App名称 | ✅ 已配置 | 智能销冠 |
| 2025-11-03 | 主题色 | ✅ 已配置 | Blue系 |
| ... | BLE UUID | ⚠️ 待替换 | 等待硬件团队提供 |
| ... | HTTP API | ⚠️ 待替换 | 等待接口文档 |

---

**最后更新**: 2025-11-03
**下次审查**: 开始第二阶段开发前 (Week 3)
