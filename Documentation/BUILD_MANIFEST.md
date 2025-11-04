# Ã°Å¸"â€¹ SMART SALES ASSISTANT - BUILD MANIFEST

**Project**: Smart Sales Assistant Android App  
**Architecture**: MVVM + Clean Architecture + Jetpack Compose  
**Language**: Kotlin  
**Target**: Android 8.0+ (API 26+)  
**Last Updated**: Session 4 - November 2025

---

## Ã°Å¸Å½Â¯ PROJECT OVERVIEW

### Core Purpose
B2B sales representative Android app that:
1. Records customer conversations via Bluetooth gadget
2. Transcribes audio using Alibaba Cloud Tingwu
3. Extracts CRM data using Qwen AI
4. Syncs files with device over HTTP
5. Exports data to PDF/CSV
6. Manages conversation history

### Technology Stack
- **UI**: Jetpack Compose + Material 3
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room (SQLite)
- **DI**: Hilt (Dagger)
- **Networking**: Retrofit + OkHttp
- **Bluetooth**: Android BLE APIs
- **AI**: Alibaba Qwen (Dashscope API)
- **Transcription**: Alibaba Tingwu API
- **File Sync**: HTTP REST API
- **Export**: iText PDF, Apache POI CSV

---

## Ã°Å¸"Å  PROGRESS TRACKING

### Overall Status: **45% Complete**

```
Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“Ë†Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“'Ã¢â€“' 45%
```

### Component Status Matrix

| Component | Status | Lines | Files | Priority | Session |
|-----------|--------|-------|-------|----------|---------|
| Project Setup | Ã¢Å“â€¦ Complete | 150 | 3 | Critical | 1 |
| Database Layer | Ã¢Å“â€¦ Complete | 450 | 6 | Critical | 2 |
| Repository Layer | Ã¢Å“â€¦ Complete | 330 | 3 | Critical | 3 |
| Domain Models | Ã¢Å“â€¦ Complete | 100 | 1 | Critical | 3 |
| BLE Module | Ã¢Å“â€¦ Complete | 280 | 2 | Critical | 3 |
| Dependency Injection | Ã¢Å“â€¦ Complete | 100 | 2 | Critical | 3 |
| Network Layer | Ã°Å¸Å¡à¸‡ In Progress | 0/400 | 0/5 | Critical | 4 |
| Business Logic | Ã¢Â³ Pending | 0/800 | 0/6 | Critical | 4-5 |
| Export System | Ã¢Â³ Pending | 0/300 | 0/2 | High | 5 |
| UI Theme | Ã¢Â³ Pending | 0/200 | 0/4 | High | 5 |
| Navigation | Ã¢Â³ Pending | 0/150 | 0/2 | High | 5 |
| ViewModels | Ã¢Â³ Pending | 0/600 | 0/6 | High | 6 |
| UI Screens | Ã¢Â³ Pending | 0/1000 | 0/8 | High | 6-7 |
| Testing | Ã¢Â³ Pending | 0/400 | 0/4 | Medium | 8 |

**Legend**: Ã¢Å“â€¦ Complete | Ã°Å¸Å¡Â§ In Progress | Ã¢Â³ Pending | Ã¢Å¡ Ã¯Â¸ Blocked | âŒ Failed

---

## Ã°Å¸â€”â€šÃ¯Â¸ FILE INVENTORY

### Ã¢Å“â€¦ COMPLETED FILES (17 files, ~1,560 lines)

#### Configuration (3 files)
- [x] `build.gradle.kts` (Module) - Dependencies, plugins
- [x] `build.gradle.kts` (Project) - Root config
- [x] `AndroidManifest.xml` - Permissions, app config

#### Application Core (2 files)
- [x] `MainActivity.kt` - Entry point
- [x] `SmartSalesApplication.kt` - Hilt setup

#### Database Entities (6 files)
- [x] `entity/ConversationEntity.kt` - Conversation table
- [x] `entity/MessageEntity.kt` - Messages table
- [x] `entity/AttachmentEntity.kt` - Attachments table
- [x] `entity/DeviceEntity.kt` - Devices table
- [x] `entity/WifiConfigEntity.kt` - WiFi configs table
- [x] `entity/ExportRecordEntity.kt` - Export history table

