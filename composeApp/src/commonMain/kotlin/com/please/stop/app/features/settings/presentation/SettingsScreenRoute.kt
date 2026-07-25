package com.please.stop.app.features.settings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.theme.LocalAppColors
import com.please.stop.app.uicomponents.error.ScreenOverlayContainer
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.settings_tab
import plzstop.composeapp.generated.resources.settings_user_manage
import plzstop.composeapp.generated.resources.settings_user_name

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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        item(contentType = "header") {
            SettingsHeader(
                headerBackground = appColors.headerGradient,
                onUserClick = onNavigateToUser,
            )
        }

        item(contentType = "header-spacing") {
            Spacer(modifier = Modifier.height(16.dp))
        }

        state.sections.forEachIndexed { sectionIndex, group ->
            item(
                key = "section-$sectionIndex",
                contentType = "section-title",
            ) {
                Text(
                    text = stringResource(group.title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        top = if (sectionIndex == 0) 8.dp else 16.dp,
                        end = 20.dp,
                        bottom = 8.dp,
                    ),
                )
            }

            itemsIndexed(
                items = group.items,
                key = { _, item -> item.id },
                contentType = { _, _ -> "settings-item" },
            ) { itemIndex, item ->
                SettingsItemCard(
                    item = item,
                    shape = settingsItemShape(
                        itemIndex = itemIndex,
                        itemCount = group.items.size,
                    ),
                    onClick = { onItemClick(item) },
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }

        item(contentType = "footer-spacing") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SettingsHeader(
    headerBackground: androidx.compose.ui.graphics.Brush,
    onUserClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val appColors = LocalAppColors.current

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
            color = appColors.headerContent,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            modifier = Modifier.clickable(onClick = onUserClick),
            colors = CardDefaults.cardColors(containerColor = appColors.headerContainer),
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
                        .background(appColors.headerAvatarContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "U",
                        style = MaterialTheme.typography.headlineMedium,
                        color = appColors.headerContent,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.settings_user_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = appColors.headerContent,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = stringResource(Res.string.settings_user_manage),
                        style = MaterialTheme.typography.bodySmall,
                        color = appColors.headerContent.copy(alpha = 0.7f),
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsItemCard(
    item: SettingsItem,
    shape: Shape,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        ),
        shape = shape,
    ) {
        SettingsItemRow(item = item, onClick = onClick)
    }
}

private fun settingsItemShape(
    itemIndex: Int,
    itemCount: Int,
): Shape = when {
    itemCount == 1 -> RoundedCornerShape(16.dp)
    itemIndex == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    itemIndex == itemCount - 1 -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
    else -> RectangleShape
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
            Icon(
                imageVector = vectorResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
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
