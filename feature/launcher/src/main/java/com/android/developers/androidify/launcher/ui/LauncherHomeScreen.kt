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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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

        WallpaperBackground()

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
            modifier = Modifier.fillMaxSize(),
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
 * 3D cube scene with enhanced depth effects. During rotation:
 * - Faces scale down slightly to enhance perspective
 * - Drop shadows appear on face edges
 * - Parallax shift adds depth to content
 * - Notification rails glow and track the rotation
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
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessLow,
        ),
        label = "cubeProgress",
    )
    val directionSign = if (cubeDirection == CubeDirection.Left) 1f else -1f
    val rotation = easedProgress * 92f * directionSign

    // Scale down during rotation for enhanced 3D depth
    val depthScale = 1f - easedProgress * 0.06f
    // Subtle vertical parallax during rotation
    val parallaxShift = easedProgress * 8f

    Box(
        modifier = modifier.graphicsLayer {
            cameraDistance = 28f * density
            scaleX = depthScale
            scaleY = depthScale
        },
    ) {
        // Home face
        CubeFace(
            rotationY = -rotation,
            transformOrigin = TransformOrigin(if (directionSign > 0f) 0f else 1f, 0.5f),
            visibility = 1f - easedProgress * 0.12f,
            scaleEffect = 1f - easedProgress * 0.03f,
            shadowAlpha = easedProgress * 0.4f,
            parallaxY = -parallaxShift,
            content = homeContent,
        )

        // AI Hub face (left)
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
                    scaleEffect = 1f + easedProgress * 0.02f,
                    shadowAlpha = (1f - easedProgress) * 0.3f,
                    parallaxY = parallaxShift,
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

        // Social Hub face (right)
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
                    scaleEffect = 1f + easedProgress * 0.02f,
                    shadowAlpha = (1f - easedProgress) * 0.3f,
                    parallaxY = parallaxShift,
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

        // Enhanced notification rails with glow pulse during rotation
        NotificationRail(
            side = CubeDirection.Left,
            alpha = (0.25f + easedProgress * 0.75f).coerceAtMost(1f),
            offsetProgress = rotation / 90f,
            glowIntensity = easedProgress,
        )
        NotificationRail(
            side = CubeDirection.Right,
            alpha = (0.25f + easedProgress * 0.75f).coerceAtMost(1f),
            offsetProgress = rotation / 90f,
            glowIntensity = easedProgress,
        )
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
 * Individual cube face with enhanced 3D effects:
 * - [scaleEffect] creates depth during rotation
 * - [shadowAlpha] adds edge shadows for realism
 * - [parallaxY] shifts content slightly for a parallax depth feel
 */
@Composable
private fun CubeFace(
    rotationY: Float,
    transformOrigin: TransformOrigin,
    visibility: Float,
    scaleEffect: Float = 1f,
    shadowAlpha: Float = 0f,
    parallaxY: Float = 0f,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.rotationY = rotationY
                this.transformOrigin = transformOrigin
                alpha = visibility
                this.scaleX = scaleEffect
                this.scaleY = scaleEffect
                translationY = parallaxY
            },
    ) {
        content()
        // Edge shadow overlay for depth
        if (shadowAlpha > 0.01f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shadowAlpha }
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.35f),
                            ),
                            startX = 0f,
                            endX = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )
        }
    }
}

/**
 * Glowing notification rail at the cube edge. Tracks the rotation and pulses
 * with [glowIntensity] during the predictive back gesture.
 */
@Composable
private fun NotificationRail(
    side: CubeDirection,
    alpha: Float,
    offsetProgress: Float,
    glowIntensity: Float = 0f,
) {
    val baseFraction = if (side == CubeDirection.Left) 0.06f else 0.94f
    // Glow width expands during active rotation
    val railWidth = 0.012f + glowIntensity * 0.006f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = size.width * (baseFraction - 0.5f - (offsetProgress * 0.5f))
            },
        contentAlignment = Alignment.Center,
    ) {
        // Outer glow (wider, more transparent)
        if (glowIntensity > 0.05f) {
            Box(
                modifier = Modifier
                    .padding(vertical = 24.dp)
                    .height(440.dp)
                    .fillMaxWidth(railWidth * 3f)
                    .graphicsLayer { this.alpha = alpha * glowIntensity * 0.4f }
                    .background(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color(0x60A6C8FF),
                                Color(0x80B8D4FF),
                                Color(0x60A6C8FF),
                                Color.Transparent,
                            ),
                        ),
                        shape = RoundedCornerShape(999.dp),
                    ),
            )
        }
        // Core rail
        Box(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .height(420.dp)
                .fillMaxWidth(railWidth)
                .graphicsLayer {
                    this.alpha = alpha
                    shadowElevation = 18f + glowIntensity * 12f
                }
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0x80FFFFFF),
                            Color(0x60A6C8FF),
                            Color(0x90B8D4FF),
                            Color(0x60A6C8FF),
                            Color(0x80FFFFFF),
                        ),
                    ),
                    shape = RoundedCornerShape(999.dp),
                ),
        )
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
    MockSideHubScreen(
        title = "Social Hub",
        subtitle = "Email, X, TikTok, Instagram, LinkedIn feed aggregator",
        chips = listOf("Unread", "Mentions", "Priority"),
        selected = selected,
    )
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
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Fade-in for app icons after wallpaper loads
    val contentAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "contentAlpha",
    )

    // [A] Velocity Buffer: consume MomentumBridge and drive spring animation
    val velocity = MomentumBridge.initialVelocity.floatValue
    val timestamp = MomentumBridge.triggerTime.longValue
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(timestamp) {
        if (timestamp > 0) {
            translationY.snapTo(800f)
            translationY.animateTo(
                targetValue = 0f,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 150f,
                ),
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            // anchoredDraggable replaces the old detectVerticalDragGestures threshold.
            // Upward drag opens the drawer; release snaps with spring physics.
            .anchoredDraggable(drawerState, Orientation.Vertical)
            .alpha(contentAlpha),
    ) {
        // [D] At-a-Glance: velocity-matched contextual widget above the grid
        ContextualAtAGlance(yOffset = translationY.value)

        // [A] Velocity-driven grid with scale compression
        AppGrid(
            apps = uiState.allApps,
            columns = 4,
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
        )
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
    // [A] Velocity Buffer for foldable layout
    val velocity = MomentumBridge.initialVelocity.floatValue
    val timestamp = MomentumBridge.triggerTime.longValue
    val translationY = remember { Animatable(0f) }

    LaunchedEffect(timestamp) {
        if (timestamp > 0) {
            translationY.snapTo(800f)
            translationY.animateTo(
                targetValue = 0f,
                initialVelocity = velocity,
                animationSpec = spring(
                    dampingRatio = 0.6f,
                    stiffness = 150f,
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