#### Database DAOs (6 files) - Included in entities files
- [x] `dao/ConversationDao` - Conversation queries
- [x] `dao/MessageDao` - Message queries
- [x] `dao/AttachmentDao` - Attachment queries
- [x] `dao/DeviceDao` - Device queries
- [x] `dao/WifiConfigDao` - WiFi queries
- [x] `dao/ExportRecordDao` - Export queries

#### Database (1 file)
- [x] `local/database/SmartSalesDatabase.kt` - Room database

#### Repositories (3 files)
- [x] `repository/ConversationRepository.kt` - Conversation logic
- [x] `repository/DeviceRepository.kt` - Device logic
- [x] `repository/ExportRepository.kt` - Export logic

#### Domain Models (1 file)
- [x] `domain/model/Models.kt` - Business models

#### Bluetooth (2 files)
- [x] `bluetooth/BleConstants.kt` - BLE constants
- [x] `bluetooth/BleManager.kt` - BLE operations

#### Dependency Injection (2 files)
- [x] `di/DatabaseModule.kt` - Database DI
- [x] `di/RepositoryModule.kt` - Repository DI

---

## Ã¢Â³ PENDING FILES (35 files, ~3,500 lines)

### Ã°Å¸Å¡à¸‡ PHASE 4A: Network Layer (5 files, ~400 lines) - NEXT!

#### API Configuration
- [ ] `data/remote/api/QwenConfig.kt` (80 lines)
  - API keys, endpoints
  - Request configuration
  - Model selection

#### API Interfaces
- [ ] `data/remote/api/DashscopeApi.kt` (100 lines)
  - Chat completion endpoint
  - Streaming support
  - Error handling

- [ ] `data/remote/api/TingwuApi.kt` (80 lines)
  - Audio upload endpoint
  - Transcription job creation
  - Result polling

- [ ] `data/remote/api/GadgetApi.kt` (60 lines)
  - File listing endpoint
  - File download endpoint
  - Device status endpoint

#### Data Transfer Objects
- [ ] `data/remote/dto/ApiModels.kt` (80 lines)
  - Request/Response DTOs
  - Error response models
  - API result wrappers

#### Dependency Injection
- [ ] `di/NetworkModule.kt` (UPDATE - add 50 lines)
  - Retrofit instances
  - OkHttp configuration
  - API interface providers

---

### PHASE 4B: Business Logic (6 files, ~800 lines)

#### AI Chat Manager
- [ ] `domain/manager/AiChatManager.kt` (200 lines)
  - Chat session management
  - Message streaming
  - Context management
  - Error retry logic

#### Audio Transcription Manager
- [ ] `domain/manager/AudioTranscriptionManager.kt` (150 lines)
  - File upload to Tingwu
  - Job status polling
  - Speaker diarization
  - Formatted results

#### File Sync Manager
- [ ] `domain/manager/FileSyncManager.kt` (150 lines)
  - HTTP file sync with gadget
  - Progress tracking
  - Conflict resolution
  - Background sync

#### CRM Extraction Manager
- [ ] `domain/manager/CrmExtractionManager.kt` (120 lines)
  - Parse transcription
  - Extract customer info
  - Validate data
  - Store to database

#### Prompt Template Manager
- [ ] `domain/manager/PromptTemplateManager.kt` (100 lines)
  - System prompts
  - CRM extraction templates
  - Chat templates
  - Template variables

#### Use Cases
- [ ] `domain/usecase/UseCases.kt` (80 lines)
  - StartRecordingUseCase
  - ProcessConversationUseCase
  - ExportDataUseCase
  - SyncFilesUseCase

---

### PHASE 5A: Export System (2 files, ~300 lines)

- [ ] `data/export/PdfExporter.kt` (150 lines)
  - Conversation to PDF
  - CRM data report
  - Custom styling
  - iText library usage

- [ ] `data/export/CsvExporter.kt` (150 lines)
  - Conversation to CSV
  - CRM data export
  - Column formatting
  - Apache POI usage

