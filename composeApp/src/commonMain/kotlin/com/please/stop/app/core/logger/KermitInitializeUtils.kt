package com.please.stop.app.core.logger

import co.touchlab.kermit.Severity

@Suppress("LongParameterList")
fun initLogger(
    minSeverity: Severity = Severity.Verbose
) {
    Kermit.Companion {
        minSeverity(minSeverity)
    }
}
