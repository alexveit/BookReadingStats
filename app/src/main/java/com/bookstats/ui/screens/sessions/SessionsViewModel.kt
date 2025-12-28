package com.bookstats.ui.screens.sessions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.ReadingSession
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the Sessions list screen.
 */
@HiltViewModel
class SessionsViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L
    
    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()
    
    private val _sessions = MutableStateFlow<List<SessionGroup>>(emptyList())
    val sessions: StateFlow<List<SessionGroup>> = _sessions.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _events = MutableSharedFlow<SessionsEvent>()
    val events: SharedFlow<SessionsEvent> = _events.asSharedFlow()

    init {
        loadData()
    }
    
    private fun loadData() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _book.value = book
                if (book != null) {
                    // Group sessions by date
                    val grouped = book.sessions
                        .groupBy { it.date }
                        .map { (date, sessions) ->
                            SessionGroup(
                                date = date,
                                sessions = sessions.sortedByDescending { it.startTime },
                                totalPages = sessions.sumOf { it.pagesRead },
                                totalMinutes = sessions.sumOf { it.durationMinutes }
                            )
                        }
                        .sortedByDescending { it.date }
                    
                    _sessions.value = grouped
                }
                _isLoading.value = false
            }
        }
    }
    
    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
        }
    }
    
    /**
     * Update a session with page validation.
     * @return true if update succeeded, false if validation failed
     */
    fun updateSession(sessionId: Long, startPage: Int, endPage: Int) {
        viewModelScope.launch {
            val currentBook = _book.value
            if (currentBook == null) {
                _events.emit(SessionsEvent.Error("Book not found"))
                return@launch
            }

            val session = currentBook.sessions.find { it.id == sessionId }
            if (session == null) {
                _events.emit(SessionsEvent.Error("Session not found"))
                return@launch
            }

            // Validate page ranges
            if (startPage < 0) {
                _events.emit(SessionsEvent.Error("Start page cannot be negative"))
                return@launch
            }

            if (endPage <= startPage) {
                _events.emit(SessionsEvent.Error("End page must be greater than start page"))
                return@launch
            }

            if (endPage > currentBook.totalPages) {
                _events.emit(SessionsEvent.Error("End page cannot exceed total pages (${currentBook.totalPages})"))
                return@launch
            }

            try {
                val updatedSession = session.copy(
                    startPage = startPage,
                    endPage = endPage
                )
                repository.updateSession(updatedSession)
                _events.emit(SessionsEvent.SessionUpdated)
            } catch (e: Exception) {
                _events.emit(SessionsEvent.Error(e.message ?: "Failed to update session"))
            }
        }
    }
}

/**
 * One-time events from the sessions screen.
 */
sealed interface SessionsEvent {
    data object SessionUpdated : SessionsEvent
    data class Error(val message: String) : SessionsEvent
}

/**
 * Group of sessions for a single date.
 */
data class SessionGroup(
    val date: LocalDate,
    val sessions: List<ReadingSession>,
    val totalPages: Int,
    val totalMinutes: Int
) {
    val dateFormatted: String
        get() = date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    
    val totalTimeFormatted: String
        get() {
            val hours = totalMinutes / 60
            val minutes = totalMinutes % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
}
