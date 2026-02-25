package com.android.developers.androidify.launcher.ui

import android.text.format.DateUtils
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.android.developers.androidify.launcher.platform.SocialCategory
import com.android.developers.androidify.launcher.platform.SocialFeedItem
import com.android.developers.androidify.launcher.platform.SocialFeedStore

private val categoryColors = mapOf(
    SocialCategory.Message to Color(0xFF4CAF50),
    SocialCategory.Social to Color(0xFF2196F3),
    SocialCategory.Email to Color(0xFFFF9800),
    SocialCategory.Note to Color(0xFFE91E63),
    SocialCategory.Other to Color(0xFF9E9E9E),
)

/**
 * Real Social Hub screen — replaces the mock. Reads live notification data
 * from [SocialFeedStore] which is populated by the NotificationListenerService.
 *
 * Shows filter chips (All / Messages / Social / Email / Notes) and a scrollable
 * feed of notification cards sorted by time.
 */
@Composable
fun SocialHubScreen(selected: Boolean) {
    val feedItems by SocialFeedStore.items.collectAsState()
    var activeFilter by remember { mutableStateOf<SocialCategory?>(null) }

    val filteredItems = if (activeFilter == null) {
        feedItems
    } else {
        feedItems.filter { it.category == activeFilter }
    }

    // Entrance animation
    val contentAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.7f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "socialAlpha",
    )
    val contentScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.95f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMediumLow),
        label = "socialScale",
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0D1B2A),
                        Color(0xFF1B2838),
                        Color(0xFF0D1B2A),
                    ),
                ),
            )
            .graphicsLayer {
                alpha = contentAlpha
                scaleX = contentScale
                scaleY = contentScale
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Swipe indicator
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
                .height(4.dp)
                .fillMaxWidth(0.12f)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.4f)),
        )

        Text(
            "Social Hub",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(4.dp))

        // Category count summary
        val msgCount = feedItems.count { it.category == SocialCategory.Message }
        val socialCount = feedItems.count { it.category == SocialCategory.Social }
        val emailCount = feedItems.count { it.category == SocialCategory.Email }
        Text(
            text = "${feedItems.size} updates · $msgCount messages · $socialCount social · $emailCount email",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.6f),
        )

        Spacer(Modifier.height(12.dp))

        // Filter chips
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FilterChipItem("All", activeFilter == null) { activeFilter = null }
            FilterChipItem("Messages", activeFilter == SocialCategory.Message) {
                activeFilter = SocialCategory.Message
            }
            FilterChipItem("Social", activeFilter == SocialCategory.Social) {
                activeFilter = SocialCategory.Social
            }
            FilterChipItem("Email", activeFilter == SocialCategory.Email) {
                activeFilter = SocialCategory.Email
            }
        }

        Spacer(Modifier.height(12.dp))

        // Google Discover-style card — barebones placeholder
        // The real implementation would bind to Google's LauncherOverlay service
        // or fetch from a content provider / RSS feed.
        DiscoverCard()

        Spacer(Modifier.height(12.dp))

        if (filteredItems.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (feedItems.isEmpty()) "No notifications yet" else "No ${activeFilter?.name?.lowercase()} notifications",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.5f),
                    )
                    if (feedItems.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Grant notification access in Settings\nto see your social feed here",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.35f),
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filteredItems, key = { it.id }) { item ->
                    SocialFeedCard(item = item)
                }
            }
        }

        // Swipe-up hint
        Text(
            text = "Swipe up to go home",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.45f),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
        )
    }
}

@Composable
private fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(label, style = MaterialTheme.typography.labelMedium)
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = Color.White.copy(alpha = 0.08f),
            labelColor = Color.White.copy(alpha = 0.7f),
            selectedContainerColor = Color(0xFF4A80F5),
            selectedLabelColor = Color.White,
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color.White.copy(alpha = 0.15f),
            selectedBorderColor = Color.Transparent,
            enabled = true,
            selected = selected,
        ),
    )
}

@Composable
private fun SocialFeedCard(item: SocialFeedItem) {
    val catColor = categoryColors[item.category] ?: Color.Gray

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.08f),
        ),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            // Category color dot
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(catColor),
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.appLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = catColor.copy(alpha = 0.9f),
                    )
                    Text(
                        text = DateUtils.getRelativeTimeSpanString(
                            item.timestamp,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE,
                        ).toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.4f),
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * Barebones Google Discover-style card. In production this would bind to
 * the Google app's LauncherOverlay service (com.google.android.googlequicksearchbox)
 * or fetch from Google News RSS / Discover API.
 *
 * For now it shows a static teaser to prove the integration point works.
 */
@Composable
private fun DiscoverCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f),
        ),
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Google "G" colored indicator
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    Color(0xFF4285F4),
                                    Color(0xFFEA4335),
                                    Color(0xFFFBBC05),
                                    Color(0xFF34A853),
                                ),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "G",
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Discover",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "Feed",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.4f),
                )
            }
            Spacer(Modifier.height(12.dp))

            // Placeholder feed items
            DiscoverFeedItem(
                source = "Android Developers",
                headline = "What's new in Android 16",
                timeAgo = "2h ago",
            )
            Spacer(Modifier.height(8.dp))
            DiscoverFeedItem(
                source = "Google Blog",
                headline = "Gemini integration across Google apps",
                timeAgo = "4h ago",
            )
            Spacer(Modifier.height(8.dp))
            DiscoverFeedItem(
                source = "9to5Google",
                headline = "Pixel feature drop: new launcher customization",
                timeAgo = "6h ago",
            )
        }
    }
}

@Composable
private fun DiscoverFeedItem(
    source: String,
    headline: String,
    timeAgo: String,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable { /* TODO: open article */ }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = source,
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF8AB4F8),
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = headline,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = timeAgo,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.35f),
        )
    }
}
