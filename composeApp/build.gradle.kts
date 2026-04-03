@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import com.please.stop.app.convention.getCompileSDK
import com.please.stop.app.convention.getMinSDK
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kmp.library")
    id("kotlin.detekt")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.ksp)
}
kotlin {

    android {
        compileSdk = getCompileSDK()
        minSdk = getMinSDK()
        namespace = "com.please.stop.app.kmp"
        androidResources { enable = true }
    }

    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }

    swiftPMDependencies {
        swiftPackage(
            url = "https://github.com/sqlcipher/SQLCipher.swift.git",
            version = "4.14.0",
            products = listOf("SQLCipher"),
        )
        swiftPackage(
            url = "https://github.com/firebase/firebase-ios-sdk.git",
            version = "11.12.0",
            products = listOf("FirebaseCore", "FirebaseRemoteConfig", "FirebaseFunctions", "FirebaseAppCheck"),
        )
    }

    val firebaseHeadersDir = project.layout.buildDirectory
        .dir("kotlin/swiftPMCheckout/checkouts/firebase-ios-sdk/FirebaseRemoteConfig/Sources/Public/FirebaseRemoteConfig")
        .get().asFile.absolutePath

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { target ->
        target.compilations.getByName("main") {
            cinterops.create("FirebaseRemoteConfig") {
                definitionFile.set(project.file("src/nativeInterop/cinterop/FirebaseRemoteConfig.def"))
                compilerOpts("-I$firebaseHeadersDir")
            }
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.koin.core)
            implementation(libs.bundles.koin)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.datastore)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.kotlinx.atomicfu)

            implementation(libs.richetext.editor)
            implementation(libs.vico.compose.m3)
            implementation(libs.composeunstyled.primitives)

            implementation(libs.bundles.coil)
            implementation(libs.bundles.ktor)
            implementation(libs.bundles.filekit)
            implementation(libs.bundles.haze)
            implementation(libs.bundles.jb.nav3)
            implementation(libs.bundles.jb.compose)

            implementation(libs.room.runtime)
            implementation(libs.room.common)
            implementation(libs.androidx.sqlite)
        }

        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }

        androidMain.dependencies {
            implementation(libs.amplitude.android)
            implementation(libs.koin.android)
            implementation(libs.ktor.okhttp)
            implementation(libs.coil.gif)

            implementation(libs.bundles.googleOauth)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.androidx.lifecycle.process)
            implementation(libs.androidx.lifecycle.service)
            implementation(libs.androidx.browser)
            implementation(libs.androidx.exifinterface)
            implementation(libs.androidx.ui.tooling)
            implementation(libs.androidx.profileinstaller)

            implementation(libs.room.ktx)
            implementation(libs.android.sqlcipher)

            implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.16.0"))
            implementation("com.google.firebase:firebase-common-ktx")
            implementation("com.google.firebase:firebase-config-ktx")
            implementation("com.google.firebase:firebase-functions-ktx")
            implementation("com.google.firebase:firebase-appcheck-ktx")
            implementation("com.google.firebase:firebase-appcheck-playintegrity")
            implementation(libs.mlkit.document.scanner)
        }

        iosMain.dependencies {
            implementation(libs.ktor.ios)
            implementation(libs.androidx.sqlite.framework)
        }
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    ksp(libs.room.compiler)
}
