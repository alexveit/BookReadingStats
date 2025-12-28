package com.bookstats.domain.model

/**
 * Type of data to display in reading charts.
 */
enum class ChartType {
    /** Show pages read */
    PAGES,
    /** Show time spent reading */
    TIME,
    /** Show both pages and time */
    BOTH
}
