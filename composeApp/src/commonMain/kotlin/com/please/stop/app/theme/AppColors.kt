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
    // Primary gradient for action accents
    val primaryGradient: Brush,
    // Success-state accent gradient
    val successGradient: Brush,
    // Warning-state accent gradient
    val warningGradient: Brush,
    // Mesh-like screen background gradient
    val meshBackground: Brush,
    // Glass card gradient fill
    val glassCardGradient: Brush,
    // Category tile background gradients
    val categoryGradients: ImmutableList<Brush>,
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

// ── Light theme ──
val LightAppColors = AppColors(
    onboardingBackground = Color(0xFFF5F9FF),
    onboardingGradientTop = Color(0xFF60A5FA),
    onboardingGradientMid = Color(0xFFEFF6FF),
    cardGlass = Color(0x66FFFFFF),
    cardGlassBorder = Color(0x99FFFFFF),
    chartColors = persistentListOf(
        Color(0xFF3B82F6),
        Color(0xFF14B8A6),
        Color(0xFF0EA5E9),
        Color(0xFF6366F1),
        Color(0xFF8B5CF6),
        Color(0xFFF59E0B),
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF60A5FA), Color(0xFF3B82F6), Color(0xFF2563EB)),
    ),
    primaryGradient = Brush.linearGradient(
        listOf(Color(0xFF60A5FA), Color(0xFF3B82F6), Color(0xFF2563EB)),
    ),
    successGradient = Brush.linearGradient(
        listOf(Color(0xFF14B8A6), Color(0xFF0D9488)),
    ),
    warningGradient = Brush.linearGradient(
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
    ),
    meshBackground = Brush.radialGradient(
        colors = listOf(Color(0x263B82F6), Color(0x1260A5FA), Color(0x00FFFFFF)),
    ),
    glassCardGradient = Brush.linearGradient(
        listOf(Color(0xB3FFFFFF), Color(0x66FFFFFF)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF2563EB))),
        Brush.linearGradient(listOf(Color(0xFF14B8A6), Color(0xFF0D9488))),
        Brush.linearGradient(listOf(Color(0xFF0EA5E9), Color(0xFF0284C7))),
        Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF4F46E5))),
        Brush.linearGradient(listOf(Color(0xFF8B5CF6), Color(0xFF7C3AED))),
        Brush.linearGradient(listOf(Color(0xFFF59E0B), Color(0xFFEF4444))),
    ),
    budgetBurnGreen = Color(0xFF14B8A6),
    budgetBurnYellow = Color(0xFFF59E0B),
    budgetBurnRed = Color(0xFFEF4444),
    spendingLine = Color(0xFF3B82F6),
    ghostLine = Color(0xFF94A3B8),
)

// ── Dark theme ──
val DarkAppColors = AppColors(
    onboardingBackground = Color(0xFF0B1220),
    onboardingGradientTop = Color(0xFF1E3A8A),
    onboardingGradientMid = Color(0xFF111827),
    cardGlass = Color(0x33FFFFFF),
    cardGlassBorder = Color(0x4DFFFFFF),
    chartColors = persistentListOf(
        Color(0xFF93C5FD),
        Color(0xFF5EEAD4),
        Color(0xFF7DD3FC),
        Color(0xFFA5B4FC),
        Color(0xFFC4B5FD),
        Color(0xFFFBBF24),
    ),
    headerGradient = Brush.linearGradient(
        listOf(Color(0xFF1E3A8A), Color(0xFF1D4ED8), Color(0xFF1E40AF)),
    ),
    primaryGradient = Brush.linearGradient(
        listOf(Color(0xFF60A5FA), Color(0xFF3B82F6), Color(0xFF2563EB)),
    ),
    successGradient = Brush.linearGradient(
        listOf(Color(0xFF2DD4BF), Color(0xFF14B8A6)),
    ),
    warningGradient = Brush.linearGradient(
        listOf(Color(0xFFFBBF24), Color(0xFFEF4444)),
    ),
    meshBackground = Brush.radialGradient(
        colors = listOf(Color(0x332563EB), Color(0x1A3B82F6), Color(0x00000000)),
    ),
    glassCardGradient = Brush.linearGradient(
        listOf(Color(0x40FFFFFF), Color(0x14FFFFFF)),
    ),
    categoryGradients = persistentListOf(
        Brush.linearGradient(listOf(Color(0xFF60A5FA), Color(0xFF3B82F6))),
        Brush.linearGradient(listOf(Color(0xFF2DD4BF), Color(0xFF14B8A6))),
        Brush.linearGradient(listOf(Color(0xFF38BDF8), Color(0xFF0EA5E9))),
        Brush.linearGradient(listOf(Color(0xFF818CF8), Color(0xFF6366F1))),
        Brush.linearGradient(listOf(Color(0xFFA78BFA), Color(0xFF8B5CF6))),
        Brush.linearGradient(listOf(Color(0xFFFBBF24), Color(0xFFEF4444))),
    ),
    budgetBurnGreen = Color(0xFF2DD4BF),
    budgetBurnYellow = Color(0xFFFBBF24),
    budgetBurnRed = Color(0xFFF87171),
    spendingLine = Color(0xFF93C5FD),
    ghostLine = Color(0xFF64748B),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }
