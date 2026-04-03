package com.please.stop.app.uicomponents.sheets

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.composables.core.DragIndication
import com.composables.core.ModalBottomSheetState
import com.composables.core.ModalSheetProperties
import com.composables.core.Scrim
import com.composables.core.Sheet
import com.composeunstyled.ProvideContentColor
import com.please.stop.app.uicomponents.ANIMATION_DURATION_MS
import com.composables.core.ModalBottomSheet as UnstyledModalBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppModalBottomSheet(
    state: ModalBottomSheetState,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    showDragIndicator: Boolean = true,
    draggableIndicatorColors: DragIndicatorColors =
        DragIndicatorColors(backgroundColor = MaterialTheme.colorScheme.onSurfaceVariant),
    properties: ModalSheetProperties = ModalSheetProperties(),
    content: @Composable () -> Unit
) {
    UnstyledModalBottomSheet(state = state, onDismiss = onDismiss, properties = properties) {
        Scrim(
            enter = fadeIn(tween(ANIMATION_DURATION_MS)),
            exit = fadeOut(tween(ANIMATION_DURATION_MS)),
            scrimColor = BottomSheetDefaults.ScrimColor
        )
        Sheet(
            modifier
                .imePadding()
                .widthIn(max = 640.dp)
                .fillMaxWidth()
                .dropShadow(
                    shadow = Shadow(
                        offset = DpOffset(0.dp, 4.dp),
                        radius = 4.dp,
                        spread = 0.dp,
                        color = Color.Black.copy(alpha = 0.25f),
                        blendMode = BlendMode.SrcOver
                    ),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ),
            backgroundColor = MaterialTheme.colorScheme.surfaceContainerLow,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            ProvideContentColor(MaterialTheme.colorScheme.onSurface) {
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
