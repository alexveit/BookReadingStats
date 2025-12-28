package com.bookstats.ui.screens.books

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.bookstats.R
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.data.sync.SyncState
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.ReadingStatus
import com.bookstats.ui.components.EmptyState
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow
import com.bookstats.ui.theme.NotStartedRed

/**
 * Main books list screen.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BooksScreen(
    onNavigateToBook: (Long) -> Unit,
    onNavigateToAddBook: () -> Unit,
    onNavigateToStatistics: () -> Unit,
    onNavigateToTimer: ((Long, Int) -> Unit)? = null,
    onNavigateToSessions: ((Long) -> Unit)? = null,
    viewModel: BooksViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedBooks by viewModel.selectedBooks.collectAsStateWithLifecycle()
    val isSelectionMode by viewModel.isSelectionMode.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    
    var showSearchBar by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showMergeDialog by remember { mutableStateOf(false) }
    
    // Single book context menu state
    var contextMenuBook by remember { mutableStateOf<Book?>(null) }
    var showSingleDeleteDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                SelectionTopBar(
                    selectedCount = selectedBooks.size,
                    onClearSelection = { viewModel.clearSelection() },
                    onDeleteSelected = { showDeleteDialog = true },
                    onMergeSelected = { showMergeDialog = true }
                )
            } else {
                TopAppBar(
                    title = { Text(stringResource(R.string.my_books)) },
                    actions = {
                        // Sync status indicator
                        SyncStatusIcon(
                            syncState = syncState,
                            onRetry = { viewModel.refresh() }
                        )

                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                imageVector = if (showSearchBar) Icons.Default.Close else Icons.Default.Search,
                                contentDescription = stringResource(R.string.search)
                            )
                        }
                        IconButton(onClick = onNavigateToStatistics) {
                            Icon(
                                imageVector = Icons.Default.BarChart,
                                contentDescription = stringResource(R.string.statistics)
                            )
                        }
                    }
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                ExtendedFloatingActionButton(
                    onClick = onNavigateToAddBook,
                    icon = { Icon(Icons.Default.Add, stringResource(R.string.add_book)) },
                    text = { Text(stringResource(R.string.add_book)) }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            AnimatedVisibility(
                visible = showSearchBar,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = viewModel::onSearchQueryChange,
                    onClear = viewModel::clearSearch,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            
            // Content
            when (val state = uiState) {
                is BooksUiState.Loading -> {
                    LoadingIndicator()
                }
                is BooksUiState.Empty -> {
                    EmptyState(
                        icon = Icons.Outlined.Book,
                        title = stringResource(R.string.no_books_yet),
                        subtitle = stringResource(R.string.add_first_book),
                        modifier = Modifier.fillMaxSize()
                    )
                }
                is BooksUiState.Success -> {
                    if (state.books.isEmpty() && searchQuery.isNotBlank()) {
                        EmptyState(
                            icon = Icons.Default.SearchOff,
                            title = stringResource(R.string.no_results),
                            subtitle = stringResource(R.string.try_different_search),
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(
                                items = state.books,
                                key = { it.id }
                            ) { book ->
                                val isSelected = book.id in selectedBooks
                                
                                BookCardWithContextMenu(
                                    book = book,
                                    isSelected = isSelected,
                                    isSelectionMode = isSelectionMode,
                                    showContextMenu = contextMenuBook?.id == book.id,
                                    onDismissContextMenu = { contextMenuBook = null },
                                    onClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleBookSelection(book.id)
                                        } else {
                                            onNavigateToBook(book.id)
                                        }
                                    },
                                    onLongClick = {
                                        if (isSelectionMode) {
                                            viewModel.toggleBookSelection(book.id)
                                        } else {
                                            contextMenuBook = book
                                        }
                                    },
                                    onStartReading = {
                                        contextMenuBook = null
                                        onNavigateToTimer?.invoke(book.id, book.currentPage)
                                            ?: onNavigateToBook(book.id)
                                    },
                                    onViewSessions = {
                                        contextMenuBook = null
                                        onNavigateToSessions?.invoke(book.id)
                                            ?: onNavigateToBook(book.id)
                                    },
                                    onEdit = {
                                        contextMenuBook = null
                                        onNavigateToBook(book.id)
                                    },
                                    onDelete = {
                                        contextMenuBook = null
                                        showSingleDeleteDialog = true
                                    },
                                    onSelect = {
                                        contextMenuBook = null
                                        viewModel.toggleBookSelection(book.id)
                                    },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                            
                            // Bottom spacing for FAB
                            item {
                                Spacer(modifier = Modifier.height(80.dp))
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Single book delete confirmation
    if (showSingleDeleteDialog && contextMenuBook != null) {
        AlertDialog(
            onDismissRequest = { 
                showSingleDeleteDialog = false
                contextMenuBook = null
            },
            title = { Text("Delete Book") },
            text = { Text("Are you sure you want to delete \"${contextMenuBook?.title}\"? This will also delete all reading sessions for this book.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        contextMenuBook?.let { viewModel.deleteBook(it.id) }
                        showSingleDeleteDialog = false
                        contextMenuBook = null
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showSingleDeleteDialog = false
                    contextMenuBook = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Bulk delete confirmation dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Books") },
            text = { Text("Are you sure you want to delete ${selectedBooks.size} book(s)? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSelectedBooks()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    // Merge confirmation dialog
    if (showMergeDialog) {
        AlertDialog(
            onDismissRequest = { showMergeDialog = false },
            title = { Text("Merge Books") },
            text = { Text("This will combine all selected books into one. Sessions from all books will be preserved. Continue?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.mergeSelectedBooks()
                        showMergeDialog = false
                    }
                ) {
                    Text("Merge")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMergeDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * Sync status icon with retry capability.
 */
