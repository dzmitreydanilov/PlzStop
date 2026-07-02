package com.please.stop.app.core

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.please.stop.app.core.models.domain.ErrorResult
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import com.please.stop.app.core.models.domain.toErrorType
import com.please.stop.app.core.models.presentation.Navigation
import com.please.stop.app.core.models.presentation.UiEffect
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.reflect.KClass

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

@Suppress("TooManyFunctions")
@OptIn(ExperimentalCoroutinesApi::class)
abstract class StateHolder<S, E> : ViewModel() {

    private val navigationChannel: Channel<Navigation> = Channel(capacity = BUFFERED)
    private val effectChannel: Channel<UiEffect> = Channel(capacity = BUFFERED)
    private val events = MutableSharedFlow<E>()
    open val sharingStarted: SharingStarted = SharingStarted.WhileSubscribed(5_000L)
    private val onceInitCompleted = atomic(false)
    private val onceInitInProgress = atomic(false)
    private val lastEmittedState = atomic<S?>(null)

    open val state: StateFlow<S> by lazy {
        val initial = getInitial()
        collectDataFlows().transformIntoState(initial)
            .stateIn(
                scope = viewModelScope,
                started = sharingStarted,
                initialValue = initial
            )
    }

    protected abstract val tag: String

    /**
     * Specifies when bootstrap() should execute relative to state emission.
     * Override in subclasses to use DEFERRED for non-critical initialization.
     */
    protected open val bootstrapTiming: BootstrapTiming = BootstrapTiming.IMMEDIATE

    /**
     * Debug-only safety net: if non-null, logs a warning when [bootstrap] is still running after this timeout.
     *
     * Notes:
     * - This does NOT cancel bootstrap, it only logs.
     * - Keep it disabled in release builds.
     */
    protected open val bootstrapWatchdogTimeoutMillis: Long? = 5_000L

    /**
     * Transforms the collected results into state using the provided initial state and concurrency model.
     *
     * @receiver A flow of results that will be transformed into state.
     * @return A StateFlow representing the transformed state.
     */
    protected fun Flow<Result>.transformIntoState(initial: S): Flow<S> {
        return flow {
            // IMPORTANT:
            // - `stateIn(initialValue = initial)` already exposes initial state to the UI.
            // - Because we use `SharingStarted.WhileSubscribed()`, this upstream can be cancelled and restarted.
            //   We seed reduction from the last emitted state to avoid "resetting" the reducer base on restarts.
            var acc: S = lastEmittedState.value ?: initial

            collect { result ->
                navigate(result)
                if (getNavigationResults().any { it.isInstance(result) }) return@collect

                dispatchEffect(result)
                if (getEffectResults().any { it.isInstance(result) }) return@collect

                acc = getStateByResultWithErrorHandling(acc, result)
                lastEmittedState.value = acc
                saveState(acc)
                emit(acc)
            }
        }.logStateCollectionCancellation()
    }

    protected open fun saveState(state: S) {
    }

    protected abstract fun getInitial(): S

    private fun Flow<S>.logStateCollectionCancellation(): Flow<S> {
        return onCompletion {
            if (it is CancellationException) {
                Logger.e { "$tag state collection cancelled" }
            }
        }
    }

    /**
     * Collects every result source that can update state, dispatch navigation, or dispatch effects.
     */
    protected fun collectDataFlows(): Flow<Result> {
        return merge(
            eventResultsFlow(),
            whileSubscribedResultsFlow(),
            bootstrapOnceFlow(),
        )
    }

    /**
     * A hook for subclasses to provide flows that should be active whenever the state is collected.
     *
     * @return An empty flow by default, but subclasses can override to add specific flows.
     */
    protected open fun collectWhileSubscribed(): Flow<Result> {
        return emptyFlow()
    }

    private fun eventResultsFlow(): Flow<Result> {
        return events.flatMapLatest(::resolveEventResult)
    }

    private fun whileSubscribedResultsFlow(): Flow<Result> {
        return flow {
            // Defer calling subclass overrides until the state is actually collected.
            emitAll(collectWhileSubscribed())
        }
    }

    private fun bootstrapOnceFlow(): Flow<Result> {
        return flow {
            // Run once-per-ViewModel bootstrap work (e.g., initial fetch) without re-triggering on re-subscribe.
            if (!onceInitCompleted.value && onceInitInProgress.compareAndSet(
                    expect = false,
                    update = true
                )
            ) {
                try {
                    // For DEFERRED timing, yield to allow initial state to emit first.
                    if (bootstrapTiming == BootstrapTiming.DEFERRED) {
                        yield()
                    }

                    val timeoutMillis = bootstrapWatchdogTimeoutMillis
                    if (timeoutMillis != null && timeoutMillis > 0L) {
                        coroutineScope {
                            val watchdog = launch {
                                delay(timeoutMillis)
                                if (!onceInitCompleted.value && onceInitInProgress.value) {
                                    Logger.w { "$tag bootstrapOnce is still running after ${timeoutMillis}ms" }
                                }
                            }

                            try {
                                bootstrap { result -> emit(result) }
                            } finally {
                                watchdog.cancel()
                            }
                        }
                    } else {
                        bootstrap { result -> emit(result) }
                    }

                    onceInitCompleted.value = true
                } catch (cancellationExc: CancellationException) {
                    // Do not mark bootstrap as completed when collection stops before bootstrap finishes.
                    throw cancellationExc
                } finally {
                    onceInitInProgress.value = false
                }
            }
        }
    }

