package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.PortalBookmark
import com.example.data.PortalMode
import com.example.data.PortalRecord
import com.example.data.RecordCategory
import com.example.data.RecordStatus
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.UserRole
import com.example.ui.theme.SyncVerifiedGreen

@Composable
fun CloudSyncScreen(
    photoUrl: String?,
    fallbackEmail: String,
    profileState: UiState<UserProfile?>,
    recordsState: UiState<List<PortalRecord>>,
    bookmarksState: UiState<List<PortalBookmark>>,
    isMutating: Boolean,
    onSaveProfile: (
        displayName: String,
        role: UserRole,
        preferredPortalMode: PortalMode,
        centerName: String?,
        city: String?
    ) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (PortalRecord) -> Unit,
    onArchiveRecord: (PortalRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onAddBookmark: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val profile = (profileState as? UiState.Success)?.data
    val preferredMode = profile?.portalModeEnum() ?: PortalMode.IN_APP
    var selectedCategoryFilter by remember { mutableStateOf<RecordCategory?>(null) }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isWide = maxWidth >= 780.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 1080.dp)
                .fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        ProfileSyncCard(
                            photoUrl = photoUrl,
                            fallbackEmail = fallbackEmail,
                            profileState = profileState,
                            isMutating = isMutating,
                            onSaveProfile = onSaveProfile,
                            onSignOut = onSignOut,
                            modifier = Modifier.weight(1f)
                        )
                        BookmarksManagerCard(
                            bookmarksState = bookmarksState,
                            onAddBookmark = onAddBookmark,
                            onOpenUrl = { url ->
                                launchUrlWithPreference(context, url, preferredMode, onOpenUrl)
                            },
                            onDeleteBookmark = onDeleteBookmark,
                            modifier = Modifier.weight(1f)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ProfileSyncCard(
                            photoUrl = photoUrl,
                            fallbackEmail = fallbackEmail,
                            profileState = profileState,
                            isMutating = isMutating,
                            onSaveProfile = onSaveProfile,
                            onSignOut = onSignOut,
                            modifier = Modifier.fillMaxWidth()
                        )
                        BookmarksManagerCard(
                            bookmarksState = bookmarksState,
                            onAddBookmark = onAddBookmark,
                            onOpenUrl = { url ->
                                launchUrlWithPreference(context, url, preferredMode, onOpenInPortal = onOpenUrl)
                            },
                            onDeleteBookmark = onDeleteBookmark,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                RecordsManagerCard(
                    recordsState = recordsState,
                    selectedFilter = selectedCategoryFilter,
                    onSelectFilter = { selectedCategoryFilter = it },
                    onAddRecord = onAddRecord,
                    onEditRecord = onEditRecord,
                    onArchiveRecord = onArchiveRecord,
                    onDeleteRecord = onDeleteRecord,
                    onOpenUrl = { url ->
                        launchUrlWithPreference(context, url, preferredMode, onOpenUrl)
                    }
                )
            }
        }
    }
}

