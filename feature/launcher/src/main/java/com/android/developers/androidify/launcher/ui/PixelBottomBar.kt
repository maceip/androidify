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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.data.AppInfo

// Google brand colours used across the bottom bar
private val GoogleBlue = Color(0xFF4285F4)
private val GoogleRed = Color(0xFFEA4335)
private val GoogleYellow = Color(0xFFFBBC05)
private val GoogleGreen = Color(0xFF34A853)

/**
 * Pixel-Launcher-style bottom chrome consisting of:
 *
 * 1. **Google search bar** — white pill with the Google G logo, Gemini sparkle,
 *    mic, and lens icons
 * 2. **Dock hotseat** — 5 pinned app icons with no labels, sitting on a subtle
 *    translucent background with rounded top corners
 *
 * Layout matches the stock Pixel Launcher bottom area as seen on Pixel 9 devices.
 */
@Composable
fun PixelBottomBar(
    dockApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── 1. Google search bar ─────────────────────────────────────────
        GoogleSearchBar(
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onLensSearch = onLensSearch,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
        )

        // ── 2. Dock hotseat ─────────────────────────────────────────────
        DockHotseat(
            dockApps = dockApps,
            onAppClick = onAppClick,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Dock hotseat
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Stock-Android-style dock / hotseat: 5 app icons with no text labels,
 * sitting on a translucent dark background with rounded top corners.
 *
 * The hotseat is always visible on the home screen and sits below the
 * Google search bar. It gets covered when the app drawer slides up.
 */
@Composable
private fun DockHotseat(
    dockApps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .background(
                color = Color.Black.copy(alpha = 0.22f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            )
            .padding(horizontal = 12.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            dockApps.forEach { app ->
                HotseatIcon(
                    app = app,
                    onClick = { onAppClick(app) },
                )
            }
            // Fill remaining empty slots to keep spacing even
            repeat((DOCK_SLOT_COUNT - dockApps.size).coerceAtLeast(0)) {
                Spacer(Modifier.size(HOTSEAT_ICON_TOUCH_SIZE))
            }
        }
    }
}

private const val DOCK_SLOT_COUNT = 5
private val HOTSEAT_ICON_TOUCH_SIZE = 58.dp
private val HOTSEAT_ICON_SIZE = 52.dp

/**
 * Individual dock icon: just the app icon, no label. Uses the same
 * spring-press scale animation as [AppIconItem] for consistency.
 */
@Composable
private fun HotseatIcon(
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
        label = "hotseatIconScale",
    )

    Box(
        modifier = Modifier
            .size(HOTSEAT_ICON_TOUCH_SIZE)
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
            }
            .semantics {
                contentDescription = app.label
                role = Role.Button
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIconImage(
            drawable = app.icon,
            contentDescription = app.label,
            modifier = Modifier
                .size(HOTSEAT_ICON_SIZE)
                .clip(RoundedCornerShape(14.dp)),
        )
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
                overflow = TextOverflow.Ellipsis,
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

            // Lens
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
            cornerRadius = CornerRadius(cornerR),
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
