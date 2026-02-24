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

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaSessionManager

/**
 * [F] Media Browser API — Connects to the active media session to extract
 * now-playing metadata (title, artist, album) with zero latency.
 *
 * Uses [MediaSessionManager] to discover the active session via the
 * notification listener component. Feeds metadata into [ContextEngine]
 * so the At-a-Glance engine can display "Now Playing" information.
 */
class MediaServiceConnection(private val context: Context) {

    /**
     * Attempt to connect to the currently active media session.
     * Uses [MediaSessionManager] to discover the active session and then
     * extracts metadata directly from the framework media controller.
     */
    fun connectToActiveSession() {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager ?: return
        try {
            val controllers = sessionManager.getActiveSessions(
                ComponentName(
                    context,
                    "com.android.developers.androidify.LauncherNotificationListenerService",
                ),
            )
            val activeSession = controllers.firstOrNull() ?: return
            val metadata = activeSession.metadata ?: return
            ContextEngine.updateMedia(
                title = metadata.getString(MediaMetadata.METADATA_KEY_TITLE),
                artist = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST),
                album = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM),
            )
        } catch (_: SecurityException) {
            // Notification listener not enabled
        }
    }

    fun disconnect() {
        // Clean up if needed in future when persistent connections are added
    }
}