    /**
     * A hook for subclasses to perform one-shot bootstrap work when the state is collected for the first time.
     *
     * Typical use-cases:
     * - initial screen fetch that should not re-run when the UI temporarily stops collecting
     *
     * Cancellation semantics:
     * - if the first collection is cancelled before completion, bootstrap will be re-run on the next collection.
     */
    protected open suspend fun bootstrap(emit: suspend (Result) -> Unit) {
        // default no-op
    }

    /**
     * Returns a flow of navigation events that other classes can observe.
     *
     * @return A flow of [Navigation] objects.
     */
    open fun getNavigation(): Flow<Navigation> = navigationChannel.receiveAsFlow()

    /**
     * Returns a flow of one-time UI effects (snackbar messages, toasts, etc.) that the UI
     * should collect and consume exactly once.
     */
    fun getEffects(): Flow<UiEffect> = effectChannel.receiveAsFlow()

    /**
     * Emits an event that will trigger a state change, use cases execution.
     *
     * @param event The event to process.
     */
    open fun processEvent(event: E) {
        viewModelScope.launch {
            events.emit(event)
        }
    }

    /**
     * Specifies a set of [Result] types that should trigger navigation logic.
     *
     * @return An empty set by default, but subclasses can override to define relevant result types.
     */
    protected open fun getNavigationResults(): Set<KClass<out Result>> {
        return emptySet()
    }

    /**
     * Retrieves the navigation destination for a given result. Can be overridden by subclasses
     * to map specific results to navigation.
     *
     * @param result The result that may trigger navigation.
     * @return A [Navigation] object if navigation is required, or null otherwise.
     */
    protected open fun getNavigationByResult(result: Result): Navigation? {
        return null
    }

    /**
     * Specifies a set of [Result] types that are effect-only and should be skipped
     * during state reduction. Mirrors [getNavigationResults].
     */
    protected open fun getEffectResults(): Set<KClass<out Result>> {
        return emptySet()
    }

    /**
     * Maps a result to a one-time [UiEffect]. Subclasses override to convert specific results
     * into fire-and-forget UI effects (snackbars, toasts, etc.). Mirrors [getNavigationByResult].
     */
    protected open fun getEffectByResult(result: Result): UiEffect? {
        return null
    }

    /**
     * Maps an event to a flow of results. This function is called whenever an event is processed
     * and is responsible for determining the resulting flow of state changes.
     *
     * By default, this returns an empty flow, but subclasses can override it to provide
     * custom logic for mapping events to results.
     *
     * @param event The event that triggered the state change.
     * @return A flow of [Result] objects representing the outcome of the event.
     */
    protected open fun resolveEventResult(event: E): Flow<Result> {
        return emptyFlow()
    }

    /**
     * Determines if a given result should trigger a navigation event, and if so, sends it.
     *
     * @param result The result that may trigger navigation.
     * @return A boolean indicating whether navigation occurred.
     */
    private fun navigate(result: Result): Boolean {
        val navigation = getNavigationByResult(result) ?: return false
        viewModelScope.launch {
            navigationChannel.send(navigation)
        }

        return true
    }

    /**
     * Determines if a given result should trigger a UI effect, and if so, sends it.
     * Mirrors [navigate].
     */
    private fun dispatchEffect(result: Result) {
        val effect = getEffectByResult(result) ?: return
        viewModelScope.launch { effectChannel.send(effect) }
    }

    /**
     * Maps a given result to a new state, handling any errors that may occur.
     *
     * @param previous The previous state before the result was processed.
     * @param result The result that may trigger a state change.
     * @return The new state after processing the result.
     */
    protected open fun getStateByResult(
        previous: S,
        result: Result,
    ): S {
        return if (result is ErrorResult) {
            getErrorStateByResult(result, result.errorType)
        } else {
            previous
        }
    }

    /**
     * Maps a given result to a new state, handling errors that may occur during the process.
     *
     * @param previous The previous state before the result was processed.
     * @param result The result that may trigger a state change.
     * @return The new state after processing the result, or the error state if an exception occurs.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun getStateByResultWithErrorHandling(previous: S, result: Result): S {
        return try {
            getStateByResult(previous, result)
        } catch (throwable: Throwable) {
            Logger.e { "$tag: ${throwable.message.orEmpty()}" }
            getErrorStateByResult(result, throwable.toErrorType())
        }
    }

    /**
     * Function for handling error states. Subclasses must implement this to map a
     * [ErrorType] to an error state.
     */
    abstract fun getErrorStateByResult(result: Result, errorType: ErrorType): S

    override fun onCleared() {
        Logger.d { "$tag onCleared()" }
        super.onCleared()
    }
}
