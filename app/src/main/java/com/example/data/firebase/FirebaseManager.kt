package com.example.data.firebase

import android.content.Context
import android.util.Log
import com.example.data.model.AdminUser
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClubSettings
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirebaseManager(private val context: Context) {

    val isFirebaseInitialized: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (e: Exception) {
            false
        }

    val auth: FirebaseAuth?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseAuth.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val firestore: FirebaseFirestore?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseFirestore.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    val storage: FirebaseStorage?
        get() = if (isFirebaseInitialized) {
            try {
                FirebaseStorage.getInstance()
            } catch (e: Exception) {
                null
            }
        } else null

    fun getCurrentUser(): AdminUser? {
        val fbAuth = auth ?: return null
        val user = fbAuth.currentUser ?: return null
        return AdminUser(
            uid = user.uid,
            email = user.email ?: "",
            displayName = user.displayName ?: (user.email?.substringBefore("@") ?: "Admin"),
            photoUrl = user.photoUrl?.toString() ?: "",
            isAuthenticated = true,
            isDemoAdmin = false
        )
    }

    // --- FIREBASE STORAGE FOR MEMBER PHOTOS ---
    /**
     * Uploads member profile image bytes to Firebase Storage under `members/{memberId}/profile.jpg`.
     * Returns Pair(downloadUrl, storagePath) on success, or null on failure.
     */
    suspend fun uploadMemberPhoto(memberId: String, imageBytes: ByteArray): Pair<String, String>? {
        val fbStorage = storage ?: return null
        val path = "members/$memberId/profile.jpg"
        val ref = fbStorage.reference.child(path)
        val metadata = StorageMetadata.Builder()
            .setContentType("image/jpeg")
            .build()

        return try {
            ref.putBytes(imageBytes, metadata).await()
            val downloadUrl = ref.downloadUrl.await().toString()
            Pair(downloadUrl, path)
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Failed to upload photo for member $memberId", e)
            null
        }
    }

    suspend fun deleteMemberPhoto(photoPath: String): Boolean {
        if (photoPath.isBlank()) return true
        val fbStorage = storage ?: return false
        return try {
            fbStorage.reference.child(photoPath).delete().await()
            true
        } catch (e: Exception) {
            Log.w("FirebaseManager", "Could not delete photo at $photoPath: ${e.message}")
            false
        }
    }

    // --- FIRESTORE MEMBERS ---
    fun listenToMembers(): Flow<List<Member>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val registration = db.collection("members")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Listen members failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val members = snapshot.documents.mapNotNull { doc ->
                        try {
                            Member(
                                memberId = doc.getString("memberId") ?: doc.id,
                                fullName = doc.getString("fullName") ?: "",
                                mobileNumber = doc.getString("mobileNumber") ?: "",
                                email = doc.getString("email") ?: "",
                                photoUrl = doc.getString("photoUrl") ?: "",
                                photoPath = doc.getString("photoPath") ?: "",
                                status = if (doc.getString("status") == "INACTIVE") MemberStatus.INACTIVE else MemberStatus.ACTIVE,
                                qrCodeId = doc.getString("qrCodeId") ?: "",
                                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(members)
                }
            }
        awaitClose { registration.remove() }
    }

    suspend fun saveMemberToFirestore(member: Member): Boolean {
        val db = firestore ?: return false
        return try {
            val data = hashMapOf(
                "memberId" to member.memberId,
                "fullName" to member.fullName,
                "mobileNumber" to member.mobileNumber,
                "email" to member.email,
                "photoUrl" to member.photoUrl,
                "photoPath" to member.photoPath,
                "status" to member.status.name,
                "qrCodeId" to member.qrCodeId,
                "createdAt" to member.createdAt
            )
            db.collection("members").document(member.memberId).set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving member", e)
            false
        }
    }

    suspend fun deleteMemberFromFirestore(memberId: String): Boolean {
        val db = firestore ?: return false
        return try {
            db.collection("members").document(memberId).delete().await()
            deleteMemberPhoto("members/$memberId/profile.jpg")
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error deleting member", e)
            false
        }
    }

    // --- FIRESTORE ATTENDANCE ---
    fun listenToAttendance(): Flow<List<AttendanceRecord>> = callbackFlow {
        val db = firestore
        if (db == null) {
            close()
            return@callbackFlow
        }

        val registration = db.collection("attendance")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FirebaseManager", "Listen attendance failed", error)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val records = snapshot.documents.mapNotNull { doc ->
                        try {
                            AttendanceRecord(
                                id = doc.id,
                                memberId = doc.getString("memberId") ?: "",
                                memberName = doc.getString("memberName") ?: "",
                                date = doc.getString("date") ?: "",
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                attendanceStatus = doc.getString("attendanceStatus") ?: "Present",
                                scannedBy = doc.getString("scannedBy") ?: "admin"
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    trySend(records)
                }
            }
        awaitClose { registration.remove() }
    }

    /**
     * Records attendance with deterministic key: "${memberId}_${date}"
     * Returns Triple(success: Boolean, alreadyMarked: Boolean, originalRecord: AttendanceRecord?)
     */
    suspend fun recordAttendanceToFirestore(record: AttendanceRecord): Triple<Boolean, Boolean, AttendanceRecord?> {
        val db = firestore ?: return Triple(false, false, null)
        val docId = "${record.memberId}_${record.date}"
        val docRef = db.collection("attendance").document(docId)

        return try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                val existing = AttendanceRecord(
                    id = snapshot.id,
                    memberId = snapshot.getString("memberId") ?: record.memberId,
                    memberName = snapshot.getString("memberName") ?: record.memberName,
                    date = snapshot.getString("date") ?: record.date,
                    timestamp = snapshot.getLong("timestamp") ?: System.currentTimeMillis(),
                    attendanceStatus = snapshot.getString("attendanceStatus") ?: "Present",
                    scannedBy = snapshot.getString("scannedBy") ?: "admin"
                )
                // Duplicate detected!
                Triple(false, true, existing)
            } else {
                val data = hashMapOf(
                    "memberId" to record.memberId,
                    "memberName" to record.memberName,
                    "date" to record.date,
                    "timestamp" to record.timestamp,
                    "attendanceStatus" to record.attendanceStatus,
                    "scannedBy" to record.scannedBy
                )
                docRef.set(data).await()
                Triple(true, false, null)
            }
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error recording attendance", e)
            Triple(false, false, null)
        }
    }

    // --- FIRESTORE SETTINGS ---
    suspend fun saveSettingsToFirestore(settings: ClubSettings): Boolean {
        val db = firestore ?: return false
        return try {
            val data = hashMapOf(
                "clubName" to settings.clubName,
                "logoUrl" to settings.logoUrl,
                "adminEmail" to settings.adminEmail,
                "workingDays" to settings.workingDays,
                "morningCutoffTime" to settings.morningCutoffTime,
                "themeColor" to settings.themeColor
            )
            db.collection("settings").document("club").set(data, SetOptions.merge()).await()
            true
        } catch (e: Exception) {
            Log.e("FirebaseManager", "Error saving settings", e)
            false
        }
    }
}
