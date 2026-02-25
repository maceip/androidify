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

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.LauncherUiState
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherLayoutType
import com.android.developers.androidify.launcher.data.RecentTask

/**
 * Full-screen app drawer that slides up from the bottom when the user swipes up
 * on the home screen.
 *
 * The drawer is always in the composition tree and translated off-screen via
 * [graphicsLayer] when collapsed, so there is no AnimatedVisibility overhead on
 * first open. Position tracks the finger 1:1 through [drawerState]; spring physics
 * snap it to the nearest anchor on release.
 *
 * **Phone layout**: recent-app mini-cards at top, search field, then alphabetical
 * app grid — coordinated via [NestedScrollConnection] so scrolling down at the
 * list top closes the drawer instead of over-scrolling.
 *
 * **Foldable layout**: running-apps tiled on the left half; search + app list on
 * the right half (see [FoldableAppDrawer]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDrawer(
    uiState: LauncherUiState,
    layoutType: LauncherLayoutType,
    drawerState: AnchoredDraggableState<DrawerValue>,
    screenHeightPx: Float,
    onDismiss: () -> Unit,
    onAppClick: (AppInfo) -> Unit,
    onTaskClick: (RecentTask) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val drawerShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Box(
        modifier = modifier
            .fillMaxSize()
            // GPU-translate the entire drawer — no recomposition during drag
            .graphicsLayer {
                val raw = drawerState.offset
                translationY = if (raw.isNaN()) screenHeightPx else raw
                shape = drawerShape
                clip = true
            }
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.55f),
                        Color.Black.copy(alpha = 0.75f),
                    ),
                ),
                shape = drawerShape,
            ),
    ) {
        when (layoutType) {
            LauncherLayoutType.Phone -> PhoneAppDrawer(
                uiState = uiState,
                drawerState = drawerState,
                onAppClick = onAppClick,
                onTaskClick = onTaskClick,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                onDismiss = onDismiss,
            )
            LauncherLayoutType.Foldable -> FoldableAppDrawer(
                uiState = uiState,
                drawerState = drawerState,
                onAppClick = onAppClick,
                onTaskClick = onTaskClick,
                onSearchQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhoneAppDrawer(
    uiState: LauncherUiState,
    drawerState: AnchoredDraggableState<DrawerValue>,
    onAppClick: (AppInfo) -> Unit,
    onTaskClick: (RecentTask) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Clear focus and hide keyboard when the drawer collapses.
    // Do NOT auto-focus on expand — keyboard should only appear when user taps search.
    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Collapsed) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    // Remaining downward scroll after the list reaches the top closes the drawer
    val nestedScrollConnection = remember(drawerState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return if (available.y > 0f) {
                    Offset(0f, drawerState.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (available.y > 0f && drawerState.currentValue != DrawerValue.Collapsed) {
                    drawerState.animateTo(
                        DrawerValue.Collapsed,
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        ),
                    )
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Drag handle pill
        Box(
            modifier = Modifier
                .padding(top = 12.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.4f))
                .align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(12.dp))

        // Search field at the top (Pixel layout: search → recent → apps)
        DrawerSearchField(
            query = uiState.searchQuery,
            onQueryChange = onSearchQueryChange,
            onSearchSubmit = onSearchSubmit,
            focusRequester = focusRequester,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
        )

        Spacer(Modifier.height(12.dp))

        if (uiState.recentTasks.isNotEmpty()) {
            Text(
                text = "Recent apps",
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(8.dp))
            RecentAppsCards(
                tasks = uiState.recentTasks,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                onTaskClick = onTaskClick,
            )
            Spacer(Modifier.height(12.dp))
        }

        // nestedScroll coordinates "scroll list" vs "close drawer" seamlessly
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .nestedScroll(nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(uiState.filteredApps, key = { it.packageName }) { app ->
                AppIconItem(app = app, onClick = { onAppClick(app) })
            }
        }
    }
}

/**
 * Foldable two-panel app drawer: recent tasks on the left, app search list on the right.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldableAppDrawer(
    uiState: LauncherUiState,
    drawerState: AnchoredDraggableState<DrawerValue>,
    onAppClick: (AppInfo) -> Unit,
    onTaskClick: (RecentTask) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(drawerState.currentValue) {
        if (drawerState.currentValue == DrawerValue.Collapsed) {
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }

    val nestedScrollConnection = remember(drawerState) {
        object : NestedScrollConnection {
            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                return if (available.y > 0f) {
                    Offset(0f, drawerState.dispatchRawDelta(available.y))
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                return if (available.y > 0f && drawerState.currentValue != DrawerValue.Collapsed) {
                    drawerState.animateTo(
                        DrawerValue.Collapsed,
                        spring(
                            dampingRatio = 0.8f,
                            stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow,
                        ),
                    )
                    available
                } else {
                    Velocity.Zero
                }
            }
        }
    }

    Row(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        // Left panel — recent apps tiled vertically
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 16.dp, top = 24.dp, end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Recent",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White.copy(alpha = 0.8f),
            )
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.recentTasks, key = { it.id }) { task ->
                    RecentTaskCard(
                        task = task,
                        modifier = Modifier.width(160.dp),
                        onClick = { onTaskClick(task) },
                    )
                }
            }
        }

        // Right panel — search + app list
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(start = 8.dp, top = 24.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DrawerSearchField(
                query = uiState.searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearchSubmit = onSearchSubmit,
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxWidth(),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection),
                contentPadding = PaddingValues(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(uiState.filteredApps, key = { it.packageName }) { app ->
                    AppIconItem(app = app, onClick = { onAppClick(app) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DrawerSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester = remember { FocusRequester() },
    placeholder: String = "Search apps",
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.focusRequester(focusRequester),
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.5f),
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White.copy(alpha = 0.15f),
            unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.White.copy(alpha = 0.5f),
            unfocusedIndicatorColor = Color.White.copy(alpha = 0.2f),
            cursorColor = Color.White,
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearchSubmit(query) },
        ),
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            textAlign = TextAlign.Start,
            color = Color.White,
        ),
    )
}
