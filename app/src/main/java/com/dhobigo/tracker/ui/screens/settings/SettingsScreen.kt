package com.dhobigo.tracker.ui.screens.settings

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dhobigo.tracker.data.local.entity.ClothingCategoryEntity
import com.dhobigo.tracker.ui.components.ConfirmationDialog
import com.dhobigo.tracker.ui.theme.BorderLight
import com.dhobigo.tracker.ui.theme.DangerRed
import com.dhobigo.tracker.ui.theme.ForestGreen
import com.dhobigo.tracker.ui.theme.MintAccent
import com.dhobigo.tracker.ui.theme.SurfaceSubtle
import com.dhobigo.tracker.ui.theme.SurfaceWhite
import com.dhobigo.tracker.ui.theme.TextMuted
import com.dhobigo.tracker.ui.theme.TextPrimary
import com.dhobigo.tracker.ui.theme.TextSecondary
import com.dhobigo.tracker.ui.theme.WarmCream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showPrivacyPolicyDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<ClothingCategoryEntity?>(null) }
    var dhobiNameInput by remember(uiState.defaultDhobiName) { mutableStateOf(uiState.defaultDhobiName) }

    // SAF Document Launchers
    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        uri?.let { viewModel.exportBackup(it) }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.restoreBackup(it) }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is SettingsEvent.ShowToast -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_LONG).show()
                }
                is SettingsEvent.LoggedOut, is SettingsEvent.AccountDeleted -> {
                    onSignOut()
                }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(WarmCream),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Header
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Text(
                    text = "Account & Settings",
                    color = TextPrimary,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Manage your profile, clothing items, backups, and privacy",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            }
        }

        // Section 1: User Profile Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = ForestGreen.copy(alpha = 0.12f),
                                modifier = Modifier.size(48.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ForestGreen,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = uiState.currentUser?.fullName ?: "Loading profile...",
                                    color = TextPrimary,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = uiState.currentUser?.email ?: "",
                                    color = TextSecondary,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        IconButton(onClick = { showEditProfileDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = ForestGreen)
                        }
                    }

                    // Optional Student info if present
                    val hasAcademicInfo = !uiState.currentUser?.college.isNullOrBlank() ||
                            !uiState.currentUser?.enrollmentNumber.isNullOrBlank() ||
                            !uiState.currentUser?.course.isNullOrBlank()

                    if (hasAcademicInfo) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = SurfaceSubtle)
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.School, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = listOfNotNull(
                                    uiState.currentUser?.college?.ifBlank { null },
                                    uiState.currentUser?.course?.ifBlank { null },
                                    uiState.currentUser?.batch?.let { "Batch $it" }
                                ).joinToString(" • "),
                                color = TextMuted,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        }

                        if (!uiState.currentUser?.enrollmentNumber.isNullOrBlank()) {
                            Text(
                                text = "Roll / Enrollment: ${uiState.currentUser?.enrollmentNumber}",
                                color = TextMuted,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(start = 22.dp, top = 2.dp)
                            )
                        }
                    }

                    if (!uiState.currentUser?.phone.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Phone: ${uiState.currentUser?.phone}",
                            color = TextMuted,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SurfaceSubtle)
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Sign Out", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Section 2: Default Dhobi / Shop Name
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storefront, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Default Dhobi / Shop Name",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Pre-fills the shop name when creating a new laundry slip.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dhobiNameInput,
                        onValueChange = {
                            dhobiNameInput = it
                            viewModel.updateDefaultDhobiName(it)
                        },
                        placeholder = { Text("e.g. Campus Dhobi / Raju Laundry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ForestGreen,
                            unfocusedBorderColor = BorderLight
                        )
                    )
                }
            }
        }

        // Section 3: Clothing Categories Manager
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Checkroom, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Clothing Categories",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = { showAddCategoryDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add Category", tint = ForestGreen)
                        }
                    }

                    Text(
                        text = "Customize the apparel items available when adding a laundry slip.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    categories.forEachIndexed { index, category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = category.name,
                                    color = if (category.isActive) TextPrimary else TextMuted,
                                    fontSize = 14.sp,
                                    fontWeight = if (category.isActive) FontWeight.Medium else FontWeight.Normal
                                )
                                if (category.isDefault) {
                                    Text("Default item", color = TextMuted, fontSize = 11.sp)
                                } else {
                                    Text("Custom item", color = MintAccent, fontSize = 11.sp)
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!category.isDefault) {
                                    IconButton(
                                        onClick = { categoryToDelete = category },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = DangerRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Switch(
                                    checked = category.isActive,
                                    onCheckedChange = { viewModel.toggleCategoryActive(category) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = SurfaceWhite,
                                        checkedTrackColor = ForestGreen
                                    )
                                )
                            }
                        }

                        if (index < categories.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = SurfaceSubtle
                            )
                        }
                    }
                }
            }
        }

        // Section 4: Safe Local Backup & Restore
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Backup, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Backup & Restore",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Export your laundry slips and original chit photos to a secure ZIP file. Restoring safely imports and re-scopes the records to your active account.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                                exportBackupLauncher.launch("DhobiGo_Backup_$timestamp.zip")
                            },
                            enabled = !uiState.isBackingUp,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isBackingUp) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SurfaceWhite, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        OutlinedButton(
                            onClick = {
                                restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                            },
                            enabled = !uiState.isRestoring,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, ForestGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            if (uiState.isRestoring) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = ForestGreen, strokeWidth = 2.dp)
                            } else {
                                Icon(Icons.Default.Restore, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore", color = ForestGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // Section 5: Privacy Policy & App Security Info
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, BorderLight)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Security, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy & Security",
                            color = TextPrimary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "• 100% Offline & Private: No external servers or analytics telemetry.\n" +
                                "• Strict Account Isolation: Multi-user data stored securely on-device with PBKDF2 hashing.\n" +
                                "• Private Photos: Chit photos stored in isolated user sandbox directories.\n" +
                                "• On-Device ML: Optical Character Recognition runs entirely on your phone.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SurfaceSubtle)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showPrivacyPolicyDialog = true }) {
                            Icon(Icons.Default.Policy, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Read Privacy Policy", color = ForestGreen, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Text(
                            text = "v1.0.0 (Production)",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Section 6: Danger Zone (Account Deletion)
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.04f)),
                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.25f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null, tint = DangerRed, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Danger Zone",
                            color = DangerRed,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Permanently delete your account, all laundry history, inventory counts, and original chit photos. This action cannot be undone.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showDeleteAccountDialog = true },
                        enabled = !uiState.isDeletingAccount,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DangerRed),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState.isDeletingAccount) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SurfaceWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Delete Account & Data", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        var newName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Clothing Category", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Scarf, Blanket, Blazer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ForestGreen,
                        unfocusedBorderColor = BorderLight
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newName.isNotBlank()) {
                            viewModel.addCategory(newName.trim())
                        }
                        showAddCategoryDialog = false
                    }
                ) {
                    Text("Add", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Profile Dialog
    if (showEditProfileDialog) {
        val user = uiState.currentUser
        var editName by remember { mutableStateOf(user?.fullName ?: "") }
        var editPhone by remember { mutableStateOf(user?.phone ?: "") }
        var editCollege by remember { mutableStateOf(user?.college ?: "") }
        var editEnrollment by remember { mutableStateOf(user?.enrollmentNumber ?: "") }
        var editCourse by remember { mutableStateOf(user?.course ?: "") }
        var editYear by remember { mutableStateOf(user?.academicYear ?: "") }
        var editBatch by remember { mutableStateOf(user?.batch ?: "") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Full Name *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )
                    OutlinedTextField(
                        value = editCollege,
                        onValueChange = { editCollege = it },
                        label = { Text("College / University") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )
                    OutlinedTextField(
                        value = editEnrollment,
                        onValueChange = { editEnrollment = it },
                        label = { Text("Enrollment / Roll Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editCourse,
                            onValueChange = { editCourse = it },
                            label = { Text("Course") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                        )
                        OutlinedTextField(
                            value = editBatch,
                            onValueChange = { editBatch = it },
                            label = { Text("Batch") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ForestGreen)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.updateProfile(
                            fullName = editName,
                            phone = editPhone,
                            college = editCollege,
                            enrollmentNumber = editEnrollment,
                            course = editCourse,
                            academicYear = editYear,
                            batch = editBatch
                        )
                        showEditProfileDialog = false
                    }
                ) {
                    Text("Save", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Privacy Policy Dialog
    if (showPrivacyPolicyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyPolicyDialog = false },
            title = { Text("Privacy Policy & Terms", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "DhobiGo Privacy Policy",
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "1. Data Storage: All laundry chits, garment tallies, receipts, and user account credentials are stored strictly on-device in app-private encrypted/sandboxed storage.\n\n" +
                                "2. Telemetry & Analytics: DhobiGo contains NO third-party advertising, trackers, analytics, or background telemetry.\n\n" +
                                "3. Password Security: Passwords are protected using salted PBKDF2 cryptography with HMAC-SHA256 and 10,000 iterations.\n\n" +
                                "4. Account Isolation: Every record and photo is scoped to an immutable user ID. Multi-user installations cannot access cross-account records.\n\n" +
                                "5. Data Deletion: You can permanently delete all account information, laundry slips, and captured photos instantly using 'Delete Account' in Settings.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 17.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyPolicyDialog = false }) {
                    Text("Understood", color = ForestGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Delete Custom Category Confirmation Dialog
    categoryToDelete?.let { cat ->
        ConfirmationDialog(
            title = "Delete Category?",
            message = "Are you sure you want to delete the custom category '${cat.name}'? Existing records using this category will retain their counts.",
            confirmButtonText = "Delete",
            dismissButtonText = "Cancel",
            isDestructive = true,
            onConfirm = {
                viewModel.deleteCustomCategory(cat)
                categoryToDelete = null
            },
            onDismiss = { categoryToDelete = null }
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        ConfirmationDialog(
            title = "Sign Out?",
            message = "Are you sure you want to sign out? Your records and photos will remain safely stored on this device until you sign back in.",
            confirmButtonText = "Sign Out",
            dismissButtonText = "Stay Signed In",
            isDestructive = false,
            onConfirm = {
                showLogoutDialog = false
                viewModel.logout()
            },
            onDismiss = { showLogoutDialog = false }
        )
    }

    // Account Deletion Confirmation Dialog
    if (showDeleteAccountDialog) {
        ConfirmationDialog(
            title = "Permanently Delete Account?",
            message = "WARNING: This will permanently delete your account, all laundry bags, item return histories, and chit photos from this device. This action cannot be reversed.",
            confirmButtonText = "Delete Everything",
            dismissButtonText = "Cancel",
            isDestructive = true,
            onConfirm = {
                showDeleteAccountDialog = false
                viewModel.deleteAccount()
            },
            onDismiss = { showDeleteAccountDialog = false }
        )
    }
}
