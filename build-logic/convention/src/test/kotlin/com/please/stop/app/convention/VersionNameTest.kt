

package com.please.stop.app.convention

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VersionNameTest {

    @Test
    fun `test build version name default`() {
        assertEquals("0.0.1-alpha0", buildVersionName(0, 0, 1, VersionType.ALPHA, 0))
        assertEquals("0.1.0-alpha0", buildVersionName(0, 1, 0, VersionType.ALPHA, 0))
        assertEquals("1.0.0-alpha0", buildVersionName(1, 0, 0, VersionType.ALPHA, 0))

        assertEquals("0.0.1-beta0", buildVersionName(0, 0, 1, VersionType.BETA, 0))
        assertEquals("0.1.0-beta0", buildVersionName(0, 1, 0, VersionType.BETA, 0))
        assertEquals("1.0.0-beta0", buildVersionName(1, 0, 0, VersionType.BETA, 0))

        assertEquals("0.0.1-rc0", buildVersionName(0, 0, 1, VersionType.DEV, 0))
        assertEquals("0.1.0-rc0", buildVersionName(0, 1, 0, VersionType.DEV, 0))
        assertEquals("1.0.0-rc0", buildVersionName(1, 0, 0, VersionType.DEV, 0))

        assertEquals("0.0.1", buildVersionName(0, 0, 1, VersionType.PROD, 0))
        assertEquals("0.1.0", buildVersionName(0, 1, 0, VersionType.PROD, 0))
        assertEquals("1.0.0", buildVersionName(1, 0, 0, VersionType.PROD, 0))
    }

    @Test
    fun `test build version code alpha version`() {
        assertEquals("1.3.3-alpha0", buildVersionName(1, 3, 3, VersionType.ALPHA, 0))
        assertEquals("1.3.3-alpha1", buildVersionName(1, 3, 3, VersionType.ALPHA, 1))
        assertEquals("1.3.3-alpha20", buildVersionName(1, 3, 3, VersionType.ALPHA, 20))
        assertEquals("2.0.0-alpha0", buildVersionName(2, 0, 0, VersionType.ALPHA, 0))
        assertEquals("3.0.0-alpha0", buildVersionName(3, 0, 0, VersionType.ALPHA, 0))
        assertEquals("99.99.99-alpha0", buildVersionName(99, 99, 99, VersionType.ALPHA, 0))
        assertEquals("99.99.99-alpha99", buildVersionName(99, 99, 99, VersionType.ALPHA, 99))
    }

    @Test
    fun `test build version code beta version`() {
        assertEquals("1.3.3-beta0", buildVersionName(1, 3, 3, VersionType.BETA, 0))
        assertEquals("1.3.3-beta1", buildVersionName(1, 3, 3, VersionType.BETA, 1))
        assertEquals("1.3.3-beta20", buildVersionName(1, 3, 3, VersionType.BETA, 20))
        assertEquals("2.0.0-beta0", buildVersionName(2, 0, 0, VersionType.BETA, 0))
        assertEquals("3.0.0-beta0", buildVersionName(3, 0, 0, VersionType.BETA, 0))
        assertEquals("99.99.99-beta0", buildVersionName(99, 99, 99, VersionType.BETA, 0))
        assertEquals("99.99.99-beta99", buildVersionName(99, 99, 99, VersionType.BETA, 99))
    }

    @Test
    fun `test build version code dev version`() {
        assertEquals("1.3.3-rc0", buildVersionName(1, 3, 3, VersionType.DEV, 0))
        assertEquals("1.3.3-rc1", buildVersionName(1, 3, 3, VersionType.DEV, 1))
        assertEquals("1.3.3-rc20", buildVersionName(1, 3, 3, VersionType.DEV, 20))
        assertEquals("2.0.0-rc0", buildVersionName(2, 0, 0, VersionType.DEV, 0))
        assertEquals("3.0.0-rc0", buildVersionName(3, 0, 0, VersionType.DEV, 0))
        assertEquals("99.99.99-rc0", buildVersionName(99, 99, 99, VersionType.DEV, 0))
        assertEquals("99.99.99-rc99", buildVersionName(99, 99, 99, VersionType.DEV, 99))
    }

    @Test
    fun `test build version code prod version`() {
        assertEquals("1.3.3", buildVersionName(1, 3, 3, VersionType.PROD, 0))
        assertEquals("2.0.0", buildVersionName(2, 0, 0, VersionType.PROD, 0))
        assertEquals("3.0.0", buildVersionName(3, 0, 0, VersionType.PROD, 0))
        assertEquals("4.2.55", buildVersionName(4, 2, 55, VersionType.PROD, 0))
        assertEquals("10.0.0", buildVersionName(10, 0, 0, VersionType.PROD, 0))
        assertEquals("10.10.10", buildVersionName(10, 10, 10, VersionType.PROD, 0))
        assertEquals("99.99.99", buildVersionName(99, 99, 99, VersionType.PROD, 0))
    }
}