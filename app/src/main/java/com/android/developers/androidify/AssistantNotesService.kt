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
import android.content.Intent
import android.os.IBinder

/**
 * [B] Notes Role Integration — Service stub for android.app.role.NOTES.
 *
 * Claiming the NOTES role gives the launcher process a higher oom_score_adj,
 * keeping it warm in memory. This also grants lock-screen drawing privileges:
 * the system places a RemoteAction on the lock screen and power menu for
 * instant note capture.
 *
 * The Notes Role is the "persistent soul" of the assistant; the launcher is
 * the "physical body."
 *
 * When the system invokes CREATE_NOTE on the lock screen, this service can
 * launch an ultra-light note-taking overlay without fully resuming the launcher.
 */
class AssistantNotesService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CREATE_NOTE) {
            handleCreateNote()
        }
        return START_NOT_STICKY
    }

    private fun handleCreateNote() {
        // Launch ultra-light note-taking version of the launcher.
        // For now this is a stub — a full implementation would show a minimal
        // Compose overlay window on the lock screen.
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            action = ACTION_CREATE_NOTE
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(launchIntent)
    }

    private companion object {
        const val ACTION_CREATE_NOTE = "android.intent.action.CREATE_NOTE"
    }
}
