/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.developers.androidify.launcher.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.data.AppInfo

// Google brand colours used across the bottom bar
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

/**
 * Full Pixel-Launcher-style bottom chrome:
 *
 * 1. **AI search bar** — small translucent pill with sparkle icon (top)
 * 2. **Dock row** — 3 pinned apps + 1 AI-suggested slot with a pulsing glow ring
 * 3. **Google search bar** — large white pill with the Google G logo, Gemini sparkle,
 *    mic, and lens icons (bottom)
 */
@Composable
fun PixelBottomBar(
    dockApps: List<AppInfo>,
    suggestedApp: AppInfo?,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    onWidgetAction: (WidgetContextAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        // ── 1. AI / web search bar ──────────────────────────────────────────
        AiSearchBar(
            onTap = { onSearch("") },
            modifier = Modifier.fillMaxWidth(),
        )

        // ── 2. Dock ─────────────────────────────────────────────────────────
        DockRow(
            dockApps = dockApps,
            suggestedApp = suggestedApp,
            onAppClick = onAppClick,
            modifier = Modifier.fillMaxWidth(),
        )

        // ── 3. Google search bar ─────────────────────────────────────────────
        GoogleSearchBar(
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onLensSearch = onLensSearch,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// AI search bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Small translucent pill at the top of the bottom bar. Mimics the secondary
 * "web search" bar visible in recent Pixel Launcher builds.
 */
@Composable
private fun AiSearchBar(
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(Color.White.copy(alpha = 0.14f))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.size(18.dp),
        )

        Spacer(Modifier.width(10.dp))

        Text(
            text = "Ask anything",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.75f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Icon(
            imageVector = Icons.Outlined.Cast,
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.6f),
            modifier = Modifier.size(18.dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dock row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DockRow(
    dockApps: List<AppInfo>,
    suggestedApp: AppInfo?,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Regular pinned dock apps (up to 3)
        dockApps.forEach { app ->
            DockIconItem(
                app = app,
                onClick = { onAppClick(app) },
            )
        }

        // Fill remaining dock slots so the AI slot always lands in position 4
        repeat((3 - dockApps.size).coerceAtLeast(0)) {
            Spacer(Modifier.size(DOCK_ICON_SIZE))
        }

        // AI-suggested / mystery slot
        AiPickSlot(
            app = suggestedApp,
            onClick = { suggestedApp?.let(onAppClick) },
        )
    }
}

private val DOCK_ICON_SIZE = 56.dp

/**
 * Dock-sized app icon: just the icon + label, no aspect-ratio inflation.
 * Uses the same spring-press scale as [AppIconItem] for consistency.
 */
@Composable
private fun DockIconItem(
    app: AppInfo,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "dockIconScale",
    )

    Column(
        modifier = Modifier
            .size(DOCK_ICON_SIZE)
            .scale(scale)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppIconImage(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(14.dp)),
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The fourth dock slot. Shows the suggested [app] icon inside a pulsing blue
 * glow ring, or a "?" mystery placeholder when no suggestion is available.
 */
@Composable
private fun AiPickSlot(
    app: AppInfo?,
    onClick: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aiPickGlow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val glowRadius by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowRadius",
    )
    // Pre-compute so drawBehind can read it without shadowing DrawScope.size
    val slotPx = with(androidx.compose.ui.platform.LocalDensity.current) {
        DOCK_ICON_SIZE.toPx()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(DOCK_ICON_SIZE)
            .drawBehind {
                // Pulsing radial glow ring
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            GoogleBlue.copy(alpha = glowAlpha),
                            GoogleBlue.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent,
                        ),
                        radius = slotPx * glowRadius,
                    ),
                    radius = slotPx * glowRadius,
                )
            }
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = if (app != null) {
                    "AI suggested: ${app.label}"
                } else {
                    "AI app pick — no suggestion yet"
                }
                role = Role.Button
            },
    ) {
        if (app != null) {
            DockIconItem(app = app, onClick = onClick)
        } else {
            // Mystery placeholder: dark circle + "?" emoji
            Box(
                modifier = Modifier
                    .size(DOCK_ICON_SIZE - 8.dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFF1A1A3E),
                                Color(0xFF0D0D24),
                            ),
                        ),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.titleLarge,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Google search bar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Large Pixel-Launcher-style Google search bar. White pill with:
 * - Left: Google "G" logo drawn in the four brand colours
 * - Centre: "Search" hint text
 * - Right: Gemini sparkle · mic · lens icons
 */
@Composable
private fun GoogleSearchBar(
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(56.dp)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onSearch("") })
            }
            .semantics {
                contentDescription = "Google Search"
                role = Role.Button
            },
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.93f),
        shadowElevation = 4.dp,
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Google "G" logo
            GoogleLogoG(modifier = Modifier.size(32.dp))

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Search",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5F6368), // Google search hint grey
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )

            // Gemini sparkle
            IconButton(
                onClick = { onSearch("") },
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = "Ask Gemini",
                    tint = GoogleBlue,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Mic
            IconButton(
                onClick = onVoiceSearch,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Mic,
                    contentDescription = "Voice search",
                    tint = GoogleBlue,
                    modifier = Modifier.size(22.dp),
                )
            }

            // Lens (camera-style lens icon drawn inline)
            IconButton(
                onClick = onLensSearch,
                modifier = Modifier.size(40.dp),
            ) {
                LensIcon(
                    tint = GoogleBlue,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Google "G" logo — four-colour arc ring with crossbar
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Renders the Google "G" logo using Canvas arcs and a horizontal crossbar.
 *
 * The arc is split into four segments using approximate angle ranges that
 * match the Google brand guidelines. The crossbar extends from centre to
 * the opening of the arc (right side), matching the iconic capital G shape.
 */
@Composable
private fun GoogleLogoG(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val sw = size.width * 0.22f          // stroke width
        val hs = sw / 2f
        val rect = Size(size.width - sw, size.height - sw)
        val topLeft = Offset(hs, hs)

        // Arc segments (clockwise from top-right gap)
        // Blue  — top through left to bottom-right (~255°)
        drawArc(
            color = GoogleBlue,
            startAngle = -220f,
            sweepAngle = 255f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        // Red   — top-right opening (~35°)
        drawArc(
            color = GoogleRed,
            startAngle = 35f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        // Yellow — right side (~30°)
        drawArc(
            color = GoogleYellow,
            startAngle = 75f,
            sweepAngle = 35f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )
        // Green — bottom-right (~40°)
        drawArc(
            color = GoogleGreen,
            startAngle = 110f,
            sweepAngle = 30f,
            useCenter = false,
            topLeft = topLeft,
            size = rect,
            style = Stroke(width = sw, cap = StrokeCap.Butt),
        )

        // Crossbar: centre → right edge of the opening
        val cx = size.width / 2f
        val cy = size.height / 2f
        drawLine(
            color = GoogleBlue,
            start = Offset(cx, cy),
            end = Offset(size.width - hs, cy),
            strokeWidth = sw,
            cap = StrokeCap.Round,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Google Lens icon (simplified camera-lens shape)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Simplified Google Lens icon: a rounded square outline with a small circle
 * in the centre — recognisable without needing the actual vector asset.
 */
@Composable
private fun LensIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val stroke = size.width * 0.12f
        val cornerR = size.width * 0.25f
        val padding = stroke / 2f

        // Outer rounded square
        drawRoundRect(
            color = tint,
            topLeft = Offset(padding, padding),
            size = Size(size.width - stroke, size.height - stroke),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerR),
            style = Stroke(width = stroke),
        )

        // Inner focus circle
        drawCircle(
            color = tint,
            radius = size.width * 0.2f,
            style = Stroke(width = stroke * 0.8f),
        )
    }
}
