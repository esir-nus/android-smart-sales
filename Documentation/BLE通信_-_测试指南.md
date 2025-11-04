# BLE通信模块 - 完成与测试指南 📡

## ✅ 已完成的内容

### 1. 数据模型（7个）
- ✅ **BleDevice** - BLE设备模型
  - 包含: name, mac, rssi, isConnectable
  - 方法: getSignalLevel(), getSignalDescription(), isGadgetDevice()
  
- ✅ **WifiCredentials** - WiFi凭证
  - 包含: ssid, password
  - 方法: toJson(), toByteArray(), isValid()
  
- ✅ **BleConnectionState** - 连接状态（sealed class）
  - Idle, Scanning, Connecting, Connected, Disconnected, Error
  
- ✅ **GadgetStatus** - Gadget状态（sealed class）
  - Idle, WifiConnecting, WifiConnected, Error
  - 方法: fromByteArray(), getDescription()
  
- ✅ **BleScanResult** - 扫描结果（sealed class）
  - ScanStarted, DeviceFound, ScanFinished, ScanFailed
  
- ✅ **BleConstants** - BLE常量
  - UUID定义（占位符）
  - 超时设置
  - 重连配置
  
- ✅ **BleException** - BLE异常体系（14个）
  - 权限、连接、读写、服务等异常

### 2. 核心功能
- ✅ **BleManager** - BLE管理器（FastBle封装）
  - 设备扫描（支持超时、过滤）
  - 设备连接（支持超时、重试）
  - WiFi配置传输
  - 状态监听（Notify特征）
  - 错误处理（统一异常封装）
  
- ✅ **BleRepository** - 数据仓库接口
- ✅ **BleRepositoryImpl** - 数据仓库实现
  - 集成Room数据库（保存WiFi配置）
  - 集成BleManager

### 3. UI界面
- ✅ **BleTestViewModel** - 测试ViewModel
  - 蓝牙状态检查
  - 设备扫描管理
  - 设备连接管理
  - WiFi配置发送
  - 状态监听
  
- ✅ **BleTestScreen** - 完整测试界面
  - 蓝牙状态卡片
  - 扫描控制卡片
  - 设备列表卡片
  - 连接状态卡片
  - WiFi配置卡片
  - 消息提示

---

## 📁 文件清单（需要创建）

### 数据模型
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/data/remote/ble/model/
├── BleDevice.kt
├── WifiCredentials.kt
├── BleConnectionState.kt
├── GadgetStatus.kt
└── BleScanResult.kt
```

### 核心功能
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/
├── data/remote/ble/
│   └── BleManager.kt
├── data/repository/
│   └── BleRepositoryImpl.kt
└── domain/repository/
    └── BleRepository.kt
```

### 异常和常量
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/core/
├── constant/
│   └── BleConstants.kt
└── exception/
    └── BleException.kt (已更新)
```

### UI界面
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/presentation/ble/
├── BleTestViewModel.kt
└── BleTestScreen.kt
```

---

## 🔧 集成步骤

### Step 1: 添加BLE测试入口

在 `MainActivity.kt` 中添加导航到BLE测试页面的按钮：

```kotlin
// 在TestScreen中添加
Button(
    onClick = { /* 导航到BLE测试页面 */ },
    modifier = Modifier.fillMaxWidth()
) {
    Icon(Icons.Default.Bluetooth, contentDescription = null)
    Spacer(modifier = Modifier.width(8.dp))
    Text("BLE通信测试")
}
```

或者创建简单的导航：

```kotlin
// 在MainActivity中
var currentScreen by remember { mutableStateOf("test") }

when (currentScreen) {
    "test" -> TestScreen(onNavigateToBle = { currentScreen = "ble" })
    "ble" -> BleTestScreen(onNavigateBack = { currentScreen = "test" })
}
```

### Step 2: 请求蓝牙权限

在 `AndroidManifest.xml` 中已包含所有必需权限，但需要在运行时请求：

创建权限请求工具：

```kotlin
// PermissionHelper.kt
object PermissionHelper {
    fun checkBlePermissions(context: Context): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        
        return permissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
    }
}
```

### Step 3: 更新Hilt模块

确保 `di/RepositoryModule.kt` 中包含BleRepository绑定（应该已经有了）：

```kotlin
@Binds
@Singleton
abstract fun bindBleRepository(
    impl: BleRepositoryImpl
): BleRepository
```

---

## 🧪 测试BLE功能

### 测试环境要求

#### 硬件要求
- ✅ **必须使用真机**（模拟器不支持BLE）
- ✅ 手机支持BLE 4.0+
- ✅ Android 8.0+ (API 26+)

#### 测试准备
1. **准备Gadget设备**（如果没有，可以用其他BLE设备模拟）
2. **启用蓝牙**
3. **授予权限**（蓝牙、定位）

---

### 测试流程

#### 测试1: 蓝牙状态检查 ✅

