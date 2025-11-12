import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.smartsales.aitest"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartsales.aitest"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

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
    implementation(project(":ai-core"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.com.google.android.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
}

kapt {
    correctErrorTypes = true
}
