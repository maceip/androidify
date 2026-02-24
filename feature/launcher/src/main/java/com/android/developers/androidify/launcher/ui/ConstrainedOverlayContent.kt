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

import android.graphics.PixelFormat
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * [E] Constrained Overlay Viewport.
 *
 * A WindowManager-managed window pinned to the bottom of the screen. Smaller
 * viewports get automatic touch passthrough above the window, reduced GPU load
 * on 120Hz, and avoid Android 12+'s "Untrusted Touch" blocking.
 *
 * Can be dynamically resized from 20% dock to 100% full launcher. Uses
 * TYPE_APPLICATION_OVERLAY or, if the Nav Observer path is used, standard
 * activity windows.
 */
object ConstrainedOverlayViewport {

    /**
     * Create [WindowManager.LayoutParams] for a bottom-pinned overlay viewport.
     *
     * @param screenHeight Total screen height in pixels
     * @param fraction Fraction of screen height for the viewport (default 0.2 = 20%)
     */
    fun createLayoutParams(
        screenHeight: Int,
        fraction: Float = 0.2f,
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * fraction).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
    }

    /**
     * Create standard activity window layout params (no overlay permission needed).
     * Used when the launcher is the foreground activity.
     */
    fun createActivityLayoutParams(
        screenHeight: Int,
        fraction: Float = 0.2f,
    ): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            (screenHeight * fraction).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_PANEL,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM
        }
    }
}

/**
 * [E] Compose content for the constrained overlay viewport.
 * Fills the available constrained space and renders the dock grid.
 */
@Composable
fun ConstrainedOverlayContent(
    content: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val viewportHeight = maxHeight
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .height(viewportHeight),
        ) {
            content()
        }
    }
}