---

### PHASE 5B: UI Theme & Navigation (6 files, ~350 lines)

#### Theme
- [ ] `ui/theme/Color.kt` (60 lines)
  - Material 3 color scheme
  - Light/dark themes
  - Brand colors

- [ ] `ui/theme/Type.kt` (50 lines)
  - Typography scale
  - Font families
  - Text styles

- [ ] `ui/theme/Theme.kt` (80 lines)
  - MaterialTheme setup
  - Dynamic colors
  - System UI controller

#### Navigation
- [ ] `ui/navigation/Screen.kt` (40 lines)
  - Sealed class for routes
  - Route parameters
  - Deep links

- [ ] `ui/navigation/NavGraph.kt` (120 lines)
  - NavHost setup
  - Screen composables
  - Navigation logic
  - Argument passing

---

### PHASE 6: ViewModels (6 files, ~600 lines)

- [ ] `ui/viewmodel/ConversationListViewModel.kt` (120 lines)
  - Load conversations
  - Search/filter
  - Delete conversations
  - Time grouping

- [ ] `ui/viewmodel/ChatViewModel.kt` (150 lines)
  - Send/receive messages
  - Streaming responses
  - Attachment handling
  - Export actions

- [ ] `ui/viewmodel/DevicePairingViewModel.kt` (100 lines)
  - BLE scanning
  - Device connection
  - WiFi configuration
  - Connection status

- [ ] `ui/viewmodel/FileSyncViewModel.kt` (100 lines)
  - File discovery
  - Sync progress
  - Conflict resolution
  - Error handling

- [ ] `ui/viewmodel/TranscriptionViewModel.kt` (80 lines)
  - Audio upload
  - Job status
  - Result display
  - CRM extraction

- [ ] `ui/viewmodel/ExportViewModel.kt` (50 lines)
  - Export format selection
  - Export progress
  - Share actions
  - History display

---

### PHASE 7: UI Screens (8 files, ~1,000 lines)

- [ ] `ui/screen/ConversationListScreen.kt` (150 lines)
  - List of conversations
  - Time-based grouping
  - Search bar
  - FAB for new chat

- [ ] `ui/screen/ChatScreen.kt` (200 lines)
  - Message list
  - Input field
  - Attachment picker
  - Streaming display

- [ ] `ui/screen/DevicePairingScreen.kt` (120 lines)
  - Scan button
  - Device list
  - Connection status
  - WiFi config form

- [ ] `ui/screen/FileSyncScreen.kt` (120 lines)
  - File list from gadget
  - Sync buttons
  - Progress indicators
  - Conflict UI

- [ ] `ui/screen/TranscriptionScreen.kt` (100 lines)
  - File picker
  - Upload button
  - Progress display
  - Results viewer

- [ ] `ui/screen/ExportScreen.kt` (80 lines)
  - Format selection
  - Conversation picker
  - Export button
  - Share dialog

- [ ] `ui/screen/SettingsScreen.kt` (120 lines)
  - API key inputs
  - Default WiFi
  - Theme selection
  - About section

- [ ] `ui/screen/components/CommonComponents.kt` (110 lines)
  - MessageBubble
  - LoadingIndicator
  - ErrorDialog
  - ConfirmDialog
  - FileItem

---

### PHASE 8: Testing (4 files, ~400 lines)

- [ ] `test/repository/ConversationRepositoryTest.kt` (100 lines)
- [ ] `test/manager/AiChatManagerTest.kt` (100 lines)
- [ ] `test/viewmodel/ChatViewModelTest.kt` (100 lines)
- [ ] `androidTest/ui/ChatScreenTest.kt` (100 lines)

---

## Ã°Å¸"Â§ CODING STANDARDS

