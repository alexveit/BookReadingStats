package com.bookstats.ui.screens.bookdetail

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bookstats.data.remote.BookCoverResult
import com.bookstats.domain.model.Book
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.components.ProgressBar
import com.bookstats.ui.components.StatItem
import kotlinx.coroutines.launch
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow
import com.bookstats.ui.theme.NotStartedRed
import kotlinx.coroutines.flow.collectLatest

/**
 * Book detail screen showing book info, stats, and sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookDetailScreen(
    onNavigateBack: () -> Unit,
    onNavigateToTimer: (Long, Int) -> Unit,
    onNavigateToSessions: (Long) -> Unit,
    onNavigateToChart: (Long) -> Unit,
    viewModel: BookDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val coverSearchResults by viewModel.coverSearchResults.collectAsStateWithLifecycle()
    val isSearchingCovers by viewModel.isSearchingCovers.collectAsStateWithLifecycle()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is BookDetailEvent.BookDeleted -> onNavigateBack()
                is BookDetailEvent.BookUpdated -> {}
                is BookDetailEvent.SessionAdded -> {}
                is BookDetailEvent.Error -> {
                    // Error is logged; could add snackbar in future
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (uiState as? BookDetailUiState.Success)?.book?.title ?: stringResource(R.string.book),
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, stringResource(R.string.edit))
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, stringResource(R.string.delete))
                    }
                }
            )
        },
        floatingActionButton = {
            val book = (uiState as? BookDetailUiState.Success)?.book
            if (book != null && !book.isFinished) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigateToTimer(book.id, book.pagesRead) },
                    icon = { Icon(Icons.Default.PlayArrow, stringResource(R.string.start_reading)) },
                    text = { Text(stringResource(R.string.start_reading)) }
                )
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is BookDetailUiState.Loading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            is BookDetailUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is BookDetailUiState.Success -> {
                BookDetailContent(
                    book = state.book,
                    onViewAllSessions = { onNavigateToSessions(state.book.id) },
                    onViewChart = { onNavigateToChart(state.book.id) },
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
    
    // Edit dialog
    if (showEditDialog) {
        val book = (uiState as? BookDetailUiState.Success)?.book
        if (book != null) {
            EditBookDialog(
                book = book,
                coverSearchResults = coverSearchResults,
                isSearchingCovers = isSearchingCovers,
                onSearchCovers = viewModel::searchCovers,
                onClearCoverSearch = viewModel::clearCoverSearch,
                onDismiss = { 
                    showEditDialog = false
                    viewModel.clearCoverSearch()
                },
                onSave = { title, author, category, pages, notes, coverUrl ->
                    viewModel.updateBook(title, author, category, pages, notes, coverUrl)
                    showEditDialog = false
                    viewModel.clearCoverSearch()
                }
            )
        }
    }
    
    // Delete book dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_book)) },
            text = { Text(stringResource(R.string.delete_book_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteBook()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun BookDetailContent(
    book: Book,
    onViewAllSessions: () -> Unit,
    onViewChart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when {
        book.isFinished -> FinishedGreen
        book.isStarted -> InProgressYellow
        else -> NotStartedRed
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Book info card with cover
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Cover image
                    if (book.coverImageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(book.coverImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.book_cover),
                            modifier = Modifier
                                .width(80.dp)
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                    
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = book.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        if (book.author.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = book.author,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        if (book.categories.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Category,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = book.categoriesFormatted,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Progress card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.percent_complete, book.percentComplete),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                        if (book.isFinished) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = stringResource(R.string.finished),
                                tint = FinishedGreen,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    ProgressBar(
                        progress = book.percentComplete / 100f,
                        color = statusColor,
                        height = 12
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${book.pagesRead} ${stringResource(R.string.pages_read_count)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "${book.pagesRemaining} ${stringResource(R.string.pages_left)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Statistics card
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.statistics),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = onViewChart) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.view_chart))
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            label = stringResource(R.string.total_time),
                            value = book.totalTimeFormatted,
                            icon = Icons.Default.Timer
                        )
                        StatItem(
                            label = stringResource(R.string.per_page),
                            value = book.timePerPageFormatted,
                            icon = Icons.Default.Speed
                        )
                        StatItem(
                            label = stringResource(R.string.sessions),
                            value = book.sessions.size.toString(),
                            icon = Icons.AutoMirrored.Filled.List
                        )
                    }
                    
                    if (!book.isFinished && book.isStarted) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = stringResource(R.string.estimated_time_left),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = book.estimatedTimeRemainingFormatted,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.pages_per_min),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.3f", book.pagesPerMinute),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    
                    // Dates
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.started),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = book.startDateFormatted,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        if (book.isFinished) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = stringResource(R.string.finished),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = book.finishDateFormatted,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Notes card (if present)
        if (book.notes.isNotBlank()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.notes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = book.notes,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
        
        // Sessions card - tap to view all
        item {
            Card(
                onClick = onViewAllSessions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reading_sessions),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (book.sessions.isEmpty()) {
                            Text(
                                text = stringResource(R.string.no_sessions_yet),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.sessions_count_time, book.sessions.size, book.totalTimeFormatted),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = stringResource(R.string.view_sessions_label),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Bottom spacing for FAB
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun EditBookDialog(
    book: Book,
    coverSearchResults: List<BookCoverResult>,
    isSearchingCovers: Boolean,
    onSearchCovers: (String) -> Unit,
    onClearCoverSearch: () -> Unit,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, String?) -> Unit
) {
    var title by remember { mutableStateOf(book.title) }
    var author by remember { mutableStateOf(book.author) }
    var category by remember { mutableStateOf(book.categoriesFormatted) }
    var totalPages by remember { mutableStateOf(book.totalPages.toString()) }
    var notes by remember { mutableStateOf(book.notes) }
    var coverImageUrl by remember { mutableStateOf(book.coverImageUrl) }
    var showCoverSearch by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    // Handle cover selection - auto-fill fields
    fun onCoverSelected(cover: BookCoverResult) {
        // Use thumbnailUrl directly - it's what we displayed in search results, so we KNOW it works
        coverImageUrl = cover.thumbnailUrl ?: cover.coverUrl
        
        // Auto-fill author if empty
        if (author.isBlank() && cover.authorsFormatted.isNotBlank()) {
            author = cover.authorsFormatted
        }
        
        // Auto-fill category if empty
        if (category.isBlank() && cover.categoriesFormatted.isNotBlank()) {
            category = cover.categoriesFormatted
        }
        
        // Auto-fill page count if empty and available
        if (totalPages.isBlank() && cover.pageCount != null && cover.pageCount > 0) {
            totalPages = cover.pageCount.toString()
        }
        
        showCoverSearch = false
        onClearCoverSearch()
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.edit_book),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                // Cover preview and search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (coverImageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.book_cover),
                            modifier = Modifier
                                .height(80.dp)
                                .width(55.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = {
                                showCoverSearch = !showCoverSearch
                                if (showCoverSearch && title.isNotBlank()) {
                                    onSearchCovers(title)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (coverImageUrl != null) stringResource(R.string.change_cover) else stringResource(R.string.find_cover))
                        }

                        if (coverImageUrl != null) {
                            TextButton(
                                onClick = { coverImageUrl = null },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.remove_cover))
                            }
                        }
                    }
                }
                
                // Cover search results
                if (showCoverSearch) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp)
                        ) {
                            if (isSearchingCovers) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.searching))
                                }
                            } else if (coverSearchResults.isNotEmpty()) {
                                Text(
                                    text = stringResource(R.string.tap_to_select),
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(coverSearchResults) { result ->
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(result.thumbnailUrl ?: result.coverUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = result.title,
                                            modifier = Modifier
                                                .height(80.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .clickable { onCoverSelected(result) }
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outline,
                                                    RoundedCornerShape(4.dp)
                                                ),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                            } else {
                                Text(
                                    text = stringResource(R.string.no_covers_found),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
                
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.title)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = author,
                    onValueChange = { author = it },
                    label = { Text(stringResource(R.string.author)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text(stringResource(R.string.category_genre)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = totalPages,
                    onValueChange = { totalPages = it.filter { c -> c.isDigit() } },
                    label = { Text(stringResource(R.string.total_pages)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val pages = totalPages.toIntOrNull() ?: book.totalPages
                            onSave(title, author, category, pages, notes, coverImageUrl)
                        },
                        enabled = title.isNotBlank() && totalPages.isNotBlank()
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}
