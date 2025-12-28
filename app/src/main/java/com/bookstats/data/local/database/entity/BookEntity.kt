package com.bookstats.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Room entity for books.
 */
@Entity(
    tableName = "books",
    indices = [
        Index("remote_id", unique = true),
        Index("sync_status"),
        Index("createdAt")
    ]
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,
    val title: String,
    val author: String = "",
    val category: String = "",
    @ColumnInfo(name = "total_pages")
    val totalPages: Int,
    val notes: String = "",
    @ColumnInfo(name = "cover_image_url")
    val coverImageUrl: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING_CREATE
)
