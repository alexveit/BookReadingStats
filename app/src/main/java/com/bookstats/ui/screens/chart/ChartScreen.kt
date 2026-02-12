package com.bookstats.ui.screens.chart

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.domain.model.DailyChartData
import com.bookstats.ui.components.EmptyState
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.components.ReadingBarChart
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow

/**
 * Screen showing reading progress charts for a specific book.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartScreen(
    onNavigateBack: () -> Unit,
    viewModel: ChartViewModel = hiltViewModel()
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val chartData by viewModel.chartData.collectAsStateWithLifecycle()
    val aggregationDays by viewModel.aggregationDays.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    
    var showAggregationMenu by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.reading_progress))
                        book?.let {
                            Text(
                                text = it.title,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showAggregationMenu = true }) {
                            Icon(Icons.Default.DateRange, stringResource(R.string.aggregation))
                        }
                        DropdownMenu(
                            expanded = showAggregationMenu,
                            onDismissRequest = { showAggregationMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.daily)) },
                                onClick = {
                                    viewModel.setAggregationDays(1)
                                    showAggregationMenu = false
                                },
                                trailingIcon = {
                                    if (aggregationDays == 1) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.weekly)) },
                                onClick = {
                                    viewModel.setAggregationDays(7)
                                    showAggregationMenu = false
                                },
                                trailingIcon = {
                                    if (aggregationDays == 7) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.monthly)) },
                                onClick = {
                                    viewModel.setAggregationDays(30)
                                    showAggregationMenu = false
                                },
                                trailingIcon = {
                                    if (aggregationDays == 30) {
                                        Icon(Icons.Default.Check, null)
                                    }
                                }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                LoadingIndicator(modifier = Modifier.padding(paddingValues))
            }
            chartData.isEmpty() -> {
                EmptyState(
                    icon = Icons.AutoMirrored.Filled.ShowChart,
                    title = stringResource(R.string.no_data_yet),
                    subtitle = stringResource(R.string.start_reading_to_see_progress),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Main chart
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
                                    text = stringResource(R.string.pages_and_time),
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
                                    .height(250.dp),
                                maxBarHeight = 180.dp,
                                showDateRangeSummary = true,
                                maxDataPoints = 14
                            )
                        }
                    }
                    
                    // Summary stats
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.period_summary),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Memoize calculations to avoid recomputation on every recomposition
                            val totalPages by remember(chartData) {
                                derivedStateOf { chartData.sumOf { it.pagesRead } }
                            }
                            val totalMinutes by remember(chartData) {
                                derivedStateOf { chartData.sumOf { it.minutesRead } }
                            }
                            val avgPagesPerDay by remember(chartData) {
                                derivedStateOf { if (chartData.isNotEmpty()) totalPages / chartData.size else 0 }
                            }
                            val avgMinutesPerDay by remember(chartData) {
                                derivedStateOf { if (chartData.isNotEmpty()) totalMinutes / chartData.size else 0 }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn(
                                    value = "$totalPages",
                                    label = stringResource(R.string.total_pages_read)
                                )
                                StatColumn(
                                    value = formatMinutes(totalMinutes),
                                    label = stringResource(R.string.total_time)
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatColumn(
                                    value = "$avgPagesPerDay",
                                    label = stringResource(R.string.avg_pages_per_day)
                                )
                                StatColumn(
                                    value = formatMinutes(avgMinutesPerDay),
                                    label = stringResource(R.string.avg_time_per_day)
                                )
                            }
                        }
                    }
                    
                    // Data table
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.daily_breakdown),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Header
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(R.string.date),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(R.string.pages),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = stringResource(R.string.time),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            
                            chartData.takeLast(10).reversed().forEach { data ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = data.date,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${data.pagesRead}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatMinutes(data.minutesRead),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatColumn(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}
