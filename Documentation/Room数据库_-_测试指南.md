# Room数据库 - 完成与测试指南 🗄️

## ✅ 已完成的内容

### 1. Entity实体类（4个）
- ✅ **ConversationEntity** - 对话表
  - 字段: id, title, summary, createdAt, updatedAt, isAnalyzed, customerName, customerCompany, exportedPdf, exportedCsv
  - 索引: updated_at, created_at
  
- ✅ **MessageEntity** - 消息表
  - 字段: id, conversationId, role, content, timestamp, attachments, metadata, status
  - 外键: conversationId → conversations.id (CASCADE删除)
  - 索引: conversation_id + timestamp
  
- ✅ **WifiConfigEntity** - WiFi配置表
  - 字段: id, ssid, password, addedAt, lastConnectedAt, connectionCount, isCurrent, notes
  - 唯一索引: ssid
  
- ✅ **AttachmentEntity** - 附件表
  - 字段: id, messageId, type, filePath, fileName, fileSize, mimeType, thumbnailPath, duration, transcription, uploadedAt, fromGadget
  - 外键: messageId → messages.id (CASCADE删除)

### 2. DAO接口（4个）
- ✅ **ConversationDao** - 20+方法
  - 查询: getAllConversations, getConversationsByTimeRange, searchConversations
  - 插入: insert, insertAll
  - 更新: update, updateTitleAndSummary, markAsAnalyzed
  - 删除: delete, deleteById, deleteAll
  
- ✅ **MessageDao** - 25+方法
  - 查询: getMessagesByConversation, getMessagesPaged, searchMessages
  - 插入: insert, insertAll
  - 更新: update, updateStatus, updateContent
  - 删除: delete, deleteByConversation
  - 事务: replaceMessages
  
- ✅ **WifiConfigDao** - 20+方法
  - 查询: getAllWifiConfigs, getWifiConfigBySSID, getCurrentWifiConfig
  - 插入: insert, insertAll
  - 更新: update, updateLastConnected, setCurrentWifi
  - 删除: delete, deleteBySSID
  
- ✅ **AttachmentDao** - 10+方法
  - 查询: getAttachmentsByMessage, getAttachmentsByType
  - 插入: insert, insertAll
  - 更新: update, updateTranscription
  - 删除: delete, deleteByMessage

### 3. 数据库类
- ✅ **AppDatabase** - Room数据库配置
  - 版本: 1
  - 包含4个表
  - TypeConverters支持

### 4. Domain模型（4个）
- ✅ **Conversation** - 对话领域模型
- ✅ **Message** - 消息领域模型（含MessageRole、MessageStatus枚举）
- ✅ **Attachment** - 附件领域模型（含AttachmentType枚举）
- ✅ **WifiConfig** - WiFi配置领域模型

### 5. Mapper映射器（3个）
- ✅ **ConversationMapper** - Entity ↔ Domain
- ✅ **MessageMapper** - Entity ↔ Domain
- ✅ **WifiConfigMapper** - Entity ↔ Domain

---

## 📁 文件清单（需要创建）

### 数据库Entity
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/data/local/database/
├── entity/
│   ├── ConversationEntity.kt
│   ├── MessageEntity.kt
│   ├── WifiConfigEntity.kt
│   └── AttachmentEntity.kt
```

### DAO接口
```
├── dao/
│   ├── ConversationDao.kt
│   ├── MessageDao.kt
│   ├── WifiConfigDao.kt
│   └── AttachmentDao.kt
```

### 数据库类
```
├── AppDatabase.kt
└── converter/
    └── Converters.kt
```

### Domain模型
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/domain/model/
├── Conversation.kt
├── Message.kt
├── Attachment.kt
└── WifiConfig.kt
```

### Mapper
```
app/src/main/kotlin/com/salesassistant/zhinengxiaoguan/data/mapper/
├── ConversationMapper.kt
├── MessageMapper.kt
└── WifiConfigMapper.kt
```

---

## 🧪 测试Room数据库

### Step 1: 创建数据库测试ViewModel

创建文件: `TestDatabaseViewModel.kt`