1. 打开BLE测试页面
2. 查看"蓝牙状态"卡片
3. **预期结果**:
   - ✅ 蓝牙支持: 支持
   - ✅ 蓝牙状态: 已开启（或显示"打开蓝牙"按钮）

**Logcat验证**:
```
I/[BleTestViewModel] [init] ViewModel初始化
D/[BleTestViewModel] [checkBluetoothStatus] supported=true, enabled=true
```

---

#### 测试2: 设备扫描 🔍

1. 点击"开始扫描"按钮
2. 等待扫描完成（30秒或发现设备后停止）
3. **预期结果**:
   - 按钮显示"扫描中..."
   - 发现设备数量实时更新
   - 设备列表显示所有发现的设备
   - Gadget设备高亮显示（蓝色背景）

**Logcat验证**:
```
I/[BleManager] [scanDevices] → 进入: timeout=30000
D/[BleManager] [onScanStarted] 扫描开始: success=true
V/[BleManager] [onScanning] 发现设备: name=SalesGadget_001, mac=XX:XX:XX:XX:XX:XX, rssi=-65, isGadget=true
I/[BleManager] [onScanFinished] 扫描完成: total=5, gadgets=1
I/[BleTestViewModel] [startScan] 扫描完成: total=5, gadgets=1
```

**注意事项**:
- 信号强度 < -90 dBm 的设备会被自动过滤
- Gadget设备会显示蓝色背景
- 设备名称前缀: `SalesGadget` (占位符，需要替换)

---

#### 测试3: 设备连接 🔗

1. 在设备列表中选择一个Gadget设备
2. 点击"连接"按钮
3. 等待连接完成
4. **预期结果**:
   - 显示"连接状态"卡片
   - 显示已连接设备名称
   - 显示Gadget状态（空闲/WiFi连接中/已连接）
   - 显示"断开连接"按钮

**Logcat验证**:
```
I/[BleManager] [connect] → 进入: device=SalesGadget_001, mac=XX:XX:XX:XX:XX:XX
D/[BleManager] [onStartConnect] 开始连接: device=SalesGadget_001
I/[BleManager] [onConnectSuccess] 连接成功: device=SalesGadget_001, status=0
I/[BleTestViewModel] [connectDevice] 连接成功
I/[BleManager] [observeGadgetStatus] → 进入
I/[BleManager] [onNotifySuccess] 通知已启用
```

**可能的错误**:
- **连接超时**: 设备距离太远或信号不稳定
- **连接失败 (133错误)**: 设备已被其他应用连接，或BLE堆栈错误
- **服务未找到**: UUID不匹配（需要替换占位符UUID）

---

#### 测试4: WiFi配置发送 📶

1. 确保设备已连接
2. 输入WiFi名称（SSID）
3. 输入WiFi密码（至少8位）
4. 点击"发送配置"按钮
5. **预期结果**:
   - 按钮显示"发送中..."
   - 发送成功后显示成功提示
   - Gadget状态变为"WiFi连接中..."
   - 连接成功后状态变为"WiFi已连接 (192.168.x.x)"

**Logcat验证**:
```
I/[BleManager] [sendWifiConfig] → 进入: ssid=MyWiFi
D/[BleManager] [sendWifiConfig] 准备写入数据: dataSize=45, json={"ssid":"MyWiFi","password":"********"}
V/[BleManager] [onWriteSuccess] 写入进度: current=20, total=45, size=20
V/[BleManager] [onWriteSuccess] 写入进度: current=40, total=45, size=20
I/[BleManager] [onWriteSuccess] 全部数据已发送
I/[BleTestViewModel] [sendWifiConfig] 发送成功
D/[BleManager] [onCharacteristicChanged] 收到状态更新: dataSize=1, data=01
I/[BleManager] [onCharacteristicChanged] 状态解析: status=WiFi连接中...
D/[BleManager] [onCharacteristicChanged] 收到状态更新: dataSize=14, data=02 C0 A8 04 01
I/[BleManager] [onCharacteristicChanged] 状态解析: status=WiFi已连接 (192.168.4.1)
```

**数据格式**:
```json
{
  "ssid": "MyWiFi",
  "password": "12345678"
}
```

**可能的错误**:
- **写入失败**: 特征UUID不匹配或不支持写入
- **密码太短**: 少于8位会被拒绝
- **设备未连接**: 需要先连接设备

---

#### 测试5: 状态监听 👀

1. 连接成功后，自动开始监听状态
2. 发送WiFi配置后观察状态变化
3. **预期结果**:
   - 空闲 → WiFi连接中 → WiFi已连接
   - 显示Gadget的IP地址

**状态码定义**:
```
0x00 - 空闲
0x01 - WiFi连接中
0x02 - WiFi已连接 (附带IP地址)
0xFF - 错误
```

---

#### 测试6: 断开连接 🔌

