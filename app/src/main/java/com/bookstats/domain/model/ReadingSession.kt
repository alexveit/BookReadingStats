package com.bookstats.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Domain model representing a single reading session.
 */
data class ReadingSession(
    val id: Long = 0,
    val bookId: Long,
    val startTime: Instant,
    val endTime: Instant,
    val durationMinutes: Int,
    val pagesRead: Int,
    val startPage: Int,
    val endPage: Int,
    val notes: String = ""
) {
    /**
     * The date of this session.
     */
    val date: LocalDate
        get() = LocalDate.ofInstant(startTime, ZoneId.systemDefault())
    
    /**
     * Start time formatted as "hh:mm a".
     */
    val startTimeFormatted: String
        get() {
            val localTime = LocalTime.ofInstant(startTime, ZoneId.systemDefault())
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    
    /**
     * End time formatted as "hh:mm a".
     */
    val endTimeFormatted: String
        get() {
            val localTime = LocalTime.ofInstant(endTime, ZoneId.systemDefault())
            return localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
        }
    
    /**
     * Date formatted as "MM/dd/yyyy".
     */
    val dateFormatted: String
        get() = date.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"))
    
    /**
     * Date formatted with day of week as "EEE, MMM d".
     */
    val dateFriendly: String
        get() = date.format(DateTimeFormatter.ofPattern("EEE, MMM d"))
    
    /**
     * Duration formatted as "Xh Ym" or just "Ym".
     */
    val durationFormatted: String
        get() {
            val hours = durationMinutes / 60
            val minutes = durationMinutes % 60
            return when {
                hours > 0 -> "${hours}h ${minutes}m"
                else -> "${minutes}m"
            }
        }
    
    /**
     * Pages range as "X - Y".
     */
    val pageRange: String
        get() = "$startPage → $endPage"
    
    /**
     * Summary string for display.
     */
    val summary: String
        get() = "$pagesRead pages in $durationFormatted"
    
    companion object {
        /**
         * Create a new reading session from timer data.
         */
        fun fromTimer(
            bookId: Long,
            startTime: Instant,
            endTime: Instant,
            startPage: Int,
            endPage: Int,
            notes: String = ""
        ): ReadingSession {
            val durationMillis = endTime.toEpochMilli() - startTime.toEpochMilli()
            val durationMinutes = (durationMillis / 60000).toInt().coerceAtLeast(1)
            val pagesRead = (endPage - startPage).coerceAtLeast(0)
            
            return ReadingSession(
                bookId = bookId,
                startTime = startTime,
                endTime = endTime,
                durationMinutes = durationMinutes,
                pagesRead = pagesRead,
                startPage = startPage,
                endPage = endPage,
                notes = notes
            )
        }
    }
}
