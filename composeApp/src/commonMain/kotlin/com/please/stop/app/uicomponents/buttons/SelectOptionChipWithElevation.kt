package com.please.stop.app.uicomponents.buttons

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.dog.care.utils.uicomponents.modifiers.noRippleClickable
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.content_desc_not_selected
import plzstop.composeapp.generated.resources.content_desc_selected

@Composable
fun SelectOptionChipWithElevation(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    color: Color = MaterialTheme.colorScheme.background,
    selectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val selectionState = if (isSelected) {
        stringResource(Res.string.content_desc_selected)
    } else {
        stringResource(Res.string.content_desc_not_selected)
    }
    val boxShape = CircleShape
    Box(
        modifier = modifier
            .semantics { stateDescription = selectionState }
            .shadow(
                elevation = 4.dp,
                shape = boxShape,
                clip = false
            )
            .background(
                color = color,
                shape = boxShape
            )
            .clip(boxShape)
            .border(
                border = BorderStroke(
                    width = if (isSelected) {
                        1.dp
                    } else {
                        0.dp
                    },
                    color = if (isSelected) {
                        selectedColor
                    } else {
                        Color.Transparent
                    }
                ),
                shape = boxShape
            )
            .noRippleClickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),

        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = if (isSelected) {
                selectedColor
            } else {
                LocalContentColor.current
            }
        )
    }
}

@Composable
fun SelectOptionChip(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    backgroundColor: Color = MaterialTheme.colorScheme.background,
    backgroundSelectedColor: Color = MaterialTheme.colorScheme.primary
) {
    val selectionState = if (isSelected) {
        stringResource(Res.string.content_desc_selected)
    } else {
        stringResource(Res.string.content_desc_not_selected)
    }
    val boxShape = CircleShape
    Box(
        modifier = modifier
            .semantics { stateDescription = selectionState }
            .background(
                color = if (isSelected) backgroundSelectedColor else backgroundColor,
                shape = boxShape
            )
            .clip(boxShape)
            .noRippleClickable(role = Role.Button) { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = title)
    }
}

@Composable
fun SelectOptionChip(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    verticalPadding: Dp = 8.dp,
    color: Color = MaterialTheme.colorScheme.background,
    textContent: @Composable () -> Unit,
    onClick: () -> Unit = {}
) {
    val boxShape = CircleShape
    Box(
        modifier = modifier
            .background(
                color = color,
                shape = boxShape
            )
            .noRippleClickable(role = Role.Button) { onClick() }
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        contentAlignment = Alignment.Center
    ) {
        textContent()
    }
}
