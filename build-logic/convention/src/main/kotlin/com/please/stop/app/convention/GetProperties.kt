package com.please.stop.app.convention

import java.io.File
import org.gradle.api.Project

fun Project.getProjectProperty(key: String, fileName: String): Any {
    val cacheKey = "cachedProperties_$fileName"
    val ext = rootProject.extensions.extraProperties

    @Suppress("UNCHECKED_CAST")
    val properties = if (ext.has(cacheKey)) {
        ext.get(cacheKey) as java.util.Properties
    } else {
        val localPropertiesFile = File(rootProject.rootDir, fileName)
        if (!localPropertiesFile.isFile) {
            error("File $fileName not found in the ${rootProject.rootDir} dir")
        }
        java.util.Properties().also { props ->
            localPropertiesFile.inputStream().use { props.load(it) }
            ext.set(cacheKey, props)
        }
    }

    return properties.getProperty(key)
}
