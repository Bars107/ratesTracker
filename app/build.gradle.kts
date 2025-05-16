plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android.gradle.plugin) // For Hilt
    alias(libs.plugins.kotlin.serialization) // For Kotlinx Serialization
    alias(libs.plugins.ksp) // For KSP (used by Room)
}

android {
    namespace = "com.bars.exchange.tracker"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.bars.exchange.tracker"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    packaging { // Added this block
        resources.excludes.add("META-INF/INDEX.LIST")
    }
}

dependencies {
    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.compiler)

    // Room Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx) // For Coroutines and Flow support
    ksp(libs.room.compiler) // Room annotation processor with KSP

    // Ktor Client
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp) // OkHttp engine
    implementation(libs.ktor.client.content.negotiation) // For JSON, XML etc.
    implementation(libs.ktor.serialization.kotlinx.json) // Kotlinx.serialization for JSON
    implementation(libs.ktor.client.logging) // For logging
    implementation(libs.ktor.client.websockets) // For WebSockets
    implementation(libs.slf4j.simple) // Simple logging implementation for Android
    implementation(libs.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.accompanist.systemuicontroller)
    testImplementation(libs.junit)
    
    // MockK for mocking in tests
    testImplementation(libs.mockk)
    
    // Coroutines test
    testImplementation(libs.kotlinx.coroutines.test)
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}