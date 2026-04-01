package com.please.stop.app.convention

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType

/**
 * Configures the detekt plugin.
 */
internal fun Project.configureDetekt(extension: DetektExtension) {
    extension.apply {
        toolVersion = libs.findVersion("detekt").get().toString()
        config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
        parallel = true
        buildUponDefaultConfig = true
        autoCorrect = true
    }

    tasks.withType<Detekt>().configureEach {
        exclude("**/**.md")
        exclude("**/*.kts")
        exclude("**/build/**")
    }

    tasks.withType<Detekt> detekt@{
        setSource(files(project.projectDir))
    }
}
