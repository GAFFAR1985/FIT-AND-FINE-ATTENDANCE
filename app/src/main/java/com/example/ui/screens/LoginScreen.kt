package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen

@Composable
fun LoginScreen(
    onSignIn: (email: String, name: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var adminEmail by remember { mutableStateOf("admin@fitfine.club") }
    var adminPassword by remember { mutableStateOf("admin123") }
    var adminName by remember { mutableStateOf("Club Teacher / Admin") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .verticalScroll(rememberScrollState())
            .testTag("login_screen")
    ) {
        // Hero Header with image
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.fitness_hero),
                contentDescription = "Fit and Fine Wellness Club",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )

            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0x660A192F),
                                Color(0xEE0A192F)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MintAccent),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.QrCodeScanner,
                            contentDescription = null,
                            tint = DeepNavySecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "FIT & FINE ATTENDANCE",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = "স্মার্ট কিউআর কোড উপস্থিতি সিস্টেম",
                            fontSize = 12.sp,
                            color = MintAccent
                        )
                    }
                }
            }
        }

        // Login Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = null,
                            tint = EmeraldGreenPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Admin Authorization",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavySecondary
                        )
                    }

                    Text(
                        text = "Sign in to manage members and record attendance scans.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )

                    // Google Sign-In Button (Standard flow)
                    Button(
                        onClick = {
                            onSignIn("admin@fitfine.club", "Google Admin User")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("google_sign_in_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavySecondary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Sign in with Google",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            text = "OR ADMIN CREDENTIALS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(horizontal = 10.dp)
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    OutlinedTextField(
                        value = adminName,
                        onValueChange = { adminName = it },
                        label = { Text("Admin / Teacher Name") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_name_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = adminEmail,
                        onValueChange = { adminEmail = it },
                        label = { Text("Admin Email") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_email_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = adminPassword,
                        onValueChange = { adminPassword = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("login_password_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Button(
                        onClick = {
                            onSignIn(adminEmail.ifBlank { "admin@fitfine.club" }, adminName.ifBlank { "Admin" })
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("admin_login_submit"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Enter Admin Dashboard",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // App Highlights
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFECFDF5),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FeatureCheckItem("1-Scan Instant Check-in with Camera QR Reader")
                    FeatureCheckItem("Duplicate Scan Prevention (Strict 1 record per day)")
                    FeatureCheckItem("Cloud Firestore Real-time Persistence & Export")
                }
            }
        }
    }
}

@Composable
fun FeatureCheckItem(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = SuccessGreen,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = Color(0xFF065F46),
            fontWeight = FontWeight.Medium
        )
    }
}
