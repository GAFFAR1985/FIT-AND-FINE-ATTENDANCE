package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AdminUser
import com.example.data.model.ClubSettings
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber

@Composable
fun SettingsScreen(
    settings: ClubSettings,
    currentUser: AdminUser,
    isFirebaseConfigured: Boolean,
    onSaveSettings: (ClubSettings) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var clubName by remember { mutableStateOf(settings.clubName) }
    var adminEmail by remember { mutableStateOf(settings.adminEmail) }
    var morningCutoff by remember { mutableStateOf(settings.morningCutoffTime) }
    var workingDays by remember { mutableStateOf(settings.workingDays.toSet()) }
    var isSavedToast by remember { mutableStateOf(false) }

    val allDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column {
                Text(
                    text = "App Settings & Configuration",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepNavySecondary
                )
                Text(
                    text = "ক্লাব সেটিংস এবং ফায়ারবেস ক্লাউড ডাটাবেস",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
            }
        }

        // Firebase Cloud Firestore & Auth Status Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("firebase_configuration_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFirebaseConfigured) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (isFirebaseConfigured) SuccessGreen.copy(alpha = 0.4f) else WarningAmber.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(if (isFirebaseConfigured) SuccessGreen.copy(alpha = 0.15f) else WarningAmber.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isFirebaseConfigured) Icons.Filled.Cloud else Icons.Filled.CloudOff,
                                contentDescription = null,
                                tint = if (isFirebaseConfigured) SuccessGreen else WarningAmber,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column {
                            Text(
                                text = if (isFirebaseConfigured) "Firebase Cloud Firestore: Connected" else "Firebase Cloud Setup & Sync Status",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isFirebaseConfigured) SuccessGreen else Color(0xFF92400E)
                            )
                            Text(
                                text = if (isFirebaseConfigured) "Real-time sync enabled across all devices & sessions" else "Local Persistent Mode Active (Room SQLite)",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isFirebaseConfigured) {
                        Text(
                            text = "Attendance records and members are securely synced with Firebase Cloud Firestore in real time with duplicate prevention.",
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                    } else {
                        Text(
                            text = "To enable cloud sync across multiple devices & Google Sign-In:\n" +
                                "1. Download your google-services.json from Firebase Console.\n" +
                                "2. Place it in the app/ folder.\n" +
                                "3. Enable Firestore & Google Authentication in Firebase.\n" +
                                "4. In the meantime, all your members and attendance are safely saved on this device!",
                            fontSize = 12.sp,
                            color = Color(0xFF475569),
                            lineHeight = 17.sp
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Copy rules button
                        OutlinedButton(
                            onClick = {
                                val rules = "rules_version = '2';\nservice cloud.firestore {\n  match /databases/{database}/documents {\n    match /attendance/{id} { allow read, write: if request.auth != null; }\n    match /members/{id} { allow read, write: if request.auth != null; }\n  }\n}"
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Firestore Rules", rules)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Security Rules copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("copy_rules_button")
                        ) {
                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Firestore Security Rules", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Club Profile Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Business, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Organization Profile", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepNavySecondary)
                    }

                    OutlinedTextField(
                        value = clubName,
                        onValueChange = { clubName = it },
                        label = { Text("Club / Organization Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_club_name_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Admin Contact Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_admin_email_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Working Days & Attendance Rules
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Rule, contentDescription = null, tint = EmeraldGreenPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Attendance Rules & Schedule", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepNavySecondary)
                    }

                    Text(
                        text = "Club Working Days (কার্যদিবস নির্বাচন করুন):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )

                    // Working days grid
                    Column {
                        allDays.chunked(4).forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                rowDays.forEach { day ->
                                    val isChecked = workingDays.contains(day)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Checkbox(
                                            checked = isChecked,
                                            onCheckedChange = { checked ->
                                                workingDays = if (checked) workingDays + day else workingDays - day
                                            },
                                            colors = CheckboxDefaults.colors(checkedColor = EmeraldGreenPrimary)
                                        )
                                        Text(text = day, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = morningCutoff,
                        onValueChange = { morningCutoff = it },
                        label = { Text("Morning Attendance Cutoff Time (e.g. 10:30 AM)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_cutoff_input"),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true
                    )
                }
            }
        }

        // Save Settings Button
        item {
            Button(
                onClick = {
                    onSaveSettings(
                        settings.copy(
                            clubName = clubName.trim().ifBlank { "FIT & FINE NUTRITION CLUB" },
                            adminEmail = adminEmail.trim(),
                            morningCutoffTime = morningCutoff.trim(),
                            workingDays = workingDays.toList()
                        )
                    )
                    Toast.makeText(context, "Settings saved successfully!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_settings_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
            ) {
                Icon(imageVector = Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Settings", fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Admin Account & Sign Out
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Filled.Person, contentDescription = null, tint = DeepNavySecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Logged In Admin", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = DeepNavySecondary)
                    }

                    Text(
                        text = "Name: ${currentUser.displayName.ifBlank { "Admin" }}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Email: ${currentUser.email.ifBlank { "admin@fitfine.club" }}",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("admin_sign_out_button"),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out Admin", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
