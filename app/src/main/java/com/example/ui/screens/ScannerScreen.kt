package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.model.Member
import com.example.data.repository.ScanResult
import com.example.ui.qr.QrCodeAnalyzer
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    scanResult: ScanResult?,
    isScanningActive: Boolean,
    members: List<Member>,
    onQrScanned: (String) -> Unit,
    onResetScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val cameraPermissionState = rememberPermissionState(permission = Manifest.permission.CAMERA)
    var camera by remember { mutableStateOf<Camera?>(null) }
    var isTorchOn by remember { mutableStateOf(false) }
    var useFrontCamera by remember { mutableStateOf(false) }
    var manualInputText by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }

    val currentIsScanningActive by rememberUpdatedState(isScanningActive)
    val currentOnQrScanned by rememberUpdatedState(onQrScanned)

    // Single background executor for barcode image analysis
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose {
            try {
                cameraExecutor.shutdown()
            } catch (_: Exception) {}
        }
    }

    // Stable PreviewView instance - NEVER recreated on recomposition
    val previewView = remember(context) {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            implementationMode = PreviewView.ImplementationMode.PERFORMANCE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    // Clean CameraX lifecycle binding & unbinding
    DisposableEffect(lifecycleOwner, useFrontCamera, cameraPermissionState.status.isGranted) {
        var cameraProvider: ProcessCameraProvider? = null

        if (cameraPermissionState.status.isGranted) {
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            val mainExecutor = ContextCompat.getMainExecutor(context)

            cameraProviderFuture.addListener({
                try {
                    val provider = cameraProviderFuture.get()
                    cameraProvider = provider

                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = if (useFrontCamera) {
                        CameraSelector.DEFAULT_FRONT_CAMERA
                    } else {
                        CameraSelector.DEFAULT_BACK_CAMERA
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor, QrCodeAnalyzer { qrText ->
                        if (currentIsScanningActive) {
                            currentOnQrScanned(qrText)
                        }
                    })

                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (e: Exception) {
                    // Safe bind exception handler
                }
            }, mainExecutor)
        }

        onDispose {
            try {
                camera?.cameraControl?.enableTorch(false)
            } catch (_: Exception) {}
            camera = null
            isTorchOn = false
            try {
                cameraProvider?.unbindAll()
            } catch (_: Exception) {}
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("scanner_screen")
    ) {
        if (cameraPermissionState.status.isGranted) {
            // Live CameraX PreviewView filling the camera preview area cleanly
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Camera permission required fallback
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0F172A))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCodeScanner,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Please allow camera access to scan member QR codes.",
                    color = Color(0xFF94A3B8),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)
                )
                Button(
                    onClick = { cameraPermissionState.launchPermissionRequest() },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Grant Camera Permission")
                }
            }
        }

        // Green QR scanning rectangle overlay ABOVE the camera preview
        // Completely transparent inside; does NOT modify or tint camera pixels
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(260.dp)
                        .border(3.dp, MintAccent, RoundedCornerShape(24.dp))
                        .testTag("scanner_reticle_frame")
                )
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0x99000000)
                ) {
                    Text(
                        text = "Align QR code within frame",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Top Bar Controls (Floating pill chips - no gradient or canvas covering the preview)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0x99000000)
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text(
                        text = "Scan Attendance QR",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "সদস্যের কিউআর কোড স্ক্যান করুন",
                        color = MintAccent,
                        fontSize = 11.sp
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Flash / Torch toggle
                IconButton(
                    onClick = {
                        val newTorch = !isTorchOn
                        isTorchOn = newTorch
                        camera?.cameraControl?.enableTorch(newTorch)
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("scanner_torch_button")
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                        contentDescription = "Torch",
                        tint = if (isTorchOn) Color(0xFFFBBF24) else Color.White
                    )
                }

                // Flip camera
                IconButton(
                    onClick = {
                        isTorchOn = false
                        useFrontCamera = !useFrontCamera
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x99000000))
                        .testTag("scanner_flip_camera_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlipCameraAndroid,
                        contentDescription = "Flip Camera",
                        tint = Color.White
                    )
                }

                // Manual input toggle
                IconButton(
                    onClick = { showManualInput = !showManualInput },
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (showManualInput) EmeraldGreenPrimary else Color(0x99000000))
                        .testTag("scanner_manual_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Keyboard,
                        contentDescription = "Manual Check-in",
                        tint = Color.White
                    )
                }
            }
        }

        // Bottom Manual Check-in / Simulator Bar (Floats at bottom edge)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (showManualInput) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Manual ID / Barcode Check-in",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualInputText,
                                onValueChange = { manualInputText = it },
                                placeholder = { Text("Enter Member ID or QR code", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp)
                                    .testTag("manual_scanner_input"),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = MintAccent,
                                    unfocusedBorderColor = Color(0xFF475569)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (manualInputText.isNotBlank()) {
                                        currentOnQrScanned(manualInputText.trim())
                                        manualInputText = ""
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("manual_scanner_submit")
                            ) {
                                Text("Mark")
                            }
                        }
                    }
                }
            }

            // Quick Tap Member Pills for instant test scanning
            if (members.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0x99000000),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text(
                            text = "Quick Test Scan (Click registered member to simulate scan):",
                            color = Color(0xFF94A3B8),
                            fontSize = 11.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(members.take(6)) { member ->
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = Color(0xFF334155),
                                    modifier = Modifier
                                        .clickable { currentOnQrScanned(member.qrPayload) }
                                        .testTag("test_scan_member_${member.memberId}")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.QrCode,
                                            contentDescription = null,
                                            tint = MintAccent,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${member.fullName} (${member.memberId})",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Large Scan Result Modal Card Overlay
        AnimatedVisibility(
            visible = scanResult != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            scanResult?.let { result ->
                ScanResultCard(
                    result = result,
                    onDismiss = onResetScan
                )
            }
        }
    }
}

