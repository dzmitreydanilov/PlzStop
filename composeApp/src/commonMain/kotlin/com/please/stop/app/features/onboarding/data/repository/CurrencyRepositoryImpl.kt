package com.please.stop.app.features.onboarding.data.repository

import com.please.stop.app.core.logger.logDebugWithTag
import com.please.stop.app.core.logger.logErrorWithTag
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import plzstop.composeapp.generated.resources.Res

/**
 * Hybrid currency source: tries Remote Config first, falls back to bundled JSON.
 * Result is cached in-memory for the session.
 */
class CurrencyRepositoryImpl(
    private val remoteConfigDataSource: RemoteConfigDataSource,
    private val ioDispatcher: CoroutineDispatcher,
) : CurrencyRepository {

    private val mutex = Mutex()
    private var cache: List<Currency>? = null

    override suspend fun getAllCurrencies(): Result<List<Currency>> = withContext(ioDispatcher) {
        runCatching {
            mutex.withLock {
                cache ?: loadCurrencies().also { cache = it }
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadCurrencies(): List<Currency> {
        val remoteJson = remoteConfigDataSource.fetchString(RemoteConfigDataSource.KEY_CURRENCIES)
        if (remoteJson != null) {
            try {
                val dtos = json.decodeFromString<List<CurrencyDto>>(remoteJson)
                if (dtos.isNotEmpty()) {
                    logDebugWithTag(tag = TAG, message = "Loaded ${dtos.size} currencies from Remote Config")
                    return dtos.map { it.toDomain() }
                }
            }  catch (e: Exception) {
                logErrorWithTag(tag = TAG, message = "Failed to parse Remote Config currencies", throwable = e)
            }
        }
        logDebugWithTag(tag = TAG, message = "Using bundled currencies")
        return loadFromBundle()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadFromBundle(): List<Currency> {
        val bytes = Res.readBytes("files/currencies.json")
        val dtos = json.decodeFromString<List<CurrencyDto>>(bytes.decodeToString())
        return dtos.map { it.toDomain() }
    }

    private companion object {
        const val TAG = "CurrencyRepository"
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class CurrencyDto(
    val code: String,
    val symbol: String,
    val name: String,
    val decimalPlaces: Int,
)

private fun CurrencyDto.toDomain() = Currency(
    code = code,
    symbol = symbol,
    name = name,
    decimalPlaces = decimalPlaces,
)
