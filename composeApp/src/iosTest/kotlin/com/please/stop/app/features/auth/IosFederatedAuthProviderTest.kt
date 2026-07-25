package com.please.stop.app.features.auth

import com.please.stop.app.features.auth.apple.IosAppleAuthProvider
import com.please.stop.app.features.auth.google.IosGoogleAuthUiProvider
import com.please.stop.app.features.auth.google.IosSocialAuthBridge
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class IosFederatedAuthProviderTest {

    @Test
    fun basicGoogleSignInDoesNotRequestSheetsAuthorization() = runTest {
        val bridge = RecordingSocialAuthBridge()
        val provider = IosGoogleAuthUiProvider(bridge)

        assertEquals("google-id-token", provider.signIn()?.idToken)
        assertEquals(1, bridge.googleSignInCalls)
        assertEquals(0, bridge.sheetsAuthorizationCalls)

        assertEquals("one-time-server-code", provider.authorizeSheets()?.value)
        assertEquals(1, bridge.sheetsAuthorizationCalls)
    }

    @Test
    fun appleCancellationClearsBridgeCredentialState() = runTest {
        val bridge = RecordingSocialAuthBridge(completeAppleImmediately = false)
        val provider = IosAppleAuthProvider(bridge)
        val result = async { provider.signIn() }
        runCurrent()

        assertTrue(bridge.hasAppleCallbacks)
        result.cancelAndJoin()

        assertEquals(1, bridge.appleCancellationCalls)
        assertFalse(bridge.hasAppleCallbacks)
    }
}

private class RecordingSocialAuthBridge(
    private val completeAppleImmediately: Boolean = true,
) : IosSocialAuthBridge {
    var googleSignInCalls = 0
    var sheetsAuthorizationCalls = 0
    var appleCancellationCalls = 0
    var hasAppleCallbacks = false

    override fun signInWithGoogle(
        onSuccess: (idToken: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        googleSignInCalls += 1
        onSuccess("google-id-token")
    }

    override fun authorizeGoogleSheets(
        scopes: List<String>,
        forceConsent: Boolean,
        onSuccess: (serverAuthCode: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        sheetsAuthorizationCalls += 1
        onSuccess("one-time-server-code")
    }

    override fun signInWithApple(
        onSuccess: (identityToken: String, nonce: String, email: String?) -> Unit,
        onError: (String) -> Unit,
    ) {
        hasAppleCallbacks = true
        if (completeAppleImmediately) {
            onSuccess("apple-token", "fresh-nonce", null)
            hasAppleCallbacks = false
        }
    }

    override fun cancelAppleSignIn() {
        appleCancellationCalls += 1
        hasAppleCallbacks = false
    }

    override fun signOut(onComplete: () -> Unit) {
        onComplete()
    }
}
