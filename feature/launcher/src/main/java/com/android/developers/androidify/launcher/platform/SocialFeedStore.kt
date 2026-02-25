package com.android.developers.androidify.launcher.platform

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton store for social notification feed items.
 *
 * Fed by the [NotificationListenerService] in the app module — social app
 * notifications are extracted and categorized here for the Social Hub view.
 * The Notes Role service also writes note snippets here.
 */
data class SocialFeedItem(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val category: SocialCategory,
)

enum class SocialCategory {
    /** Direct messages, chat */
    Message,
    /** Social media posts, stories, mentions */
    Social,
    /** Email */
    Email,
    /** Notes captured via Notes Role */
    Note,
    /** Everything else from social apps */
    Other,
}

object SocialFeedStore {
    private const val MAX_ITEMS = 50

    private val _items = MutableStateFlow<List<SocialFeedItem>>(emptyList())
    val items: StateFlow<List<SocialFeedItem>> = _items.asStateFlow()

    /** Replace the entire feed (called on full notification rebuild). */
    fun update(items: List<SocialFeedItem>) {
        _items.value = items.take(MAX_ITEMS)
    }

    /** Append a single item (e.g. from Notes Role). */
    fun addItem(item: SocialFeedItem) {
        _items.value = (listOf(item) + _items.value).take(MAX_ITEMS)
    }

    /** Social apps we track — package names mapped to app labels. */
    val SOCIAL_PACKAGES = mapOf(
        // Messaging
        "com.google.android.apps.messaging" to "Messages",
        "com.whatsapp" to "WhatsApp",
        "org.telegram.messenger" to "Telegram",
        "com.discord" to "Discord",
        "com.Slack" to "Slack",
        "com.facebook.orca" to "Messenger",
        // Social media
        "com.instagram.android" to "Instagram",
        "com.twitter.android" to "X",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.linkedin.android" to "LinkedIn",
        "com.reddit.frontpage" to "Reddit",
        "com.snapchat.android" to "Snapchat",
        "com.facebook.katana" to "Facebook",
        "com.tumblr" to "Tumblr",
        "com.pinterest" to "Pinterest",
        "org.thoughtcrime.securesms" to "Signal",
        // Email
        "com.google.android.gm" to "Gmail",
        "com.microsoft.office.outlook" to "Outlook",
        "com.yahoo.mobile.client.android.mail" to "Yahoo Mail",
    )

    /** Categorize a package into a social category. */
    fun categorize(packageName: String): SocialCategory = when (packageName) {
        "com.google.android.apps.messaging",
        "com.whatsapp",
        "org.telegram.messenger",
        "com.discord",
        "com.Slack",
        "com.facebook.orca",
        "org.thoughtcrime.securesms",
        -> SocialCategory.Message

        "com.google.android.gm",
        "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail",
        -> SocialCategory.Email

        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.linkedin.android",
        "com.reddit.frontpage",
        "com.snapchat.android",
        "com.facebook.katana",
        "com.tumblr",
        "com.pinterest",
        -> SocialCategory.Social

        else -> SocialCategory.Other
    }
}
