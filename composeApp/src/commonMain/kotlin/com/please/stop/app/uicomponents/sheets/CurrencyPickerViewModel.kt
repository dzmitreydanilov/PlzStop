package com.please.stop.app.uicomponents.sheets

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.please.stop.app.features.onboarding.domain.model.Currency
import com.please.stop.app.features.onboarding.domain.repository.CurrencyRepository
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val DEFAULT_CURRENCY_USD = "USD"
private const val DEFAULT_CURRENCY_EUR = "EUR"

class CurrencyPickerViewModel(
    private val currencyRepository: CurrencyRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _state = MutableStateFlow(CurrencyPickerUiState())
    val state: StateFlow<CurrencyPickerUiState> = _state.asStateFlow()

    private var allCurrencies: List<Currency> = emptyList()
    private var deviceCurrencyCode: String? = null

    fun init(selectedCurrencyCode: String?, deviceCode: String?) {
        deviceCurrencyCode = deviceCode
        viewModelScope.launch(ioDispatcher) {
            currencyRepository.getAllCurrencies()
                .onSuccess { currencies ->
                    allCurrencies = currencies
                    val selected = selectedCurrencyCode?.let { code ->
                        currencies.find { it.code == code }
                    }
                    _state.update {
                        it.copy(
                            isLoading = false,
                            selectedCurrency = selected,
                        )
                    }
                    applyFilter("")
                }
                .onFailure {
                    _state.update { it.copy(isLoading = false) }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        applyFilter(query)
    }

    private fun applyFilter(query: String) {
        val filtered = if (query.isBlank()) {
            allCurrencies
        } else {
            val q = query.trim().lowercase()
            allCurrencies.filter {
                it.code.lowercase().contains(q) ||
                    it.name.lowercase().contains(q) ||
                    it.symbol.lowercase().contains(q)
            }
        }

        val suggestedCodes = buildSet {
            add(DEFAULT_CURRENCY_USD)
            add(DEFAULT_CURRENCY_EUR)
            deviceCurrencyCode?.let { add(it) }
        }

        val (suggested, other) = filtered.partition { it.code in suggestedCodes }

        _state.update {
            it.copy(
                suggestedCurrencies = suggested.toImmutableList(),
                otherCurrencies = other.toImmutableList(),
            )
        }
    }
}

@Stable
data class CurrencyPickerUiState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val suggestedCurrencies: ImmutableList<Currency> = persistentListOf(),
    val otherCurrencies: ImmutableList<Currency> = persistentListOf(),
    val selectedCurrency: Currency? = null,
)
