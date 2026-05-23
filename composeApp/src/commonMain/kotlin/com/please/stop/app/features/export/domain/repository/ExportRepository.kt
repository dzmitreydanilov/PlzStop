package com.please.stop.app.features.export.domain.repository

import kotlinx.coroutines.flow.Flow

interface ExportRepository {
    fun enqueExport(
        startDateMillis: Long,
        endDateMillis: Long,
    ): Flow<Result<Unit>>
}
