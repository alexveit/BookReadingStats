package com.bookstats.domain.repository

import com.bookstats.data.sync.SyncState
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.DailyChartData
import com.bookstats.domain.model.ReadingSession
import com.bookstats.domain.model.ReadingStatistics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for book and reading session data operations.
 */
interface BookRepository {
    
    // Sync state
    val syncState: StateFlow<SyncState>
    
    /** Manually trigger a sync refresh */
    suspend fun refreshSync()
    
    // Book operations
    fun getAllBooks(): Flow<List<Book>>
    fun getBookById(bookId: Long): Flow<Book?>
    fun searchBooks(query: String): Flow<List<Book>>
    suspend fun insertBook(book: Book): Long
    suspend fun updateBook(book: Book)
    suspend fun deleteBook(bookId: Long)
    
    // Reading session operations
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSession>>
    suspend fun insertSession(session: ReadingSession): Long
    suspend fun updateSession(session: ReadingSession)
    suspend fun deleteSession(sessionId: Long)
    
    // Statistics
    fun getReadingStatistics(): Flow<ReadingStatistics>
    fun getDailyChartData(): Flow<List<DailyChartData>>
    fun getDailyChartDataForBook(bookId: Long): Flow<List<DailyChartData>>
    
    // For book merging
    suspend fun mergeBooks(targetBookId: Long, sourceBookIds: List<Long>)
    
    // Export/Import
    suspend fun exportToJson(): String
    suspend fun importFromJson(json: String): Result<Int>
}
