package com.bookstats.domain.model

/**
 * Domain model for global reading statistics.
 */
data class ReadingStatistics(
    val totalBooks: Int = 0,
    val booksFinished: Int = 0,
    val booksInProgress: Int = 0,
    val booksNotStarted: Int = 0,
    val totalPagesRead: Int = 0,
    val totalPagesAllBooks: Int = 0,
    val totalMinutesRead: Int = 0,
    val totalSessions: Int = 0
) {
    /**
     * Pages remaining across all books.
     */
    val pagesRemaining: Int
        get() = (totalPagesAllBooks - totalPagesRead).coerceAtLeast(0)
    
    /**
     * Average reading speed in pages per minute.
     */
    val averagePagesPerMinute: Float
        get() = if (totalMinutesRead > 0) {
            totalPagesRead.toFloat() / totalMinutesRead
        } else 0f
    
    /**
     * Average time per page in seconds.
     */
    val averageSecondsPerPage: Int
        get() = if (totalPagesRead > 0) {
            (totalMinutesRead * 60) / totalPagesRead
        } else 0
    
    /**
     * Total reading time formatted as "Xmo Xd Xh Xm".
     */
    val totalTimeFormatted: String
        get() {
            var remaining = totalMinutesRead
            val months = remaining / (60 * 24 * 30)
            remaining %= (60 * 24 * 30)
            val days = remaining / (60 * 24)
            remaining %= (60 * 24)
            val hours = remaining / 60
            val minutes = remaining % 60
            
            return buildString {
                if (months > 0) append("${months}mo ")
                if (days > 0) append("${days}d ")
                if (hours > 0) append("${hours}h ")
                append("${minutes}m")
            }.trim()
        }
    
    /**
     * Average time per page formatted as "Xm Xs".
     */
    val averageTimePerPageFormatted: String
        get() {
            val totalSeconds = averageSecondsPerPage
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return when {
                minutes > 0 -> "${minutes}m ${seconds}s"
                else -> "${seconds}s"
            }
        }
    
    /**
     * Pages per minute formatted to 3 decimal places.
     */
    val pagesPerMinuteFormatted: String
        get() = String.format("%.3f", averagePagesPerMinute)
}

/**
 * Data class for daily chart data.
 */
data class DailyChartData(
    val date: String,
    val pagesRead: Int,
    val minutesRead: Int
)
