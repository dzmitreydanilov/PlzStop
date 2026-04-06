package com.please.stop.app.navigation.tabs

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import com.please.stop.app.features.categories.presentation.ui.CategoriesScreen
import com.please.stop.app.navigation.nav3.Router
import com.please.stop.app.navigation.routes.CategoriesRoute
import com.please.stop.app.navigation.routes.MainBottomTabs
import com.please.stop.app.theme.LocalAppColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.settings_tab

private const val HEADER_ANIMATION_DURATION_MS = 400
private const val HEADER_INITIAL_OFFSET_PX = -20f
private const val SECTION_ANIMATION_DURATION_MS = 400
private const val SECTION_INITIAL_OFFSET_PX = -20f

internal fun EntryProviderScope<NavKey>.settingsTabEntries(router: Router<NavKey>) {
    entry<MainBottomTabs.Settings> {
        SettingsScreen(onNavigateToCategories = { router.push(CategoriesRoute) })
    }
    entry<CategoriesRoute> {
        CategoriesScreen(onGoBack = { router.pop() })
    }
}

@Composable
private fun SettingsScreen(onNavigateToCategories: () -> Unit = {}) {
    val appColors = LocalAppColors.current

    val headerAlpha = remember { Animatable(0f) }
    val headerOffset = remember { Animatable(HEADER_INITIAL_OFFSET_PX) }

    LaunchedEffect(Unit) {
        headerAlpha.animateTo(1f, tween(HEADER_ANIMATION_DURATION_MS))
    }
    LaunchedEffect(Unit) {
        headerOffset.animateTo(0f, tween(HEADER_ANIMATION_DURATION_MS))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                .background(appColors.headerGradient)
                .padding(horizontal = 20.dp)
                .padding(top = 48.dp, bottom = 24.dp)
                .graphicsLayer {
                    alpha = headerAlpha.value
                    translationY = headerOffset.value
                },
        ) {
            Text(
                text = stringResource(Res.string.settings_tab),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Card(
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
                            text = "User",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Manage your account",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AnimatedSettingsSection(title = "General", delayMillis = 150) {
                SettingsItem(emoji = "\uD83D\uDCB1", title = "Currency", subtitle = "Change your default currency")
                SettingsItem(
                    emoji = "\uD83D\uDCC1",
                    title = "Categories",
                    subtitle = "Manage expense categories",
                    onClick = onNavigateToCategories,
                )
                SettingsItem(emoji = "\uD83D\uDCCA", title = "Budget", subtitle = "Set monthly budget limits")
            }

            AnimatedSettingsSection(title = "Preferences", delayMillis = 300) {
                SettingsItem(emoji = "\uD83D\uDD14", title = "Notifications", subtitle = "Budget alerts and reminders")
                SettingsItem(emoji = "\uD83C\uDF19", title = "Appearance", subtitle = "Dark mode and display settings")
                SettingsItem(
                    emoji = "\uD83D\uDCE4",
                    title = "Export Data",
                    subtitle = "Download your financial reports"
                )
            }

            AnimatedSettingsSection(title = "About", delayMillis = 450) {
                SettingsItem(emoji = "\u2753", title = "Help & Support", subtitle = "Get help with the app")
                SettingsItem(emoji = "\uD83D\uDCC4", title = "Privacy Policy", subtitle = "Read our privacy terms")
                SettingsItem(emoji = "\u2139\uFE0F", title = "App Version", subtitle = "1.0.0")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
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
private fun SettingsItem(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {},
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
            Text(text = emoji, style = MaterialTheme.typography.titleMedium)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
