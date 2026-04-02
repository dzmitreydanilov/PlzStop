package com.please.stop.app.uicomponents.dialogs

import androidx.compose.ui.window.DialogProperties

actual fun getDialogProperties(
    dismissOnBackPress: Boolean,
    decorFitsSystemWindows: Boolean,
    useDefaultPlatformWidth: Boolean
): DialogProperties {
    return DialogProperties(
        dismissOnBackPress = dismissOnBackPress,
        dismissOnClickOutside = false,
        usePlatformDefaultWidth = useDefaultPlatformWidth
    )
}
