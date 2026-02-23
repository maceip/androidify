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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.data.ChromeTab
import com.android.developers.androidify.launcher.data.RecentFile
import com.android.developers.androidify.launcher.data.WidgetContextAction

/**
 * Expanded search widget for the foldable inner display home screen.
 *
 * Unlike the compact [PillSearchWidget], this widget includes:
 * - The standard pill search bar with voice/lens shortcuts
 * - A "Recent Chrome tabs" section showing the last few open browser tabs
 * - A "Recent files" section showing recently accessed documents/images
 *
 * This reflects the richer at-a-glance information that makes sense on the
 * larger Pixel Fold inner display.
 */
@Composable
fun FoldableSearchWidget(
    chromeTabs: List<ChromeTab>,
    recentFiles: List<RecentFile>,
    modifier: Modifier = Modifier,
    onSearch: (String) -> Unit = {},
    onVoiceSearch: () -> Unit = {},
    onLensSearch: () -> Unit = {},
    onChromeTabClick: (ChromeTab) -> Unit = {},
    onRecentFileClick: (RecentFile) -> Unit = {},
    onWidgetAction: (WidgetContextAction) -> Unit = {},
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = Color.White.copy(alpha = 0.92f),
        shadowElevation = 8.dp,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            // Pill search row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clickable { onSearch("") }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                // Google "G" logo
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "G",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = "Search",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.weight(1f),
                )
                // Lens icon
                IconButton(onClick = onLensSearch, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Google Lens",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
                // Voice icon
                IconButton(onClick = onVoiceSearch, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Outlined.Mic,
                        contentDescription = "Voice search",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (chromeTabs.isNotEmpty() || recentFiles.isNotEmpty()) {
                HorizontalDivider(
                    color = Color.Black.copy(alpha = 0.08f),
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            // Recent Chrome tabs
            if (chromeTabs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Chrome tabs",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(4.dp))
                chromeTabs.take(3).forEach { tab ->
                    ChromeTabRow(tab = tab, onClick = { onChromeTabClick(tab) })
                }
            }

            // Recent files
            if (recentFiles.isNotEmpty()) {
                if (chromeTabs.isNotEmpty()) {
                    HorizontalDivider(
                        color = Color.Black.copy(alpha = 0.06f),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    )
                } else {
                    Spacer(Modifier.height(8.dp))
                }
                Text(
                    text = "Recent files",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Spacer(Modifier.height(4.dp))
                recentFiles.take(3).forEach { file ->
                    RecentFileRow(file = file, onClick = { onRecentFileClick(file) })
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ChromeTabRow(
    tab: ChromeTab,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (tab.favicon != null) {
            AppIconImage(
                drawable = tab.favicon,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .background(Color(0xFF4285F4), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text("G", style = MaterialTheme.typography.labelSmall, color = Color.White)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tab.title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = tab.url,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun RecentFileRow(
    file: RecentFile,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fileIcon = when {
        file.mimeType.startsWith("image/") -> "🖼"
        file.mimeType.contains("pdf") -> "📄"
        file.mimeType.contains("word") -> "📝"
        file.mimeType.contains("excel") || file.mimeType.contains("sheet") -> "📊"
        else -> "📁"
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(text = fileIcon, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = file.name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.DarkGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}
