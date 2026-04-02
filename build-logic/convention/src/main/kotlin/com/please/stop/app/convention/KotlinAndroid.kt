package com.please.stop.app.convention

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import kotlin.text.get
import kotlin.toString

private const val CompileSDK = "compileSdk"
private const val MinSDK = "minSdk"
private const val TargetSDK = "targetSdk"


fun Project.getCompileSDK(): Int {
    return libs.findVersion(CompileSDK).get().toString().toInt()
}

fun Project.getMinSDK(): Int {
    return libs.findVersion(MinSDK).get().toString().toInt()
}

fun Project.getTargetSDK(): Int {
    return libs.findVersion(TargetSDK).get().toString().toInt()
}

/**
 * Configure base Kotlin with Android options
 */
fun Project.configureKotlinAndroid(
    commonExtension: CommonExtension,
) {
    commonExtension.apply {
        compileSdk = getCompileSDK()

        defaultConfig.apply {
            minSdk = getMinSDK()
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        val javaVersion = JavaVersion.toVersion(libs.findVersion("jvmTarget").get().toString())
        compileOptions.apply {
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            isCoreLibraryDesugaringEnabled = true
        }
    }

    configureKotlin()
}

/**
 * Configure base Kotlin options
 */
fun Project.configureKotlin() {
    // Use withType to workaround https://youtrack.jetbrains.com/issue/KT-55947
    tasks.withType<KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(libs.findVersion("jvmTarget").get().toString()))
            allWarningsAsErrors.set(
                project.findProperty("warningsAsErrors")?.toString().toBoolean()
            )
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                "-opt-in=kotlinx.coroutines.FlowPreview",
                "-Xdebug"
            )
        }
    }
}
