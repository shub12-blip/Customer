package com.example.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AicsmRepository
import com.example.data.AicsmUrls
import com.example.data.BookmarkCategory
import com.example.data.PortalBookmark
import com.example.data.PortalMode
import com.example.data.PortalRecord
import com.example.data.RecordCategory
import com.example.data.RecordStatus
import com.example.data.UiState
import com.example.data.UserProfile
import com.example.data.UserRole
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class PortalTab(val label: String) {
    HOME("Home"),
    PORTAL("Portal"),
    FRANCHISE("Franchise"),
    SYNC("Cloud Sync")
}

class PortalViewModel(
    private val repository: AicsmRepository,
    private val currentUserId: String,
    initialDisplayName: String,
    initialEmail: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(PortalTab.HOME)
    val selectedTab: StateFlow<PortalTab> = _selectedTab.asStateFlow()

    private val _activeWebUrl = MutableStateFlow(AicsmUrls.HOME_PORTAL)
    val activeWebUrl: StateFlow<String> = _activeWebUrl.asStateFlow()

    private val _portalNavigationNonce = MutableStateFlow(0)
    val portalNavigationNonce: StateFlow<Int> = _portalNavigationNonce.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    val profileState: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing profile", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val recordsState: StateFlow<UiState<List<PortalRecord>>> = repository.observeRecords(currentUserId)
        .map<List<PortalRecord>, UiState<List<PortalRecord>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing records", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync records"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val bookmarksState: StateFlow<UiState<List<PortalBookmark>>> = repository.observeBookmarks(currentUserId)
        .map<List<PortalBookmark>, UiState<List<PortalBookmark>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing bookmarks", rootError)
            emit(UiState.Error(rootError.message ?: "Unable to sync bookmarks"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val result = repository.ensureUserProfile(
                userId = currentUserId,
                displayName = initialDisplayName,
                email = initialEmail
            )
            result.onFailure { error ->
                _snackbarMessage.value = error.localizedMessage ?: "Failed to initialize cloud profile"
            }
        }
    }

    fun selectTab(tab: PortalTab) {
        _selectedTab.value = tab
    }

    fun openUrlInPortal(url: String) {
        _activeWebUrl.value = url
        _portalNavigationNonce.value += 1
        _selectedTab.value = PortalTab.PORTAL
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun updateProfile(
        displayName: String,
        role: UserRole,
        preferredPortalMode: PortalMode,
        centerName: String?,
        city: String?
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateUserProfile(
                userId = currentUserId,
                displayName = displayName,
                role = role,
                preferredPortalMode = preferredPortalMode,
                centerName = centerName,
                city = city
            )
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Cloud profile updated & synced" },
                onFailure = { it.localizedMessage ?: "Failed to update profile" }
            )
        }
    }

    fun createRecord(
        title: String,
        category: RecordCategory,
        status: RecordStatus,
        details: String,
        linkedUrl: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createRecord(
                userId = currentUserId,
                title = title,
                category = category,
                status = status,
                details = details,
                linkedUrl = linkedUrl
            )
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Record saved to cloud" },
                onFailure = { it.localizedMessage ?: "Failed to save record" }
            )
        }
    }

    fun updateRecord(
        recordId: String,
        title: String,
        category: RecordCategory,
        status: RecordStatus,
        details: String,
        linkedUrl: String
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateRecord(
                userId = currentUserId,
                recordId = recordId,
                title = title,
                category = category,
                status = status,
                details = details,
                linkedUrl = linkedUrl
            )
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Record updated in cloud" },
                onFailure = { it.localizedMessage ?: "Failed to update record" }
            )
        }
    }

    fun deleteRecord(recordId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteRecord(currentUserId, recordId)
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Record deleted from cloud" },
                onFailure = { it.localizedMessage ?: "Failed to delete record" }
            )
        }
    }

    fun createBookmark(
        title: String,
        url: String,
        category: BookmarkCategory
    ) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createBookmark(
                userId = currentUserId,
                title = title,
                url = url,
                category = category
            )
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Bookmark synced to cloud" },
                onFailure = { it.localizedMessage ?: "Failed to save bookmark" }
            )
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteBookmark(currentUserId, bookmarkId)
            _isMutating.value = false
            _snackbarMessage.value = result.fold(
                onSuccess = { "Bookmark removed" },
                onFailure = { it.localizedMessage ?: "Failed to remove bookmark" }
            )
        }
    }

    private companion object {
        const val TAG = "PortalVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
