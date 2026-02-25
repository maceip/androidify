package com.android.developers.androidify.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Pixel-style context menu shown when the user long-presses the home screen
 * wallpaper. Appears near the press location, clamped to stay on screen.
 */
@Composable
fun HomeScreenContextMenu(
    pressOffset: Offset = Offset.Zero,
    onDismiss: () -> Unit,
    onWallpaper: () -> Unit,
    onWidgets: () -> Unit,
    onAppsList: () -> Unit,
    onSettings: () -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "menuScale",
    )
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "menuAlpha",
    )

    val density = LocalDensity.current
    // Approximate menu dimensions for clamping
    val menuWidthPx = with(density) { 260.dp.toPx() }
    val menuHeightPx = with(density) { 240.dp.toPx() } // ~4 items × 60dp each

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.08f * alpha))
            .clickable(onClick = onDismiss),
    ) {
        val screenWidthPx = with(density) { maxWidth.toPx() }
        val screenHeightPx = with(density) { maxHeight.toPx() }

        // Position menu centered on the press point, but clamped to screen edges
        val menuX = (pressOffset.x - menuWidthPx / 2f)
            .coerceIn(with(density) { 16.dp.toPx() }, screenWidthPx - menuWidthPx - with(density) { 16.dp.toPx() })
        val menuY = (pressOffset.y - menuHeightPx / 2f)
            .coerceIn(with(density) { 48.dp.toPx() }, screenHeightPx - menuHeightPx - with(density) { 48.dp.toPx() })

        Surface(
            modifier = Modifier
                .offset { IntOffset(menuX.toInt(), menuY.toInt()) }
                .widthIn(max = 260.dp)
                .graphicsLayer {
                    scaleX = 0.8f + 0.2f * scale
                    scaleY = 0.8f + 0.2f * scale
                    this.alpha = alpha
                    // Scale from the press point relative to the menu
                    val pivotX = ((pressOffset.x - menuX) / menuWidthPx).coerceIn(0f, 1f)
                    val pivotY = ((pressOffset.y - menuY) / menuHeightPx).coerceIn(0f, 1f)
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotX, pivotY)
                }
                .clickable(enabled = false, onClick = {}),
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2A2A2E),
            shadowElevation = 16.dp,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                ContextMenuItem(
                    icon = Icons.Outlined.Wallpaper,
                    label = "Wallpaper & style",
                    onClick = {
                        onDismiss()
                        onWallpaper()
                    },
                )
                ContextMenuItem(
                    icon = Icons.Outlined.Widgets,
                    label = "Widgets",
                    onClick = {
                        onDismiss()
                        onWidgets()
                    },
                )
                ContextMenuItem(
                    icon = Icons.Outlined.Apps,
                    label = "Apps list",
                    onClick = {
                        onDismiss()
                        onAppsList()
                    },
                )
                ContextMenuItem(
                    icon = Icons.Outlined.Settings,
                    label = "Home settings",
                    onClick = {
                        onDismiss()
                        onSettings()
                    },
                )
            }
        }
    }
}

@Composable
private fun ContextMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(22.dp),
        )
        Spacer(Modifier.width(14.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.9f),
        )
    }
}
