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
    // Category tile background gradients
    val categoryGradients: ImmutableList<Brush>,
)

val LightAppColors = AppColors(
    onboardingBackground = Color(0xFFF6FAFF),
    onboardingGradientTop = Color(0xFF60A5FA),
    onboardingGradientMid = Color(0xFFEAF3FF),
    cardGlass = Color(0xCCFFFFFF),
    cardGlassBorder = Color(0x33FFFFFF),
    chartColors = persistentListOf(
        Color(0xFF14B8A6), // teal
        Color(0xFF3B82F6), // blue
        Color(0xFF8B5CF6), // purple
        Color(0xFF10B981), // emerald
        Color(0xFFF59E0B), // amber
        Color(0xFFEC4899), // pink
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF4A8FFF), Color(0xFF2E73F0), Color(0xFF1756CC)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFFEF5350))),
        Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFF8B5CF6))),
        Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF26C6DA))),
        Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF26A69A))),
        Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFFFEE58))),
        Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFFEC407A))),
    ),
)

val DarkAppColors = AppColors(
    onboardingBackground = Color(0xFF0F172A),
    onboardingGradientTop = Color(0xFF1E3A5F),
    onboardingGradientMid = Color(0xFF162844),
    cardGlass = Color(0xCC1E293B),
    cardGlassBorder = Color(0x33FFFFFF),
    chartColors = persistentListOf(
        Color(0xFF2DD4BF), // teal
        Color(0xFF60A5FA), // blue
        Color(0xFFA78BFA), // purple
        Color(0xFF34D399), // emerald
        Color(0xFFFBBF24), // amber
        Color(0xFFF472B6), // pink
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF1E3A5F), Color(0xFF1A2E4A), Color(0xFF0F172A)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFFD4603A), Color(0xFFC0392B))),
        Brush.linearGradient(listOf(Color(0xFFBF3060), Color(0xFF7C3AED))),
        Brush.linearGradient(listOf(Color(0xFF2B7DE9), Color(0xFF0EA5E9))),
        Brush.linearGradient(listOf(Color(0xFF22C55E), Color(0xFF0D9488))),
        Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFEAB308))),
        Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFFBF3060))),
    ),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
