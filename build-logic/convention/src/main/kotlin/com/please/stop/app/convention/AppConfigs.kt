package com.please.stop.app.convention

object AppConfigs {

    const val namespace = "com.please.stop.app"
    internal const val applicationId = "com.please.stop.app"

    internal const val binaryBaseName = "PlzStop"

    internal val versionCode =
        buildVersionCode(versionMajor, versionMinor, versionPatch, versionType, typeVersion)
    internal val versionName =
        buildVersionName(versionMajor, versionMinor, versionPatch, versionType, typeVersion)
}
