package com.please.stop.app.features.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenRoute(
) {
    val stateHolder = koinViewModel<HomeStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    val factory = rememberPermissionsControllerFactory()
    val controller = remember(factory) { factory.createPermissionsController() }
    BindEffect(controller)

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(HomeEvent.DismissOverlayPopup) },
        onAutoDismiss = { stateHolder.processEvent(HomeEvent.DismissOverlayPopup) },
        onRetry = { stateHolder.processEvent(HomeEvent.Retry) }
    ) {
        HomeScreenContent(
            modifier = Modifier,
            state = state,
            onNavigateLogin = { stateHolder.processEvent(HomeEvent.SignInProposalDialogDismissed) },
            onSignInProposalDialogDismiss = { stateHolder.processEvent(HomeEvent.SignIn) },
            onNavigateProfile = onNavigateProfile,
            onAddSymptomsClick = { stateHolder.processEvent(HomeEvent.AddSymptomsClick) },
            onCreateReminder = { stateHolder.processEvent(HomeEvent.CreateReminderClick) },
            onReminderSelect = { id, date -> onReminderClick(id, date.value) },
            onOpenRemindersList = { stateHolder.processEvent(HomeEvent.SeeAllRemindersClick) },
            onStorySelect = { stateHolder.processEvent(HomeEvent.StorySelected(it)) },
            onOpenStories = { stateHolder.processEvent(HomeEvent.OpenStoriesBottomSheet(it)) },
            onDismissStoriesBottomSheet = { stateHolder.processEvent(HomeEvent.DismissStoriesBottomSheet) }
        )
    }

    ResultEffect<ReminderCreatedResult> {
        stateHolder.state.first { it !is HomeState.Initial }
        stateHolder.processEvent(HomeEvent.ReminderCreated)
    }

    LaunchedEffect(key1 = state.isGuest, key2 = state.pushNotificationRequested) {
        if (!state.isGuest && state.pushNotificationRequested == false) {
            val isGranted = try {
                if (
                    controller.getPermissionState(Permission.REMOTE_NOTIFICATION)
                    != PermissionState.Granted
                ) {
                    controller.providePermission(Permission.REMOTE_NOTIFICATION)
                }
                true
            } catch (e: DeniedAlwaysException) {
                logDebug("REMOTE_NOTIFICATION Request denied")
                false
            } catch (e: DeniedException) {
                logDebug("REMOTE_NOTIFICATION Request denied always")
                false
            } catch (e: RequestCanceledException) {
                logDebug("REMOTE_NOTIFICATION Request canceled")
                false
            }
            stateHolder.processEvent(HomeEvent.PushNotificationPermissionGranted(isGranted))
        }
    }

    CollectNavigationFlow(
        key1 = stateHolder,
        flow = stateHolder.getNavigation()
    ) { navigation ->
        when (navigation) {
            is HomeNavigation.PreSignIn -> onNavigatePreSignIn()
            is HomeNavigation.SignIn -> onNavigateLogin()
            is NavigateCreateReminderScreen -> onCreateReminder()
            is NavigateAddSymptomsScreen -> onAddSymptomsClick()
            is NavigateSeeAllReminderScreen -> onNavigateRemindersSearch()
            is NavigateStoriesScreen -> onNavigateArticlesFromStories(navigation.selectedStoryId)
        }
    }
}