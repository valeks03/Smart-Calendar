plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.24-1.0.20"
}

android {
    namespace = "com.example.smartcalendar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.smartcalendar"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")// для Theme.Material3.*
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}