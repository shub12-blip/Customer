package com.example.data

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class AicsmRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(expectedUserId: String): String {
        val uid = auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        check(uid == expectedUserId) { "Signed-in user changed; refusing cross-user access." }
        return uid
    }

    suspend fun ensureUserProfile(
        userId: String,
        displayName: String,
        email: String
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().ifBlank { "AICSM User" }.take(80)
                val safeEmail = email.trim().ifBlank { "member@aicsm.com" }.take(160)
                val profile = UserProfile(
                    userId = uid,
                    displayName = safeName,
                    email = safeEmail,
                    role = UserRole.STUDENT.name,
                    preferredPortalMode = PortalMode.IN_APP.name
                )
                docRef.set(profile.toCreateMap()).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId(userId)
        val docPath = "users/$uid"
        db.collection("users").document(uid)
            .snapshots()
            .map { snapshot ->
                snapshot.toObject(
                    UserProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.GET, docPath)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun updateUserProfile(
        userId: String,
        displayName: String,
        role: UserRole,
        preferredPortalMode: PortalMode,
        centerName: String?,
        city: String?
    ): Result<Unit> {
        val docPath = "users/$userId"
        return try {
            val uid = requireUserId(userId)
            val cleanName = displayName.trim()
            require(cleanName.length in 1..80) { "Display name must be 1 to 80 characters." }

            val updates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "role" to role.name,
                "preferredPortalMode" to preferredPortalMode.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            val cleanCenter = centerName?.trim()?.take(100)
            if (!cleanCenter.isNullOrEmpty()) {
                updates["centerName"] = cleanCenter
            }
            val cleanCity = city?.trim()?.take(80)
            if (!cleanCity.isNullOrEmpty()) {
                updates["city"] = cleanCity
            }

            db.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    fun observeRecords(userId: String): Flow<List<PortalRecord>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/records"
        db.collection("users").document(uid).collection("records")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PortalRecord::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createRecord(
        userId: String,
        title: String,
        category: RecordCategory,
        status: RecordStatus,
        details: String,
        linkedUrl: String
    ): Result<Unit> {
        var docPath = "users/$userId/records"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanDetails = details.trim()
            val cleanUrl = linkedUrl.trim().ifBlank { AicsmUrls.HOME_PORTAL }
            require(cleanTitle.length in 1..100) { "Title must be 1 to 100 characters." }
            require(cleanDetails.length <= 500) { "Details must be 500 characters or fewer." }
            require(AicsmUrls.isValidAicsmUrl(cleanUrl)) {
                "URL must be an official https://aicsm.com or https://www.aicsm.com link."
            }

            val docRef = db.collection("users").document(uid).collection("records").document()
            docPath = docRef.path
            val record = PortalRecord(
                title = cleanTitle,
                category = category.name,
                status = status.name,
                details = cleanDetails,
                linkedUrl = cleanUrl
            )
            docRef.set(record.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateRecord(
        userId: String,
        recordId: String,
        title: String,
        category: RecordCategory,
        status: RecordStatus,
        details: String,
        linkedUrl: String
    ): Result<Unit> {
        val docPath = "users/$userId/records/$recordId"
        return try {
            val uid = requireUserId(userId)
            require(recordId.isNotBlank()) { "Record ID must not be blank." }
            val cleanTitle = title.trim()
            val cleanDetails = details.trim()
            val cleanUrl = linkedUrl.trim().ifBlank { AicsmUrls.HOME_PORTAL }
            require(cleanTitle.length in 1..100) { "Title must be 1 to 100 characters." }
            require(cleanDetails.length <= 500) { "Details must be 500 characters or fewer." }
            require(AicsmUrls.isValidAicsmUrl(cleanUrl)) {
                "URL must be an official https://aicsm.com or https://www.aicsm.com link."
            }

            db.collection("users").document(uid).collection("records").document(recordId)
                .update(
                    mapOf(
                        "title" to cleanTitle,
                        "category" to category.name,
                        "status" to status.name,
                        "details" to cleanDetails,
                        "linkedUrl" to cleanUrl,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteRecord(userId: String, recordId: String): Result<Unit> {
        val docPath = "users/$userId/records/$recordId"
        return try {
            val uid = requireUserId(userId)
            require(recordId.isNotBlank()) { "Record ID must not be blank." }
            db.collection("users").document(uid).collection("records").document(recordId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }

    fun observeBookmarks(userId: String): Flow<List<PortalBookmark>> = flow {
        val uid = requireUserId(userId)
        val path = "users/$uid/bookmarks"
        db.collection("users").document(uid).collection("bookmarks")
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(
                    PortalBookmark::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                ).sortedByDescending { it.updatedAt ?: it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
            }
            .catch { error ->
                val firestoreError = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>()
                    .firstOrNull()
                if (firestoreError == null && error is CancellationException) throw error
                val exceptionToLog = firestoreError ?: (error as? Exception)
                if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                    handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                }
                throw error
            }
            .collect { emit(it) }
    }

    suspend fun createBookmark(
        userId: String,
        title: String,
        url: String,
        category: BookmarkCategory
    ): Result<Unit> {
        var docPath = "users/$userId/bookmarks"
        return try {
            val uid = requireUserId(userId)
            val cleanTitle = title.trim()
            val cleanUrl = url.trim()
            require(cleanTitle.length in 1..80) { "Bookmark title must be 1 to 80 characters." }
            require(AicsmUrls.isValidAicsmUrl(cleanUrl)) {
                "Bookmark URL must be an official https://aicsm.com or https://www.aicsm.com link."
            }

            val docRef = db.collection("users").document(uid).collection("bookmarks").document()
            docPath = docRef.path
            val bookmark = PortalBookmark(
                title = cleanTitle,
                url = cleanUrl,
                category = category.name
            )
            docRef.set(bookmark.toCreateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun deleteBookmark(userId: String, bookmarkId: String): Result<Unit> {
        val docPath = "users/$userId/bookmarks/$bookmarkId"
        return try {
            val uid = requireUserId(userId)
            require(bookmarkId.isNotBlank()) { "Bookmark ID must not be blank." }
            db.collection("users").document(uid).collection("bookmarks").document(bookmarkId)
                .delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docPath)
            Result.failure(e)
        }
    }
}
