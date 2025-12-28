package com.bookstats.ui.screens.books

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.data.sync.SyncState
import com.bookstats.domain.model.Book
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Books list screen.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class BooksViewModel @Inject constructor(
    private val repository: BookRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Persisted across process death
    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow(KEY_SEARCH_QUERY, "")

    private val _uiState = MutableStateFlow<BooksUiState>(BooksUiState.Loading)
    val uiState: StateFlow<BooksUiState> = _uiState.asStateFlow()

    // Selection state - persisted as List<Long> since Set isn't directly supported
    private val _selectedBooks = MutableStateFlow(
        savedStateHandle.get<List<Long>>(KEY_SELECTED_BOOKS)?.toSet() ?: emptySet()
    )
    val selectedBooks: StateFlow<Set<Long>> = _selectedBooks.asStateFlow()
    
    val isSelectionMode: StateFlow<Boolean> = _selectedBooks.map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)
    
    // Expose sync state for UI
    val syncState: StateFlow<SyncState> = repository.syncState
    
    init {
        observeBooks()
    }
    
    private fun observeBooks() {
        viewModelScope.launch {
            combine(
                searchQuery.debounce(SEARCH_DEBOUNCE_MS),
                repository.getAllBooks()
            ) { query, books ->
                if (query.isBlank()) {
                    books
                } else {
                    books.filter { it.title.contains(query, ignoreCase = true) }
                }
            }.collect { filteredBooks ->
                _uiState.value = if (filteredBooks.isEmpty() && searchQuery.value.isBlank()) {
                    BooksUiState.Empty
                } else {
                    BooksUiState.Success(filteredBooks)
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        savedStateHandle[KEY_SEARCH_QUERY] = query
    }

    fun clearSearch() {
        savedStateHandle[KEY_SEARCH_QUERY] = ""
    }

    fun toggleBookSelection(bookId: Long) {
        _selectedBooks.update { selected ->
            val newSelection = if (bookId in selected) {
                selected - bookId
            } else {
                selected + bookId
            }
            savedStateHandle[KEY_SELECTED_BOOKS] = newSelection.toList()
            newSelection
        }
    }

    fun clearSelection() {
        _selectedBooks.value = emptySet()
        savedStateHandle[KEY_SELECTED_BOOKS] = emptyList<Long>()
    }

    companion object {
        private const val KEY_SEARCH_QUERY = "searchQuery"
        private const val KEY_SELECTED_BOOKS = "selectedBooks"
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
    
    fun deleteSelectedBooks() {
        viewModelScope.launch {
            _selectedBooks.value.forEach { bookId ->
                repository.deleteBook(bookId)
            }
            clearSelection()
        }
    }
    
    fun mergeSelectedBooks() {
        viewModelScope.launch {
            val selected = _selectedBooks.value.toList()
            if (selected.size >= 2) {
                val targetId = selected.first()
                val sourceIds = selected.drop(1)
                repository.mergeBooks(targetId, sourceIds)
            }
            clearSelection()
        }
    }
    
    fun deleteBook(bookId: Long) {
        viewModelScope.launch {
            repository.deleteBook(bookId)
        }
    }
    
    /** Manual refresh/retry sync */
    fun refresh() {
        viewModelScope.launch {
            repository.refreshSync()
        }
    }
}

/**
 * UI state for the Books screen.
 */
sealed interface BooksUiState {
    data object Loading : BooksUiState
    data object Empty : BooksUiState
    data class Success(val books: List<Book>) : BooksUiState
}
