plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.example.scenicguide"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.scenicguide"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"

        // 默认走 adb reverse：先执行 adb reverse tcp:8000 tcp:8000，模拟器和真机都通用。
        // 后端在别的机器上时用命令行覆盖：./gradlew assembleDebug -Pscenic.baseUrl=http://192.168.1.10:8000/
        val baseUrl = (findProperty("scenic.baseUrl") as String?) ?: "http://127.0.0.1:8000/"
        buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.02.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.core:core-ktx:1.19.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.datastore:datastore-preferences:1.3.0")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.1")
    implementation("com.squareup.okhttp3:logging-interceptor:5.5.1")

    // 注意：kotlinx-serialization 1.13.0 起依赖 kotlin-stdlib 2.4，而 AGP 9.4 内置 Kotlin 编译器是 2.2，
    // 会出现「metadata version 2.4.0 无法读取」，这里固定在 2.2 线编译的版本。
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // 图片加载（AsyncImage）
    implementation("io.coil-kt:coil-compose:2.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
