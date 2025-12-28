package com.bookstats.data.local.database.dao

import androidx.room.*
import com.bookstats.data.local.database.entity.ReadingSessionEntity
import com.bookstats.data.local.database.entity.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for reading session operations.
 */
@Dao
interface ReadingSessionDao {
    
    @Query("SELECT * FROM reading_sessions WHERE sync_status != 'PENDING_DELETE' ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<ReadingSessionEntity>>
    
    @Query("SELECT * FROM reading_sessions WHERE bookId = :bookId AND sync_status != 'PENDING_DELETE' ORDER BY startTime DESC")
    fun getSessionsForBook(bookId: Long): Flow<List<ReadingSessionEntity>>
    
    @Query("SELECT * FROM reading_sessions WHERE id = :sessionId AND sync_status != 'PENDING_DELETE'")
    suspend fun getSessionById(sessionId: Long): ReadingSessionEntity?
    
    @Query("SELECT * FROM reading_sessions WHERE remote_id = :remoteId")
    suspend fun getSessionByRemoteId(remoteId: String): ReadingSessionEntity?
    
    // Sync-related queries
    @Query("SELECT * FROM reading_sessions WHERE sync_status = :status")
    suspend fun getSessionsBySyncStatus(status: SyncStatus): List<ReadingSessionEntity>
    
    @Query("SELECT * FROM reading_sessions WHERE sync_status != 'SYNCED'")
    suspend fun getUnsyncedSessions(): List<ReadingSessionEntity>
    
    @Query("UPDATE reading_sessions SET sync_status = :status WHERE id = :sessionId")
    suspend fun updateSyncStatus(sessionId: Long, status: SyncStatus)
    
    @Query("UPDATE reading_sessions SET remote_id = :remoteId, sync_status = 'SYNCED' WHERE id = :sessionId")
    suspend fun markAsSynced(sessionId: Long, remoteId: String)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ReadingSessionEntity): Long
    
    @Update
    suspend fun updateSession(session: ReadingSessionEntity)
    
    @Delete
    suspend fun deleteSession(session: ReadingSessionEntity)
    
    @Query("DELETE FROM reading_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: Long)
    
    // Hard delete for items that were pending create (never synced to cloud)
    @Query("DELETE FROM reading_sessions WHERE id = :sessionId AND sync_status = 'PENDING_CREATE'")
    suspend fun hardDeleteIfPendingCreate(sessionId: Long): Int
    
    @Query("DELETE FROM reading_sessions WHERE bookId = :bookId")
    suspend fun deleteSessionsForBook(bookId: Long)
    
    @Query("DELETE FROM reading_sessions")
    suspend fun deleteAllSessions()
}
