package com.bookstats.data.local.database.dao

import androidx.room.*
import com.bookstats.data.local.database.entity.BookEntity
import com.bookstats.data.local.database.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for book operations.
 */
@Dao
interface BookDao {
    
    @Query("SELECT * FROM books WHERE sync_status != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun getAllBooks(): Flow<List<BookEntity>>
    
    @Query("SELECT * FROM books WHERE id = :bookId AND sync_status != 'PENDING_DELETE'")
    suspend fun getBookById(bookId: Long): BookEntity?
    
    @Query("SELECT * FROM books WHERE id = :bookId AND sync_status != 'PENDING_DELETE'")
    fun getBookByIdFlow(bookId: Long): Flow<BookEntity?>
    
    @Query("SELECT * FROM books WHERE remote_id = :remoteId")
    suspend fun getBookByRemoteId(remoteId: String): BookEntity?
    
    @Query("SELECT * FROM books WHERE title LIKE '%' || :query || '%' AND sync_status != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun searchBooks(query: String): Flow<List<BookEntity>>
    
    // Sync-related queries
    @Query("SELECT * FROM books WHERE sync_status = :status")
    suspend fun getBooksBySyncStatus(status: SyncStatus): List<BookEntity>
    
    @Query("SELECT * FROM books WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedBooks(): List<BookEntity>
    
    @Query("UPDATE books SET sync_status = :status WHERE id = :bookId")
    suspend fun updateSyncStatus(bookId: Long, status: SyncStatus)
    
    @Query("UPDATE books SET remote_id = :remoteId, sync_status = 'SYNCED' WHERE id = :bookId")
    suspend fun markAsSynced(bookId: Long, remoteId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity): Long
    
    @Update
    suspend fun updateBook(book: BookEntity)
    
    @Delete
    suspend fun deleteBook(book: BookEntity)
    
    @Query("DELETE FROM books WHERE id = :bookId")
    suspend fun deleteBookById(bookId: Long)

    // Hard delete for items that were pending create (never synced to cloud)
    @Query("DELETE FROM books WHERE id = :bookId AND sync_status = 'PENDING_CREATE'")
    suspend fun hardDeleteIfPendingCreate(bookId: Long): Int
    
    @Query("DELETE FROM books")
    suspend fun deleteAllBooks()
}
