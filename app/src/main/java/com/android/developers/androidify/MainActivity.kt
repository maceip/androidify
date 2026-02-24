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
package com.android.developers.androidify

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.window.OnBackInvokedDispatcher
import android.window.TrustedPresentationThresholds
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.android.developers.androidify.launcher.platform.ContextEngine
import com.android.developers.androidify.launcher.platform.MediaServiceConnection
import com.android.developers.androidify.launcher.platform.MomentumBridge
import com.android.developers.androidify.launcher.platform.UsageGatekeeper
import com.android.developers.androidify.navigation.MainNavigation
import com.android.developers.androidify.theme.AndroidifyTheme
import com.android.developers.androidify.util.LocalOcclusion
import dagger.hilt.android.AndroidEntryPoint
import java.util.function.Consumer

@ExperimentalMaterial3ExpressiveApi
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val isWindowOccluded = mutableStateOf(false)
    private val presentationListener = Consumer<Boolean> { isMinFractionRendered ->
        isWindowOccluded.value = !isMinFractionRendered
    }

    /** [F] Media Browser connection for At-a-Glance now-playing data. */
    private var mediaServiceConnection: MediaServiceConnection? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        registerNavigationObserver()
        mediaServiceConnection = MediaServiceConnection(this)

        setContent {
            AndroidifyTheme {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.light(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                    navigationBarStyle = SystemBarStyle.light(
                        Color.Transparent.toArgb(),
                        Color.Transparent.toArgb(),
                    ),
                )
                CompositionLocalProvider(LocalOcclusion provides isWindowOccluded) {
                    MainNavigation()
                }
            }
        }
    }

    /**
     * [A] Inject synthetic velocity on every resume so spring animations have
     * kinetic continuity even when the system gesture gave us zero touch data.
     * [C] Query the ID Access Gate for the app the user just left.
     * [F] Refresh media session metadata for At-a-Glance.
     */
    override fun onResume() {
        super.onResume()
        MomentumBridge.inject(-3000f)
        UsageGatekeeper.getTopPackage(this)
        mediaServiceConnection?.connectToActiveSession()
    }

    /**
     * [G] onUserLeaveHint — Universal fallback for home detection.
     * Fires right before the app goes to background due to Home press or swipe-up.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        ContextEngine.updateVelocity(MomentumBridge.initialVelocity.floatValue)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaServiceConnection?.disconnect()
    }

    /**
     * [G] Navigation Observer — On Android 16+, PRIORITY_SYSTEM_NAVIGATION_OBSERVER
     * fires when the system detects a back-to-home gesture, before the transition
     * completes. This is the "starting gun" for the Velocity Buffer, allowing us
     * to inject a higher synthetic velocity earlier in the animation pipeline.
     */
    private fun registerNavigationObserver() {
        if (Build.VERSION.SDK_INT >= 36) {
            try {
                onBackInvokedDispatcher.registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_SYSTEM_NAVIGATION_OBSERVER,
                ) {
                    MomentumBridge.inject(-3500f)
                }
            } catch (_: Exception) {
                // PRIORITY_SYSTEM_NAVIGATION_OBSERVER may not be available on all devices
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val minAlpha = 1f
            val minFractionRendered = 0.25f
            val stabilityRequirements = 500
            val presentationThreshold = TrustedPresentationThresholds(
                minAlpha,
                minFractionRendered,
                stabilityRequirements,
            )

            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.registerTrustedPresentationListener(
                window.decorView.windowToken,
                presentationThreshold,
                mainExecutor,
                presentationListener,
            )
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            val windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
            windowManager.unregisterTrustedPresentationListener(presentationListener)
        }
    }
}
