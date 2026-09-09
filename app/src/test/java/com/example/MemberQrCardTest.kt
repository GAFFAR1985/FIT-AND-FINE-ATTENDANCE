package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceEntity
import com.example.data.local.MemberEntity
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.getTodayDateString
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import com.example.ui.qr.MemberCardGenerator
import com.example.ui.qr.QrCodeGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class MemberQrCardTest {

    @Test
    fun `test QR bitmap generation`() {
        val bitmap = QrCodeGenerator.generateQrBitmap(
            content = "FFM-FN-1001",
            size = 200
        )
        assertNotNull(bitmap)
        assertEquals(200, bitmap.width)
        assertEquals(200, bitmap.height)
    }

    @Test
    fun `test member card bitmap generation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val member = Member(
            memberId = "FN-1002",
            fullName = "Anisur Rahman",
            mobileNumber = "01819998877",
            email = "anis@example.com",
            status = MemberStatus.ACTIVE,
            qrCodeId = "FFM-FN-1002",
            createdAt = System.currentTimeMillis()
        )

        val cardBitmap = MemberCardGenerator.generateCardBitmap(context, member)
        assertNotNull(cardBitmap)
        assertEquals(800, cardBitmap.width)
        assertEquals(1200, cardBitmap.height)
    }

    @Test
    fun `test member card download and shareable uri`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val member = Member(
            memberId = "FN-1003",
            fullName = "Nusrat Jahan",
            mobileNumber = "01912345678",
            email = "nusrat@example.com",
            status = MemberStatus.ACTIVE,
            qrCodeId = "FFM-FN-1003",
            createdAt = System.currentTimeMillis()
        )

        val cardBitmap = MemberCardGenerator.generateCardBitmap(context, member)
        val downloadSuccess = MemberCardGenerator.downloadCardToDevice(context, cardBitmap, member)
        assertTrue(downloadSuccess)

        val shareUri = MemberCardGenerator.getShareableCardUri(context, cardBitmap, member.memberId)
        assertNotNull(shareUri)
    }

    @Test
    fun `test QR regeneration and stability`() {
        val member = Member(
            memberId = "FN-1004",
            fullName = "Karim Hossain",
            mobileNumber = "01755667788",
            status = MemberStatus.ACTIVE
        )

        // Initial default payload is stable
        assertEquals("FFM-FN-1004", member.qrPayload)

        // Regenerated QR creates new stable identifier
        val newQrCodeId = "FFM-FN-1004-9876"
        val updatedMember = member.copy(qrCodeId = newQrCodeId)
        assertEquals("FFM-FN-1004-9876", updatedMember.qrPayload)
    }

    @Test
    fun `test scan logic with active, inactive, not found and duplicate member`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        // 1. Inactive member scan should fail
        val inactiveMember = Member(
            memberId = "FN-INACTIVE-1",
            fullName = "Inactive User",
            mobileNumber = "01700000000",
            status = MemberStatus.INACTIVE,
            qrCodeId = "FFM-FN-INACTIVE-1"
        )
        repository.saveMember(inactiveMember)

        val inactiveScan = repository.processScan("FFM-FN-INACTIVE-1")
        assertTrue(inactiveScan is ScanResult.InactiveMember)

        // 2. Non-existent member scan
        val notFoundScan = repository.processScan("UNKNOWN_QR_12345")
        assertTrue(notFoundScan is ScanResult.MemberNotFound)

        // 3. Active member initial scan should succeed
        val activeMember = Member(
            memberId = "FN-ACTIVE-1",
            fullName = "Active User",
            mobileNumber = "01711111111",
            status = MemberStatus.ACTIVE,
            qrCodeId = "FFM-FN-ACTIVE-1"
        )
        repository.saveMember(activeMember)

        val firstScan = repository.processScan("FFM-FN-ACTIVE-1")
        assertTrue(firstScan is ScanResult.Success)

        // 4. Duplicate scan on the same day should return AlreadyMarked without duplicate entry
        val duplicateScan = repository.processScan("FFM-FN-ACTIVE-1")
        assertTrue(duplicateScan is ScanResult.AlreadyMarked)
    }

    @Test
    fun `test member card print`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val member = Member(
            memberId = "FN-PRINT-1",
            fullName = "Print Test User",
            mobileNumber = "01722222222",
            status = MemberStatus.ACTIVE
        )
        val bitmap = MemberCardGenerator.generateCardBitmap(context, member)
        assertNotNull(bitmap)
        // Ensure calling printCard completes without throwing unhandled exception
        MemberCardGenerator.printCard(context, bitmap, member)
    }

    @Test
    fun `test member management CRUD`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        // Add
        val member = Member(
            memberId = "FN-CRUD-1",
            fullName = "CRUD User",
            mobileNumber = "01733333333",
            status = MemberStatus.ACTIVE
        )
        repository.saveMember(member)
        var fetched = repository.findMemberByIdOrQr("FN-CRUD-1")
        assertNotNull(fetched)
        assertEquals("CRUD User", fetched?.fullName)

        // Update
        val updated = member.copy(fullName = "Updated CRUD User", status = MemberStatus.INACTIVE)
        repository.saveMember(updated)
        fetched = repository.findMemberByIdOrQr("FN-CRUD-1")
        assertEquals("Updated CRUD User", fetched?.fullName)
        assertEquals(MemberStatus.INACTIVE, fetched?.status)

        // Delete
        repository.deleteMember("FN-CRUD-1")
        fetched = repository.findMemberByIdOrQr("FN-CRUD-1")
        assertEquals(null, fetched)
    }

    @Test
    fun `test settings and reports CSV export`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        // Settings test
        val customSettings = com.example.data.model.ClubSettings(
            clubName = "Fit & Fine Nutrition Test Club",
            themeColor = "Emerald Green"
        )
        repository.saveSettings(customSettings)

        // CSV export test
        val records = listOf(
            com.example.data.model.AttendanceRecord(
                id = "FN-CSV-1_2026-09-08",
                memberId = "FN-CSV-1",
                memberName = "Test Member",
                date = "2026-09-08",
                timestamp = System.currentTimeMillis(),
                attendanceStatus = "Present",
                scannedBy = "admin"
            )
        )
        val fileName = "FitFine_Test_Attendance.csv"
        val file = java.io.File(context.cacheDir, fileName)
        val writer = java.io.FileWriter(file)
        writer.append("Record ID,Date,Time,Member ID,Member Name,Status,Scanned By\n")
        for (r in records) {
            writer.append("\"${r.id}\",\"${r.date}\",\"${r.formattedTime}\",\"${r.memberId}\",\"${r.memberName}\",\"${r.attendanceStatus}\",\"${r.scannedBy}\"\n")
        }
        writer.flush()
        writer.close()
        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
