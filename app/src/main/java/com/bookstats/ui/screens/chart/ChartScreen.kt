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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.domain.model.ChartType
import com.bookstats.domain.model.DailyChartData
import com.bookstats.ui.components.EmptyState
import com.bookstats.ui.components.LoadingIndicator
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow
import com.bookstats.ui.theme.ReadingBlue

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
    val chartType by viewModel.chartType.collectAsStateWithLifecycle()
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
                    // Chart type selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        SingleChoiceSegmentedButtonRow {
                            SegmentedButton(
                                selected = chartType == ChartType.PAGES,
                                onClick = { viewModel.setChartType(ChartType.PAGES) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 0,
                                    count = 3
                                )
                            ) {
                                Text(stringResource(R.string.pages))
                            }
                            SegmentedButton(
                                selected = chartType == ChartType.TIME,
                                onClick = { viewModel.setChartType(ChartType.TIME) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 1,
                                    count = 3
                                )
                            ) {
                                Text(stringResource(R.string.time))
                            }
                            SegmentedButton(
                                selected = chartType == ChartType.BOTH,
                                onClick = { viewModel.setChartType(ChartType.BOTH) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = 2,
                                    count = 3
                                )
                            ) {
                                Text(stringResource(R.string.both))
                            }
                        }
                    }
                    
                    // Main chart
                    Card(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = when (chartType) {
                                    ChartType.PAGES -> stringResource(R.string.pages_read_label)
                                    ChartType.TIME -> stringResource(R.string.time_spent)
                                    ChartType.BOTH -> stringResource(R.string.pages_and_time)
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            BarChartView(
                                data = chartData,
                                chartType = chartType,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(250.dp)
                            )

                            // Legend
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                if (chartType == ChartType.PAGES || chartType == ChartType.BOTH) {
                                    LegendItem(color = FinishedGreen, label = stringResource(R.string.pages))
                                    Spacer(modifier = Modifier.width(24.dp))
                                }
                                if (chartType == ChartType.TIME || chartType == ChartType.BOTH) {
                                    LegendItem(color = ReadingBlue, label = stringResource(R.string.minutes))
                                }
                            }
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
private fun BarChartView(
    data: List<DailyChartData>,
    chartType: ChartType,
    modifier: Modifier = Modifier
) {
    val displayData = data.takeLast(14) // Show last 14 data points
    
    if (displayData.isEmpty()) return
    
    val maxPages = displayData.maxOfOrNull { it.pagesRead }?.coerceAtLeast(1) ?: 1
    val maxMinutes = displayData.maxOfOrNull { it.minutesRead }?.coerceAtLeast(1) ?: 1
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        displayData.forEach { dayData ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    if (chartType == ChartType.PAGES || chartType == ChartType.BOTH) {
                        val pagesHeight = (dayData.pagesRead.toFloat() / maxPages * 180).coerceAtLeast(4f)
                        Surface(
                            modifier = Modifier
                                .width(if (chartType == ChartType.BOTH) 6.dp else 12.dp)
                                .height(pagesHeight.dp),
                            color = FinishedGreen,
                            shape = MaterialTheme.shapes.small
                        ) {}
                    }
                    
                    if (chartType == ChartType.BOTH) {
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    
                    if (chartType == ChartType.TIME || chartType == ChartType.BOTH) {
                        val timeHeight = (dayData.minutesRead.toFloat() / maxMinutes * 180).coerceAtLeast(4f)
                        Surface(
                            modifier = Modifier
                                .width(if (chartType == ChartType.BOTH) 6.dp else 12.dp)
                                .height(timeHeight.dp),
                            color = ReadingBlue,
                            shape = MaterialTheme.shapes.small
                        ) {}
                    }
                }
                
                // Date label (show every few bars to avoid crowding)
                // Always render the text to maintain consistent bar alignment
                Text(
                    text = if (displayData.indexOf(dayData) % 3 == 0) dayData.date.takeLast(5) else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun LegendItem(
    color: Color,
    label: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(12.dp),
            color = color,
            shape = MaterialTheme.shapes.small
        ) {}
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium
        )
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
