package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.expenses.presentation.KeyboardOperator
import com.please.stop.app.features.expenses.presentation.NumericKey
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_backspace
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.ic_schedule

private val BUTTON_SPACING = 6.dp
private val CORNER_RADIUS = 12.dp
private val SAVE_BUTTON_STROKE_WIDTH = 2.dp
private const val LEFT_COLUMNS_WEIGHT = 4f
private const val SAVE_BUTTON_ASPECT_RATIO = 0.5f
private const val SAVING_INDICATOR_HEIGHT_FRACTION = 0.3f

// Removed hardcoded glass colors — using theme tokens via LocalAppColors

@Suppress("MagicNumber")
@Composable
internal fun NumericKeyboard(
    currencySymbol: String,
    isInExpressionMode: Boolean,
    isSaving: Boolean,
    isSaveEnabled: Boolean,
    onKey: (NumericKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
    ) {
        // Row 1: ÷  7  8  9  ⌫
        KeyboardRow {
            OperatorButton("÷", KeyboardOperator.DIVIDE, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(7, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(8, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(9, onKey, Modifier.weight(1f).aspectRatio(1f))
            KeyButton(
                onClick = { onKey(NumericKey.Backspace) },
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_backspace),
                    contentDescription = null,
                )
            }
        }
        // Row 2: ×  4  5  6  📅
        KeyboardRow {
            OperatorButton("×", KeyboardOperator.MULTIPLY, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(4, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(5, onKey, Modifier.weight(1f).aspectRatio(1f))
            DigitButton(6, onKey, Modifier.weight(1f).aspectRatio(1f))
            KeyButton(
                onClick = { onKey(NumericKey.Calendar) },
                modifier = Modifier.weight(1f).aspectRatio(1f),
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_schedule),
                    contentDescription = null,
                )
            }
        }
        // Rows 3–4: Save/= spans right column across both rows
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
        ) {
            // Left 4 columns: rows 3 and 4 stacked
            Column(
                modifier = Modifier.weight(LEFT_COLUMNS_WEIGHT),
                verticalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
            ) {
                // Row 3: -  1  2  3
                KeyboardRow {
                    OperatorButton("-", KeyboardOperator.SUBTRACT, onKey, Modifier.weight(1f).aspectRatio(1f))
                    DigitButton(1, onKey, Modifier.weight(1f).aspectRatio(1f))
                    DigitButton(2, onKey, Modifier.weight(1f).aspectRatio(1f))
                    DigitButton(3, onKey, Modifier.weight(1f).aspectRatio(1f))
                }
                // Row 4: +  ¤  0  ,
                KeyboardRow {
                    OperatorButton("+", KeyboardOperator.ADD, onKey, Modifier.weight(1f).aspectRatio(1f))
                    KeyButton(
                        onClick = { onKey(NumericKey.CurrencySymbol) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                        enabled = false,
                    ) {
                        Text(currencySymbol, style = MaterialTheme.typography.titleMedium)
                    }
                    DigitButton(0, onKey, Modifier.weight(1f).aspectRatio(1f))
                    KeyButton(
                        onClick = { onKey(NumericKey.Decimal) },
                        modifier = Modifier.weight(1f).aspectRatio(1f),
                    ) {
                        Text(",", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            // Right column: Save/= spanning 2 rows
            SaveEqualsButton(
                isInExpressionMode = isInExpressionMode,
                isSaving = isSaving,
                isSaveEnabled = isSaveEnabled,
                onClick = { onKey(NumericKey.Equals) },
                modifier = Modifier.weight(1f).aspectRatio(SAVE_BUTTON_ASPECT_RATIO),
            )
        }
    }
}

@Composable
private fun KeyboardRow(
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BUTTON_SPACING),
        content = content,
    )
}

@Suppress("MagicNumber")
@Composable
private fun DigitButton(
    digit: Int,
    onKey: (NumericKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyButton(
        onClick = { onKey(NumericKey.Digit(digit)) },
        modifier = modifier,
    ) {
        Text(
            digit.toString(),
            style = MaterialTheme.typography.headlineSmall,
        )
    }
}

@Composable
private fun OperatorButton(
    symbol: String,
    op: KeyboardOperator,
    onKey: (NumericKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyButton(
        onClick = { onKey(NumericKey.Operator(op)) },
        modifier = modifier,
    ) {
        Text(
            symbol,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SaveEqualsButton(
    isInExpressionMode: Boolean,
    isSaving: Boolean,
    isSaveEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = if (isInExpressionMode) true else isSaveEnabled && !isSaving

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = RoundedCornerShape(CORNER_RADIUS),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = if (isInExpressionMode) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.tertiary
            },
            contentColor = if (isInExpressionMode) {
                MaterialTheme.colorScheme.onTertiaryContainer
            } else {
                MaterialTheme.colorScheme.onTertiary
            },
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        when {
            isSaving -> CircularProgressIndicator(
                modifier = Modifier.fillMaxHeight(SAVING_INDICATOR_HEIGHT_FRACTION).aspectRatio(1f),
                strokeWidth = SAVE_BUTTON_STROKE_WIDTH,
                color = MaterialTheme.colorScheme.onTertiary,
            )
            isInExpressionMode -> Text("=", style = MaterialTheme.typography.headlineSmall)
            else -> Icon(
                imageVector = vectorResource(Res.drawable.ic_check),
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.drawBehind {
            drawRoundRect(
                color = borderColor,
                cornerRadius = CornerRadius(CORNER_RADIUS.toPx()),
                style = Stroke(width = 1.dp.toPx()),
            )
        },
        enabled = enabled,
        shape = RoundedCornerShape(CORNER_RADIUS),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        content()
    }
}
