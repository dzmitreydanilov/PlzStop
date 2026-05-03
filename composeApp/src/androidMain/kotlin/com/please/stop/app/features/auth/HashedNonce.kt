package com.please.stop.app.features.auth

import io.ktor.utils.io.core.toByteArray
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class)
fun createHashedNonceString(): String {
    val rawNonce = Uuid.random().toString()
    val bytes = rawNonce.toByteArray()
    val md = MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)

    return digest.fold("") { str, it -> str + "%02x".format(it) }
}
