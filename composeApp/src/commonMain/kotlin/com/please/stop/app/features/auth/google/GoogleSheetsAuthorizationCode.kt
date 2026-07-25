package com.please.stop.app.features.auth.google

import kotlin.jvm.JvmInline

/** One-time Google server authorization code used to connect Sheets export. */
@JvmInline
value class GoogleSheetsAuthorizationCode(val value: String)
