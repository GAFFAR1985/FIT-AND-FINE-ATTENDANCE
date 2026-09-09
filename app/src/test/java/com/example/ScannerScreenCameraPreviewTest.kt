package com.example

import android.content.Context
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.getTodayDateString
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.ScanResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ScannerScreenCameraPreviewTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `test scan screen opens, camera controls toggle, and reticle displays correctly`() {
        composeTestRule.waitForIdle()

        // 1. Navigate to Scan Attendance
        composeTestRule.onNodeWithTag("dashboard_screen").performScrollToNode(hasTestTag("quick_action_scan_qr"))
        composeTestRule.onNodeWithTag("quick_action_scan_qr").performClick()
        composeTestRule.waitForIdle()

        // 2. Scanner screen and reticle are present
        composeTestRule.onNodeWithTag("scanner_screen").assertExists()
        composeTestRule.onNodeWithTag("scanner_reticle_frame").assertExists()

        // 3. Torch control toggle
        composeTestRule.onNodeWithTag("scanner_torch_button").assertExists().performClick()
        composeTestRule.waitForIdle()

        // 4. Flip camera control toggle
        composeTestRule.onNodeWithTag("scanner_flip_camera_button").assertExists().performClick()
        composeTestRule.waitForIdle()

        // 5. Manual check-in toggle and input
        composeTestRule.onNodeWithTag("scanner_manual_toggle_button").assertExists().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("manual_scanner_input").assertExists()
        composeTestRule.onNodeWithTag("manual_scanner_submit").assertExists()
    }

    @Test
    fun `test scan member QR code and duplicate prevention`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = AttendanceRepository(context, this)

        // Seed a test member
        val testMember = Member(
            memberId = "FN-SCAN-999",
            fullName = "Tariqul Islam",
            mobileNumber = "01812345678",
            status = MemberStatus.ACTIVE,
            qrCodeId = "FFM-FN-SCAN-999"
        )
        repository.saveMember(testMember)

        // First scan: Expect success
        val result1 = repository.processScan(testMember.qrPayload, "admin")
        assertTrue("Expected ScanResult.Success on first scan", result1 is ScanResult.Success)
        val successResult = result1 as ScanResult.Success
        assertEquals("FN-SCAN-999", successResult.member.memberId)
        assertEquals("Present", successResult.record.attendanceStatus)

        // Second scan: Expect duplicate prevention (AlreadyMarked)
        val result2 = repository.processScan(testMember.qrPayload, "admin")
        assertTrue("Expected ScanResult.AlreadyMarked on duplicate scan", result2 is ScanResult.AlreadyMarked)
        val alreadyMarkedResult = result2 as ScanResult.AlreadyMarked
        assertEquals("FN-SCAN-999", alreadyMarkedResult.member.memberId)
        assertEquals(getTodayDateString(), alreadyMarkedResult.originalRecord.date)
    }

    @Test
    fun `test repeated navigation between Scanner and other tabs without errors or leaks`() {
        composeTestRule.waitForIdle()

        // Loop navigation between tabs 3 times
        repeat(3) {
            // Go to Scan
            composeTestRule.onNodeWithTag("nav_item_scan").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("scanner_screen").assertExists()

            // Go to Members
            composeTestRule.onNodeWithTag("nav_item_members").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("members_screen").assertExists()

            // Go to Reports
            composeTestRule.onNodeWithTag("nav_item_reports").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("reports_screen").assertExists()

            // Go to Home
            composeTestRule.onNodeWithTag("nav_item_home").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("dashboard_screen").assertExists()
        }

        // Return to Scan one final time to verify it starts normally
        composeTestRule.onNodeWithTag("nav_item_scan").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("scanner_screen").assertExists()
        composeTestRule.onNodeWithTag("scanner_reticle_frame").assertExists()
    }
}