1. 点击"断开连接"按钮
2. **预期结果**:
   - 连接状态卡片消失
   - WiFi配置卡片消失
   - 可以重新扫描和连接

**Logcat验证**:
```
I/[BleManager] [disconnect] → 进入
I/[BleManager] [disconnect] 断开连接: device=SalesGadget_001
W/[BleManager] [onDisConnected] 连接断开: device=SalesGadget_001, active=true, status=0
```

---

## ✅ 测试检查清单

### 基础功能 ✅
- [ ] 蓝牙状态正确显示
- [ ] 可以打开蓝牙（如果未开启）
- [ ] 设备扫描正常工作
- [ ] 发现的设备显示在列表中
- [ ] Gadget设备高亮显示

### 连接功能 ✅
- [ ] 可以连接到BLE设备
- [ ] 连接状态正确显示
- [ ] 连接失败有明确错误提示
- [ ] 可以主动断开连接

### WiFi配置 ✅
- [ ] WiFi表单验证正确（密码8位+）
- [ ] 可以发送WiFi配置
- [ ] 发送进度显示正确
- [ ] 发送成功/失败有明确提示

### 状态监听 ✅
- [ ] 自动启用Notify特征
- [ ] 状态更新实时显示
- [ ] WiFi连接过程状态正确
- [ ] 显示Gadget IP地址

### 日志系统 ✅
- [ ] 所有操作都有详细日志
- [ ] 日志格式统一：`[模块][方法] 描述`
- [ ] 异常日志包含堆栈信息
- [ ] VERBOSE级别记录详细流程

---

## 🐛 常见问题排查

### 问题1: 扫描不到任何设备

**可能原因**:
1. 权限未授予（定位权限）
2. 蓝牙未开启
3. 设备不在范围内
4. Android 12+ 需要BLUETOOTH_SCAN权限

**解决方案**:
```kotlin
// 检查权限
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    requestPermissions(arrayOf(
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.ACCESS_FINE_LOCATION
    ))
}
```

---

### 问题2: 连接后立即断开

**可能原因**:
1. 设备距离太远
2. 信号干扰
3. BLE堆栈问题（状态133）

**解决方案**:
- 靠近设备
- 关闭并重新打开蓝牙
- 重启手机
- 检查设备是否被其他应用占用

---

### 问题3: 写入数据失败

**可能原因**:
1. UUID不匹配（占位符未替换）
2. 特征不支持写入
3. 服务发现未完成

**解决方案**:
- 替换真实的UUID
- 增加服务发现延迟（500ms）
- 检查特征权限（Write/WriteNoResponse）

---

### 问题4: 状态通知不工作

**可能原因**:
1. UUID不匹配
2. 特征不支持Notify
3. Descriptor未正确配置

**解决方案**:
- 替换真实的UUID
- 确认特征支持Notify
- FastBle会自动配置Descriptor

---

## 🎯 测试通过标准

### ✅ 所有测试必须通过

1. **扫描功能** - 可以发现BLE设备
2. **连接功能** - 可以连接并保持连接
3. **数据传输** - WiFi配置发送成功
4. **状态监听** - 实时接收Gadget状态
5. **错误处理** - 所有错误有友好提示
6. **日志完整** - Logcat有详细操作日志

---

## 📊 BLE模块统计

```
文件数: 11个
代码行数: ~2000行
方法数: 50+
状态类: 3个 (sealed class)
异常类: 14个
测试UI: 完整测试界面

支持功能:
  ✅ 设备扫描（带过滤）
  ✅ 设备连接（带超时）
  ✅ WiFi配置传输（JSON格式）
  ✅ 状态监听（Notify特征）
  ✅ 错误处理（统一异常）
  ✅ 日志记录（高详细度）
  ✅ WiFi配置持久化（Room数据库）
```

---

## 🚀 测试完成后

如果所有测试通过，恭喜！BLE通信模块已经**完全就绪**。

### 下一步选择：

1. **"开始实现HTTP客户端"** - 实现与Gadget的HTTP通信
2. **"开始实现Repository层"** - 完善其他Repository
3. **"开始实现主界面"** - 构建完整UI框架
4. **"实现BLE自动重连"** - 增强BLE稳定性

---

## 📝 重要提醒

### ⚠️ 占位符需要替换

在实际使用前，必须替换以下占位符：

```kotlin
// BleConstants.kt
const val SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb"  // ⚠️ 替换
const val WIFI_CONFIG_CHAR_UUID = "0000fff1-0000-1000-8000-00805f9b34fb"  // ⚠️ 替换
const val STATUS_CHAR_UUID = "0000fff2-0000-1000-8000-00805f9b34fb"  // ⚠️ 替换
const val DEVICE_NAME_PREFIX = "SalesGadget"  // ⚠️ 替换
```

### 📱 权限请求

记得在首次使用时请求权限：
- Android 12+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION
- Android <12: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION

---

**准备好测试了吗？连接真机，开始测试BLE功能！** 📡✨
