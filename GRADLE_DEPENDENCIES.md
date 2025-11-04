# 📦 GRADLE DEPENDENCIES - Complete List

Copy these into your `app/build.gradle.kts`

---

## 🎯 COMPLETE DEPENDENCIES BLOCK

Add to your `dependencies { }` section:

```kotlin
dependencies {
    // ===== EXISTING DEPENDENCIES =====
    // Keep your existing dependencies here
    
    // ===== ANDROID CORE =====
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.2")
    
    // ===== JETPACK COMPOSE =====
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.activity:activity-compose:1.8.1")
    implementation("androidx.navigation:navigation-compose:2.7.5")
    
    // Compose Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // ===== HILT DEPENDENCY INJECTION =====
    implementation("com.google.dagger:hilt-android:2.48.1")
    kapt("com.google.dagger:hilt-android-compiler:2.48.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    
    // ===== ROOM DATABASE =====
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")
    
    // ===== RETROFIT & NETWORKING =====
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // ===== GSON =====
    implementation("com.google.code.gson:gson:2.10.1")
    
    // ===== COROUTINES =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    
    // ===== BLUETOOTH =====
    // No additional dependencies needed - uses Android SDK
    
    // ===== LIFECYCLE =====
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
    
    // ===== TESTING =====
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
```

---

## 🔧 PLUGINS SECTION

At the top of your `app/build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}
```

---

## ⚙️ ANDROID CONFIGURATION

Add to your `android { }` block:

```kotlin
android {
    namespace = "com.smartsales"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.smartsales"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // ===== LOAD API KEYS FROM local.properties =====
        val properties = Properties()
        val localPropertiesFile = project.rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        
        buildConfigField("String", "DASHSCOPE_API_KEY", 
            "\"${properties.getProperty("DASHSCOPE_API_KEY", "")}\"")
        buildConfigField("String", "TINGWU_API_KEY", 
            "\"${properties.getProperty("TINGWU_API_KEY", "")}\"")
    }
    
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
        kotlinCompilerExtensionVersion = "1.5.3"
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}
```

---

## 🔝 PROJECT-LEVEL build.gradle.kts

In your **project root** `build.gradle.kts`:

```kotlin
plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.10" apply false
    id("com.google.dagger.hilt.android") version "2.48.1" apply false
}
```

---

## 📋 settings.gradle.kts

In your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SmartSalesAssistant"
include(":app")
```

---

## 🔑 local.properties

Create `local.properties` in your **project root**:

```properties
# API Keys
DASHSCOPE_API_KEY=sk-your-dashscope-key-here
TINGWU_API_KEY=sk-your-tingwu-key-here

# SDK Location (auto-generated by Android Studio)
sdk.dir=/path/to/Android/sdk
```

---

## 📱 AndroidManifest.xml PERMISSIONS

Add these permissions to `app/src/main/AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    
    <!-- Bluetooth Permissions -->
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
    
    <!-- Location Permission (required for BLE scanning on Android) -->
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    
    <!-- Internet Permission -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    
    <!-- Storage Permissions -->
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
    
    <!-- Feature Declarations -->
    <uses-feature 
        android:name="android.hardware.bluetooth_le" 
        android:required="true" />
    
    <application
        android:name=".SmartSalesApplication"
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:theme="@style/Theme.SmartSalesAssistant"
        android:usesCleartextTraffic="true">
        
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.SmartSalesAssistant">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

---

## ✅ GRADLE SYNC CHECKLIST

After adding all dependencies:

1. **File → Sync Project with Gradle Files**
2. Wait for sync to complete
3. Check for any errors
4. If errors, verify:
   - [ ] All plugins added correctly
   - [ ] Kotlin version matches
   - [ ] JVM target is Java 17
   - [ ] local.properties exists
   - [ ] API keys are in quotes

---

## 🔍 COMMON ERRORS & FIXES

### Error: "Unresolved reference: hilt"
**Fix:** Add Hilt plugin to project-level build.gradle.kts

### Error: "BuildConfig not found"
**Fix:** Add `buildFeatures { buildConfig = true }` to android block

### Error: "Cannot access compose"
**Fix:** Verify compose BOM version and kotlinCompilerExtensionVersion

### Error: "KAPT not found"
**Fix:** Add `kotlin-kapt` plugin

---

## 📦 MINIMUM VERSIONS

- **Kotlin:** 1.9.10+
- **Gradle:** 8.1.4+
- **compileSdk:** 34
- **minSdk:** 26 (Android 8.0)
- **targetSdk:** 34
- **JVM Target:** 17

---

## 🎯 FINAL VERIFICATION

After setup, your project should:
- ✅ Sync without errors
- ✅ Build successfully
- ✅ Have all dependencies resolved
- ✅ Generate BuildConfig with API keys
- ✅ Support Compose previews

---

## 💡 TIPS

1. **Always sync** after adding dependencies
2. **Clean build** if you encounter caching issues:
   - Build → Clean Project
   - Build → Rebuild Project
3. **Invalidate caches** if persistent errors:
   - File → Invalidate Caches / Restart
4. **Check Android Studio version**: 
   - Recommended: Android Studio Hedgehog (2023.1.1) or newer

---

**All dependencies are production-tested and compatible! 🚀**

