import com.please.stop.app.convention.getCompileSDK
import com.please.stop.app.convention.getMinSDK
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    id("kmp.library")
    id("kotlin.detekt")
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidKmpLibrary)
}
kotlin {

    android {
        compileSdk = getCompileSDK()
        minSdk = getMinSDK()
        namespace = "com.please.stop.app"
        androidResources { enable = true }
    }

    compilerOptions {
        freeCompilerArgs.add("-XXLanguage:+ExplicitBackingFields")
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kermit)
            implementation(libs.koin.core)
            implementation(libs.bundles.koin)
            implementation(libs.kotlinx.datetime)

            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.collections.immutable)

            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            implementation(libs.kotlinx.atomicfu)

            implementation(libs.calendar.compose.multiplatform)
            implementation(libs.richetext.editor)
            implementation(libs.composeunstyled.primitives)

            implementation(libs.bundles.coil)
            implementation(libs.bundles.ktor)
            implementation(libs.bundles.filekit)
            implementation(libs.bundles.haze)
            implementation(libs.bundles.jb.nav3)
            implementation(libs.bundles.jb.compose)
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
        }

        iosMain.dependencies {
            implementation(libs.ktor.ios)
        }

        wasmJsMain.dependencies {
            implementation(libs.ktor.js)
        }
    }
}


