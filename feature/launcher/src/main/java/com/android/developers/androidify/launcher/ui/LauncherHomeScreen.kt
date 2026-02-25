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

import android.annotation.SuppressLint
import android.app.StatusBarManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import android.os.Process
import com.android.developers.androidify.launcher.LauncherViewModel
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherLayoutType
import com.android.developers.androidify.launcher.platform.MomentumBridge
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.launch

/** Two settled positions for the app-drawer sheet. */
enum class DrawerValue { Collapsed, Expanded }

private enum class LauncherFace { Ai, Home, Social }

private enum class CubeDirection { Left, Right }

/**
 * Root composable for the launcher home screen. Chooses between phone and foldable
 * layouts based on [layoutType], and manages the swipe-up app-drawer overlay.
 *
 * The drawer is driven by [AnchoredDraggableState] so it follows the finger 1:1
 * during a drag and snaps to Collapsed/Expanded with spring physics on release.
 *
 * Predictive back gestures trigger a 3D cube rotation between Home, AI Hub, and
 * Social Hub faces. Swipe-up from AI/Social views triggers an arc-minimize animation
 * back to the home screen, converging toward the search bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
    layoutType: LauncherLayoutType = LauncherLayoutType.Phone,
    foldingFeature: FoldingFeature? = null,
    viewModel: LauncherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    var currentFace by remember { mutableStateOf(LauncherFace.Home) }
    var cubeDirection by remember { mutableStateOf(CubeDirection.Left) }
    var backProgress by remember { mutableFloatStateOf(0f) }
    var committedToEdge by remember { mutableStateOf(false) }
    val cubeProgress = remember { Animatable(0f) }

    // Swipe-up-to-home arc animation state
    val arcProgress = remember { Animatable(0f) }
    var isArcAnimating by remember { mutableStateOf(false) }

    LaunchedEffect(backProgress) {
        cubeProgress.snapTo(backProgress)
    }

    val drawerState = remember {
        AnchoredDraggableState(
            initialValue = DrawerValue.Collapsed,
        )
    }

    // Dock expand state: collapses when on side face, re-expands when returning to Home
    val dockExpanded = currentFace == LauncherFace.Home && !isArcAnimating

    PredictiveBackHandler(enabled = drawerState.currentValue == DrawerValue.Collapsed) { backEvents ->
        committedToEdge = false
        try {
            backEvents.collect { event ->
                cubeDirection = if (event.swipeEdge == BackEventCompat.EDGE_LEFT) {
                    CubeDirection.Left
                } else {
                    CubeDirection.Right
                }
                val progress = event.progress.coerceIn(0f, 1f)
                backProgress = progress
                if (!committedToEdge && progress >= 0.92f) {
                    committedToEdge = true
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }
            val targetFace = if (cubeDirection == CubeDirection.Left) {
                LauncherFace.Ai
            } else {
                LauncherFace.Social
            }
            // Commit: snap to target face with spring overshoot
            cubeProgress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = 0.75f,
                    stiffness = Spring.StiffnessMedium,
                ),
            )
            currentFace = targetFace
            cubeProgress.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.8f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
            backProgress = 0f
        } catch (_: java.util.concurrent.CancellationException) {
            backProgress = 0f
            cubeProgress.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 300
                    0.15f at 80
                    0.04f at 180
                    0f at 300
                },
            )
        }
    }

    // Refresh recent tasks whenever the drawer settles fully open
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Expanded) viewModel.refreshRecentTasks()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val screenWidthPx = with(density) { maxWidth.toPx() }

        // Update anchors every time the resolved screen height changes
        SideEffect {
            drawerState.updateAnchors(
                DraggableAnchors {
                    DrawerValue.Collapsed at screenHeightPx
                    DrawerValue.Expanded at 0f
                },
            )
        }

        // Compute drawer open progress (0 = collapsed, 1 = fully expanded)
        val drawerOffset = drawerState.offset
        val drawerProgress = if (drawerOffset.isNaN() || screenHeightPx == 0f) {
            0f
        } else {
            (1f - (drawerOffset / screenHeightPx)).coerceIn(0f, 1f)
        }
        // Blur radius scales with drawer open progress (max 30dp when fully open)
        val blurRadius = (drawerProgress * 30f).dp

        WallpaperBackground(
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.5.dp) Modifier.blur(blurRadius) else Modifier),
        )

        // Arc animation overlay: when swiping up from a side face, the current
        // face scales down and follows a parabolic arc toward the search bar area
        val arcP = arcProgress.value
        val arcOffsetX = if (currentFace == LauncherFace.Ai) {
            // Arc from left side toward center
            (-screenWidthPx * 0.3f * sin(arcP * Math.PI.toFloat())).roundToInt()
        } else {
            // Arc from right side toward center
            (screenWidthPx * 0.3f * sin(arcP * Math.PI.toFloat())).roundToInt()
        }
        val arcOffsetY = (screenHeightPx * 0.7f * arcP).roundToInt()
        val arcScale = 1f - arcP * 0.85f
        val arcAlpha = (1f - arcP * 1.2f).coerceIn(0f, 1f)

        LauncherCubePredictiveBackScene(
            progress = cubeProgress.value,
            cubeDirection = cubeDirection,
            currentFace = currentFace,
            arcProgress = arcP,
            arcOffsetX = arcOffsetX,
            arcOffsetY = arcOffsetY,
            arcScale = arcScale,
            arcAlpha = arcAlpha,
            modifier = Modifier
                .fillMaxSize()
                .then(if (blurRadius > 0.5.dp) Modifier.blur(blurRadius) else Modifier),
            onSwipeUpFromSideFace = {
                if (currentFace != LauncherFace.Home && !isArcAnimating) {
                    scope.launch {
                        isArcAnimating = true
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        arcProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = spring(
                                dampingRatio = 0.82f,
                                stiffness = Spring.StiffnessMediumLow,
                            ),
                        )
                        currentFace = LauncherFace.Home
                        arcProgress.snapTo(0f)
                        isArcAnimating = false
                    }
                }
            },
            homeContent = {
                when (layoutType) {
                    LauncherLayoutType.Phone -> PhoneLauncherLayout(
                        uiState = uiState,
                        drawerState = drawerState,
                        dockExpanded = dockExpanded,
                        viewModel = viewModel,
                        onAppClick = { app ->
                            viewModel.launchApp(app)
                            scope.launch { drawerState.animateTo(DrawerValue.Collapsed) }
                        },
                        onSearch = viewModel::launchSearch,
                        onVoiceSearch = viewModel::launchVoiceSearch,
                        onLensSearch = viewModel::launchLensSearch,
                    )

                    LauncherLayoutType.Foldable -> FoldableLauncherLayout(
                        uiState = uiState,
                        foldingFeature = foldingFeature,
                        drawerState = drawerState,
                        onAppClick = { app ->
                            viewModel.launchApp(app)
                            scope.launch { drawerState.animateTo(DrawerValue.Collapsed) }
                        },
                        onSearch = viewModel::launchSearch,
                        onVoiceSearch = viewModel::launchVoiceSearch,
                        onLensSearch = viewModel::launchLensSearch,
                        onRecentFileClick = viewModel::openRecentFile,
                    )
                }
            },
        )

        // App drawer — always in composition, GPU-translated off-screen when collapsed
        AppDrawer(
            uiState = uiState,
            layoutType = layoutType,
            drawerState = drawerState,
            screenHeightPx = screenHeightPx,
            onDismiss = { scope.launch { drawerState.animateTo(DrawerValue.Collapsed) } },
            onAppClick = { app ->
                viewModel.launchApp(app)
                scope.launch { drawerState.animateTo(DrawerValue.Collapsed) }
            },
            onTaskClick = { task ->
                viewModel.launchApp(
                    AppInfo(
                        packageName = task.packageName,
                        className = "",
                        label = task.label,
                        icon = task.icon,
                        launchIntent = null,
                        user = Process.myUserHandle(),
                    ),
                )
                scope.launch { drawerState.animateTo(DrawerValue.Collapsed) }
            },
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSearchSubmit = { query ->
                viewModel.launchSearch(query)
                scope.launch { drawerState.animateTo(DrawerValue.Collapsed) }
            },
        )
    }
}

/**
 * 3D prism scene: three faces arranged as \_/ viewed from above.
 *
 *   Face A (AI)  |rail|  Face B (Home)  |rail|  Face C (Social)
 *
 * During predictive-back:
 *  - Left edge drag → counter-clockwise rotation → AI face rotates into view
 *  - Right edge drag → clockwise rotation → Social face rotates into view
 *
 * Between faces, horizontal info rails show user-configurable data (weather,
 * notifications, etc.). Rails are only visible while dragging.
 */
