package com.android.developers.androidify

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.android.developers.androidify.launcher.platform.NotificationDotsStore
import com.android.developers.androidify.launcher.platform.SocialCategory
import com.android.developers.androidify.launcher.platform.SocialFeedItem
import com.android.developers.androidify.launcher.platform.SocialFeedStore

class LauncherNotificationListenerService : NotificationListenerService() {
    override fun onListenerConnected() {
        super.onListenerConnected()
        val notifications = activeNotifications.orEmpty()
        rebuildCounts(notifications)
        rebuildSocialFeed(notifications)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notifications = activeNotifications.orEmpty()
        rebuildCounts(notifications)
        rebuildSocialFeed(notifications)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val notifications = activeNotifications.orEmpty()
        rebuildCounts(notifications)
        rebuildSocialFeed(notifications)
    }

    private fun rebuildCounts(notifications: Array<out StatusBarNotification>) {
        val counts = notifications
            .filter { !it.isOngoing }
            .groupingBy { it.packageName }
            .eachCount()
        NotificationDotsStore.update(counts)
    }

    private fun rebuildSocialFeed(notifications: Array<out StatusBarNotification>) {
        val socialItems = notifications
            .filter { !it.isOngoing && it.packageName in SocialFeedStore.SOCIAL_PACKAGES }
            .sortedByDescending { it.postTime }
            .mapNotNull { sbn ->
                val extras = sbn.notification.extras
                val title = extras.getCharSequence("android.title")?.toString() ?: return@mapNotNull null
                val text = extras.getCharSequence("android.text")?.toString() ?: ""
                val appLabel = SocialFeedStore.SOCIAL_PACKAGES[sbn.packageName] ?: sbn.packageName
                SocialFeedItem(
                    id = sbn.key,
                    packageName = sbn.packageName,
                    appLabel = appLabel,
                    title = title,
                    text = text,
                    timestamp = sbn.postTime,
                    category = SocialFeedStore.categorize(sbn.packageName),
                )
            }
        SocialFeedStore.update(socialItems)
    }
}
