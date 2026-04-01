package com.please.stop.app.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/**
 * Configure Compose-specific options
 */
internal fun Project.configureAndroidCompose(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {

        buildFeatures.apply {
            compose = true
        }

        testOptions.apply {
            unitTests.apply {
                isIncludeAndroidResources = true
            }
        }
    }
}
