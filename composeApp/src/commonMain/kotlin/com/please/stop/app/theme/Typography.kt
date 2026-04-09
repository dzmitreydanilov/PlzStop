package com.please.stop.app.theme

import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.jetbrains.compose.resources.Font
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.nunito_variable

@Composable
internal fun NunitoFamily(): FontFamily = FontFamily(
    Font(Res.font.nunito_variable, FontWeight.Normal),
    Font(Res.font.nunito_variable, FontWeight.Medium),
    Font(Res.font.nunito_variable, FontWeight.SemiBold),
    Font(Res.font.nunito_variable, FontWeight.Bold),
)

// Nunito-based typography scale
// Display: 64sp — primary figures (currency amounts)
// Headlines: 24sp — section headers
// Body & Buttons: 20sp — calculator keys, main body
// Labels: 14sp — meta-information, UI pills
@Composable
internal fun AppTypography(): Typography {
    val nunito = NunitoFamily()
    return Typography(
        displayLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 64.sp,
            lineHeight = 72.sp,
            letterSpacing = (-1).sp,
        ),
        displayMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 44.sp,
            lineHeight = 52.sp,
            letterSpacing = (-0.5).sp,
        ),
        displaySmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 36.sp,
            lineHeight = 44.sp,
        ),
        headlineLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            lineHeight = 32.sp,
        ),
        headlineMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 22.sp,
            lineHeight = 28.sp,
        ),
        headlineSmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.SemiBold,
            fontSize = 20.sp,
            lineHeight = 26.sp,
        ),
        titleMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 18.sp,
            lineHeight = 24.sp,
        ),
        titleSmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodyLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 20.sp,
            lineHeight = 28.sp,
        ),
        bodyMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 22.sp,
        ),
        bodySmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 20.sp,
        ),
        labelLarge = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.5.sp,
        ),
        labelMedium = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.5.sp,
        ),
        labelSmall = TextStyle(
            fontFamily = nunito,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.5.sp,
        ),
    )
}

internal val AppShapes = Shapes()
