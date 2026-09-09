package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.data.model.AttendanceRecord
import com.example.data.model.Member
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MemberQrCardDialog
import com.example.ui.components.StatusBadge
import com.example.ui.qr.MemberCardGenerator
import com.example.ui.qr.QrCodeGenerator
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemberProfileScreen(
    member: Member,
    attendanceRecords: List<AttendanceRecord>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    onRegenerateQr: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showQrCardDialog by remember { mutableStateOf(false) }
    val currentMonthYear = remember {
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    }

    val memberRecords = remember(attendanceRecords, member.memberId) {
        attendanceRecords.filter { it.memberId == member.memberId }
            .sortedByDescending { it.timestamp }
    }

    val totalAttendance = memberRecords.size
    val currentMonthAttendance = memberRecords.count { it.date.startsWith(currentMonthYear) }
    // Assuming 26 club working days per month
    val attendancePercentage = if (totalAttendance > 0) ((currentMonthAttendance.toFloat() / 26f) * 100f).coerceAtMost(100f) else 0f

    val qrBitmap = remember(member.qrPayload) {
        QrCodeGenerator.generateQrBitmap(
            content = member.qrPayload,
            size = 400,
            foregroundHex = 0xFF0A2540.toInt(),
            backgroundHex = 0xFFFFFFFF.toInt()
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Member Profile & Attendance", fontSize = 17.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("member_profile_back_btn")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showQrCardDialog = true },
                        modifier = Modifier.testTag("topbar_view_qr_card_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCode,
                            contentDescription = "View QR Card",
                            tint = EmeraldGreenPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = DeepNavySecondary
                )
            )
        },
        modifier = modifier.testTag("member_profile_screen")
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Profile Card with QR Code
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_main_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MemberAvatar(
                                photoUrl = member.photoUrl,
                                name = member.fullName,
                                size = 64.dp,
                                testTag = "member_profile_avatar"
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = member.fullName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Member ID: ${member.memberId}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = EmeraldGreenPrimary
                                )
                                Text(
                                    text = "Registered: ${member.formattedCreatedDate}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            StatusBadge(status = member.status)
                        }

                        if (member.mobileNumber.isNotBlank() || member.email.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (member.mobileNumber.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(member.mobileNumber, fontSize = 13.sp, color = Color(0xFF334155))
                                        }
                                    }
                                    if (member.email.isNotBlank()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(member.email, fontSize = 13.sp, color = Color(0xFF334155))
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // QR Code display
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "Member QR Code",
                                modifier = Modifier.size(158.dp)
                            )
                        }

                        Text(
                            text = "Secure QR Identity: ${member.qrPayload}",
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // FEATURE 1: VIEW QR CARD Button
                        Button(
                            onClick = { showQrCardDialog = true },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("view_qr_card_button")
                        ) {
                            Icon(Icons.Filled.QrCode, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("VIEW QR CARD", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Direct Quick Actions: Download, Share, Print
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Download Card
                            OutlinedButton(
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        val bitmap = MemberCardGenerator.generateCardBitmap(context, member)
                                        val success = MemberCardGenerator.downloadCardToDevice(context, bitmap, member)
                                        withContext(Dispatchers.Main) {
                                            if (success) {
                                                Toast.makeText(context, "QR Card downloaded to Pictures!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to download card", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("profile_download_card_btn")
                            ) {
                                Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Share Card
                            OutlinedButton(
                                onClick = {
                                    val bitmap = MemberCardGenerator.generateCardBitmap(context, member)
                                    MemberCardGenerator.shareCard(context, bitmap, member)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("profile_share_card_btn")
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }

                            // Print Card
                            OutlinedButton(
                                onClick = {
                                    val bitmap = MemberCardGenerator.generateCardBitmap(context, member)
                                    MemberCardGenerator.printCard(context, bitmap, member)
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp)
                                    .testTag("profile_print_card_btn")
                            ) {
                                Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Print", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Stats Cards Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ProfileStatSmall(
                        title = "Total Attendance",
                        value = "$totalAttendance",
                        subtitle = "সর্বমোট উপস্থিতি",
                        color = EmeraldGreenPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatSmall(
                        title = "This Month",
                        value = "$currentMonthAttendance",
                        subtitle = "চলতি মাসে",
                        color = DeepNavySecondary,
                        modifier = Modifier.weight(1f)
                    )
                    ProfileStatSmall(
                        title = "Monthly Rate",
                        value = "%.0f%%".format(attendancePercentage),
                        subtitle = "উপস্থিতির হার",
                        color = SuccessGreen,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // History Section Header
            item {
                Text(
                    text = "Complete Attendance History",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavySecondary,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }

            if (memberRecords.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No attendance records found for this member",
                                color = Color(0xFF64748B),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                items(memberRecords) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("attendance_history_item_${record.id}"),
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFDCFCE7)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = record.formattedDateDisplay,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Text(
                                        text = "Scan Time: ${record.formattedTime}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
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
                        }
                    }
                }
            }
        }
    }

    if (showQrCardDialog) {
        MemberQrCardDialog(
            member = member,
            onDismiss = { showQrCardDialog = false },
            onRegenerateQr = onRegenerateQr
        )
    }
}

@Composable
fun ProfileStatSmall(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, fontSize = 11.sp, color = Color(0xFF64748B), maxLines = 1)
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color, modifier = Modifier.padding(vertical = 2.dp))
            Text(subtitle, fontSize = 9.sp, color = Color(0xFF94A3B8))
        }
    }
}