@Composable
private fun LauncherCubePredictiveBackScene(
    progress: Float,
    cubeDirection: CubeDirection,
    currentFace: LauncherFace,
    arcProgress: Float,
    arcOffsetX: Int,
    arcOffsetY: Int,
    arcScale: Float,
    arcAlpha: Float,
    homeContent: @Composable () -> Unit,
    onSwipeUpFromSideFace: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val easedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = 0.78f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "cubeProgress",
    )
    // Left edge swipe → directionSign = 1 → counter-clockwise → AI from left
    val directionSign = if (cubeDirection == CubeDirection.Left) 1f else -1f
    val rotation = easedProgress * 90f * directionSign

    // Depth scaling during rotation — subtle shrink for perspective
    val depthScale = 1f - easedProgress * 0.04f

    Box(
        modifier = modifier.graphicsLayer {
            cameraDistance = 24f * density
            scaleX = depthScale
            scaleY = depthScale
        },
    ) {
        // ── Home face (B) ───────────────────────────────────────────────
        CubeFace(
            rotationY = -rotation,
            transformOrigin = TransformOrigin(
                if (directionSign > 0f) 0f else 1f, 0.5f,
            ),
            visibility = 1f - easedProgress * 0.08f,
            content = homeContent,
        )

        // ── AI Hub face (A) — left ─────────────────────────────────────
        val aiIsVisible = (directionSign > 0f && easedProgress > 0.01f) ||
            (currentFace == LauncherFace.Ai && arcProgress > 0f)
        if (aiIsVisible) {
            val aiModifier = if (currentFace == LauncherFace.Ai && arcProgress > 0f) {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = arcScale
                        this.scaleY = arcScale
                        this.alpha = arcAlpha
                        this.translationX = arcOffsetX.toFloat()
                        this.translationY = arcOffsetY.toFloat()
                        shadowElevation = 24f * (1f - arcProgress)
                        shape = RoundedCornerShape((arcProgress * 32).dp)
                        clip = true
                    }
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = aiModifier) {
                CubeFace(
                    rotationY = 90f - rotation,
                    transformOrigin = TransformOrigin(1f, 0.5f),
                    visibility = easedProgress,
                    content = {
                        SwipeUpContainer(
                            enabled = currentFace == LauncherFace.Ai,
                            onSwipeUp = onSwipeUpFromSideFace,
                        ) {
                            AiHubMockScreen(selected = currentFace == LauncherFace.Ai)
                        }
                    },
                )
            }
        }

        // ── Social Hub face (C) — right ────────────────────────────────
        val socialIsVisible = (directionSign < 0f && easedProgress > 0.01f) ||
            (currentFace == LauncherFace.Social && arcProgress > 0f)
        if (socialIsVisible) {
            val socialModifier = if (currentFace == LauncherFace.Social && arcProgress > 0f) {
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        this.scaleX = arcScale
                        this.scaleY = arcScale
                        this.alpha = arcAlpha
                        this.translationX = arcOffsetX.toFloat()
                        this.translationY = arcOffsetY.toFloat()
                        shadowElevation = 24f * (1f - arcProgress)
                        shape = RoundedCornerShape((arcProgress * 32).dp)
                        clip = true
                    }
            } else {
                Modifier.fillMaxSize()
            }

            Box(modifier = socialModifier) {
                CubeFace(
                    rotationY = -90f - rotation,
                    transformOrigin = TransformOrigin(0f, 0.5f),
                    visibility = easedProgress,
                    content = {
                        SwipeUpContainer(
                            enabled = currentFace == LauncherFace.Social,
                            onSwipeUp = onSwipeUpFromSideFace,
                        ) {
                            SocialHubMockScreen(selected = currentFace == LauncherFace.Social)
                        }
                    },
                )
            }
        }

        // ── Info rails between faces ───────────────────────────────────
        // Only visible while actively dragging (easedProgress > 0).
        // Left rail (between AI ↔ Home) and right rail (Home ↔ Social).
        if (easedProgress > 0.02f) {
            InfoRail(
                side = CubeDirection.Left,
                progress = easedProgress,
                rotation = rotation,
                label = "WEATHER: 49\u00B0",
            )
            InfoRail(
                side = CubeDirection.Right,
                progress = easedProgress,
                rotation = rotation,
                label = "NOTIFICATIONS: 3",
            )
        }
    }
}

