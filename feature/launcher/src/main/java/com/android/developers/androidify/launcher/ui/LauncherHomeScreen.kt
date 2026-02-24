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
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberSplineBasedDecay
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.android.developers.androidify.launcher.LauncherViewModel
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherLayoutType
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
    val cubeProgress = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(backProgress) {
        cubeProgress.snapTo(backProgress)
    }

    val drawerState = remember {
        AnchoredDraggableState(
            initialValue = DrawerValue.Collapsed,
        )
    }

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
            cubeProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 220),
            )
            currentFace = targetFace
            cubeProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320),
            )
            backProgress = 0f
        } catch (_: java.util.concurrent.CancellationException) {
            backProgress = 0f
            cubeProgress.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 260
                    0.12f at 80
                    0f at 260
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

        LauncherCubePredictiveBackScene(
            progress = cubeProgress.value,
            cubeDirection = cubeDirection,
            currentFace = currentFace,
            modifier = Modifier.fillMaxSize(),
            homeContent = {
                when (layoutType) {
                    LauncherLayoutType.Phone -> PhoneLauncherLayout(
                        uiState = uiState,
                        drawerState = drawerState,
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

@Composable
private fun LauncherCubePredictiveBackScene(
    progress: Float,
    cubeDirection: CubeDirection,
    currentFace: LauncherFace,
    homeContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val easedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "cubeProgress",
    )
    val directionSign = if (cubeDirection == CubeDirection.Left) 1f else -1f
    val rotation = easedProgress * 92f * directionSign

    Box(modifier = modifier.graphicsLayer { cameraDistance = 30f * density }) {
        CubeFace(
            rotationY = -rotation,
            transformOrigin = TransformOrigin(if (directionSign > 0f) 0f else 1f, 0.5f),
            visibility = 1f - easedProgress * 0.15f,
            content = homeContent,
        )
        CubeFace(
            rotationY = 90f - rotation,
            transformOrigin = TransformOrigin(1f, 0.5f),
            visibility = if (directionSign > 0f) easedProgress else 0f,
            content = { AiHubMockScreen(selected = currentFace == LauncherFace.Ai) },
        )
        CubeFace(
            rotationY = -90f - rotation,
            transformOrigin = TransformOrigin(0f, 0.5f),
            visibility = if (directionSign < 0f) easedProgress else 0f,
            content = { SocialHubMockScreen(selected = currentFace == LauncherFace.Social) },
        )
        NotificationRail(
            side = CubeDirection.Left,
            alpha = (0.35f + easedProgress).coerceAtMost(1f),
            offsetProgress = rotation / 90f,
        )
        NotificationRail(
            side = CubeDirection.Right,
            alpha = (0.35f + easedProgress).coerceAtMost(1f),
            offsetProgress = rotation / 90f,
        )
    }
}

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

@Composable
private fun NotificationRail(
    side: CubeDirection,
    alpha: Float,
    offsetProgress: Float,
) {
    val baseFraction = if (side == CubeDirection.Left) 0.06f else 0.94f
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = size.width * (baseFraction - 0.5f - (offsetProgress * 0.5f))
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .height(420.dp)
                .fillMaxWidth(0.012f)
                .graphicsLayer { this.alpha = alpha }
                .alpha(alpha)
                .graphicsLayer {
                    shadowElevation = 18f
                }
                .background(
                    brush = Brush.verticalGradient(
                        listOf(Color(0x80FFFFFF), Color(0x40A6C8FF), Color(0x80FFFFFF)),
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
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
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
        AppGrid(
            apps = uiState.allApps,
            columns = 4,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .anchoredDraggable(drawerState, Orientation.Vertical),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppGrid(
            apps = uiState.allApps,
            columns = 5,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
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