@Composable
private fun ProfileSyncCard(
    photoUrl: String?,
    fallbackEmail: String,
    profileState: UiState<UserProfile?>,
    isMutating: Boolean,
    onSaveProfile: (
        displayName: String,
        role: UserRole,
        preferredPortalMode: PortalMode,
        centerName: String?,
        city: String?
    ) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profile = (profileState as? UiState.Success)?.data
    var displayName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.STUDENT) }
    var portalMode by remember { mutableStateOf(PortalMode.IN_APP) }
    var centerName by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }

    LaunchedEffect(profile) {
        if (profile != null) {
            displayName = profile.displayName
            role = profile.roleEnum()
            portalMode = profile.portalModeEnum()
            centerName = profile.centerName ?: ""
            city = profile.city ?: ""
        }
    }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = stringResource(R.string.cd_user_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (displayName.ifBlank { fallbackEmail }).take(1).uppercase(),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = profile?.displayName ?: "Cloud Account",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = profile?.email ?: fallbackEmail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    color = SyncVerifiedGreen.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(50.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = null,
                            tint = SyncVerifiedGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Synced",
                            style = MaterialTheme.typography.labelSmall,
                            color = SyncVerifiedGreen
                        )
                    }
                }
            }

            if (profileState is UiState.Loading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { if (it.length <= 80) displayName = it },
                    label = { Text("Full Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )

                Text("Account Role", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    UserRole.entries.forEach { option ->
                        FilterChip(
                            selected = role == option,
                            onClick = { role = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Text("Link Opening Mode", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PortalMode.entries.forEach { option ->
                        FilterChip(
                            selected = portalMode == option,
                            onClick = { portalMode = option },
                            label = { Text(option.label) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = centerName,
                        onValueChange = { if (it.length <= 100) centerName = it },
                        label = { Text("Center Name (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { if (it.length <= 80) city = it },
                        label = { Text("City (Optional)") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (displayName.trim().isNotEmpty()) {
                                onSaveProfile(
                                    displayName.trim(),
                                    role,
                                    portalMode,
                                    centerName.trim().ifBlank { null },
                                    city.trim().ifBlank { null }
                                )
                            }
                        },
                        enabled = !isMutating && displayName.trim().isNotEmpty(),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_profile_button")
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save Profile")
                    }

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.sign_out))
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarksManagerCard(
    bookmarksState: UiState<List<PortalBookmark>>,
    onAddBookmark: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDeleteBookmark: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cloud-Synced Bookmarks", style = MaterialTheme.typography.titleMedium)
                TextButton(
                    onClick = onAddBookmark,
                    modifier = Modifier.testTag("sync_add_bookmark_button")
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
                        Text(
                            text = "No bookmarks saved yet. Tap Add or bookmark any page inside the Portal tab.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        bookmarksState.data.forEach { bm ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenUrl(bm.url) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = bm.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = bm.url.removePrefix("https://"),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    IconButton(onClick = { onDeleteBookmark(bm.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = stringResource(R.string.cd_delete_record),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
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

@Composable
private fun RecordsManagerCard(
    recordsState: UiState<List<PortalRecord>>,
    selectedFilter: RecordCategory?,
    onSelectFilter: (RecordCategory?) -> Unit,
    onAddRecord: () -> Unit,
    onEditRecord: (PortalRecord) -> Unit,
    onArchiveRecord: (PortalRecord) -> Unit,
    onDeleteRecord: (String) -> Unit,
    onOpenUrl: (String) -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Synced Records & Checklists", style = MaterialTheme.typography.titleMedium)
                Button(
                    onClick = onAddRecord,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("sync_add_record_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Record")
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { onSelectFilter(null) },
                    label = { Text("All") }
                )
                RecordCategory.entries.forEach { cat ->
                    FilterChip(
                        selected = selectedFilter == cat,
                        onClick = { onSelectFilter(cat) },
                        label = { Text(cat.label) }
                    )
                }
            }

            when (recordsState) {
                UiState.Loading -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }

                is UiState.Error -> Text(
                    text = recordsState.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )

                is UiState.Success -> {
                    val filtered = if (selectedFilter == null) {
                        recordsState.data
                    } else {
                        recordsState.data.filter { it.categoryEnum() == selectedFilter }
                    }

                    if (filtered.isEmpty()) {
                        Text(
                            text = "No records found in this category. Create one to sync it securely to Firestore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        filtered.forEach { record ->
                            val isArchived = record.statusEnum() == RecordStatus.ARCHIVED
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = record.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            color = if (isArchived) {
                                                MaterialTheme.colorScheme.outlineVariant
                                            } else {
                                                MaterialTheme.colorScheme.primaryContainer
                                            },
                                            shape = RoundedCornerShape(50.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                if (isArchived) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Lock,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                                Text(
                                                    text = record.statusEnum().label,
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        text = record.categoryEnum().label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    if (record.details.isNotBlank()) {
                                        Text(
                                            text = record.details,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TextButton(onClick = { onOpenUrl(record.linkedUrl) }) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Open Link")
                                        }

                                        Row {
                                            if (!isArchived) {
                                                IconButton(onClick = { onEditRecord(record) }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Edit,
                                                        contentDescription = stringResource(R.string.cd_edit_record)
                                                    )
                                                }
                                                IconButton(onClick = { onArchiveRecord(record) }) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Archive,
                                                        contentDescription = stringResource(R.string.cd_archive_record)
                                                    )
                                                }
                                            }
                                            IconButton(onClick = { onDeleteRecord(record.id) }) {
                                                Icon(
                                                    imageVector = Icons.Filled.Delete,
                                                    contentDescription = stringResource(R.string.cd_delete_record),
                                                    tint = Color(0xFFDC2626)
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
    }
}
