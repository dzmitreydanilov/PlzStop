package com.please.stop.app.navigation.nav3

import androidx.compose.runtime.Composable
import androidx.compose.ui.uikit.LocalUIViewController
import platform.UIKit.navigationController
import platform.posix.exit

@Composable
actual fun platformOnBack(): () -> Unit {
    val viewController = LocalUIViewController.current

    return {
        val nav = viewController.navigationController
        val canPop = nav?.viewControllers?.size?.let { it > 1 } == true

        when {
            canPop -> {
                nav.popViewControllerAnimated(true)
            }
            viewController.presentingViewController != null -> {
                viewController.dismissViewControllerAnimated(true, completion = null)
            }
            nav?.presentingViewController != null -> {
                nav.dismissViewControllerAnimated(true, completion = null)
            }
            else -> {
                exit(0)
            }
        }
    }
}
