package com.please.stop.app.uicomponents.buttons

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.please.stop.app.uicomponents.modifiers.debounceClickable

@Composable
fun RoundOutlinedDebounceButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 48.dp,
    debounceInterval: Long = 400,
    backgroundColor: Color = Color.Transparent,
    borderColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = modifier
            .size(size)
            .background(color = backgroundColor, shape = CircleShape)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = CircleShape
            )
            .clip(CircleShape)
            .then(
                if (enabled) {
                    Modifier.debounceClickable(
                        debounceInterval = debounceInterval,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
