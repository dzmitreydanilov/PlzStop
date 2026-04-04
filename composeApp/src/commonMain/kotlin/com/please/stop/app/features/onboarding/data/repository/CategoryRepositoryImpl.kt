package com.please.stop.app.features.onboarding.data.repository

import com.please.stop.app.core.logger.logDebugWithTag
import com.please.stop.app.core.logger.logErrorWithTag
import com.please.stop.app.features.onboarding.domain.repository.CategoryRepository
import com.please.stop.app.features.onboarding.presentation.CategoryUiModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import plzstop.composeapp.generated.resources.Res

/**
 * Hybrid category source: tries Remote Config first, falls back to bundled JSON.
 * Result is cached in-memory for the session.
 */
class CategoryRepositoryImpl(
    private val remoteConfigDataSource: RemoteConfigDataSource,
    private val ioDispatcher: CoroutineDispatcher,
) : CategoryRepository {

    private val mutex = Mutex()
    private var cache: List<CategoryUiModel>? = null

    override suspend fun getDefaultCategories(): Result<List<CategoryUiModel>> =
        withContext(ioDispatcher) {
            runCatching {
                mutex.withLock {
                    cache ?: loadCategories().also { cache = it }
                }
            }
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadCategories(): List<CategoryUiModel> {
        val remoteJson =
            remoteConfigDataSource.fetchString(RemoteConfigDataSource.KEY_DEFAULT_CATEGORIES)
        if (remoteJson != null) {
            try {
                val dtos = json.decodeFromString<List<CategoryDto>>(remoteJson)
                if (dtos.isNotEmpty()) {
                    logDebugWithTag(
                        tag = TAG,
                        message = "Loaded ${dtos.size} categories from Remote Config"
                    )
                    return dtos.map { it.toUiModel() }
                }
            } catch (e: Exception) {
                logErrorWithTag(
                    tag = TAG,
                    message = "Failed to parse Remote Config categories",
                    throwable = e
                )
            }
        }
        logDebugWithTag(
            tag = TAG,
            message = "Using bundled categories"
        )
        return loadFromBundle()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadFromBundle(): List<CategoryUiModel> {
        val bytes = Res.readBytes("files/categories.json")
        val dtos = json.decodeFromString<List<CategoryDto>>(bytes.decodeToString())
        return dtos.map { it.toUiModel() }
    }

    private companion object {
        const val TAG = "CategoryRepository"
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class CategoryDto(
    val id: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)

private fun CategoryDto.toUiModel() = CategoryUiModel(
    id = id,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
)
