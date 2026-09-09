package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.example.data.local.AppDatabase
import com.example.data.local.AttendanceEntity
import com.example.data.local.MemberEntity
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.data.model.getTodayDateString
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("FIT & FINE ATTENDANCE", appName)
  }

  @Test
  fun `test member insertion and duplicate attendance prevention`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = AppDatabase.getInstance(context)

    // 1. Insert member
    val member = Member(
      memberId = "FN-1001",
      fullName = "Rahim Ahmed",
      mobileNumber = "01711223344",
      email = "rahim@example.com",
      status = MemberStatus.ACTIVE,
      qrCodeId = "FFM-FN-1001",
      createdAt = System.currentTimeMillis()
    )
    db.memberDao().insertOrUpdate(MemberEntity.fromDomain(member))

    val fetched = db.memberDao().getMemberByIdOrQr("FN-1001")
    assertNotNull(fetched)
    assertEquals("Rahim Ahmed", fetched?.fullName)

    // 2. Attendance with deterministic ID
    val today = getTodayDateString()
    val deterministicId = "FN-1001_$today"

    val attendance1 = AttendanceEntity(
      id = deterministicId,
      memberId = "FN-1001",
      memberName = "Rahim Ahmed",
      date = today,
      timestamp = 1000L,
      attendanceStatus = "Present",
      scannedBy = "admin"
    )
    db.attendanceDao().insertOrUpdate(attendance1)

    // Verify existing record found
    val existing = db.attendanceDao().getAttendanceById(deterministicId)
    assertNotNull(existing)
    assertEquals(1000L, existing?.timestamp)

    // 3. Duplicate scan prevention check
    val existingToday = db.attendanceDao().getAttendanceForMemberOnDate("FN-1001", today)
    assertNotNull(existingToday)
    assertEquals("Present", existingToday?.attendanceStatus)
  }

  @get:org.junit.Rule
  val composeTestRule = androidx.compose.ui.test.junit4.createAndroidComposeRule<MainActivity>()

  @Test
  fun `test main activity launch`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    val activity = controller.create().start().resume().get()
    assertNotNull(activity)
  }

  @Test
  fun `test main activity touch interaction`() {
    composeTestRule.waitForIdle()

    // 1. Home dashboard exists and responds
    composeTestRule.onNodeWithTag("dashboard_screen").assertExists()

    // 2. Scan Attendance CTA responds to touch
    composeTestRule.onNodeWithTag("dashboard_screen").performScrollToNode(hasTestTag("quick_action_scan_qr"))
    composeTestRule.onNodeWithTag("quick_action_scan_qr").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("scanner_screen").assertExists()

    // 3. Bottom Nav: Members responds to touch
    composeTestRule.onNodeWithTag("nav_item_members").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("members_screen").assertExists()

    // 4. Add Member FAB responds to touch
    composeTestRule.onNodeWithTag("add_member_fab").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_member_name_input").assertExists()
    // Dismiss add dialog
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()

    // 5. Member QR Card Dialog responds to touch (download, share, print)
    composeTestRule.onNodeWithTag("member_qr_btn_FN-1001").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("member_qr_card_dialog").assertExists()
    composeTestRule.onNodeWithTag("download_card_button").assertExists().performClick()
    composeTestRule.onNodeWithTag("share_card_button").assertExists().performClick()
    composeTestRule.onNodeWithTag("print_card_button").assertExists().performClick()
    composeTestRule.onNodeWithTag("close_qr_card_dialog_button").performClick()
    composeTestRule.waitForIdle()

    // 6. Bottom Nav: Reports responds to touch
    composeTestRule.onNodeWithTag("nav_item_reports").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("reports_screen").assertExists()

    // 7. Bottom Nav: Settings responds to touch
    composeTestRule.onNodeWithTag("nav_item_menu").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_screen").assertExists()

    // 8. Bottom Nav: Home returns to dashboard
    composeTestRule.onNodeWithTag("nav_item_home").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_screen").assertExists()

    // 9. Add Member from Dashboard quick action responds to touch
    composeTestRule.onNodeWithTag("dashboard_screen").performScrollToNode(hasTestTag("quick_action_add_member"))
    composeTestRule.onNodeWithTag("quick_action_add_member").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dialog_member_name_input").assertExists()
    composeTestRule.onNodeWithText("Cancel").performClick()
    composeTestRule.waitForIdle()
  }
}

