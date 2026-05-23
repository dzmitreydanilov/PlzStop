package com.please.stop.app.features.auth.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.please.stop.app.core.INotificationPermission

internal class AndroidNotificationPermission(
    private val context: Context
) : INotificationPermission {

    override suspend fun isGranted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    override suspend fun request(): Boolean {
        // Permission request must be triggered from Activity via Compose permission APIs.
        // This returns the current state; actual request is handled by the UI layer.
        return isGranted()
    }
}
