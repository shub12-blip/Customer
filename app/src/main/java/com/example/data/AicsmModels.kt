package com.example.data

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object AicsmUrls {
    const val HOME_PORTAL = "https://www.aicsm.com"
    const val FRANCHISE_ENQUIRY = "https://aicsm.com/CustomerEnquiry.php"

    private val allowedHostRegex = Regex("^https://(www\\.)?aicsm\\.com(/.*)?$")

    fun isValidAicsmUrl(url: String): Boolean {
        return url.isNotBlank() && url.length <= 300 && allowedHostRegex.matches(url.trim())
    }
}

enum class UserRole(val label: String) {
    STUDENT("Student"),
    FRANCHISE_APPLICANT("Franchise Applicant"),
    CENTER_PARTNER("Authorized Center");

    companion object {
        fun fromRaw(raw: String?): UserRole =
            entries.firstOrNull { it.name == raw } ?: STUDENT
    }
}

enum class PortalMode(val label: String) {
    IN_APP("In-App Web Portal"),
    EXTERNAL_BROWSER("External Browser");

    companion object {
        fun fromRaw(raw: String?): PortalMode =
            entries.firstOrNull { it.name == raw } ?: IN_APP
    }
}

enum class RecordCategory(val label: String) {
    FRANCHISE_ENQUIRY("Franchise Enquiry"),
    COURSE_NOTE("Course & Exam Note"),
    DOCUMENT_CHECKLIST("Center Checklist");

    companion object {
        fun fromRaw(raw: String?): RecordCategory =
            entries.firstOrNull { it.name == raw } ?: COURSE_NOTE
    }
}

enum class RecordStatus(val label: String) {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    ARCHIVED("Archived");

    companion object {
        fun fromRaw(raw: String?): RecordStatus =
            entries.firstOrNull { it.name == raw } ?: DRAFT
    }
}

enum class BookmarkCategory(val label: String) {
    PORTAL("Official Portal"),
    FRANCHISE("Franchise"),
    COURSES("Courses"),
    VERIFICATION("Verification");

    companion object {
        fun fromRaw(raw: String?): BookmarkCategory =
            entries.firstOrNull { it.name == raw } ?: PORTAL
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val role: String = UserRole.STUDENT.name,
    val preferredPortalMode: String = PortalMode.IN_APP.name,
    val centerName: String? = null,
    val city: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun roleEnum(): UserRole = UserRole.fromRaw(role)
    fun portalModeEnum(): PortalMode = PortalMode.fromRaw(preferredPortalMode)

    fun toCreateMap(): Map<String, Any> {
        val raw = mapOf(
            "userId" to userId,
            "displayName" to displayName,
            "email" to email,
            "role" to role,
            "preferredPortalMode" to preferredPortalMode,
            "centerName" to centerName?.takeIf { it.isNotBlank() },
            "city" to city?.takeIf { it.isNotBlank() },
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        @Suppress("UNCHECKED_CAST")
        return raw.filterValues { it != null } as Map<String, Any>
    }
}

@Keep
@IgnoreExtraProperties
data class PortalRecord(
    @DocumentId val id: String = "",
    val title: String = "",
    val category: String = RecordCategory.FRANCHISE_ENQUIRY.name,
    val status: String = RecordStatus.DRAFT.name,
    val details: String = "",
    val linkedUrl: String = AicsmUrls.FRANCHISE_ENQUIRY,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): RecordCategory = RecordCategory.fromRaw(category)
    fun statusEnum(): RecordStatus = RecordStatus.fromRaw(status)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title,
        "category" to category,
        "status" to status,
        "details" to details,
        "linkedUrl" to linkedUrl,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class PortalBookmark(
    @DocumentId val id: String = "",
    val title: String = "",
    val url: String = AicsmUrls.HOME_PORTAL,
    val category: String = BookmarkCategory.PORTAL.name,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun categoryEnum(): BookmarkCategory = BookmarkCategory.fromRaw(category)

    fun toCreateMap(): Map<String, Any> = mapOf(
        "title" to title,
        "url" to url,
        "category" to category,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
