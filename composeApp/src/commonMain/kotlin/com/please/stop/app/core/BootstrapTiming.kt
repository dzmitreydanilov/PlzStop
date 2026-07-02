package com.please.stop.app.core

/**
 * Determines when bootstrap() should execute relative to state emission.
 */
enum class BootstrapTiming {
    /**
     * Bootstrap executes immediately, blocking initial state emission until complete.
     * Use for critical initialization that must complete before the screen can render.
     */
    IMMEDIATE,

    /**
     * Bootstrap executes after the initial state is emitted to UI.
     * Use for non-critical initialization like retry logic or background data refresh.
     * This improves perceived performance by allowing UI to render immediately.
     */
    DEFERRED
}
