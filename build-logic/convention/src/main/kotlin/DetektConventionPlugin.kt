import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import com.please.stop.app.convention.configureDetekt
import com.please.stop.app.convention.libs

class DetektConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("io.gitlab.arturbosch.detekt")
            val extension = extensions.getByType<DetektExtension>()
            configureDetekt(extension)

            dependencies {
                "detektPlugins"(libs.findLibrary("detekt-formatting").get())
                "detektPlugins"(libs.findLibrary("detekt-compose-formatting").get())
            }
        }
    }
}