### File Organization
```
SmartSalesAssistant/
Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ app/
Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ build.gradle.kts
Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ src/main/
Ã¢"â€š       Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ AndroidManifest.xml
Ã¢"â€š       Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ java/com/smartsales/
Ã¢"â€š       Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ MainActivity.kt
Ã¢"â€š       Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ SmartSalesApplication.kt
Ã¢"â€š       Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ data/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ local/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ entity/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ dao/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ database/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ remote/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ api/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ dto/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ bluetooth/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ repository/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ export/
Ã¢"â€š       Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ domain/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ model/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ manager/
Ã¢"â€š       Ã¢"â€š   Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ usecase/
Ã¢"â€š       Ã¢"â€š   Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ di/
Ã¢"â€š       Ã¢"â€š   Ã¢""Ã¢"â‚¬Ã¢"â‚¬ ui/
Ã¢"â€š       Ã¢"â€š       Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ theme/
Ã¢"â€š       Ã¢"â€š       Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ navigation/
Ã¢"â€š       Ã¢"â€š       Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ viewmodel/
Ã¢"â€š       Ã¢"â€š       Ã¢""Ã¢"â‚¬Ã¢"â‚¬ screen/
Ã¢"â€š       Ã¢""Ã¢"â‚¬Ã¢"â‚¬ res/
Ã¢"â€š           Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ values/
Ã¢"â€š           Ã¢"Å“Ã¢"â‚¬Ã¢"â‚¬ drawable/
Ã¢"â€š           Ã¢""Ã¢"â‚¬Ã¢"â‚¬ xml/
Ã¢""Ã¢"â‚¬Ã¢"â‚¬ build.gradle.kts
```

### Naming Conventions
- **Entities**: `[Name]Entity.kt` (e.g., `ConversationEntity`)
- **DAOs**: `[Name]Dao` interface (e.g., `ConversationDao`)
- **Repositories**: `[Name]Repository.kt` (e.g., `ConversationRepository`)
- **ViewModels**: `[Screen]ViewModel.kt` (e.g., `ChatViewModel`)
- **Screens**: `[Name]Screen.kt` (e.g., `ChatScreen`)
- **Managers**: `[Purpose]Manager.kt` (e.g., `AiChatManager`)

### Code Style
- **Indentation**: 4 spaces
- **Line Length**: Max 120 characters
- **Imports**: Organize alphabetically
- **Documentation**: KDoc for public APIs
- **Coroutines**: Use suspend functions
- **StateFlow**: For UI state
- **Flow**: For reactive data

### Dependency Injection
- All dependencies via Hilt `@Inject`
- Modules: `@Module @InstallIn(SingletonComponent::class)`
- ViewModels: `@HiltViewModel`
- Scope: `@Singleton` for app-level

### Error Handling
- Sealed classes for Results
- Try-catch in repository/manager layers
- User-friendly error messages
- Logging for debugging

---

## Ã°Å¸"' API KEYS & CONFIGURATION

### Required Keys (Not in repository)
```kotlin
// In QwenConfig.kt
object QwenConfig {
    const val API_KEY = "YOUR_DASHSCOPE_KEY" // TODO: Use BuildConfig
    const val BASE_URL = "https://dashscope.aliyuncs.com/api/v1/"
    const val MODEL_NAME = "qwen-turbo"
}

// In TingwuConfig.kt (to be created)
object TingwuConfig {
    const val APP_KEY = "YOUR_TINGWU_KEY"
    const val BASE_URL = "https://tingwu.aliyun.com/api/v1/"
}
```

### Build Configuration
```gradle
// In build.gradle.kts
android {
    defaultConfig {
        buildConfigField("String", "QWEN_API_KEY", "\"${project.findProperty("QWEN_API_KEY")}\"")
        buildConfigField("String", "TINGWU_APP_KEY", "\"${project.findProperty("TINGWU_APP_KEY")}\"")
    }
}
```

---

## Ã°Å¸Â§Âª TESTING STRATEGY

### Unit Tests
- Repository layer tests with fake DAOs
- Manager layer tests with mocked dependencies
- ViewModel tests with test coroutines

### Integration Tests
- Database migrations
- API communication
- BLE connection flow

### UI Tests
- Screen navigation
- User interactions
- State changes

---

## Ã°Å¸"â€ž VERSION CONTROL

### Git Ignore
```
*.iml
.gradle/
local.properties
.idea/
.DS_Store
build/
captures/
.externalNativeBuild/
.cxx/
*.apk
*.ap_
*.aab
```

