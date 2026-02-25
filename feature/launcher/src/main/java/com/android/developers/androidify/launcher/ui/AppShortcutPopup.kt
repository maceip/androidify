package com.android.developers.androidify.launcher.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherShortcut

/**
 * Pixel-Launcher-style long-press popup. Shows app shortcuts from the
 * ShortcutManager, plus "App info" at the bottom.
 *
 * Appears as a floating card centered over the screen with a scrim backdrop.
 */
@Composable
fun AppShortcutPopup(
    app: AppInfo,
    onDismiss: () -> Unit,
    onLoadShortcuts: (String, (List<LauncherShortcut>) -> Unit) -> Unit,
    onLaunchShortcut: (LauncherShortcut) -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    var shortcuts by remember { mutableStateOf<List<LauncherShortcut>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }

    LaunchedEffect(app.packageName) {
        onLoadShortcuts(app.packageName) { result ->
            shortcuts = result.take(5)
            loaded = true
        }
    }

    // Entrance animation
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "popupScale",
    )
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "popupAlpha",
    )

    // Full-screen scrim + centered card
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f * alpha))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .graphicsLayer {
                    scaleX = 0.85f + 0.15f * scale
                    scaleY = 0.85f + 0.15f * scale
                    this.alpha = alpha
                }
                .clickable(enabled = false, onClick = {}), // block click-through
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF2A2A2E),
            shadowElevation = 16.dp,
            tonalElevation = 4.dp,
        ) {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                // App header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AppIconImage(
                        drawable = app.icon,
                        contentDescription = app.label,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleSmall,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                // Shortcuts list
                if (shortcuts.isNotEmpty()) {
                    HorizontalDivider(
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    shortcuts.forEach { shortcut ->
                        ShortcutRow(
                            label = shortcut.shortLabel.toString(),
                            onClick = { onLaunchShortcut(shortcut) },
                        )
                    }
                }

                // Bottom actions
                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )

                // App info
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onAppInfo)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "App info",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutRow(
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Small shortcut indicator dot
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Color(0xFF4A80F5)),
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
