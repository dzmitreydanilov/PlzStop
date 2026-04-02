package com.please.stop.app.navigation.deeplink

import com.please.stop.app.core.logger.logDebugWithTag
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Handles deep link events for runtime navigation (when the app is already running).
 *
 * Incoming URI strings are resolved via [DeepLinkResolver] and emitted as
 * [DeepLinkResult] events for the navigation layer to consume.
 *
 * Used for:
 * - `onNewIntent` deep links on Android (app already in foreground)
 * - Push notification taps while the app is running
 * - iOS Universal Links received while the app is active
 */
class DeepLinkHandler(
    private val resolver: DeepLinkResolver
) {
    private val _deepLinkEvents = MutableSharedFlow<DeepLinkResult>(replay = 1)
    val deepLinkEvents: SharedFlow<DeepLinkResult> = _deepLinkEvents.asSharedFlow()

    /**
     * Parses and resolves a deep link URI, emitting a navigation event if matched.
     *
     * @param uriString The raw deep link URI string
     */
    fun handleDeepLink(uriString: String) {
        val data = parseDeepLinkUri(uriString)
        logDebugWithTag(tag = TAG, message = "handleDeepLink: uri=$uriString, parsed=$data")
        if (data == null) return
        val result = resolver.resolve(data)
        logDebugWithTag(tag = TAG, message = "handleDeepLink: resolved=$result, emitting=${result != null}")
        if (result == null) return
        _deepLinkEvents.tryEmit(result)
    }

    fun consumeEvent() {
        _deepLinkEvents.resetReplayCache()
    }

    private companion object {
        const val TAG = "DeepLinkHandler"
    }
}
