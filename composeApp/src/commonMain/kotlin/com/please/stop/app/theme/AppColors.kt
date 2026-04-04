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

// ── Light theme ──
val LightAppColors = AppColors(
    onboardingBackground = Color(0xFFFAF8F5),
    onboardingGradientTop = Color(0xFF6B8F71),
    onboardingGradientMid = Color(0xFFEEF3EF),
    cardGlass = Color(0x14000000),
    cardGlassBorder = Color(0x0A000000),
    chartColors = persistentListOf(
        Color(0xFF6B8F71), // sage
        Color(0xFFA07855), // clay
        Color(0xFFC4A265), // wheat
        Color(0xFF7A8FA6), // dusty blue
        Color(0xFF9B8EA6), // lavender
        Color(0xFFB87D6E), // rose
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF6B8F71), Color(0xFF5A7D60), Color(0xFF4A6B50)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFF6B8F71), Color(0xFF5A7D60))),
        Brush.linearGradient(listOf(Color(0xFFA07855), Color(0xFF8B6545))),
        Brush.linearGradient(listOf(Color(0xFFC4A265), Color(0xFFB09050))),
        Brush.linearGradient(listOf(Color(0xFF7A8FA6), Color(0xFF6A7D94))),
        Brush.linearGradient(listOf(Color(0xFF9B8EA6), Color(0xFF887A94))),
        Brush.linearGradient(listOf(Color(0xFFB87D6E), Color(0xFFA06A5C))),
    ),
)

// ── Dark theme ──
val DarkAppColors = AppColors(
    onboardingBackground = Color(0xFF1C1B18),
    onboardingGradientTop = Color(0xFF31302D),
    onboardingGradientMid = Color(0xFF252420),
    cardGlass = Color(0x0DFFFFFF),
    cardGlassBorder = Color(0x14FFFFFF),
    chartColors = persistentListOf(
        Color(0xFF9CC6A1), // sage (light)
        Color(0xFFD4A87A), // clay (light)
        Color(0xFFC4A265), // wheat
        Color(0xFF9BB1C6), // dusty blue (light)
        Color(0xFFB8AAC6), // lavender (light)
        Color(0xFFD49A8C), // rose (light)
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF302F2B), Color(0xFF252420), Color(0xFF1C1B18)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFF9CC6A1), Color(0xFF7AAF80))),
        Brush.linearGradient(listOf(Color(0xFFD4A87A), Color(0xFFBF9060))),
        Brush.linearGradient(listOf(Color(0xFFC4A265), Color(0xFFAE8E50))),
        Brush.linearGradient(listOf(Color(0xFF9BB1C6), Color(0xFF809AB0))),
        Brush.linearGradient(listOf(Color(0xFFB8AAC6), Color(0xFF9F90B0))),
        Brush.linearGradient(listOf(Color(0xFFD49A8C), Color(0xFFBF8070))),
    ),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
