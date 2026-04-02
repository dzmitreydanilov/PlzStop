package com.dog.care.utils.uicomponents.modifiers

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.constrainWidth

fun Modifier.exposedDropdownSize(matchTextFieldWidth: Boolean = true, anchorWidth: Int): Modifier =
    layout { measurable, constraints ->
        val menuWidth = constraints.constrainWidth(anchorWidth)
        val menuConstraints =
            constraints.copy(
                minWidth =
                if (matchTextFieldWidth) menuWidth else constraints.minWidth,
                maxWidth =
                if (matchTextFieldWidth) menuWidth else constraints.maxWidth,
            )
        val placeable = measurable.measure(menuConstraints)
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
