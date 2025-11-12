import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.smartsales"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartsales"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        vectorDrawables {
            useSupportLibrary = true
        }

        // Load API keys from local.properties
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }

        buildConfigField(
            "String",
            "DASHSCOPE_API_KEY",
            "\"${properties.getProperty("DASHSCOPE_API_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "DASHSCOPE_APP_KEY",
            "\"${properties.getProperty("DASHSCOPE_APP_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "TINGWU_API_KEY",
            "\"${properties.getProperty("TINGWU_API_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "TINGWU_APP_KEY",
            "\"${properties.getProperty("TINGWU_APP_KEY", "")}\"",
        )
        buildConfigField(
            "String",
            "ALI_OSS_ENDPOINT",
            "\"${properties.getProperty("ALI_OSS_ENDPOINT", "")}\"",
        )
        buildConfigField(
            "String",
            "ALI_OSS_BUCKET",
            "\"${properties.getProperty("ALI_OSS_BUCKET", "")}\"",
        )
        buildConfigField(
            "String",
            "ALI_OSS_ACCESS_KEY_ID",
            "\"${properties.getProperty("ALI_OSS_ACCESS_KEY_ID", "")}\"",
        )
        buildConfigField(
            "String",
            "ALI_OSS_ACCESS_KEY_SECRET",
            "\"${properties.getProperty("ALI_OSS_ACCESS_KEY_SECRET", "")}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
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
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = true
    }
}

dependencies {
    implementation(project(":device-connectivity"))
    implementation(project(":ai-core"))

    // ===== ANDROID CORE =====
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // ===== JETPACK COMPOSE =====
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.com.google.android.material)
    implementation(libs.androidx.navigation.compose)

    // Compose Debug
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // ===== HILT DEPENDENCY INJECTION =====
    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // ===== ROOM DATABASE =====
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)

    // ===== RETROFIT & NETWORKING =====
    implementation(libs.retrofit)
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // ===== GSON =====
    implementation("com.google.code.gson:gson:2.10.1")

    // ===== COROUTINES =====
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ===== LIFECYCLE =====
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // ===== TESTING =====
    testImplementation(libs.junit)
    testImplementation(libs.mockwebserver)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.kotlin)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.navigation.testing)
}

// Allow references to generated code
kapt {
    correctErrorTypes = true
}
