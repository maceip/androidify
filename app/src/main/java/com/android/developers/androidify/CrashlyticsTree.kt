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

import android.util.Log
import timber.log.Timber

/**
 * A Timber tree for release builds that filters verbose/debug logs.
 *
 * @@@ TODO: Replace with a real crash reporting service (Firebase Crashlytics,
 * Sentry, Bugsnag, etc.) before shipping to production.
 */
class CrashlyticsTree : Timber.Tree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority == Log.VERBOSE || priority == Log.DEBUG) {
            return
        }
        // Log to system log for now; replace with crash reporting SDK later
        Log.println(priority, tag ?: "Launcher", message)
        if (t != null) {
            Log.e(tag ?: "Launcher", "Exception:", t)
        }
    }
}
