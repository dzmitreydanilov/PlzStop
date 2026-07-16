package com.please.stop.app.features.auth.data

import com.please.stop.app.core.models.data.GoogleAccountLink
import kotlin.test.Test
import kotlin.test.assertEquals

class GoogleAccountLinkCodecTest {

    @Test
    fun encodeAndDecodePreservesAccountLink() {
        val link = GoogleAccountLink(
            email = "person@example.com",
            isConnected = true,
        )

        assertEquals(link, GoogleAccountLinkCodec.decode(GoogleAccountLinkCodec.encode(link)))
    }

    @Test
    fun decodeMigratesLegacyPayload() {
        val expected = GoogleAccountLink(
            email = "person@example.com",
            isConnected = true,
        )

        assertEquals(expected, GoogleAccountLinkCodec.decode("person@example.com|true"))
    }
}
