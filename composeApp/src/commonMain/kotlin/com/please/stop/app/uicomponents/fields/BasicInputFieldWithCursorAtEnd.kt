package com.please.stop.app.uicomponents.fields

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun BasicInputFieldWithCursorAtEnd(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = TextStyle.Default,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    cursorBrush: Brush = SolidColor(MaterialTheme.colorScheme.primary),
    maxLength: Int? = null,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() }
) {
    // State to hold the latest TextFieldValue
    var textFieldValueState by remember {
        mutableStateOf(
            TextFieldValue(
                text = value,
                selection = TextRange(value.length, value.length)
            )
        )
    }

    if (textFieldValueState.text != value) {
        textFieldValueState = textFieldValueState.copy(
            text = value,
            selection = TextRange(value.length, value.length)
        )
    }

    BasicTextField(
        value = textFieldValueState,
        onValueChange = { newTextFieldValueState ->
            val newText = newTextFieldValueState.text

            val finalText = if (maxLength != null && newText.length > maxLength) {
                newText.take(maxLength)
            } else {
                newText
            }

            val finalTextFieldValue = if (finalText != newText) {
                val selection = newTextFieldValueState.selection
                val newSelection = TextRange(
                    start = minOf(selection.start, finalText.length),
                    end = minOf(selection.end, finalText.length)
                )
                newTextFieldValueState.copy(
                    text = finalText,
                    selection = newSelection
                )
            } else {
                newTextFieldValueState
            }

            textFieldValueState = finalTextFieldValue

            if (finalText != value) {
                onValueChange(finalText)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        onTextLayout = onTextLayout,
        interactionSource = interactionSource,
        cursorBrush = cursorBrush,
        decorationBox = decorationBox,
    )
}
