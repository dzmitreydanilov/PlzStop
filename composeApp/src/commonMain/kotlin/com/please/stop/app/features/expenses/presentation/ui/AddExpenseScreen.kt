package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.expenses.create.presentation.CreateExpenseStateHolder
import com.please.stop.app.features.expenses.domain.model.ReceiptError
import com.please.stop.app.features.expenses.edit.presentation.EditExpenseStateHolder
import com.please.stop.app.features.expenses.presentation.AddExpenseEvent
import com.please.stop.app.features.expenses.presentation.AddExpenseNavigation
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.BaseExpenseStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.navigation.nav3.HandleNavigationBack
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.add_expense_receipt_no_network
import plzstop.composeapp.generated.resources.add_expense_receipt_not_receipt
import plzstop.composeapp.generated.resources.add_expense_receipt_service_unavailable
import plzstop.composeapp.generated.resources.add_expense_receipt_unreadable

@Composable
fun CreateExpenseScreen(
    categoryId: Long?,
    onGoBack: () -> Unit,
    onOpenReceiptItems: () -> Unit = {},
) {
    val stateHolder = koinViewModel<CreateExpenseStateHolder>(
        key = "create_expense_$categoryId",
    ) { parametersOf(categoryId) }

    ExpenseScreenContent(
        stateHolder = stateHolder,
        onGoBack = onGoBack,
        onOpenReceiptItems = onOpenReceiptItems,
    )
}

@Composable
fun EditExpenseScreen(
    expenseId: Long,
    onGoBack: () -> Unit,
) {
    val stateHolder = koinViewModel<EditExpenseStateHolder>(
        key = "edit_expense_$expenseId",
    ) { parametersOf(expenseId) }

    ExpenseScreenContent(
        stateHolder = stateHolder,
        onGoBack = onGoBack,
    )
}

@Composable
private fun ExpenseScreenContent(
    stateHolder: BaseExpenseStateHolder,
    onGoBack: () -> Unit,
    onOpenReceiptItems: () -> Unit = {},
) {
    val state = stateHolder.state.collectAsStateWithLifecycle().value

    HandleNavigationBack(enabled = state.status.hasUnsavedChanges) {
        stateHolder.processEvent(AddExpenseEvent.BackClicked)
    }

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            AddExpenseNavigation.GoBack -> onGoBack()
            AddExpenseNavigation.OpenReceiptItems -> onOpenReceiptItems()
        }
    }

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(AddExpenseEvent.DismissError) },
    ) {
        AddExpenseContent(
            state = state,
            onEvent = stateHolder::processEvent,
        )
    }
}

internal val AddExpenseState.asOverlay: ScreenOverlay?
    @Composable
    get() = errorOverlay?.let { overlay ->
        ScreenOverlay.Error(
            type = overlay.errorType,
            title = overlay.receiptError?.toOverlayMessage(),
        )
    }

@Composable
private fun ReceiptError.toOverlayMessage(): String = when (this) {
    ReceiptError.NOT_RECEIPT -> stringResource(Res.string.add_expense_receipt_not_receipt)
    ReceiptError.UNREADABLE -> stringResource(Res.string.add_expense_receipt_unreadable)
    ReceiptError.NO_NETWORK -> stringResource(Res.string.add_expense_receipt_no_network)
    ReceiptError.SERVICE_UNAVAILABLE -> stringResource(Res.string.add_expense_receipt_service_unavailable)
}
