package com.android.developers.androidify.launcher.platform

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val HOST_ID = 1024

@Singleton
class LauncherWidgetHostController @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val appWidgetManager = AppWidgetManager.getInstance(context)
    private val appWidgetHost = AppWidgetHost(context, HOST_ID)

    fun startListening() = appWidgetHost.startListening()
    fun stopListening() = appWidgetHost.stopListening()
    fun allocateWidgetId(): Int = appWidgetHost.allocateAppWidgetId()

    fun bindWidgetIdIfAllowed(widgetId: Int, provider: ComponentName): Boolean =
        appWidgetManager.bindAppWidgetIdIfAllowed(widgetId, provider)

    fun createHostView(widgetId: Int): AppWidgetHostView? {
        val providerInfo = appWidgetManager.getAppWidgetInfo(widgetId) ?: return null
        return appWidgetHost.createView(context, widgetId, providerInfo)
    }

    fun requestPinAppWidget(provider: ComponentName): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
        return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
}
