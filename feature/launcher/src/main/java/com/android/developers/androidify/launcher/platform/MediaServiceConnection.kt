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
import android.media.session.MediaSessionManager
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat

/**
 * [F] Media Browser API — Direct connection to the currently-playing app's
 * MediaBrowserService via [MediaBrowserCompat].
 *
 * Yields high-res album art, track metadata, and playback state with zero latency —
 * unlike the notification-listener approach which parses RemoteViews.
 *
 * Feeds metadata into [ContextEngine] so the At-a-Glance engine can display
 * "Now Playing" information in perfect sync with the icon grid animations.
 */
class MediaServiceConnection(private val context: Context) {

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null

    private val connectionCallbacks = object : MediaBrowserCompat.ConnectionCallback() {
        override fun onConnected() {
            val browser = mediaBrowser ?: return
            val token = browser.sessionToken
            mediaController = MediaControllerCompat(context, token).also { controller ->
                ContextEngine.updateMedia(controller.metadata)
                controller.registerCallback(controllerCallback)
            }
        }

        override fun onConnectionSuspended() {
            mediaController?.unregisterCallback(controllerCallback)
            mediaController = null
        }

        override fun onConnectionFailed() {
            mediaController = null
        }
    }

    private val controllerCallback = object : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            ContextEngine.updateMedia(metadata)
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            // Could extend ContextEngine with playback state if needed
        }
    }

    /**
     * Connect to a specific media app's MediaBrowserService.
     */
    fun connect(targetPackage: String, serviceName: String) {
        disconnect()
        mediaBrowser = MediaBrowserCompat(
            context,
            ComponentName(targetPackage, serviceName),
            connectionCallbacks,
            null,
        ).apply { connect() }
    }

    /**
     * Attempt to connect to the currently active media session.
     * Uses [MediaSessionManager] to discover the active session and then
     * connects via MediaBrowser if a service component is found.
     */
    fun connectToActiveSession() {
        val sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE)
            as? MediaSessionManager ?: return
        try {
            val controllers = sessionManager.getActiveSessions(
                ComponentName(context, "com.android.developers.androidify.LauncherNotificationListenerService"),
            )
            val activeSession = controllers.firstOrNull() ?: return
            val metadata = activeSession.metadata
            if (metadata != null) {
                ContextEngine.updateMedia(
                    MediaMetadataCompat.Builder()
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_TITLE,
                            metadata.getString(android.media.MediaMetadata.METADATA_KEY_TITLE),
                        )
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_ARTIST,
                            metadata.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST),
                        )
                        .putString(
                            MediaMetadataCompat.METADATA_KEY_ALBUM,
                            metadata.getString(android.media.MediaMetadata.METADATA_KEY_ALBUM),
                        )
                        .build(),
                )
            }
        } catch (_: SecurityException) {
            // Notification listener not enabled
        }
    }

    fun disconnect() {
        mediaController?.unregisterCallback(controllerCallback)
        mediaController = null
        mediaBrowser?.disconnect()
        mediaBrowser = null
    }
}
