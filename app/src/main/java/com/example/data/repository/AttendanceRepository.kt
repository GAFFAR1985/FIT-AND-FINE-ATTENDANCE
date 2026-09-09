package com.example.data.repository

import android.content.Context
import com.example.data.firebase.FirebaseManager
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceEntity
import com.example.data.local.ClubSettingsEntity
import com.example.data.local.MemberEntity
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClubSettings
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.getTodayDateString
import com.example.util.ImageUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

sealed class ScanResult {
    data class Success(val record: AttendanceRecord, val member: Member) : ScanResult()
    data class AlreadyMarked(val originalRecord: AttendanceRecord, val member: Member) : ScanResult()
    data class MemberNotFound(val scannedCode: String) : ScanResult()
    data class InactiveMember(val member: Member) : ScanResult()
    data class Error(val message: String) : ScanResult()
}

class AttendanceRepository(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val db = AppDatabase.getInstance(context)
    val firebaseManager = FirebaseManager(context)

    init {
        // If Firebase is available, start background synchronizer
        if (firebaseManager.isFirebaseInitialized && firebaseManager.firestore != null) {
            scope.launch(Dispatchers.IO) {
                firebaseManager.listenToMembers().collect { fbMembers ->
                    if (fbMembers.isNotEmpty()) {
                        db.memberDao().insertAll(fbMembers.map { MemberEntity.fromDomain(it) })
                    }
                }
            }
            scope.launch(Dispatchers.IO) {
                firebaseManager.listenToAttendance().collect { fbRecords ->
                    if (fbRecords.isNotEmpty()) {
                        db.attendanceDao().insertAll(fbRecords.map { AttendanceEntity.fromDomain(it) })
                    }
                }
            }
        }
    }

    val membersFlow: Flow<List<Member>> = db.memberDao().getAllMembers()
        .map { list -> list.map { it.toDomain() } }

    val attendanceFlow: Flow<List<AttendanceRecord>> = db.attendanceDao().getAllAttendance()
        .map { list -> list.map { it.toDomain() } }

    val settingsFlow: Flow<ClubSettings> = db.settingsDao().getSettings()
        .map { entity -> entity?.toDomain() ?: ClubSettings() }

    suspend fun findMemberByIdOrQr(code: String): Member? {
        val cleanCode = code.trim()
        val local = db.memberDao().getMemberByIdOrQr(cleanCode)
        if (local != null) return local.toDomain()

        // Also check if prefix FFM- was stripped or included
        val rawId = cleanCode.removePrefix("FFM-").removePrefix("FF-MEM-").trim()
        val altLocal = db.memberDao().getMemberByIdOrQr(rawId)
        return altLocal?.toDomain()
    }

    suspend fun saveMember(member: Member): Boolean {
        // Save to local Room
        db.memberDao().insertOrUpdate(MemberEntity.fromDomain(member))
        // Sync to Firebase if available
        if (firebaseManager.isFirebaseInitialized) {
            firebaseManager.saveMemberToFirestore(member)
        }
        return true
    }

    suspend fun saveMemberWithPhoto(
        member: Member,
        newPhotoBytes: ByteArray?,
        removeExistingPhoto: Boolean = false
    ): Member {
        var updatedMember = member

        if (removeExistingPhoto) {
            if (member.photoPath.isNotBlank()) {
                firebaseManager.deleteMemberPhoto(member.photoPath)
            }
            ImageUtils.deleteLocalMemberPhoto(context, member.memberId)
            updatedMember = member.copy(photoUrl = "", photoPath = "")
        } else if (newPhotoBytes != null) {
            val localFile = ImageUtils.saveImageLocally(context, member.memberId, newPhotoBytes)
            var finalPhotoUrl = localFile.toURI().toString()
            var finalPhotoPath = ""

            if (firebaseManager.isFirebaseInitialized) {
                val uploadResult = firebaseManager.uploadMemberPhoto(member.memberId, newPhotoBytes)
                if (uploadResult != null) {
                    finalPhotoUrl = uploadResult.first
                    finalPhotoPath = uploadResult.second
                }
            }

            updatedMember = member.copy(
                photoUrl = finalPhotoUrl,
                photoPath = finalPhotoPath
            )
        }

        saveMember(updatedMember)
        return updatedMember
    }

    suspend fun deleteMember(memberId: String): Boolean {
        ImageUtils.deleteLocalMemberPhoto(context, memberId)
        db.memberDao().deleteMember(memberId)
        if (firebaseManager.isFirebaseInitialized) {
            firebaseManager.deleteMemberFromFirestore(memberId)
        }
        return true
    }

    /**
     * Records attendance for member on date.
     * Guaranteed duplicate prevention with deterministic key: "${memberId}_${date}"
     */
    suspend fun processScan(qrOrMemberId: String, scannedBy: String = "admin"): ScanResult {
        val member = findMemberByIdOrQr(qrOrMemberId)
            ?: return ScanResult.MemberNotFound(qrOrMemberId)

        if (member.status == MemberStatus.INACTIVE) {
            return ScanResult.InactiveMember(member)
        }

        val todayDate = getTodayDateString()
        val deterministicId = "${member.memberId}_$todayDate"

        // 1. Check local Room for existing record today
        val existingLocal = db.attendanceDao().getAttendanceById(deterministicId)
            ?: db.attendanceDao().getAttendanceForMemberOnDate(member.memberId, todayDate)

        if (existingLocal != null) {
            return ScanResult.AlreadyMarked(
                originalRecord = existingLocal.toDomain(),
                member = member
            )
        }

        // 2. Check Firestore if initialized
        if (firebaseManager.isFirebaseInitialized) {
            val (success, alreadyMarked, fbExisting) = firebaseManager.recordAttendanceToFirestore(
                AttendanceRecord(
                    id = deterministicId,
                    memberId = member.memberId,
                    memberName = member.fullName,
                    date = todayDate,
                    timestamp = System.currentTimeMillis(),
                    attendanceStatus = "Present",
                    scannedBy = scannedBy
                )
            )

            if (alreadyMarked && fbExisting != null) {
                // Save locally to keep in sync
                db.attendanceDao().insertOrUpdate(AttendanceEntity.fromDomain(fbExisting))
                return ScanResult.AlreadyMarked(originalRecord = fbExisting, member = member)
            }
        }

        // 3. Create and commit new attendance record
        val now = System.currentTimeMillis()
        val newRecord = AttendanceRecord(
            id = deterministicId,
            memberId = member.memberId,
            memberName = member.fullName,
            date = todayDate,
            timestamp = now,
            attendanceStatus = "Present",
            scannedBy = scannedBy
        )

        db.attendanceDao().insertOrUpdate(AttendanceEntity.fromDomain(newRecord))

        return ScanResult.Success(record = newRecord, member = member)
    }

    suspend fun saveSettings(settings: ClubSettings) {
        db.settingsDao().saveSettings(ClubSettingsEntity.fromDomain(settings))
        if (firebaseManager.isFirebaseInitialized) {
            firebaseManager.saveSettingsToFirestore(settings)
        }
    }
}
