package com.android.developers.androidify.launcher.ui

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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherShortcut
import kotlin.math.absoluteValue
import kotlin.math.ceil

/** Number of rows visible on a phone home screen page. */
private const val ROWS_PER_PAGE = 5

/**
 * Paged home screen grid with horizontal swiping between pages and a dot
 * indicator. Each page shows a fixed [columns] × [ROWS_PER_PAGE] grid.
 * The app drawer (swipe-up) retains its own scrollable [AppGrid].
 */
@Composable
fun PagedHomeGrid(
    apps: List<AppInfo>,
    columns: Int = 4,
    modifier: Modifier = Modifier,
    onAppClick: (AppInfo) -> Unit = {},
    onAppLongPress: (AppInfo) -> Unit = {},
    onLoadShortcuts: (String, (List<LauncherShortcut>) -> Unit) -> Unit = { _, _ -> },
    onLaunchShortcut: (LauncherShortcut) -> Unit = {},
    onAppInfo: (AppInfo) -> Unit = {},
) {
    val appsPerPage = columns * ROWS_PER_PAGE
    val pageCount by remember(apps.size, appsPerPage) {
        derivedStateOf { ceil(apps.size.toFloat() / appsPerPage).toInt().coerceAtLeast(1) }
    }
    val pagerState = rememberPagerState(pageCount = { pageCount })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1,
        ) { pageIndex ->
            val pageApps = apps.drop(pageIndex * appsPerPage).take(appsPerPage)
            HomeGridPage(
                apps = pageApps,
                columns = columns,
                rows = ROWS_PER_PAGE,
                onAppClick = onAppClick,
                onAppLongPress = onAppLongPress,
                onLoadShortcuts = onLoadShortcuts,
                onLaunchShortcut = onLaunchShortcut,
                onAppInfo = onAppInfo,
                modifier = Modifier.fillMaxSize(),
            )
        }

        // Page indicator dots
        if (pageCount > 1) {
            PageIndicator(
                pagerState = pagerState,
                pageCount = pageCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
        }
    }
}

/**
 * A single page of app icons laid out in a fixed [columns] × [rows] grid.
 * Unlike [AppGrid] which uses a LazyVerticalGrid, this uses a static
 * Column/Row layout since each page has a bounded number of items.
 */
@Composable
private fun HomeGridPage(
    apps: List<AppInfo>,
    columns: Int,
    rows: Int,
    onAppClick: (AppInfo) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onLoadShortcuts: (String, (List<LauncherShortcut>) -> Unit) -> Unit,
    onLaunchShortcut: (LauncherShortcut) -> Unit,
    onAppInfo: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Track which app has its shortcut popup open
    var popupApp by remember { mutableStateOf<AppInfo?>(null) }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    for (col in 0 until columns) {
                        val index = row * columns + col
                        if (index < apps.size) {
                            val app = apps[index]
                            Box(modifier = Modifier.weight(1f)) {
                                AppIconItem(
                                    app = app,
                                    onClick = { onAppClick(app) },
                                    onLongPress = {
                                        popupApp = app
                                        onAppLongPress(app)
                                    },
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        // Shortcut popup overlay
        popupApp?.let { app ->
            AppShortcutPopup(
                app = app,
                onDismiss = { popupApp = null },
                onLoadShortcuts = onLoadShortcuts,
                onLaunchShortcut = {
                    popupApp = null
                    onLaunchShortcut(it)
                },
                onAppInfo = {
                    popupApp = null
                    onAppInfo(app)
                },
                onUninstall = {
                    popupApp = null
                    // Uninstall intent handled at caller level
                },
            )
        }
    }
}

/**
 * Page indicator dots. The current page dot is brighter and slightly larger.
 */
@Composable
private fun PageIndicator(
    pagerState: PagerState,
    pageCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            val offsetFraction = (pagerState.currentPage - index +
                pagerState.currentPageOffsetFraction).absoluteValue.coerceIn(0f, 1f)
            val dotAlpha = 1f - offsetFraction * 0.6f
            val dotScale = 1f - offsetFraction * 0.3f

            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(8.dp)
                    .graphicsLayer {
                        alpha = dotAlpha
                        scaleX = dotScale
                        scaleY = dotScale
                    },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.85f),
            ) {}
        }
    }
}
