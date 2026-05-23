package com.please.stop.app.core

import platform.UIKit.UIApplication
import platform.UIKit.UINavigationController
import platform.UIKit.UITabBarController
import platform.UIKit.UIViewController

internal fun presentIosViewController(viewController: UIViewController) {
    topViewController()?.presentViewController(
        viewControllerToPresent = viewController,
        animated = true,
        completion = null,
    )
}

private fun topViewController(): UIViewController? =
    UIApplication.sharedApplication.keyWindow
        ?.rootViewController
        ?.topMostViewController()

private tailrec fun UIViewController.topMostViewController(): UIViewController {
    val nextViewController = presentedViewController
        ?: visibleNavigationViewController()
        ?: selectedTabViewController()
        ?: return this

    return if (nextViewController == this) {
        this
    } else {
        nextViewController.topMostViewController()
    }
}

private fun UIViewController.visibleNavigationViewController(): UIViewController? =
    (this as? UINavigationController)?.visibleViewController

private fun UIViewController.selectedTabViewController(): UIViewController? =
    (this as? UITabBarController)?.selectedViewController