```kotlin
package com.salesassistant.zhinengxiaoguan.presentation.test

import androidx.lifecycle.viewModelScope
import com.salesassistant.zhinengxiaoguan.core.base.BaseViewModel
import com.salesassistant.zhinengxiaoguan.core.util.Logger
import com.salesassistant.zhinengxiaoguan.data.local.database.dao.ConversationDao
import com.salesassistant.zhinengxiaoguan.data.local.database.dao.MessageDao
import com.salesassistant.zhinengxiaoguan.data.local.database.entity.ConversationEntity
import com.salesassistant.zhinengxiaoguan.data.local.database.entity.MessageEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TestDatabaseViewModel @Inject constructor(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao
) : BaseViewModel() {
    
    private val _uiState = MutableStateFlow(TestDatabaseUiState())
    val uiState: StateFlow<TestDatabaseUiState> = _uiState.asStateFlow()
    
    init {
        Logger.i("TestDatabaseViewModel", "init", "ViewModel初始化")
        observeConversations()
    }
    
    /**
     * 观察对话列表
     */
    private fun observeConversations() {
        viewModelScope.launch {
            conversationDao.getAllConversations().collect { conversations ->
                Logger.d("TestDatabaseViewModel", "observeConversations", 
                    "对话数量", "count" to conversations.size)
                _uiState.update { it.copy(conversations = conversations) }
            }
        }
    }
    
    /**
     * 插入测试对话
     */
    fun insertTestConversation() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val conversation = ConversationEntity(
                    title = "测试对话 ${now % 10000}",
                    summary = "这是一个测试对话的摘要",
                    createdAt = now,
                    updatedAt = now
                )
                
                val id = conversationDao.insert(conversation)
                Logger.i("TestDatabaseViewModel", "insertTestConversation", 
                    "插入成功", "id" to id)
                
                _uiState.update { it.copy(lastOperationMessage = "插入成功, ID: $id") }
            } catch (e: Exception) {
                Logger.e("TestDatabaseViewModel", "insertTestConversation", 
                    "插入失败", e)
                _uiState.update { it.copy(lastOperationMessage = "插入失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 删除所有对话
     */
    fun deleteAllConversations() {
        viewModelScope.launch {
            try {
                conversationDao.deleteAllConversations()
                Logger.i("TestDatabaseViewModel", "deleteAllConversations", "删除成功")
                _uiState.update { it.copy(lastOperationMessage = "已清空所有对话") }
            } catch (e: Exception) {
                Logger.e("TestDatabaseViewModel", "deleteAllConversations", 
                    "删除失败", e)
                _uiState.update { it.copy(lastOperationMessage = "删除失败: ${e.message}") }
            }
        }
    }
    
    /**
     * 批量插入测试数据
     */
    fun insertBatchTestData() {
        viewModelScope.launch {
            try {
                val now = System.currentTimeMillis()
                val conversations = List(5) { index ->
                    ConversationEntity(
                        title = "批量测试对话 $index",
                        summary = "这是第 $index 个测试对话",
                        createdAt = now - index * 60000, // 每个间隔1分钟
                        updatedAt = now - index * 60000
                    )
                }
                
                val ids = conversationDao.insertAll(conversations)
                Logger.i("TestDatabaseViewModel", "insertBatchTestData", 
                    "批量插入成功", "count" to ids.size)
                
                _uiState.update { it.copy(lastOperationMessage = "批量插入${ids.size}条记录") }
            } catch (e: Exception) {
                Logger.e("TestDatabaseViewModel", "insertBatchTestData", 
                    "批量插入失败", e)
                _uiState.update { it.copy(lastOperationMessage = "批量插入失败: ${e.message}") }
            }
        }
    }
}

data class TestDatabaseUiState(
    val conversations: List<ConversationEntity> = emptyList(),
    val lastOperationMessage: String? = null
)
```

### Step 2: 在TestScreen中添加数据库测试卡片

在 `TestScreen.kt` 中添加：

```kotlin
@Composable
fun DatabaseTestCard(viewModel: TestDatabaseViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🗄️ 数据库测试",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Divider()
            
            // 对话数量
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("对话数量:")
                Text(
                    text = "${uiState.conversations.size}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.insertTestConversation() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("插入1条")
                }
                
                Button(
                    onClick = { viewModel.insertBatchTestData() },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("插入5条")
                }
            }
            
            Button(
                onClick = { viewModel.deleteAllConversations() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("清空数据")
            }
            
            // 对话列表
            if (uiState.conversations.isNotEmpty()) {
                Text(
                    text = "对话列表:",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold
                )
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                ) {
                    items(uiState.conversations) { conversation ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp)
                            ) {
                                Text(
                                    text = conversation.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "ID: ${conversation.id}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
            
            // 操作结果
            uiState.lastOperationMessage?.let { message ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
```

然后在TestScreen的主Column中添加：
```kotlin
// 数据库测试卡片
DatabaseTestCard()
```

