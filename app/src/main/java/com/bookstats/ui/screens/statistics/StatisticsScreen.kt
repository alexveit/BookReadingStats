package com.bookstats.ui.screens.statistics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.domain.model.CategoryStatistics
import com.bookstats.domain.model.DailyChartData
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.components.ReadingBarChart
import com.bookstats.ui.components.StatItem
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow

private enum class ExpandedStat {
    TIME, PAGES, REMAINING, SESSIONS
}

/**
 * Statistics screen showing global reading stats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onNavigateBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val categoryStatistics by viewModel.categoryStatistics.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    var expandedStat by remember { mutableStateOf<ExpandedStat?>(null) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            LoadingIndicator(modifier = Modifier.padding(paddingValues))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Books overview
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.books),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(
                                label = stringResource(R.string.total),
                                value = statistics.totalBooks.toString(),
                                icon = Icons.AutoMirrored.Filled.MenuBook
                            )
                            StatItem(
                                label = stringResource(R.string.finished),
                                value = statistics.booksFinished.toString(),
                                icon = Icons.Default.CheckCircle
                            )
                            StatItem(
                                label = stringResource(R.string.reading),
                                value = statistics.booksInProgress.toString(),
                                icon = Icons.Default.AutoStories
                            )
                            StatItem(
                                label = stringResource(R.string.unstarted),
                                value = statistics.booksNotStarted.toString(),
                                icon = Icons.Default.Book
                            )
                        }
                    }
                }
                
                // Reading stats
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.reading_stats),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ExpandableStatRow(
                            label = stringResource(R.string.total_time_read),
                            value = statistics.totalTimeFormatted,
                            icon = Icons.Default.Timer,
                            isExpanded = expandedStat == ExpandedStat.TIME,
                            onToggle = {
                                expandedStat = if (expandedStat == ExpandedStat.TIME) null else ExpandedStat.TIME
                            },
                            categoryBreakdown = categoryStatistics.map { it.category.name to it.totalTimeFormatted }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        ExpandableStatRow(
                            label = stringResource(R.string.total_pages_read),
                            value = "${statistics.totalPagesRead}",
                            icon = Icons.Default.Description,
                            isExpanded = expandedStat == ExpandedStat.PAGES,
                            onToggle = {
                                expandedStat = if (expandedStat == ExpandedStat.PAGES) null else ExpandedStat.PAGES
                            },
                            categoryBreakdown = categoryStatistics.map { it.category.name to "${it.totalPagesRead}" }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        ExpandableStatRow(
                            label = stringResource(R.string.pages_remaining),
                            value = "${statistics.pagesRemaining}",
                            icon = Icons.Default.Bookmark,
                            isExpanded = expandedStat == ExpandedStat.REMAINING,
                            onToggle = {
                                expandedStat = if (expandedStat == ExpandedStat.REMAINING) null else ExpandedStat.REMAINING
                            },
                            categoryBreakdown = categoryStatistics.map { it.category.name to "${it.pagesRemaining}" }
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        ExpandableStatRow(
                            label = stringResource(R.string.total_sessions),
                            value = "${statistics.totalSessions}",
                            icon = Icons.Default.PlayCircle,
                            isExpanded = expandedStat == ExpandedStat.SESSIONS,
                            onToggle = {
                                expandedStat = if (expandedStat == ExpandedStat.SESSIONS) null else ExpandedStat.SESSIONS
                            },
                            categoryBreakdown = categoryStatistics.map { it.category.name to "${it.sessionCount}" }
                        )
                    }
                }
                
                // Average stats
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.averages),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = statistics.averageTimePerPageFormatted,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.per_page_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = statistics.pagesPerMinuteFormatted,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = stringResource(R.string.pages_per_min_label),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Chart preview card
                if (chartData.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.reading_progress),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )

                                // Legend
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(8.dp),
                                            color = FinishedGreen,
                                            shape = MaterialTheme.shapes.small
                                        ) {}
                                        Text(
                                            text = stringResource(R.string.pages),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(8.dp),
                                            color = InProgressYellow,
                                            shape = MaterialTheme.shapes.small
                                        ) {}
                                        Text(
                                            text = stringResource(R.string.time),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            ReadingBarChart(
                                data = chartData,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                maxBarHeight = 150.dp,
                                showDateRangeSummary = false,
                                maxDataPoints = 14
                            )

                            Text(
                                text = stringResource(R.string.last_n_days, minOf(chartData.size, 14)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }

            }
        }
    }
}

@Composable
private fun StatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ExpandableStatRow(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    categoryBreakdown: List<Pair<String, String>>
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        label = "chevron rotation"
    )

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = categoryBreakdown.isNotEmpty()) { onToggle() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (categoryBreakdown.isNotEmpty()) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(rotationAngle),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isExpanded && categoryBreakdown.isNotEmpty(),
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 36.dp, top = 12.dp)
            ) {
                categoryBreakdown.forEach { (category, categoryValue) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = categoryValue,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}


