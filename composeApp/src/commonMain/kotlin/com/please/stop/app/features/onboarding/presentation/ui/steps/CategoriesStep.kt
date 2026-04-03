package com.please.stop.app.features.onboarding.presentation.ui.steps

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.please.stop.app.uicomponents.categoryEmojiForKey
import com.please.stop.app.features.onboarding.presentation.CategoryUiModel
import com.please.stop.app.features.onboarding.presentation.OnboardingEvent
import com.please.stop.app.features.onboarding.presentation.OnboardingState
import com.please.stop.app.features.onboarding.presentation.OnboardingState.Content
import com.please.stop.app.features.onboarding.presentation.OnboardingStep
import com.please.stop.app.uicomponents.GlassTextField
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingCenteredContent
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingPrimaryButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingSecondaryTextButton
import com.please.stop.app.features.onboarding.presentation.ui.components.OnboardingStepIndicator
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_add
import plzstop.composeapp.generated.resources.onboarding_add_custom_category
import plzstop.composeapp.generated.resources.onboarding_back
import plzstop.composeapp.generated.resources.onboarding_cancel
import plzstop.composeapp.generated.resources.onboarding_category_name_label
import plzstop.composeapp.generated.resources.onboarding_min_categories
import plzstop.composeapp.generated.resources.onboarding_new_category
import plzstop.composeapp.generated.resources.onboarding_pick_categories
import kotlin.math.abs

@Composable
fun CategoriesStep(
    state: Content,
    onEvent: (OnboardingEvent) -> Unit,
) {
    var showAddDialog by remember { mutableStateOf(false) }
    val allCategories = state.availableCategories + state.customCategories

    OnboardingCenteredContent(scrollable = false) {
        Column(
            modifier = Modifier.padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_pick_categories),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = stringResource(Res.string.onboarding_min_categories, OnboardingState.MIN_CATEGORIES),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .shadow(elevation = 10.dp, shape = RoundedCornerShape(18.dp), clip = true)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(18.dp)),
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(allCategories, key = { it.id }) { category ->
                        CategoryTile(
                            category = category,
                            isSelected = category.id in state.selectedCategoryIds,
                            onClick = { onEvent(OnboardingEvent.CategoryToggled(category.id)) },
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Scroll to see more categories",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.canAddCustomCategory) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 12.dp),
                ) {
                    OutlinedButton(
                    onClick = { showAddDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(2.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color.Transparent),
                ) {
                    Text(
                        text = stringResource(Res.string.onboarding_add_custom_category),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                }
            }

            Spacer(modifier = Modifier.height(44.dp))
            OnboardingStepIndicator(currentStep = OnboardingStep.CATEGORIES)

            Spacer(modifier = Modifier.height(16.dp))

            OnboardingPrimaryButton(
                currentStep = OnboardingStep.CATEGORIES,
                isEnabled = state.isNextEnabled,
                isSaving = state.isSaving,
                onClick = { onEvent(OnboardingEvent.NextTapped) },
            )

            OnboardingSecondaryTextButton(
                text = stringResource(Res.string.onboarding_back),
                onClick = { onEvent(OnboardingEvent.BackTapped) },
            )
        }
    }

    if (showAddDialog) {
        AddCategoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                onEvent(OnboardingEvent.CustomCategoryAdded(name = name, iconKey = "ic_other"))
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun CategoryTile(
    category: CategoryUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                categoryBackgroundFor(category.id)
            },
        ),
        border = if (!isSelected) BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant,
        ) else null,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        } else {
                            MaterialTheme.colorScheme.secondaryContainer
                        },
                        shape = RoundedCornerShape(12.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = categoryEmojiForKey(category.iconKey),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            Text(
                text = category.name,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun AddCategoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp),
        title = {
            Text(
                text = stringResource(Res.string.onboarding_new_category),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Column {
                GlassTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text(stringResource(Res.string.onboarding_category_name_label)) },
                )
            }
        },
        confirmButton = {
            OutlinedButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty(),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Text(
                    text = stringResource(Res.string.onboarding_add),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(Res.string.onboarding_cancel),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
private fun categoryBackgroundFor(categoryId: Long): Color {
    val variants = listOf(
        Color(0xFFE9F3FF),
        Color(0xFFEAF8FF),
        Color(0xFFF0EDFF),
        Color(0xFFEFFFF5),
        Color(0xFFFFF1E9),
        Color(0xFFFBEFFF),
    )
    return variants[(abs(categoryId) % variants.size).toInt()]
}
