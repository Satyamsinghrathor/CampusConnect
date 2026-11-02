package com.example.campusconnect.screens.noticescreen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.campusconnect.data.model.Notice
import com.example.campusconnect.data.model.NoticeCategory
import com.example.campusconnect.data.model.UserRole
import com.example.campusconnect.screens.eventscreen.formatFullDate
import com.example.campusconnect.screens.eventscreen.formatTime
import kotlinx.coroutines.launch

/** Everything about one notice, opened by tapping a notice card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoticeDetailScreen(
    navController: NavHostController,
    noticeId: String
) {
    val viewModel: NoticeDetailViewModel = viewModel()
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(noticeId) { viewModel.load(noticeId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notice") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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

        val notice = state.notice

        when {
            state.isLoading -> CenteredContent(innerPadding) { CircularProgressIndicator() }

            state.notFound -> CenteredContent(innerPadding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "This notice is no longer available.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { navController.popBackStack() }) { Text("Go back") }
                }
            }

            notice == null -> CenteredContent(innerPadding) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = state.error ?: "Couldn't load this notice.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.load(noticeId, force = true) }) { Text("Retry") }
                }
            }

            else -> NoticeDetailContent(
                notice = notice,
                departmentName = state.departmentName,
                innerPadding = innerPadding,
                onOpenLink = {
                    if (!openLink(context, notice.linkUrl)) {
                        scope.launch { snackbarHostState.showSnackbar("Couldn't open the link") }
                    }
                }
            )
        }
    }
}

@Composable
private fun CenteredContent(innerPadding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
private fun NoticeDetailContent(
    notice: Notice,
    departmentName: String,
    innerPadding: PaddingValues,
    onOpenLink: () -> Unit
) {
    val postedBy = listOf(notice.createdByName, UserRole.fromValue(notice.createdByRole).label)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    val now = System.currentTimeMillis()
    val expired = notice.expiresAtMillis > 0L && notice.expiresAtMillis < now
    val divider = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = NoticeCategory.fromValue(notice.category).label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            if (notice.important) {
                Text(
                    text = "IMPORTANT",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = notice.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(20.dp))

        Text(
            text = notice.body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (notice.linkUrl.isNotBlank()) {
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onOpenLink,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open link")
            }
        }

        Spacer(Modifier.height(24.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                if (postedBy.isNotBlank()) {
                    InfoRow(Icons.Default.Person, "Posted by", postedBy)
                    HorizontalDivider(color = divider)
                }
                if (notice.createdAtMillis > 0L) {
                    InfoRow(
                        icon = Icons.Default.CalendarMonth,
                        label = if (notice.isEdited) "Posted (edited)" else "Posted",
                        value = "${formatFullDate(notice.createdAtMillis)} · ${formatTime(notice.createdAtMillis)}"
                    )
                    HorizontalDivider(color = divider)
                }
                if (notice.expiresAtMillis > 0L) {
                    InfoRow(
                        icon = Icons.Default.Info,
                        label = if (expired) "Expired on" else "Expires on",
                        value = formatFullDate(notice.expiresAtMillis)
                    )
                    HorizontalDivider(color = divider)
                }
                InfoRow(
                    icon = Icons.Default.Groups,
                    label = "Who can see this",
                    value = noticeAudienceSummary(notice, departmentName)
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
