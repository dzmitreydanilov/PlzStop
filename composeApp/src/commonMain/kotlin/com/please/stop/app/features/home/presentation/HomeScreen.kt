package com.please.stop.app.features.home.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.home.presentation.ui.CollapsedBarHeight
import com.please.stop.app.features.home.presentation.ui.CollapsingHomeHeader
import com.please.stop.app.features.home.presentation.ui.HomeBody
import com.please.stop.app.navigation.CollectNavigationFlow
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    onNavigateToAddExpense: (selectedCategoryId: Long) -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val stateHolder = koinViewModel<HomeStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    CollectNavigationFlow(
        flow = stateHolder.getNavigation(),
        key1 = stateHolder,
    ) { navigation ->
        when (navigation) {
            is HomeNavigation.NavigateToAddExpense -> onNavigateToAddExpense(navigation.expenseCategoryId)
            HomeNavigation.NavigateToSettings -> onNavigateToSettings()
        }
    }
    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(HomeEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(showProgress = state is HomeState.Loading)
        HomeContent(
            state = state,
            onEvent = stateHolder::processEvent,
        )
    }
}

@Composable
private fun HomeContent(
    state: HomeState,
    onEvent: (HomeEvent) -> Unit,
) {
    val gridState = rememberLazyGridState()
    val headerScrollState = rememberHomeHeaderScrollState()
    val isGridScrollable = gridState.canScrollForward || gridState.canScrollBackward

    SideEffect {
        headerScrollState.updateContentScrollability(isScrollable = isGridScrollable)
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(headerScrollState.nestedScrollConnection),
        ) {
            HomeHeader(
                state = state,
                headerScrollState = headerScrollState,
                onEvent = onEvent,
            )

            HomeBody(
                state = state,
                onEvent = onEvent,
                gridState = gridState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )
        }
    }
}

@Composable
private fun HomeHeader(
    state: HomeState,
    headerScrollState: HomeHeaderScrollState,
    onEvent: (HomeEvent) -> Unit,
) {
    CollapsingHomeHeader(
        displayName = state.displayName,
        totalSpentFormatted = state.totalSpentFormatted.orEmpty(),
        onProfileClicked = { onEvent(HomeEvent.ProfileClicked) },
        currentHeight = headerScrollState.currentHeight,
        expandedHeight = headerScrollState.expandedHeight,
        onExpandedHeightMeasured = headerScrollState::onExpandedHeightMeasured,
        modifier = Modifier,
    )
}

@Composable
private fun rememberHomeHeaderScrollState(): HomeHeaderScrollState {
    val density = LocalDensity.current
    val collapsedBarHeightPx = with(density) { CollapsedBarHeight.toPx() }

    return remember(collapsedBarHeightPx) {
        HomeHeaderScrollState(
            density = density,
            collapsedBarHeightPx = collapsedBarHeightPx,
        )
    }
}

@Stable
private class HomeHeaderScrollState(
    private val density: Density,
    private val collapsedBarHeightPx: Float,
) {
    private var expandedHeightPx by mutableFloatStateOf(0f)
    private var offsetPx by mutableFloatStateOf(0f)
    private var isContentScrollable = false

    val currentHeight: Dp
        get() = with(density) {
            if (expandedHeightPx > 0f) {
                (expandedHeightPx + offsetPx).coerceAtLeast(collapsedBarHeightPx).toDp()
            } else {
                Dp.Unspecified
            }
        }

    val expandedHeight: Dp
        get() = with(density) { expandedHeightPx.toDp() }

    val nestedScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            if (available.y >= 0f) return Offset.Zero
            return updateOffset(availableY = available.y)
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            if (available.y <= 0f) return Offset.Zero
            return updateOffset(availableY = available.y)
        }
    }

    fun onExpandedHeightMeasured(measuredHeightPx: Float) {
        if (measuredHeightPx <= expandedHeightPx) return

        expandedHeightPx = measuredHeightPx
        offsetPx = offsetPx.coerceIn(
            minimumValue = headerMaxOffset(
                expandedHeightPx = measuredHeightPx,
                collapsedBarHeightPx = collapsedBarHeightPx,
            ),
            maximumValue = 0f,
        )
    }

    fun updateContentScrollability(isScrollable: Boolean) {
        if (offsetPx == 0f) {
            isContentScrollable = isScrollable
        }
    }

    private fun updateOffset(availableY: Float): Offset {
        if (availableY < 0f && !isContentScrollable) return Offset.Zero

        val maxOffset = headerMaxOffset(
            expandedHeightPx = expandedHeightPx,
            collapsedBarHeightPx = collapsedBarHeightPx,
        )
        if (maxOffset == 0f) return Offset.Zero

        val newOffset = (offsetPx + availableY).coerceIn(maxOffset, 0f)
        val consumed = newOffset - offsetPx
        offsetPx = newOffset
        return Offset(x = 0f, y = consumed)
    }
}

private fun headerMaxOffset(
    expandedHeightPx: Float,
    collapsedBarHeightPx: Float,
): Float = if (expandedHeightPx > collapsedBarHeightPx) {
    -(expandedHeightPx - collapsedBarHeightPx)
} else {
    0f
}
