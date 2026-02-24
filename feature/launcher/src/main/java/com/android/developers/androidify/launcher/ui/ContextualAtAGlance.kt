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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.platform.ContextEngine
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * [D] Contextual At-a-Glance Engine.
 *
 * Replaces the Pixel's RemoteView-based Smartspace (which suffers IPC lag between
 * the Google App and the launcher) with a native Compose component in the same
 * process as the icon grid. Because it shares the same Choreographer frame clock,
 * it animates in perfect sync with the icons (parallax, velocity-matched entry).
 *
 * Data sources:
 *  - MediaBrowser API for now-playing (via [ContextEngine])
 *  - UsageStats for last app (via [ContextEngine])
 *  - Local notes (since we own the Notes Role)
 *
 * @param yOffset The current momentum translation — used for parallax at 0.4x rate
 */
@Composable
fun ContextualAtAGlance(
    yOffset: Float,
    modifier: Modifier = Modifier,
) {
    val ctx by ContextEngine.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer { translationY = yOffset * 0.4f },
    ) {
        // Primary header: context-aware
        Text(
            text = when {
                ctx.mediaInfo?.title != null -> "Now Playing: ${ctx.mediaInfo?.title}"
                ctx.packageName == "com.android.chrome" -> "Continue reading..."
                else -> getFormattedDate()
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )

        Spacer(Modifier.height(4.dp))

        // Secondary info line
        Text(
            text = ctx.mediaInfo?.artist ?: "",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )

        Spacer(Modifier.height(8.dp))

        // Note card: shown when idle or returning with high velocity
        if (ctx.packageName == "root" || ctx.velocity < -2000f) {
            ctx.lastNoteSnippet?.let { snippet ->
                NoteCard(snippet = snippet)
            }
        }

        // Now-playing card: shown when media is active
        val media = ctx.mediaInfo
        if (media?.title != null) {
            NowPlayingCard(
                title = media.title,
                artist = media.artist,
            )
        }
    }
}

/**
 * [D] Integrated At-a-Glance variant that adapts based on the top app ID.
 * Uses stronger parallax (0.5x) and fades out as the momentum offset increases.
 */
@Composable
fun IntegratedAtAGlance(
    topAppId: String,
    yOffset: Float,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .graphicsLayer {
                translationY = yOffset * 0.5f
                alpha = 1f - (yOffset / 500f).coerceIn(0f, 1f)
            },
    ) {
        Text(
            text = when (topAppId) {
                "com.android.chrome" -> "Continue reading..."
                "com.spotify.music" -> "Now Playing"
                else -> getFormattedDate()
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
    }
}

@Composable
private fun NoteCard(
    snippet: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.Notes,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = snippet,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 2,
            )
        }
    }
}

@Composable
private fun NowPlayingCard(
    title: String,
    artist: String?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.12f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    maxLines = 1,
                )
                if (artist != null) {
                    Text(
                        text = artist,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun getFormattedDate(): String {
    val formatter = SimpleDateFormat("EEEE, MMM d", Locale.getDefault())
    return formatter.format(Date())
}
