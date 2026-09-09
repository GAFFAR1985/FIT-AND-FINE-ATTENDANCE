package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MemberStatus {
    ACTIVE,
    INACTIVE
}

data class Member(
    val memberId: String = "",
    val fullName: String = "",
    val mobileNumber: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val photoPath: String = "",
    val status: MemberStatus = MemberStatus.ACTIVE,
    val qrCodeId: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    val formattedCreatedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            return sdf.format(Date(createdAt))
        }

    // Secure QR code payload - contains ONLY secure unique member identifier
    val qrPayload: String
        get() = if (qrCodeId.isNotBlank()) qrCodeId else "FFM-$memberId"
}

data class AttendanceRecord(
    val id: String = "", // Deterministic key: "${memberId}_${date}"
    val memberId: String = "",
    val memberName: String = "",
    val date: String = "", // YYYY-MM-DD
    val timestamp: Long = System.currentTimeMillis(),
    val attendanceStatus: String = "Present",
    val scannedBy: String = "admin"
) {
    val formattedTime: String
        get() {
            val sdf = SimpleDateFormat("hh:mm:ss a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val formattedDateDisplay: String
        get() {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val parsed = parser.parse(date)
                val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                if (parsed != null) formatter.format(parsed) else date
            } catch (e: Exception) {
                date
            }
        }
}

data class ClubSettings(
    val clubName: String = "FIT & FINE NUTRITION CLUB",
    val logoUrl: String = "",
    val adminEmail: String = "admin@fitfine.club",
    val workingDays: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat"),
    val morningCutoffTime: String = "10:30 AM",
    val themeColor: String = "Emerald Green"
)

data class AdminUser(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val isAuthenticated: Boolean = false,
    val isDemoAdmin: Boolean = false
)

// Helper to get today's date string YYYY-MM-DD
fun getTodayDateString(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return sdf.format(Date())
}
