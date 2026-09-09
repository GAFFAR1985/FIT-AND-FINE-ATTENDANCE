package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ToggleOff
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.data.model.Member
import com.example.data.model.MemberStatus
import com.example.ui.components.MemberAvatar
import com.example.ui.components.MemberQrCardDialog
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DeepNavySecondary
import com.example.ui.theme.EmeraldGreenPrimary
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.MintAccent
import com.example.ui.theme.SuccessGreen
import com.example.util.ImageUtils

@Composable
fun MembersScreen(
    members: List<Member>,
    onAddOrUpdateMember: (
        id: String,
        name: String,
        mobile: String,
        email: String,
        status: MemberStatus,
        photoBytes: ByteArray?,
        removePhoto: Boolean
    ) -> Unit,
    onDeleteMember: (memberId: String) -> Unit,
    onToggleStatus: (Member) -> Unit,
    onViewProfile: (Member) -> Unit,
    modifier: Modifier = Modifier,
    onRegenerateQr: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // All, Active, Inactive

    var showAddEditDialog by remember { mutableStateOf(false) }
    var memberToEdit by remember { mutableStateOf<Member?>(null) }
    var memberForQrModal by remember { mutableStateOf<Member?>(null) }
    var memberToDelete by remember { mutableStateOf<Member?>(null) }

    val filteredMembers = remember(members, searchQuery, selectedFilter) {
        members.filter { member ->
            val matchesSearch = searchQuery.isBlank() ||
                member.fullName.contains(searchQuery, ignoreCase = true) ||
                member.memberId.contains(searchQuery, ignoreCase = true) ||
                member.mobileNumber.contains(searchQuery)

            val matchesFilter = when (selectedFilter) {
                "Active" -> member.status == MemberStatus.ACTIVE
                "Inactive" -> member.status == MemberStatus.INACTIVE
                else -> true
            }
            matchesSearch && matchesFilter
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    memberToEdit = null
                    showAddEditDialog = true
                },
                containerColor = EmeraldGreenPrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.testTag("add_member_fab")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Filled.Add, contentDescription = "Add Member")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Add Member", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        },
        modifier = modifier.testTag("members_screen")
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Filter Header
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
                            text = "Member Directory",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepNavySecondary
                        )
                        Text(
                            text = "মোট নিবন্ধিত সদস্য: ${members.size} জন",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by name, mobile, or ID...", fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Filled.Search, contentDescription = null, tint = Color(0xFF64748B))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("member_search_input"),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldGreenPrimary,
                        unfocusedBorderColor = Color(0xFFCBD5E1)
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Status Filter Chips
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    listOf("All" to "সকল (${members.size})", "Active" to "সক্রিয় (${members.count { it.status == MemberStatus.ACTIVE }})", "Inactive" to "নিষ্ক্রিয় (${members.count { it.status == MemberStatus.INACTIVE }})").forEach { (key, label) ->
                        val isSelected = selectedFilter == key
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isSelected) EmeraldGreenPrimary else Color(0xFFF1F5F9),
                            modifier = Modifier
                                .clickable { selectedFilter = key }
                                .testTag("filter_chip_$key")
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else Color(0xFF334155),
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Member List
            if (filteredMembers.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Filled.Group,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No members match your search" else "No members registered yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "নতুন সদস্য নিবন্ধন করতে নিচের '+ Add Member' বাটনে চাপ দিন।",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredMembers, key = { it.memberId }) { member ->
                        MemberCard(
                            member = member,
                            onShowQr = { memberForQrModal = member },
                            onEdit = {
                                memberToEdit = member
                                showAddEditDialog = true
                            },
                            onDelete = { memberToDelete = member },
                            onToggleStatus = { onToggleStatus(member) },
                            onViewProfile = { onViewProfile(member) }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }
    }

    // Add/Edit Member Dialog
    if (showAddEditDialog) {
        AddEditMemberDialog(
            memberToEdit = memberToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { id, name, mobile, email, status, photoBytes, removePhoto ->
                onAddOrUpdateMember(id, name, mobile, email, status, photoBytes, removePhoto)
                showAddEditDialog = false
            }
        )
    }

    // Member QR Modal - Full Professional Membership Card Dialog
    memberForQrModal?.let { member ->
        val currentMember = members.find { it.memberId == member.memberId } ?: member
        MemberQrCardDialog(
            member = currentMember,
            onDismiss = { memberForQrModal = null },
            onRegenerateQr = onRegenerateQr
        )
    }

    // Delete Confirmation Dialog
    memberToDelete?.let { member ->
        AlertDialog(
            onDismissRequest = { memberToDelete = null },
            title = { Text("Delete Member") },
            text = { Text("Are you sure you want to delete ${member.fullName} (${member.memberId})? This will permanently remove their profile.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteMember(member.memberId)
                        memberToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { memberToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun MemberCard(
    member: Member,
    onShowQr: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onViewProfile: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewProfile() }
            .testTag("member_card_${member.memberId}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            MemberAvatar(
                photoUrl = member.photoUrl,
                name = member.fullName,
                size = 46.dp,
                testTag = "member_card_avatar_${member.memberId}"
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = member.fullName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = "ID: ${member.memberId}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldGreenPrimary
                )

                if (member.mobileNumber.isNotBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = member.mobileNumber,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))
                StatusBadge(status = member.status)
            }

            // Quick QR Button
            IconButton(
                onClick = onShowQr,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1F5F9))
                    .testTag("member_qr_btn_${member.memberId}")
            ) {
                Icon(
                    imageVector = Icons.Filled.QrCode,
                    contentDescription = "View QR",
                    tint = DeepNavySecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Options Menu
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier.testTag("member_options_btn_${member.memberId}")
                ) {
                    Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "Options")
                }

                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("View Profile & Attendance") },
                        onClick = {
                            menuExpanded = false
                            onViewProfile()
                        },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Show QR Code") },
                        onClick = {
                            menuExpanded = false
                            onShowQr()
                        },
                        leadingIcon = { Icon(Icons.Filled.QrCode, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit Details") },
                        onClick = {
                            menuExpanded = false
                            onEdit()
                        },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(if (member.status == MemberStatus.ACTIVE) "Deactivate" else "Activate") },
                        onClick = {
                            menuExpanded = false
                            onToggleStatus()
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = if (member.status == MemberStatus.ACTIVE) Icons.Filled.ToggleOff else Icons.Filled.ToggleOn,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = ErrorRed) }
                    )
                }
            }
        }
    }
}

