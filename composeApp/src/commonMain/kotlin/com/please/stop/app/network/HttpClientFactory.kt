package com.please.stop.app.network

import com.please.stop.app.core.ITokensStorage
import com.please.stop.app.core.buildSettings.DeviceIdReader
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
    deviceIdReader: DeviceIdReader
): HttpClient {
    return createHttpClient(config).config {
        configureDefaultRequest(deviceIdReader = deviceIdReader)
    }
}

fun createChatHttpClientWithBearerToken(
    config: HttpClientDataConfig,
    tokenStorage: ITokensStorage,
    deviceIdReader: DeviceIdReader
): HttpClient {
    return createHttpClient(config).config {
        bearerAuth(tokenStorage)
        configureDefaultRequest(
            deviceIdReader = deviceIdReader,
            port = config.port
        )
    }
}


fun createHttpClientWithBearerToken(
    config: HttpClientDataConfig,
    tokenStorage: ITokensStorage,
    deviceIdReader: DeviceIdReader
): HttpClient {
    return createHttpClient(config).config {
        bearerAuth(tokenStorage)
        configureDefaultRequest(deviceIdReader = deviceIdReader)
    }
}
