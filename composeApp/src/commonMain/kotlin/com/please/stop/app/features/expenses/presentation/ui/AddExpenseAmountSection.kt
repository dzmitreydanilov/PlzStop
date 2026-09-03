package com.please.stop.app.features.expenses.presentation.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.please.stop.app.features.expenses.presentation.AddExpenseState
import com.please.stop.app.features.expenses.presentation.ConversionState
import com.please.stop.app.features.expenses.presentation.ExpenseFormInput
import com.please.stop.app.uicomponents.sheets.AppModalBottomSheet
import com.please.stop.app.uicomponents.sheets.rememberFullyExpandedAppModalBottomSheetState
import com.please.stop.app.utils.DEFAULT_CURRENCY_DECIMAL_PLACES
import com.please.stop.app.utils.minorUnitsMultiplier
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.conversion_converted_amount
import plzstop.composeapp.generated.resources.conversion_custom_rate
import plzstop.composeapp.generated.resources.conversion_fetching_rate
import plzstop.composeapp.generated.resources.conversion_rate_format
import plzstop.composeapp.generated.resources.conversion_rate_override_apply
import plzstop.composeapp.generated.resources.conversion_rate_override_hint
import plzstop.composeapp.generated.resources.conversion_rate_override_title
import plzstop.composeapp.generated.resources.conversion_rate_unavailable
import plzstop.composeapp.generated.resources.conversion_reset_rate
import plzstop.composeapp.generated.resources.conversion_save_in
import plzstop.composeapp.generated.resources.ic_keyboard_arrow_down

private val AMOUNT_DISPLAY_HEIGHT = 64.dp
private val AMOUNT_ROW_MAX_WIDTH = 340.dp

@Suppress("ModifierHeightWithText")
@Composable
internal fun AmountSection(
    state: AddExpenseState,
    form: ExpenseFormInput,
    onCurrencyClick: () -> Unit,
    onEditRate: () -> Unit,
    onResetRate: () -> Unit,
    onToggleSaveInOriginal: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isEmpty = form.amountDisplayExpression.isEmpty()
    val displayText = if (isEmpty) {
        "0"
    } else {
        form.amountDisplayExpression.withoutCurrencySymbol(state.currency.symbol)
    }
    val currencyText = state.currency.symbol.ifEmpty { state.currency.code }

    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isEmpty) 1f else 1.02f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
        ),
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .height(AMOUNT_DISPLAY_HEIGHT),
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .widthIn(max = AMOUNT_ROW_MAX_WIDTH),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = displayText,
                    style = MaterialTheme.typography.displaySmall,
                    color = if (isEmpty) {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(weight = 1f, fill = false),
                )
                Spacer(modifier = Modifier.width(8.dp))
                CurrencyChip(
                    currencyText = currencyText,
                    onClick = onCurrencyClick,
                )
            }
        }

        ConversionInfo(
            conversion = state.conversion,
            selectedCurrencyCode = state.currency.code,
            onEditRate = onEditRate,
            onResetRate = onResetRate,
            onToggleSaveInOriginal = onToggleSaveInOriginal,
        )
    }
}

@Composable
private fun CurrencyChip(
    currencyText: String,
    onClick: () -> Unit,
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = currencyText,
                style = MaterialTheme.typography.labelMedium,
            )
        },
        trailingIcon = {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_keyboard_arrow_down),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        shape = RoundedCornerShape(20.dp),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            trailingIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

private fun String.withoutCurrencySymbol(currencySymbol: String): String {
    if (currencySymbol.isEmpty()) return this

    return replace(" $currencySymbol", "")
        .replace(currencySymbol, "")
        .trim()
        .ifEmpty { "0" }
}

@Composable
private fun ConversionInfo(
    conversion: ConversionState,
    selectedCurrencyCode: String,
    onEditRate: () -> Unit,
    onResetRate: () -> Unit,
    onToggleSaveInOriginal: () -> Unit,
) {
    val isVisible = (conversion.isLoading || conversion.rate != null || conversion.hasFetchError) &&
        selectedCurrencyCode != conversion.defaultCurrencyCode &&
        selectedCurrencyCode.isNotEmpty() &&
        conversion.defaultCurrencyCode.isNotEmpty()

    if (!isVisible) return

    when {
        conversion.isLoading -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(12.dp),
                    strokeWidth = 1.5.dp,
                )
                Text(
                    text = stringResource(Res.string.conversion_fetching_rate),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        conversion.rate != null -> {
            val rateFormatted = conversion.rate.toString()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(
                        Res.string.conversion_rate_format,
                        selectedCurrencyCode,
                        rateFormatted,
                        conversion.defaultCurrencyCode,
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable(onClick = onEditRate),
                )
                if (conversion.isManualOverride) {
                    Text(
                        text = stringResource(Res.string.conversion_custom_rate),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                    if (conversion.fetchedRate != null) {
                        Text(
                            text = stringResource(Res.string.conversion_reset_rate),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.clickable(onClick = onResetRate),
                        )
                    }
                }
            }
            if (conversion.convertedAmountMinorUnits != null) {
                val convertedDisplay = formatMinorUnitsForDisplay(
                    conversion.convertedAmountMinorUnits,
                )
                Text(
                    text = stringResource(
                        Res.string.conversion_converted_amount,
                        convertedDisplay,
                        conversion.defaultCurrencySymbol,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val saveCurrencyCode = if (conversion.saveInOriginalCurrency) {
                selectedCurrencyCode
            } else {
                conversion.defaultCurrencyCode
            }
            Text(
                text = stringResource(Res.string.conversion_save_in, saveCurrencyCode),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onToggleSaveInOriginal),
            )
        }

        else -> {
            Text(
                text = stringResource(Res.string.conversion_rate_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.clickable(onClick = onEditRate),
            )
        }
    }
}

private fun formatMinorUnitsForDisplay(
    minorUnits: Long,
    decimalPlaces: Int = DEFAULT_CURRENCY_DECIMAL_PLACES,
): String {
    val multiplier = minorUnitsMultiplier(decimalPlaces)
    val intPart = minorUnits / multiplier
    val fracPart = (minorUnits % multiplier).toString().padStart(decimalPlaces, '0')
    return "$intPart.$fracPart"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RateOverrideSheet(
    input: String,
    fromCode: String,
    toCode: String,
    onInputChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberFullyExpandedAppModalBottomSheetState()
    AppModalBottomSheet(
        state = sheetState,
        onDismiss = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                text = stringResource(Res.string.conversion_rate_override_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 16.dp),
            )
            OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                label = {
                    Text(
                        stringResource(
                            Res.string.conversion_rate_override_hint,
                            fromCode,
                            toCode,
                        ),
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onConfirm,
                enabled = input.toDoubleOrNull()?.let { it > 0 } == true,
                modifier = Modifier.align(Alignment.End),
            ) {
                Text(stringResource(Res.string.conversion_rate_override_apply))
            }
        }
    }
}
