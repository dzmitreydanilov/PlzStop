package com.please.stop.app.features.home.domain.repository

import com.please.stop.app.features.home.domain.model.HomeData
import kotlinx.coroutines.flow.Flow

interface HomeRepository {
    fun observeHomeData(): Flow<HomeData>
    suspend fun addCategory(name: String, iconKey: String): Result<Unit>
}
