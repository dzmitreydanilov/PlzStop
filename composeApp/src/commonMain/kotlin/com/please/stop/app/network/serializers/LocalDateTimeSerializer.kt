package com.please.stop.app.network.serializers

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 2025-05-08 -> 2025-05-08T18:51:12.960038
 * 2025-05-08T18:51:12.960038 -> 2025-05-08
 */
object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeString(value.toString()) // e.g. "2025-05-08T18:51:12.960038"
    }

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(decoder: Decoder): LocalDateTime {
        val text = decoder.decodeString()
        return try {
            LocalDateTime.parse(text)
        } catch (e: Exception) {
            // Try parsing as LocalDate and convert to LocalDateTime at midnight
            val date = LocalDate.parse(text)
            LocalDateTime(date, LocalTime(0, 0))
        }
    }
}
