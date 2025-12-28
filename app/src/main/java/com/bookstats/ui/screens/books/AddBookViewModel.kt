package com.bookstats.ui.screens.books

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.data.remote.BookCoverResult
import com.bookstats.data.remote.GoogleBooksApi
import com.bookstats.domain.model.Book
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Add Book screen.
 */
@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val repository: BookRepository,
    private val googleBooksApi: GoogleBooksApi
) : ViewModel() {
    
    private val _coverSearchResults = MutableStateFlow<List<BookCoverResult>>(emptyList())
    val coverSearchResults: StateFlow<List<BookCoverResult>> = _coverSearchResults.asStateFlow()

    private val _isSearchingCovers = MutableStateFlow(false)
    val isSearchingCovers: StateFlow<Boolean> = _isSearchingCovers.asStateFlow()

    private val _coverSearchError = MutableStateFlow<String?>(null)
    val coverSearchError: StateFlow<String?> = _coverSearchError.asStateFlow()

    private val _selectedCover = MutableStateFlow<BookCoverResult?>(null)
    val selectedCover: StateFlow<BookCoverResult?> = _selectedCover.asStateFlow()

    private var searchJob: Job? = null

    /**
     * Search for book covers by title with debounce.
     * Waits for SEARCH_DEBOUNCE_MS after the last keystroke before making the API call.
     */
    fun searchCovers(title: String) {
        // Cancel any pending search
        searchJob?.cancel()
        _coverSearchError.value = null

        if (title.isBlank()) {
            _coverSearchResults.value = emptyList()
            _isSearchingCovers.value = false
            return
        }

        searchJob = viewModelScope.launch {
            // Debounce: wait before searching
            delay(SEARCH_DEBOUNCE_MS)

            _isSearchingCovers.value = true
            try {
                _coverSearchResults.value = googleBooksApi.searchByTitle(title, maxResults = MAX_COVER_RESULTS)
                _coverSearchError.value = null
            } catch (e: Exception) {
                _coverSearchResults.value = emptyList()
                _coverSearchError.value = "Failed to search covers. Check your connection."
            } finally {
                _isSearchingCovers.value = false
                searchJob = null
            }
        }
    }

    /**
     * Clear cover search error.
     */
    fun clearCoverSearchError() {
        _coverSearchError.value = null
    }
    
    /**
     * Select a cover from search results.
     * Returns the selected cover for auto-filling fields.
     */
    fun selectCover(cover: BookCoverResult) {
        _selectedCover.value = cover
    }
    
    /**
     * Clear selected cover.
     */
    fun clearSelectedCover() {
        _selectedCover.value = null
    }
    
    /**
     * Clear search results and error.
     */
    fun clearCoverSearch() {
        _coverSearchResults.value = emptyList()
        _coverSearchError.value = null
    }

    /**
     * Get the best working cover URL for a BookCoverResult.
     * Tries high-res first, falls back to thumbnail if unavailable.
     */
    suspend fun getBestCoverUrl(cover: BookCoverResult): String? {
        return googleBooksApi.getBestWorkingUrl(cover)
    }

    /**
     * Add a new book with all metadata.
     * Input is sanitized with length limits to prevent database issues.
     */
    suspend fun addBook(
        title: String,
        author: String,
        category: String,
        totalPages: Int,
        notes: String,
        coverImageUrl: String?
    ): Long {
        val book = Book(
            title = title.trim().take(MAX_TITLE_LENGTH),
            author = author.trim().take(MAX_AUTHOR_LENGTH),
            category = category.trim().take(MAX_CATEGORY_LENGTH),
            totalPages = totalPages.coerceIn(1, MAX_PAGE_COUNT),
            notes = notes.trim().take(MAX_NOTES_LENGTH),
            coverImageUrl = coverImageUrl?.take(MAX_URL_LENGTH)
        )
        return repository.insertBook(book)
    }

    companion object {
        const val SEARCH_DEBOUNCE_MS = 300L
        const val MAX_COVER_RESULTS = 8
        const val MIN_SEARCH_LENGTH = 3
        const val MAX_TITLE_LENGTH = 500
        const val MAX_AUTHOR_LENGTH = 300
        const val MAX_CATEGORY_LENGTH = 200
        const val MAX_NOTES_LENGTH = 5000
        const val MAX_URL_LENGTH = 2000
        const val MAX_PAGE_COUNT = 10000
    }
}
