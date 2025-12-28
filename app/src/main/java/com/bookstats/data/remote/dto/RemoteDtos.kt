package com.bookstats.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Book DTO for Supabase.
 * Maps to the 'books' table.
 */
@Serializable
data class BookDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val title: String,
    val author: String = "",
    val category: String = "",
    @SerialName("total_pages")
    val totalPages: Int,
    val notes: String = "",
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)

/**
 * Reading Session DTO for Supabase.
 * Maps to the 'reading_sessions' table.
 */
@Serializable
data class ReadingSessionDto(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    @SerialName("book_id")
    val bookId: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("duration_seconds")
    val durationSeconds: Int = 0,  // Computed by DB, read-only
    @SerialName("start_page")
    val startPage: Int,
    @SerialName("end_page")
    val endPage: Int,
    @SerialName("created_at")
    val createdAt: String? = null
)

/**
 * Insert DTOs - used when creating new records.
 */
@Serializable
data class BookInsertDto(
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val author: String = "",
    val category: String = "",
    @SerialName("total_pages")
    val totalPages: Int,
    val notes: String = "",
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null
)

@Serializable
data class BookUpdateDto(
    val title: String? = null,
    val author: String? = null,
    val category: String? = null,
    @SerialName("total_pages")
    val totalPages: Int? = null,
    val notes: String? = null,
    @SerialName("cover_image_url")
    val coverImageUrl: String? = null
)

// No duration_seconds - DB computes it!
@Serializable
data class ReadingSessionInsertDto(
    @SerialName("user_id")
    val userId: String,
    @SerialName("book_id")
    val bookId: String,
    @SerialName("start_time")
    val startTime: String,
    @SerialName("end_time")
    val endTime: String,
    @SerialName("start_page")
    val startPage: Int,
    @SerialName("end_page")
    val endPage: Int
)

@Serializable
data class ReadingSessionUpdateDto(
    @SerialName("start_page")
    val startPage: Int? = null,
    @SerialName("end_page")
    val endPage: Int? = null
)

/**
 * Extension functions for timestamp conversion.
 */
object TimestampConverter {
    private val formatter = DateTimeFormatter.ISO_INSTANT
    
    fun Instant.toIsoString(): String = formatter.format(this)
    
    fun String.toInstant(): Instant = Instant.parse(this)
}
