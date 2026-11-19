package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AicsmUrls
import com.example.data.PortalBookmark
import com.example.data.PortalMode
import com.example.data.PortalRecord
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.ui.theme.SyncVerifiedGreen

fun launchUrlWithPreference(
    context: Context,
    url: String,
    preferredMode: PortalMode,
    onOpenInPortal: (String) -> Unit
) {
    if (preferredMode == PortalMode.EXTERNAL_BROWSER) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }.onFailure {
            onOpenInPortal(url)
        }
    } else {
        onOpenInPortal(url)
    }
}

@Composable
fun HomeScreen(
    profileState: UiState<UserProfile?>,
    recordsState: UiState<List<PortalRecord>>,
    bookmarksState: UiState<List<PortalBookmark>>,
    onOpenInPortal: (String) -> Unit,
    onNavigateTab: (PortalTab) -> Unit,
    onQuickAddRecord: () -> Unit,
    onQuickAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val preferredMode = profile?.portalModeEnum() ?: PortalMode.IN_APP
    val displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: "AICSM Member"

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 720.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1080.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Hero Portal Card
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(if (isWide) 220.dp else 184.dp)
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.img_aicsm_hero),
                                contentDescription = stringResource(R.string.cd_hero_banner),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color(0x330B1324),
                                                Color(0xDD0B1324)
                                            )
                                        )
                                    )
                            )
                            Surface(
                                color = SyncVerifiedGreen.copy(alpha = 0.92f),
                                shape = RoundedCornerShape(50.dp),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDone,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "Cloud Synced",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "Welcome back, $displayName",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "AICSM Responsive Web Portal • www.aicsm.com",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.88f)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    launchUrlWithPreference(
                                        context = context,
                                        url = AicsmUrls.HOME_PORTAL,
                                        preferredMode = preferredMode,
                                        onOpenInPortal = onOpenInPortal
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("home_open_portal_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Language,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Open aicsm.com", maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    launchUrlWithPreference(
                                        context = context,
                                        url = AicsmUrls.FRANCHISE_ENQUIRY,
                                        preferredMode = preferredMode,
                                        onOpenInPortal = onOpenInPortal
                                    )
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.secondary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .testTag("home_franchise_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Storefront,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Franchise", maxLines = 1)
                            }
                        }
                    }
                }
            }

            // Quick Services Grid (Responsive 1 or 2 columns)
            item {
                Text(
                    text = "Quick Portal Access",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            item {
                val cards = listOf(
                    QuickLinkData(
                        title = "Official AICSM Website",
                        subtitle = "Courses, student verification, results & notices on www.aicsm.com",
                        icon = Icons.Filled.School,
                        accentColor = MaterialTheme.colorScheme.primary,
                        tag = "quick_official_site",
                        onClick = {
                            launchUrlWithPreference(
                                context,
                                AicsmUrls.HOME_PORTAL,
                                preferredMode,
                                onOpenInPortal
                            )
                        }
                    ),
                    QuickLinkData(
                        title = "Authorized Center Franchise",
                        subtitle = "Interested in a training center? Submit CustomerEnquiry.php",
                        icon = Icons.Filled.Storefront,
                        accentColor = MaterialTheme.colorScheme.secondary,
                        tag = "quick_franchise_enquiry",
                        onClick = { onNavigateTab(PortalTab.FRANCHISE) }
                    ),
                    QuickLinkData(
                        title = "Cloud-Synced Records",
                        subtitle = "Manage enquiry notes, checklists & study records across devices",
                        icon = Icons.Filled.CloudSync,
                        accentColor = MaterialTheme.colorScheme.tertiary,
                        tag = "quick_cloud_records",
                        onClick = { onNavigateTab(PortalTab.SYNC) }
                    ),
                    QuickLinkData(
                        title = "Direct Franchise Form",
                        subtitle = "Open https://aicsm.com/CustomerEnquiry.php in device browser",
                        icon = Icons.Filled.VerifiedUser,
                        accentColor = SyncVerifiedGreen,
                        tag = "quick_franchise_browser",
                        onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse(AicsmUrls.FRANCHISE_ENQUIRY))
                                )
                            }
                        }
                    )
                )

                if (isWide) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        cards.chunked(2).forEach { pair ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                pair.forEach { item ->
                                    QuickServiceCard(item, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        cards.forEach { item ->
                            QuickServiceCard(item, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // Cloud-Synced Bookmarks + Recent Records
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        SyncedBookmarksSection(
                            bookmarksState = bookmarksState,
                            onOpenUrl = { url ->
                                launchUrlWithPreference(context, url, preferredMode, onOpenInPortal)
                            },
                            onAddBookmark = onQuickAddBookmark,
                            modifier = Modifier.weight(1f)
                        )
                        RecentRecordsSection(
                            recordsState = recordsState,
                            onAddRecord = onQuickAddRecord,
                            onViewAll = { onNavigateTab(PortalTab.SYNC) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        SyncedBookmarksSection(
                            bookmarksState = bookmarksState,
                            onOpenUrl = { url ->
                                launchUrlWithPreference(context, url, preferredMode, onOpenInPortal)
                            },
                            onAddBookmark = onQuickAddBookmark,
                            modifier = Modifier.fillMaxWidth()
                        )
                        RecentRecordsSection(
                            recordsState = recordsState,
                            onAddRecord = onQuickAddRecord,
                            onViewAll = { onNavigateTab(PortalTab.SYNC) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private data class QuickLinkData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val accentColor: Color,
    val tag: String,
    val onClick: () -> Unit
)

@Composable
private fun QuickServiceCard(
    data: QuickLinkData,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = data.onClick)
            .testTag(data.tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(data.accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = data.icon,
                    contentDescription = null,
                    tint = data.accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = data.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = data.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun SyncedBookmarksSection(
    bookmarksState: UiState<List<PortalBookmark>>,
    onOpenUrl: (String) -> Unit,
    onAddBookmark: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Synced Bookmarks", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = onAddBookmark,
                    modifier = Modifier.testTag("home_add_bookmark_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }

            when (bookmarksState) {
                UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                is UiState.Error -> Text(
                    text = bookmarksState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                is UiState.Success -> {
                    if (bookmarksState.data.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Save quick AICSM links to sync across your devices.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            DefaultBookmarkRow(
                                title = "AICSM Official Website",
                                url = AicsmUrls.HOME_PORTAL,
                                onClick = { onOpenUrl(AicsmUrls.HOME_PORTAL) }
                            )
                            DefaultBookmarkRow(
                                title = "Franchise Customer Enquiry",
                                url = AicsmUrls.FRANCHISE_ENQUIRY,
                                onClick = { onOpenUrl(AicsmUrls.FRANCHISE_ENQUIRY) }
                            )
                        }
                    } else {
                        bookmarksState.data.take(4).forEach { bm ->
                            DefaultBookmarkRow(
                                title = bm.title,
                                url = bm.url,
                                onClick = { onOpenUrl(bm.url) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultBookmarkRow(
    title: String,
    url: String,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Bookmark,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = url.removePrefix("https://"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun RecentRecordsSection(
    recordsState: UiState<List<PortalRecord>>,
    onAddRecord: () -> Unit,
    onViewAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent Cloud Records", style = MaterialTheme.typography.titleMedium)
                Row {
                    TextButton(
                        onClick = onAddRecord,
                        modifier = Modifier.testTag("home_add_record_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                    TextButton(onClick = onViewAll) {
                        Text("All")
                    }
                }
            }

            when (recordsState) {
                UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }

                is UiState.Error -> Text(
                    text = recordsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                is UiState.Success -> {
                    if (recordsState.data.isEmpty()) {
                        Text(
                            text = "No cloud records yet. Add a franchise enquiry note or study checklist to sync across devices.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        recordsState.data.take(3).forEach { record ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Text(
                                                text = record.statusEnum().label,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    if (record.details.isNotBlank()) {
                                        Text(
                                            text = record.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
