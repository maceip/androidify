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

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context

/**
 * [C] ID Access Gate — Identifies which app the user just left.
 *
 * Uses [UsageStatsManager.queryEvents] with [UsageEvents.Event.ACTIVITY_RESUMED]
 * / [UsageEvents.Event.ACTIVITY_PAUSED] event types. This is the Google-approved
 * method (NOT AccessibilityService, which risks Play Store delisting).
 *
 * Requires the `PACKAGE_USAGE_STATS` permission, which the user grants in
 * Settings > Usage Access.
 *
 * The resolved package name is pushed into [ContextEngine] so the At-a-Glance
 * engine and Velocity Buffer can tailor entry animations and contextual content.
 */
object UsageGatekeeper {

    /**
     * Query UsageStats for the most recently resumed app within the last [windowMs].
     * Returns the package name or `null` if permission is not granted or no events found.
     */
    fun getTopPackage(context: Context, windowMs: Long = 2000L): String? {
        val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE)
            as? UsageStatsManager ?: return null

        val endTime = System.currentTimeMillis()
        val events = try {
            usageStatsManager.queryEvents(endTime - windowMs, endTime)
        } catch (_: SecurityException) {
            // PACKAGE_USAGE_STATS not granted
            return null
        }

        var lastResumedPackage: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                lastResumedPackage = event.packageName
            }
        }

        // Push into ContextEngine so other systems can react
        lastResumedPackage?.let { ContextEngine.updateApp(it) }

        return lastResumedPackage
    }
}
