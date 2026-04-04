package com.please.stop.app.features.home.presentation.ui

import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.please.stop.app.features.home.presentation.HomeEvent
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.home_add_expense
import plzstop.composeapp.generated.resources.ic_add

@Composable
internal fun HomeFab(onEvent: (HomeEvent) -> Unit) {
    FloatingActionButton(
        onClick = { onEvent(HomeEvent.AddExpenseClicked) },
        containerColor = MaterialTheme.colorScheme.secondary,
        contentColor = MaterialTheme.colorScheme.onSecondary,
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_add),
            contentDescription = stringResource(Res.string.home_add_expense),
        )
    }
}
