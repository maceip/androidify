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
package com.android.developers.androidify.launcher

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.ChromeTab
import com.android.developers.androidify.launcher.data.RecentFile
import com.android.developers.androidify.launcher.data.RecentTask
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    /** Up to 5 pinned dock/hotseat apps sourced from common installed packages. */
    val dockApps: List<AppInfo> = emptyList(),
    val recentTasks: List<RecentTask> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val chromeTabs: List<ChromeTab> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadRecentFiles()
        }
    }

    init {
        loadInstalledApps()
        loadRecentTasks()
        loadRecentFiles()
        context.contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true,
            mediaContentObserver,
        )
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(mediaContentObserver)
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { state ->
            val filtered = if (query.isBlank()) {
                state.allApps
            } else {
                state.allApps.filter {
                    it.label.contains(query, ignoreCase = true) ||
                        it.packageName.contains(query, ignoreCase = true)
                }
            }
            state.copy(searchQuery = query, filteredApps = filtered)
        }
    }

    fun refreshRecentTasks() {
        loadRecentTasks()
    }

    private fun loadInstalledApps() {
        viewModelScope.launch(Dispatchers.IO) {
            val pm = context.packageManager
            val mainIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val resolvedApps = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
            val apps = resolvedApps
                .map { resolveInfo ->
                    AppInfo(
                        packageName = resolveInfo.activityInfo.packageName,
                        label = resolveInfo.loadLabel(pm).toString(),
                        icon = resolveInfo.loadIcon(pm),
                        launchIntent = pm.getLaunchIntentForPackage(
                            resolveInfo.activityInfo.packageName,
                        ),
                    )
                }
                .sortedBy { it.label.lowercase() }
                .distinctBy { it.packageName }

            // Pick up to 5 dock apps from the preferred package list, in order
            val dockApps = DOCK_PACKAGE_PRIORITY
                .mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                .take(5)

            _uiState.update { state ->
                state.copy(
                    allApps = apps,
                    filteredApps = if (state.searchQuery.isBlank()) apps else state.filteredApps,
                    dockApps = dockApps,
                    isLoading = false,
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadRecentTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
                as ActivityManager
            val pm = context.packageManager

            // getRecentTasks is limited on API 21+ for privacy — returns own tasks + a few others
            val tasks = try {
                activityManager.getRecentTasks(
                    MAX_RECENT_TASKS,
                    ActivityManager.RECENT_IGNORE_UNAVAILABLE,
                )?.mapNotNull { taskInfo ->
                    val packageName = taskInfo.baseActivity?.packageName ?: return@mapNotNull null
                    // Skip our own app in the recents list
                    if (packageName == context.packageName) return@mapNotNull null
                    val icon = try {
                        pm.getApplicationIcon(packageName)
                    } catch (e: PackageManager.NameNotFoundException) {
                        null
                    }
                    val label = try {
                        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
                    } catch (e: PackageManager.NameNotFoundException) {
                        packageName
                    }
                    RecentTask(
                        id = taskInfo.id,
                        label = label,
                        packageName = packageName,
                        icon = icon,
                        thumbnail = null, // Thumbnails require system-level permission
                    )
                } ?: emptyList()
            } catch (e: SecurityException) {
                emptyList()
            }

            _uiState.update { state -> state.copy(recentTasks = tasks) }
        }
    }

    private fun loadRecentFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            val files = mutableListOf<RecentFile>()
            val projection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
            )
            val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${
                RECENT_FILE_MIME_TYPES.joinToString { "'$it'" }
            })"

            try {
                val cursor = context.contentResolver.query(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Files.getContentUri("external")
                    },
                    projection,
                    selection,
                    null,
                    sortOrder,
                )
                cursor?.use {
                    val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val mimeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                    val modifiedCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                    var count = 0
                    while (it.moveToNext() && count < MAX_RECENT_FILES) {
                        val id = it.getLong(idCol)
                        val name = it.getString(nameCol) ?: continue
                        val mime = it.getString(mimeCol) ?: continue
                        val modified = it.getLong(modifiedCol)
                        val contentUri = Uri.withAppendedPath(
                            MediaStore.Files.getContentUri("external"),
                            id.toString(),
                        )
                        files.add(
                            RecentFile(
                                name = name,
                                mimeType = mime,
                                uri = contentUri,
                                thumbnail = null,
                                modifiedTime = modified,
                            ),
                        )
                        count++
                    }
                }
            } catch (e: Exception) {
                // Permission not granted or query failed — return empty
            }

            _uiState.update { state -> state.copy(recentFiles = files) }
        }
    }

    fun launchApp(appInfo: AppInfo) {
        val intent = appInfo.launchIntent?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        } ?: return
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // App not found or launch failed
        }
    }

    fun launchSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No browser available
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
            ?: Intent("com.google.android.googlequicksearchbox.VOICE_SEARCH_ACTION").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // Voice search not available
        }
    }

    fun launchLensSearch() {
        val lensIntent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.google.ar.lens")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("intent://lens/#Intent;scheme=googlelens;end")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        try {
            context.startActivity(lensIntent)
        } catch (e: Exception) {
            // Lens not available
        }
    }

    fun openRecentFile(recentFile: RecentFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(recentFile.uri, recentFile.mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            // No app to handle this file type
        }
    }

    private companion object {
        const val MAX_RECENT_TASKS = 10
        const val MAX_RECENT_FILES = 6

        /**
         * Preferred packages for the 5 dock / hotseat slots, tried in order.
         * Matches the stock Pixel Launcher default: Phone, Messages, Chrome,
         * Camera, Maps — with fallbacks to other common Google apps.
         */
        val DOCK_PACKAGE_PRIORITY = listOf(
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.android.chrome",
            "com.google.android.GoogleCamera",
            "com.google.android.apps.maps",
            "com.google.android.googlequicksearchbox",
            "com.google.android.gm",
            "com.android.vending",
            "com.google.android.youtube",
            "com.google.android.apps.photos",
        )

        val RECENT_FILE_MIME_TYPES = listOf(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "image/jpeg",
            "image/png",
            "text/plain",
        )
    }
}
