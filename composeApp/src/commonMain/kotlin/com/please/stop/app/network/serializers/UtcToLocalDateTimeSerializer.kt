package com.please.stop.app.network.serializers

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.offsetAt
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
object UtcToLocalDateTimeSerializer : KSerializer<LocalDateTime> {
    private val zone = TimeZone.currentSystemDefault()

    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UtcToLocalDateTime", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val isoString = decoder.decodeString()
        val instant = Instant.parse(isoString)
        return instant.toLocalDateTime(zone)
    }

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        val instant = value.toInstant(zone)
        val offset = zone.offsetAt(instant)
        encoder.encodeString("$value$offset")
    }
}
