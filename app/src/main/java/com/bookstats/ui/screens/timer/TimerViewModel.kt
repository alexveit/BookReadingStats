package com.bookstats.ui.screens.timer

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.ReadingSession
import com.bookstats.domain.repository.BookRepository
import com.bookstats.service.TimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import javax.inject.Inject

/**
 * ViewModel for the Timer/Stopwatch screen.
 * 
 * The SERVICE is the source of truth for timer state.
 * ViewModel just observes and controls the service.
 * 
 * Key state is persisted via SavedStateHandle to survive process death:
 * - currentPage: User-entered page number
 * - sessionStartMs: Wall-clock time when session started
 */
@HiltViewModel
class TimerViewModel @Inject constructor(
    private val repository: BookRepository,
    @ApplicationContext private val context: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val bookId: Long = savedStateHandle.get<Long>("bookId") ?: -1L
    private val initialPage: Int = savedStateHandle.get<Int>("currentPage") ?: 0
    
    // Persisted state keys
    private companion object {
        const val KEY_USER_CURRENT_PAGE = "userCurrentPage"
        const val KEY_SESSION_START_MS = "sessionStartMs"
        const val KEY_HAS_STARTED = "hasStarted"
        const val MIN_SESSION_DURATION_SECONDS = 60
    }
    
    // Wall-clock time when session started (for saving to DB)
    // Persisted to survive process death
    private var sessionStartInstant: Instant?
        get() = savedStateHandle.get<Long>(KEY_SESSION_START_MS)?.let { Instant.ofEpochMilli(it) }
        set(value) { savedStateHandle[KEY_SESSION_START_MS] = value?.toEpochMilli() }
    
    // User's current page - persisted to survive process death
    private val savedCurrentPage: StateFlow<Int> = savedStateHandle.getStateFlow(KEY_USER_CURRENT_PAGE, initialPage)
    
    // Whether session has started - persisted
    private var hasStartedSaved: Boolean
        get() = savedStateHandle.get<Boolean>(KEY_HAS_STARTED) ?: false
        set(value) { savedStateHandle[KEY_HAS_STARTED] = value }
    
    private val _timerState = MutableStateFlow(
        TimerState(
            currentPage = savedStateHandle.get<Int>(KEY_USER_CURRENT_PAGE) ?: initialPage,
            hasStarted = hasStartedSaved,
            startTime = sessionStartInstant
        )
    )
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()
    
    private val _book = MutableStateFlow<Book?>(null)
    val book: StateFlow<Book?> = _book.asStateFlow()
    
    private val _events = MutableSharedFlow<TimerEvent>()
    val events: SharedFlow<TimerEvent> = _events.asSharedFlow()
    
    // Service binding
    private var timerService: TimerService? = null
    private var serviceBound = false
    private var isBindingInProgress = false
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            isBindingInProgress = false
            val timerBinder = binder as? TimerService.TimerBinder ?: return
            val service = timerBinder.getService()
            
            timerService = service
            serviceBound = true
            
            // If service has an active session, sync our start time
            if (service.isRunning.value || service.getAccumulatedMs() > 0) {
                if (sessionStartInstant == null) {
                    // Approximate: current time minus elapsed
                    val elapsedMs = service.getAccumulatedMs() + 
                        if (service.isRunning.value) {
                            SystemClock.elapsedRealtime() - service.getStartedAtElapsedRealtime()
                        } else 0L
                    sessionStartInstant = Instant.now().minusMillis(elapsedMs)
                }
                
                hasStartedSaved = true
                _timerState.update { 
                    it.copy(
                        hasStarted = true,
                        startTime = sessionStartInstant
                    ) 
                }
            }
            
            // Observe service state - SERVICE IS SOURCE OF TRUTH
            viewModelScope.launch {
                launch {
                    service.elapsedSeconds.collect { seconds ->
                        _timerState.update { it.copy(elapsedSeconds = seconds) }
                    }
                }
                launch {
                    service.isRunning.collect { running ->
                        _timerState.update { it.copy(isRunning = running) }
                    }
                }
                launch {
                    service.isPausedByInterrupt.collect { paused ->
                        _timerState.update { it.copy(isPausedByInterrupt = paused) }
                    }
                }
                launch {
                    service.interruptReason.collect { reason ->
                        _timerState.update { it.copy(interruptReason = reason) }
                    }
                }
            }
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            timerService = null
            serviceBound = false
            isBindingInProgress = false
        }
    }
    
    init {
        loadBook()
        // Try to bind to existing service (in case returning from notification or process death)
        bindService()
        
        // Sync saved current page to state flow
        viewModelScope.launch {
            savedCurrentPage.collect { page ->
                _timerState.update { it.copy(currentPage = page) }
            }
        }
    }
    
    private fun loadBook() {
        viewModelScope.launch {
            repository.getBookById(bookId).collect { book ->
                _book.value = book
            }
        }
    }
    
    fun startTimer() {
        // If service is already running, don't restart
        if (timerService?.isRunning?.value == true) {
            return
        }
        
        // If we have a session in progress (paused), resume instead
        // This works even before binding completes because the intent goes directly to service
        if (_timerState.value.hasStarted || hasStartedSaved) {
            val intent = Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_RESUME
            }
            context.startService(intent)
            return
        }
        
        // Record wall-clock start time (for saving session later)
        sessionStartInstant = Instant.now()
        hasStartedSaved = true
        
        _timerState.update { 
            it.copy(
                hasStarted = true,
                startTime = sessionStartInstant
            ) 
        }
        
        // Start foreground service - IT manages the timer
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_START
            putExtra(TimerService.EXTRA_BOOK_TITLE, _book.value?.title ?: "Reading Session")
            putExtra(TimerService.EXTRA_BOOK_ID, bookId)
            putExtra(TimerService.EXTRA_INITIAL_PAGE, initialPage)
        }
        context.startForegroundService(intent)
        
        // Bind to observe state
        bindService()
    }
    
    fun pauseTimer() {
        // Tell service to pause - it handles the time tracking
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_PAUSE
        }
        context.startService(intent)
    }
    
    fun resumeTimer() {
        // Tell service to resume
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_RESUME
        }
        context.startService(intent)
    }
    
    fun resetTimer() {
        // Clear persisted state
        sessionStartInstant = null
        hasStartedSaved = false
        savedStateHandle[KEY_USER_CURRENT_PAGE] = initialPage
        
        _timerState.update { 
            TimerState(currentPage = initialPage) 
        }
        
        // Reset service
        timerService?.resetTimer()
    }
    
    private fun bindService() {
        if (!serviceBound && !isBindingInProgress) {
            isBindingInProgress = true
            val intent = Intent(context, TimerService::class.java)
            try {
                context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
            } catch (e: Exception) {
                isBindingInProgress = false
            }
        }
    }
    
    private fun unbindService() {
        if (serviceBound) {
            try {
                context.unbindService(serviceConnection)
            } catch (e: Exception) {
                // Already unbound
            }
            serviceBound = false
            timerService = null
        }
    }
    
    fun updateCurrentPage(page: Int) {
        // Persist to SavedStateHandle - survives process death!
        savedStateHandle[KEY_USER_CURRENT_PAGE] = page
        _timerState.update { it.copy(currentPage = page) }
    }
    
    fun saveSession() {
        viewModelScope.launch {
            val state = _timerState.value
            val book = _book.value
            
            if (book == null) {
                _events.emit(TimerEvent.Error("Book not found"))
                return@launch
            }
            
            // Get elapsed time from service (source of truth)
            val finalElapsedSeconds = timerService?.elapsedSeconds?.value ?: state.elapsedSeconds
            
            // Validate minimum duration (1 minute)
            if (finalElapsedSeconds < MIN_SESSION_DURATION_SECONDS) {
                _events.emit(TimerEvent.Error("Please read for at least 1 minute"))
                return@launch
            }
            
            // Validate pages
            val pagesRead = state.currentPage - initialPage
            if (pagesRead < 0) {
                _events.emit(TimerEvent.Error("Current page cannot be less than starting page"))
                return@launch
            }
            
            if (state.currentPage > book.totalPages) {
                _events.emit(TimerEvent.Error("Current page cannot exceed total pages"))
                return@launch
            }
            
            // Warn if no pages were read
            if (pagesRead == 0 && !state.confirmedZeroPages) {
                _events.emit(TimerEvent.ConfirmZeroPages)
                return@launch
            }
            
            performSaveSession(state)
        }
    }
    
    fun confirmSaveWithZeroPages() {
        _timerState.update { it.copy(confirmedZeroPages = true) }
        saveSession()
    }
    
    private suspend fun performSaveSession(state: TimerState) {
        val endTime = Instant.now()
        val session = ReadingSession.fromTimer(
            bookId = bookId,
            startTime = sessionStartInstant ?: endTime,
            endTime = endTime,
            startPage = initialPage,
            endPage = state.currentPage
        )
        
        repository.insertSession(session)
        
        // Clear persisted state
        clearPersistedState()
        
        // Stop service
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
        unbindService()
        
        _events.emit(TimerEvent.SessionSaved)
    }
    
    fun cancel() {
        // Clear persisted state
        clearPersistedState()
        
        // Stop service
        val intent = Intent(context, TimerService::class.java).apply {
            action = TimerService.ACTION_STOP
        }
        context.startService(intent)
        unbindService()
        
        viewModelScope.launch {
            _events.emit(TimerEvent.Cancelled)
        }
    }
    
    private fun clearPersistedState() {
        savedStateHandle.remove<Long>(KEY_SESSION_START_MS)
        savedStateHandle.remove<Boolean>(KEY_HAS_STARTED)
        savedStateHandle.remove<Int>(KEY_USER_CURRENT_PAGE)
    }
    
    override fun onCleared() {
        super.onCleared()
        // Only unbind if timer is not running and no active session
        // This prevents memory leaks while preserving timer functionality
        if (!_timerState.value.isRunning && !_timerState.value.hasStarted) {
            unbindService()
        }
    }
}

/**
 * State for the timer screen.
 */
data class TimerState(
    val elapsedSeconds: Int = 0,
    val isRunning: Boolean = false,
    val hasStarted: Boolean = false,
    val currentPage: Int = 0,
    val startTime: Instant? = null,
    val isPausedByInterrupt: Boolean = false,
    val interruptReason: String? = null,
    val confirmedZeroPages: Boolean = false
) {
    val formattedTime: String
        get() {
            val hours = elapsedSeconds / 3600
            val minutes = (elapsedSeconds % 3600) / 60
            val seconds = elapsedSeconds % 60
            
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }
    
    val elapsedMinutes: Int
        get() = elapsedSeconds / 60
}

/**
 * One-time events from the timer screen.
 */
sealed interface TimerEvent {
    data object SessionSaved : TimerEvent
    data object Cancelled : TimerEvent
    data object ConfirmZeroPages : TimerEvent
    data class Error(val message: String) : TimerEvent
}
