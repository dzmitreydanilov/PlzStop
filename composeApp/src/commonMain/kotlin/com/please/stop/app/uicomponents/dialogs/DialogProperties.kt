/*
 * DialogProperties.kt
 * Audit
 *
 * Created by dzmitreydanilau on 13/1/2025.
 * Copyright © 2025 Enablon. All rights reserved.
 */

package com.please.stop.app.uicomponents.dialogs

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

internal val DialogMinWidth = 280.dp
internal val DialogMaxWidth = 560.dp


expect fun getDialogProperties(
    dismissOnBackPress: Boolean = true,
    decorFitsSystemWindows: Boolean = true,
    useDefaultPlatformWidth: Boolean = false
): DialogProperties
