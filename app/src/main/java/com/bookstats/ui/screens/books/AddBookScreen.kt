package com.bookstats.ui.screens.books

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.bookstats.data.remote.BookCoverResult
import kotlinx.coroutines.launch

/**
 * Screen for adding a new book with metadata auto-fill from Google Books.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBookScreen(
    onNavigateBack: () -> Unit,
    onBookAdded: (Long) -> Unit,
    viewModel: AddBookViewModel = hiltViewModel()
) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var totalPages by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var coverImageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showCoverSearch by remember { mutableStateOf(false) }
    
    val coverSearchResults by viewModel.coverSearchResults.collectAsStateWithLifecycle()
    val isSearchingCovers by viewModel.isSearchingCovers.collectAsStateWithLifecycle()
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    // Handle cover selection - auto-fill fields
    fun onCoverSelected(cover: BookCoverResult) {
        // Use thumbnailUrl directly - it's what we displayed in search results, so we KNOW it works
        // The high-res URLs with zoom=2 modifications often fail with placeholder images
        coverImageUrl = cover.thumbnailUrl ?: cover.coverUrl
        
        // Auto-fill title with the full book title from Google Books
        title = cover.title
        
        // Auto-fill author if empty
        if (author.isBlank() && cover.authorsFormatted.isNotBlank()) {
            author = cover.authorsFormatted
        }
        
        // Auto-fill category if empty
        if (category.isBlank() && cover.primaryCategory.isNotBlank()) {
            category = cover.primaryCategory
        }
        
        // Auto-fill page count if empty and available
        if (totalPages.isBlank() && cover.pageCount != null && cover.pageCount > 0) {
            totalPages = cover.pageCount.toString()
        }
        
        // Auto-fill notes with description if empty
        if (notes.isBlank() && !cover.description.isNullOrBlank()) {
            // Truncate very long descriptions
            val desc = cover.description
            notes = if (desc.length > 500) {
                desc.take(497) + "..."
            } else {
                desc
            }
        }
        
        showCoverSearch = false
        viewModel.clearCoverSearch()
    }
    
    val errorEnterTitle = stringResource(R.string.error_enter_title)
    val errorValidPageCount = stringResource(R.string.error_enter_valid_page_count)
    val errorPageCountTooHigh = stringResource(R.string.error_page_count_too_high)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_book)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            if (title.isBlank()) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorEnterTitle)
                                }
                                return@IconButton
                            }

                            val pages = totalPages.toIntOrNull()
                            if (pages == null || pages <= 0) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorValidPageCount)
                                }
                                return@IconButton
                            }
                            if (pages > 10000) {
                                scope.launch {
                                    snackbarHostState.showSnackbar(errorPageCountTooHigh)
                                }
                                return@IconButton
                            }
                            
                            isLoading = true
                            scope.launch {
                                val bookId = viewModel.addBook(
                                    title = title,
                                    author = author,
                                    category = category,
                                    totalPages = pages,
                                    notes = notes,
                                    coverImageUrl = coverImageUrl
                                )
                                onBookAdded(bookId)
                            }
                        },
                        enabled = !isLoading && title.isNotBlank() && totalPages.isNotBlank()
                    ) {
                        Icon(Icons.Default.Check, stringResource(R.string.save))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Cover preview and search
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (coverImageUrl != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(coverImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = stringResource(R.string.book_cover),
                            modifier = Modifier
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = { coverImageUrl = null }) {
                            Icon(Icons.Default.Clear, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.remove_cover))
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Book,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedButton(
                        onClick = {
                            showCoverSearch = true
                            if (title.isNotBlank()) {
                                viewModel.searchCovers(title)
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.find_cover_autofill))
                    }
                }
            }
            
            // Cover search results
            if (showCoverSearch) {
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
                                text = stringResource(R.string.select_a_cover),
                                style = MaterialTheme.typography.titleMedium
                            )
                            IconButton(onClick = {
                                showCoverSearch = false
                                viewModel.clearCoverSearch()
                            }) {
                                Icon(Icons.Default.Close, stringResource(R.string.close))
                            }
                        }

                        OutlinedTextField(
                            value = title,
                            onValueChange = {
                                title = it
                                if (it.length >= 3) {
                                    viewModel.searchCovers(it)
                                }
                            },
                            label = { Text(stringResource(R.string.search_by_title)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = {
                                if (isSearchingCovers) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    IconButton(onClick = { viewModel.searchCovers(title) }) {
                                        Icon(Icons.Default.Search, stringResource(R.string.search))
                                    }
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (coverSearchResults.isNotEmpty()) {
                            Text(
                                text = stringResource(R.string.tap_to_select_autofill),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(coverSearchResults) { result ->
                                    CoverSearchResultItem(
                                        result = result,
                                        onSelect = { onCoverSelected(result) }
                                    )
                                }
                            }
                        } else if (!isSearchingCovers && title.length >= 3) {
                            Text(
                                text = stringResource(R.string.no_covers_found),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Title field
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(R.string.book_title)) },
                placeholder = { Text(stringResource(R.string.enter_book_title)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )

            // Author field
            OutlinedTextField(
                value = author,
                onValueChange = { author = it },
                label = { Text(stringResource(R.string.author_optional)) },
                placeholder = { Text(stringResource(R.string.enter_author_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Default.Person, contentDescription = null)
                }
            )

            // Category field
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text(stringResource(R.string.category_optional)) },
                placeholder = { Text(stringResource(R.string.category_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Default.Category, contentDescription = null)
                }
            )

            // Total pages field
            OutlinedTextField(
                value = totalPages,
                onValueChange = { totalPages = it.filter { c -> c.isDigit() } },
                label = { Text(stringResource(R.string.total_pages)) },
                placeholder = { Text(stringResource(R.string.enter_total_pages)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                leadingIcon = {
                    Icon(Icons.Default.Numbers, contentDescription = null)
                }
            )

            // Notes field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.notes_optional)) },
                placeholder = { Text(stringResource(R.string.add_notes_hint)) },
                minLines = 4,
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            )
            
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CoverSearchResultItem(
    result: BookCoverResult,
    onSelect: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelect)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(result.thumbnailUrl ?: result.coverUrl)
                .crossfade(true)
                .build(),
            contentDescription = result.title,
            modifier = Modifier
                .height(120.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(4.dp)),
            contentScale = ContentScale.Fit
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = result.title,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        if (result.authorsFormatted.isNotBlank()) {
            Text(
                text = result.authorsFormatted,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Show what will be auto-filled
        if (result.pageCount != null) {
            Text(
                text = "${result.pageCount} pages",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
