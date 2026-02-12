package com.bookstats.ui.screens.bookdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.data.remote.BookCoverResult
import com.bookstats.data.remote.GoogleBooksApi
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.ReadingSession
import com.bookstats.domain.repository.BookRepository
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the Book Detail screen.
 */
@HiltViewModel
class BookDetailViewModel @Inject constructor(
    private val repository: BookRepository,
    private val googleBooksApi: GoogleBooksApi,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L

    companion object {
        private const val TAG = "BookDetailViewModel"
    }
    
    private val _uiState = MutableStateFlow<BookDetailUiState>(BookDetailUiState.Loading)
    val uiState: StateFlow<BookDetailUiState> = _uiState.asStateFlow()
    
    private val _events = MutableSharedFlow<BookDetailEvent>()
    val events: SharedFlow<BookDetailEvent> = _events.asSharedFlow()
    
    private val _coverSearchResults = MutableStateFlow<List<BookCoverResult>>(emptyList())
    val coverSearchResults: StateFlow<List<BookCoverResult>> = _coverSearchResults.asStateFlow()
    
    private val _isSearchingCovers = MutableStateFlow(false)
    val isSearchingCovers: StateFlow<Boolean> = _isSearchingCovers.asStateFlow()
    
    init {
        if (bookId > 0) {
            observeBook()
        }
    }
    
    private fun observeBook() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _uiState.value = if (book != null) {
                    BookDetailUiState.Success(book)
                } else {
                    BookDetailUiState.Error("Book not found")
                }
            }
        }
    }
    
    /**
     * Search for book covers by title.
     */
    fun searchCovers(title: String) {
        if (title.isBlank()) {
            _coverSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearchingCovers.value = true
            try {
                val results = googleBooksApi.searchByTitle(title, maxResults = 8)
                _coverSearchResults.value = results
                Log.d(TAG, "Cover search for '$title': ${results.size} results")
            } catch (e: Exception) {
                Log.e(TAG, "Cover search failed for '$title'", e)
                _coverSearchResults.value = emptyList()
            } finally {
                _isSearchingCovers.value = false
            }
        }
    }
    
    /**
     * Clear cover search results.
     */
    fun clearCoverSearch() {
        _coverSearchResults.value = emptyList()
    }
    
    /**
     * Get the best working cover URL for a BookCoverResult.
     * Tries high-res first, falls back to thumbnail if unavailable.
     */
    suspend fun getBestCoverUrl(cover: BookCoverResult): String? {
        return googleBooksApi.getBestWorkingUrl(cover)
    }
    
    /**
     * Update book with all metadata fields.
     */
    fun updateBook(
        title: String,
        author: String,
        categoryString: String,
        totalPages: Int,
        notes: String,
        coverImageUrl: String?
    ) {
        viewModelScope.launch {
            val currentState = _uiState.value
            if (currentState is BookDetailUiState.Success) {
                try {
                    // Convert comma-separated category string to Category objects
                    val categoryNames = categoryString.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    val categories = if (categoryNames.isNotEmpty()) {
                        repository.getOrCreateCategories(categoryNames)
                    } else {
                        emptyList()
                    }

                    val updatedBook = currentState.book.copy(
                        title = title,
                        author = author,
                        categories = categories,
                        totalPages = totalPages,
                        notes = notes,
                        coverImageUrl = coverImageUrl
                    )
                    repository.updateBook(updatedBook)
                    _events.emit(BookDetailEvent.BookUpdated)
                } catch (e: Exception) {
                    _events.emit(BookDetailEvent.Error(e.message ?: "Failed to update book"))
                }
            }
        }
    }
    
    fun deleteBook() {
        viewModelScope.launch {
            repository.deleteBook(bookId)
            _events.emit(BookDetailEvent.BookDeleted)
        }
    }
    
    fun addSession(startTime: Instant, endTime: Instant, startPage: Int, endPage: Int) {
        viewModelScope.launch {
            val session = ReadingSession.fromTimer(
                bookId = bookId,
                startTime = startTime,
                endTime = endTime,
                startPage = startPage,
                endPage = endPage
            )
            repository.insertSession(session)
            _events.emit(BookDetailEvent.SessionAdded)
        }
    }
}

/**
 * UI state for the Book Detail screen.
 */
sealed interface BookDetailUiState {
    data object Loading : BookDetailUiState
    data class Success(val book: Book) : BookDetailUiState
    data class Error(val message: String) : BookDetailUiState
}

/**
 * One-time events from the Book Detail screen.
 */
sealed interface BookDetailEvent {
    data object BookUpdated : BookDetailEvent
    data object BookDeleted : BookDetailEvent
    data object SessionAdded : BookDetailEvent
    data class Error(val message: String) : BookDetailEvent
}
