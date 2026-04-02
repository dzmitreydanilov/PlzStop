package com.please.stop.app.uicomponents.snackbar.core

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.currentValueOf
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A modifier that prevents content from being overlapped by a snackbar positioned at the top of the screen.
 *
 * When applied to a composable, this modifier will automatically adjust the padding to ensure that
 * the content remains visible when a snackbar is displayed at the top of the screen.
 *
 * @return A [Modifier] with the no-overlap behavior applied.
 */
public fun Modifier.noOverlapTopContentBySnackbar(): Modifier {
    return this then com.please.stop.app.uicomponents.snackbar.core.LocalMessengerOffsetElement(
        alignment = com.please.stop.app.uicomponents.snackbar.core.ContentAlignment.Top
    )
}

/**
 * A modifier that prevents content from being overlapped by a snackbar positioned at the bottom of the screen.
 *
 * When applied to a composable, this modifier will automatically adjust the padding to ensure that
 * the content remains visible when a snackbar is displayed at the bottom of the screen.
 *
 * @return A [Modifier] with the no-overlap behavior applied.
 */
public fun Modifier.noOverlapBottomContentBySnackbar(): Modifier {
    return this then com.please.stop.app.uicomponents.snackbar.core.LocalMessengerOffsetElement(
        alignment = com.please.stop.app.uicomponents.snackbar.core.ContentAlignment.Bottom
    )
}

@Suppress("CompositionLocalAllowlist")
internal val LocalBottomMessengerOffset = compositionLocalOf { mutableStateMapOf<String, Int>() }

@Suppress("CompositionLocalAllowlist")
internal val LocalTopMessengerOffset = compositionLocalOf { mutableStateMapOf<String, Int>() }

private data class LocalMessengerOffsetElement(
    private val alignment: com.please.stop.app.uicomponents.snackbar.core.ContentAlignment,
) : ModifierNodeElement<com.please.stop.app.uicomponents.snackbar.core.LocalMessengerOffsetModifierNode>() {
    override fun create() =
        com.please.stop.app.uicomponents.snackbar.core.LocalMessengerOffsetModifierNode(
            alignment
        )

    @Suppress("EmptyFunctionBlock")
    override fun update(node: com.please.stop.app.uicomponents.snackbar.core.LocalMessengerOffsetModifierNode) {
    }
}

private class LocalMessengerOffsetModifierNode(
    private val alignment: com.please.stop.app.uicomponents.snackbar.core.ContentAlignment
) : Modifier.Node(), CompositionLocalConsumerModifierNode, GlobalPositionAwareModifierNode {
    @OptIn(ExperimentalUuidApi::class)
    private val key = Uuid.random().toString()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (coordinates.isAttached) {
            val y = coordinates.positionInWindow().y
            val rootHeight = coordinates.findRootCoordinates().size.height
            when (alignment) {
                com.please.stop.app.uicomponents.snackbar.core.ContentAlignment.Top -> {
                    val topOffset = (coordinates.size.height + y).toInt().coerceAtMost(rootHeight)
                    val localMessageOffsets = currentValueOf(com.please.stop.app.uicomponents.snackbar.core.LocalTopMessengerOffset)
                    localMessageOffsets[key] = topOffset
                }

                com.please.stop.app.uicomponents.snackbar.core.ContentAlignment.Bottom -> {
                    val bottomOffset = (rootHeight - y).toInt().coerceAtLeast(0)
                    val localMessageOffsets = currentValueOf(com.please.stop.app.uicomponents.snackbar.core.LocalBottomMessengerOffset)
                    localMessageOffsets[key] = bottomOffset
                }
            }
        }
    }

    override fun onDetach() {
        val localBottomMessageOffsets = currentValueOf(com.please.stop.app.uicomponents.snackbar.core.LocalBottomMessengerOffset)
        val localTopMessageOffsets = currentValueOf(com.please.stop.app.uicomponents.snackbar.core.LocalTopMessengerOffset)
        localBottomMessageOffsets.remove(key)
        localTopMessageOffsets.remove(key)
    }
}

private enum class ContentAlignment {
    Top,
    Bottom
}
