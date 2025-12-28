package com.bookstats.data.sync

/**
 * Represents the current state of a sync operation (for UI display).
 * 
 * Note: This is different from [SyncStatus] which tracks whether
 * individual database records need syncing.
 */
sealed class SyncState {
    /** No sync in progress */
    data object Idle : SyncState()
    
    /** Currently syncing with remote */
    data object Syncing : SyncState()
    
    /** Sync completed successfully */
    data object Success : SyncState()
    
    /** Sync failed */
    data class Error(val message: String) : SyncState()
    
    /** Device is offline */
    data object Offline : SyncState()
}
