package com.please.stop.app.network

import com.please.stop.app.core.ITokensStorage
import io.ktor.client.HttpClient

private fun createHttpClient(
    config: HttpClientDataConfig
): HttpClient {
    return HttpClient(config.engine) {
        expectSuccess = true
        logging()
        contentEncoding()
        configureRedirection()
        configureContent(json = config.json)
        configureTimeOut(configuration = config)
    }
}

fun createAuthFreeHttpClient(
    config: HttpClientDataConfig,
): HttpClient {
    return createHttpClient(config).config {
        configureDefaultRequest()
    }
}

fun createHttpClientWithBearerToken(
    config: HttpClientDataConfig,
    tokenStorage: ITokensStorage,
): HttpClient {
    return createHttpClient(config).config {
        bearerAuth(tokenStorage)
        configureDefaultRequest()
    }
}
