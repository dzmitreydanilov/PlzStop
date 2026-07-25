package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewWrapper
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.uicomponents.previews.ApplicationPreviewThemeWrapper
import kotlinx.collections.immutable.persistentListOf

private const val COLUMNS_COMPACT = 3
private const val COLUMNS_MEDIUM = 4
private const val COLUMNS_EXPANDED = 6

@Composable
internal fun HomeBody(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
    gridState: LazyGridState = rememberLazyGridState(),
) {
    val columnCount = getColumnCount()

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        state = gridState,
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(
            items = state.categories,
            key = { _, it -> it.id },
        ) { _, category ->
            CategoryTile(
                category = category,
                onClick = { onEvent(HomeEvent.CategoryClicked(category.id)) },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun getColumnCount(): Int {
    val windowSizeClass = currentWindowAdaptiveInfoV2().windowSizeClass
    return when {
        windowSizeClass.isWidthAtLeastBreakpoint(
            widthDpBreakpoint = WIDTH_DP_EXPANDED_LOWER_BOUND
        ) -> COLUMNS_EXPANDED

        windowSizeClass.isWidthAtLeastBreakpoint(
            widthDpBreakpoint = WIDTH_DP_MEDIUM_LOWER_BOUND
        ) -> COLUMNS_MEDIUM

        else -> COLUMNS_COMPACT
    }
}

@Suppress("MagicNumber")
@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun HomeBodyWithCategoriesPreview() {
    val categories = persistentListOf(
        HomeCategoryUiModel(1, "Food", "ic_food", "$120.50", true),
        HomeCategoryUiModel(2, "Transport", "ic_transport", "$45.00", true),
        HomeCategoryUiModel(3, "Entertainment", "ic_entertainment", "$0.00", false),
        HomeCategoryUiModel(4, "Shopping", "ic_food", "$89.00", true),
        HomeCategoryUiModel(5, "Health", "ic_food", "$35.00", true),
        HomeCategoryUiModel(6, "Education", "ic_food", "$50.00", true),
        HomeCategoryUiModel(7, "Bills", "ic_food", "$200.00", true),
        HomeCategoryUiModel(8, "Groceries", "ic_food", "$150.00", true),
        HomeCategoryUiModel(9, "Coffee", "ic_food", "$22.50", true),
        HomeCategoryUiModel(10, "Gym", "ic_food", "$40.00", true),
        HomeCategoryUiModel(11, "Clothing", "ic_food", "$75.00", true),
        HomeCategoryUiModel(12, "Pets", "ic_food", "$30.00", true),
        HomeCategoryUiModel(13, "Gifts", "ic_food", "$60.00", true),
        HomeCategoryUiModel(14, "Travel", "ic_food", "$300.00", true),
        HomeCategoryUiModel(15, "Rent", "ic_food", "$1200.00", true),
        HomeCategoryUiModel(16, "Insurance", "ic_food", "$180.00", true),
        HomeCategoryUiModel(17, "Savings", "ic_food", "$500.00", true),
        HomeCategoryUiModel(18, "Dining Out", "ic_food", "$95.00", true),
        HomeCategoryUiModel(19, "Streaming", "ic_food", "$15.00", true),
        HomeCategoryUiModel(20, "Phone", "ic_food", "$55.00", true),
        HomeCategoryUiModel(21, "Internet", "ic_food", "$60.00", true),
        HomeCategoryUiModel(22, "Gas", "ic_food", "$70.00", true),
        HomeCategoryUiModel(23, "Parking", "ic_food", "$25.00", true),
        HomeCategoryUiModel(24, "Taxi", "ic_food", "$40.00", true),
        HomeCategoryUiModel(25, "Books", "ic_food", "$18.00", true),
        HomeCategoryUiModel(26, "Music", "ic_food", "$10.00", true),
        HomeCategoryUiModel(27, "Games", "ic_food", "$30.00", true),
        HomeCategoryUiModel(28, "Hobbies", "ic_food", "$45.00", true),
        HomeCategoryUiModel(29, "Charity", "ic_food", "$20.00", true),
        HomeCategoryUiModel(30, "Laundry", "ic_food", "$12.00", true),
        HomeCategoryUiModel(31, "Haircut", "ic_food", "$35.00", true),
        HomeCategoryUiModel(32, "Dentist", "ic_food", "$100.00", true),
        HomeCategoryUiModel(33, "Vitamins", "ic_food", "$25.00", true),
        HomeCategoryUiModel(34, "Electronics", "ic_food", "$200.00", true),
        HomeCategoryUiModel(35, "Furniture", "ic_food", "$0.00", false),
        HomeCategoryUiModel(36, "Tools", "ic_food", "$0.00", false),
        HomeCategoryUiModel(37, "Garden", "ic_food", "$15.00", true),
        HomeCategoryUiModel(38, "Kids", "ic_food", "$80.00", true),
        HomeCategoryUiModel(39, "Snacks", "ic_food", "$8.00", true),
        HomeCategoryUiModel(40, "Alcohol", "ic_food", "$45.00", true),
    )
    HomeBody(
        state = HomeState.Content(
            displayName = "Dmitry",
            currency = null,
            totalSpentFormatted = "$165.50",
            categories = categories,
            hasAnyExpenses = true,
            showAddCategorySheet = false,
        ),
        onEvent = {},
    )
}

@Preview(showBackground = true, showSystemUi = true)
@PreviewWrapper(ApplicationPreviewThemeWrapper::class)
@Composable
private fun HomeBodyEmptyPreview() {
    val categories = persistentListOf(
        HomeCategoryUiModel(1, "Food", "ic_food", "$0.00", false),
        HomeCategoryUiModel(2, "Transport", "ic_transport", "$0.00", false),
    )
    HomeBody(
        state = HomeState.Content(
            displayName = "Dmitry",
            currency = null,
            totalSpentFormatted = "$0.00",
            categories = categories,
            hasAnyExpenses = false,
            showAddCategorySheet = false,
        ),
        onEvent = {},
    )
}
