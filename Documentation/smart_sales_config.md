# Smart Sales Assistant - Project Configuration

## 4. é¡¹ç›®é…ç½®æ–‡ä»¶

### 4.1 build.gradle.kts (Project Level)

```kotlin
// build.gradle.kts (Project level)
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("com.android.library") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.20" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("com.google.devtools.ksp") version "1.9.20-1.0.14" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20" apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
```

---

### 4.2 build.gradle.kts (App Module)

```kotlin
// build.gradle.kts (App level)
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.smartsales"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartsales"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.4"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.01.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // Room Database
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // OkHttp & Retrofit (if needed)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Hilt (Dependency Injection)
    implementation("com.google.dagger:hilt-android:2.48")
    ksp("com.google.dagger:hilt-compiler:2.48")
    
    // Bluetooth
    implementation("androidx.bluetooth:bluetooth:1.0.0-alpha02")
    
    // DataStore (for preferences)
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    
    // Coil (Image Loading)
    implementation("io.coil-kt:coil-compose:2.5.0")
    
    // Accompanist (Permissions, etc.)
    implementation("com.google.accompanist:accompanist-permissions:0.33.2-alpha")
    
    // WorkManager (for background tasks)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

---

### 4.3 AndroidManifest.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- ç½‘ç»œæƒé™ -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- è“ç‰™æƒé™ -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN"
        android:usesPermissionFlags="neverForLocation" />
    
    <!-- ä½ç½®æƒé™ (BLEæ‰«æéœ€è¦) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- å­˜å‚¨æƒé™ -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
        android:maxSdkVersion="32" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="29"
        tools:ignore="ScopedStorage" />
    
    <!-- å½•éŸ³æƒé™ -->
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
    
    <!-- WiFiæƒé™ -->
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
    
    <!-- å‰å°æœåŠ¡æƒé™ -->
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    
    <!-- BLEç‰¹æ€§å£°æ˜Ž -->
    <uses-feature
        android:name="android.hardware.bluetooth_le"
        android:required="true" />

    <application
        android:name=".SmartSalesApplication"
        android:allowBackup="true"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="@xml/backup_rules"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.SmartSales"
        android:usesCleartextTraffic="true"
        tools:targetApi="31">
        
        <!-- ä¸»Activity -->
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SmartSales"
            android:windowSoftInputMode="adjustResize">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
        
        <!-- FileProvider for sharing files -->
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
        
        <!-- æ–‡ä»¶åŒæ­¥æœåŠ¡ (å¯é€‰) -->
        <service
            android:name=".services.FileSyncService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />
    </application>

</manifest>
```

---

### 4.4 SmartSalesApplication.kt

```kotlin
package com.example.smartsales

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class SmartSalesApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // åˆå§‹åŒ–Timberæ—¥å¿—
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        
        // åˆå§‹åŒ–Qwen API Key
        QwenConfig.setApiKey(getQwenApiKey())
        
        Timber.d("SmartSales Application Created")
    }
    
    private fun getQwenApiKey(): String {
        // TODO: ä»Žå®‰å…¨å­˜å‚¨ä¸­è¯»å–API Key
        // å»ºè®®ä½¿ç”¨åŠ å¯†çš„SharedPreferencesæˆ–å…¶ä»–å®‰å…¨æ–¹æ¡ˆ
        return BuildConfig.QWEN_API_KEY
    }
}
```

---

### 4.5 MainActivity.kt

```kotlin
package com.example.smartsales

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.smartsales.ui.SmartSalesApp
import com.example.smartsales.ui.theme.SmartSalesTheme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        permissions.entries.forEach {
            Timber.d("${it.key} = ${it.value}")
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // è¯·æ±‚å¿…è¦æƒé™
        requestPermissions()
        
        setContent {
            SmartSalesTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartSalesApp()
                }
            }
        }
    }
    
    private fun requestPermissions() {
        val permissions = mutableListOf<String>()
        
        // è“ç‰™æƒé™
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        // ä½ç½®æƒé™ (BLEéœ€è¦)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        
        // å½•éŸ³æƒé™
        permissions.add(Manifest.permission.RECORD_AUDIO)
        
        // å­˜å‚¨æƒé™
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q) {
                permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }
        
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }
}
```

---

### 4.6 Hilt Moduleé…ç½®

