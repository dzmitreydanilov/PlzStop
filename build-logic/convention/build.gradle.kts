@file:OptIn(kotlin.ExperimentalStdlibApi::class)

import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
}

group = "com.please.stop.test.build-logic"

java {
    sourceCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
    targetCompatibility = JavaVersion.toVersion(libs.versions.jvmTarget.get())
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        val javaVersion = libs.versions.jvmTarget.map { version ->
            @Suppress("DEPRECATION")
            JvmTarget.values().first { it.target == version }
        }
        jvmTarget.set(javaVersion)
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    testImplementation(kotlin("test"))
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.detekt.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplicationCompose") {
            id = "android.application.compose"
            implementationClass = "AndroidApplicationComposeConventionPlugin"
        }
        register("androidApplication") {
            id = "android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("kotlinDetekt") {
            id = "kotlin.detekt"
            implementationClass = "DetektConventionPlugin"
        }
        register("kmpLibrary") {
            id = "kmp.library"
            implementationClass = "KotlinMultiplatformLibraryConventionPlugin"
        }

        register("androidTest") {
            id = "android.test"
            implementationClass = "AndroidTestConventionPlugin"
        }
    }
}