@Composable
private fun SyncStatusIcon(
    syncState: SyncState,
    onRetry: () -> Unit
) {
    when (syncState) {
        is SyncState.Syncing -> {
            CircularProgressIndicator(
                modifier = Modifier
                    .padding(12.dp)
                    .size(24.dp),
                strokeWidth = 2.dp
            )
        }
        is SyncState.Error -> {
            IconButton(onClick = onRetry) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = stringResource(R.string.sync_failed_tap_retry),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
        is SyncState.Offline -> {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = stringResource(R.string.offline),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(12.dp)
            )
        }
        // Idle and Success - show nothing (clean UI)
        else -> {}
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionTopBar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onMergeSelected: () -> Unit
) {
    TopAppBar(
        title = { Text(stringResource(R.string.selected_count, selectedCount)) },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, stringResource(R.string.clear_selection))
            }
        },
        actions = {
            if (selectedCount >= 2) {
                IconButton(onClick = onMergeSelected) {
                    Icon(Icons.AutoMirrored.Filled.MergeType, stringResource(R.string.merge_books))
                }
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Default.Delete, stringResource(R.string.delete_selected_books))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCardWithContextMenu(
    book: Book,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    showContextMenu: Boolean,
    onDismissContextMenu: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onStartReading: () -> Unit,
    onViewSessions: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (book.status) {
        ReadingStatus.FINISHED -> FinishedGreen
        ReadingStatus.IN_PROGRESS -> InProgressYellow
        ReadingStatus.NOT_STARTED -> NotStartedRed
    }
    
    val statusIcon = when (book.status) {
        ReadingStatus.FINISHED -> Icons.Filled.CheckCircle
        ReadingStatus.IN_PROGRESS -> Icons.AutoMirrored.Filled.MenuBook
        ReadingStatus.NOT_STARTED -> Icons.Filled.Book
    }
    
    Box(modifier = modifier) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Row(
                modifier = Modifier.padding(if (isSelectionMode) 8.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                
                // Cover image
                Box(
                    modifier = Modifier
                        .padding(start = if (isSelectionMode) 0.dp else 12.dp, top = 12.dp, bottom = 12.dp)
                        .size(50.dp, 75.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    if (book.coverImageUrl != null) {
                        AsyncImage(
                            model = book.coverImageUrl,
                            contentDescription = stringResource(R.string.cover_of_book, book.title),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = stringResource(R.string.no_cover_image),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                // Inline book card content (not using BookCard to avoid nested clickable)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = book.status.name,
                            tint = statusColor,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress bar
                    LinearProgressIndicator(
                        progress = { book.percentComplete / 100f },
                        modifier = Modifier.fillMaxWidth(),
                        color = statusColor,
                        trackColor = statusColor.copy(alpha = 0.2f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${book.currentPage}/${book.totalPages} pages",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${book.percentComplete}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor
                        )
                    }
                    
                    if (book.totalMinutesRead > 0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Read: ${book.totalTimeFormatted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Context menu dropdown
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = onDismissContextMenu
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.start_reading)) },
                onClick = onStartReading,
                leadingIcon = { Icon(Icons.Default.PlayArrow, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.view_sessions)) },
                onClick = onViewSessions,
                leadingIcon = { Icon(Icons.Default.History, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = onEdit,
                leadingIcon = { Icon(Icons.Default.Edit, null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.select)) },
                onClick = onSelect,
                leadingIcon = { Icon(Icons.Default.CheckBox, null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) },
                onClick = onDelete,
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

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(stringResource(R.string.search_books)) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Clear, stringResource(R.string.clear))
                }
            }
        },
        singleLine = true
    )
}
