package com.android.developers.androidify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.android.developers.androidify.launcher.platform.NotificationDotsStore

class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        rebuildCounts(activeNotifications.orEmpty())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        rebuildCounts(activeNotifications.orEmpty())
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        rebuildCounts(activeNotifications.orEmpty())
    }

    private fun rebuildCounts(notifications: Array<StatusBarNotification>) {
        val counts = notifications
            .filter { !it.isOngoing }
            .groupingBy { it.packageName }
            .eachCount()
        NotificationDotsStore.update(counts)
    }
}
