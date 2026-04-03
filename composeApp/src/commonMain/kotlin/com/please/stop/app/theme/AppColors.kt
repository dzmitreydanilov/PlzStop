package com.please.stop.app.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class AppColors(
    val teal500: Color = Color(0xFF14B8A6),
    val teal600: Color = Color(0xFF0D9488),
    val cyan500: Color = Color(0xFF06B6D4),
    val blue400: Color = Color(0xFF60A5FA),
    val blue500: Color = Color(0xFF3B82F6),
    val blue600: Color = Color(0xFF2563EB),
    val emerald500: Color = Color(0xFF10B981),
    val emerald600: Color = Color(0xFF059669),
    val rose500: Color = Color(0xFFF43F5E),
    val rose600: Color = Color(0xFFE11D48),
    val amber500: Color = Color(0xFFF59E0B),
    val amber600: Color = Color(0xFFD97706),
    val purple500: Color = Color(0xFF8B5CF6),
    val pink500: Color = Color(0xFFEC4899),
    val slate400: Color = Color(0xFF94A3B8),
    val slate500: Color = Color(0xFF64748B),
    val slate600: Color = Color(0xFF475569),
    val slate900: Color = Color(0xFF0F172A),
    val cardGlass: Color = Color(0xCCFFFFFF),
    val cardGlassBorder: Color = Color(0x33FFFFFF),
) {
    val primaryGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF7CB5FF), blue500, Color(0xFF1F5FD9)))

    val headerGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF4A8FFF), Color(0xFF2E73F0), Color(0xFF1756CC)))

    val tealCyanGradient: Brush
        get() = Brush.linearGradient(listOf(teal500, cyan500))

    val successGradient: Brush
        get() = Brush.linearGradient(listOf(teal500, emerald500))

    val categoryGradients: List<Brush>
        get() = listOf(
            Brush.linearGradient(listOf(Color(0xFFFF8A65), Color(0xFFEF5350))),
            Brush.linearGradient(listOf(Color(0xFFEC407A), Color(0xFF8B5CF6))),
            Brush.linearGradient(listOf(Color(0xFF42A5F5), Color(0xFF26C6DA))),
            Brush.linearGradient(listOf(Color(0xFF66BB6A), Color(0xFF26A69A))),
            Brush.linearGradient(listOf(Color(0xFFFFA726), Color(0xFFFFEE58))),
            Brush.linearGradient(listOf(Color(0xFF7E57C2), Color(0xFFEC407A))),
        )
}

val LocalAppColors = staticCompositionLocalOf { AppColors() }

object AppColorsDefaults {
    @Composable
    fun colors(): AppColors = LocalAppColors.current
}
