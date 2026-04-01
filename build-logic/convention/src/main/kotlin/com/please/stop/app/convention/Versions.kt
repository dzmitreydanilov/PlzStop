package com.please.stop.app.convention

internal const val versionMajor = 1
internal const val versionMinor = 0
internal const val versionPatch = 0
internal val versionType: VersionType = VersionType.ALPHA
internal const val typeVersion = 62

enum class VersionType(val type: Int) {
    ALPHA(1), BETA(2), DEV(3), PROD(9)
}

/**
 * Version code is build in the following pattern: MM.mm.PP.TT.TV
 * MM - Major, mm - Minor, PP - Patch, TT - Release Type, TV - Type Version
 *
 * Example: Version code: 10000900 -> 1.00.00.9.00 -> 1.0.0 - Prod version
 * Example: Version code: 10000200 -> 1.00.00.2.00 -> 1.0.0-beta0 - Beta version
 *
 * Example: Version code: 10303233 -> 1.03.03.2.33 -> 1.3.3-beta33 - Beta version
 * Example: Version code: 10303377 -> 1.03.03.3.77 -> 1.3.3-rc77 - Dev version
 * Example: Version code: 10303900 -> 1.03.03.9.00 -> 1.3.3 - Prod version
 *
 * Example: Version code: 20000900 -> 2.00.00.9.00 -> 2.0.0 - Prod version
 * Example: Version code: 20000900 -> 2.00.00.2.01 -> 2.0.0-beta1 - Beta version
 *
 * Example: Version code: 999999900 -> 99.99.99.9.00 -> 99.99.99 - Prod version
 */
internal fun buildVersionCode(
    major: Int,
    minor: Int,
    patch: Int,
    versionType: VersionType,
    typeVersion: Int
): Int {
    return (major * 10000000) + (minor * 100000) + (patch * 1000) + (versionType.type * 100) + typeVersion
}

internal fun buildVersionName(
    major: Int,
    minor: Int,
    patch: Int,
    versionType: VersionType,
    typeVersion: Int
): String {
    val versionBase = "$major.$minor.$patch"
    return when (versionType) {
        VersionType.ALPHA -> "$versionBase-alpha$typeVersion"
        VersionType.BETA -> "$versionBase-beta$typeVersion"
        VersionType.DEV -> "$versionBase-rc$typeVersion"
        VersionType.PROD -> versionBase
    }
}
