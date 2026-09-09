package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AdminUser
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClubSettings
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.getTodayDateString
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import com.example.ui.qr.SoundFeedback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DashboardMetrics(
    val totalMembers: Int = 0,
    val activeMembers: Int = 0,
    val todayPresent: Int = 0,
    val todayAbsent: Int = 0,
    val attendancePercentage: Float = 0f
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AttendanceRepository(application, viewModelScope)

    // Current Admin User
    private val _currentUser = MutableStateFlow(
        repository.firebaseManager.getCurrentUser() ?: AdminUser(
            uid = "admin-local",
            email = "admin@fitfine.club",
            displayName = "Club Admin",
            isAuthenticated = true,
            isDemoAdmin = !repository.firebaseManager.isFirebaseInitialized
        )
    )
    val currentUser: StateFlow<AdminUser> = _currentUser.asStateFlow()

    val isFirebaseConfigured: Boolean
        get() = repository.firebaseManager.isFirebaseInitialized

    // Data streams
    val members: StateFlow<List<Member>> = repository.membersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendance: StateFlow<List<AttendanceRecord>> = repository.attendanceFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<ClubSettings> = repository.settingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ClubSettings())

    // Dashboard Metrics
    val todayDateString: String = getTodayDateString()

    val todayAttendance: StateFlow<List<AttendanceRecord>> = allAttendance.combine(
        MutableStateFlow(todayDateString)
    ) { list, today ->
        list.filter { it.date == today }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val metrics: StateFlow<DashboardMetrics> = combine(
        members,
        todayAttendance
    ) { memberList, todayAtt ->
        val activeCount = memberList.count { it.status == MemberStatus.ACTIVE }
        val presentCount = todayAtt.map { it.memberId }.distinct().count()
        val absentCount = (activeCount - presentCount).coerceAtLeast(0)
        val pct = if (activeCount > 0) (presentCount.toFloat() / activeCount.toFloat()) * 100f else 0f
        DashboardMetrics(
            totalMembers = memberList.size,
            activeMembers = activeCount,
            todayPresent = presentCount,
            todayAbsent = absentCount,
            attendancePercentage = pct
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    // Scanner state
    private val _scanResult = MutableStateFlow<ScanResult?>(null)
    val scanResult: StateFlow<ScanResult?> = _scanResult.asStateFlow()

    private val _isScanningActive = MutableStateFlow(true)
    val isScanningActive: StateFlow<Boolean> = _isScanningActive.asStateFlow()

    fun resetScanResult() {
        _scanResult.value = null
        _isScanningActive.value = true
    }

    fun onQrScanned(rawCode: String) {
        if (!_isScanningActive.value) return
        _isScanningActive.value = false

        viewModelScope.launch {
            val result = repository.processScan(
                qrOrMemberId = rawCode,
                scannedBy = _currentUser.value.email.ifBlank { "admin" }
            )
            _scanResult.value = result

            when (result) {
                is ScanResult.Success -> {
                    SoundFeedback.playSuccess()
                }
                is ScanResult.AlreadyMarked -> {
                    SoundFeedback.playWarning()
                }
                else -> {
                    SoundFeedback.playWarning()
                }
            }
        }
    }

    // Member CRUD
    fun addOrUpdateMember(
        id: String,
        name: String,
        mobile: String,
        email: String,
        status: MemberStatus,
        photoBytes: ByteArray? = null,
        removeExistingPhoto: Boolean = false,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val cleanId = id.trim().ifBlank {
                "FN-" + (1001 + members.value.size)
            }
            val existing = members.value.find { it.memberId == cleanId }
            val member = Member(
                memberId = cleanId,
                fullName = name.trim(),
                mobileNumber = mobile.trim(),
                email = email.trim(),
                photoUrl = existing?.photoUrl ?: "",
                photoPath = existing?.photoPath ?: "",
                status = status,
                qrCodeId = existing?.qrCodeId ?: "FFM-$cleanId",
                createdAt = existing?.createdAt ?: System.currentTimeMillis()
            )
            repository.saveMemberWithPhoto(
                member = member,
                newPhotoBytes = photoBytes,
                removeExistingPhoto = removeExistingPhoto
            )
            onComplete?.invoke(true)
        }
    }

    fun toggleMemberStatus(member: Member) {
        viewModelScope.launch {
            val updated = member.copy(
                status = if (member.status == MemberStatus.ACTIVE) MemberStatus.INACTIVE else MemberStatus.ACTIVE
            )
            repository.saveMember(updated)
        }
    }

    fun regenerateMemberQrCode(memberId: String) {
        viewModelScope.launch {
            val member = members.value.find { it.memberId == memberId } ?: return@launch
            val randomSuffix = (1000..9999).random()
            val newQrCodeId = "FFM-${member.memberId}-$randomSuffix"
            val updated = member.copy(qrCodeId = newQrCodeId)
            repository.saveMember(updated)
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            repository.deleteMember(memberId)
        }
    }

    // Settings
    fun saveClubSettings(newSettings: ClubSettings) {
        viewModelScope.launch {
            repository.saveSettings(newSettings)
        }
    }

    // Login/Logout
    fun signInAsAdmin(email: String, name: String) {
        _currentUser.value = AdminUser(
            uid = "admin-" + System.currentTimeMillis(),
            email = email,
            displayName = name,
            isAuthenticated = true,
            isDemoAdmin = true
        )
    }

    fun signOut() {
        try {
            repository.firebaseManager.auth?.signOut()
        } catch (e: Exception) {
            // Ignore
        }
        _currentUser.value = AdminUser(isAuthenticated = false)
    }

    // CSV Export
    fun exportAttendanceCsv(context: Context, filteredRecords: List<AttendanceRecord>): File? {
        return try {
            val fileName = "FitFine_Attendance_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Record ID,Date,Time,Member ID,Member Name,Status,Scanned By\n")
            for (record in filteredRecords) {
                writer.append("\"${record.id}\",")
                writer.append("\"${record.date}\",")
                writer.append("\"${record.formattedTime}\",")
                writer.append("\"${record.memberId}\",")
                writer.append("\"${record.memberName}\",")
                writer.append("\"${record.attendanceStatus}\",")
                writer.append("\"${record.scannedBy}\"\n")
            }
            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            null
        }
    }

    fun exportMonthlySummaryCsv(
        context: Context,
        monthLabel: String,
        members: List<Member>,
        records: List<AttendanceRecord>,
        totalDaysInMonth: Int
    ): File? {
        return try {
            val sanitizedMonth = monthLabel.replace(" ", "_").replace("/", "-")
            val fileName = "FitFine_Monthly_Summary_${sanitizedMonth}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            val writer = FileWriter(file)

            writer.append("Member ID,Full Name,Mobile Number,Email,Status,Total Days Present,Total Days in Month,Attendance Rate\n")
            for (member in members) {
                val memberRecords = records.filter { it.memberId == member.memberId }
                val presentDays = memberRecords.map { it.date }.distinct().size
                val rate = if (totalDaysInMonth > 0) {
                    val pct = (presentDays.toFloat() / totalDaysInMonth * 100).toInt()
                    "$pct%"
                } else "N/A"

                writer.append("\"${member.memberId}\",")
                writer.append("\"${member.fullName}\",")
                writer.append("\"${member.mobileNumber}\",")
                writer.append("\"${member.email}\",")
                writer.append("\"${member.status.name}\",")
                writer.append("\"$presentDays\",")
                writer.append("\"$totalDaysInMonth\",")
                writer.append("\"$rate\"\n")
            }
            writer.flush()
            writer.close()
            file
        } catch (e: Exception) {
            null
        }
    }
}
