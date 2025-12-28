package com.bookstats.data.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.bookstats.data.local.database.dao.BookDao
import com.bookstats.data.local.database.dao.ReadingSessionDao
import com.bookstats.data.local.database.entity.SyncStatus
import com.bookstats.data.remote.SupabaseDataSource
import com.bookstats.data.remote.dto.BookInsertDto
import com.bookstats.data.remote.dto.BookUpdateDto
import com.bookstats.data.remote.dto.ReadingSessionInsertDto
import com.bookstats.data.remote.dto.ReadingSessionUpdateDto
import com.bookstats.data.remote.dto.TimestampConverter.toIsoString
import com.bookstats.data.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages synchronization between local Room database and Supabase.
 */
@Singleton
class SyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bookDao: BookDao,
    private val sessionDao: ReadingSessionDao,
    private val remoteDataSource: SupabaseDataSource,
    private val authRepository: AuthRepository
) {
    companion object {
        private const val TAG = "SyncManager"
    }
    
    /**
     * Check if device has internet connectivity.
     */
    fun isOnline(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Sync all pending changes to Supabase.
     * Returns true if sync was successful, false otherwise.
     */
    suspend fun syncAll(): Boolean = withContext(Dispatchers.IO) {
        if (!isOnline()) {
            Log.d(TAG, "Offline - skipping sync")
            return@withContext false
        }
        
        val userId = authRepository.getCurrentUserId()
        if (userId == null) {
            Log.d(TAG, "Not authenticated - skipping sync")
            return@withContext false
        }
        
        try {
            Log.d(TAG, "Starting sync for user: $userId")
            
            // Sync books first (sessions depend on books)
            syncPendingBooks(userId)
            
            // Then sync sessions
            syncPendingSessions(userId)
            
            // Handle deletions
            syncPendingDeletes(userId)
            
            Log.d(TAG, "Sync completed successfully")
            return@withContext true
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            return@withContext false
        }
    }
    
    /**
     * Sync books that need to be created on Supabase.
     */
    private suspend fun syncPendingBooks(userId: String) {
        val pendingCreate = bookDao.getBooksBySyncStatus(SyncStatus.PENDING_CREATE)
        Log.d(TAG, "Books pending create: ${pendingCreate.size}")
        
        for (book in pendingCreate) {
            try {
                val insertDto = BookInsertDto(
                    userId = userId,
                    title = book.title,
                    author = book.author,
                    category = book.category,
                    totalPages = book.totalPages,
                    notes = book.notes,
                    coverImageUrl = book.coverImageUrl
                )
                val remoteBook = remoteDataSource.insertBook(insertDto)
                
                // Safe null handling instead of force unwrap
                remoteBook.id?.let { remoteId ->
                    bookDao.markAsSynced(book.id, remoteId)
                    Log.d(TAG, "Synced book: ${book.title} -> $remoteId")
                } ?: run {
                    Log.e(TAG, "Remote book ID was null for: ${book.title}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync book: ${book.title}", e)
            }
        }
        
        // Handle updates
        val pendingUpdate = bookDao.getBooksBySyncStatus(SyncStatus.PENDING_UPDATE)
        Log.d(TAG, "Books pending update: ${pendingUpdate.size}")
        
        for (book in pendingUpdate) {
            if (book.remoteId == null) {
                // Should not happen, but handle it
                bookDao.updateSyncStatus(book.id, SyncStatus.PENDING_CREATE)
                continue
            }
            
            try {
                val updateDto = BookUpdateDto(
                    title = book.title,
                    author = book.author,
                    category = book.category,
                    totalPages = book.totalPages,
                    notes = book.notes,
                    coverImageUrl = book.coverImageUrl
                )
                remoteDataSource.updateBook(userId, book.remoteId, updateDto)
                bookDao.updateSyncStatus(book.id, SyncStatus.SYNCED)
                Log.d(TAG, "Updated book: ${book.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update book: ${book.title}", e)
            }
        }
    }
    
    /**
     * Sync sessions that need to be created on Supabase.
     */
    private suspend fun syncPendingSessions(userId: String) {
        val pendingCreate = sessionDao.getSessionsBySyncStatus(SyncStatus.PENDING_CREATE)
        Log.d(TAG, "Sessions pending create: ${pendingCreate.size}")
        
        for (session in pendingCreate) {
            // Get the book's remote ID
            val book = bookDao.getBookById(session.bookId)
            if (book?.remoteId == null) {
                Log.w(TAG, "Cannot sync session - book not synced yet: ${session.bookId}")
                continue
            }
            
            try {
                val insertDto = ReadingSessionInsertDto(
                    userId = userId,
                    bookId = book.remoteId,
                    startTime = session.startTime.toIsoString(),
                    endTime = session.endTime.toIsoString(),
                    startPage = session.startPage,
                    endPage = session.endPage
                )
                val remoteSession = remoteDataSource.insertSession(insertDto)
                
                // Safe null handling instead of force unwrap
                remoteSession.id?.let { remoteId ->
                    sessionDao.markAsSynced(session.id, remoteId)
                    Log.d(TAG, "Synced session: ${session.id} -> $remoteId")
                } ?: run {
                    Log.e(TAG, "Remote session ID was null for session: ${session.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync session: ${session.id}", e)
            }
        }
        
        // Handle updates
        val pendingUpdate = sessionDao.getSessionsBySyncStatus(SyncStatus.PENDING_UPDATE)
        Log.d(TAG, "Sessions pending update: ${pendingUpdate.size}")
        
        for (session in pendingUpdate) {
            if (session.remoteId == null) {
                sessionDao.updateSyncStatus(session.id, SyncStatus.PENDING_CREATE)
                continue
            }
            
            try {
                val updateDto = ReadingSessionUpdateDto(
                    startPage = session.startPage,
                    endPage = session.endPage
                )
                remoteDataSource.updateSession(userId, session.remoteId, updateDto)
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED)
                Log.d(TAG, "Updated session: ${session.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to update session: ${session.id}", e)
            }
        }
    }
    
    /**
     * Sync pending deletes.
     */
    private suspend fun syncPendingDeletes(userId: String) {
        // Delete sessions first (due to foreign key)
        val sessionsToDelete = sessionDao.getSessionsBySyncStatus(SyncStatus.PENDING_DELETE)
        Log.d(TAG, "Sessions pending delete: ${sessionsToDelete.size}")

        for (session in sessionsToDelete) {
            try {
                if (session.remoteId != null) {
                    remoteDataSource.deleteSession(userId, session.remoteId)
                }
                sessionDao.deleteSessionById(session.id)
                Log.d(TAG, "Deleted session: ${session.id}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete session: ${session.id}", e)
            }
        }

        // Then delete books
        val booksToDelete = bookDao.getBooksBySyncStatus(SyncStatus.PENDING_DELETE)
        Log.d(TAG, "Books pending delete: ${booksToDelete.size}")

        for (book in booksToDelete) {
            try {
                if (book.remoteId != null) {
                    remoteDataSource.deleteBook(userId, book.remoteId)
                }
                bookDao.deleteBookById(book.id)
                Log.d(TAG, "Deleted book: ${book.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete book: ${book.title}", e)
            }
        }
    }
    
    /**
     * Get count of items pending sync.
     */
    suspend fun getPendingSyncCount(): Int = withContext(Dispatchers.IO) {
        val books = bookDao.getUnsyncedBooks().size
        val sessions = sessionDao.getUnsyncedSessions().size
        books + sessions
    }
}
