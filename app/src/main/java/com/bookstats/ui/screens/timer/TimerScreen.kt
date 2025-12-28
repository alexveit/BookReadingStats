package com.bookstats.ui.screens.timer

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.bookstats.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bookstats.ui.theme.InProgressYellow
import com.bookstats.ui.theme.ReadingBlue
import com.bookstats.ui.util.PermissionHelper
import kotlinx.coroutines.flow.collectLatest

/**
 * Timer/Stopwatch screen for tracking reading sessions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    onNavigateBack: () -> Unit,
    viewModel: TimerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val book by viewModel.book.collectAsStateWithLifecycle()
    
    var showCancelDialog by remember { mutableStateOf(false) }
    var showPermissionRationale by remember { mutableStateOf(false) }
    var showZeroPagesDialog by remember { mutableStateOf(false) }
    var pageText by remember(timerState.currentPage) { 
        mutableStateOf(timerState.currentPage.toString()) 
    }
    
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Check if notification permission is granted (either already or just now)
        val notificationGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check if it was in this request and granted, OR if it's already granted
            permissions[Manifest.permission.POST_NOTIFICATIONS] == true ||
                PermissionHelper.hasNotificationPermission(context)
        } else true
        
        if (notificationGranted) {
            viewModel.startTimer()
        }
        // Phone state permission is optional - timer works without it
    }
    
    // Function to start timer with permission check
    fun startTimerWithPermissions() {
        val missingPermissions = PermissionHelper.getMissingPermissions(context)
        if (missingPermissions.isEmpty()) {
            viewModel.startTimer()
        } else {
            permissionLauncher.launch(missingPermissions.toTypedArray())
        }
    }
    
    // Handle events
    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is TimerEvent.SessionSaved -> onNavigateBack()
                is TimerEvent.Cancelled -> onNavigateBack()
                is TimerEvent.ConfirmZeroPages -> {
                    showZeroPagesDialog = true
                }
                is TimerEvent.Error -> {
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }
    
    // Handle back press
    BackHandler(enabled = timerState.hasStarted) {
        if (timerState.hasStarted) {
            viewModel.pauseTimer()
            showCancelDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(book?.title ?: stringResource(R.string.reading_session)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (timerState.isPausedByInterrupt) 
                        InProgressYellow 
                    else 
                        MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Interrupt banner
            AnimatedVisibility(
                visible = timerState.isPausedByInterrupt,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = InProgressYellow.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhoneInTalk,
                            contentDescription = null,
                            tint = InProgressYellow
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.timer_paused),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = timerState.interruptReason ?: "Interrupted",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { viewModel.startTimer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = InProgressYellow
                            )
                        ) {
                            Text(stringResource(R.string.resume))
                        }
                    }
                }
            }
            
            // Book info
            book?.let { currentBook ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = stringResource(R.string.total_pages),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentBook.totalPages}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = stringResource(R.string.pages_read),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${currentBook.pagesRead}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Timer display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timerState.formattedTime,
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Light
                    ),
                    color = when {
                        timerState.isRunning -> ReadingBlue
                        timerState.isPausedByInterrupt -> InProgressYellow
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
                
                AnimatedVisibility(
                    visible = timerState.isRunning,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FiberManualRecord,
                            contentDescription = null,
                            tint = ReadingBlue,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.reading_ellipsis),
                            style = MaterialTheme.typography.bodyLarge,
                            color = ReadingBlue
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Page input (visible when paused and started)
            AnimatedVisibility(
                visible = timerState.hasStarted && !timerState.isRunning,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(vertical = 24.dp)
                ) {
                    Text(
                        text = stringResource(R.string.what_page_now),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pageText,
                        onValueChange = { value ->
                            // Filter to digits only, limit length, strip leading zeros
                            val filtered = value.filter { it.isDigit() }.take(5)
                            val normalized = filtered.dropWhile { it == '0' }.ifEmpty { if (filtered.isNotEmpty()) "0" else "" }
                            pageText = normalized
                            normalized.toIntOrNull()?.let { page ->
                                // Validate against book's total pages
                                val maxPage = book?.totalPages ?: Int.MAX_VALUE
                                if (page in 0..maxPage) {
                                    viewModel.updateCurrentPage(page)
                                }
                            }
                        },
                        label = { Text(stringResource(R.string.current_page)) },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        singleLine = true,
                        modifier = Modifier.width(150.dp),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp
                        )
                    )
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Control buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (!timerState.hasStarted) {
                    // Not started yet - show Start button
                    Button(
                        onClick = { startTimerWithPermissions() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.start_reading), style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = { onNavigateBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                } else if (timerState.isRunning) {
                    // Timer running - show Pause button
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Icon(Icons.Default.Pause, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.pause), style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                    // Timer paused - show Resume, Save, Cancel (vertical stack)
                    Button(
                        onClick = { viewModel.startTimer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.resume_reading), style = MaterialTheme.typography.titleMedium)
                    }

                    OutlinedButton(
                        onClick = { viewModel.saveSession() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.save_session), style = MaterialTheme.typography.titleMedium)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(
                            onClick = {
                                viewModel.pauseTimer()
                                showCancelDialog = true
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.cancel))
                        }

                        TextButton(
                            onClick = { viewModel.resetTimer() }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.reset_timer))
                        }
                    }
                }
            }
        }
    }
    
    // Cancel confirmation dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(stringResource(R.string.cancel_session)) },
            text = { Text(stringResource(R.string.cancel_session_confirm)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        viewModel.cancel()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(stringResource(R.string.cancel_session))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(stringResource(R.string.continue_reading))
                }
            }
        )
    }
    
    // Zero pages warning dialog
    if (showZeroPagesDialog) {
        AlertDialog(
            onDismissRequest = { showZeroPagesDialog = false },
            title = { Text(stringResource(R.string.no_pages_read_title)) },
            text = { Text(stringResource(R.string.no_pages_read_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showZeroPagesDialog = false
                        viewModel.confirmSaveWithZeroPages()
                    }
                ) {
                    Text(stringResource(R.string.save_anyway))
                }
            },
            dismissButton = {
                TextButton(onClick = { showZeroPagesDialog = false }) {
                    Text(stringResource(R.string.update_page))
                }
            }
        )
    }
}
