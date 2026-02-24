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

import androidx.compose.runtime.mutableStateOf

/**
 * [C] Central context state singleton.
 *
 * Aggregates signals from the ID Access Gate (UsageStats), the Media Browser,
 * and the Notes Role into a single observable [ActiveContext]. The At-a-Glance
 * engine reads [current] to decide which contextual widget to show.
 */
data class MediaInfo(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
)

data class ActiveContext(
    val packageName: String = "root",
    val mediaInfo: MediaInfo? = null,
    val lastNoteSnippet: String? = null,
    val velocity: Float = 0f,
)

object ContextEngine {

    var current = mutableStateOf(ActiveContext())

    fun updateMedia(title: String?, artist: String?, album: String?) {
        val info = if (title != null) MediaInfo(title, artist, album) else null
        current.value = current.value.copy(mediaInfo = info)
    }

    fun updateApp(pkg: String) {
        current.value = current.value.copy(packageName = pkg)
    }

    fun updateNote(snippet: String?) {
        current.value = current.value.copy(lastNoteSnippet = snippet)
    }

    fun updateVelocity(velocity: Float) {
        current.value = current.value.copy(velocity = velocity)
    }
}