@Composable
fun ScanResultCard(
    result: ScanResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("scan_result_modal"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (result) {
                is ScanResult.Success -> {
                    // Large Success UI
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Attendance Marked Successfully",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "উপস্থিতি সফলভাবে রেকর্ড করা হয়েছে",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailRow("Member Name:", result.member.fullName, isBold = true)
                            DetailRow("Member ID:", result.member.memberId)
                            DetailRow("Date:", result.record.formattedDateDisplay)
                            DetailRow("Scan Time:", result.record.formattedTime)
                            DetailRow("Status:", "Present (উপস্থিত)", highlight = true)
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("scan_next_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary)
                    ) {
                        Icon(imageVector = Icons.Filled.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Scan Next QR Code", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                is ScanResult.AlreadyMarked -> {
                    // Duplicate Warning UI
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Warning,
                            contentDescription = "Already Marked",
                            tint = WarningAmber,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Attendance already recorded for today.",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "এই সদস্যের উপস্থিতি আজ আগেই নেওয়া হয়েছে",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB)
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DetailRow("Member Name:", result.member.fullName, isBold = true)
                            DetailRow("Member ID:", result.member.memberId)
                            DetailRow("Today's Date:", result.originalRecord.formattedDateDisplay)
                            DetailRow("Original Check-in Time:", result.originalRecord.formattedTime, isBold = true)
                            DetailRow("Rule:", "1 attendance per calendar day")
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_duplicate_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavySecondary)
                    ) {
                        Text(text = "OK, Continue", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                is ScanResult.MemberNotFound -> {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Not Found",
                            tint = ErrorRed,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Invalid QR / Member Not Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "কোনো নিবন্ধিত সদস্যের সাথে এই কোড মিলেনি।",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                    Text(
                        text = "Scanned Code: ${result.scannedCode}",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_not_found_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                    ) {
                        Text(text = "Try Again", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                is ScanResult.InactiveMember -> {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = "Inactive",
                            tint = ErrorRed,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Member is Inactive",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = ErrorRed,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${result.member.fullName} এর অ্যাকাউন্টটি নিষ্ক্রিয় রয়েছে। মেম্বার ম্যানেজমেন্ট থেকে সক্রিয় করুন।",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("dismiss_inactive_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                    ) {
                        Text(text = "Dismiss", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                }

                is ScanResult.Error -> {
                    Text(
                        text = "Error: ${result.message}",
                        color = ErrorRed,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onDismiss) { Text("Close") }
                }
            }
        }
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    isBold: Boolean = false,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF64748B)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = if (isBold || highlight) FontWeight.Bold else FontWeight.Medium,
            color = if (highlight) SuccessGreen else Color(0xFF0F172A)
        )
    }
}
