package com.please.stop.app.network

import com.please.stop.app.core.logger.logError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.accept
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.contentType
import io.ktor.http.encodedPath
import io.ktor.serialization.JsonConvertException
import io.ktor.util.StringValues
import kotlinx.serialization.SerializationException

abstract class ApiService(protected val httpClient: HttpClient) {

    protected suspend inline fun <reified T> get(
        contentType: ContentType = ContentType.Application.Json,
        headers: StringValues? = null,
        path: String,
        queryParameters: StringValues? = null,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.get(
                httpRequestBuilder(
                    headers = headers,
                    urlBuilder = {
                        encodedPath = path
                        queryParameters?.let {
                            encodedParameters.appendAll(it)
                        }
                    },
                    contentType = contentType,
                )
            )
        }
    }

    protected suspend inline fun <reified T> delete(
        contentType: ContentType = ContentType.Application.Json,
        headers: StringValues? = null,
        path: String,
        queryParameters: StringValues? = null,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.delete(
                httpRequestBuilder(
                    headers = headers,
                    urlBuilder = {
                        encodedPath = path
                        queryParameters?.let {
                            encodedParameters.appendAll(it)
                        }
                    },
                    contentType = contentType
                )
            )
        }
    }

    protected suspend inline fun <reified T> delete(
        body: Any?,
        contentType: ContentType = ContentType.Application.Json,
        path: String,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.delete(
                httpRequestBuilder(
                    body = body,
                    urlBuilder = { encodedPath = path },
                    contentType = contentType
                )
            )
        }
    }

    protected suspend inline fun <reified T> post(
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
        headers: StringValues? = null,
        path: String,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.post(
                httpRequestBuilder(
                    headers = headers,
                    body = body,
                    urlBuilder = { encodedPath = path },
                    contentType = contentType
                )
            )
        }
    }

    protected suspend inline fun <reified T> put(
        body: Any?,
        contentType: ContentType = ContentType.Application.Json,
        path: String,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.put(
                httpRequestBuilder(
                    body = body,
                    urlBuilder = { encodedPath = path },
                    contentType = contentType
                )
            )
        }
    }

    protected suspend inline fun <reified T> patch(
        body: Any?,
        contentType: ContentType = ContentType.Application.Json,
        path: String,
    ): RestResponse<T> {
        return handleResponse {
            httpClient.patch(
                httpRequestBuilder(
                    body = body,
                    urlBuilder = { encodedPath = path },
                    contentType = contentType
                )
            )
        }
    }

    protected fun httpRequestBuilder(
        body: Any? = null,
        contentType: ContentType = ContentType.Application.Json,
        headers: StringValues? = null,
        urlBuilder: URLBuilder.(URLBuilder) -> Unit
    ): HttpRequestBuilder.() -> Unit = {
        url(urlBuilder)
        contentType(contentType)
        accept(contentType)
        body?.let {
            setBody(it)
        }
        headers {
            headers?.let {
                appendAll(headers)
            }
        }
    }
}

@Suppress("detekt:TooGenericExceptionCaught")
suspend inline fun <reified T> handleResponse(
    request: () -> HttpResponse
): RestResponse<T> {
    return try {
        val response = request.invoke()
        val statusCode = response.status.value
        val body = response.body<T>()
        RestResponse.Success(body, statusCode)
    } catch (exception: Exception) {
        val applicationException = mapToApplicationException(exception)
        RestResponse.Error(applicationException)
    }
}

suspend inline fun mapToApplicationException(exception: Throwable): ApplicationException {
    logError(
        exception,
        "Exception: ${exception::class.simpleName}, message ${exception.message.orEmpty()}"
    )

    return if (isNoConnectionError(exception)) {
        ApplicationException.NetworkException(exception.message)
    } else {
        when (exception) {
            // 500..599
            is ServerResponseException -> ApplicationException.ServerException(
                statusCode = exception.response.status.value,
                message = exception.response.bodyAsText(),
            )
            // 400..499
            // When the request made by the client contains a mistake
            // or cannot be processed by the server.
            is ClientRequestException -> {
                val body = exception.response.bodyAsText()
                when (exception.response.status) {
                    HttpStatusCode.Unauthorized -> ApplicationException.AuthenticationException(
                        statusCode = exception.response.status.value,
                        message = body,
                    )

                    else -> ApplicationException.RequestException(
                        statusCode = exception.response.status.value,
                        message = body,
                    )
                }
            }

            // Can be thrown only by BearerAuthConfigProvider in case of refresh token expiration
            is OAuthSessionExpirationException -> {
                ApplicationException.AuthenticationException(
                    statusCode = exception.response.status.value,
                    message = exception.response.bodyAsText(),
                )
            }

            is SerializationException,
            is JsonConvertException -> ApplicationException.UnknownResponseException(
                message = exception.message
            )

            else -> ApplicationException.UnknownException(message = exception.message)
        }
    }
}
