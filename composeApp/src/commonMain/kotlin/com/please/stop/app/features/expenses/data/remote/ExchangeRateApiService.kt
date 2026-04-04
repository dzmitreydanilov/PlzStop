package com.please.stop.app.features.expenses.data.remote

import com.please.stop.app.features.expenses.data.remote.model.ExchangeRateResponse
import com.please.stop.app.network.ApiService
import com.please.stop.app.network.RestResponse
import io.ktor.client.HttpClient
import io.ktor.http.parametersOf

class ExchangeRateApiService(httpClient: HttpClient) : ApiService(httpClient) {

    suspend fun getRate(
        from: String,
        to: String,
        date: String?,
    ): RestResponse<ExchangeRateResponse> = get(
        path = "/${date ?: "latest"}",
        queryParameters = parametersOf(
            "from" to listOf(from),
            "to" to listOf(to),
        ),
    )
}
