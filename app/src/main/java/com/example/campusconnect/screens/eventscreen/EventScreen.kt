package com.example.campusconnect.screens.eventscreen

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusconnect.data.model.CampusEvent
import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.UserRole
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreen(navController: NavHostController) {
    val viewModel: EventsViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editorOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CampusEvent?>(null) }
    var eventToDelete by remember { mutableStateOf<CampusEvent?>(null) }

    // The list is only fetched on demand, so reload whenever this screen is opened again.
    LaunchedEffect(Unit) { viewModel.refresh() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    LaunchedEffect(state.eventSaved) {
        if (state.eventSaved) {
            editorOpen = false
            editing = null
            viewModel.consumeSaved()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Events") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh events")
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
        floatingActionButton = {
            if (state.canPost && !state.isLoading && state.error == null) {
                ExtendedFloatingActionButton(
                    onClick = {
                        editing = null
                        editorOpen = true
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Add event") }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->

        when {
            state.isLoading -> Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.error != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
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

            else -> {
                val shown = if (state.filter == EventFilter.Upcoming) state.upcoming else state.past

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.filter == EventFilter.Upcoming,
                            onClick = { viewModel.setFilter(EventFilter.Upcoming) },
                            label = { Text("Upcoming") }
                        )
                        FilterChip(
                            selected = state.filter == EventFilter.Past,
                            onClick = { viewModel.setFilter(EventFilter.Past) },
                            label = { Text("Past") }
                        )
                    }

                    if (shown.isEmpty()) {
                        EmptyEventsState(
                            past = state.filter == EventFilter.Past,
                            canPost = state.canPost
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            // Extra bottom space so the last card isn't hidden behind the button.
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp)
                        ) {
                            items(shown, key = { it.event.id }) { item ->
                                EventCard(
                                    item = item,
                                    canManage = state.canPost && item.event.createdBy == state.myUid,
                                    onOpenMap = {
                                        if (!MapLinks.open(context, item.event, state.collegeName)) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar("Couldn't open Google Maps")
                                            }
                                        }
                                    },
                                    onEdit = {
                                        editing = item.event
                                        editorOpen = true
                                    },
                                    onDelete = { eventToDelete = item.event }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (editorOpen) {
        EventEditorSheet(
            initial = editing,
            canChooseDepartment = state.myDepartmentId.isNotBlank(),
            departmentName = state.myDepartmentName,
            isSaving = state.isSaving,
            onDismiss = {
                editorOpen = false
                editing = null
            },
            onSave = { draft -> viewModel.saveEvent(editing, draft) }
        )
    }

    eventToDelete?.let { event ->
        AlertDialog(
            onDismissRequest = { eventToDelete = null },
            title = { Text("Delete event?") },
            text = { Text("\"${event.title}\" will be removed for everyone.") },
            confirmButton = {
                TextButton(onClick = {
                    eventToDelete = null
                    viewModel.deleteEvent(event)
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { eventToDelete = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun EventCard(
    item: EventItem,
    canManage: Boolean,
    onOpenMap: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val event = item.event
    val audienceRoles = EventAudience.roles.filter { it.value in event.audienceRoles }
    val who = if (audienceRoles.size == EventAudience.roles.size) {
        "Everyone"
    } else {
        audienceRoles.joinToString(", ") { EventAudience.pluralLabel(it) }
    }
    val audience = "$who · ${item.departmentName.ifBlank { "Whole college" }}"
    val postedBy = listOf(event.createdByName, UserRole.fromValue(event.createdByRole).label)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, top = 12.dp, end = 8.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = event.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = formatDateTime(event.startAtMillis),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (canManage) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit ${event.title}")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete ${event.title}",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            if (MapLinks.hasMap(event)) {
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable(onClickLabel = "Open in Google Maps", onClick = onOpenMap)
                        .padding(vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = event.location.ifBlank { "View on map" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    )
                    Text(
                        text = "  ·  Open in Maps",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (event.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "For: $audience",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (postedBy.isNotBlank()) {
                Text(
                    text = "Posted by $postedBy",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

@Composable
private fun EmptyEventsState(past: Boolean, canPost: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text(
            text = if (past) "No past events" else "No upcoming events yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = when {
                past -> "Events that have already happened will show up here."
                canPost -> "Tap Add event to post the first one."
                else -> "Check back soon — new events will show up here."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventEditorSheet(
    initial: CampusEvent?,
    canChooseDepartment: Boolean,
    departmentName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (EventDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var location by remember { mutableStateOf(initial?.location.orEmpty()) }
    var mapUrl by remember { mutableStateOf(initial?.mapUrl.orEmpty()) }
    var mapUrlError by remember { mutableStateOf<String?>(null) }
    var startMillis by remember { mutableLongStateOf(initial?.startAtMillis ?: defaultStartMillis()) }
    var wholeCollege by remember { mutableStateOf(initial?.departmentId.isNullOrBlank()) }
    var audience by remember {
        mutableStateOf(
            (initial?.audienceRoles ?: EventAudience.allRoleValues).map { UserRole.fromValue(it) }.toSet()
        )
    }
    var audienceError by remember { mutableStateOf<String?>(null) }
    var titleError by remember { mutableStateOf<String?>(null) }
    var dateError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val titleMessage = titleError
    val mapUrlMessage = mapUrlError

    ModalBottomSheet(
        onDismissRequest = { if (!isSaving) onDismiss() },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = if (initial == null) "Add event" else "Edit event",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value = title,
                onValueChange = {
                    title = it.take(100)
                    titleError = null
                },
                label = { Text("Title") },
                singleLine = true,
                isError = titleMessage != null,
                supportingText = if (titleMessage != null) {
                    { Text(titleMessage) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it.take(500) },
                label = { Text("Description (optional)") },
                minLines = 2,
                maxLines = 5,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it.take(100) },
                label = { Text("Location (optional)") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = mapUrl,
                onValueChange = {
                    mapUrl = it.take(MapLinks.MAX_URL_LENGTH)
                    mapUrlError = null
                },
                label = { Text("Google Maps link (optional)") },
                singleLine = true,
                isError = mapUrlMessage != null,
                supportingText = {
                    Text(mapUrlMessage ?: "Share a place from Google Maps and paste the link to point to the exact spot.")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "When",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(formatDate(startMillis))
                }
                OutlinedButton(
                    onClick = { showTimePicker = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(formatTime(startMillis))
                }
            }
            dateError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Text(
                text = "Who can see this?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            EventAudience.roles.chunked(2).forEach { rowRoles ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowRoles.forEach { role ->
                        FilterChip(
                            selected = role in audience,
                            onClick = {
                                audience = if (role in audience) audience - role else audience + role
                                audienceError = null
                            },
                            label = { Text(EventAudience.pluralLabel(role)) }
                        )
                    }
                }
            }
            Text(
                text = "College admins can always see every event.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            audienceError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (canChooseDepartment) {
                Text(
                    text = "Which department?",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = wholeCollege,
                        onClick = { wholeCollege = true },
                        label = { Text("Whole college") }
                    )
                    FilterChip(
                        selected = !wholeCollege,
                        onClick = { wholeCollege = false },
                        label = { Text(departmentName.ifBlank { "My department" }) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val trimmed = title.trim()
                    titleError = if (trimmed.length < 3) "Enter a title (at least 3 characters)" else null
                    val changedTime = initial == null || startMillis != initial.startAtMillis
                    dateError = if (changedTime && startMillis < System.currentTimeMillis()) {
                        "Pick a date and time in the future"
                    } else {
                        null
                    }
                    val link = mapUrl.trim()
                    mapUrlError = if (MapLinks.isValidMapUrl(link)) null else "Use a link from Google Maps"
                    audienceError = if (audience.isEmpty()) "Choose at least one group" else null
                    if (titleError == null && dateError == null && audienceError == null && mapUrlError == null) {
                        onSave(
                            EventDraft(trimmed, description, location, mapUrl, startMillis, wholeCollege, audience)
                        )
                    }
                },
                enabled = !isSaving,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (initial == null) "Add event" else "Save changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = toPickerMillis(startMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { startMillis = withPickedDate(startMillis, it) }
                    dateError = null
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (showTimePicker) {
        val current = Calendar.getInstance().apply { timeInMillis = startMillis }
        val timeState = rememberTimePickerState(
            initialHour = current.get(Calendar.HOUR_OF_DAY),
            initialMinute = current.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    startMillis = withTime(startMillis, timeState.hour, timeState.minute)
                    dateError = null
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timeState) }
        )
    }
}

// ---- Date and time helpers (java.time needs API 26, and this app supports 25) ----

/** Tomorrow at 10:00 in the device's time zone. */
private fun defaultStartMillis(): Long =
    Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

/** The date picker works in UTC, so turn a local date into the matching UTC midnight. */
private fun toPickerMillis(millis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = millis }
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

/** Keeps the time of day of [millis] and swaps in the date the picker returned. */
private fun withPickedDate(millis: Long, pickerMillis: Long): Long {
    val picked = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = pickerMillis }
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(picked.get(Calendar.YEAR), picked.get(Calendar.MONTH), picked.get(Calendar.DAY_OF_MONTH))
    }.timeInMillis
}

private fun withTime(millis: Long, hour: Int, minute: Int): Long =
    Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun formatDate(millis: Long): String =
    SimpleDateFormat("EEE, d MMM yyyy", Locale.getDefault()).format(Date(millis))

private fun formatTime(millis: Long): String =
    SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(millis))

private fun formatDateTime(millis: Long): String = "${formatDate(millis)} · ${formatTime(millis)}"
