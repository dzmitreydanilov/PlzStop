package com.please.stop.app

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.please.stop.app.navigation.RootContent
import com.please.stop.app.presentation.RootState
import com.please.stop.app.presentation.RootStateHolder
import com.please.stop.app.theme.AppTheme
import com.please.stop.app.theme.LocalAppColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.onboarding_app_name
import plzstop.composeapp.generated.resources.onboarding_tagline

@Composable
fun App() {
    AppTheme {
        val rootStateHolder = koinViewModel<RootStateHolder>()
        val state by rootStateHolder.state.collectAsStateWithLifecycle()

        when (val s = state) {
            is RootState.Loading -> SplashScreen()
            is RootState.Ready -> RootContent(initialRoute = s.initialRoute)
        }
    }
}

private const val LOGO_INITIAL_SCALE = 0.5f
private const val LOGO_FADE_DURATION_MS = 400
private const val TEXT_VISIBLE_DELAY_MS = 200L
private const val PROGRESS_VISIBLE_DELAY_MS = 400L
private const val TEXT_FADE_DURATION_MS = 500
private const val PROGRESS_FADE_DURATION_MS = 400
private val LOGO_SIZE = 88.dp
private val LOGO_CORNER_RADIUS = 26.dp
private val LOGO_BOTTOM_SPACER = 28.dp
private val TAGLINE_BOTTOM_SPACER = 8.dp
private val PROGRESS_BOTTOM_SPACER = 48.dp
private val PROGRESS_HEIGHT = 3.dp
private val PROGRESS_CORNER_RADIUS = 2.dp
private const val PROGRESS_WIDTH_FRACTION = 0.4f
private const val LOGO_BG_ALPHA = 0.2f
private const val TAGLINE_ALPHA = 0.8f
private const val PROGRESS_TRACK_ALPHA = 0.2f

@Composable
private fun SplashScreen() {
    val appColors = LocalAppColors.current

    val logoScale = remember { Animatable(LOGO_INITIAL_SCALE) }
    val logoAlpha = remember { Animatable(0f) }
    var textVisible by remember { mutableStateOf(false) }
    var progressVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        logoAlpha.animateTo(1f, tween(LOGO_FADE_DURATION_MS))
    }
    LaunchedEffect(Unit) {
        logoScale.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        )
        delay(TEXT_VISIBLE_DELAY_MS)
        textVisible = true
        delay(PROGRESS_VISIBLE_DELAY_MS)
        progressVisible = true
    }

    val textAlpha by animateFloatAsState(
        targetValue = if (textVisible) 1f else 0f,
        animationSpec = tween(TEXT_FADE_DURATION_MS),
    )
    val progressAlpha by animateFloatAsState(
        targetValue = if (progressVisible) 1f else 0f,
        animationSpec = tween(PROGRESS_FADE_DURATION_MS),
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.headerGradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        alpha = logoAlpha.value
                    }
                    .size(LOGO_SIZE)
                    .clip(RoundedCornerShape(LOGO_CORNER_RADIUS))
                    .background(Color.White.copy(alpha = LOGO_BG_ALPHA)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "\uD83D\uDCB0",
                    style = MaterialTheme.typography.displayMedium,
                )
            }

            Spacer(modifier = Modifier.height(LOGO_BOTTOM_SPACER))

            Text(
                text = stringResource(Res.string.onboarding_app_name),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
            )

            Spacer(modifier = Modifier.height(TAGLINE_BOTTOM_SPACER))

            Text(
                text = stringResource(Res.string.onboarding_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = TAGLINE_ALPHA),
                modifier = Modifier.graphicsLayer { alpha = textAlpha },
            )

            Spacer(modifier = Modifier.height(PROGRESS_BOTTOM_SPACER))

            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth(PROGRESS_WIDTH_FRACTION)
                    .height(PROGRESS_HEIGHT)
                    .clip(RoundedCornerShape(PROGRESS_CORNER_RADIUS))
                    .graphicsLayer { alpha = progressAlpha },
                color = Color.White,
                trackColor = Color.White.copy(alpha = PROGRESS_TRACK_ALPHA),
            )
        }
    }
}
