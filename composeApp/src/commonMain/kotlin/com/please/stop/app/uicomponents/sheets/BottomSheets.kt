package com.please.stop.app.uicomponents.sheets

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composeunstyled.DragIndication
import com.composeunstyled.ModalBottomSheetProperties
import com.composeunstyled.ModalBottomSheetState
import com.composeunstyled.Scrim
import com.composeunstyled.Sheet
import com.composeunstyled.SheetDetent
import com.composeunstyled.UnstyledModalBottomSheet
import com.composeunstyled.rememberModalBottomSheetState
import kotlinx.coroutines.launch

/**
 * Remembers the default app modal sheet state for sheets that should stay fully expanded while composed.
 */
@Composable
fun rememberFullyExpandedAppModalBottomSheetState(): ModalBottomSheetState {
    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.FullyExpanded,
        detents = remember { listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded) },
    )

    LaunchedEffect(
        sheetState.isIdle,
        sheetState.currentDetent,
    ) {
        if (
            sheetState.isIdle &&
            sheetState.currentDetent == SheetDetent.Hidden
        ) {
            sheetState.animateTo(SheetDetent.FullyExpanded)
        }
    }

    return sheetState
}

/**
 * Displays a fully expanded modal sheet and removes it only after its dismissal animation completes.
 *
 * The [content] slot receives the same dismissal action used by scrim, back, and escape interactions.
 */
@Composable
fun AnimatedAppModalBottomSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragIndicator: Boolean = true,
    draggableIndicatorColors: DragIndicatorColors =
        DragIndicatorColors(backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant),
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable (dismiss: () -> Unit) -> Unit,
) {
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(
        initialDetent = SheetDetent.Hidden,
        detents = remember { listOf(SheetDetent.Hidden, SheetDetent.FullyExpanded) },
    )
    var isDismissing by remember { mutableStateOf(false) }

    LaunchedEffect(sheetState) {
        sheetState.animateTo(SheetDetent.FullyExpanded)
    }

    val dismiss = {
        if (!isDismissing) {
            isDismissing = true
            scope.launch {
                sheetState.animateTo(SheetDetent.Hidden)
                latestOnDismiss()
            }
        }
    }

    AppModalBottomSheet(
        state = sheetState,
        onDismiss = dismiss,
        modifier = modifier,
        showDragIndicator = showDragIndicator,
        draggableIndicatorColors = draggableIndicatorColors,
        properties = properties,
    ) {
        content(dismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    state: ModalBottomSheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragIndicator: Boolean = true,
    draggableIndicatorColors: DragIndicatorColors =
        DragIndicatorColors(backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant),
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable () -> Unit
) {
    UnstyledModalBottomSheet(
        state = state,
        onDismiss = onDismiss,
        properties = properties,
        overlay = {
            Scrim(
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
                scrimColor = BottomSheetDefaults.ScrimColor
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentAlignment = Alignment.TopCenter,
        ) {
            Sheet(
                modifier = modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .dropShadow(
                        shadow = Shadow(
                            offset = DpOffset(0.dp, 4.dp),
                            radius = 4.dp,
                            spread = 0.dp,
                            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f),
                            blendMode = BlendMode.SrcOver
                        ),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                    )
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column {
                    if (showDragIndicator) {
                        DragIndication(
                            Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(top = 8.dp)
                                .background(
                                    color = draggableIndicatorColors.backgroundColor,
                                    MaterialTheme.shapes.small
                                ).height(4.dp)
                                .width(32.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    content()
                }
            }
        }
    }
}

data class DragIndicatorColors(val backgroundColor: Color)
