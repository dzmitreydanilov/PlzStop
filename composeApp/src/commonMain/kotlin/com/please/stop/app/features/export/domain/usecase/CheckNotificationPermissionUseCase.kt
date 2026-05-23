package com.please.stop.app.features.export.domain.usecase

import com.please.stop.app.core.INotificationPermission
import com.please.stop.app.core.flow.flowFromSuspend
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class CheckNotificationPermissionUseCase(
    private val notificationPermission: INotificationPermission,
) {
    operator fun invoke(): Flow<HasNotificationPermissionResult> {
        return flowFromSuspend {
            notificationPermission.isGranted()
        }.map { hasPermission ->
            HasNotificationPermissionResult.HasPermission.takeIf { hasPermission }
                ?: HasNotificationPermissionResult.NoPermission
        }
    }
}

sealed interface HasNotificationPermissionResult : com.please.stop.app.core.models.domain.Result {
    data object HasPermission : HasNotificationPermissionResult
    data object NoPermission : HasNotificationPermissionResult
}
