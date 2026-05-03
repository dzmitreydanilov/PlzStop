package com.please.stop.app.core

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

internal class IosUrlOpener : UrlOpener {

    override fun open(url: String) {
        val nsUrl = NSURL(string = url) ?: return
        UIApplication.sharedApplication.openURL(nsUrl)
    }
}