@Composable
fun AddEditMemberDialog(
    memberToEdit: Member?,
    onDismiss: () -> Unit,
    onSave: (
        id: String,
        name: String,
        mobile: String,
        email: String,
        status: MemberStatus,
        photoBytes: ByteArray?,
        removePhoto: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    var memberId by remember { mutableStateOf(memberToEdit?.memberId ?: "") }
    var name by remember { mutableStateOf(memberToEdit?.fullName ?: "") }
    var mobile by remember { mutableStateOf(memberToEdit?.mobileNumber ?: "") }
    var email by remember { mutableStateOf(memberToEdit?.email ?: "") }
    var status by remember { mutableStateOf(memberToEdit?.status ?: MemberStatus.ACTIVE) }

    // Photo selection state
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var photoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var removePhoto by remember { mutableStateOf(false) }
    val currentPhotoUrl = memberToEdit?.photoUrl ?: ""
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val bytes = ImageUtils.compressImageFromUri(context, uri)
            if (bytes != null) {
                photoBytes = bytes
                selectedImageUri = uri
                removePhoto = false
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success && tempCameraUri != null) {
            val bytes = ImageUtils.compressImageFromUri(context, tempCameraUri!!)
            if (bytes != null) {
                photoBytes = bytes
                selectedImageUri = tempCameraUri
                removePhoto = false
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            val (uri, _) = ImageUtils.createTempCameraUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun openCamera() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            val (uri, _) = ImageUtils.createTempCameraUri(context)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val hasPhoto = (selectedImageUri != null) || (!removePhoto && currentPhotoUrl.isNotBlank())

    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (memberToEdit == null) "Register New Member" else "Edit Member Details",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Profile Photo Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_photo_section"),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Member Photo (সদস্য ছবি)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF475569)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Avatar Preview
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedImageUri != null) {
                                AsyncImage(
                                    model = selectedImageUri,
                                    contentDescription = "Selected Photo",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else if (!removePhoto && currentPhotoUrl.isNotBlank()) {
                                MemberAvatar(
                                    photoUrl = currentPhotoUrl,
                                    name = name,
                                    size = 80.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = "Default Avatar",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Photo Actions
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { openCamera() },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("dialog_camera_btn")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Camera", fontSize = 12.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("dialog_gallery_btn")
                            ) {
                                Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Gallery", fontSize = 12.sp)
                            }

                            if (hasPhoto) {
                                OutlinedButton(
                                    onClick = {
                                        removePhoto = true
                                        selectedImageUri = null
                                        photoBytes = null
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .height(38.dp)
                                        .testTag("dialog_remove_photo_btn")
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Remove", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                if (memberToEdit == null) {
                    OutlinedTextField(
                        value = memberId,
                        onValueChange = { memberId = it },
                        label = { Text("Member ID (e.g. FN-1001 or leave blank)") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_member_id_input"),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorMsg = null
                    },
                    label = { Text("Full Name (সম্পূর্ণ নাম) *") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_member_name_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = mobile,
                    onValueChange = { mobile = it },
                    label = { Text("Mobile Number (মোবাইল নম্বর)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_member_phone_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (ঐচ্ছিক)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dialog_member_email_input"),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Status:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = status == MemberStatus.ACTIVE,
                            onClick = { status = MemberStatus.ACTIVE },
                            colors = RadioButtonDefaults.colors(selectedColor = SuccessGreen)
                        )
                        Text("Active", fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = status == MemberStatus.INACTIVE,
                            onClick = { status = MemberStatus.INACTIVE },
                            colors = RadioButtonDefaults.colors(selectedColor = ErrorRed)
                        )
                        Text("Inactive", fontSize = 13.sp)
                    }
                }

                errorMsg?.let {
                    Text(text = it, color = ErrorRed, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMsg = "Please enter member name"
                        return@Button
                    }
                    onSave(memberId, name, mobile, email, status, photoBytes, removePhoto)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreenPrimary),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_save_member_button")
            ) {
                Text("Save Member")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("dialog_cancel_member_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