/**
 * Container that detects upward swipe gestures and triggers [onSwipeUp].
 * Used on AI and Social hub faces to enable swipe-up-to-home navigation.
 */
@Composable
private fun SwipeUpContainer(
    enabled: Boolean,
    onSwipeUp: () -> Unit,
    content: @Composable () -> Unit,
) {
    var cumulativeDrag by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectVerticalDragGestures(
                            onDragStart = { cumulativeDrag = 0f },
                            onDragEnd = {
                                if (cumulativeDrag < -200f) {
                                    onSwipeUp()
                                }
                                cumulativeDrag = 0f
                            },
                            onDragCancel = { cumulativeDrag = 0f },
                            onVerticalDrag = { change, dragAmount ->
                                cumulativeDrag += dragAmount
                                if (dragAmount < 0f) {
                                    change.consume()
                                }
                            },
                        )
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        content()
    }
}

/**
 * A single face of the 3D prism. Rendered via [graphicsLayer] rotationY
 * with the specified [transformOrigin] as the hinge point.
 */
@Composable
private fun CubeFace(
    rotationY: Float,
    transformOrigin: TransformOrigin,
    visibility: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationY = rotationY
                this.transformOrigin = transformOrigin
                alpha = visibility
            },
    ) {
        content()
    }
}

