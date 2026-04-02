package com.please.stop.app.uicomponents.icons

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.vectorResource
import plzstop.composeapp.generated.resources.Res
import plzstop.composeapp.generated.resources.compose_multiplatform

@Composable
fun AppLogoSmallIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector = vectorResource(Res.drawable.compose_multiplatform),
    tint: Color = MaterialTheme.colorScheme.primary
) {
    Icon(
        modifier = modifier.size(44.dp),
        imageVector = imageVector,
        contentDescription = null,
        tint = tint
    )
}

@Preview
@PreviewScreenSizes
@Composable
private fun AppLogoSmallIconPreview() {
    com.please.stop.app.uicomponents.icons.AppLogoSmallIcon()
}
