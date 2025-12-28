package com.bookstats.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.bookstats.MainActivity
import com.bookstats.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Foreground service for running the reading timer.
 * Uses TIMESTAMPS instead of counting - this ensures accuracy even when
 * the phone is locked or the app is in background.
 * 
 * How it works:
 * - When started/resumed: record startedAtElapsedRealtime
 * - When paused: accumulatedMs += (now - startedAtElapsedRealtime)
 * - Elapsed time = accumulatedMs + (if running: now - startedAtElapsedRealtime else 0)
 * 
 * Uses SystemClock.elapsedRealtime() which continues even when phone sleeps.
 */
class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var displayRefreshJob: Job? = null

    // SharedPreferences for state persistence across process death
    private lateinit var prefs: SharedPreferences

    // TIMESTAMP-BASED TIMER STATE
    // Using elapsedRealtime which keeps running even when phone sleeps
    private var startedAtElapsedRealtime: Long = 0L  // When current run started
    private var accumulatedMs: Long = 0L              // Total time from previous runs
    
    // Observable state
    private val _elapsedSeconds = MutableStateFlow(0)
    val elapsedSeconds: StateFlow<Int> = _elapsedSeconds.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    private val _isPausedByInterrupt = MutableStateFlow(false)
    val isPausedByInterrupt: StateFlow<Boolean> = _isPausedByInterrupt.asStateFlow()
    
    private val _interruptReason = MutableStateFlow<String?>(null)
    val interruptReason: StateFlow<String?> = _interruptReason.asStateFlow()
    
    private var bookTitle: String = "Reading Session"
    private var bookId: Long = -1L
    private var initialPage: Int = 0
    
    // Expose current session info for reconnection
    fun getCurrentBookId(): Long = bookId
    fun getInitialPage(): Int = initialPage
    fun getAccumulatedMs(): Long = accumulatedMs
    fun getStartedAtElapsedRealtime(): Long = startedAtElapsedRealtime
    
    // Audio focus for interrupt detection
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    
    // Telephony for call detection (API 31+)
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallback: TelephonyCallback? = null
    
    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onCreate() {
        super.onCreate()
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        createNotificationChannel()
        setupAudioFocusListener()
        setupPhoneCallListener()
    }

    // ==================== STATE PERSISTENCE ====================

    /**
     * Save timer state to SharedPreferences for recovery after process death.
     */
    private fun saveState() {
        prefs.edit()
            .putLong(KEY_ACCUMULATED_MS, accumulatedMs)
            .putLong(KEY_STARTED_AT, startedAtElapsedRealtime)
            .putLong(KEY_SAVED_AT_REALTIME, SystemClock.elapsedRealtime())
            .putBoolean(KEY_IS_RUNNING, _isRunning.value)
            .putBoolean(KEY_IS_PAUSED_BY_INTERRUPT, _isPausedByInterrupt.value)
            .putString(KEY_INTERRUPT_REASON, _interruptReason.value)
            .putLong(KEY_BOOK_ID, bookId)
            .putString(KEY_BOOK_TITLE, bookTitle)
            .putInt(KEY_INITIAL_PAGE, initialPage)
            .putBoolean(KEY_HAS_ACTIVE_SESSION, true)
            .apply()
    }

    /**
     * Restore timer state after process death.
     * Returns true if state was restored successfully.
     */
    private fun restoreState(): Boolean {
        if (!prefs.getBoolean(KEY_HAS_ACTIVE_SESSION, false)) {
            return false
        }

        val savedAtRealtime = prefs.getLong(KEY_SAVED_AT_REALTIME, 0L)
        val wasRunning = prefs.getBoolean(KEY_IS_RUNNING, false)
        val savedAccumulatedMs = prefs.getLong(KEY_ACCUMULATED_MS, 0L)
        val savedStartedAt = prefs.getLong(KEY_STARTED_AT, 0L)

        // Restore book info
        bookId = prefs.getLong(KEY_BOOK_ID, -1L)
        bookTitle = prefs.getString(KEY_BOOK_TITLE, "Reading Session") ?: "Reading Session"
        initialPage = prefs.getInt(KEY_INITIAL_PAGE, 0)

        // Restore interrupt state
        _isPausedByInterrupt.value = prefs.getBoolean(KEY_IS_PAUSED_BY_INTERRUPT, false)
        _interruptReason.value = prefs.getString(KEY_INTERRUPT_REASON, null)

        if (wasRunning) {
            // Timer was running when killed - calculate elapsed time since save
            val elapsedSinceSave = SystemClock.elapsedRealtime() - savedAtRealtime
            accumulatedMs = savedAccumulatedMs + (savedAtRealtime - savedStartedAt) + elapsedSinceSave
            startedAtElapsedRealtime = SystemClock.elapsedRealtime()
            _isRunning.value = true
            _elapsedSeconds.value = calculateElapsedSeconds()
        } else {
            // Timer was paused - restore accumulated time
            accumulatedMs = savedAccumulatedMs
            startedAtElapsedRealtime = 0L
            _isRunning.value = false
            _elapsedSeconds.value = calculateElapsedSeconds()
        }

        return bookId != -1L
    }

    /**
     * Clear saved state (called when timer is stopped by user).
     */
    private fun clearSavedState() {
        prefs.edit()
            .putBoolean(KEY_HAS_ACTIVE_SESSION, false)
            .remove(KEY_ACCUMULATED_MS)
            .remove(KEY_STARTED_AT)
            .remove(KEY_SAVED_AT_REALTIME)
            .remove(KEY_IS_RUNNING)
            .remove(KEY_IS_PAUSED_BY_INTERRUPT)
            .remove(KEY_INTERRUPT_REASON)
            .remove(KEY_BOOK_ID)
            .remove(KEY_BOOK_TITLE)
            .remove(KEY_INITIAL_PAGE)
            .apply()
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Service restarted after process death - try to restore state
        if (intent?.action == null) {
            if (restoreState()) {
                // Successfully restored - resume timer operation
                startForeground(NOTIFICATION_ID, createNotification())
                if (_isRunning.value) {
                    startDisplayRefresh()
                }
                return START_STICKY
            } else {
                // No state to restore - stop service
                stopSelf()
                return START_NOT_STICKY
            }
        }

        when (intent.action) {
            ACTION_START -> {
                bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Reading Session"
                bookId = intent.getLongExtra(EXTRA_BOOK_ID, -1L)
                initialPage = intent.getIntExtra(EXTRA_INITIAL_PAGE, 0)
                startTimer()
            }
            ACTION_PAUSE -> pauseTimer(userInitiated = true)
            ACTION_RESUME -> resumeTimer()
            ACTION_STOP -> stopTimer()
        }
        return START_STICKY
    }
    
    // ==================== TIMESTAMP-BASED TIMER ====================
    
    /**
     * Calculate current elapsed time in milliseconds.
     * This is always accurate regardless of coroutine state.
     */
    private fun calculateElapsedMs(): Long {
        return if (_isRunning.value) {
            accumulatedMs + (SystemClock.elapsedRealtime() - startedAtElapsedRealtime)
        } else {
            accumulatedMs
        }
    }
    
    /**
     * Calculate current elapsed time in seconds.
     */
    private fun calculateElapsedSeconds(): Int {
        return (calculateElapsedMs() / 1000).toInt()
    }
    
    private fun startTimer() {
        if (_isRunning.value) return
        
        // Record when this run started
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        
        _isRunning.value = true
        _isPausedByInterrupt.value = false
        _interruptReason.value = null
        
        // Request audio focus to detect interrupts
        audioFocusRequest?.let { audioManager.requestAudioFocus(it) }
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        // Start display refresh loop (just for UI, not for timekeeping!)
        startDisplayRefresh()
    }
    
    private fun pauseTimer(userInitiated: Boolean = false) {
        if (!_isRunning.value) return

        // Save accumulated time
        accumulatedMs += SystemClock.elapsedRealtime() - startedAtElapsedRealtime

        _isRunning.value = false
        displayRefreshJob?.cancel()

        if (userInitiated) {
            _isPausedByInterrupt.value = false
            _interruptReason.value = null
        }

        // Update display one more time
        _elapsedSeconds.value = calculateElapsedSeconds()
        updateNotification()

        // Save state for crash recovery
        saveState()
    }
    
    private fun resumeTimer() {
        if (_isRunning.value) return
        
        // Record when this run started (accumulated time is preserved)
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        
        _isRunning.value = true
        _isPausedByInterrupt.value = false
        _interruptReason.value = null
        
        startDisplayRefresh()
        updateNotification()
    }
    
    private fun stopTimer() {
        displayRefreshJob?.cancel()
        _isRunning.value = false
        _isPausedByInterrupt.value = false
        _interruptReason.value = null

        // Reset timestamps
        accumulatedMs = 0L
        startedAtElapsedRealtime = 0L
        _elapsedSeconds.value = 0

        // Clear saved state (user intentionally stopped)
        clearSavedState()

        // Release audio focus
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }
    
    fun resetTimer() {
        accumulatedMs = 0L
        startedAtElapsedRealtime = SystemClock.elapsedRealtime()
        _elapsedSeconds.value = 0
    }
    
    /**
     * Start the display refresh loop.
     * This ONLY updates the UI - the actual time is always calculated from timestamps.
     * Also saves state every 5 seconds for crash recovery.
     */
    private fun startDisplayRefresh() {
        displayRefreshJob?.cancel()
        displayRefreshJob = serviceScope.launch {
            var saveCounter = 0
            while (isActive && _isRunning.value) {
                // Recalculate from timestamps - always accurate!
                _elapsedSeconds.value = calculateElapsedSeconds()
                updateNotification()

                // Save state every 5 seconds for crash recovery
                saveCounter++
                if (saveCounter >= 5) {
                    saveState()
                    saveCounter = 0
                }

                delay(1000)
            }
        }
    }
    
    // ==================== AUDIO FOCUS (Detects other apps playing audio) ====================
    
    private fun setupAudioFocusListener() {
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        val focusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
            when (focusChange) {
                AudioManager.AUDIOFOCUS_LOSS,
                AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                    if (_isRunning.value) {
                        pauseForInterrupt("Audio interruption")
                    }
                }
                AudioManager.AUDIOFOCUS_GAIN -> {
                    if (_isPausedByInterrupt.value) {
                        showResumePrompt()
                    }
                }
            }
        }
        
        audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setOnAudioFocusChangeListener(focusChangeListener)
            .build()
    }
    
    // ==================== PHONE CALL DETECTION ====================
    
    private fun setupPhoneCallListener() {
        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setupModernPhoneListener()
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun setupModernPhoneListener() {
        val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
            override fun onCallStateChanged(state: Int) {
                handleCallState(state)
            }
        }
        telephonyCallback = callback

        try {
            telephonyManager?.registerTelephonyCallback(
                mainExecutor,
                callback
            )
        } catch (e: SecurityException) {
            // Permission not granted - rely on audio focus
        }
    }
    
    private fun handleCallState(state: Int) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING,
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (_isRunning.value) {
                    pauseForInterrupt(if (state == TelephonyManager.CALL_STATE_RINGING) "Incoming call" else "Phone call")
                }
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                if (_isPausedByInterrupt.value) {
                    showResumePrompt()
                }
            }
        }
    }
    
    // ==================== INTERRUPT HANDLING ====================
    
    private fun pauseForInterrupt(reason: String) {
        if (!_isRunning.value) return
        
        _isPausedByInterrupt.value = true
        _interruptReason.value = reason
        
        // Pause timer (saves accumulated time)
        pauseTimer(userInitiated = false)
        
        // Restore interrupt state after pause cleared it
        _isPausedByInterrupt.value = true
        _interruptReason.value = reason
        
        vibrateShort()
        updateNotificationForInterrupt(reason)
    }
    
    private fun showResumePrompt() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, createResumePromptNotification())
        vibratePattern()
    }
    
    private fun vibrateShort() {
        getVibrator().vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
    }
    
    private fun vibratePattern() {
        getVibrator().vibrate(VibrationEffect.createWaveform(longArrayOf(0, 150, 100, 150), -1))
    }
    
    private fun getVibrator(): Vibrator {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    
    // ==================== NOTIFICATIONS ====================
    
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reading Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Shows reading timer progress"
            setShowBadge(false)
        }
        
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        
        val resumeChannel = NotificationChannel(
            CHANNEL_ID_RESUME,
            "Timer Resume Prompts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Prompts to resume reading after interruption"
        }
        notificationManager.createNotificationChannel(resumeChannel)
    }
    
    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_NAVIGATE_TO_TIMER, true)
                putExtra(EXTRA_BOOK_ID, bookId)
                putExtra(EXTRA_INITIAL_PAGE, initialPage)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val pauseResumeIntent = PendingIntent.getService(
            this, 1,
            Intent(this, TimerService::class.java).apply {
                action = if (_isRunning.value) ACTION_PAUSE else ACTION_RESUME
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val stopIntent = PendingIntent.getService(
            this, 2,
            Intent(this, TimerService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        val statusText = when {
            _isRunning.value -> formatTime(calculateElapsedSeconds())
            _isPausedByInterrupt.value -> "⏸ Paused: ${_interruptReason.value}"
            else -> "⏸ ${formatTime(calculateElapsedSeconds())}"
        }
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(bookTitle)
            .setContentText(statusText)
            .setSmallIcon(R.drawable.ic_book)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setSilent(true)
            .addAction(
                if (_isRunning.value) R.drawable.ic_pause else R.drawable.ic_play,
                if (_isRunning.value) "Pause" else "Resume",
                pauseResumeIntent
            )
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .build()
    }
    
    private fun updateNotificationForInterrupt(reason: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("⏸ $bookTitle - Paused")
            .setContentText("$reason • ${formatTime(calculateElapsedSeconds())} elapsed")
            .setSmallIcon(R.drawable.ic_pause)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setSilent(true)
            .addAction(R.drawable.ic_play, "Resume", PendingIntent.getService(this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }, PendingIntent.FLAG_IMMUTABLE))
            .addAction(R.drawable.ic_stop, "Stop", PendingIntent.getService(this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE))
            .build()
        
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
    }
    
    private fun createResumePromptNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID_RESUME)
            .setContentTitle("📚 Resume reading?")
            .setContentText("$bookTitle • ${formatTime(calculateElapsedSeconds())} elapsed")
            .setSmallIcon(R.drawable.ic_book)
            .setContentIntent(PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP }, PendingIntent.FLAG_IMMUTABLE))
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(R.drawable.ic_play, "Resume", PendingIntent.getService(this, 1, Intent(this, TimerService::class.java).apply { action = ACTION_RESUME }, PendingIntent.FLAG_IMMUTABLE))
            .addAction(R.drawable.ic_stop, "Stop", PendingIntent.getService(this, 2, Intent(this, TimerService::class.java).apply { action = ACTION_STOP }, PendingIntent.FLAG_IMMUTABLE))
            .setAutoCancel(false)
            .build()
    }
    
    private fun updateNotification() {
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(NOTIFICATION_ID, createNotification())
    }
    
    private fun formatTime(seconds: Int): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minutes, secs)
        else String.format("%02d:%02d", minutes, secs)
    }
    
    // ==================== CLEANUP ====================
    
    override fun onDestroy() {
        super.onDestroy()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { telephonyManager?.unregisterTelephonyCallback(it) }
        }
        audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        displayRefreshJob?.cancel()
        displayRefreshJob = null
        serviceScope.cancel()
    }
    
    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val CHANNEL_ID_RESUME = "timer_resume_channel"
        const val NOTIFICATION_ID = 1

        const val ACTION_START = "com.bookstats.ACTION_START"
        const val ACTION_PAUSE = "com.bookstats.ACTION_PAUSE"
        const val ACTION_RESUME = "com.bookstats.ACTION_RESUME"
        const val ACTION_STOP = "com.bookstats.ACTION_STOP"

        const val EXTRA_BOOK_TITLE = "book_title"
        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_INITIAL_PAGE = "initial_page"
        const val EXTRA_NAVIGATE_TO_TIMER = "navigate_to_timer"

        // SharedPreferences keys for state persistence
        private const val PREFS_NAME = "timer_service_state"
        private const val KEY_ACCUMULATED_MS = "accumulated_ms"
        private const val KEY_STARTED_AT = "started_at"
        private const val KEY_SAVED_AT_REALTIME = "saved_at_realtime"
        private const val KEY_IS_RUNNING = "is_running"
        private const val KEY_IS_PAUSED_BY_INTERRUPT = "is_paused_by_interrupt"
        private const val KEY_INTERRUPT_REASON = "interrupt_reason"
        private const val KEY_BOOK_ID = "book_id_state"
        private const val KEY_BOOK_TITLE = "book_title_state"
        private const val KEY_INITIAL_PAGE = "initial_page_state"
        private const val KEY_HAS_ACTIVE_SESSION = "has_active_session"
    }
}
