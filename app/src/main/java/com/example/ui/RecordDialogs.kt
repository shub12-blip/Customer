package com.example.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.AicsmUrls
import com.example.data.BookmarkCategory
import com.example.data.PortalRecord
import com.example.data.RecordCategory
import com.example.data.RecordStatus

@Composable
fun RecordEditorDialog(
    existingRecord: PortalRecord?,
    defaultCategory: RecordCategory = RecordCategory.FRANCHISE_ENQUIRY,
    defaultUrl: String = AicsmUrls.FRANCHISE_ENQUIRY,
    onDismiss: () -> Unit,
    onConfirm: (
        title: String,
        category: RecordCategory,
        status: RecordStatus,
        details: String,
        linkedUrl: String
    ) -> Unit
) {
    var title by remember(existingRecord) { mutableStateOf(existingRecord?.title ?: "") }
    var category by remember(existingRecord) {
        mutableStateOf(existingRecord?.categoryEnum() ?: defaultCategory)
    }
    var status by remember(existingRecord) {
        mutableStateOf(existingRecord?.statusEnum() ?: RecordStatus.DRAFT)
    }
    var details by remember(existingRecord) { mutableStateOf(existingRecord?.details ?: "") }
    var linkedUrl by remember(existingRecord) {
        mutableStateOf(existingRecord?.linkedUrl ?: defaultUrl)
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = if (existingRecord == null) "New Cloud-Synced Record" else "Edit Synced Record",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 100) title = it },
                    label = { Text("Title (${title.trim().length}/100)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_title_input")
                )

                Text("Category", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordCategory.entries.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = {
                                category = option
                                if (existingRecord == null) {
                                    linkedUrl = if (option == RecordCategory.FRANCHISE_ENQUIRY) {
                                        AicsmUrls.FRANCHISE_ENQUIRY
                                    } else {
                                        AicsmUrls.HOME_PORTAL
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = when (option) {
                                        RecordCategory.FRANCHISE_ENQUIRY -> "Franchise"
                                        RecordCategory.COURSE_NOTE -> "Course"
                                        RecordCategory.DOCUMENT_CHECKLIST -> "Checklist"
                                    },
                                    maxLines = 1
                                )
                            }
                        )
                    }
                }

                Text("Status", style = MaterialTheme.typography.labelMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    RecordStatus.entries.forEach { option ->
                        FilterChip(
                            selected = status == option,
                            onClick = { status = option },
                            label = { Text(option.label, maxLines = 1) }
                        )
                    }
                }

                OutlinedTextField(
                    value = details,
                    onValueChange = { if (it.length <= 500) details = it },
                    label = { Text("Notes / Details (${details.length}/500)") },
                    minLines = 3,
                    maxLines = 4,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_details_input")
                )

                OutlinedTextField(
                    value = linkedUrl,
                    onValueChange = { if (it.length <= 300) linkedUrl = it },
                    label = { Text("Linked AICSM URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("record_url_input")
                )

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanUrl = linkedUrl.trim()
                    when {
                        cleanTitle.isEmpty() -> validationError = "Please enter a title."
                        !AicsmUrls.isValidAicsmUrl(cleanUrl) ->
                            validationError = "URL must start with https://www.aicsm.com or https://aicsm.com"
                        else -> onConfirm(cleanTitle, category, status, details.trim(), cleanUrl)
                    }
                },
                modifier = Modifier.testTag("save_record_button")
            ) {
                Text("Save to Cloud")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BookmarkEditorDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, url: String, category: BookmarkCategory) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf(AicsmUrls.HOME_PORTAL) }
    var category by remember { mutableStateOf(BookmarkCategory.PORTAL) }
    var validationError by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text("Save AICSM Bookmark", style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= 80) title = it },
                    label = { Text("Bookmark Name") },
                    placeholder = { Text("e.g., Franchise Enquiry Form") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bookmark_title_input")
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = { if (it.length <= 300) url = it },
                    label = { Text("AICSM URL") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("bookmark_url_input")
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = url == AicsmUrls.HOME_PORTAL,
                        onClick = {
                            url = AicsmUrls.HOME_PORTAL
                            if (title.isBlank()) title = "AICSM Official Portal"
                            category = BookmarkCategory.PORTAL
                        },
                        label = { Text("aicsm.com") }
                    )
                    FilterChip(
                        selected = url == AicsmUrls.FRANCHISE_ENQUIRY,
                        onClick = {
                            url = AicsmUrls.FRANCHISE_ENQUIRY
                            if (title.isBlank()) title = "Franchise Enquiry"
                            category = BookmarkCategory.FRANCHISE
                        },
                        label = { Text("Franchise Form") }
                    )
                }

                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanTitle = title.trim()
                    val cleanUrl = url.trim()
                    when {
                        cleanTitle.isEmpty() -> validationError = "Please enter a bookmark name."
                        !AicsmUrls.isValidAicsmUrl(cleanUrl) ->
                            validationError = "URL must start with https://www.aicsm.com or https://aicsm.com"
                        else -> onConfirm(cleanTitle, cleanUrl, category)
                    }
                },
                modifier = Modifier.testTag("save_bookmark_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
