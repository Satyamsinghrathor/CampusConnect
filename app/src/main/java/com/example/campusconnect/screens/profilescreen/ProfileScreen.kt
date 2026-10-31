package com.example.campusconnect.screens.profilescreen

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusconnect.data.model.ProfileOptions
import com.example.campusconnect.data.model.UserProfile
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.data.model.yearSemesterLabel
import com.example.campusconnect.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    rootNavController: NavHostController,
    navController: NavHostController,
    darkTheme: Boolean,
    onToggleTheme: () -> Unit,
    viewModel: LogoutViewModel = viewModel()
) {
    val profileViewModel: ProfileViewModel = viewModel()
    val state by profileViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) profileViewModel.onPhotoPicked(uri)
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            profileViewModel.consumeMessage()
        }
    }

    // While editing, the system back gesture discards the edit instead of leaving the screen.
    BackHandler(enabled = state.isEditing) { profileViewModel.cancelEditing() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit Profile" else "Profile") },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.isEditing) profileViewModel.cancelEditing()
                            else navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (state.loadError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = state.loadError ?: "",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(16.dp))
                Button(onClick = profileViewModel::load) { Text("Retry") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp)
        ) {
            val profile = state.profile
            val subtitle = if (profile.userRole == UserRole.STUDENT) {
                listOf(state.departmentName, profile.yearSemesterLabel())
            } else {
                listOf(profile.designation.ifBlank { profile.userRole.label }, state.departmentName)
            }
                .filter { it.isNotBlank() }
                .joinToString(" · ")
                .ifBlank { profile.userRole.label }

            ProfileHeader(
                name = profile.name.ifBlank { "Your name" },
                subtitle = subtitle,
                photo = if (state.isEditing) state.draftPhoto ?: state.photo else state.photo,
                editable = state.isEditing,
                onAvatarClick = {
                    photoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                }
            )

            Spacer(Modifier.height(24.dp))

            if (state.isEditing) {
                Text(
                    text = "Tap your photo to change it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                EditProfileForm(
                    draft = state.draft,
                    collegeName = state.collegeName,
                    departmentName = state.departmentName,
                    errors = state.errors,
                    isSaving = state.isSaving,
                    onDraftChange = profileViewModel::updateDraft,
                    onSave = profileViewModel::saveProfile,
                    onCancel = profileViewModel::cancelEditing
                )
            } else {
                ProfileDetailsCard(profile, state.collegeName, state.departmentName)

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = profileViewModel::startEditing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }

                Spacer(Modifier.height(24.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column {
                        if (profile.userRole.canApprove) {
                            SettingsRow(
                                icon = Icons.Default.CheckCircle,
                                label = "Pending Approvals",
                                onClick = { rootNavController.navigate(NavRoutes.Approvals) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                        if (profile.userRole == UserRole.SUPER_ADMIN || profile.userRole == UserRole.ADMIN) {
                            SettingsRow(
                                icon = Icons.Default.Home,
                                label = if (profile.userRole == UserRole.SUPER_ADMIN) "Manage Colleges" else "Manage My College",
                                onClick = { rootNavController.navigate(NavRoutes.ManageColleges) }
                            )
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        }
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            label = "Notification Preferences",
                            onClick = { /* TODO: navigate to notification settings */ }
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

                        // Night / day mode toggle — switches CampusConnectTheme app-wide.
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.width(16.dp))
                            Text(
                                text = "Dark Mode",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = darkTheme,
                                onCheckedChange = { onToggleTheme() }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Log Out")
                }
            }
        }
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Log out?") },
            text = { Text("You'll need to sign in again to use CampusConnect.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()

                        rootNavController.navigate(NavRoutes.Login) {
                            popUpTo(0) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("Log Out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ProfileHeader(
    name: String,
    subtitle: String,
    photo: Bitmap?,
    editable: Boolean,
    onAvatarClick: () -> Unit
) {
    val image = remember(photo) { photo?.asImageBitmap() }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(88.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .then(
                        if (editable) {
                            Modifier.clickable(
                                onClickLabel = "Change profile picture",
                                onClick = onAvatarClick
                            )
                        } else {
                            Modifier
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (image != null) {
                    Image(
                        bitmap = image,
                        contentDescription = "Profile picture",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile picture",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            if (editable) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ProfileDetailsCard(
    profile: UserProfile,
    collegeName: String,
    departmentName: String
) {
    val role = profile.userRole
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            val divider = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            DetailRow(Icons.Default.Person, "Name", profile.name)
            HorizontalDivider(color = divider)
            DetailRow(Icons.Default.Email, "Email", profile.email)
            HorizontalDivider(color = divider)
            DetailRow(Icons.Default.AccountCircle, "Role", role.label)
            HorizontalDivider(color = divider)
            DetailRow(Icons.Default.Home, "College", collegeName)
            if (role.usesDepartment) {
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.Info, "Department", departmentName)
            }
            if (role == UserRole.STUDENT) {
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.AccountBox, "Roll number", profile.rollNumber)
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.DateRange, "Batch", profile.batch)
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.DateRange, "Year / Semester", profile.yearSemesterLabel())
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.Star, "Section", profile.section)
            } else {
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.AccountBox, "Employee ID", profile.employeeId)
                HorizontalDivider(color = divider)
                DetailRow(Icons.Default.Star, "Designation", profile.designation)
            }
            HorizontalDivider(color = divider)
            DetailRow(Icons.Default.Phone, "Phone number", profile.phone)
        }
    }
}

@Composable
private fun DetailRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value.ifBlank { "Not set" },
                style = MaterialTheme.typography.bodyLarge,
                color = if (value.isBlank()) {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
        }
    }
}

@Composable
private fun EditProfileForm(
    draft: UserProfile,
    collegeName: String,
    departmentName: String,
    errors: Map<EditField, String>,
    isSaving: Boolean,
    onDraftChange: ((UserProfile) -> UserProfile) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        ProfileTextField(
            value = draft.name,
            onValueChange = { v -> onDraftChange { it.copy(name = v.take(60)) } },
            label = "Full name",
            icon = Icons.Default.Person,
            error = errors[EditField.Name],
            capitalization = KeyboardCapitalization.Words
        )
        ProfileTextField(
            value = draft.email,
            onValueChange = {},
            label = "Email",
            icon = Icons.Default.Email,
            enabled = false,
            supportingText = "Email comes from your account and can't be changed here"
        )
        ProfileTextField(
            value = collegeName,
            onValueChange = {},
            label = "College",
            icon = Icons.Default.Home,
            enabled = false
        )
        if (draft.userRole.usesDepartment) {
            ProfileTextField(
                value = departmentName,
                onValueChange = {},
                label = "Department",
                icon = Icons.Default.Info,
                enabled = false,
                supportingText = "College and department were set during registration"
            )
        }
        if (draft.userRole != UserRole.STUDENT) {
            ProfileTextField(
                value = draft.employeeId,
                onValueChange = { v ->
                    val cleaned = v.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '/' }.take(20)
                    onDraftChange { it.copy(employeeId = cleaned) }
                },
                label = "Employee ID",
                icon = Icons.Default.AccountBox,
                error = errors[EditField.EmployeeId],
                capitalization = KeyboardCapitalization.Characters
            )
            ProfileTextField(
                value = draft.designation,
                onValueChange = { v -> onDraftChange { it.copy(designation = v.take(40)) } },
                label = "Designation",
                icon = Icons.Default.Star,
                error = errors[EditField.Designation],
                capitalization = KeyboardCapitalization.Words
            )
        }
        if (draft.userRole == UserRole.STUDENT) {
            ProfileTextField(
                value = draft.rollNumber,
                onValueChange = { v ->
                    val cleaned = v.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '/' }.take(20)
                    onDraftChange { it.copy(rollNumber = cleaned) }
                },
                label = "Roll number",
                icon = Icons.Default.AccountBox,
                error = errors[EditField.RollNumber],
                capitalization = KeyboardCapitalization.Characters
            )
            ProfileTextField(
                value = draft.batch,
                onValueChange = { v ->
                    val cleaned = v.filter { c -> c.isDigit() || c == '-' }.take(9)
                    onDraftChange { it.copy(batch = cleaned) }
                },
                label = "Batch (e.g. 2023-2027)",
                icon = Icons.Default.DateRange,
                error = errors[EditField.Batch],
                keyboardType = KeyboardType.Number
            )

            Text(
                text = "Year",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            NumberChipRow(
                options = ProfileOptions.years,
                selected = draft.year,
                onSelect = { n -> onDraftChange { it.copy(year = n) } }
            )

            Text(
                text = "Semester",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            ProfileOptions.semestersFor(draft.year).chunked(4).forEachIndexed { index, row ->
                if (index > 0) Spacer(Modifier.height(4.dp))
                NumberChipRow(
                    options = row,
                    selected = draft.semester,
                    onSelect = { n -> onDraftChange { it.copy(semester = n) } }
                )
            }

            Text(
                text = "Section",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileOptions.sections.forEach { section ->
                    FilterChip(
                        selected = section == draft.section,
                        onClick = {
                            onDraftChange { it.copy(section = if (section == draft.section) "" else section) }
                        },
                        label = {
                            Text(
                                text = section,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

        }

        Spacer(Modifier.height(8.dp))

        ProfileTextField(
            value = draft.phone,
            onValueChange = { v ->
                val cleaned = v.filter { c -> c.isDigit() || c == '+' }.take(14)
                onDraftChange { it.copy(phone = cleaned) }
            },
            label = "Phone number (optional)",
            icon = Icons.Default.Phone,
            error = errors[EditField.Phone],
            keyboardType = KeyboardType.Phone,
            imeAction = ImeAction.Done,
            onDone = { focusManager.clearFocus() }
        )

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    focusManager.clearFocus()
                    onSave()
                },
                enabled = !isSaving,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Save Profile")
                }
            }
        }
    }
}

@Composable
private fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    error: String? = null,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onDone: () -> Unit = {}
) {
    val message = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        isError = error != null,
        supportingText = if (message != null) {
            { Text(message) }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(
            capitalization = capitalization,
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun NumberChipRow(
    options: List<Int>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { n ->
            FilterChip(
                selected = n == selected,
                onClick = { onSelect(if (n == selected) 0 else n) },
                label = {
                    Text(
                        text = "$n",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = tint,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}