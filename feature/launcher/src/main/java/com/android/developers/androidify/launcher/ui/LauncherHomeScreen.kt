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
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.FoldingFeature
import com.android.developers.androidify.launcher.LauncherViewModel
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherLayoutType
import com.android.developers.androidify.launcher.data.RecentTask
import com.android.developers.androidify.launcher.data.WidgetContextAction

/**
 * Root composable for the launcher home screen. Chooses between phone and foldable
 * layouts based on [layoutType], and manages the swipe-up app-drawer overlay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherHomeScreen(
    layoutType: LauncherLayoutType = LauncherLayoutType.Phone,
    foldingFeature: FoldingFeature? = null,
    viewModel: LauncherViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showAppDrawer by remember { mutableStateOf(false) }

    // Refresh recent tasks whenever the drawer opens
    LaunchedEffect(showAppDrawer) {
        if (showAppDrawer) viewModel.refreshRecentTasks()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        WallpaperBackground()

        when (layoutType) {
            LauncherLayoutType.Phone -> PhoneLauncherLayout(
                uiState = uiState,
                onSwipeUp = { showAppDrawer = true },
                onAppClick = { app ->
                    viewModel.launchApp(app)
                    showAppDrawer = false
                },
                onSearch = viewModel::launchSearch,
                onVoiceSearch = viewModel::launchVoiceSearch,
                onLensSearch = viewModel::launchLensSearch,
                onWidgetAction = { /* no-op for now */ },
            )

            LauncherLayoutType.Foldable -> FoldableLauncherLayout(
                uiState = uiState,
                foldingFeature = foldingFeature,
                onSwipeUp = { showAppDrawer = true },
                onAppClick = { app ->
                    viewModel.launchApp(app)
                    showAppDrawer = false
                },
                onSearch = viewModel::launchSearch,
                onVoiceSearch = viewModel::launchVoiceSearch,
                onLensSearch = viewModel::launchLensSearch,
                onRecentFileClick = viewModel::openRecentFile,
            )
        }

        // App drawer overlay — slides up from the bottom
        AppDrawer(
            uiState = uiState,
            layoutType = layoutType,
            visible = showAppDrawer,
            onDismiss = { showAppDrawer = false },
            onAppClick = { app ->
                viewModel.launchApp(app)
                showAppDrawer = false
            },
            onTaskClick = { task ->
                viewModel.launchApp(
                    com.android.developers.androidify.launcher.data.AppInfo(
                        packageName = task.packageName,
                        label = task.label,
                        icon = task.icon,
                        launchIntent = null,
                    ),
                )
                showAppDrawer = false
            },
            onSearchQueryChange = viewModel::updateSearchQuery,
            onSearchSubmit = { query ->
                viewModel.launchSearch(query)
                showAppDrawer = false
            },
        )
    }
}

/**
 * Phone home screen: wallpaper + app icon grid + pill search widget at bottom.
 * A swipe-up gesture anywhere triggers the app drawer.
 */
@Composable
private fun PhoneLauncherLayout(
    uiState: com.android.developers.androidify.launcher.LauncherUiState,
    onSwipeUp: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    onWidgetAction: (WidgetContextAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track drag direction to detect a meaningful upward swipe
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 120f

    // Fade-in animation for app icons after wallpaper loads
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
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { cumulativeDragY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeDragY += dragAmount
                        if (cumulativeDragY < -swipeThresholdPx) {
                            onSwipeUp()
                            cumulativeDragY = 0f
                        }
                    },
                    onDragEnd = { cumulativeDragY = 0f },
                    onDragCancel = { cumulativeDragY = 0f },
                )
            }
            .alpha(contentAlpha),
    ) {
        // App icon grid takes the majority of the screen
        AppGrid(
            apps = uiState.allApps,
            columns = 4,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onAppClick = onAppClick,
        )

        // Single pill-shaped search widget pinned at the bottom
        PillSearchWidget(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 12.dp),
            onSearch = onSearch,
            onVoiceSearch = onVoiceSearch,
            onLensSearch = onLensSearch,
            onWidgetAction = onWidgetAction,
        )
    }
}

/**
 * Foldable inner-display home screen.
 *
 * The expanded canvas uses a richer layout:
 * - Larger search widget with recent Chrome tabs and recent files
 * - App grid sized for a wider display
 */
@Composable
fun FoldableLauncherLayout(
    uiState: com.android.developers.androidify.launcher.LauncherUiState,
    foldingFeature: FoldingFeature? = null,
    onSwipeUp: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onSearch: (String) -> Unit,
    onVoiceSearch: () -> Unit,
    onLensSearch: () -> Unit,
    onRecentFileClick: (com.android.developers.androidify.launcher.data.RecentFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    var cumulativeDragY by remember { mutableFloatStateOf(0f) }
    val swipeThresholdPx = 120f

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { cumulativeDragY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        cumulativeDragY += dragAmount
                        if (cumulativeDragY < -swipeThresholdPx) {
                            onSwipeUp()
                            cumulativeDragY = 0f
                        }
                    },
                    onDragEnd = { cumulativeDragY = 0f },
                    onDragCancel = { cumulativeDragY = 0f },
                )
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Wider 5-column grid for the unfolded display
        AppGrid(
            apps = uiState.allApps,
            columns = 5,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            onAppClick = onAppClick,
        )

        Spacer(Modifier.height(8.dp))

        // Expanded search widget with Chrome tabs + recent files
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
