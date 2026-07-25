package com.please.stop.app.features.auth.google

import kotlin.jvm.JvmInline

/** Transient Google identity credential used only for Firebase sign-in or reauthentication. */
@JvmInline
value class GoogleSignInCredential(val idToken: String)
