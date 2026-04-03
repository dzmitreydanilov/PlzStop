package com.please.stop.app.uicomponents.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import org.jetbrains.compose.resources.stringResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.content_desc_loading

private const val SCRIM_ALPHA = 0.32f
private const val POLYGON_COUNT = 6

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DisplayFullScreenProgress(
    showProgress: Boolean,
    modifier: Modifier = Modifier,
    indicatorSize: Dp = 80.dp,
    scrimBackground: Boolean = false,
    scrimBackgroundColor: Color = MaterialTheme.colorScheme.scrim.copy(SCRIM_ALPHA)
) {
    if (showProgress) {
        val loadingDesc = stringResource(Res.string.content_desc_loading)
        Box(
            modifier = modifier.fillMaxSize()
                .then(
                    if (scrimBackground) {
                        Modifier.background(color = scrimBackgroundColor)
                    } else {
                        Modifier.background(color = Color.Transparent)
                    }
                )
                .zIndex(1f)
                .pointerInput(Unit) {}
        ) {
            LoadingIndicator(
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(indicatorSize)
                    .zIndex(1f)
                    .semantics {
                        contentDescription = loadingDesc
                        liveRegion = LiveRegionMode.Polite
                    },
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.take(
                    POLYGON_COUNT
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
private fun DisplayFullScreenProgressInProgressPreview() {
    DisplayFullScreenProgress(showProgress = true, modifier = Modifier.fillMaxSize())
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun DisplayFullScreenProgressInProgressWithScrimBackgroundPreview() {
    DisplayFullScreenProgress(
        showProgress = true,
        scrimBackground = true,
        modifier = Modifier.fillMaxSize()
    )
}
