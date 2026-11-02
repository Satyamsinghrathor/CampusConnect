package com.example.campusconnect.screens.noticescreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.campusconnect.data.model.EventAudience
import com.example.campusconnect.data.model.Notice
import com.example.campusconnect.data.model.NoticeCategory
import com.example.campusconnect.data.model.ProfileOptions
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.screens.eventscreen.formatDate
import com.example.campusconnect.screens.eventscreen.toPickerMillis
import com.example.campusconnect.screens.eventscreen.withPickedDate

/** Form for posting or editing a notice. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeEditorSheet(
    initial: Notice?,
    canChooseDepartment: Boolean,
    departmentName: String,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onSave: (NoticeDraft) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember { mutableStateOf(initial?.body.orEmpty()) }
    var category by remember {
        mutableStateOf(NoticeCategory.fromValue(initial?.category ?: NoticeCategory.GENERAL.value))
    }
    var important by remember { mutableStateOf(initial?.important ?: false) }
    var linkUrl by remember { mutableStateOf(initial?.linkUrl.orEmpty()) }
    var expiresAt by remember { mutableLongStateOf(initial?.expiresAtMillis ?: 0L) }
    var wholeCollege by remember { mutableStateOf(initial?.departmentId.isNullOrBlank()) }
    var audience by remember {
        mutableStateOf(
            (initial?.audienceRoles ?: EventAudience.allRoleValues).map { UserRole.fromValue(it) }.toSet()
        )
    }
    var targetYear by remember { mutableStateOf(initial?.targetYear ?: 0) }
    var targetSemester by remember { mutableStateOf(initial?.targetSemester ?: 0) }
    var targetSection by remember { mutableStateOf(initial?.targetSection.orEmpty()) }

    var titleError by remember { mutableStateOf<String?>(null) }
    var bodyError by remember { mutableStateOf<String?>(null) }
    var linkError by remember { mutableStateOf<String?>(null) }
    var expiryError by remember { mutableStateOf<String?>(null) }
    var audienceError by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Local copies so the compiler can see the values aren't null inside the lambdas below.
    val titleMessage = titleError
    val bodyMessage = bodyError
    val linkMessage = linkError

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
                text = if (initial == null) "Post a notice" else "Edit notice",
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
                value = body,
                onValueChange = {
                    body = it.take(2000)
                    bodyError = null
                },
                label = { Text("Notice") },
                minLines = 4,
                maxLines = 10,
                isError = bodyMessage != null,
                supportingText = if (bodyMessage != null) {
                    { Text(bodyMessage) }
                } else {
                    null
                },
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Category")
            ChipGroup(
                options = NoticeCategory.entries,
                isSelected = { it == category },
                label = { it.label },
                onClick = { category = it },
                perRow = 3
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Mark as important", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "Important notices are pinned to the top.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(checked = important, onCheckedChange = { important = it })
            }

            OutlinedTextField(
                value = linkUrl,
                onValueChange = {
                    linkUrl = it.take(MAX_LINK_LENGTH)
                    linkError = null
                },
                label = { Text("Link (optional)") },
                singleLine = true,
                isError = linkMessage != null,
                supportingText = {
                    Text(linkMessage ?: "A form, document or page. Must start with https://")
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.fillMaxWidth()
            )

            SectionLabel("Expires (optional)")
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (expiresAt > 0L) formatDate(expiresAt) else "No expiry")
                }
                if (expiresAt > 0L) {
                    TextButton(onClick = {
                        expiresAt = 0L
                        expiryError = null
                    }) {
                        Text("Clear")
                    }
                }
            }
            expiryError?.let { ErrorText(it) }

            SectionLabel("Who can see this?")
            ChipGroup(
                options = EventAudience.roles,
                isSelected = { it in audience },
                label = { EventAudience.pluralLabel(it) },
                onClick = {
                    audience = if (it in audience) audience - it else audience + it
                    audienceError = null
                },
                perRow = 2
            )
            Text(
                text = "College admins can always see every notice.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            audienceError?.let { ErrorText(it) }

            if (canChooseDepartment) {
                SectionLabel("Which department?")
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

            if (UserRole.STUDENT in audience) {
                SectionLabel("Narrow down students (optional)")

                Text("Year", style = MaterialTheme.typography.bodySmall)
                ChipGroup(
                    options = listOf(0) + ProfileOptions.years,
                    isSelected = { it == targetYear },
                    label = { if (it == 0) "Any" else "$it" },
                    onClick = { year ->
                        targetYear = year
                        if (year != 0 && targetSemester !in ProfileOptions.semestersFor(year)) {
                            targetSemester = 0
                        }
                    },
                    perRow = 5
                )

                Text("Semester", style = MaterialTheme.typography.bodySmall)
                ChipGroup(
                    options = listOf(0) + ProfileOptions.semestersFor(targetYear),
                    isSelected = { it == targetSemester },
                    label = { if (it == 0) "Any" else "$it" },
                    onClick = { targetSemester = it },
                    perRow = 5
                )

                Text("Section", style = MaterialTheme.typography.bodySmall)
                ChipGroup(
                    options = listOf("") + ProfileOptions.sections,
                    isSelected = { it == targetSection },
                    label = { it.ifEmpty { "Any" } },
                    onClick = { targetSection = it },
                    perRow = 4
                )
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = {
                    val trimmedTitle = title.trim()
                    val trimmedBody = body.trim()
                    val trimmedLink = linkUrl.trim()

                    titleError = if (trimmedTitle.length < 3) "Enter a title (at least 3 characters)" else null
                    bodyError = if (trimmedBody.isEmpty()) "Write the notice" else null
                    linkError = if (isValidNoticeLink(trimmedLink)) null else "Use a full link starting with https://"
                    val expiryChanged = initial == null || expiresAt != initial.expiresAtMillis
                    expiryError = if (expiresAt > 0L && expiryChanged && expiresAt < System.currentTimeMillis()) {
                        "Pick a date in the future"
                    } else {
                        null
                    }
                    audienceError = if (audience.isEmpty()) "Choose at least one group" else null

                    if (listOf(titleError, bodyError, linkError, expiryError, audienceError).all { it == null }) {
                        onSave(
                            NoticeDraft(
                                title = trimmedTitle,
                                body = trimmedBody,
                                category = category,
                                important = important,
                                linkUrl = trimmedLink,
                                expiresAtMillis = expiresAt,
                                wholeCollege = wholeCollege,
                                audienceRoles = audience,
                                targetYear = targetYear,
                                targetSemester = targetSemester,
                                targetSection = targetSection
                            )
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
                    Text(if (initial == null) "Post notice" else "Save changes")
                }
            }
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = toPickerMillis(
                if (expiresAt > 0L) expiresAt else System.currentTimeMillis()
            )
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { picked ->
                        expiresAt = endOfDay(withPickedDate(System.currentTimeMillis(), picked))
                    }
                    expiryError = null
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
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ErrorText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error
    )
}

/** Chips laid out [perRow] to a row so they never run off the edge of the screen. */
@Composable
private fun <T> ChipGroup(
    options: List<T>,
    isSelected: (T) -> Boolean,
    label: (T) -> String,
    onClick: (T) -> Unit,
    perRow: Int
) {
    options.chunked(perRow).forEach { rowOptions ->
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            rowOptions.forEach { option ->
                FilterChip(
                    selected = isSelected(option),
                    onClick = { onClick(option) },
                    label = { Text(label(option)) }
                )
            }
        }
    }
}
