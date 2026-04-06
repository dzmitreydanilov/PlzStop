package com.please.stop.app.features.analytics.monthly.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.analytics.monthly.presentation.DayGroupUiModel
import com.please.stop.app.features.analytics.monthly.presentation.ExpenseEntryUiModel
import com.please.stop.app.features.analytics.monthly.presentation.MonthPageState
import com.please.stop.app.features.analytics.monthly.presentation.MonthlyExpensesEvent
import com.please.stop.app.features.analytics.monthly.presentation.MonthlyExpensesNavigation
import com.please.stop.app.features.analytics.monthly.presentation.MonthlyExpensesStateHolder
import com.please.stop.app.features.analytics.monthly.presentation.MonthlyWindowState
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import com.please.stop.app.utils.date.currentMonthPageIndex
import com.please.stop.app.utils.date.monthName
import com.please.stop.app.utils.date.pageIndexToYearMonth
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_arrow_forward
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_down
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_up
import plzstop.composeapp.generated.resources.monthly_expenses_collapse_receipt
import plzstop.composeapp.generated.resources.monthly_expenses_empty
import plzstop.composeapp.generated.resources.monthly_expenses_expand_receipt
import plzstop.composeapp.generated.resources.monthly_expenses_item_count
import plzstop.composeapp.generated.resources.monthly_expenses_next_month
import plzstop.composeapp.generated.resources.monthly_expenses_previous_month
import plzstop.composeapp.generated.resources.monthly_expenses_receipt_fallback

@Composable
fun MonthlyExpensesScreen(
    onNavigateToEditExpense: (expenseId: Long) -> Unit,
) {
    val stateHolder = koinViewModel<MonthlyExpensesStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    val maxPage = remember { currentMonthPageIndex() }
    val pagerState = rememberPagerState(
        initialPage = maxPage,
        pageCount = { maxPage + 1 },
    )

    LaunchedEffect(pagerState.currentPage) {
        val (year, month) = pageIndexToYearMonth(pagerState.currentPage)
        stateHolder.processEvent(MonthlyExpensesEvent.MonthSelected(year, month))
    }

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            is MonthlyExpensesNavigation.OpenEditExpense -> onNavigateToEditExpense(navigation.expenseId)
        }
    }

    val (currentYear, currentMonth) = remember(pagerState.currentPage) {
        pageIndexToYearMonth(pagerState.currentPage)
    }

    ScreenOverlayContainer(
        overlay = state.asOverlay(currentYear, currentMonth),
        onDismiss = {},
    ) {
        Scaffold(
            topBar = {
                MonthHeader(
                    monthLabel = "${monthName(currentMonth)} $currentYear",
                    canGoToPreviousMonth = pagerState.currentPage > 0,
                    canGoToNextMonth = pagerState.currentPage < maxPage,
                    onPreviousClicked = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                    },
                    onNextClicked = {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                )
            },
        ) { innerPadding ->
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 1,
                modifier = Modifier.padding(innerPadding),
            ) { page ->
                val (year, month) = pageIndexToYearMonth(page)
                val pageState = state.pageStateFor(year, month)

                MonthBody(
                    state = pageState,
                    onExpenseClicked = { id ->
                        stateHolder.processEvent(MonthlyExpensesEvent.ExpenseClicked(id))
                    },
                    onReceiptGroupClicked = { id ->
                        stateHolder.processEvent(
                            MonthlyExpensesEvent.ReceiptGroupClicked(id, year, month)
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun MonthBody(
    state: MonthPageState,
    modifier: Modifier = Modifier,
    onExpenseClicked: (Long) -> Unit,
    onReceiptGroupClicked: (Long) -> Unit,
) {
    when {
        state is MonthPageState.Loading -> DisplayFullScreenProgress(
            showProgress = true,
            modifier = modifier.fillMaxSize(),
        )

        state.isEmpty -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.monthly_expenses_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        else -> MonthlyExpenseList(
            dayGroups = state.dayGroups,
            expandedReceiptIds = state.expandedReceiptIds,
            modifier = modifier,
            onExpenseClicked = onExpenseClicked,
            onReceiptGroupClicked = onReceiptGroupClicked,
        )
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
                contentDescription = stringResource(Res.string.monthly_expenses_previous_month),
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
                contentDescription = stringResource(Res.string.monthly_expenses_next_month),
            )
        }
    }
}

@Composable
private fun MonthlyExpenseList(
    dayGroups: ImmutableList<DayGroupUiModel>,
    expandedReceiptIds: ImmutableSet<Long>,
    modifier: Modifier = Modifier,
    onExpenseClicked: (Long) -> Unit,
    onReceiptGroupClicked: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
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
                    text = group.merchantName
                        ?: stringResource(Res.string.monthly_expenses_receipt_fallback),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    text = stringResource(Res.string.monthly_expenses_item_count, group.itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = group.amountFormatted,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Icon(
                imageVector = if (isExpanded) vectorResource(Res.drawable.ic_keyboard_arrow_up)
                else vectorResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = stringResource(
                    if (isExpanded) Res.string.monthly_expenses_collapse_receipt
                    else Res.string.monthly_expenses_expand_receipt
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
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

internal fun MonthlyWindowState.asOverlay(year: Int, month: Int): ScreenOverlay? =
    when (val pageState = pageStateFor(year, month)) {
        is MonthPageState.Error -> ScreenOverlay.Error(type = pageState.errorType)
        else -> null
    }
