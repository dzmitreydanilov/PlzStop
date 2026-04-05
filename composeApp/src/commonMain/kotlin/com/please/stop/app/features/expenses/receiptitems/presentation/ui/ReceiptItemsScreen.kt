package com.please.stop.app.features.expenses.receiptitems.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.expenses.presentation.CategoryUiModel
import com.please.stop.app.features.expenses.presentation.SubcategoryUiModel
import com.please.stop.app.features.expenses.receiptitems.presentation.ConversionSummary
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemUiModel
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemsEvent
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemsNavigation
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemsState
import com.please.stop.app.features.expenses.receiptitems.presentation.ReceiptItemsStateHolder
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.navigation.routes.ReceiptItemsRoute
import com.please.stop.app.utils.date.DatePattern
import com.please.stop.app.utils.date.currentMonthMillisRange
import com.please.stop.app.utils.date.format
import com.please.stop.app.utils.date.localDateTimeFromMillis
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.vectorResource
import kotlin.math.pow
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_arrow_back
import plzstop.composeapp.generated.resources.ic_calendar
import plzstop.composeapp.generated.resources.ic_edit
import plzstop.composeapp.generated.resources.ic_error
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_right

@Composable
fun ReceiptItemsScreen(
    route: ReceiptItemsRoute,
    onGoBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val stateHolder = koinViewModel<ReceiptItemsStateHolder>(
        key = "receipt_items_${route.merchantName}",
    ) { parametersOf(route) }

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            ReceiptItemsNavigation.GoBack -> onGoBack()
            ReceiptItemsNavigation.PopTwice -> onSaved()
            ReceiptItemsNavigation.PopOnce -> onGoBack()
        }
    }

    val state by stateHolder.state.collectAsStateWithLifecycle()

    when (val s = state) {
        is ReceiptItemsState.Content -> ReceiptItemsContent(
            state = s,
            isManualEntry = route.isManualEntry,
            onEvent = stateHolder::processEvent,
        )
        is ReceiptItemsState.Error -> {
            // minimal error state
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptItemsContent(
    state: ReceiptItemsState.Content,
    isManualEntry: Boolean,
    onEvent: (ReceiptItemsEvent) -> Unit,
) {
    val monthRange = remember { currentMonthMillisRange() }
    val isDateFromDifferentMonth = state.dateMillis < monthRange.fromMillis || state.dateMillis >= monthRange.toMillis

    val editingItem = state.editingItemId?.let { id -> state.items.firstOrNull { it.id == id } }

    if (editingItem != null) {
        ModalBottomSheet(onDismissRequest = { onEvent(ReceiptItemsEvent.DoneEditingItem(editingItem.id)) }) {
            EditItemSheet(
                item = editingItem,
                categories = state.categories,
                subcategories = state.subcategories,
                currencySymbol = state.currency.symbol,
                onEvent = onEvent,
            )
        }
    }

    if (state.showDatePicker) {
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.dateMillis)
        DatePickerDialog(
            onDismissRequest = { onEvent(ReceiptItemsEvent.DismissDatePicker) },
            confirmButton = {
                TextButton(onClick = {
                    val selected = datePickerState.selectedDateMillis
                    if (selected != null) onEvent(ReceiptItemsEvent.DateChanged(selected))
                    else onEvent(ReceiptItemsEvent.DismissDatePicker)
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { onEvent(ReceiptItemsEvent.DismissDatePicker) }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (state.showDateWarningDialog) {
        val dateText = remember(state.dateMillis) {
            localDateTimeFromMillis(state.dateMillis).date.toString()
        }
        AlertDialog(
            onDismissRequest = { onEvent(ReceiptItemsEvent.DismissDateWarningDialog) },
            title = { Text("Receipt from a different month") },
            text = {
                Text(
                    "This receipt is dated $dateText, which is outside the current month. " +
                        "Expenses will be saved to that month and won't appear in this month's dashboard. " +
                        "You can change the date using the calendar icon."
                )
            },
            confirmButton = {
                TextButton(onClick = { onEvent(ReceiptItemsEvent.DismissDateWarningDialog) }) {
                    Text("Got it")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Receipt Items") },
                navigationIcon = {
                    IconButton(onClick = { onEvent(ReceiptItemsEvent.BackClicked) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_arrow_back),
                            contentDescription = "Back",
                        )
                    }
                },
                actions = {
                    if (isDateFromDifferentMonth) {
                        IconButton(onClick = { onEvent(ReceiptItemsEvent.ShowDateWarningDialog) }) {
                            Icon(
                                imageVector = vectorResource(Res.drawable.ic_error),
                                contentDescription = "Date warning",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    IconButton(onClick = { onEvent(ReceiptItemsEvent.ShowDatePicker) }) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_calendar),
                            contentDescription = "Change date",
                        )
                    }
                },
            )
        },
        bottomBar = {
            StickyFooter(
                state = state,
                onConfirm = { onEvent(ReceiptItemsEvent.ConfirmAll) },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                ReceiptHeader(
                    merchantName = state.merchantName,
                    dateMillis = state.dateMillis,
                    isDateFromDifferentMonth = isDateFromDifferentMonth,
                    isDateAutoAssigned = state.isDateAutoAssigned,
                )
            }
            items(state.items, key = { it.id }) { item ->
                SwipeToDeleteItem(
                    onDelete = { onEvent(ReceiptItemsEvent.DeleteItem(item.id)) },
                ) {
                    ReceiptItemCard(
                        item = item,
                        currencySymbol = state.currency.symbol,
                        decimalPlaces = state.currency.decimalPlaces,
                        onTap = { onEvent(ReceiptItemsEvent.EditItem(item.id)) },
                    )
                }
            }
            if (isManualEntry) {
                item {
                    TextButton(
                        onClick = { onEvent(ReceiptItemsEvent.AddItem) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    ) {
                        Text("+ Add item")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDeleteItem(
    onDelete: () -> Unit,
    content: @Composable () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = "Delete",
                    modifier = Modifier.padding(end = 20.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        content = { content() },
    )
}

@Composable
private fun ReceiptHeader(
    merchantName: String?,
    dateMillis: Long,
    isDateFromDifferentMonth: Boolean,
    isDateAutoAssigned: Boolean,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        merchantName?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
        val dateText = remember(dateMillis) {
            localDateTimeFromMillis(dateMillis).date.toString()
        }
        val dateColor = when {
            isDateFromDifferentMonth -> MaterialTheme.colorScheme.error
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        if (isDateAutoAssigned) {
            Text(
                text = "Date not found on receipt — saved as today ($dateText)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.tertiary,
            )
        } else {
            Text(
                text = dateText,
                style = MaterialTheme.typography.bodyMedium,
                color = dateColor,
            )
        }
    }
}

@Composable
private fun ReceiptItemCard(
    item: ReceiptItemUiModel,
    currencySymbol: String,
    decimalPlaces: Int,
    onTap: () -> Unit,
) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = item.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatAmount(item.amountMinorUnits, currencySymbol, decimalPlaces),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    IconButton(onClick = onTap, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_edit),
                            contentDescription = "Edit",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (item.categoryName != null || item.subcategoryName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(end = 12.dp),
                ) {
                    item.categoryName?.let { name ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = {},
                            label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(20.dp),
                            colors = androidx.compose.material3.SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                            border = null,
                        )
                    }
                    if (item.categoryName != null && item.subcategoryName != null) {
                        Icon(
                            imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item.subcategoryName?.let { name ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = {},
                            label = { Text(name, style = MaterialTheme.typography.labelMedium) },
                            shape = RoundedCornerShape(20.dp),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditItemSheet(
    item: ReceiptItemUiModel,
    categories: ImmutableList<CategoryUiModel>,
    subcategories: ImmutableList<SubcategoryUiModel>,
    currencySymbol: String,
    onEvent: (ReceiptItemsEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = item.name,
            onValueChange = { onEvent(ReceiptItemsEvent.ItemNameChanged(item.id, it)) },
            label = { Text("Item name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        OutlinedTextField(
            value = item.amountInput,
            onValueChange = { onEvent(ReceiptItemsEvent.ItemAmountChanged(item.id, it)) },
            label = { Text("Amount") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { onEvent(ReceiptItemsEvent.DoneEditingItem(item.id)) }),
            suffix = { Text(currencySymbol) },
        )
        if (categories.isNotEmpty()) {
            Text("Category", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                categories.forEach { category ->
                    FilterChip(
                        selected = item.categoryId == category.id,
                        onClick = { onEvent(ReceiptItemsEvent.ItemCategoryChanged(item.id, category.id)) },
                        label = { Text(category.name, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }
        }
        val filteredSubcategories = subcategories.filter { it.parentCategoryId == item.categoryId }
        if (filteredSubcategories.isNotEmpty()) {
            Text("Subcategory", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                filteredSubcategories.forEach { sub ->
                    FilterChip(
                        selected = item.subcategoryId == sub.id,
                        onClick = {
                            val newId = if (item.subcategoryId == sub.id) null else sub.id
                            onEvent(ReceiptItemsEvent.ItemSubcategoryChanged(item.id, newId))
                        },
                        label = { Text(sub.name, style = MaterialTheme.typography.labelMedium) },
                        shape = RoundedCornerShape(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StickyFooter(
    state: ReceiptItemsState.Content,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Total",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = formatAmount(
                    state.totalAmountMinorUnits,
                    state.currency.symbol,
                    state.currency.decimalPlaces,
                ),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        state.conversionSummary?.let { summary ->
            Text(
                text = "1 ${summary.originalCurrencyCode} = ${summary.rate} → " +
                    "${summary.defaultCurrencySymbol}${formatAmount(summary.convertedTotalMinorUnits, "", 2)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isSaving,
        ) {
            if (state.isSaving) {
                CircularProgressIndicator()
            } else {
                Text("Confirm")
            }
        }
    }
}

private fun formatAmount(minorUnits: Long, symbol: String, decimalPlaces: Int): String {
    if (decimalPlaces == 0) return "$symbol$minorUnits"
    val multiplier = 10.0.pow(decimalPlaces).toLong()
    val intPart = minorUnits / multiplier
    val fracPart = (minorUnits % multiplier).toString().padStart(decimalPlaces, '0')
    return "$symbol$intPart.$fracPart"
}
