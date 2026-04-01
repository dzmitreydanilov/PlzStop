import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import com.please.stop.app.convention.configureKotlin

class KotlinMultiplatformLibraryConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("org.jetbrains.kotlin.multiplatform")
            }

            extensions.configure<KotlinMultiplatformExtension> {

                applyDefaultHierarchyTemplate()

                listOf(
                    iosArm64(),
                    iosSimulatorArm64(),
                ).forEach { target ->
                    target.binaries.framework {
                        baseName = "ComposeApp"
                        isStatic = true
                    }
                }

                configureKotlin()
            }
        }
    }
}
