package com.example.campusconnect.screens.collegescreen

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.campusconnect.data.model.College
import com.example.campusconnect.data.model.Department

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCollegesScreen(
    onBack: () -> Unit,
    viewModel: ManageCollegesViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddCollege by remember { mutableStateOf(false) }
    var addDepartmentFor by remember { mutableStateOf<College?>(null) }
    var collegeToEdit by remember { mutableStateOf<College?>(null) }
    var departmentToEdit by remember { mutableStateOf<Department?>(null) }
    var collegeToDelete by remember { mutableStateOf<College?>(null) }
    var departmentToDelete by remember { mutableStateOf<Department?>(null) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isSuperAdmin) "Manage Colleges" else "Manage College") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isSuperAdmin && state.hasAccess && state.error == null && !state.isLoading) {
                        IconButton(onClick = { showAddCollege = true }) {
                            Icon(Icons.Default.Add, contentDescription = "Add college")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when {
            state.isLoading -> CenteredBox(innerPadding) { CircularProgressIndicator() }

            !state.hasAccess -> CenteredBox(innerPadding) {
                Text(
                    text = "Only college admins and the super admin can manage colleges.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }

            state.error != null -> CenteredBox(innerPadding) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = state.error ?: "",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = viewModel::load) { Text("Retry") }
                }
            }

            else -> Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (state.isWorking) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (state.items.isEmpty()) {
                    CenteredBox(PaddingValues(0.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (state.isSuperAdmin) "No colleges yet" else "Your college isn't set up yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                            if (state.isSuperAdmin) {
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = { showAddCollege = true }) { Text("Add college") }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.items, key = { it.college.id }) { item ->
                            CollegeCard(
                                item = item,
                                enabled = !state.isWorking,
                                isSuperAdmin = state.isSuperAdmin,
                                onEditCollege = { collegeToEdit = item.college },
                                onAddDepartment = { addDepartmentFor = item.college },
                                onDeleteCollege = { collegeToDelete = item.college },
                                onEditDepartment = { departmentToEdit = it },
                                onDeleteDepartment = { departmentToDelete = it }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddCollege) {
        CodeNameDialog(
            title = "Add college",
            subtitle = null,
            codeLabel = "College code",
            nameLabel = "College name",
            initialCode = "",
            initialName = "",
            initialShortName = "",
            codeEditable = true,
            confirmLabel = "Add",
            onDismiss = { showAddCollege = false },
            onConfirm = { code, name, shortName ->
                showAddCollege = false
                viewModel.addCollege(code, name, shortName)
            }
        )
    }

    collegeToEdit?.let { college ->
        CodeNameDialog(
            title = "Edit college",
            subtitle = if (state.isSuperAdmin) null else "Only the super admin can change the college code.",
            codeLabel = "College code",
            nameLabel = "College name",
            initialCode = college.code,
            initialName = college.name,
            initialShortName = college.shortName,
            codeEditable = state.isSuperAdmin,
            confirmLabel = "Save",
            onDismiss = { collegeToEdit = null },
            onConfirm = { code, name, shortName ->
                collegeToEdit = null
                viewModel.updateCollege(college, code, name, shortName)
            }
        )
    }

    addDepartmentFor?.let { college ->
        CodeNameDialog(
            title = "Add department",
            subtitle = college.name,
            codeLabel = "Department code",
            nameLabel = "Department name",
            initialCode = "",
            initialName = "",
            initialShortName = "",
            codeEditable = true,
            confirmLabel = "Add",
            onDismiss = { addDepartmentFor = null },
            onConfirm = { code, name, shortName ->
                addDepartmentFor = null
                viewModel.addDepartment(college, code, name, shortName)
            }
        )
    }

    departmentToEdit?.let { department ->
        CodeNameDialog(
            title = "Edit department",
            subtitle = if (state.isSuperAdmin) null else "Only the super admin can change the department code.",
            codeLabel = "Department code",
            nameLabel = "Department name",
            initialCode = department.code,
            initialName = department.name,
            initialShortName = department.shortName,
            codeEditable = state.isSuperAdmin,
            confirmLabel = "Save",
            onDismiss = { departmentToEdit = null },
            onConfirm = { code, name, shortName ->
                departmentToEdit = null
                viewModel.updateDepartment(department, code, name, shortName)
            }
        )
    }

    collegeToDelete?.let { college ->
        AlertDialog(
            onDismissRequest = { collegeToDelete = null },
            title = { Text("Remove ${college.name}?") },
            text = {
                Text(
                    "This also removes all of its departments. It can't be removed while " +
                            "anyone is registered under it."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    collegeToDelete = null
                    viewModel.deleteCollege(college)
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { collegeToDelete = null }) { Text("Cancel") }
            }
        )
    }

    departmentToDelete?.let { department ->
        AlertDialog(
            onDismissRequest = { departmentToDelete = null },
            title = { Text("Remove ${department.name}?") },
            text = { Text("It can't be removed while anyone is registered under it.") },
            confirmButton = {
                TextButton(onClick = {
                    departmentToDelete = null
                    viewModel.deleteDepartment(department)
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { departmentToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun CenteredBox(innerPadding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun CollegeCard(
    item: CollegeWithDepartments,
    enabled: Boolean,
    isSuperAdmin: Boolean,
    onEditCollege: () -> Unit,
    onAddDepartment: () -> Unit,
    onDeleteCollege: () -> Unit,
    onEditDepartment: (Department) -> Unit,
    onDeleteDepartment: (Department) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.college.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = listOf(
                            item.college.code.ifBlank { "Code not set" },
                            item.college.shortName
                        ).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onEditCollege, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit ${item.college.name}"
                    )
                }
                if (isSuperAdmin) {
                    IconButton(onClick = onDeleteCollege, enabled = enabled) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove ${item.college.name}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                modifier = Modifier.padding(end = 8.dp)
            )

            if (item.departments.isEmpty()) {
                Text(
                    text = "No departments yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                item.departments.forEach { department ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = department.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = listOf(
                                    department.code.ifBlank { "Code not set" },
                                    department.shortName
                                ).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { onEditDepartment(department) }, enabled = enabled) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit ${department.name}"
                            )
                        }
                        if (isSuperAdmin) {
                            IconButton(onClick = { onDeleteDepartment(department) }, enabled = enabled) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove ${department.name}"
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = onAddDepartment, enabled = enabled) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Add department")
            }
        }
    }
}

/**
 * Shared form for colleges and departments: a code, a name and an optional short name.
 * When [codeEditable] is false the code is shown but locked.
 */
@Composable
private fun CodeNameDialog(
    title: String,
    subtitle: String?,
    codeLabel: String,
    nameLabel: String,
    initialCode: String,
    initialName: String,
    initialShortName: String,
    codeEditable: Boolean,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (code: String, name: String, shortName: String) -> Unit
) {
    var code by remember { mutableStateOf(initialCode) }
    var name by remember { mutableStateOf(initialName) }
    var shortName by remember { mutableStateOf(initialShortName) }

    val codeValid = !codeEditable || code.length in 2..20
    val valid = codeValid && name.trim().length >= 2

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                OutlinedTextField(
                    value = code,
                    onValueChange = { v ->
                        code = v.uppercase().filter { c -> c.isLetterOrDigit() || c == '-' || c == '_' }.take(20)
                    },
                    label = { Text(codeLabel) },
                    singleLine = true,
                    enabled = codeEditable,
                    supportingText = if (codeEditable) {
                        { Text("2–20 letters or digits. Must be unique.") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(80) },
                    label = { Text(nameLabel) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Next
                    )
                )
                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it.take(12) },
                    label = { Text("Short name (optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        imeAction = ImeAction.Done
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(code, name, shortName) }, enabled = valid) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}