package com.bookstats.ui.screens.sessions

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.domain.model.ReadingSession
import kotlinx.coroutines.flow.collectLatest
import com.bookstats.ui.components.EmptyState
import com.bookstats.ui.components.LoadingIndicator

/**
 * Screen showing all reading sessions for a book, grouped by date.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SessionsViewModel = hiltViewModel()
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val sessions by viewModel.sessions.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var showDeleteDialog by remember { mutableStateOf<Long?>(null) }
    var editingSession by remember { mutableStateOf<ReadingSession?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events (errors, success)
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is SessionsEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
                is SessionsEvent.SessionUpdated -> {
                    editingSession = null
                }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.reading_sessions),
                            style = MaterialTheme.typography.titleLarge
                        )
                        book?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            sessions.isEmpty() -> {
                EmptyState(
                    icon = Icons.Default.Timer,
                    title = stringResource(R.string.no_sessions_yet),
                    subtitle = stringResource(R.string.start_reading_first_session),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Summary card
                    item {
                        book?.let { currentBook ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${currentBook.sessions.size}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.sessions),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = currentBook.totalTimeFormatted,
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.total_time),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "${currentBook.pagesRead}",
                                            style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = stringResource(R.string.pages_read),
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Session groups by date
                    sessions.forEach { group ->
                        item(key = "header_${group.date}") {
                            SessionDateHeader(group = group)
                        }
                        
                        items(
                            items = group.sessions,
                            key = { it.id }
                        ) { session ->
                            SessionItem(
                                session = session,
                                onEdit = { editingSession = session },
                                onDelete = { showDeleteDialog = session.id }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Delete confirmation dialog
    showDeleteDialog?.let { sessionId ->
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            title = { Text(stringResource(R.string.delete_session)) },
            text = { Text(stringResource(R.string.delete_session_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSession(sessionId)
                        showDeleteDialog = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
    
    // Edit session dialog
    editingSession?.let { session ->
        EditSessionDialog(
            session = session,
            maxPages = book?.totalPages ?: 9999,
            onDismiss = { editingSession = null },
            onSave = { startPage, endPage ->
                viewModel.updateSession(session.id, startPage, endPage)
                editingSession = null
            }
        )
    }
}

@Composable
private fun SessionDateHeader(group: SessionGroup) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = group.dateFormatted,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "${group.totalPages} pages · ${group.totalTimeFormatted}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionItem(
    session: ReadingSession,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        text = stringResource(R.string.n_pages, session.pagesRead),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = session.durationFormatted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${session.startTimeFormatted} - ${session.endTimeFormatted}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (session.pagesRead > 0) {
                            stringResource(R.string.pages_format, session.pageRange)
                        } else {
                            stringResource(R.string.no_pages_recorded)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, stringResource(R.string.options))
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        },
                        leadingIcon = {
                            Icon(Icons.Default.Edit, null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun EditSessionDialog(
    session: ReadingSession,
    maxPages: Int,
    onDismiss: () -> Unit,
    onSave: (startPage: Int, endPage: Int) -> Unit
) {
    var startPageText by remember { mutableStateOf(session.startPage.toString()) }
    var endPageText by remember { mutableStateOf(session.endPage.toString()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val errorStartPageNegative = stringResource(R.string.error_start_page_negative)
    val errorEndLessThanStart = stringResource(R.string.error_end_less_than_start)
    val errorEndExceedsMax = stringResource(R.string.error_end_exceeds_max, maxPages)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_session)) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_page_numbers),
                    style = MaterialTheme.typography.bodyMedium
                )

                OutlinedTextField(
                    value = startPageText,
                    onValueChange = {
                        startPageText = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.start_page)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = endPageText,
                    onValueChange = {
                        endPageText = it.filter { c -> c.isDigit() }
                        errorMessage = null
                    },
                    label = { Text(stringResource(R.string.end_page)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val startPage = startPageText.toIntOrNull() ?: 0
                    val endPage = endPageText.toIntOrNull() ?: 0

                    when {
                        startPage < 0 -> errorMessage = errorStartPageNegative
                        endPage < startPage -> errorMessage = errorEndLessThanStart
                        endPage > maxPages -> errorMessage = errorEndExceedsMax
                        else -> onSave(startPage, endPage)
                    }
                }
            ) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
