package com.example.campusconnect.screens.profilesetupscreen

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
//import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.data.model.ProfileOptions

/**
 * Step 2 of registration: collects college details after the first login
 * and saves them to Firestore at users/{uid}.
 */
@Composable
fun ProfileSetupScreen(
    onProfileSaved: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: ProfileSetupViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onProfileSaved()
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Complete Your Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Tell us a bit about your college so CampusConnect can show you the right timetable, notices and events.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            state.loadError?.let { message ->
                MessageBanner(
                    message = message,
                    actionLabel = "Retry",
                    onAction = viewModel::loadColleges
                )
                Spacer(Modifier.height(8.dp))
            }

            SetupTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Name",
                icon = Icons.Default.Person,
                readOnly = state.isNameLocked,
                error = state.errors[SetupField.Name],
                supportingText = if (state.isNameLocked) "From your Google account" else null,
                capitalization = KeyboardCapitalization.Words
            )

            SetupTextField(
                value = state.email,
                onValueChange = {},
                label = "Email",
                icon = Icons.Default.Email,
                readOnly = true,
                supportingText = "From your account"
            )

            DropdownField(
                label = "College",
                value = state.colleges.firstOrNull { it.id == state.collegeId }?.name.orEmpty(),
                options = state.colleges,
                optionLabel = { it.name },
                onSelected = viewModel::onCollegeSelected,
                icon = Icons.Default.Home,
                enabled = !state.isLoadingColleges,
                placeholder = if (state.isLoadingColleges) "Loading colleges…" else "Select college",
                error = state.errors[SetupField.College],
                emptyMessage = "No colleges available yet"
            )

            DropdownField(
                label = "Department",
                value = state.departments.firstOrNull { it.id == state.departmentId }?.name.orEmpty(),
                options = state.departments,
                optionLabel = { it.name },
                onSelected = viewModel::onDepartmentSelected,
                icon = Icons.Default.Info,
                enabled = state.collegeId.isNotEmpty() && !state.isLoadingDepartments,
                placeholder = when {
                    state.collegeId.isEmpty() -> "Select a college first"
                    state.isLoadingDepartments -> "Loading departments…"
                    else -> "Select department"
                },
                error = state.errors[SetupField.Department],
                emptyMessage = "No departments for this college"
            )

            SetupTextField(
                value = state.rollNumber,
                onValueChange = viewModel::onRollNumberChange,
                label = "Roll number",
                icon = Icons.Default.AccountBox,
                error = state.errors[SetupField.RollNumber],
                capitalization = KeyboardCapitalization.Characters,
                placeholder = "23CSE001"
            )

            SetupTextField(
                value = state.batch,
                onValueChange = viewModel::onBatchChange,
                label = "Batch",
                icon = Icons.Default.DateRange,
                error = state.errors[SetupField.Batch],
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onNext = { focusManager.clearFocus() },
                placeholder = "2023-2027"
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "Year",
                        value = if (state.year > 0) "${state.year}" else "",
                        options = ProfileOptions.years,
                        optionLabel = { "Year $it" },
                        onSelected = viewModel::onYearSelected,
                        icon = null,
                        placeholder = "Select",
                        error = state.errors[SetupField.Year]
                    )
                }
                Box(modifier = Modifier.weight(1f)) {
                    DropdownField(
                        label = "Semester",
                        value = if (state.semester > 0) "${state.semester}" else "",
                        options = ProfileOptions.semestersFor(state.year),
                        optionLabel = { "Semester $it" },
                        onSelected = viewModel::onSemesterSelected,
                        icon = null,
                        placeholder = "Select",
                        error = state.errors[SetupField.Semester]
                    )
                }
            }

            DropdownField(
                label = "Section",
                value = state.section,
                options = ProfileOptions.sections,
                optionLabel = { "Section $it" },
                onSelected = viewModel::onSectionSelected,
                icon = Icons.Default.Star,
                placeholder = "Select section",
                error = state.errors[SetupField.Section]
            )

            state.saveError?.let { message ->
                Spacer(Modifier.height(4.dp))
                MessageBanner(message = message)
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.saveProfile()
                },
                enabled = !state.isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }

            TextButton(
                onClick = {
                    viewModel.signOut()
                    onSignOut()
                },
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Use a different account")
            }
        }
    }
}

@Composable
private fun SetupTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    readOnly: Boolean = false,
    error: String? = null,
    supportingText: String? = null,
    placeholder: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.None,
    imeAction: ImeAction = ImeAction.Next,
    onNext: (() -> Unit)? = null
) {
    val focusManager = LocalFocusManager.current
    val message = error ?: supportingText
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        singleLine = true,
        label = { Text(label) },
        placeholder = if (placeholder != null) {
            { Text(placeholder) }
        } else {
            null
        },
        leadingIcon = { Icon(icon, contentDescription = null) },
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
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onNext = { focusManager.moveFocus(FocusDirection.Down) },
            onDone = { onNext?.invoke() }
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

/** A read-only field that opens a menu of [options] when tapped. */
@Composable
private fun <T> DropdownField(
    label: String,
    value: String,
    options: List<T>,
    optionLabel: (T) -> String,
    onSelected: (T) -> Unit,
    icon: ImageVector?,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    error: String? = null,
    emptyMessage: String = "No options available"
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            singleLine = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            leadingIcon = if (icon != null) {
                { Icon(icon, contentDescription = null) }
            } else {
                null
            },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            isError = error != null,
            supportingText = if (error != null) {
                { Text(error) }
            } else {
                null
            },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        if (enabled) {
            // Taps on a read-only text field aren't delivered to it, so overlay a click target.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(
                        onClickLabel = "Choose $label",
                        role = Role.DropdownList
                    ) { expanded = true }
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(emptyMessage) },
                    onClick = { expanded = false },
                    enabled = false
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            expanded = false
                            onSelected(option)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBanner(
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.weight(1f)
            )
            if (actionLabel != null && onAction != null) {
                TextButton(onClick = onAction) { Text(actionLabel) }
            }
        }
    }
}
