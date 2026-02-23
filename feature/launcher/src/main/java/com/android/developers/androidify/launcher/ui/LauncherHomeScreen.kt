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
import androidx.compose.animation.core.rememberSplineBasedDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.android.developers.androidify.launcher.LauncherViewModel
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherLayoutType
import com.android.developers.androidify.launcher.data.RecentTask
import kotlinx.coroutines.launch

/** Two settled positions for the app-drawer sheet. */
enum class DrawerValue { Collapsed, Expanded }

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
    val decaySpec = rememberSplineBasedDecay<Float>()
    val scope = rememberCoroutineScope()

    val drawerState = remember {
        AnchoredDraggableState(
            initialValue = DrawerValue.Collapsed,
            positionalThreshold = { distance -> distance * 0.5f },
            velocityThreshold = { with(density) { 125.dp.toPx() } },
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
            decayAnimationSpec = decaySpec,
        )
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
                        label = task.label,
                        icon = task.icon,
                        launchIntent = null,
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
                .padding(bottom = 4.dp),
            dockApps = uiState.dockApps,
            onAppClick = onAppClick,
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onLensSearch = onLensSearch,
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
