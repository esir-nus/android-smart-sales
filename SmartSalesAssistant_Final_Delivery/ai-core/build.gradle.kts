plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.smartsales.aicore"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    lint {
        baseline = file("lint-baseline.xml")
        warningsAsErrors = true
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.hilt.android)
    kapt(libs.hilt.android.compiler)

    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.coroutines.android)

    api(libs.retrofit)
    api("com.squareup.retrofit2:converter-gson:2.9.0")
    api(libs.okhttp)
    api(libs.okhttp.logging.interceptor)
    api("com.google.code.gson:gson:2.10.1")

    testImplementation(libs.junit)
}

kapt {
    correctErrorTypes = true
}
