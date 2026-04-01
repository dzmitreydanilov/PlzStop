package com.please.stop.app.network

sealed class ApplicationException(override val message: String?, open val statusCode: Int?) :
    Throwable(message) {

    class AuthenticationException(
        override val message: String?,
        override val statusCode: Int?,
    ) : ApplicationException(message, statusCode)

    class ServerException(
        override val message: String?,
        override val statusCode: Int?
    ) : ApplicationException(message, statusCode)

    class RequestException(
        override val message: String?,
        override val statusCode: Int?
    ) : ApplicationException(message, statusCode)

    class UnknownResponseException(
        override val message: String?
    ) : ApplicationException(message, null)

    class UnknownException(
        override val message: String?
    ) : ApplicationException(message, null)

    class NetworkException(
        override val message: String?
    ) : ApplicationException(message, null)
}

