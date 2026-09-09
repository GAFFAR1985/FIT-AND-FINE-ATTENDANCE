package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.DashboardMetrics
import com.example.data.model.AttendanceRecord
import com.example.data.model.ClubSettings
import com.example.ui.components.GlassStatCard
import com.example.ui.components.NavItem
import com.example.ui.theme.ProfessionalBlue100
import com.example.ui.theme.ProfessionalBlue200
import com.example.ui.theme.ProfessionalBlue700
import com.example.ui.theme.ProfessionalBlue800
import com.example.ui.theme.ProfessionalBlue900
import com.example.ui.theme.ProfessionalGreen100
import com.example.ui.theme.ProfessionalGreen500
import com.example.ui.theme.ProfessionalGreen600
import com.example.ui.theme.ProfessionalGreen700
import com.example.ui.theme.ProfessionalGreen800
import com.example.ui.theme.ProfessionalSlate100
import com.example.ui.theme.ProfessionalSlate400
import com.example.ui.theme.ProfessionalSlate50
import com.example.ui.theme.ProfessionalSlate600
import com.example.ui.theme.ProfessionalSlate900
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(
    metrics: DashboardMetrics,
    recentAttendance: List<AttendanceRecord>,
    settings: ClubSettings,
    isFirebaseConfigured: Boolean,
    onNavigate: (NavItem) -> Unit,
    onAddMemberClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(ProfessionalSlate50)
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // "Professional Polish" Header with curved bottom corners and glassmorphism stats
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 12.dp,
                        shape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                    )
                    .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                ProfessionalBlue900,
                                Color(0xFF172554) // Blue 950
                            )
                        )
                    )
                    .testTag("dashboard_hero_card")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 22.dp)
                ) {
                    // Top App Identity Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Green brand icon badge
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(ProfessionalGreen500),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FitnessCenter,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "ATTENDANCE MANAGER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.5.sp,
                                    color = ProfessionalBlue200
                                )
                                Text(
                                    text = settings.clubName.ifEmpty { "FIT & FINE CLUB" },
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Profile / Cloud Sync circular button
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(ProfessionalBlue800)
                                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.4f), CircleShape)
                                .clickable { onNavigate(NavItem.MENU) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFirebaseConfigured) Icons.Filled.Cloud else Icons.Filled.Person,
                                contentDescription = "Admin Menu",
                                tint = if (isFirebaseConfigured) ProfessionalGreen500 else Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date row
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = ProfessionalBlue200.copy(alpha = 0.8f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = currentDateStr,
                            fontSize = 12.sp,
                            color = ProfessionalBlue200.copy(alpha = 0.9f)
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // 2x2 Glassmorphic Stats Grid inside Header
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassStatCard(
                                title = "Total Members",
                                value = "${metrics.totalMembers}",
                                bengaliSubtitle = "মোট সদস্য",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(NavItem.MEMBERS) }
                            )
                            GlassStatCard(
                                title = "Attendance",
                                value = "%.0f%%".format(metrics.attendancePercentage),
                                bengaliSubtitle = "উপস্থিতির হার",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(NavItem.REPORTS) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GlassStatCard(
                                title = "Today's Present",
                                value = "${metrics.todayPresent}",
                                bengaliSubtitle = "আজ উপস্থিত",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(NavItem.REPORTS) }
                            )
                            GlassStatCard(
                                title = "Today's Absent",
                                value = "${metrics.todayAbsent}",
                                bengaliSubtitle = "আজ অনুপস্থিত",
                                modifier = Modifier.weight(1f),
                                onClick = { onNavigate(NavItem.REPORTS) }
                            )
                        }
                    }
                }
            }
        }

        // Main Action Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Secondary Quick Action Grid (Add Member & Reports)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfessionalActionCard(
                        title = "ADD MEMBER",
                        subtitle = "সদস্য যোগ",
                        icon = Icons.Filled.PersonAdd,
                        iconTint = ProfessionalGreen600,
                        iconBg = ProfessionalGreen100,
                        modifier = Modifier.weight(1f),
                        onClick = onAddMemberClick,
                        tag = "quick_action_add_member"
                    )
                    ProfessionalActionCard(
                        title = "REPORTS",
                        subtitle = "উপস্থিতি রিপোর্ট",
                        icon = Icons.Filled.Assessment,
                        iconTint = Color(0xFF2563EB),
                        iconBg = ProfessionalBlue100,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(NavItem.REPORTS) },
                        tag = "quick_action_reports"
                    )
                }

                // 2 Additional Action Cards (Members & Settings)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ProfessionalActionCard(
                        title = "MEMBERS",
                        subtitle = "সকল সদস্য",
                        icon = Icons.Filled.Group,
                        iconTint = Color(0xFF7C3AED),
                        iconBg = Color(0xFFEDE9FE),
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(NavItem.MEMBERS) },
                        tag = "quick_action_members"
                    )
                    ProfessionalActionCard(
                        title = "SETTINGS",
                        subtitle = "ক্লাব সেটিংস",
                        icon = Icons.Filled.Settings,
                        iconTint = ProfessionalSlate600,
                        iconBg = ProfessionalSlate100,
                        modifier = Modifier.weight(1f),
                        onClick = { onNavigate(NavItem.MENU) },
                        tag = "quick_action_settings"
                    )
                }

                // Primary Large "Scan Attendance" CTA Button with 3D Bevel & Shadow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(24.dp),
                            spotColor = ProfessionalGreen500
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(ProfessionalGreen800) // 3D bottom bevel
                ) {
                    Button(
                        onClick = { onNavigate(NavItem.SCAN) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(bottom = 3.dp) // creates border-b-4 effect
                            .testTag("quick_action_scan_qr"),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ProfessionalGreen600,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "SCAN ATTENDANCE",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "কিউআর কোড স্ক্যান করে উপস্থিতি নিন",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recent Scans Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Recent Scans",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfessionalSlate900
                    )
                    Text(
                        text = "আজকের উপস্থিতি তালিকা (${recentAttendance.size})",
                        fontSize = 11.sp,
                        color = ProfessionalSlate400
                    )
                }

                if (recentAttendance.isNotEmpty()) {
                    Text(
                        text = "View All",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = ProfessionalBlue700,
                        modifier = Modifier
                            .clickable { onNavigate(NavItem.REPORTS) }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .testTag("view_all_recent_attendance")
                    )
                }
            }
        }

        // Recent Scans List or Empty State
        if (recentAttendance.isEmpty()) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("empty_attendance_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ProfessionalSlate100)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(CircleShape)
                                .background(ProfessionalSlate100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.QrCodeScanner,
                                contentDescription = null,
                                tint = ProfessionalSlate400,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No Scans Recorded Today",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = ProfessionalSlate900
                        )
                        Text(
                            text = "আজ এখনো কোনো সদস্যের উপস্থিতি নেওয়া হয়নি। উপরে 'SCAN ATTENDANCE' বাটনে চাপ দিয়ে শুরু করুন।",
                            fontSize = 12.sp,
                            color = ProfessionalSlate600,
                            modifier = Modifier.padding(top = 4.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(recentAttendance.take(8)) { record ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("recent_attendance_item_${record.memberId}"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, ProfessionalSlate100),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Member initials avatar
                            val initials = record.memberName
                                .split(" ")
                                .filter { it.isNotEmpty() }
                                .take(2)
                                .map { it.first().uppercaseChar() }
                                .joinToString("")
                                .ifEmpty { "FF" }

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(ProfessionalSlate100),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfessionalSlate600
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = record.memberName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = ProfessionalSlate900,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "ID: ${record.memberId} • ${record.formattedTime}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = ProfessionalSlate400
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = ProfessionalGreen100
                        ) {
                            Text(
                                text = "PRESENT",
                                color = ProfessionalGreen700,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfessionalActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    tag: String
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .testTag(tag),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ProfessionalSlate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = ProfessionalSlate600,
                maxLines = 1
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                color = ProfessionalSlate400,
                maxLines = 1
            )
        }
    }
}

