package com.please.stop.app.core.logger

import co.touchlab.kermit.Logger
import co.touchlab.kermit.Severity
import co.touchlab.kermit.StaticConfig

private var logger: Logger? = null

class Kermit {

    companion object {
        operator fun invoke(block: Builder.() -> Unit) = with(LoggerBuilder()) {
            block(this)
            build()
        }
    }

    interface Builder {
        fun minSeverity(severity: Severity): Builder
        fun build()
    }

    internal class LoggerBuilder : Builder {
        private var minSeverity: Severity = Severity.Verbose

        override fun minSeverity(severity: Severity): Builder {
            this.minSeverity = severity
            return this
        }

        override fun build() {
            logger = Logger(StaticConfig(minSeverity))
        }
    }
}

fun logDebug(message: String) {
    logger?.d { message }
}

fun logDebugWithTag(message: String, tag: String) {
    logger?.d(message = { message }, tag = tag)
}

fun logInformation(message: String) {
    logger?.i { message }
}

fun logInformationWithTag(message: String, tag: String) {
    logger?.i(message = { message }, tag = tag)
}

fun logWarning(message: String) {
    logger?.w { message }
}

fun logWarningWithTag(message: String, tag: String = "") {
    logger?.w(message = { message }, tag = tag)
}

fun logError(throwable: Throwable? = null, message: String) {
    logger?.e(throwable = throwable, message = { message })
}

fun logErrorWithTag(throwable: Throwable? = null, message: String, tag: String = "") {
    logger?.e(throwable = throwable, message = { message }, tag = tag)
}

