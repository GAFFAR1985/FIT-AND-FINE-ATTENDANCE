package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.ui.components.FitFineBottomBar
import com.example.ui.components.NavItem
import com.example.ui.screens.AddEditMemberDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MemberProfileScreen
import com.example.ui.screens.MembersScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FitAndFineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FitAndFineTheme {
                MainApp()
            }
        }
    }
}

@Composable
fun MainApp(viewModel: MainViewModel = viewModel()) {
    val currentUser by viewModel.currentUser.collectAsState()
    val metrics by viewModel.metrics.collectAsState()
    val members by viewModel.members.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val allAttendance by viewModel.allAttendance.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()
    val isScanningActive by viewModel.isScanningActive.collectAsState()

    var currentScreen by remember { mutableStateOf(NavItem.HOME) }
    var selectedMemberForProfile by remember { mutableStateOf<Member?>(null) }
    var showAddMemberDialogFromHome by remember { mutableStateOf(false) }

    // If unauthenticated, display the Admin Login Screen
    if (!currentUser.isAuthenticated) {
        LoginScreen(
            onSignIn = { email, name ->
                viewModel.signInAsAdmin(email, name)
            }
        )
        return
    }

    // If viewing a member's detail profile
    selectedMemberForProfile?.let { member ->
        val currentMember = members.find { it.memberId == member.memberId } ?: member
        MemberProfileScreen(
            member = currentMember,
            attendanceRecords = allAttendance,
            onBack = { selectedMemberForProfile = null },
            onRegenerateQr = { id -> viewModel.regenerateMemberQrCode(id) }
        )
        return
    }

    // Main App Navigation Shell
    Scaffold(
        bottomBar = {
            FitFineBottomBar(
                currentScreen = currentScreen,
                onNavigate = { destination ->
                    currentScreen = destination
                    if (destination != NavItem.SCAN) {
                        viewModel.resetScanResult()
                    }
                }
            )
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                NavItem.HOME -> {
                    DashboardScreen(
                        metrics = metrics,
                        recentAttendance = todayAttendance,
                        settings = settings,
                        isFirebaseConfigured = viewModel.isFirebaseConfigured,
                        onNavigate = { destination ->
                            currentScreen = destination
                        },
                        onAddMemberClick = {
                            showAddMemberDialogFromHome = true
                        }
                    )
                }

                NavItem.SCAN -> {
                    ScannerScreen(
                        scanResult = scanResult,
                        isScanningActive = isScanningActive,
                        members = members,
                        onQrScanned = { rawCode ->
                            viewModel.onQrScanned(rawCode)
                        },
                        onResetScan = {
                            viewModel.resetScanResult()
                        }
                    )
                }

                NavItem.MEMBERS -> {
                    MembersScreen(
                        members = members,
                        onAddOrUpdateMember = { id, name, mobile, email, status, photoBytes, removePhoto ->
                            viewModel.addOrUpdateMember(id, name, mobile, email, status, photoBytes, removePhoto)
                        },
                        onDeleteMember = { memberId ->
                            viewModel.deleteMember(memberId)
                        },
                        onToggleStatus = { member ->
                            viewModel.toggleMemberStatus(member)
                        },
                        onViewProfile = { member ->
                            selectedMemberForProfile = member
                        },
                        onRegenerateQr = { id ->
                            viewModel.regenerateMemberQrCode(id)
                        }
                    )
                }

                NavItem.REPORTS -> {
                    ReportsScreen(
                        allAttendance = allAttendance,
                        members = members,
                        viewModel = viewModel
                    )
                }

                NavItem.MENU -> {
                    SettingsScreen(
                        settings = settings,
                        currentUser = currentUser,
                        isFirebaseConfigured = viewModel.isFirebaseConfigured,
                        onSaveSettings = { newSettings ->
                            viewModel.saveClubSettings(newSettings)
                        },
                        onSignOut = {
                            viewModel.signOut()
                        }
                    )
                }
            }
        }
    }

    // Add Member Dialog triggered from Dashboard Quick Action
    if (showAddMemberDialogFromHome) {
        AddEditMemberDialog(
            memberToEdit = null,
            onDismiss = { showAddMemberDialogFromHome = false },
            onSave = { id, name, mobile, email, status, photoBytes, removePhoto ->
                viewModel.addOrUpdateMember(id, name, mobile, email, status, photoBytes, removePhoto)
                showAddMemberDialogFromHome = false
            }
        )
    }
}