```kotlin
package com.example.smartsales.di

import android.content.Context
import androidx.room.Room
import com.example.smartsales.data.local.SmartSalesDatabase
import com.example.smartsales.data.repository.*
import com.example.smartsales.network.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SmartSalesDatabase {
        return Room.databaseBuilder(
            context,
            SmartSalesDatabase::class.java,
            "smart_sales_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }
    
    @Provides
    @Singleton
    fun provideConversationDao(database: SmartSalesDatabase) = database.conversationDao()
    
    @Provides
    @Singleton
    fun provideMessageDao(database: SmartSalesDatabase) = database.messageDao()
    
    @Provides
    @Singleton
    fun provideAttachmentDao(database: SmartSalesDatabase) = database.attachmentDao()
    
    @Provides
    @Singleton
    fun provideWifiConfigDao(database: SmartSalesDatabase) = database.wifiConfigDao()
    
    @Provides
    @Singleton
    fun provideDeviceSettingDao(database: SmartSalesDatabase) = database.deviceSettingDao()
    
    @Provides
    @Singleton
    fun provideCrmExportDao(database: SmartSalesDatabase) = database.crmExportDao()
    
    @Provides
    @Singleton
    fun provideConversationRepository(
        conversationDao: ConversationDao,
        messageDao: MessageDao,
        attachmentDao: AttachmentDao
    ): ConversationRepository {
        return ConversationRepository(conversationDao, messageDao, attachmentDao)
    }
    
    @Provides
    @Singleton
    fun provideDeviceRepository(
        deviceSettingDao: DeviceSettingDao,
        wifiConfigDao: WifiConfigDao
    ): DeviceRepository {
        return DeviceRepository(deviceSettingDao, wifiConfigDao)
    }
    
    @Provides
    @Singleton
    fun provideExportRepository(
        crmExportDao: CrmExportDao
    ): ExportRepository {
        return ExportRepository(crmExportDao)
    }
    
    @Provides
    @Singleton
    fun provideBleManager(@ApplicationContext context: Context): BleManager {
        return BleManager(context)
    }
    
    @Provides
    @Singleton
    fun provideQwenChatClient(): QwenChatClient {
        return QwenChatClient()
    }
    
    @Provides
    @Singleton
    fun provideQwenTingwuClient(): QwenTingwuClient {
        return QwenTingwuClient()
    }
    
    @Provides
    @Singleton
    fun provideAiServiceManager(
        conversationRepository: ConversationRepository
    ): AiServiceManager {
        return AiServiceManager(conversationRepository)
    }
    
    @Provides
    @Singleton
    fun provideFileSyncManager(
        @ApplicationContext context: Context,
        deviceRepository: DeviceRepository
    ): FileSyncManager {
        return FileSyncManager(context, deviceRepository)
    }
    
    @Provides
    @Singleton
    fun providePdfGenerator(@ApplicationContext context: Context): PdfGenerator {
        return PdfGenerator(context)
    }
    
    @Provides
    @Singleton
    fun provideCsvGenerator(): CsvGenerator {
        return CsvGenerator()
    }
    
    @Provides
    @Singleton
    fun provideExportManager(
        @ApplicationContext context: Context,
        conversationRepository: ConversationRepository,
        exportRepository: ExportRepository,
        aiServiceManager: AiServiceManager
    ): ExportManager {
        return ExportManager(context, conversationRepository, exportRepository, aiServiceManager)
    }
}
```

---

### 4.7 gradle.properties

```properties
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true

# AndroidX package structure
android.useAndroidX=true
android.enableJetifier=true

# Kotlin code style
kotlin.code.style=official

# Non-transitive R classes
android.nonTransitiveRAppClass=true
android.nonFinalResIds=true

# Qwen API Key (è¯·æ›¿æ¢ä¸ºå®žé™…çš„key)
QWEN_API_KEY=your_api_key_here
```

---

### 4.8 proguard-rules.pro

```proguard
# Add project specific ProGuard rules here.

# Keep data classes
-keep class com.example.smartsales.data.** { *; }

# Keep Room entities
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *

# Keep Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Keep Compose
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
```

---

### 4.9 res/xml/file_paths.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <external-files-path
        name="exports"
        path="exports/" />
    <files-path
        name="files"
        path="." />
    <cache-path
        name="cache"
        path="." />
    <files-path
        name="gadget_files"
        path="gadget_files/" />
</paths>
```

---

### 4.10 res/xml/backup_rules.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<full-backup-content>
    <include domain="database" path="." />
    <include domain="sharedpref" path="." />
    <exclude domain="database" path="room-wal" />
</full-backup-content>
```

---

### 4.11 res/xml/data_extraction_rules.xml

```xml
<?xml version="1.0" encoding="utf-8"?>
<data-extraction-rules>
    <cloud-backup>
        <include domain="database" path="." />
        <include domain="sharedpref" path="." />
    </cloud-backup>
    <device-transfer>
        <include domain="database" path="." />
        <include domain="sharedpref" path="." />
    </device-transfer>
</data-extraction-rules>
```

---

### 4.12 res/values/strings.xml

