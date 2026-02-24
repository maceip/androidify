package com.android.developers.androidify.launcher.platform

import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.os.Build
import android.os.Process
import android.os.UserHandle
import com.android.developers.androidify.launcher.data.AppInfo
import com.android.developers.androidify.launcher.data.LauncherShortcut
import com.android.developers.androidify.launcher.data.toLauncherShortcut
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class LauncherAppsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(android.os.UserManager::class.java)

    private val _apps = MutableStateFlow(emptyList<AppInfo>())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()


    private val callback = object : LauncherApps.Callback() {
        override fun onPackageAdded(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackageRemoved(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackageChanged(packageName: String, user: UserHandle) = reloadApps()
        override fun onPackagesAvailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reloadApps()
        override fun onPackagesUnavailable(packageNames: Array<String>, user: UserHandle, replacing: Boolean) = reloadApps()
        override fun onPackagesSuspended(packageNames: Array<String>, user: UserHandle) = reloadApps()
        override fun onPackagesUnsuspended(packageNames: Array<String>, user: UserHandle) = reloadApps()
        override fun onShortcutsChanged(packageName: String, shortcuts: MutableList<android.content.pm.ShortcutInfo>, user: UserHandle) = reloadApps()
    }

    init {
        launcherApps.registerCallback(callback)
        reloadApps()
    }


    fun getProfiles(): List<UserHandle> = userManager.userProfiles.toList()

    fun reloadApps() {
        val pm = context.packageManager
        val profiles = getProfiles()
        val apps = profiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                AppInfo(
                    packageName = info.applicationInfo.packageName,
                    className = info.name,
                    label = info.label?.toString() ?: info.applicationInfo.packageName,
                    icon = info.getIcon(0),
                    launchIntent = pm.getLaunchIntentForPackage(info.applicationInfo.packageName),
                    user = user,
                    isWorkProfile = user != Process.myUserHandle(),
                )
            }
        }.sortedBy { it.label.lowercase() }
            .distinctBy { "${it.user.hashCode()}-${it.packageName}-${it.className}" }
        _apps.value = apps
    }

    fun launchMainActivity(appInfo: AppInfo) {
        runCatching {
            launcherApps.startMainActivity(
                ComponentName(appInfo.packageName, appInfo.className),
                appInfo.user,
                null,
                null,
            )
        }.getOrElse {
            appInfo.launchIntent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)?.let(context::startActivity)
        }
    }

    fun getShortcuts(packageName: String): List<LauncherShortcut> {
        val profiles = getProfiles()
        return profiles.flatMap { user ->
            val query = ShortcutQuery().apply {
                setPackage(packageName)
                setQueryFlags(
                    ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        ShortcutQuery.FLAG_MATCH_MANIFEST or
                        ShortcutQuery.FLAG_MATCH_PINNED,
                )
            }
            launcherApps.getShortcuts(query, user).orEmpty().map { it.toLauncherShortcut() }
        }.sortedBy { it.rank }
    }

    fun startShortcut(shortcut: LauncherShortcut) {
        launcherApps.startShortcut(
            shortcut.packageName,
            shortcut.id,
            null,
            null,
            shortcut.user,
        )
    }

    fun pinShortcuts(packageName: String, user: UserHandle, ids: List<String>) {
        launcherApps.pinShortcuts(packageName, ids, user)
    }


    fun getAppUsageLimit(packageName: String, user: UserHandle): LauncherApps.AppUsageLimit? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return null
        return launcherApps.getAppUsageLimit(packageName, user)
    }

    fun shouldHideFromSuggestions(packageName: String, user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return launcherApps.shouldHideFromSuggestions(packageName, user)
    }
}
