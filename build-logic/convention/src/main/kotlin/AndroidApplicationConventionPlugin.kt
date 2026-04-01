import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import com.please.stop.app.convention.AppConfigs
import com.please.stop.app.convention.Constants.RELEASE_BUILD_TYPE_ALIAS
import com.please.stop.app.convention.Constants.RELEASE_KEY_PROP_FILE_NAME
import com.please.stop.app.convention.configureKotlinAndroid
import com.please.stop.app.convention.getTargetSDK
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import java.util.Properties

class AndroidApplicationConventionPlugin : Plugin<Project> {

    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            extensions.configure<ApplicationExtension> {
                buildFeatures {
                    buildConfig = true
                    compose = true
                }
                packaging {
                    resources {
                        excludes += "/META-INF/LICENSE.md"
                        excludes += "/META-INF/LICENSE-notice.md"
                    }
                }

                namespace = AppConfigs.namespace

                defaultConfig {
                    applicationId = AppConfigs.applicationId
                    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    versionCode = AppConfigs.versionCode
                    versionName = AppConfigs.versionName
                }

                defaultConfig.targetSdk = getTargetSDK()
                configureKotlinAndroid(this)
                configureBuildTypes(project = project)
            }
        }
    }

    private fun ApplicationExtension.configureBuildTypes(project: Project) {
        buildTypes {

            signingConfigs {
                createSigningConfig(
                    project,
                    RELEASE_BUILD_TYPE_ALIAS,
                    RELEASE_KEY_PROP_FILE_NAME
                )
            }

            getByName(RELEASE_BUILD_TYPE_ALIAS) {
                isMinifyEnabled = true
                isDebuggable = false
                signingConfig = signingConfigs.getByName(RELEASE_BUILD_TYPE_ALIAS)
                matchingFallbacks += listOf(RELEASE_BUILD_TYPE_ALIAS)
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro"
                )
                buildConfigField("boolean", "IS_QA_BUILD", "false")
            }

            create("benchmarkRelease") {
                signingConfig = signingConfigs.getByName(RELEASE_BUILD_TYPE_ALIAS)
                matchingFallbacks += listOf(RELEASE_BUILD_TYPE_ALIAS)
                buildConfigField("boolean", "IS_QA_BUILD", "false")
            }

            sourceSets { configureSourSets() }
        }
    }

    private fun NamedDomainObjectContainer<out AndroidSourceSet>.configureSourSets() {
        getByName(RELEASE_BUILD_TYPE_ALIAS) {
            manifest.srcFile("src/androidMain/AndroidManifest.xml")
            res.directories.add("src/androidMain/res")
        }
    }

    private fun NamedDomainObjectContainer<out ApkSigningConfig>.createSigningConfig(
        project: Project,
        name: String,
        propertiesFileName: String = "keystore.properties"
    ) {
        val keystoreFile = project.rootProject.file(propertiesFileName)

        if (!keystoreFile.exists()) {
            println("⚠️ Warning: $propertiesFileName not found at ${keystoreFile.absolutePath}. $name builds may fail or be unsigned.")
            create(name) {
                println("⚠️ Warning: Signing config for $name not fully configured due to missing $propertiesFileName.")
            }
            return
        }

        val keystoreProperties = loadCachedProperties(project, propertiesFileName, keystoreFile)

        create(name) {
            keyAlias = keystoreProperties["keyAlias"] as? String
            keyPassword = keystoreProperties["keyPassword"] as? String
            storePassword = keystoreProperties["storePassword"] as? String
            storeFile = project.file(keystoreProperties["storeFile"] as? String ?: "")
        }
    }

    private fun loadCachedProperties(
        project: Project,
        propertiesFileName: String,
        file: java.io.File
    ): Properties {
        val cacheKey = "cachedProperties_$propertiesFileName"
        val ext = project.rootProject.extensions.extraProperties

        @Suppress("UNCHECKED_CAST")
        return if (ext.has(cacheKey)) {
            ext.get(cacheKey) as Properties
        } else {
            Properties().also { props ->
                file.inputStream().use { props.load(it) }
                ext.set(cacheKey, props)
            }
        }
    }
}
