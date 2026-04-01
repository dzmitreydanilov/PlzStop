package com.please.stop.app.convention

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VersionCodeTest {

    @Test
    fun `test build version code default`() {
        assertEquals(1100, buildVersionCode(0, 0, 1, VersionType.ALPHA, 0))
        assertEquals(100100, buildVersionCode(0, 1, 0, VersionType.ALPHA, 0))
        assertEquals(10000100, buildVersionCode(1, 0, 0, VersionType.ALPHA, 0))

        assertEquals(1200, buildVersionCode(0, 0, 1, VersionType.BETA, 0))
        assertEquals(100200, buildVersionCode(0, 1, 0, VersionType.BETA, 0))
        assertEquals(10000200, buildVersionCode(1, 0, 0, VersionType.BETA, 0))

        assertEquals(1300, buildVersionCode(0, 0, 1, VersionType.DEV, 0))
        assertEquals(100300, buildVersionCode(0, 1, 0, VersionType.DEV, 0))
        assertEquals(10000300, buildVersionCode(1, 0, 0, VersionType.DEV, 0))

        assertEquals(1900, buildVersionCode(0, 0, 1, VersionType.PROD, 0))
        assertEquals(100900, buildVersionCode(0, 1, 0, VersionType.PROD, 0))
        assertEquals(10000900, buildVersionCode(1, 0, 0, VersionType.PROD, 0))
    }

    @Test
    fun `test build version code alpha version`() {
        assertEquals(10303100, buildVersionCode(1, 3, 3, VersionType.ALPHA, 0))
        assertEquals(10303101, buildVersionCode(1, 3, 3, VersionType.ALPHA, 1))
        assertEquals(10303120, buildVersionCode(1, 3, 3, VersionType.ALPHA, 20))
        assertEquals(20000100, buildVersionCode(2, 0, 0, VersionType.ALPHA, 0))
        assertEquals(30000100, buildVersionCode(3, 0, 0, VersionType.ALPHA, 0))
        assertEquals(999999100, buildVersionCode(99, 99, 99, VersionType.ALPHA, 0))
        assertEquals(999999199, buildVersionCode(99, 99, 99, VersionType.ALPHA, 99))
    }

    @Test
    fun `test build version code beta version`() {
        assertEquals(10303200, buildVersionCode(1, 3, 3, VersionType.BETA, 0))
        assertEquals(10303201, buildVersionCode(1, 3, 3, VersionType.BETA, 1))
        assertEquals(10303220, buildVersionCode(1, 3, 3, VersionType.BETA, 20))
        assertEquals(20000200, buildVersionCode(2, 0, 0, VersionType.BETA, 0))
        assertEquals(30000200, buildVersionCode(3, 0, 0, VersionType.BETA, 0))
        assertEquals(999999200, buildVersionCode(99, 99, 99, VersionType.BETA, 0))
        assertEquals(999999299, buildVersionCode(99, 99, 99, VersionType.BETA, 99))
    }

    @Test
    fun `test build version code dev version`() {
        assertEquals(10303300, buildVersionCode(1, 3, 3, VersionType.DEV, 0))
        assertEquals(10303301, buildVersionCode(1, 3, 3, VersionType.DEV, 1))
        assertEquals(10303320, buildVersionCode(1, 3, 3, VersionType.DEV, 20))
        assertEquals(20000300, buildVersionCode(2, 0, 0, VersionType.DEV, 0))
        assertEquals(30000300, buildVersionCode(3, 0, 0, VersionType.DEV, 0))
        assertEquals(999999300, buildVersionCode(99, 99, 99, VersionType.DEV, 0))
        assertEquals(999999399, buildVersionCode(99, 99, 99, VersionType.DEV, 99))
    }

    @Test
    fun `test build version code prod version`() {
        assertEquals(10303900, buildVersionCode(1, 3, 3, VersionType.PROD, 0))
        assertEquals(20000900, buildVersionCode(2, 0, 0, VersionType.PROD, 0))
        assertEquals(30000900, buildVersionCode(3, 0, 0, VersionType.PROD, 0))
        assertEquals(40255900, buildVersionCode(4, 2, 55, VersionType.PROD, 0))
        assertEquals(100000900, buildVersionCode(10, 0, 0, VersionType.PROD, 0))
        assertEquals(101010900, buildVersionCode(10, 10, 10, VersionType.PROD, 0))
        assertEquals(999999900, buildVersionCode(99, 99, 99, VersionType.PROD, 0))
    }
}