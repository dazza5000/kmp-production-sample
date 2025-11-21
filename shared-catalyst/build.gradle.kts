import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    val xcf = XCFramework("RssReader")

    /**
     * Kotlin Gradle plugin does not allow 2 targets of the same type (e.g. iosSimulatorArm64) in a single project.
     * To work around this limitation, we can create a separate Gradle project for the Catalyst target, and use it
     * as a "wrapper" around original shared project.
     */
    listOf(
        iosX64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "RssReader"
            isStatic = true
            binaryOption("macabi", "true")
            // Re-export the original project.
            export(project(":shared"))
            xcf.add(this)
        }
    }

    jvm()

    sourceSets {
        commonMain.dependencies {
            api(project(":shared"))
        }
    }
}