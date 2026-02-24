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
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.content.Intent
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
import com.android.developers.androidify.launcher.data.LauncherShortcut
import com.android.developers.androidify.launcher.data.RecentFile
import com.android.developers.androidify.launcher.data.RecentTask
import com.android.developers.androidify.launcher.platform.LauncherAppsRepository
import com.android.developers.androidify.launcher.platform.LauncherLayoutStore
import com.android.developers.androidify.launcher.platform.LauncherWidgetHostController
import com.android.developers.androidify.launcher.platform.NotificationDotsStore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LauncherUiState(
    val allApps: List<AppInfo> = emptyList(),
    val filteredApps: List<AppInfo> = emptyList(),
    val dockApps: List<AppInfo> = emptyList(),
    val suggestedApp: AppInfo? = null,
    val recentTasks: List<RecentTask> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val chromeTabs: List<ChromeTab> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val pinnedWidgetIds: Set<Int> = emptySet(),
)

@HiltViewModel
class LauncherViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val launcherAppsRepository: LauncherAppsRepository,
    private val launcherLayoutStore: LauncherLayoutStore,
    private val launcherWidgetHostController: LauncherWidgetHostController,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LauncherUiState())
    val uiState: StateFlow<LauncherUiState> = _uiState.asStateFlow()

    private val mediaContentObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            loadRecentFiles()
        }
    }

    init {
        loadRecentTasks()
        loadRecentFiles()
        observeApps()
        observePinnedWidgets()
        context.contentResolver.registerContentObserver(
            MediaStore.Files.getContentUri("external"),
            true,
            mediaContentObserver,
        )
    }

    override fun onCleared() {
        super.onCleared()
        context.contentResolver.unregisterContentObserver(mediaContentObserver)
        launcherWidgetHostController.stopListening()
    }

    fun startWidgetHost() {
        launcherWidgetHostController.startListening()
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

    fun launchApp(appInfo: AppInfo) {
        launcherAppsRepository.launchMainActivity(appInfo)
    }

    fun loadShortcutsForPackage(packageName: String, onResult: (List<LauncherShortcut>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            onResult(launcherAppsRepository.getShortcuts(packageName))
        }
    }

    fun launchShortcut(shortcut: LauncherShortcut) {
        launcherAppsRepository.startShortcut(shortcut)
    }

    fun pinShortcut(shortcut: LauncherShortcut) {
        launcherAppsRepository.pinShortcuts(shortcut.packageName, shortcut.user, listOf(shortcut.id))
    }

    fun requestPinWidget(providerInfo: AppWidgetProviderInfo): Boolean =
        launcherWidgetHostController.requestPinAppWidget(providerInfo.provider)

    fun addWidget(widgetId: Int) {
        viewModelScope.launch {
            launcherLayoutStore.addPinnedWidgetId(widgetId)
        }
    }

    fun launchWallpaperPicker() {
        val wallpaperIntent = Intent(Intent.ACTION_SET_WALLPAPER).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(wallpaperIntent) }
    }

    fun launchSearch(query: String) {
        val intent = Intent(Intent.ACTION_WEB_SEARCH).apply {
            putExtra("query", query)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(intent) }
    }

    fun launchVoiceSearch() {
        val intent = Intent("android.speech.action.VOICE_SEARCH_HANDS_FREE").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { context.startActivity(intent) }
    }

    fun launchLensSearch() {
        val lensIntent = Intent(Intent.ACTION_VIEW).apply {
            setPackage("com.google.ar.lens")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }.takeIf { it.resolveActivity(context.packageManager) != null }
            ?: Intent(Intent.ACTION_VIEW, Uri.parse("intent://lens/#Intent;scheme=googlelens;end")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        runCatching { context.startActivity(lensIntent) }
    }

    fun openRecentFile(recentFile: RecentFile) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(recentFile.uri, recentFile.mimeType)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(intent) }
    }

    private fun observeApps() {
        viewModelScope.launch {
            combine(
                launcherAppsRepository.apps,
                NotificationDotsStore.counts,
            ) { apps, notificationCounts ->
                apps.map { it.copy(notificationCount = notificationCounts[it.packageName] ?: 0) }
            }.collect { apps ->
                val dockApps = DOCK_PACKAGE_PRIORITY.mapNotNull { pkg -> apps.find { it.packageName == pkg } }.take(3)
                val suggestedApp = SUGGESTED_PACKAGE_PRIORITY
                    .mapNotNull { pkg -> apps.find { it.packageName == pkg } }
                    .firstOrNull { it !in dockApps }
                _uiState.update { state ->
                    val filtered = if (state.searchQuery.isBlank()) {
                        apps
                    } else {
                        apps.filter {
                            it.label.contains(state.searchQuery, ignoreCase = true) ||
                                it.packageName.contains(state.searchQuery, ignoreCase = true)
                        }
                    }
                    state.copy(
                        allApps = apps,
                        filteredApps = filtered,
                        dockApps = dockApps,
                        suggestedApp = suggestedApp,
                        isLoading = false,
                    )
                }
            }
        }
    }

    private fun observePinnedWidgets() {
        viewModelScope.launch {
            launcherLayoutStore.pinnedWidgetIds.collect { ids ->
                _uiState.update { it.copy(pinnedWidgetIds = ids) }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun loadRecentTasks() {
        viewModelScope.launch(Dispatchers.IO) {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val pm = context.packageManager
            val tasks = try {
                activityManager.getRecentTasks(MAX_RECENT_TASKS, ActivityManager.RECENT_IGNORE_UNAVAILABLE)
                    .mapNotNull { recentTaskInfo ->
                        val packageName = recentTaskInfo.baseIntent?.component?.packageName ?: return@mapNotNull null
                        val app = _uiState.value.allApps.find { it.packageName == packageName }
                        RecentTask(
                            id = recentTaskInfo.id,
                            label = app?.label ?: packageName,
                            packageName = packageName,
                            icon = app?.icon ?: pm.getApplicationIcon(packageName),
                            thumbnail = null,
                        )
                    }
            } catch (_: Exception) {
                emptyList()
            }
            _uiState.update { it.copy(recentTasks = tasks) }
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
            val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} IN (${RECENT_FILE_MIME_TYPES.joinToString { "'$it'" }})"
            val cursor = runCatching {
                context.contentResolver.query(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                    } else {
                        MediaStore.Files.getContentUri("external")
                    },
                    projection,
                    selection,
                    null,
                    "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC",
                )
            }.getOrNull()
            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val modifiedCol = it.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)
                while (it.moveToNext() && files.size < MAX_RECENT_FILES) {
                    val id = it.getLong(idCol)
                    val name = it.getString(nameCol) ?: continue
                    val mime = it.getString(mimeCol) ?: continue
                    files.add(
                        RecentFile(
                            name = name,
                            mimeType = mime,
                            uri = Uri.withAppendedPath(MediaStore.Files.getContentUri("external"), id.toString()),
                            thumbnail = null,
                            modifiedTime = it.getLong(modifiedCol),
                        ),
                    )
                }
            }
            _uiState.update { it.copy(recentFiles = files) }
        }
    }

    private companion object {
        const val MAX_RECENT_TASKS = 10
        const val MAX_RECENT_FILES = 6
        val DOCK_PACKAGE_PRIORITY = listOf(
            "com.android.chrome",
            "com.google.android.googlequicksearchbox",
            "com.google.android.dialer",
            "com.google.android.apps.messaging",
            "com.google.android.gm",
            "com.android.vending",
        )
        val SUGGESTED_PACKAGE_PRIORITY = listOf(
            "com.google.android.apps.maps",
            "com.google.android.youtube",
            "com.google.android.apps.photos",
            "com.google.android.apps.docs",
            "com.spotify.music",
            "com.instagram.android",
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
