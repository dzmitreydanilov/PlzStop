package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.theme.AppTheme
import kotlinx.collections.immutable.persistentListOf

private const val COLUMNS_COMPACT = 3
private const val COLUMNS_MEDIUM = 4
private const val COLUMNS_EXPANDED = 6

@Composable
internal fun HomeBody(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
    val columnCount = when {
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND) -> COLUMNS_EXPANDED
        windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_MEDIUM_LOWER_BOUND) -> COLUMNS_MEDIUM
        else -> COLUMNS_COMPACT
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columnCount),
        modifier = modifier.padding(horizontal = 8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            HomeHeader(
                displayName = state.displayName,
                totalSpentFormatted = state.totalSpentFormatted.orEmpty(),
                onProfileClicked = { onEvent(HomeEvent.ProfileClicked) },
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(modifier = Modifier.height(8.dp))
        }

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

@Suppress("MagicNumber")
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeBodyWithCategoriesPreview() {
    val categories = persistentListOf(
        HomeCategoryUiModel(1, "Food", "ic_food", "$120.50", true),
        HomeCategoryUiModel(2, "Transport", "ic_transport", "$45.00", true),
        HomeCategoryUiModel(3, "Entertainment", "ic_entertainment", "$0.00", false),
    )
    AppTheme {
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
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun HomeBodyEmptyPreview() {
    val categories = persistentListOf(
        HomeCategoryUiModel(1, "Food", "ic_food", "$0.00", false),
        HomeCategoryUiModel(2, "Transport", "ic_transport", "$0.00", false),
    )
    AppTheme {
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
}
