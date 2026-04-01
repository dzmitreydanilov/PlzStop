package com.please.stop.app.core

/**
 * An abstract base class that combines [StateHolder] with action tracking capabilities.
 *
 * This class is designed to track the latest action performed by the user, allowing for
 * automatic retry functionality when operations fail. It's particularly useful for handling
 * network operations, database transactions, or any other operations that might fail and
 * require retry.
 *
 * ## Key Features
 * - Automatically tracks actions derived from events
 * - Maintains a reference to the last performed action
 * - Provides retry functionality for failed operations
 * - Reduces boilerplate code in state holder implementations
 *
 * ## How It Works
 * 1. When an event is processed via [processEvent], it's mapped to an action
 * using [mapEventToOperation]
 * 2. If an action is identified, it's tracked using [trackAction]
 * 3. When a retry is needed, the [LatestOperationTracker.handleRetry] method
 * uses the stored action to repeat the operation
 *
 * ## Implementation Example
 * ```
 * class PetInfoStateHolder(...) : ActionTrackingStateHolder<PetInfoState, PetInfoEvent,
 * PetInfoActions>() {
 *
 *     // Map events to actions
 *     override fun mapEventToOperation(event: PetInfoEvent): PetInfoActions? {
 *         return when (event) {
 *             is PetInfoEvent.UpdateImage -> PetInfoActions.UpdateAvatar(event.imageUri)
 *             is PetInfoEvent.DeleteAccount -> PetInfoActions.DeleteAccount
 *             is PetInfoEvent.Save -> PetInfoActions.Save
 *             else -> null
 *         }
 *     }
 *
 *     // Define retry behavior for each action
 *     override fun handleRetry(operation: PetInfoActions?): Flow<Result> {
 *         return when (operation) {
 *             is PetInfoActions.DeleteAccount -> deleteAccountUseCase.execute()
 *             is PetInfoActions.UpdateAvatar -> updateAvatarUseCase.execute(
 *                 IUpdateAvatarUseCase.Params(operation.url)
 *             )
 *             is PetInfoActions.Save -> savePetProfileData()
 *             else -> getPetUseCase.execute()
 *         }
 *     }
 *
 *     // Handle retry events in resolveEventResult
 *     override fun resolveEventResult(event: PetInfoEvent): Flow<Result> {
 *         return when (event) {
 *             is PetInfoEvent.Retry -> handleRetry(lastAction)
 *             // Other event handling...
 *         }
 *     }
 * }
 * ```
 *
 * ## Usage in UI
 * When an error occurs, the UI can trigger a retry by sending a Retry event:
 * ```
 * UnexpectedErrorDialogContent(
 *     errorType = (state as PetInfoState.Error).errorType,
 *     onRetryClick = {
 *         stateHolder.processEvent(PetInfoEvent.Retry)
 *     }
 * )
 * ```
 *
 * @param S The state type managed by this state holder
 * @param E The event type that can be processed by this state holder
 * @param T The action type that extends [LastAction] and can be tracked
 */
abstract class ActionTrackingStateHolder<S, E, T : LastAction> :
    StateHolder<S, E>(), LatestOperationTracker<S, E, T> {

    /**
     * Delegate for action tracking functionality.
     * Uses [DefaultOperationTrackingStateHolder] to avoid duplicating tracking logic.
     */
    private val actionTracker = DefaultOperationTrackingStateHolder<S, E, T>(
        eventMapper = { event -> mapEventToOperation(event) },
        retryHandler = { operation -> handleRetry(operation) }
    )

    /**
     * Returns the last tracked action, or null if no action has been tracked.
     * This is used by the retry mechanism to determine which action to retry.
     */
    override val lastAction: T? get() = actionTracker.lastAction

    /**
     * Tracks an action by storing it for potential retry.
     * When an operation succeeds, this should be called with null to clear the tracked action.
     *
     * @param operation The action to track, or null to clear the tracked action
     */
    override fun trackAction(operation: LastAction?) {
        actionTracker.trackAction(operation)
    }

    /**
     * Maps an event to an action that can be tracked.
     * Override this in subclasses to define which events should be tracked as actions.
     * Return null for events that don't correspond to trackable actions.
     *
     * @param event The event to map to an action
     * @return The corresponding action, or null if the event doesn't map to an action
     */
    override fun mapEventToOperation(event: E): T? {
        return null
    }

    /**
     * Processes an event, automatically tracking any corresponding action.
     * This override ensures that actions are tracked without requiring explicit calls
     * in each state holder implementation.
     *
     * @param event The event to process
     */
    override fun processEvent(event: E) {
        val operation = mapEventToOperation(event)
        if (operation != null) {
            trackAction(operation)
        }
        super.processEvent(event)
    }
}
