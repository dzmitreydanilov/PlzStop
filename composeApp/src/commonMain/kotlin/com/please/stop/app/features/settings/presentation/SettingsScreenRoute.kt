package com.please.stop.app.features.settings.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.settings_tab
import plzstop.composeapp.generated.resources.settings_user_manage
import plzstop.composeapp.generated.resources.settings_user_name

private const val SECTION_ANIMATION_DURATION_MS = 400
private const val SECTION_INITIAL_OFFSET_PX = -20f
private const val SECTION_DELAY_BASE_MS = 150
private const val SECTION_DELAY_STEP_MS = 150

@Composable
fun SettingsScreenRoute(
    onNavigateToUser: () -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToSubscriptions: () -> Unit,
    onNavigateToExportData: () -> Unit,
) {
    val stateHolder = koinViewModel<SettingsStateHolder>()
    val state by stateHolder.state.collectAsStateWithLifecycle()

    ScreenOverlayContainer(
        overlay = state.asOverlay,
        onDismiss = { stateHolder.processEvent(SettingsEvent.DismissError) },
    ) {
        SettingsContent(
            state = state,
            onNavigateToUser = onNavigateToUser,
            onItemClick = { item ->
                when (item) {
                    is SettingsItem.Categories -> onNavigateToCategories()
                    is SettingsItem.Subscriptions -> onNavigateToSubscriptions()
                    is SettingsItem.ExportData -> onNavigateToExportData()
                    else -> Unit
                }
            },
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsState,
    onNavigateToUser: () -> Unit,
    onItemClick: (SettingsItem) -> Unit,
) {
    val appColors = LocalAppColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SettingsHeader(
            headerBackground = appColors.headerGradient,
            onUserClick = onNavigateToUser,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.sections.forEachIndexed { index, group ->
                AnimatedSettingsSection(
                    title = stringResource(group.title),
                    delayMillis = SECTION_DELAY_BASE_MS + index * SECTION_DELAY_STEP_MS,
                ) {
                    group.items.forEach { item ->
                        SettingsItemRow(
                            item = item,
                            onClick = { onItemClick(item) },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsHeader(
    headerBackground: androidx.compose.ui.graphics.Brush,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
            .background(headerBackground)
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 24.dp),
    ) {
        Text(
            text = stringResource(Res.string.settings_tab),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.clickable(onClick = onUserClick),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
            shape = RoundedCornerShape(20.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "U",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_user_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.settings_user_manage),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedSettingsSection(
    title: String,
    delayMillis: Int,
    content: @Composable () -> Unit,
) {
    val alpha = remember { Animatable(0f) }
    val offsetX = remember { Animatable(SECTION_INITIAL_OFFSET_PX) }

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        alpha.animateTo(1f, tween(SECTION_ANIMATION_DURATION_MS))
    }
    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        offsetX.animateTo(0f, tween(SECTION_ANIMATION_DURATION_MS))
    }

    Column(
        modifier = Modifier.graphicsLayer {
            this.alpha = alpha.value
            translationX = offsetX.value
        },
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            ),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingsItemRow(
    item: SettingsItem,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = item.emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = stringResource(item.subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
