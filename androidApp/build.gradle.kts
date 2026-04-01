import java.util.Properties

plugins {
    id("android.application")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.gms)
}

android {
    defaultConfig {
        val localPropertiesFile = rootProject.file("local.properties")
        val localProperties = Properties()
        if (localPropertiesFile.exists()) {
            localPropertiesFile.reader().use { localProperties.load(it) }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(projects.composeApp)
    implementation(libs.bundles.filekit)
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.nav3.ui)

    implementation(project.dependencies.platform("com.google.firebase:firebase-bom:33.16.0"))
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-config")

    implementation(libs.purchases.core)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.robolectric)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
