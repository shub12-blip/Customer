package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.auth.AuthScreen
import com.example.auth.attemptAutoSignIn
import com.example.auth.authStateFlow
import com.example.auth.signOut
import com.example.data.AicsmRepository
import com.example.data.AicsmUrls
import com.example.data.PortalRecord
import com.example.data.RecordCategory
import com.example.data.RecordStatus
import com.example.ui.BookmarkEditorDialog
import com.example.ui.CloudSyncScreen
import com.example.ui.FranchiseScreen
import com.example.ui.HomeScreen
import com.example.ui.PortalTab
import com.example.ui.PortalViewModel
import com.example.ui.RecordEditorDialog
import com.example.ui.WebPortalScreen
import com.example.ui.theme.AicsmPortalTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AicsmPortalTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope
        )
    }

    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            val viewModel: PortalViewModel = viewModel(
                key = user.uid,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY]) {
                            "APPLICATION_KEY missing from CreationExtras"
                        }
                        val databaseId = app.getString(R.string.firestore_database_id)
                        val db = FirebaseFirestore.getInstance(databaseId)
                        PortalViewModel(
                            repository = AicsmRepository(db),
                            currentUserId = user.uid,
                            initialDisplayName = user.displayName ?: "AICSM Member",
                            initialEmail = user.email ?: "member@aicsm.com"
                        )
                    }
                }
            )
            MainPortalShell(
                viewModel = viewModel,
                photoUrl = user.photoUrl?.toString(),
                userEmail = user.email ?: "member@aicsm.com",
                onSignOut = {
                    signOut(credentialManager = credentialManager, scope = scope)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPortalShell(
    viewModel: PortalViewModel,
    photoUrl: String?,
    userEmail: String,
    onSignOut: () -> Unit
) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activeWebUrl by viewModel.activeWebUrl.collectAsStateWithLifecycle()
    val portalNavigationNonce by viewModel.portalNavigationNonce.collectAsStateWithLifecycle()
    val profileState by viewModel.profileState.collectAsStateWithLifecycle()
    val recordsState by viewModel.recordsState.collectAsStateWithLifecycle()
    val bookmarksState by viewModel.bookmarksState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    var showRecordDialog by remember { mutableStateOf(false) }
    var editingRecord by remember { mutableStateOf<PortalRecord?>(null) }
    var defaultRecordCategory by remember { mutableStateOf(RecordCategory.FRANCHISE_ENQUIRY) }
    var showBookmarkDialog by remember { mutableStateOf(false) }

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    BackHandler(enabled = selectedTab != PortalTab.HOME) {
        viewModel.selectTab(PortalTab.HOME)
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            contentWindowInsets = WindowInsets.statusBars,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge
                            )
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
                            ) {
                                Text(
                                    text = selectedTab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(windowInsets = WindowInsets.navigationBars) {
                        PortalTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label,
                                        modifier = Modifier.size(22.dp)
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("nav_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        PortalTab.entries.forEach { tab ->
                            val selected = selectedTab == tab
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    Icon(
                                        imageVector = tabIcon(tab, selected),
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label, maxLines = 1) },
                                modifier = Modifier.testTag("rail_${tab.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                    when (selectedTab) {
                        PortalTab.HOME -> HomeScreen(
                            profileState = profileState,
                            recordsState = recordsState,
                            bookmarksState = bookmarksState,
                            onOpenInPortal = { url -> viewModel.openUrlInPortal(url) },
                            onNavigateTab = { tab -> viewModel.selectTab(tab) },
                            onQuickAddRecord = {
                                editingRecord = null
                                defaultRecordCategory = RecordCategory.COURSE_NOTE
                                showRecordDialog = true
                            },
                            onQuickAddBookmark = { showBookmarkDialog = true }
                        )

                        PortalTab.PORTAL -> WebPortalScreen(
                            targetUrl = activeWebUrl,
                            navigationNonce = portalNavigationNonce,
                            onSaveBookmark = { title, url, category ->
                                viewModel.createBookmark(title, url, category)
                            }
                        )

                        PortalTab.FRANCHISE -> FranchiseScreen(
                            recordsState = recordsState,
                            onOpenInPortal = { url -> viewModel.openUrlInPortal(url) },
                            onAddFranchiseRecord = {
                                editingRecord = null
                                defaultRecordCategory = RecordCategory.FRANCHISE_ENQUIRY
                                showRecordDialog = true
                            },
                            onManageSyncTab = { viewModel.selectTab(PortalTab.SYNC) }
                        )

                        PortalTab.SYNC -> CloudSyncScreen(
                            photoUrl = photoUrl,
                            fallbackEmail = userEmail,
                            profileState = profileState,
                            recordsState = recordsState,
                            bookmarksState = bookmarksState,
                            isMutating = isMutating,
                            onSaveProfile = { name, role, mode, center, city ->
                                viewModel.updateProfile(name, role, mode, center, city)
                            },
                            onAddRecord = {
                                editingRecord = null
                                defaultRecordCategory = RecordCategory.FRANCHISE_ENQUIRY
                                showRecordDialog = true
                            },
                            onEditRecord = { record ->
                                editingRecord = record
                                showRecordDialog = true
                            },
                            onArchiveRecord = { record ->
                                viewModel.updateRecord(
                                    recordId = record.id,
                                    title = record.title,
                                    category = record.categoryEnum(),
                                    status = RecordStatus.ARCHIVED,
                                    details = record.details,
                                    linkedUrl = record.linkedUrl
                                )
                            },
                            onDeleteRecord = { id -> viewModel.deleteRecord(id) },
                            onAddBookmark = { showBookmarkDialog = true },
                            onOpenUrl = { url -> viewModel.openUrlInPortal(url) },
                            onDeleteBookmark = { id -> viewModel.deleteBookmark(id) },
                            onSignOut = onSignOut
                        )
                    }
                }
            }
        }
    }

    if (showRecordDialog) {
        val currentEditing = editingRecord
        RecordEditorDialog(
            existingRecord = currentEditing,
            defaultCategory = defaultRecordCategory,
            defaultUrl = if (defaultRecordCategory == RecordCategory.FRANCHISE_ENQUIRY) {
                AicsmUrls.FRANCHISE_ENQUIRY
            } else {
                AicsmUrls.HOME_PORTAL
            },
            onDismiss = { showRecordDialog = false },
            onConfirm = { title, category, status, details, linkedUrl ->
                showRecordDialog = false
                if (currentEditing == null) {
                    viewModel.createRecord(title, category, status, details, linkedUrl)
                } else {
                    viewModel.updateRecord(
                        recordId = currentEditing.id,
                        title = title,
                        category = category,
                        status = status,
                        details = details,
                        linkedUrl = linkedUrl
                    )
                }
            }
        )
    }

    if (showBookmarkDialog) {
        BookmarkEditorDialog(
            onDismiss = { showBookmarkDialog = false },
            onConfirm = { title, url, category ->
                showBookmarkDialog = false
                viewModel.createBookmark(title, url, category)
            }
        )
    }
}

private fun tabIcon(tab: PortalTab, selected: Boolean) = when (tab) {
    PortalTab.HOME -> if (selected) Icons.Filled.Home else Icons.Outlined.Home
    PortalTab.PORTAL -> if (selected) Icons.Filled.Language else Icons.Outlined.Language
    PortalTab.FRANCHISE -> if (selected) Icons.Filled.Storefront else Icons.Outlined.Storefront
    PortalTab.SYNC -> if (selected) Icons.Filled.CloudSync else Icons.Outlined.CloudSync
}
