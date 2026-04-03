package com.please.stop.app.features.home.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import com.please.stop.app.features.home.presentation.HomeCategoryUiModel
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.features.home.presentation.HomeState
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.uicomponents.animation.FadeSlideIn
import kotlinx.collections.immutable.persistentListOf
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_empty_hint
import plzstop.composeapp.generated.resources.home_empty_title

private const val COLUMNS_COMPACT = 3
private const val COLUMNS_MEDIUM = 4
private const val COLUMNS_EXPANDED = 6
private const val EMPTY_STATE_DELAY_MS = 300

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

        if (!state.hasAnyExpenses && state.categories.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                FadeSlideIn(delayMillis = EMPTY_STATE_DELAY_MS) {
                    EmptyStateHint()
                }
            }
        }

        itemsIndexed(
            items = state.categories,
            key = { _, it -> it.id },
        ) { index, category ->
            CategoryTile(
                category = category,
                index = index,
                onClick = { onEvent(HomeEvent.CategoryClicked(category.id)) },
            )
        }
    }
}

@Composable
private fun EmptyStateHint() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "\uD83D\uDCB0",
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun EmptyStateHintPreview() {
    AppTheme {
        EmptyStateHint()
    }
}

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
