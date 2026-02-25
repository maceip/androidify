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

import android.app.Service
import android.content.ComponentName
import android.content.Intent
import android.os.IBinder
import com.android.developers.androidify.launcher.platform.ContextEngine
import com.android.developers.androidify.launcher.platform.SocialCategory
import com.android.developers.androidify.launcher.platform.SocialFeedItem
import com.android.developers.androidify.launcher.platform.SocialFeedStore

/**
 * [B] Notes Role Integration — Service for android.app.role.NOTES.
 *
 * Claiming the NOTES role gives the launcher process a higher oom_score_adj,
 * keeping it warm in memory. This also grants lock-screen drawing privileges:
 * the system places a RemoteAction on the lock screen and power menu for
 * instant note capture.
 *
 * When CREATE_NOTE fires, we:
 * 1. Push the note snippet into the Social Hub feed
 * 2. Update ContextEngine for At-a-Glance
 * 3. Launch the main activity for full note editing
 */
class AssistantNotesService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CREATE_NOTE) {
            handleCreateNote(intent)
        }
        return START_NOT_STICKY
    }

    private fun handleCreateNote(intent: Intent) {
        // Extract any text content passed with the note intent
        val noteText = intent.getStringExtra(Intent.EXTRA_TEXT)
            ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()

        val snippet = noteText ?: "Quick note captured"

        // Feed into Social Hub
        SocialFeedStore.addItem(
            SocialFeedItem(
                id = "note_${System.currentTimeMillis()}",
                packageName = packageName,
                appLabel = "Notes",
                title = "Quick Note",
                text = snippet,
                timestamp = System.currentTimeMillis(),
                category = SocialCategory.Note,
            ),
        )

        // Feed into At-a-Glance context
        ContextEngine.updateNote(snippet)

        // Launch the main activity for full editing
        val launchIntent = Intent(ACTION_CREATE_NOTE).apply {
            component = ComponentName(
                this@AssistantNotesService,
                "com.android.developers.androidify.MainActivity",
            )
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (noteText != null) putExtra(Intent.EXTRA_TEXT, noteText)
        }
        startActivity(launchIntent)
    }

    private companion object {
        const val ACTION_CREATE_NOTE = "android.intent.action.CREATE_NOTE"
    }
}
