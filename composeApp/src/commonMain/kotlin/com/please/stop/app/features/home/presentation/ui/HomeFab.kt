package com.please.stop.app.features.home.presentation.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.home.presentation.HomeEvent
import com.please.stop.app.uicomponents.ANIMATION_DURATION_MS
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_add_category
import plzstop.composeapp.generated.resources.home_add_expense
import plzstop.composeapp.generated.resources.ic_add
import plzstop.composeapp.generated.resources.ic_edit

private const val FAB_ICON_ROTATION = 45f
private const val MINI_FAB_SIZE = 40
private const val INITIAL_SCALE = 0.8f
private const val SLIDE_OFFSET_PX = 30f
private const val STAGGER_DELAY_MS = 80L
private val MINI_FAB_ELEVATION = 6.dp

@Composable
internal fun HomeFab(onEvent: (HomeEvent) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    val rotation by animateFloatAsState(
        targetValue = if (expanded) FAB_ICON_ROTATION else 0f,
        label = "fab_rotation",
    )

    Column(
        modifier = Modifier.wrapContentSize(),
        horizontalAlignment = Alignment.End,
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(expandFrom = Alignment.Bottom),
            exit = shrinkVertically(shrinkTowards = Alignment.Bottom),
        ) {
            Column(
                modifier = Modifier
                    .wrapContentSize()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MiniFabItem(
                    label = stringResource(Res.string.home_add_expense),
                    icon = { Icon(vectorResource(Res.drawable.ic_edit), contentDescription = null) },
                    index = 1,
                    expanded = expanded,
                    onClick = {
                        expanded = false
                        onEvent(HomeEvent.AddExpenseClicked)
                    },
                )
                MiniFabItem(
                    label = stringResource(Res.string.home_add_category),
                    icon = { Icon(vectorResource(Res.drawable.ic_add), contentDescription = null) },
                    index = 0,
                    expanded = expanded,
                    onClick = {
                        expanded = false
                        onEvent(HomeEvent.AddCategoryClicked)
                    },
                )
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            modifier = Modifier.padding(top = 12.dp),
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_add),
                contentDescription = stringResource(Res.string.home_add_expense),
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}

@Composable
private fun MiniFabItem(
    label: String,
    icon: @Composable () -> Unit,
    index: Int,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(index * STAGGER_DELAY_MS)
        progress.animateTo(1f, tween(ANIMATION_DURATION_MS, easing = FastOutSlowInEasing))
    }

    Row(
        modifier = Modifier
            .wrapContentSize()
            .padding(end = 4.dp)
            .graphicsLayer {
                val scale = INITIAL_SCALE + (1f - INITIAL_SCALE) * progress.value
                scaleX = scale
                scaleY = scale
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        )

        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(MINI_FAB_SIZE.dp),
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = if (expanded && progress.value == 1f) MINI_FAB_ELEVATION else 0.dp,
                pressedElevation = MINI_FAB_ELEVATION,
                focusedElevation = MINI_FAB_ELEVATION,
                hoveredElevation = MINI_FAB_ELEVATION,
            ),
        ) {
            icon()
        }
    }
}
