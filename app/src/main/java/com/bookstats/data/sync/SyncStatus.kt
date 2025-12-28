package com.bookstats.data.local.database.entity

/**
 * Sync status for offline-first data management.
 */
enum class SyncStatus {
    /** Fully synced with Supabase */
    SYNCED,
    
    /** Created locally, needs to be pushed to Supabase */
    PENDING_CREATE,
    
    /** Updated locally, needs to be pushed to Supabase */
    PENDING_UPDATE,
    
    /** Deleted locally, needs to be deleted from Supabase */
    PENDING_DELETE
}
