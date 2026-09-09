package com.example.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.ui.qr.QrCodeGenerator
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import com.example.ui.theme.ProfessionalBlue100
import com.example.ui.theme.ProfessionalBlue200
import com.example.ui.theme.ProfessionalBlue700
import com.example.ui.theme.ProfessionalBlue800
import com.example.ui.theme.ProfessionalBlue900
import com.example.ui.theme.ProfessionalGreen100
import com.example.ui.theme.ProfessionalGreen600
import com.example.ui.theme.ProfessionalGreen700
import com.example.ui.theme.ProfessionalGreen800
import com.example.ui.theme.ProfessionalSlate100
import com.example.ui.theme.ProfessionalSlate200
import com.example.ui.theme.ProfessionalSlate400
import com.example.ui.theme.ProfessionalSlate600
import com.example.ui.theme.ProfessionalSlate900

enum class NavItem(
    val title: String,
    val bengaliTitle: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", "হোম", Icons.Filled.Home, Icons.Outlined.Home),
    SCAN("Scan", "স্ক্যান", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
    MEMBERS("Members", "সদস্য", Icons.Filled.Group, Icons.Outlined.Group),
    REPORTS("Reports", "রিপোর্ট", Icons.Filled.DateRange, Icons.Outlined.DateRange),
    MENU("Menu", "মেনু", Icons.Filled.Settings, Icons.Outlined.Settings)
}

@Composable
fun FitFineBottomBar(
    currentScreen: NavItem,
    onNavigate: (NavItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .border(BorderStroke(1.dp, ProfessionalSlate100)),
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        NavigationBar(
            modifier = Modifier.testTag("bottom_nav_bar"),
            containerColor = Color.White,
            tonalElevation = 0.dp
        ) {
            NavItem.values().forEach { item ->
                val isSelected = currentScreen == item
                NavigationBarItem(
                    modifier = Modifier.testTag("nav_item_${item.name.lowercase()}"),
                    selected = isSelected,
                    onClick = { onNavigate(item) },
                    icon = {
                        Icon(
                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = item.title,
                            modifier = Modifier.size(22.dp)
                        )
                    },
                    label = {
                        Text(
                            text = item.title.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = (-0.2).sp,
                            maxLines = 1
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = ProfessionalBlue700,
                        selectedTextColor = ProfessionalBlue700,
                        indicatorColor = ProfessionalBlue100,
                        unselectedIconColor = ProfessionalSlate400,
                        unselectedTextColor = ProfessionalSlate400
                    )
                )
            }
        }
    }
}

@Composable
fun GlassStatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bengaliSubtitle: String? = null,
    onClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .testTag("glass_stat_${title.lowercase().replace(" ", "_")}")
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp,
                color = ProfessionalBlue200
            )
            if (!bengaliSubtitle.isNullOrEmpty()) {
                Text(
                    text = bengaliSubtitle,
                    fontSize = 9.sp,
                    color = ProfessionalBlue200.copy(alpha = 0.75f)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    bengaliSubtitle: String,
    value: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .testTag("stat_card_${title.lowercase().replace(" ", "_")}")
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, ProfessionalSlate100),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = ProfessionalSlate600,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = bengaliSubtitle,
                        fontSize = 10.sp,
                        color = ProfessionalSlate400
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = ProfessionalSlate900
            )
        }
    }
}

@Composable
fun StatusBadge(
    status: MemberStatus,
    modifier: Modifier = Modifier
) {
    val isActive = status == MemberStatus.ACTIVE
    val bgColor = if (isActive) ProfessionalGreen100 else Color(0xFFFEE2E2)
    val textColor = if (isActive) ProfessionalGreen700 else Color(0xFFB91C1C)
    val text = if (isActive) "ACTIVE • সক্রিয়" else "INACTIVE • নিষ্ক্রিয়"

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.4.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MemberQrDialog(
    member: Member,
    onDismiss: () -> Unit,
    onShare: () -> Unit
) {
    val qrBitmap = remember(member.qrPayload) {
        QrCodeGenerator.generateQrBitmap(
            content = member.qrPayload,
            size = 512,
            foregroundHex = 0xFF0A2540.toInt(),
            backgroundHex = 0xFFFFFFFF.toInt()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("member_qr_dialog")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Member QR Pass",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepNavySecondary
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qr_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                Text(
                    text = "FIT & FINE NUTRITION CLUB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldGreenPrimary,
                    modifier = Modifier.padding(top = 2.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "QR Code for ${member.fullName}",
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = member.fullName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Member ID: ${member.memberId}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF64748B)
                )
                Text(
                    text = "QR Identity: ${member.qrPayload}",
                    fontSize = 11.sp,
                    color = Color(0xFF94A3B8)
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("cancel_qr_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = onShare,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_qr_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share QR")
                    }
                }
            }
        }
    }
}
