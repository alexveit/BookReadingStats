package com.bookstats.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.bookstats.data.repository.AuthState
import com.bookstats.ui.screens.auth.AuthScreen
import com.bookstats.ui.screens.auth.AuthViewModel
import com.bookstats.ui.screens.bookdetail.BookDetailScreen
import com.bookstats.ui.screens.books.AddBookScreen
import com.bookstats.ui.screens.books.BooksScreen
import com.bookstats.ui.screens.chart.ChartScreen
import com.bookstats.ui.screens.sessions.SessionsScreen
import com.bookstats.ui.screens.statistics.StatisticsScreen
import com.bookstats.ui.screens.timer.TimerScreen

/**
 * Navigation routes.
 */
object Routes {
    const val AUTH = "auth"
    const val BOOKS = "books"
    const val ADD_BOOK = "add_book"
    const val BOOK_DETAIL = "book_detail/{bookId}"
    const val TIMER = "timer/{bookId}/{currentPage}"
    const val SESSIONS = "sessions/{bookId}"
    const val CHART = "chart/{bookId}"
    const val STATISTICS = "statistics"
    
    fun bookDetail(bookId: Long) = "book_detail/$bookId"
    fun timer(bookId: Long, currentPage: Int) = "timer/$bookId/$currentPage"
    fun sessions(bookId: Long) = "sessions/$bookId"
    fun chart(bookId: Long) = "chart/$bookId"
}

/**
 * Main navigation graph with authentication flow.
 */
@Composable
fun BookStatsNavGraph(
    navigateToTimerBookId: Long? = null,
    navigateToTimerPage: Int? = null,
    onTimerNavigationHandled: () -> Unit = {},
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    // Track if we've handled initial auth redirect
    var hasHandledAuthRedirect by rememberSaveable { mutableStateOf(false) }
    
    // Track pending timer navigation (survives recomposition)
    var pendingTimerBookId by rememberSaveable { mutableStateOf<Long?>(null) }
    var pendingTimerPage by rememberSaveable { mutableStateOf<Int?>(null) }
    
    // Capture incoming navigation request
    LaunchedEffect(navigateToTimerBookId, navigateToTimerPage) {
        if (navigateToTimerBookId != null && navigateToTimerPage != null) {
            pendingTimerBookId = navigateToTimerBookId
            pendingTimerPage = navigateToTimerPage
            onTimerNavigationHandled()
        }
    }
    
    // Handle auth state changes and pending navigation
    LaunchedEffect(authState, pendingTimerBookId, pendingTimerPage) {
        when (authState) {
            is AuthState.Authenticated -> {
                // Redirect from auth to books if needed
                if (!hasHandledAuthRedirect) {
                    val currentRoute = navController.currentDestination?.route
                    if (currentRoute == Routes.AUTH) {
                        navController.navigate(Routes.BOOKS) {
                            popUpTo(0) { inclusive = true }  // Clear entire backstack
                            launchSingleTop = true
                        }
                    }
                    hasHandledAuthRedirect = true
                }
                
                // Handle pending timer navigation
                val timerBookId = pendingTimerBookId
                val timerPage = pendingTimerPage
                if (timerBookId != null && timerPage != null) {
                    val bookId = timerBookId
                    val page = timerPage
                    
                    // Clear pending state first to prevent re-navigation
                    pendingTimerBookId = null
                    pendingTimerPage = null
                    
                    // Navigate to timer
                    navController.navigate(Routes.timer(bookId, page)) {
                        launchSingleTop = true
                    }
                }
            }
            is AuthState.NotAuthenticated -> {
                hasHandledAuthRedirect = false
                pendingTimerBookId = null
                pendingTimerPage = null
            }
            else -> {
                // Loading - wait for auth to resolve
            }
        }
    }
    
    // Use BOOKS as default start - prevents NavHost recreation when auth changes
    NavHost(
        navController = navController,
        startDestination = Routes.BOOKS
    ) {
        // Auth screen
        composable(Routes.AUTH) {
            AuthScreen(
                onNavigateToHome = {
                    navController.navigate(Routes.BOOKS) {
                        popUpTo(0) { inclusive = true }  // Clear entire backstack
                        launchSingleTop = true
                    }
                }
            )
        }
        
        // Books list - handles auth check internally
        composable(Routes.BOOKS) {
            val currentAuthState = authState
            if (currentAuthState is AuthState.NotAuthenticated) {
                LaunchedEffect(Unit) {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            } else {
                BooksScreen(
                    onNavigateToTimer = { bookId, currentPage ->
                        navController.navigate(Routes.timer(bookId, currentPage))
                    },
                    onNavigateToSessions = { bookId ->
                        navController.navigate(Routes.sessions(bookId))
                    },
                    onNavigateToBook = { bookId -> 
                        navController.navigate(Routes.bookDetail(bookId)) 
                    },
                    onNavigateToAddBook = { navController.navigate(Routes.ADD_BOOK) },
                    onNavigateToStatistics = { navController.navigate(Routes.STATISTICS) }
                )
            }
        }
        
        // Add book
        composable(Routes.ADD_BOOK) {
            AddBookScreen(
                onNavigateBack = { navController.popBackStack() },
                onBookAdded = { bookId ->
                    navController.navigate(Routes.bookDetail(bookId)) {
                        popUpTo(Routes.BOOKS)
                    }
                }
            )
        }
        
        // Book detail
        composable(
            route = Routes.BOOK_DETAIL,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) {
            BookDetailScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToTimer = { bookId, currentPage ->
                    navController.navigate(Routes.timer(bookId, currentPage))
                },
                onNavigateToSessions = { bookId ->
                    navController.navigate(Routes.sessions(bookId))
                },
                onNavigateToChart = { bookId ->
                    navController.navigate(Routes.chart(bookId))
                }
            )
        }
        
        // Timer
        composable(
            route = Routes.TIMER,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType },
                navArgument("currentPage") { type = NavType.IntType }
            )
        ) { TimerScreen(onNavigateBack = { navController.popBackStack() }) }
        
        // Sessions
        composable(
            route = Routes.SESSIONS,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { SessionsScreen(onNavigateBack = { navController.popBackStack() }) }
        
        // Chart
        composable(
            route = Routes.CHART,
            arguments = listOf(navArgument("bookId") { type = NavType.LongType })
        ) { ChartScreen(onNavigateBack = { navController.popBackStack() }) }
        
        // Statistics
        composable(Routes.STATISTICS) { StatisticsScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