```xml
<resources>
    <string name="app_name">æ™ºèƒ½é”€å”®åŠ©æ‰‹</string>
    
    <!-- ä¸»ç•Œé¢ -->
    <string name="tab_chat">èŠå¤©</string>
    <string name="tab_history">åŽ†å²</string>
    <string name="tab_device">è®¾å¤‡</string>
    <string name="tab_files">æ–‡ä»¶</string>
    <string name="tab_settings">è®¾ç½®</string>
    
    <!-- èŠå¤©ç•Œé¢ -->
    <string name="input_hint">è¾“å…¥æ¶ˆæ¯...</string>
    <string name="btn_send">å‘é€</string>
    <string name="btn_analyze_customer">åˆ†æžå®¢æˆ·</string>
    <string name="btn_generate_pdf">ç”ŸæˆPDF</string>
    <string name="btn_generate_csv">ç”ŸæˆCSV</string>
    
    <!-- è®¾å¤‡ç®¡ç† -->
    <string name="scan_devices">æ‰«æè®¾å¤‡</string>
    <string name="connect">è¿žæŽ¥</string>
    <string name="disconnect">æ–­å¼€</string>
    <string name="device_connected">å·²è¿žæŽ¥</string>
    <string name="device_disconnected">æœªè¿žæŽ¥</string>
    
    <!-- æƒé™ -->
    <string name="permission_bluetooth">éœ€è¦è“ç‰™æƒé™æ¥è¿žæŽ¥è®¾å¤‡</string>
    <string name="permission_location">éœ€è¦ä½ç½®æƒé™æ¥æ‰«æè“ç‰™è®¾å¤‡</string>
    <string name="permission_storage">éœ€è¦å­˜å‚¨æƒé™æ¥ä¿å­˜æ–‡ä»¶</string>
    <string name="permission_audio">éœ€è¦å½•éŸ³æƒé™æ¥å½•åˆ¶éŸ³é¢‘</string>
    
    <!-- é”™è¯¯ä¿¡æ¯ -->
    <string name="error_connection_failed">è¿žæŽ¥å¤±è´¥</string>
    <string name="error_sync_failed">åŒæ­¥å¤±è´¥</string>
    <string name="error_export_failed">å¯¼å‡ºå¤±è´¥</string>
</resources>
```

---

## ðŸŽ‰ é¡¹ç›®å®Œæ•´é…ç½®å®Œæˆ!

### å®Œæ•´åŠŸèƒ½æ¸…å•:

âœ… **æ•°æ®å±‚**
- Roomæ•°æ®åº“ + DAO + Repositoryæ¨¡å¼
- 6ä¸ªæ ¸å¿ƒå®žä½“è¡¨

âœ… **è¿žæŽ¥å±‚**
- BLEè“ç‰™æ‰«æã€é…å¯¹ã€é€šä¿¡
- WiFié…ç½®ä¼ è¾“
- HTTPæ–‡ä»¶åŒæ­¥

âœ… **AIé›†æˆ**
- Qwen DashscopeèŠå¤©API
- Qwen Tingwuè½¬å†™API
- æç¤ºè¯æ¨¡æ¿ç³»ç»Ÿ

âœ… **å¯¼å‡ºåŠŸèƒ½**
- PDFç”Ÿæˆ (å®¢æˆ·åˆ†æžã€ä¼šè®®çºªè¦)
- CSVç”Ÿæˆ (CRMæ•°æ®å¯¼å‡º)
- å¤šç§CRMæ ¼å¼æ”¯æŒ

âœ… **UIç•Œé¢**
- Jetpack ComposeçŽ°ä»£åŒ–UI
- Material Design 3
- ä¸»èŠå¤©ç•Œé¢
- åŽ†å²è®°å½•ç®¡ç†
- è®¾å¤‡é…å¯¹ç•Œé¢
- æ–‡ä»¶æŸ¥çœ‹å™¨

âœ… **é¡¹ç›®é…ç½®**
- Gradleé…ç½®
- æƒé™ç®¡ç†
- Hiltä¾èµ–æ³¨å…¥
- ProGuardæ··æ·†

### ä¸‹ä¸€æ­¥å»ºè®®:

1. **æµ‹è¯•ç¡¬ä»¶è®¾å¤‡API** - æ ¹æ®å®žé™…ç¡¬ä»¶è°ƒæ•´HTTPç«¯ç‚¹
2. **æ›¿æ¢API Key** - åœ¨gradle.propertiesä¸­é…ç½®çœŸå®žçš„Qwen APIå¯†é’¥
3. **ä¼˜åŒ–UIç»†èŠ‚** - æ ¹æ®è®¾è®¡ç¨¿è°ƒæ•´ä¸»é¢˜é¢œè‰²
4. **æ·»åŠ å•å…ƒæµ‹è¯•** - ä¸ºRepositoryå’ŒViewModelç¼–å†™æµ‹è¯•
5. **æ€§èƒ½ä¼˜åŒ–** - å›¾ç‰‡åŽ‹ç¼©ã€åˆ—è¡¨åˆ†é¡µç­‰

é¡¹ç›®ä»£ç å·²å®Œæ•´! éœ€è¦æˆ‘å¸®ä½ æ‰“åŒ…æˆå®Œæ•´çš„é¡¹ç›®ç»“æž„å—?
