package com.please.stop.app.features.onboarding.data.repository

import com.please.stop.app.core.db.dao.SubcategoryDao
import com.please.stop.app.core.db.entity.SubcategoryEntity
import com.please.stop.app.core.logger.logDebugWithTag
import com.please.stop.app.core.logger.logErrorWithTag
import com.please.stop.app.features.onboarding.domain.model.Subcategory
import com.please.stop.app.features.onboarding.domain.repository.SubcategoryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import plzstop.composeapp.generated.resources.Res
import kotlin.concurrent.Volatile

class SubcategoryRepositoryImpl(
    private val subcategoryDao: SubcategoryDao,
    private val remoteConfigDataSource: RemoteConfigDataSource,
    private val ioDispatcher: CoroutineDispatcher,
) : SubcategoryRepository {

    private val mutex = Mutex()
    private var cache: List<Subcategory>? = null

    @Volatile
    private var seeded = false

    override fun observeByParent(parentCategoryId: Long): Flow<List<Subcategory>> =
        subcategoryDao.observeByParent(parentCategoryId)
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override fun observeAll(): Flow<List<Subcategory>> =
        subcategoryDao.observeAll()
            .map { entities -> entities.map { it.toDomain() } }
            .flowOn(ioDispatcher)

    override suspend fun getDefaultSubcategories(): Result<List<Subcategory>> =
        withContext(ioDispatcher) {
            runCatching {
                mutex.withLock {
                    cache ?: loadSubcategories().also { cache = it }
                }
            }
        }

    override suspend fun ensureSeeded(): Unit = withContext(ioDispatcher) {
        if (seeded) return@withContext

        mutex.withLock {
            if (seeded) return@withLock

            val count = subcategoryDao.count()
            if (count > 0) {
                seeded = true
                return@withLock
            }

            val defaults = getDefaultSubcategories().getOrNull() ?: return@withLock
            subcategoryDao.insertAll(defaults.map { it.toEntity() })
            seeded = true
            logDebugWithTag(tag = TAG, message = "Seeded ${defaults.size} default subcategories")
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun loadSubcategories(): List<Subcategory> {
        val remoteJson =
            remoteConfigDataSource.fetchString(RemoteConfigDataSource.KEY_DEFAULT_SUBCATEGORIES)
        if (remoteJson != null) {
            try {
                val dtos = json.decodeFromString<List<SubcategoryDto>>(remoteJson)
                if (dtos.isNotEmpty()) {
                    logDebugWithTag(
                        tag = TAG,
                        message = "Loaded ${dtos.size} subcategories from Remote Config",
                    )
                    return dtos.map { it.toDomain() }
                }
            } catch (e: Exception) {
                logErrorWithTag(
                    tag = TAG,
                    message = "Failed to parse Remote Config subcategories",
                    throwable = e,
                )
            }
        }
        logDebugWithTag(tag = TAG, message = "Using bundled subcategories")
        return loadFromBundle()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadFromBundle(): List<Subcategory> {
        val bytes = Res.readBytes("files/subcategories.json")
        val dtos = json.decodeFromString<List<SubcategoryDto>>(bytes.decodeToString())
        return dtos.map { it.toDomain() }
    }

    private companion object {
        const val TAG = "SubcategoryRepository"
        val json = Json { ignoreUnknownKeys = true }
    }
}

@Serializable
private data class SubcategoryDto(
    val id: Long,
    val parentCategoryId: Long,
    val name: String,
    val iconKey: String,
    val isDefault: Boolean,
    val sortOrder: Int,
)

private fun SubcategoryDto.toDomain() = Subcategory(
    id = id,
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

private fun SubcategoryEntity.toDomain() = Subcategory(
    id = id,
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    sortOrder = sortOrder,
)

private fun Subcategory.toEntity() = SubcategoryEntity(
    parentCategoryId = parentCategoryId,
    name = name,
    iconKey = iconKey,
    isDefault = isDefault,
    sortOrder = sortOrder,
)
