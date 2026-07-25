package com.please.stop.app.uicomponents.sheets.currency

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composeunstyled.ModalBottomSheetProperties
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.uicomponents.sheets.AnimatedAppModalBottomSheet
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.ic_check
import plzstop.composeapp.generated.resources.ic_close
import plzstop.composeapp.generated.resources.ic_search
import plzstop.composeapp.generated.resources.onboarding_all_currencies
import plzstop.composeapp.generated.resources.onboarding_search_currencies
import plzstop.composeapp.generated.resources.onboarding_selected
import plzstop.composeapp.generated.resources.onboarding_suggested

private const val ICON_SCALE = 0.6f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    selectedCurrencyCode: String?,
    deviceCurrencyCode: String?,
    onCurrencySelected: (Currency) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel = koinViewModel<CurrencyPickerViewModel>()
    val pickerState by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.init(selectedCurrencyCode, deviceCurrencyCode)
    }

    AnimatedAppModalBottomSheet(
        onDismiss = onDismiss,
        properties = ModalBottomSheetProperties(offsetForIme = true),
    ) { _ ->
        SearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = pickerState.searchQuery,
                    onQueryChange = viewModel::onSearchQueryChanged,
                    onSearch = {},
                    expanded = false,
                    onExpandedChange = {},
                    placeholder = {
                        Text(text = stringResource(Res.string.onboarding_search_currencies))
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.ic_search),
                            contentDescription = null,
                        )
                    },
                    trailingIcon = {
                        AnimatedVisibility(
                            visible = pickerState.searchQuery.isNotEmpty(),
                            enter = fadeIn() + scaleIn(initialScale = ICON_SCALE),
                            exit = fadeOut() + scaleOut(targetScale = ICON_SCALE),
                        ) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(
                                    painter = painterResource(Res.drawable.ic_close),
                                    contentDescription = null,
                                )
                            }
                        }
                    },
                )
            },
            expanded = false,
            onExpandedChange = {},
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        ) {}

        Spacer(modifier = Modifier.height(8.dp))

        if (pickerState.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
            ) {
                if (pickerState.suggestedCurrencies.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.onboarding_suggested),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 12.dp, bottom = 8.dp,
                            ),
                        )
                    }
                    items(pickerState.suggestedCurrencies, key = { it.code }) { currency ->
                        CurrencyRow(
                            currency = currency,
                            isSelected = currency == pickerState.selectedCurrency,
                            onClick = { onCurrencySelected(currency) },
                        )
                    }
                }

                if (pickerState.otherCurrencies.isNotEmpty()) {
                    item {
                        Text(
                            text = stringResource(Res.string.onboarding_all_currencies),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = 16.dp, end = 16.dp,
                                top = 12.dp, bottom = 8.dp,
                            ),
                        )
                    }
                    items(pickerState.otherCurrencies, key = { it.code }) { currency ->
                        CurrencyRow(
                            currency = currency,
                            isSelected = currency == pickerState.selectedCurrency,
                            onClick = { onCurrencySelected(currency) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrencyRow(
    currency: Currency,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.background(bgColor)
                else Modifier
            )
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = currency.code,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = currency.symbol,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = currency.name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        if (isSelected) {
            Icon(
                painter = painterResource(Res.drawable.ic_check),
                contentDescription = stringResource(Res.string.onboarding_selected),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
