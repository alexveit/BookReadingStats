package com.bookstats.domain.model

/**
 * Statistics for a single category.
 */
data class CategoryStatistics(
    val category: Category,
    val bookCount: Int,
    val totalPagesRead: Int,
    val totalMinutesRead: Int,
    val pagesRemaining: Int,
    val sessionCount: Int
) {
    /**
     * Total time formatted as "Xh Ym".
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
}
