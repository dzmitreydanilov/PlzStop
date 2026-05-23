package com.please.stop.app.features.settings.presentation

import com.please.stop.app.core.StateHolder
import com.please.stop.app.core.models.domain.ErrorType
import com.please.stop.app.core.models.domain.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class SettingsStateHolder(
    private val sectionsProvider: SettingsSectionsProvider,
) : StateHolder<SettingsState, SettingsEvent>() {

    override val tag = "SettingsStateHolder"

    override fun getInitial(): SettingsState = SettingsState.Loaded(
        sections = sectionsProvider.getSections(),
    )

    override fun resolveEventResult(event: SettingsEvent): Flow<Result> = when (event) {
        SettingsEvent.DismissError -> flowOf(DismissErrorResult)
    }

    override fun getStateByResult(previous: SettingsState, result: Result): SettingsState =
        when (result) {
            DismissErrorResult -> SettingsState.Loaded(sections = previous.sections)
            else -> super.getStateByResult(previous, result)
        }

    override fun getErrorStateByResult(result: Result, errorType: ErrorType): SettingsState =
        SettingsState.Error(errorType = errorType, sections = state.value.sections)
}

private data object DismissErrorResult : Result
