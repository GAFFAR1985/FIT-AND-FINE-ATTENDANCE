package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Update
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClubSettings
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "members")
data class MemberEntity(
    @PrimaryKey val memberId: String,
    val fullName: String,
    val mobileNumber: String,
    val email: String,
    val photoUrl: String,
    val photoPath: String = "",
    val status: String,
    val qrCodeId: String,
    val createdAt: Long
) {
    fun toDomain(): Member = Member(
        memberId = memberId,
        fullName = fullName,
        mobileNumber = mobileNumber,
        email = email,
        photoUrl = photoUrl,
        photoPath = photoPath,
        status = try { MemberStatus.valueOf(status) } catch (e: Exception) { MemberStatus.ACTIVE },
        qrCodeId = qrCodeId,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(m: Member): MemberEntity = MemberEntity(
            memberId = m.memberId,
            fullName = m.fullName,
            mobileNumber = m.mobileNumber,
            email = m.email,
            photoUrl = m.photoUrl,
            photoPath = m.photoPath,
            status = m.status.name,
            qrCodeId = m.qrCodeId,
            createdAt = m.createdAt
        )
    }
}

@Entity(tableName = "attendance")
data class AttendanceEntity(
    @PrimaryKey val id: String, // Deterministic: "${memberId}_${date}"
    val memberId: String,
    val memberName: String,
    val date: String,
    val timestamp: Long,
    val attendanceStatus: String,
    val scannedBy: String
) {
    fun toDomain(): AttendanceRecord = AttendanceRecord(
        id = id,
        memberId = memberId,
        memberName = memberName,
        date = date,
        timestamp = timestamp,
        attendanceStatus = attendanceStatus,
        scannedBy = scannedBy
    )

    companion object {
        fun fromDomain(a: AttendanceRecord): AttendanceEntity = AttendanceEntity(
            id = a.id,
            memberId = a.memberId,
            memberName = a.memberName,
            date = a.date,
            timestamp = a.timestamp,
            attendanceStatus = a.attendanceStatus,
            scannedBy = a.scannedBy
        )
    }
}

@Entity(tableName = "club_settings")
data class ClubSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val clubName: String,
    val logoUrl: String,
    val adminEmail: String,
    val workingDaysCsv: String,
    val morningCutoffTime: String,
    val themeColor: String
) {
    fun toDomain(): ClubSettings = ClubSettings(
        clubName = clubName,
        logoUrl = logoUrl,
        adminEmail = adminEmail,
        workingDays = workingDaysCsv.split(",").filter { it.isNotBlank() },
        morningCutoffTime = morningCutoffTime,
        themeColor = themeColor
    )

    companion object {
        fun fromDomain(s: ClubSettings): ClubSettingsEntity = ClubSettingsEntity(
            id = 1,
            clubName = s.clubName,
            logoUrl = s.logoUrl,
            adminEmail = s.adminEmail,
            workingDaysCsv = s.workingDays.joinToString(","),
            morningCutoffTime = s.morningCutoffTime,
            themeColor = s.themeColor
        )
    }
}

@Dao
interface MemberDao {
    @Query("SELECT * FROM members ORDER BY fullName ASC")
    fun getAllMembers(): Flow<List<MemberEntity>>

    @Query("SELECT * FROM members WHERE memberId = :id OR qrCodeId = :id LIMIT 1")
    suspend fun getMemberByIdOrQr(id: String): MemberEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(member: MemberEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<MemberEntity>)

    @Query("DELETE FROM members WHERE memberId = :memberId")
    suspend fun deleteMember(memberId: String)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY timestamp DESC")
    fun getAllAttendance(): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE date = :date ORDER BY timestamp DESC")
    fun getAttendanceForDate(date: String): Flow<List<AttendanceEntity>>

    @Query("SELECT * FROM attendance WHERE id = :id LIMIT 1")
    suspend fun getAttendanceById(id: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE memberId = :memberId AND date = :date LIMIT 1")
    suspend fun getAttendanceForMemberOnDate(memberId: String, date: String): AttendanceEntity?

    @Query("SELECT * FROM attendance WHERE memberId = :memberId ORDER BY timestamp DESC")
    fun getAttendanceForMember(memberId: String): Flow<List<AttendanceEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAttendance(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(attendance: AttendanceEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<AttendanceEntity>)
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM club_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<ClubSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: ClubSettingsEntity)
}

@Database(
    entities = [MemberEntity::class, AttendanceEntity::class, ClubSettingsEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "fit_and_fine_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
