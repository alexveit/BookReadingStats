package com.bookstats.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.domain.model.ChartType
import com.bookstats.domain.model.DailyChartData
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.components.StatItem
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow

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
    val chartType by viewModel.chartType.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var showChartTypeDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (chartData.isNotEmpty()) {
                        IconButton(onClick = { showChartTypeDialog = true }) {
                            Icon(Icons.AutoMirrored.Filled.ShowChart, stringResource(R.string.chart_options))
                        }
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

                        StatRow(
                            label = stringResource(R.string.total_time_read),
                            value = statistics.totalTimeFormatted,
                            icon = Icons.Default.Timer
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        StatRow(
                            label = stringResource(R.string.total_pages_read),
                            value = "${statistics.totalPagesRead}",
                            icon = Icons.Default.Description
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        StatRow(
                            label = stringResource(R.string.pages_remaining),
                            value = "${statistics.pagesRemaining}",
                            icon = Icons.Default.Bookmark
                        )

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                        StatRow(
                            label = stringResource(R.string.total_sessions),
                            value = "${statistics.totalSessions}",
                            icon = Icons.Default.PlayCircle
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

                                // Chart type chips
                                Row {
                                    FilterChip(
                                        selected = chartType == ChartType.PAGES,
                                        onClick = { viewModel.setChartType(ChartType.PAGES) },
                                        label = { Text(stringResource(R.string.pages)) },
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    FilterChip(
                                        selected = chartType == ChartType.TIME,
                                        onClick = { viewModel.setChartType(ChartType.TIME) },
                                        label = { Text(stringResource(R.string.time)) }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Simple bar chart visualization
                            SimpleBarChart(
                                data = chartData.takeLast(14),
                                chartType = chartType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
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
    
    // Chart type dialog
    if (showChartTypeDialog) {
        AlertDialog(
            onDismissRequest = { showChartTypeDialog = false },
            title = { Text(stringResource(R.string.chart_type)) },
            text = {
                Column {
                    ChartType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = chartType == type,
                                onClick = {
                                    viewModel.setChartType(type)
                                    showChartTypeDialog = false
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = when (type) {
                                    ChartType.PAGES -> stringResource(R.string.pages_read_label)
                                    ChartType.TIME -> stringResource(R.string.time_read)
                                    ChartType.BOTH -> stringResource(R.string.both)
                                }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChartTypeDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
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
private fun SimpleBarChart(
    data: List<DailyChartData>,
    chartType: ChartType,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_data_yet),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    // Memoize max calculations to avoid recomputation on every recomposition
    val maxPages by remember(data) {
        derivedStateOf { data.maxOfOrNull { it.pagesRead } ?: 1 }
    }
    val maxMinutes by remember(data) {
        derivedStateOf { data.maxOfOrNull { it.minutesRead } ?: 1 }
    }
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { dayData ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                // Bar for pages
                if (chartType == ChartType.PAGES || chartType == ChartType.BOTH) {
                    val pagesHeight = if (maxPages > 0) {
                        (dayData.pagesRead.toFloat() / maxPages * 150).coerceAtLeast(4f)
                    } else 4f
                    
                    Surface(
                        modifier = Modifier
                            .width(8.dp)
                            .height(pagesHeight.dp),
                        color = FinishedGreen,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
                
                // Bar for time
                if (chartType == ChartType.TIME || chartType == ChartType.BOTH) {
                    val timeHeight = if (maxMinutes > 0) {
                        (dayData.minutesRead.toFloat() / maxMinutes * 150).coerceAtLeast(4f)
                    } else 4f
                    
                    Spacer(modifier = Modifier.width(2.dp))
                    
                    Surface(
                        modifier = Modifier
                            .width(8.dp)
                            .height(timeHeight.dp),
                        color = InProgressYellow,
                        shape = MaterialTheme.shapes.small
                    ) {}
                }
            }
        }
    }
}