/**
 * Horizontal info rail visible at the edge between two prism faces.
 *
 * The user described these as the "hinge" between faces:
 *   A_|x|_B_|x|_C   where x shows configurable text like "WEATHER: 49"
 *
 * The rail is a narrow vertical strip at the seam of the cube edge, containing
 * a horizontal text label. It fades in during drag and fades out on release.
 * Only visible when [progress] > 0 (i.e. user is actively dragging).
 */
@Composable
private fun InfoRail(
    side: CubeDirection,
    progress: Float,
    rotation: Float,
    label: String,
) {
    // Position the rail at the edge between the two faces
    val baseFraction = if (side == CubeDirection.Left) 0.02f else 0.98f
    val railAlpha = (progress * 2.5f).coerceIn(0f, 0.9f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                // Track rotation so the rail stays at the seam
                translationX = size.width * (baseFraction - 0.5f - (rotation / 90f * 0.5f))
                alpha = railAlpha
            },
        contentAlignment = Alignment.Center,
    ) {
        // Thin vertical strip with text
        Column(
            modifier = Modifier
                .height(160.dp)
                .padding(horizontal = 2.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Render each character vertically for a spine-like effect
            label.forEach { ch ->
                Text(
                    text = ch.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun AiHubMockScreen(selected: Boolean) {
    MockSideHubScreen(
        title = "AI Hub",
        subtitle = "Previous, ongoing, and aggregated AI chats",
        chips = listOf("New Android Function", "Gemini Sessions", "Pinned Prompts"),
        selected = selected,
    )
}

@Composable
private fun SocialHubMockScreen(selected: Boolean) {
    SocialHubScreen(selected = selected)
}

@Composable
private fun MockSideHubScreen(
    title: String,
    subtitle: String,
    chips: List<String>,
    selected: Boolean,
) {
    // Entrance animation when this screen becomes selected
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "hubContentAlpha",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = 0.7f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "hubContentScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B2838),
                        Color(0xFF0D1B2A),
                    ),
                ),
            )
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = contentScale
                scaleY = contentScale
            }
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Swipe indicator at top
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp)
                .height(4.dp)
                .fillMaxWidth(0.12f)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.4f)),
        )

        Text(title, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            chips.forEach { chip ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) Color(0xFF4A80F5) else Color.White.copy(alpha = 0.12f),
                    ),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text(
                        text = chip,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
        }
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.2f),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = 0.85f },
            ) {
                WallpaperBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.45f },
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = 0.8f }
                        .alpha(1f),
                )
            }
        }

        Spacer(Modifier.weight(1f))

        // Swipe-up hint at bottom
        Text(
            text = "Swipe up to go home",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/**
 * Phone home screen: app icon grid + Pixel-style bottom bar (dock + search widget).
 * Swipe up anywhere opens the app drawer via [drawerState].
 */
@Composable
private fun PhoneLauncherLayout(
    uiState: com.android.developers.androidify.launcher.LauncherUiState,
    drawerState: AnchoredDraggableState<DrawerValue>,
    dockExpanded: Boolean,
    viewModel: LauncherViewModel,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // Fade-in for app icons after wallpaper loads
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentAlpha",
    )

    // Subtle entry animation — content fades up gently on resume.
    // The old "velocity buffer" (800px snap + -3000px/s) was too aggressive and
    // created a visible disconnect. This uses a short, smooth spring instead.
    val timestamp = MomentumBridge.triggerTime.longValue
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(timestamp) {
        if (timestamp > 0) {
            translationY.snapTo(120f)
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 400f,
                ),
            )
        }
    }

    val haptic = LocalHapticFeedback.current
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                // Swipe down on the home screen expands the notification shade
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 40f) {
                            expandNotificationShade(context)
                        }
                    }
                }
                // anchoredDraggable replaces the old detectVerticalDragGestures threshold.
                // Upward drag opens the drawer; release snaps with spring physics.
                .anchoredDraggable(drawerState, Orientation.Vertical)
                // Long-press to show context menu at the press location
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            contextMenuOffset = offset
                            showContextMenu = true
                        },
                    )
                }
                .alpha(contentAlpha),
        ) {
            // [D] At-a-Glance: velocity-matched contextual widget above the grid
            ContextualAtAGlance(yOffset = translationY.value)

            // Paged grid with gentle scale compression
            PagedHomeGrid(
                apps = uiState.allApps,
                columns = 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .graphicsLayer {
                        this.translationY = translationY.value
                        val scale = 1f - (translationY.value / 5000f)
                        this.scaleX = scale.coerceIn(0.95f, 1f)
                        this.scaleY = scale.coerceIn(0.95f, 1f)
                    },
                onAppClick = onAppClick,
                onLoadShortcuts = viewModel::loadShortcutsForPackage,
                onLaunchShortcut = viewModel::launchShortcut,
                onAppInfo = { app ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
            )

            PixelBottomBar(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                dockApps = uiState.dockApps,
                suggestedApp = uiState.suggestedApp,
                onAppClick = onAppClick,
                onSearch = onSearch,
                onVoiceSearch = onVoiceSearch,
                onLensSearch = onLensSearch,
                onWidgetAction = {},
                expanded = dockExpanded,
                onSearchBarLongPress = {
                    val intent = Intent(context, Class.forName("com.android.developers.androidify.SearchBarCustomizeActivity"))
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(intent)
                },
            )
        }

        // Long-press context menu overlay
        if (showContextMenu) {
            HomeScreenContextMenu(
                pressOffset = contextMenuOffset,
                onDismiss = { showContextMenu = false },
                onWallpaper = { viewModel.launchWallpaperPicker() },
                onWidgets = {
                    // TODO: widget picker
                },
                onAppsList = {
                    scope.launch { drawerState.animateTo(DrawerValue.Expanded) }
                },
                onSettings = {
                    val intent = Intent("android.intent.action.APPLICATION_PREFERENCES").apply {
                        setPackage(context.packageName)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    runCatching { context.startActivity(intent) }
                },
            )
        }
    }
}

