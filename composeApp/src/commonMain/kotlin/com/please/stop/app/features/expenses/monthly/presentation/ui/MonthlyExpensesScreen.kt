package com.please.stop.app.features.expenses.monthly.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.expenses.monthly.presentation.DayGroupUiModel
import com.please.stop.app.features.expenses.monthly.presentation.ExpenseEntryUiModel
import com.please.stop.app.features.expenses.monthly.presentation.MonthlyExpensesEvent
import com.please.stop.app.features.expenses.monthly.presentation.MonthlyExpensesNavigation
import com.please.stop.app.features.expenses.monthly.presentation.MonthlyExpensesState
import com.please.stop.app.features.expenses.monthly.presentation.MonthlyExpensesStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_arrow_forward

@Composable
fun MonthlyExpensesScreen(
    onNavigateToEditExpense: (expenseId: Long) -> Unit,
) {
    val stateHolder = koinViewModel<MonthlyExpensesStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            is MonthlyExpensesNavigation.OpenEditExpense -> onNavigateToEditExpense(navigation.expenseId)
        }
    }

    val errorOverlay: ScreenOverlay? = when (val s = state) {
        is MonthlyExpensesState.Error -> ScreenOverlay.Error(type = s.errorType)
        else -> null
    }

    ScreenOverlayContainer(
        overlay = errorOverlay,
        onDismiss = {},
    ) {
        DisplayFullScreenProgress(showProgress = state is MonthlyExpensesState.Loading)

        when (val s = state) {
            is MonthlyExpensesState.Content -> MonthlyExpensesContent(
                state = s,
                onEvent = stateHolder::processEvent,
            )
            else -> {}
        }
    }
}

@Composable
private fun MonthlyExpensesContent(
    state: MonthlyExpensesState.Content,
    onEvent: (MonthlyExpensesEvent) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        MonthHeader(
            monthLabel = state.monthLabel,
            canGoToPreviousMonth = state.canGoToPreviousMonth,
            canGoToNextMonth = state.canGoToNextMonth,
            onPreviousClicked = { onEvent(MonthlyExpensesEvent.PreviousMonthClicked) },
            onNextClicked = { onEvent(MonthlyExpensesEvent.NextMonthClicked) },
        )
        if (state.isEmpty) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No expenses this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            MonthlyExpenseList(
                dayGroups = state.dayGroups,
                expandedReceiptIds = state.expandedReceiptIds,
                onExpenseClicked = { id -> onEvent(MonthlyExpensesEvent.ExpenseClicked(id)) },
                onReceiptGroupClicked = { id -> onEvent(MonthlyExpensesEvent.ReceiptGroupClicked(id)) },
            )
        }
    }
}

@Composable
private fun MonthHeader(
    monthLabel: String,
    canGoToPreviousMonth: Boolean,
    canGoToNextMonth: Boolean,
    onPreviousClicked: () -> Unit,
    onNextClicked: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(
            onClick = onPreviousClicked,
            enabled = canGoToPreviousMonth,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_arrow_back),
                contentDescription = "Previous month",
            )
        }
        Text(
            text = monthLabel,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(
            onClick = onNextClicked,
            enabled = canGoToNextMonth,
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_arrow_forward),
                contentDescription = "Next month",
            )
        }
    }
}

@Composable
private fun MonthlyExpenseList(
    dayGroups: ImmutableList<DayGroupUiModel>,
    expandedReceiptIds: ImmutableSet<Long>,
    onExpenseClicked: (Long) -> Unit,
    onReceiptGroupClicked: (Long) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        dayGroups.forEach { group ->
            stickyHeader(key = group.dayEpochMillis) {
                DayGroupHeader(group)
            }
            group.entries.forEach { entry ->
                when (entry) {
                    is ExpenseEntryUiModel.Single -> {
                        item(key = "single_${entry.id}") {
                            ExpenseCard(
                                expense = entry,
                                onClick = { onExpenseClicked(entry.id) },
                            )
                        }
                    }
                    is ExpenseEntryUiModel.ReceiptGroup -> {
                        item(key = "receipt_${entry.receiptId}") {
                            ReceiptGroupCard(
                                group = entry,
                                isExpanded = entry.receiptId in expandedReceiptIds,
                                onGroupClicked = { onReceiptGroupClicked(entry.receiptId) },
                                onExpenseClicked = onExpenseClicked,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGroupHeader(group: DayGroupUiModel) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = group.dayLabel,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = group.totalFormatted,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ReceiptGroupCard(
    group: ExpenseEntryUiModel.ReceiptGroup,
    isExpanded: Boolean,
    onGroupClicked: () -> Unit,
    onExpenseClicked: (Long) -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onGroupClicked)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = group.merchantName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = "${group.itemCount} items",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = group.amountFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (isExpanded) "▲" else "▼",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isExpanded) {
            group.expenses.forEach { expense ->
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                ExpenseRowContent(
                    expense = expense,
                    onClick = { onExpenseClicked(expense.id) },
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: ExpenseEntryUiModel.Single,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onClick,
    ) {
        ExpenseRowContent(expense = expense, onClick = onClick)
    }
}

@Composable
private fun ExpenseRowContent(
    expense: ExpenseEntryUiModel.Single,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = expense.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = expense.categoryName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = expense.amountFormatted,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}
