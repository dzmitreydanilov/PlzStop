package com.please.stop.app.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

internal val BrandTeal = Color(0xFF006B5D)
internal val BrandViolet = Color(0xFF66558E)
internal val BrandCoral = Color(0xFF984A52)

internal val LightColorScheme = lightColorScheme(
    primary = BrandTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF9EF2DD),
    onPrimaryContainer = Color(0xFF00201B),
    secondary = BrandViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = BrandCoral,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDADB),
    onTertiaryContainer = Color(0xFF40000B),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    surface = Color(0xFFF7FAF7),
    onSurface = Color(0xFF181D1B),
    surfaceVariant = Color(0xFFDBE5E0),
    onSurfaceVariant = Color(0xFF404944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5F1),
    surfaceContainer = Color(0xFFEBEFEC),
    surfaceContainerHigh = Color(0xFFE5E9E6),
    surfaceContainerHighest = Color(0xFFDFE3E0),
    outline = Color(0xFF707974),
    outlineVariant = Color(0xFFBFC9C4),
    inverseSurface = Color(0xFF2D312F),
    inverseOnSurface = Color(0xFFEFF1EE),
    inversePrimary = Color(0xFF81D5C1),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81D5C1),
    onPrimary = Color(0xFF00382F),
    primaryContainer = Color(0xFF005045),
    onPrimaryContainer = Color(0xFF9EF2DD),
    secondary = Color(0xFFCBC2DB),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFFFB3B8),
    onTertiary = Color(0xFF5E1120),
    tertiaryContainer = Color(0xFF7B2D37),
    onTertiaryContainer = Color(0xFFFFDADB),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF404944),
    onSurfaceVariant = Color(0xFFBFC9C4),
    surfaceContainerLowest = Color(0xFF0B0F0D),
    surfaceContainerLow = Color(0xFF181C1A),
    surfaceContainer = Color(0xFF1C201E),
    surfaceContainerHigh = Color(0xFF262A28),
    surfaceContainerHighest = Color(0xFF313532),
    outline = Color(0xFF89938E),
    outlineVariant = Color(0xFF404944),
    inverseSurface = Color(0xFFE1E3E0),
    inverseOnSurface = Color(0xFF2D312F),
    inversePrimary = BrandTeal,
)
