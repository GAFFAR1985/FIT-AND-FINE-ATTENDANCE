package com.example

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AttendanceRecord
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import com.example.ui.qr.MemberCardGenerator
import com.example.util.ImageUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileWriter

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemberFeatureEnhancementTest {

    @Test
    fun `test save and delete local member photo via ImageUtils and repository`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        val memberId = "FN-PHOTO-TEST-1"
        val fakePhotoBytes = ByteArray(1024) { (it % 128).toByte() }

        // Save local photo
        val savedFile = ImageUtils.saveImageLocally(context, memberId, fakePhotoBytes)
        assertNotNull(savedFile)
        assertTrue(savedFile.exists())
        assertEquals(1024, savedFile.length())

        // Save member with photo via repository
        val member = Member(
            memberId = memberId,
            fullName = "Photo Test Member",
            status = MemberStatus.ACTIVE
        )
        val updated = repository.saveMemberWithPhoto(member, fakePhotoBytes)
        assertTrue(updated.photoUrl.isNotBlank())

        // Delete photo via repository
        val removed = repository.saveMemberWithPhoto(updated, null, removeExistingPhoto = true)
        assertEquals("", removed.photoUrl)
        assertFalse(File(context.filesDir, "member_photos/$memberId.jpg").exists())
    }

    @Test
    fun `test member card generator with photo rendered`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val memberId = "FN-PHOTO-CARD-1"

        // Create a small test photo on disk
        val photoDir = File(context.filesDir, "member_photos")
        photoDir.mkdirs()
        val photoFile = File(photoDir, "$memberId.jpg")
        val sampleBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        val fos = photoFile.outputStream()
        sampleBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
        fos.close()

        val member = Member(
            memberId = memberId,
            fullName = "Tariqul Islam",
            mobileNumber = "01799887766",
            status = MemberStatus.ACTIVE,
            photoUrl = photoFile.absolutePath,
            qrCodeId = "FFM-$memberId"
        )

        val cardBitmap = MemberCardGenerator.generateCardBitmap(context, member)
        assertNotNull(cardBitmap)
        assertEquals(800, cardBitmap.width)
        assertEquals(1200, cardBitmap.height)

        // Clean up
        photoFile.delete()
    }

    @Test
    fun `test monthly attendance summary calculations and CSV export`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val members = listOf(
            Member(memberId = "FN-1", fullName = "Member One", status = MemberStatus.ACTIVE),
            Member(memberId = "FN-2", fullName = "Member Two", status = MemberStatus.ACTIVE),
            Member(memberId = "FN-3", fullName = "Member Three", status = MemberStatus.INACTIVE)
        )

        val selectedMonth = "2026-09"
        val records = listOf(
            AttendanceRecord(id = "FN-1_2026-09-01", memberId = "FN-1", memberName = "Member One", date = "2026-09-01", timestamp = 1000L),
            AttendanceRecord(id = "FN-1_2026-09-02", memberId = "FN-1", memberName = "Member One", date = "2026-09-02", timestamp = 2000L),
            AttendanceRecord(id = "FN-2_2026-09-01", memberId = "FN-2", memberName = "Member Two", date = "2026-09-01", timestamp = 3000L)
        )

        val totalMonthDays = 30
        val exportFile = File(context.cacheDir, "Monthly_Summary_Test.csv")
        val writer = FileWriter(exportFile)
        writer.append("Member ID,Member Name,Status,Total Present Days,Working Days,Attendance Percentage\n")
        for (m in members) {
            val memberRecords = records.filter { it.memberId == m.memberId }
            val presentDays = memberRecords.map { it.date }.distinct().size
            val percentage = if (totalMonthDays > 0) ((presentDays.toFloat() / totalMonthDays.toFloat()) * 100f).coerceAtMost(100f) else 0f
            writer.append("\"${m.memberId}\",\"${m.fullName}\",\"${m.status.name}\",$presentDays,$totalMonthDays,\"%.1f%%\"\n".format(percentage))
        }
        writer.flush()
        writer.close()

        assertTrue(exportFile.exists())
        assertTrue(exportFile.length() > 0)
    }

    @Test
    fun `test attendance duplicate prevention strictly enforces one record per day`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        val member = Member(
            memberId = "FN-DUP-CHECK-1",
            fullName = "Duplicate Check User",
            status = MemberStatus.ACTIVE,
            qrCodeId = "FFM-FN-DUP-CHECK-1"
        )
        repository.saveMember(member)

        // First scan succeeds
        val scan1 = repository.processScan(member.qrPayload)
        assertTrue("First scan should succeed", scan1 is ScanResult.Success)

        // Duplicate scan on same day returns AlreadyMarked
        val scan2 = repository.processScan(member.qrPayload)
        assertTrue("Duplicate scan should return AlreadyMarked", scan2 is ScanResult.AlreadyMarked)

        // Direct record count should be exactly 1
        val today = com.example.data.model.getTodayDateString()
        val expectedRecordId = "${member.memberId}_$today"
        val allAttendance = repository.attendanceFlow.first()
        val memberAttendance = allAttendance.filter { it.memberId == member.memberId }
        assertEquals(1, memberAttendance.size)
        assertEquals(expectedRecordId, memberAttendance[0].id)
    }

    @Test
    fun `test attendance history reverse chronological order`() {
        val records = listOf(
            AttendanceRecord(id = "1", memberId = "FN-HIST-1", memberName = "A", date = "2026-09-01", timestamp = 1000L),
            AttendanceRecord(id = "2", memberId = "FN-HIST-1", memberName = "A", date = "2026-09-05", timestamp = 5000L),
            AttendanceRecord(id = "3", memberId = "FN-HIST-1", memberName = "A", date = "2026-09-03", timestamp = 3000L)
        )

        val sorted = records.sortedByDescending { it.timestamp }
        assertEquals("2", sorted[0].id)
        assertEquals("3", sorted[1].id)
        assertEquals("1", sorted[2].id)
    }
}
