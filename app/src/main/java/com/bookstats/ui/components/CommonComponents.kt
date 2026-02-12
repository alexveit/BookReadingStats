package com.bookstats.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bookstats.R
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.DailyChartData
import com.bookstats.domain.model.ReadingStatus
import com.bookstats.ui.theme.FinishedGreen
import com.bookstats.ui.theme.InProgressYellow
import com.bookstats.ui.theme.NotStartedRed

/**
 * Card displaying a book's information and progress.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookCard(
    book: Book,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (book.status) {
        ReadingStatus.FINISHED -> FinishedGreen
        ReadingStatus.IN_PROGRESS -> InProgressYellow
        ReadingStatus.NOT_STARTED -> NotStartedRed
    }
    
    val statusIcon = when (book.status) {
        ReadingStatus.FINISHED -> Icons.Filled.CheckCircle
        ReadingStatus.IN_PROGRESS -> Icons.AutoMirrored.Filled.MenuBook
        ReadingStatus.NOT_STARTED -> Icons.Filled.Book
    }
    
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = statusIcon,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${book.percentComplete}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = statusColor
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress bar
            ProgressBar(
                progress = book.percentComplete / 100f,
                color = statusColor,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pages",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${book.pagesRead} / ${book.totalPages}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Time Read",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = book.totalTimeFormatted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (!book.isFinished && book.isStarted) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Est. Left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = book.estimatedTimeRemainingFormatted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

/**
 * Animated progress bar.
 */
@Composable
fun ProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    height: Int = 8
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 500),
        label = "progress"
    )
    
    Box(
        modifier = modifier
            .height(height.dp)
            .clip(RoundedCornerShape(height / 2))
            .background(backgroundColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animatedProgress)
                .clip(RoundedCornerShape(height / 2))
                .background(color)
        )
    }
}

/**
 * Statistics item for displaying a label and value.
 */
@Composable
fun StatItem(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Empty state placeholder.
 */
@Composable
fun EmptyState(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Loading indicator.
 */
@Composable
fun LoadingIndicator(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

/**
 * Reusable bar chart for displaying reading progress (pages and time).
 * Shows bars evenly spaced with an optional date range summary.
 */
@Composable
fun ReadingBarChart(
    data: List<DailyChartData>,
    modifier: Modifier = Modifier,
    maxBarHeight: Dp = 180.dp,
    showDateRangeSummary: Boolean = false,
    maxDataPoints: Int = 14
) {
    val displayData = data.takeLast(maxDataPoints)

    if (displayData.isEmpty()) {
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

    val maxPages by remember(displayData) {
        derivedStateOf { displayData.maxOfOrNull { it.pagesRead }?.coerceAtLeast(1) ?: 1 }
    }
    val maxMinutes by remember(displayData) {
        derivedStateOf { displayData.maxOfOrNull { it.minutesRead }?.coerceAtLeast(1) ?: 1 }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.Bottom
        ) {
            displayData.forEach { dayData ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    // Pages bar
                    val pagesHeight = (dayData.pagesRead.toFloat() / maxPages * maxBarHeight.value).coerceAtLeast(4f)
                    Surface(
                        modifier = Modifier
                            .width(8.dp)
                            .height(pagesHeight.dp),
                        color = FinishedGreen,
                        shape = MaterialTheme.shapes.small
                    ) {}

                    // Time bar
                    val timeHeight = (dayData.minutesRead.toFloat() / maxMinutes * maxBarHeight.value).coerceAtLeast(4f)
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

        // Date range summary
        if (showDateRangeSummary && displayData.isNotEmpty()) {
            val firstDate = displayData.first().date
            val lastDate = displayData.last().date
            val summaryText = if (firstDate == lastDate) {
                firstDate
            } else {
                "$firstDate - $lastDate"
            }
            Text(
                text = summaryText,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
