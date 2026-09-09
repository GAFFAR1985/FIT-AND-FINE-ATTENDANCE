package com.example.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.MainViewModel
import com.example.data.model.AttendanceRecord
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.ui.components.MemberAvatar
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    allAttendance: List<AttendanceRecord>,
    members: List<Member>,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Attendance List, 1: Monthly Summary

    // Filters
    var dateFilterMode by remember { mutableStateOf("Today") } // Today, Yesterday, This Month, Last Month, All
    var selectedStatusFilter by remember { mutableStateOf("All") } // All, Active, Inactive
    var searchQuery by remember { mutableStateOf("") }
    var selectedMemberFilterId by remember { mutableStateOf("All") }
    var showMemberDropdown by remember { mutableStateOf(false) }

    // Month Selector for Monthly Summary
    val availableMonths = remember {
        val cal = Calendar.getInstance()
        val sdfVal = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfLbl = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val list = mutableListOf<Pair<String, String>>()
        for (i in 0 until 12) {
            val clone = cal.clone() as Calendar
            clone.add(Calendar.MONTH, -i)
            list.add(sdfVal.format(clone.time) to sdfLbl.format(clone.time))
        }
        list
    }
    var selectedMonthValue by remember { mutableStateOf(availableMonths.firstOrNull()?.first ?: "") }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val todayDate = viewModel.todayDateString
    val yesterdayDate = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)
    }
    val currentMonthPrefix = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }
    val lastMonthPrefix = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    // Filtered records for Tab 0
    val filteredRecords = remember(allAttendance, dateFilterMode, selectedStatusFilter, selectedMemberFilterId, searchQuery, members) {
        allAttendance.filter { record ->
            val dateMatches = when (dateFilterMode) {
                "Today" -> record.date == todayDate
                "Yesterday" -> record.date == yesterdayDate
                "This Month" -> record.date.startsWith(currentMonthPrefix)
                "Last Month" -> record.date.startsWith(lastMonthPrefix)
                else -> true
            }
            val member = members.find { it.memberId == record.memberId }
            val memberMatches = selectedMemberFilterId == "All" || record.memberId == selectedMemberFilterId
            val statusMatches = when (selectedStatusFilter) {
                "Active" -> member?.status == MemberStatus.ACTIVE
                "Inactive" -> member?.status == MemberStatus.INACTIVE
                else -> true
            }
            val searchMatches = searchQuery.isBlank() ||
                record.memberName.contains(searchQuery, ignoreCase = true) ||
                record.memberId.contains(searchQuery, ignoreCase = true)

            dateMatches && memberMatches && statusMatches && searchMatches
        }
    }

    // Stats calculations for filtered view
    val totalActiveMembers = members.count { it.status == MemberStatus.ACTIVE }
    val distinctPresentCount = filteredRecords.map { it.memberId }.distinct().count()
    val absentCount = (totalActiveMembers - distinctPresentCount).coerceAtLeast(0)
    val percentage = if (totalActiveMembers > 0) (distinctPresentCount.toFloat() / totalActiveMembers.toFloat()) * 100f else 0f

    // Monthly summary stats for Tab 1
    val selectedMonthRecords = remember(allAttendance, selectedMonthValue) {
        allAttendance.filter { it.date.startsWith(selectedMonthValue) }
    }
    val selectedMonthLabel = availableMonths.find { it.first == selectedMonthValue }?.second ?: selectedMonthValue
    val monthDistinctPresent = selectedMonthRecords.map { it.memberId }.distinct().count()
    val monthDistinctAbsent = (totalActiveMembers - monthDistinctPresent).coerceAtLeast(0)
    val monthPercentage = if (totalActiveMembers > 0) (monthDistinctPresent.toFloat() / totalActiveMembers.toFloat()) * 100f else 0f
    val daysInSelectedMonth = remember(selectedMonthValue) {
        try {
            val parts = selectedMonthValue.split("-")
            val year = parts[0].toInt()
            val month = parts[1].toInt() - 1
            val cal = Calendar.getInstance()
            cal.set(year, month, 1)
            cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        } catch (e: Exception) {
            30
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("reports_screen")
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Attendance Reports",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavySecondary
                    )
                    Text(
                        text = "উপস্থিতি রিপোর্ট ও মাসিক বিশ্লেষণ",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }

                // Export Button depending on tab
                if (selectedTab == 0) {
                    Button(
                        onClick = {
                            val file = viewModel.exportAttendanceCsv(context, filteredRecords)
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Fit & Fine Attendance Report ($dateFilterMode)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Attendance CSV"))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Export CSV", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            val file = viewModel.exportMonthlySummaryCsv(
                                context,
                                selectedMonthLabel,
                                members,
                                selectedMonthRecords,
                                daysInSelectedMonth
                            )
                            if (file != null) {
                                val uri = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/csv"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    putExtra(Intent.EXTRA_SUBJECT, "Fit & Fine Monthly Summary ($selectedMonthLabel)")
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Monthly Summary CSV"))
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        modifier = Modifier.testTag("export_monthly_csv_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Export Month", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tabs: Attendance List vs Monthly Attendance Summary
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = EmeraldGreenPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = EmeraldGreenPrimary
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Attendance List", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_datewise")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Monthly Summary", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                    modifier = Modifier.testTag("tab_member_summary")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (selectedTab == 0) {
                // Filters for Attendance List Tab
                // Date Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val dateOptions = listOf(
                        "Today" to "Today",
                        "Yesterday" to "Yesterday",
                        "This Month" to "This Month",
                        "Last Month" to "Last Month",
                        "All" to "All Time"
                    )
                    items(dateOptions) { (key, label) ->
                        val isSelected = dateFilterMode == key
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) DeepNavySecondary else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable { dateFilterMode = key }
                                .testTag("date_filter_$key")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Member & Status Filters Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Member Filter Selector
                    Box(modifier = Modifier.weight(1f)) {
                        val memberLabel = if (selectedMemberFilterId == "All") "All Members"
                        else members.find { it.memberId == selectedMemberFilterId }?.fullName ?: selectedMemberFilterId

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showMemberDropdown = true }
                                .testTag("filter_member_selector")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = memberLabel,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = Color(0xFF334155)
                                )
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showMemberDropdown,
                            onDismissRequest = { showMemberDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("All Members") },
                                onClick = {
                                    selectedMemberFilterId = "All"
                                    showMemberDropdown = false
                                }
                            )
                            members.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text("${m.fullName} (${m.memberId})") },
                                    onClick = {
                                        selectedMemberFilterId = m.memberId
                                        showMemberDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Status Filter Chips
                    listOf("All", "Active", "Inactive").forEach { statusKey ->
                        val isSelected = selectedStatusFilter == statusKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) EmeraldGreenPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier.clickable { selectedStatusFilter = statusKey }
                        ) {
                            Text(
                                text = statusKey,
                                color = if (isSelected) Color.White else Color(0xFF475569),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by member name or ID...", fontSize = 12.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .testTag("reports_search_input"),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreenPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )
            } else {
                // Month Selector for Monthly Summary Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Selected Month:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF475569)
                    )

                    Box {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable { showMonthDropdown = true }
                                .testTag("month_selector_button")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = DeepNavySecondary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = selectedMonthLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavySecondary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false }
                        ) {
                            availableMonths.forEach { (valMonth, lblMonth) ->
                                DropdownMenuItem(
                                    text = { Text(lblMonth, fontWeight = if (valMonth == selectedMonthValue) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedMonthValue = valMonth
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary Metric Strip
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFFF8FAFC),
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedTab == 0) {
                    MetricColumn("Total Present", "$distinctPresentCount", SuccessGreen)
                    MetricColumn("Total Absent", "$absentCount", ErrorRed)
                    MetricColumn("Attendance Rate", "%.1f%%".format(percentage), EmeraldGreenPrimary)
                    MetricColumn("Total Records", "${filteredRecords.size}", DeepNavySecondary)
                } else {
                    MetricColumn("Active Members", "$totalActiveMembers", DeepNavySecondary)
                    MetricColumn("Month Present", "$monthDistinctPresent", SuccessGreen)
                    MetricColumn("Month Absent", "$monthDistinctAbsent", ErrorRed)
                    MetricColumn("Attendance Rate", "%.1f%%".format(monthPercentage), EmeraldGreenPrimary)
                }
            }
        }

        // Content
        if (selectedTab == 0) {
            // Date-wise Attendance records
            if (filteredRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No attendance records found for selected filter",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredRecords) { record ->
                        val member = members.find { it.memberId == record.memberId }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("report_record_${record.id}"),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Member Avatar with photo
                                    MemberAvatar(
                                        photoUrl = member?.photoUrl,
                                        name = record.memberName,
                                        size = 42.dp,
                                        testTag = "report_avatar_${record.id}"
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = record.memberName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "ID: ${record.memberId} • Date: ${record.formattedDateDisplay}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFECFDF5)
                                    ) {
                                        Text(
                                            text = record.attendanceStatus,
                                            color = SuccessGreen,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = record.formattedTime,
                                        fontSize = 11.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Monthly Member-wise Attendance Summary
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Member-wise Breakdown ($selectedMonthLabel)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavySecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }

                items(members) { member ->
                    val memberMonthRecords = selectedMonthRecords.filter { it.memberId == member.memberId }
                    val presentDays = memberMonthRecords.map { it.date }.distinct().size
                    val attendanceRate = if (daysInSelectedMonth > 0) {
                        ((presentDays.toFloat() / daysInSelectedMonth.toFloat()) * 100f).coerceAtMost(100f)
                    } else 0f

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("monthly_member_summary_${member.memberId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    MemberAvatar(
                                        photoUrl = member.photoUrl,
                                        name = member.fullName,
                                        size = 44.dp,
                                        testTag = "monthly_summary_avatar_${member.memberId}"
                                    )

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = member.fullName,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Text(
                                            text = "ID: ${member.memberId}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                StatusBadge(status = member.status)
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Attendance Stats & Progress Bar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$presentDays / $daysInSelectedMonth days present",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldGreenPrimary
                                )
                                Text(
                                    text = "%.0f%%".format(attendanceRate),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (attendanceRate >= 70f) SuccessGreen else if (attendanceRate >= 40f) Color(0xFFD97706) else ErrorRed
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = (attendanceRate / 100f).coerceIn(0f, 1f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = if (attendanceRate >= 70f) SuccessGreen else if (attendanceRate >= 40f) Color(0xFFD97706) else ErrorRed,
                                trackColor = Color(0xFFE2E8F0)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 10.sp, color = Color(0xFF64748B))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color)
    }
}