/**
 * Foldable inner-display home screen.
 *
 * Wider 5-column grid + expanded search widget with Chrome tabs and recent files.
 */
@Composable
fun FoldableLauncherLayout(
    uiState: com.android.developers.androidify.launcher.LauncherUiState,
    foldingFeature: FoldingFeature? = null,
    drawerState: AnchoredDraggableState<DrawerValue>,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    onRecentFileClick: (com.android.developers.androidify.launcher.data.RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val timestamp = MomentumBridge.triggerTime.longValue
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(timestamp) {
        if (timestamp > 0) {
            translationY.snapTo(120f)
            translationY.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = 400f,
                ),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .anchoredDraggable(drawerState, Orientation.Vertical),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // [D] At-a-Glance for foldable
        ContextualAtAGlance(yOffset = translationY.value)

        AppGrid(
            apps = uiState.allApps,
            columns = 5,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .graphicsLayer {
                    this.translationY = translationY.value
                    val scale = 1f - (translationY.value / 5000f)
                    this.scaleX = scale.coerceIn(0.9f, 1f)
                    this.scaleY = scale.coerceIn(0.9f, 1f)
                },
            onAppClick = onAppClick,
        )

        Spacer(Modifier.height(8.dp))

        FoldableSearchWidget(
            chromeTabs = uiState.chromeTabs,
            recentFiles = uiState.recentFiles,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onLensSearch = onLensSearch,
            onRecentFileClick = onRecentFileClick,
        )
    }
}

/**
 * Expand the system notification shade. Uses EXPAND_STATUS_BAR permission on API < 31,
 * and StatusBarManager.expandNotificationsPanel() on API 31+.
 */
@SuppressLint("WrongConstant")
private fun expandNotificationShade(context: Context) {
    try {
        // Use reflection on the internal StatusBarService — works on all API levels
        // with EXPAND_STATUS_BAR permission. StatusBarManager.expandNotificationsPanel()
        // is only available API 34+ and may require additional permissions.
        @Suppress("DEPRECATION")
        val service = context.getSystemService("statusbar")
        service?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(service)
    } catch (_: Exception) {
        // Permission denied or method not available — silently fail
    }
}
