/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.android.developers.androidify.launcher.ui

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import com.android.developers.androidify.launcher.data.AppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Standard icon size in pixels for pre-rasterization. */
private const val ICON_SIZE_PX = 192

@Composable
fun AppGrid(
    apps: List<AppInfo>,
    columns: Int = 4,
    modifier: Modifier = Modifier,
    onAppClick: (AppInfo) -> Unit = {},
    onAppLongPress: (AppInfo) -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        items(apps, key = { "${it.packageName}:${it.className}:${it.user.hashCode()}" }) { app ->
            AppIconItem(app = app, onClick = { onAppClick(app) }, onLongPress = { onAppLongPress(app) })
        }
    }
}

@Composable
fun AppIconItem(
    app: AppInfo,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongPress: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(0.8f)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongPress()
                },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box {
            AppIconImage(
                drawable = app.icon,
                contentDescription = app.label,
                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(14.dp)),
            )
            if (app.notificationCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD50000))
                        .border(1.dp, Color.White, CircleShape),
                )
            }
        }
        Text(
            text = app.label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * Renders a [Drawable] as an [Image], rasterizing the drawable to a bitmap
 * off the main thread to avoid jank during scroll.
 */
@Composable
fun AppIconImage(drawable: Drawable?, contentDescription: String?, modifier: Modifier = Modifier) {
    if (drawable == null) {
        Box(modifier = modifier)
        return
    }

    val bitmapState: State<ImageBitmap?> = produceState<ImageBitmap?>(
        initialValue = null,
        key1 = drawable,
    ) {
        value = withContext(Dispatchers.Default) {
            val w = drawable.intrinsicWidth.coerceAtLeast(1)
            val h = drawable.intrinsicHeight.coerceAtLeast(1)
            // Rasterize at a fixed size — avoids huge bitmaps from xxxhdpi icons
            val targetSize = ICON_SIZE_PX.coerceAtMost(maxOf(w, h))
            drawable.toBitmap(targetSize, targetSize, Bitmap.Config.ARGB_8888).asImageBitmap()
        }
    }

    val bitmap = bitmapState.value
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    } else {
        // Placeholder while loading — prevents layout shift
        Box(modifier = modifier)
    }
}
