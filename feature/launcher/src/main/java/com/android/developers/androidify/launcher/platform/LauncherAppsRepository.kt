package com.android.developers.androidify.launcher.platform

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.LauncherApps.ShortcutQuery
import android.content.res.Resources
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.InsetDrawable
import android.graphics.drawable.LayerDrawable
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
        // Reload icons when wallpaper colors change so themed tints update
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                WallpaperManager.getInstance(context).addOnColorsChangedListener(
                    { _, _ -> reloadApps() },
                    android.os.Handler(android.os.Looper.getMainLooper()),
                )
            } catch (_: Exception) { }
        }
    }


    fun getProfiles(): List<UserHandle> = userManager.userProfiles.toList()

    fun reloadApps() {
        val pm = context.packageManager
        val displayDensity = Resources.getSystem().displayMetrics.densityDpi
        val profiles = getProfiles()
        val apps = profiles.flatMap { user ->
            launcherApps.getActivityList(null, user).map { info ->
                val icon = loadThemedIconOrFallback(info, displayDensity)
                AppInfo(
                    packageName = info.applicationInfo.packageName,
                    className = info.name,
                    label = info.label?.toString() ?: info.applicationInfo.packageName,
                    icon = icon,
                    launchIntent = pm.getLaunchIntentForPackage(info.applicationInfo.packageName),
                    user = user,
                    isWorkProfile = user != Process.myUserHandle(),
                )
            }
        }.sortedBy { it.label.lowercase() }
            .distinctBy { "${it.user.hashCode()}-${it.packageName}-${it.className}" }
        _apps.value = apps
    }

    /**
     * Loads the icon for an activity. On API 33+, if the icon declares a
     * monochrome layer, extracts it and tints it with wallpaper-derived colors
     * to match the Material You "themed icons" feature from Pixel Launcher.
     *
     * The tinting logic:
     *  - Background: a light/pastel version of the wallpaper primary color
     *  - Foreground: the monochrome alpha mask tinted with the wallpaper primary color
     */
    private fun loadThemedIconOrFallback(
        info: android.content.pm.LauncherActivityInfo,
        displayDensity: Int,
    ): Drawable? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val icon = info.getIcon(displayDensity)
                if (icon is AdaptiveIconDrawable) {
                    val mono = icon.monochrome
                    if (mono != null) {
                        return createThemedIcon(mono) ?: icon
                    }
                }
                return icon
            } catch (_: Exception) {
                // Fall through to basic icon loading
            }
        }
        return info.getIcon(0)
    }

    /**
     * Creates a themed icon by tinting [monoDrawable] with wallpaper colors.
     * Returns an [AdaptiveIconDrawable] with colored background + tinted mono foreground,
     * or null if wallpaper colors aren't available.
     */
    private fun createThemedIcon(monoDrawable: Drawable): Drawable? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null

        val colors = try {
            WallpaperManager.getInstance(context).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
        } catch (_: Exception) {
            null
        } ?: return null

        val primary = colors.primaryColor.toArgb()

        // Derive background: lighten the primary color significantly for the
        // pastel background that Pixel Launcher uses behind monochrome icons.
        val bgColor = blendWithWhite(primary, 0.78f)
        // Foreground tint: the primary color itself (dark on light bg).
        val fgColor = darken(primary, 0.6f)

        // Tint the monochrome drawable
        val tintedMono = monoDrawable.mutate().apply {
            colorFilter = BlendModeColorFilter(fgColor, BlendMode.SRC_IN)
        }

        // Wrap in an AdaptiveIconDrawable shape: colored bg + tinted mono fg
        return AdaptiveIconDrawable(
            ColorDrawable(bgColor),
            InsetDrawable(tintedMono, 0.1f),
        )
    }

    /** Blend [color] toward white by [ratio] (0 = original, 1 = white). */
    private fun blendWithWhite(color: Int, ratio: Float): Int {
        val r = ((color shr 16 and 0xFF) + ((255 - (color shr 16 and 0xFF)) * ratio)).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) + ((255 - (color shr 8 and 0xFF)) * ratio)).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) + ((255 - (color and 0xFF)) * ratio)).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }

    /** Darken [color] by [factor] (0 = black, 1 = original). */
    private fun darken(color: Int, factor: Float): Int {
        val r = ((color shr 16 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) * factor).toInt().coerceIn(0, 255)
        val b = ((color and 0xFF) * factor).toInt().coerceIn(0, 255)
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
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
        // Shortcut access requires being the default launcher. If the user hasn't
        // set us as default yet, the system throws SecurityException.
        return try {
            val profiles = getProfiles()
            profiles.flatMap { user ->
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
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun startShortcut(shortcut: LauncherShortcut) {
        try {
            launcherApps.startShortcut(
                shortcut.packageName,
                shortcut.id,
                null,
                null,
                shortcut.user,
            )
        } catch (_: SecurityException) {
            // Not the default launcher — shortcut access denied
        }
    }

    fun pinShortcuts(packageName: String, user: UserHandle, ids: List<String>) {
        try {
            launcherApps.pinShortcuts(packageName, ids, user)
        } catch (_: SecurityException) {
            // Not the default launcher
        }
    }

    fun shouldHideFromSuggestions(packageName: String, user: UserHandle): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            launcherApps.shouldHideFromSuggestions(packageName, user)
        } catch (_: SecurityException) {
            false
        }
    }
}
