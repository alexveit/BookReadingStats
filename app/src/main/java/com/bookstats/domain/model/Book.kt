package com.bookstats.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * Domain model representing a book with its reading statistics.
 */
data class Book(
    val id: Long = 0,
    val title: String,
    val author: String = "",
    val categories: List<Category> = emptyList(),
    val totalPages: Int,
    val notes: String = "",
    val coverImageUrl: String? = null,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val sessions: List<ReadingSession> = emptyList()
) {
    /**
     * Categories formatted as a comma-separated string.
     */
    val categoriesFormatted: String
        get() = categories.joinToString(", ") { it.name }

    /**
     * Whether the book has a cover image.
     */
    val hasCover: Boolean
        get() = !coverImageUrl.isNullOrBlank()
    
    /**
     * Current page (last page read from most recent session).
     */
    val currentPage: Int
        get() = sessions.maxByOrNull { it.endTime }?.endPage ?: 0
    
    /**
     * Total pages read across all sessions.
     */
    val pagesRead: Int
        get() = sessions.sumOf { it.pagesRead }
    
    /**
     * Pages remaining to finish the book.
     */
    val pagesRemaining: Int
        get() = (totalPages - currentPage).coerceAtLeast(0)
    
    /**
     * Percentage of the book completed (0-100).
     */
    val percentComplete: Int
        get() = if (totalPages > 0) {
            ((currentPage.toFloat() / totalPages) * 100).roundToInt().coerceIn(0, 100)
        } else 0
    
    /**
     * Whether the book has been completed.
     */
    val isFinished: Boolean
        get() = currentPage >= totalPages && totalPages > 0
    
    /**
     * Whether any reading has been done.
     */
    val isStarted: Boolean
        get() = currentPage > 0
    
    /**
     * Total reading time in minutes.
     */
    val totalMinutesRead: Int
        get() = sessions.sumOf { it.durationMinutes }
    
    /**
     * Total reading time formatted as "Xh Ym".
     */
    val totalTimeFormatted: String
        get() {
            val hours = totalMinutesRead / 60
            val minutes = totalMinutesRead % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
    
    /**
     * Average reading speed in pages per minute.
     */
    val pagesPerMinute: Float
        get() = if (totalMinutesRead > 0) {
            pagesRead.toFloat() / totalMinutesRead
        } else 0f
    
    /**
     * Average time per page in seconds.
     */
    val secondsPerPage: Int
        get() = if (pagesRead > 0) {
            (totalMinutesRead * 60) / pagesRead
        } else 0
    
    /**
     * Average time per page formatted as "Xm Ys".
     */
    val timePerPageFormatted: String
        get() {
            val totalSeconds = secondsPerPage
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return when {
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    
    /**
     * Estimated time to finish the book in minutes.
     */
    val estimatedMinutesRemaining: Int
        get() = if (pagesRead > 0 && pagesRemaining > 0) {
            (pagesRemaining * totalMinutesRead) / pagesRead
        } else if (pagesRemaining > 0) {
            // If no reading done yet, return -1 to indicate unknown
            -1
        } else 0
    
    /**
     * Estimated time remaining formatted.
     */
    val estimatedTimeRemainingFormatted: String
        get() {
            val minutes = estimatedMinutesRemaining
            if (minutes < 0) return "Unknown"
            if (minutes == 0) return "Done!"
            
            val days = minutes / (60 * 24)
            val hours = (minutes % (60 * 24)) / 60
            val mins = minutes % 60
            
            return when {
                days > 0 -> "${days}d ${hours}h ${mins}m"
                hours > 0 -> "${hours}h ${mins}m"
                else -> "${mins}m"
            }
        }
    
    /**
     * Date the book was started (first session).
     */
    val startDate: LocalDate?
        get() = sessions.minByOrNull { it.startTime }?.startTime?.let {
            LocalDate.ofInstant(it, ZoneId.systemDefault())
        }
    
    /**
     * Date the book was finished (if applicable).
     */
    val finishDate: LocalDate?
        get() = if (isFinished) {
            sessions.maxByOrNull { it.endTime }?.endTime?.let {
                LocalDate.ofInstant(it, ZoneId.systemDefault())
            }
        } else null
    
    /**
     * Start date formatted as "MM/dd/yyyy".
     */
    val startDateFormatted: String
        get() = startDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "--"
    
    /**
     * Finish date formatted as "MM/dd/yyyy".
     */
    val finishDateFormatted: String
        get() = finishDate?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")) ?: "--"
    
    /**
     * Reading status for UI display.
     */
    val status: ReadingStatus
        get() = when {
            isFinished -> ReadingStatus.FINISHED
            isStarted -> ReadingStatus.IN_PROGRESS
            else -> ReadingStatus.NOT_STARTED
        }
}

/**
 * Enum representing the reading status of a book.
 */
enum class ReadingStatus {
    NOT_STARTED,
    IN_PROGRESS,
    FINISHED
}
