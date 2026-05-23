package com.please.stop.app.core

import android.content.Context
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri

internal class AndroidUrlOpener(private val context: Context) : UrlOpener {

    override fun open(url: String) {
        val intent = CustomTabsIntent.Builder().build()
        intent.launchUrl(context, url.toUri())
    }
}
