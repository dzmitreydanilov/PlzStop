package com.please.stop.app.core

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController

internal class IosUrlOpener : UrlOpener {

    override fun open(url: String) {
        val nsUrl = NSURL(string = url)
        presentIosViewController(SFSafariViewController(uRL = nsUrl))
    }
}
