import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.plugin.serialization) version "2.0.21"
}

fun String.escapeForJavaString(): String = replace("\\", "\\\\").replace("\"", "\\\"")

val siliconflowApiKey: String =
    run {
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }
        properties.getProperty("SILICONFLOW_API_KEY")
            ?: System.getenv("SILICONFLOW_API_KEY")
            ?: ""
    }.escapeForJavaString()

val dashscopeApiKey: String =
    run {
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }
        properties.getProperty("DASHSCOPE_API_KEY")
            ?: System.getenv("DASHSCOPE_API_KEY")
            ?: "sk-4ec0c58f7a5c442680e1f5b56c590b3f"
    }.escapeForJavaString()

android {
    namespace = "top.isyuah.dev.yumuzk.mpipemvp"
    compileSdk = 35

    defaultConfig {
        applicationId = "top.isyuah.dev.yumuzk.mpipemvp"
        minSdk = 31
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SILICONFLOW_API_KEY", "\"$siliconflowApiKey\"")
        buildConfigField("String", "DASHSCOPE_API_KEY", "\"$dashscopeApiKey\"")
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
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    val camerax_version = "1.3.0"
    implementation("androidx.camera:camera-core:$camerax_version")
    implementation("androidx.camera:camera-camera2:$camerax_version")
    implementation("androidx.camera:camera-lifecycle:$camerax_version")
    implementation("androidx.camera:camera-view:$camerax_version")
    
    implementation(libs.mediapipe.tasks.vision)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    // Image loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    
    // OpenCV for Android
    implementation("com.quickbirdstudios:opencv-contrib:4.5.3.0")

    // Markdown Rendering
    implementation(libs.markwon.core)
    implementation(libs.markwon.ext.tables)
    implementation(libs.markwon.ext.strikethrough)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
