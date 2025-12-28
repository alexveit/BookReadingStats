package com.bookstats

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bookstats.service.TimerService
import com.bookstats.ui.navigation.BookStatsNavGraph
import com.bookstats.ui.theme.BookStatsTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main entry point for the Book Reading Stats app.
 * Single Activity architecture with Jetpack Compose navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    // Navigation state from notification
    private var navigateToTimerBookId by mutableStateOf<Long?>(null)
    private var navigateToTimerPage by mutableStateOf<Int?>(null)
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if launched from notification
        handleIntent(intent)
        
        // Enable edge-to-edge display
        enableEdgeToEdge()
        
        setContent {
            BookStatsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    BookStatsNavGraph(
                        navigateToTimerBookId = navigateToTimerBookId,
                        navigateToTimerPage = navigateToTimerPage,
                        onTimerNavigationHandled = {
                            navigateToTimerBookId = null
                            navigateToTimerPage = null
                        }
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(TimerService.EXTRA_NAVIGATE_TO_TIMER, false) == true) {
            val bookId = intent.getLongExtra(TimerService.EXTRA_BOOK_ID, -1L)
            val page = intent.getIntExtra(TimerService.EXTRA_INITIAL_PAGE, 0)
            if (bookId != -1L) {
                navigateToTimerBookId = bookId
                navigateToTimerPage = page
            }
        }
    }
}
