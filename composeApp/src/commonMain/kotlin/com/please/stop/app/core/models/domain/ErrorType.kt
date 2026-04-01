package com.please.stop.app.core.models.domain

import com.please.stop.app.network.ApplicationException
import kotlinx.serialization.Serializable

@Serializable
sealed interface ErrorType {

    val message: String?

    @Serializable
    data class Authentication(override val message: String?) : ErrorType

    @Serializable
    data class Server(override val message: String?, val statusCode: Int? = null) : ErrorType

    @Serializable
    data class Request(override val message: String?, val statusCode: Int? = null) : ErrorType

    @Serializable
    data class Network(override val message: String?) : ErrorType

    @Serializable
    data class Unknown(override val message: String?) : ErrorType
}

fun Throwable.toErrorType(message: String? = null): ErrorType {
    return when (this) {
        is ApplicationException.AuthenticationException -> {
            ErrorType.Authentication(this.message)
        }

        is ApplicationException.NetworkException -> {
            ErrorType.Network(this.message)
        }

        is ApplicationException.RequestException -> {
            ErrorType.Request(message = this.message, statusCode = this.statusCode)
        }

        is ApplicationException.ServerException -> {
            ErrorType.Server(message = this.message, statusCode = this.statusCode)
        }

        is ApplicationException.UnknownException -> {
            ErrorType.Unknown(this.message)
        }

        else -> ErrorType.Unknown(message)
    }
}