---

## ✅ 测试检查清单

### 基础测试
- [ ] **编译通过** - 所有文件无编译错误
- [ ] **应用启动** - 应用正常启动，无崩溃
- [ ] **ViewModel注入** - TestDatabaseViewModel成功注入

### 数据库操作测试
- [ ] **插入单条** - 点击"插入1条"按钮
  - [ ] 对话数量增加1
  - [ ] Logcat显示插入成功日志
  - [ ] 显示新对话ID
  
- [ ] **批量插入** - 点击"插入5条"按钮
  - [ ] 对话数量增加5
  - [ ] Logcat显示批量插入日志
  - [ ] 列表显示5条新对话
  
- [ ] **清空数据** - 点击"清空数据"按钮
  - [ ] 对话数量变为0
  - [ ] 列表清空
  - [ ] Logcat显示删除成功日志
  
- [ ] **实时更新** - 数据变化自动反映到UI
  - [ ] 插入后立即显示新数据
  - [ ] 删除后立即刷新列表

### Logcat验证
搜索关键词: `TestDatabaseViewModel`

预期日志：
```
I/[TestDatabaseViewModel] [init] ViewModel初始化
D/[TestDatabaseViewModel] [observeConversations] 对话数量: count=0
I/[TestDatabaseViewModel] [insertTestConversation] 插入成功: id=1
D/[TestDatabaseViewModel] [observeConversations] 对话数量: count=1
I/[TestDatabaseViewModel] [insertBatchTestData] 批量插入成功: count=5
D/[TestDatabaseViewModel] [observeConversations] 对话数量: count=6
I/[TestDatabaseViewModel] [deleteAllConversations] 删除成功
D/[TestDatabaseViewModel] [observeConversations] 对话数量: count=0
```

---

## 🎯 测试通过标准

### ✅ 所有测试必须通过

1. **编译成功** - 无任何编译错误
2. **数据插入** - 单条和批量插入都成功
3. **数据查询** - Flow正确返回数据
4. **数据删除** - 清空操作成功
5. **实时更新** - UI自动响应数据变化
6. **日志正常** - Logcat有详细操作日志
7. **无内存泄漏** - LeakCanary无警告

---

## 📊 数据库结构总览

```
AppDatabase (v1)
├── conversations (对话表)
│   ├── id (PK)
│   ├── title
│   ├── summary
│   ├── created_at (indexed)
│   ├── updated_at (indexed)
│   ├── is_analyzed
│   ├── customer_name
│   ├── customer_company
│   ├── exported_pdf
│   └── exported_csv
│
├── messages (消息表)
│   ├── id (PK)
│   ├── conversation_id (FK → conversations.id)
│   ├── role
│   ├── content
│   ├── timestamp (indexed)
│   ├── attachments (JSON)
│   ├── metadata (JSON)
│   └── status
│
├── wifi_configs (WiFi配置表)
│   ├── id (PK)
│   ├── ssid (unique indexed)
│   ├── password
│   ├── added_at (indexed)
│   ├── last_connected_at
│   ├── connection_count
│   ├── is_current
│   └── notes
│
└── attachments (附件表)
    ├── id (PK)
    ├── message_id (FK → messages.id)
    ├── type
    ├── file_path (indexed)
    ├── file_name
    ├── file_size
    ├── mime_type
    ├── thumbnail_path
    ├── duration
    ├── transcription
    ├── uploaded_at
    └── from_gadget
```

---

## 🚀 测试完成后

如果所有测试通过，恭喜！你的Room数据库层已经**完全就绪**。

### 下一步选择：

1. **"开始实现BLE模块"** - 实现与Gadget的蓝牙通信
2. **"开始实现Repository层"** - 完善数据仓库实现
3. **"开始实现主界面"** - 构建完整的UI框架

告诉我你的选择，我会立即继续开发！ 🎯

---

## 📝 常见问题

### Q: 编译错误 "Cannot find symbol: Converters"
**A**: 确保创建了 `Converters.kt` 文件并放在正确位置

### Q: 运行时崩溃 "Cannot create an instance of class TestDatabaseViewModel"
**A**: 确保添加了 `@HiltViewModel` 注解和 `@Inject constructor`

### Q: Flow不更新UI
**A**: 确保在Composable中使用了 `collectAsState()`

### Q: 外键约束失败
**A**: 确保在插入Message前先插入对应的Conversation

---

**准备好测试了吗？开始运行并告诉我结果！** 🧪✨
