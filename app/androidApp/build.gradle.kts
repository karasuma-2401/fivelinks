import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

val debugOnlineServerUrl = providers.gradleProperty("onlineDebugServerUrl")
    .orElse("ws://localhost:8080/ws")
val releaseOnlineServerUrl = providers.gradleProperty("onlineReleaseServerUrl")
    .orElse("")

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(projects.app.shared)

    implementation(libs.androidx.activity.compose)

    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

// The game plays with the heuristic AI. The ONNX model (~260 MB) and its runtime only
// serve :server, so they stay out of the APK (see also packaging below).
configurations.configureEach {
    exclude(group = "com.microsoft.onnxruntime")
}

// Release signing comes from keystore.properties at project root or environment variables (CI secret store).
// Without it, release builds are produced unsigned.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStorePath = keystoreProperties.getProperty("storeFile") ?: System.getenv("KEYSTORE_FILE")
val releaseStorePassword = keystoreProperties.getProperty("storePassword") ?: System.getenv("KEYSTORE_PASSWORD")
val releaseKeyAlias = keystoreProperties.getProperty("keyAlias") ?: System.getenv("KEY_ALIAS")
val releaseKeyPassword = keystoreProperties.getProperty("keyPassword") ?: System.getenv("KEY_PASSWORD")

android {
    namespace = "com.karasuma.fivelinks.fivelinks_cmp"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.karasuma.fivelinks.fivelinks_cmp"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "1.0.1"
    }
    signingConfigs {
        if (!releaseStorePath.isNullOrBlank() && rootProject.file(releaseStorePath).exists()) {
            create("release") {
                storeFile = rootProject.file(releaseStorePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "models/**"
        }
    }
    buildTypes {
        getByName("debug") {
            // adb reverse lets a USB-connected phone reach this address on the development laptop.
            buildConfigField("String", "ONLINE_SERVER_URL", buildConfigString(debugOnlineServerUrl.get()))
        }
        getByName("release") {
            // Deliberately blank unless CI supplies -PonlineReleaseServerUrl=wss://…/ws.
            buildConfigField("String", "ONLINE_SERVER_URL", buildConfigString(releaseOnlineServerUrl.get()))
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
