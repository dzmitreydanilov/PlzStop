package com.please.stop.app.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class AppColors(
    // Onboarding screen background
    val onboardingBackground: Color,
    // Onboarding gradient top color
    val onboardingGradientTop: Color,
    // Onboarding gradient middle color
    val onboardingGradientMid: Color,
    // Glass morphism card fill
    val cardGlass: Color,
    // Glass morphism card border
    val cardGlassBorder: Color,
    // Chart / data-visualization palette
    val chartColors: ImmutableList<Color>,
    // Header gradient (splash, home, analytics, settings headers)
    val headerGradient: Brush,
    // Content displayed directly on the branded header
    val headerContent: Color,
    // Translucent card displayed on the branded header
    val headerContainer: Color,
    // Translucent avatar background displayed on the branded header
    val headerAvatarContainer: Color,
    // Budget burn progress: safe zone
    val budgetBurnGreen: Color,
    // Budget burn progress: warning zone
    val budgetBurnYellow: Color,
    // Budget burn progress: over-budget zone
    val budgetBurnRed: Color,
    // Actual-spending line on daily trends chart
    val spendingLine: Color,
    // Budget-pacing ghost line on daily trends chart
    val ghostLine: Color,
)

val LightAppColors = AppColors(
    onboardingBackground = Color(0xFFF7FAF7),
    onboardingGradientTop = Color(0xFF00796B),
    onboardingGradientMid = Color(0xFFDDF5EE),
    cardGlass = Color(0x14000000),
    cardGlassBorder = Color(0x0A000000),
    chartColors = persistentListOf(
        Color(0xFF006B5D),
        Color(0xFF66558E),
        Color(0xFFA33F52),
        Color(0xFF8B6100),
        Color(0xFF006493),
        Color(0xFF854A70),
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF00796B), Color(0xFF005F54), Color(0xFF17433C)),
    ),
    headerContent = LightColorScheme.onPrimary,
    headerContainer = LightColorScheme.onPrimary.copy(alpha = 0.15f),
    headerAvatarContainer = LightColorScheme.onPrimary.copy(alpha = 0.25f),
    budgetBurnGreen = Color(0xFF2E7D32),
    budgetBurnYellow = Color(0xFF9A6700),
    budgetBurnRed = Color(0xFFBA1A1A),
    spendingLine = Color(0xFF006B5D),
    ghostLine = Color(0xFF707974),
)

val DarkAppColors = AppColors(
    onboardingBackground = Color(0xFF101412),
    onboardingGradientTop = Color(0xFF245C52),
    onboardingGradientMid = Color(0xFF183F3A),
    cardGlass = Color(0x0DFFFFFF),
    cardGlassBorder = Color(0x14FFFFFF),
    chartColors = persistentListOf(
        Color(0xFF81D5C1),
        Color(0xFFCBC2DB),
        Color(0xFFFFB3B8),
        Color(0xFFFFD17A),
        Color(0xFF89CDF1),
        Color(0xFFE3B3D1),
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF245C52), Color(0xFF183F3A), Color(0xFF122C29)),
    ),
    headerContent = DarkColorScheme.onPrimaryContainer,
    headerContainer = DarkColorScheme.onPrimaryContainer.copy(alpha = 0.15f),
    headerAvatarContainer = DarkColorScheme.onPrimaryContainer.copy(alpha = 0.25f),
    budgetBurnGreen = Color(0xFF81C784),
    budgetBurnYellow = Color(0xFFFFCA58),
    budgetBurnRed = Color(0xFFFFB4AB),
    spendingLine = Color(0xFF81D5C1),
    ghostLine = Color(0xFF89938E),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
