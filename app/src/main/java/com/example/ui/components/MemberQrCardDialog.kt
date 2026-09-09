package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.ui.qr.MemberCardGenerator
import com.example.ui.qr.QrCodeGenerator
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.ProfessionalGreen100
import com.example.ui.theme.ProfessionalGreen700
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MemberQrCardDialog(
    member: Member,
    onDismiss: () -> Unit,
    onRegenerateQr: ((memberId: String) -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showRegenerateConfirm by remember { mutableStateOf(false) }

    // Generate high-resolution bitmap for downloading/printing/sharing
    val cardBitmap = remember(member) {
        MemberCardGenerator.generateCardBitmap(context, member)
    }

    // Direct QR bitmap for interactive card preview
    val qrBitmap = remember(member.qrPayload) {
        QrCodeGenerator.generateQrBitmap(
            content = member.qrPayload,
            size = 480,
            foregroundHex = 0xFF0A2540.toInt(),
            backgroundHex = 0xFFFFFFFF.toInt()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .testTag("member_qr_card_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF8FAFC),
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Title and Close Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Member QR Card",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavySecondary
                        )
                        Text(
                            text = "Official Digital Membership Pass",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_qr_card_dialog_button")
                    ) {
                        Icon(imageVector = Icons.Filled.Close, contentDescription = "Close", tint = DeepNavySecondary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Professional Membership Card UI
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("digital_membership_card"),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0xFFCBD5E1), Color(0xFFE2E8F0)))
                    )
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Navy Card Header Banner
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF0A2540), Color(0xFF1E3A8A))
                                    )
                                )
                                .padding(vertical = 14.dp, horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "FIT & FINE NUTRITION CLUB",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 0.5.sp,
                                    textAlign = TextAlign.Center
                                )
                                Text(
                                    text = "DIGITAL MEMBERSHIP PASS",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MintAccent,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                                Text(
                                    text = "Health • Nutrition • Wellness",
                                    fontSize = 9.sp,
                                    color = Color(0xFF94A3B8),
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }

                        // Emerald Accent Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(EmeraldGreenPrimary)
                        )

                        // Member Info Section
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Member Avatar
                            MemberAvatar(
                                photoUrl = member.photoUrl,
                                name = member.fullName,
                                size = 64.dp,
                                border = BorderStroke(2.dp, EmeraldGreenPrimary),
                                testTag = "qr_dialog_member_avatar"
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Member Name
                            Text(
                                text = member.fullName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            // Member ID badge
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF1F5F9)
                            ) {
                                Text(
                                    text = "MEMBER ID: ${member.memberId}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepNavySecondary,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Status Badge (Active / Inactive)
                            val isActive = member.status == MemberStatus.ACTIVE
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isActive) ProfessionalGreen100 else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (isActive) "● ACTIVE MEMBER" else "● INACTIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) ProfessionalGreen700 else ErrorRed,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }

                            // Phone Number (if available)
                            if (member.mobileNumber.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = member.mobileNumber,
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Prominent QR Code Box
                            Box(
                                modifier = Modifier
                                    .size(200.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color.White)
                                    .border(1.5.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "Membership QR Code for ${member.fullName}",
                                    modifier = Modifier.size(184.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Scan for Attendance Check-in",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF334155)
                            )

                            Text(
                                text = "QR ID: ${member.qrPayload}",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        // Footer note
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF8FAFC))
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "FIT & FINE NUTRITION CLUB • Official Verified Pass",
                                fontSize = 9.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons Grid: DOWNLOAD, SHARE, PRINT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // DOWNLOAD CARD Button
                    Button(
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val success = MemberCardGenerator.downloadCardToDevice(context, cardBitmap, member)
                                withContext(Dispatchers.Main) {
                                    if (success) {
                                        Toast.makeText(
                                            context,
                                            "QR Card downloaded to Pictures / FitFineCards!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    } else {
                                        Toast.makeText(
                                            context,
                                            "Failed to download card image",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("download_card_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("DOWNLOAD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // SHARE CARD Button
                    Button(
                        onClick = {
                            MemberCardGenerator.shareCard(context, cardBitmap, member)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("share_card_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavySecondary),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SHARE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    // PRINT CARD Button
                    Button(
                        onClick = {
                            MemberCardGenerator.printCard(context, cardBitmap, member)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("print_card_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PRINT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Regenerate QR (Feature 9) & Close
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onRegenerateQr != null) {
                        OutlinedButton(
                            onClick = { showRegenerateConfirm = true },
                            modifier = Modifier
                                .height(38.dp)
                                .testTag("regenerate_qr_button"),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = WarningAmber
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Regenerate QR",
                                fontSize = 11.sp,
                                color = WarningAmber,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("dismiss_card_button"),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp)
                    ) {
                        Text("Close", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Confirmation Warning Dialog for QR Code Regeneration (FEATURE 9)
    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Warning",
                    tint = WarningAmber,
                    modifier = Modifier.size(36.dp)
                )
            },
            title = {
                Text(
                    text = "Regenerate QR Code?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = DeepNavySecondary
                )
            },
            text = {
                Text(
                    text = "Warning: Generating a new QR code will invalidate the previous physical or digital membership card for ${member.fullName}. The member will need to use the new QR card for future attendance scanning.\n\nDo you want to proceed?",
                    fontSize = 13.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateConfirm = false
                        onRegenerateQr?.invoke(member.memberId)
                        Toast.makeText(context, "New QR Code generated for ${member.fullName}", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = WarningAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("confirm_regenerate_qr_button")
                ) {
                    Text("Confirm Regenerate")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showRegenerateConfirm = false },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
