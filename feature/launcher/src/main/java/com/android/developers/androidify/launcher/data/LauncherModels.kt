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
package com.android.developers.androidify.launcher.data

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.UserHandle

/** Represents an installed application on the device. */
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val icon: Drawable?,
    val launchIntent: Intent?,
    val user: UserHandle,
    val isWorkProfile: Boolean = false,
    val notificationCount: Int = 0,
)

data class LauncherShortcut(
    val id: String,
    val shortLabel: CharSequence,
    val longLabel: CharSequence?,
    val packageName: String,
    val user: UserHandle,
    val rank: Int,
)

fun ShortcutInfo.toLauncherShortcut() = LauncherShortcut(
    id = id,
    shortLabel = shortLabel,
    longLabel = longLabel,
    packageName = `package`,
    user = userHandle,
    rank = rank,
)

/** Represents a recently used task (running app) for the recents panel. */
data class RecentTask(
    val id: Int,
    val label: String,
    val packageName: String,
    val icon: Drawable?,
    val thumbnail: android.graphics.Bitmap?,
)

/** Represents a recently opened Chrome/browser tab. */
data class ChromeTab(
    val title: String,
    val url: String,
    val favicon: Drawable?,
)

/** Represents a recently accessed file. */
data class RecentFile(
    val name: String,
    val mimeType: String,
    val uri: android.net.Uri,
    val thumbnail: android.graphics.Bitmap?,
    val modifiedTime: Long,
)

/** The primary widget action items for the pill search widget context menu. */
enum class WidgetContextAction {
    Resize,
    Remove,
    EditStyle,
}

/** Layout type for the launcher — phone vs foldable inner display. */
enum class LauncherLayoutType {
    Phone,
    Foldable,
}
