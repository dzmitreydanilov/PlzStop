package com.please.stop.app.features.analytics.presentation.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.features.analytics.presentation.AnalyticsEvent
import com.please.stop.app.features.analytics.presentation.AnalyticsState
import com.please.stop.app.features.analytics.presentation.AnalyticsStateHolder
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.animation.FadeSlideIn
import com.please.stop.app.uicomponents.animation.rememberShimmerOffset
import com.please.stop.app.uicomponents.error.ScreenOverlay
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import com.please.stop.app.uicomponents.progress.DisplayFullScreenProgress
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.analytics_insights_subtitle
import plzstop.composeapp.generated.resources.analytics_tab
import plzstop.composeapp.generated.resources.ic_send

@Composable
fun AnalyticsScreen() {
    val stateHolder = koinViewModel<AnalyticsStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()
    var showExportSheet by remember { mutableStateOf(false) }

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(AnalyticsEvent.DismissError) },
        onAutoDismiss = { stateHolder.processEvent(AnalyticsEvent.DismissError) },
    ) {
        DisplayFullScreenProgress(showProgress = state is AnalyticsState.Loading)
        Box(modifier = Modifier.fillMaxSize()) {
            AnalyticsScreenContent(
                state = state,
                onEvent = stateHolder::processEvent,
            )
            if (state is AnalyticsState.Content) {
                FloatingActionButton(
                    onClick = { showExportSheet = true },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                ) {
                    Icon(
                        vectorResource(Res.drawable.ic_send),
                        contentDescription = "Export to Sheets"
                    )
                }
            }
        }
    }
}

private val AnalyticsState.asOverlay: ScreenOverlay?
    @Composable get() = when (this) {
        is AnalyticsState.Error -> ScreenOverlay.Error(type = errorType)
        else -> null
    }

@Composable
private fun AnalyticsScreenContent(
    state: AnalyticsState,
    onEvent: (AnalyticsEvent) -> Unit,
) {
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        FadeSlideIn {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                    .background(appColors.headerGradient)
                    .padding(horizontal = 20.dp)
                    .padding(top = 48.dp, bottom = 24.dp),
            ) {
                Text(
                    text = stringResource(Res.string.analytics_tab),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = appColors.headerContent,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(Res.string.analytics_insights_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = appColors.headerContent.copy(alpha = 0.8f),
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (val s = state) {
            is AnalyticsState.Content -> DashboardContent(
                state = s,
                onEvent = onEvent,
            )

            else -> {
                /* Loading/Error handled by ScreenOverlayContainer + DisplayFullScreenProgress */
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
private fun DashboardContent(
    state: AnalyticsState.Content,
    onEvent: (AnalyticsEvent) -> Unit,
) {
    val shimmerOffset = rememberShimmerOffset()
    val appColors = LocalAppColors.current

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        FadeSlideIn(delayMillis = 100) {
            QuickStatRow(state)
        }

        if (state.hasAnyExpenses && state.spendingSlices.isNotEmpty()) {
            state.budgetBurn?.let { burn ->
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 150) {
                    BudgetPulseCard(burn)
                }
            }

            if (state.spendingSlices.size > 1) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 200) {
                    SpendingOverviewCard(
                        slices = state.spendingSlices,
                        shimmerOffset = shimmerOffset,
                    )
                }
            }

            if (state.dailySpending.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 300) {
                    DailyTrendsCard(
                        dailySpending = state.dailySpending,
                        hasBudgetPacing = state.budgetPacingPoints.isNotEmpty(),
                        budgetPacingPoints = state.budgetPacingPoints,
                    )
                }
            }

            if (state.heatmapDays.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 400) {
                    SpendingHeatmapCard(
                        heatmapDays = state.heatmapDays,
                        accentColor = appColors.chartColors.first(),
                        onDayTap = { day -> onEvent(AnalyticsEvent.DayTapped(day)) },
                    )
                }
            }

            if (state.monthlyBars.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 500) {
                    MonthlyComparisonCard(
                        monthlyBars = state.monthlyBars,
                        projectedTotal = state.projectedTotal,
                    )
                }
            }

            if (state.categoryProgress.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                FadeSlideIn(delayMillis = 600) {
                    CategoryProgressCard(categoryProgress = state.categoryProgress)
                }
            }
        }

        if (!state.hasAnyExpenses) {
            Spacer(modifier = Modifier.height(20.dp))
            FadeSlideIn(delayMillis = 200) {
                EmptyStateCard()
            }
        }
    }

    if (state.selectedDaySheet != null || state.isDaySheetLoading) {
        DayExpensesSheet(
            sheetUi = state.selectedDaySheet,
            isLoading = state.isDaySheetLoading,
            onDismiss = { onEvent(AnalyticsEvent.DismissDaySheet) },
        )
    }
}
