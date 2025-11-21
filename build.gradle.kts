import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import javax.inject.Inject

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.dependencyUpdates) apply false
    alias(libs.plugins.compose.compiler) apply false
}

allprojects {
    // ./gradlew dependencyUpdates
    // Report: build/dependencyUpdates/report.txt
    apply(plugin = "com.github.ben-manes.versions")
}

/**
 * Workaround limitation of KGP (KT-59316) to create a single XCFramework with
 * iOS, Catalyst and Simulator targets.
 *
 * Note that the task is solely for demonstration purposes and likely have to be adjusted for
 * production purposes.
 */
abstract class MergeXCFrameworksTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations
) : DefaultTask() {

    /**
     * List of source XCFramework directories to merge.
     */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceXCFrameworks: ConfigurableFileCollection

    /**
     * Output directory where the merged XCFramework will be created.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * Name of the output XCFramework (without .xcframework extension).
     */
    @get:Input
    abstract val frameworkName: Property<String>

    init {
        group = "build"
        description = "Merges multiple XCFrameworks into a single XCFramework"
    }

    @TaskAction
    fun merge() {
        val sources = sourceXCFrameworks.files
        val outputDir = outputDirectory.get().asFile
        val name = frameworkName.get()
        val outputXCFramework = outputDir.resolve("$name.xcframework")

        require(sources.isNotEmpty()) {
            "At least one source XCFramework must be provided"
        }

        sources.forEach { xcframework ->
            require(xcframework.exists() && xcframework.isDirectory) {
                "Source XCFramework does not exist or is not a directory: ${xcframework.absolutePath}"
            }
        }

        fileSystemOperations.delete {
            delete(outputXCFramework)
        }

        val frameworkPaths = mutableListOf<String>()
        sources.forEach { xcframework ->
            xcframework.listFiles()?.forEach { file ->
                if (file.isDirectory && !file.name.endsWith(".plist")) {
                    file.listFiles()
                        ?.firstOrNull { it.extension == "framework" }
                        ?.let { frameworkPaths.add(it.absolutePath) }
                }
            }
        }
        require(frameworkPaths.isNotEmpty()) {
            "No frameworks found in source XCFrameworks"
        }
        execOperations.exec {
            executable = "xcodebuild"
            args("-create-xcframework")
            frameworkPaths.forEach { path ->
                args("-framework", path)
            }
            args("-output", outputXCFramework.absolutePath)
        }
    }
}

tasks.register<MergeXCFrameworksTask>("mergeXCFrameworks") {
    outputDirectory = layout.buildDirectory.dir("MergedXCFrameworks")
    frameworkName = "RssReader"

    sourceXCFrameworks.from(
        project(":shared").layout.buildDirectory.file("XCFrameworks/release/RssReader.xcframework"),
        project(":shared-catalyst").layout.buildDirectory.file("XCFrameworks/release/RssReader.xcframework")
    )
    sourceXCFrameworks.builtBy(
        project(":shared").tasks.named("assembleRssReaderXCFramework"),
        project(":shared-catalyst").tasks.named("assembleRssReaderXCFramework")
    )
}