### Commit Message Format
```
[Component] Brief description

- Detail 1
- Detail 2

Session: X | Lines: +/-X
```

---

## Ã°Å¸"â€¹ SESSION CHECKLIST

### Before Each Session
- [ ] Review this manifest
- [ ] Check previous session's output
- [ ] Identify next phase components
- [ ] Verify dependencies are ready

### During Session
- [ ] Create files in correct structure
- [ ] Follow naming conventions
- [ ] Add proper documentation
- [ ] Update progress tracking
- [ ] Test compilation

### After Session
- [ ] Update this manifest
- [ ] Create progress report
- [ ] Archive completed files
- [ ] Document any blockers
- [ ] Plan next session

---

## Ã¢Å¡ Ã¯Â¸ KNOWN ISSUES & BLOCKERS

### Current Issues
None - Clean build!

### Potential Risks
1. **API Rate Limits** - Need to handle gracefully
2. **BLE Stability** - Test on multiple devices
3. **File Size Limits** - Large audio files
4. **Memory Management** - Audio processing

---

## Ã°Å¸"Å¡ DEPENDENCIES REFERENCE

### Current Dependencies (from build.gradle.kts)
```kotlin
// Core Android
androidx.core:core-ktx:1.12.0
androidx.lifecycle:lifecycle-runtime-ktx:2.7.0
androidx.activity:activity-compose:1.8.2

// Compose
androidx.compose.ui:ui:1.6.1
androidx.compose.material3:material3:1.2.0
androidx.compose.ui:ui-tooling-preview:1.6.1

// Room Database
androidx.room:room-runtime:2.6.1
androidx.room:room-ktx:2.6.1
kapt androidx.room:room-compiler:2.6.1

// Hilt DI
com.google.dagger:hilt-android:2.50
kapt com.google.dagger:hilt-compiler:2.50
androidx.hilt:hilt-navigation-compose:1.1.0

// Network (to be added)
com.squareup.retrofit2:retrofit:2.9.0
com.squareup.retrofit2:converter-gson:2.9.0
com.squareup.okhttp3:logging-interceptor:4.12.0

// Export (to be added)
com.itextpdf:itext7-core:7.2.5
org.apache.poi:poi-ooxml:5.2.5

// Testing
junit:junit:4.13.2
androidx.test.ext:junit:1.1.5
androidx.test.espresso:espresso-core:3.5.1
androidx.compose.ui:ui-test-junit4:1.6.1
```

---

## ðŸŽ¯ SUCCESS CRITERIA

### Phase Completion
- [ ] All files compile without errors
- [ ] All dependencies resolve
- [ ] No placeholder code (TODOs are OK)
- [ ] Follows architecture patterns
- [ ] Documentation complete

### Project Completion
- [ ] All features working end-to-end
- [ ] UI polished and responsive
- [ ] Error handling robust
- [ ] Performance optimized
- [ ] Ready for production testing

---

## Ã°Å¸"Å¾ CONTACT & RESOURCES

### Documentation
- Android Docs: https://developer.android.com
- Compose: https://developer.android.com/jetpack/compose
- Room: https://developer.android.com/training/data-storage/room
- Hilt: https://developer.android.com/training/dependency-injection/hilt-android

### APIs
- Qwen/Dashscope: https://help.aliyun.com/zh/dashscope
- Tingwu: https://www.aliyun.com/product/tingwu

---

## Ã°Å¸"Å  METRICS

### Code Quality
- Lines of Code: 1,560 / 5,000 (31%)
- Files Created: 17 / 52 (33%)
- Test Coverage: 0% (Target: 60%)
- TODO Count: 0 (Keep it 0!)

### Time Estimates
- **Completed**: ~12 hours (Sessions 1-3)
- **In Progress**: ~4 hours (Session 4 - Network)
- **Remaining**: ~16 hours (Sessions 5-8)
- **Total Estimate**: ~32 hours

---

**Last Updated**: Session 4  
**Next Priority**: Network Layer (Phase 4A)  
**Status**: Ã°Å¸Å¡Å“ Building...

