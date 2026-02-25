package com.android.developers.androidify.launcher.platform

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.launcherDataStore by preferencesDataStore(name = "launcher_layout")

@Singleton
class LauncherLayoutStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pinnedWidgetsKey = stringSetPreferencesKey("pinned_widget_ids")

    val pinnedWidgetIds: Flow<Set<Int>> = context.launcherDataStore.data.map { prefs ->
        prefs[pinnedWidgetsKey].orEmpty().mapNotNull { it.toIntOrNull() }.toSet()
    }

    suspend fun addPinnedWidgetId(id: Int) {
        context.launcherDataStore.edit { prefs ->
            val current = prefs[pinnedWidgetsKey].orEmpty().toMutableSet()
            current.add(id.toString())
            prefs[pinnedWidgetsKey] = current
        }
    }

    suspend fun removePinnedWidgetId(id: Int) {
        context.launcherDataStore.edit { prefs ->
            val current = prefs[pinnedWidgetsKey].orEmpty().toMutableSet()
            current.remove(id.toString())
            prefs[pinnedWidgetsKey] = current
        }
    }
}
