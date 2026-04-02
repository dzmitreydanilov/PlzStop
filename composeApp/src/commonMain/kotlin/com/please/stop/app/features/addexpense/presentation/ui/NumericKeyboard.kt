package com.please.stop.app.features.addexpense.presentation.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.addexpense.presentation.NumericKey
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_backspace

private const val KEY_ASPECT_RATIO = 2.4f

@Composable
internal fun NumericKeyboard(
    showDecimal: Boolean,
    onKey: (NumericKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(if (showDecimal) "." else "", "0", "backspace"),
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        rows.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        when (key) {
                            "" -> Box(Modifier.fillMaxWidth().aspectRatio(KEY_ASPECT_RATIO))
                            "backspace" -> KeyButton(
                                onClick = { onKey(NumericKey.Backspace) },
                                modifier = Modifier.fillMaxWidth().aspectRatio(KEY_ASPECT_RATIO),
                            ) {
                                Icon(
                                    imageVector = vectorResource(Res.drawable.ic_backspace),
                                    contentDescription = null,
                                )
                            }
                            "." -> KeyButton(
                                onClick = { onKey(NumericKey.Decimal) },
                                modifier = Modifier.fillMaxWidth().aspectRatio(KEY_ASPECT_RATIO),
                            ) {
                                Text(".", style = MaterialTheme.typography.titleMedium)
                            }
                            else -> KeyButton(
                                onClick = { onKey(NumericKey.Digit(key.toInt())) },
                                modifier = Modifier.fillMaxWidth().aspectRatio(KEY_ASPECT_RATIO),
                            ) {
                                Text(key, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
    ) {
        content()
    }
}
