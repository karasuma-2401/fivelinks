import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm()

    sourceSets {
        jvmMain.dependencies {
            implementation(projects.app.shared)
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
        }
    }
}

compose.desktop {
    application {
        mainClass = "com.karasuma.fivelinks.fivelinks_cmp.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "FiveLink"
            packageVersion = "1.0.0"

            windows { iconFile.set(project.file("icons/fivelink.ico")) }
            macOS { iconFile.set(project.file("icons/fivelink.icns")) }
            linux { iconFile.set(project.file("icons/fivelink.png")) }
        }
    }
}
