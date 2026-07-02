package com.please.stop.app.core

import com.please.stop.app.core.models.domain.Result
import kotlinx.coroutines.flow.Flow

internal fun interface ResultSourcePlugin {
    fun results(): Flow<Result>
}
