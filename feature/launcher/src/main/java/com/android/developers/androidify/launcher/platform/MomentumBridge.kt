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
package com.android.developers.androidify.launcher.platform

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf

/**
 * [A] Velocity Buffer — Global synthetic momentum state holder.
 *
 * The system-level gesture (swipe-up from any app) happens in a blind phase where
 * our process has zero touch data. MomentumBridge acts as a Synthetic Momentum
 * Injector: it records a high initial velocity the instant our process resumes so
 * that spring-based Compose animations can fake kinetic continuity, eliminating the
 * "dead start" feel of third-party launchers.
 *
 * Three logical sub-systems feed into this bridge:
 *  - **Momentum Capturer** – VelocityTracker for in-app gestures (future).
 *  - **State Hand-off** – MutatorMutex ensuring animations take over at finger-lift (future).
 *  - **Spring Controller** – SpringSpec with tuned damping/stiffness for overshoot bounce,
 *    consumed wherever [initialVelocity] is read inside a `LaunchedEffect`.
 */
object MomentumBridge {

    /** Synthetic velocity (px/s) injected on resume. Negative = upward motion. */
    var initialVelocity = mutableFloatStateOf(0f)

    /** Monotonically increasing trigger timestamp so `LaunchedEffect(triggerTime)` re-fires. */
    var triggerTime = mutableLongStateOf(0L)

    /** The package name of the app the user just left (populated by the ID Access Gate). */
    var activePackage = mutableLongStateOf(0L)

    /**
     * Inject a synthetic velocity into the bridge. Called from:
     *  - `MainActivity.onResume()` (default -3 000 px/s)
     *  - Navigation Observer callback (Android 16+, -3 500 px/s for earlier "starting gun")
     */
    fun inject(velocity: Float) {
        initialVelocity.floatValue = velocity
        triggerTime.longValue = System.currentTimeMillis()
    }
}
