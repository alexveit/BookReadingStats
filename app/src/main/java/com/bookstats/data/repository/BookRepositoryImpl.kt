package com.bookstats.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.bookstats.data.local.database.BookDatabase
import com.bookstats.data.local.database.dao.BookDao
import com.bookstats.data.local.database.dao.CategoryDao
import com.bookstats.data.local.database.dao.ReadingSessionDao
import com.bookstats.data.local.database.entity.BookCategoryEntity
import com.bookstats.data.local.database.entity.BookEntity
import com.bookstats.data.local.database.entity.CategoryEntity
import com.bookstats.data.local.database.entity.ReadingSessionEntity
import com.bookstats.data.local.database.entity.SyncStatus
import com.bookstats.data.remote.SupabaseDataSource
import com.bookstats.data.remote.dto.*
import com.bookstats.data.remote.dto.TimestampConverter.toInstant
import com.bookstats.data.remote.dto.TimestampConverter.toIsoString
import com.bookstats.data.sync.SyncManager
import com.bookstats.data.sync.SyncState
import com.bookstats.data.sync.SyncWorker
import com.bookstats.domain.model.Book
import com.bookstats.domain.model.Category
import com.bookstats.domain.model.CategoryStatistics
import com.bookstats.domain.model.DailyChartData
import com.bookstats.domain.model.ReadingSession
import com.bookstats.domain.model.ReadingStatistics
import com.bookstats.domain.repository.BookRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository implementation with offline-first sync strategy.
 * 
 * Strategy:
 * - Always write to local Room first
 * - Try to sync to Supabase immediately if online
 * - If offline, mark as pending sync
 * - Background worker syncs pending items when online
 */
@Singleton
class BookRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BookDatabase,
    private val bookDao: BookDao,
    private val sessionDao: ReadingSessionDao,
    private val categoryDao: CategoryDao,
    private val remoteDataSource: SupabaseDataSource,
    private val authRepository: AuthRepository,
    private val syncManager: SyncManager
) : BookRepository {
    
    // ============================================
    // SYNC STATE
    // ============================================
    
    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    override val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    
    override suspend fun refreshSync() {
        if (!syncManager.isOnline()) {
            _syncState.value = SyncState.Offline
            return
        }
        
        if (!authRepository.isAuthenticated()) {
            _syncState.value = SyncState.Idle
            return
        }
        
        _syncState.value = SyncState.Syncing

        try {
            val userId = authRepository.getCurrentUserId()
                ?: throw IllegalStateException("Not authenticated")

            // Pull from remote (with user_id filtering for defense-in-depth)
            val remoteBooks = remoteDataSource.getAllBooks(userId)
            val remoteSessions = remoteDataSource.getAllSessions(userId)
            syncFromRemote(remoteBooks, remoteSessions)
            
            // Push pending local changes
            val success = syncManager.syncAll()
            
            _syncState.value = if (success) SyncState.Success else SyncState.Error("Sync failed")
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Unknown sync error")
        }
    }
    
    // ============================================
    // BOOKS
    // ============================================
    
    override fun getAllBooks(): Flow<List<Book>> {
        // Use combine for reactive updates when database changes
        return combine(
            bookDao.getAllBooks(),
            sessionDao.getAllSessions(),
            categoryDao.getAllCategories()
        ) { books, sessions, _ ->
            val sessionsByBook = sessions.groupBy { it.bookId }
            books.map { book ->
                val categories = categoryDao.getCategoriesForBookList(book.id)
                book.toBook(sessionsByBook[book.id] ?: emptyList(), categories)
            }
        }.onStart {
            // Try to sync from remote on first collection
            val userId = authRepository.getCurrentUserId()
            if (userId != null && syncManager.isOnline()) {
                try {
                    val remoteBooks = remoteDataSource.getAllBooks(userId)
                    val remoteSessions = remoteDataSource.getAllSessions(userId)
                    syncFromRemote(remoteBooks, remoteSessions)
                } catch (e: Exception) {
                    // Continue with cached data
                }
            }
        }.flowOn(Dispatchers.IO)
    }
    
    override fun getBookById(bookId: Long): Flow<Book?> {
        return combine(
            bookDao.getBookByIdFlow(bookId),
            sessionDao.getSessionsForBook(bookId),
            categoryDao.getCategoriesForBook(bookId)
        ) { book, sessions, categories ->
            book?.toBook(sessions, categories)
        }.flowOn(Dispatchers.IO)
    }

    override fun searchBooks(query: String): Flow<List<Book>> {
        return combine(
            bookDao.searchBooks(query),
            sessionDao.getAllSessions(),
            categoryDao.getAllCategories()
        ) { books, sessions, _ ->
            val sessionsByBook = sessions.groupBy { it.bookId }
            books.map { book ->
                val categories = categoryDao.getCategoriesForBookList(book.id)
                book.toBook(sessionsByBook[book.id] ?: emptyList(), categories)
            }
        }.flowOn(Dispatchers.IO)
    }
    
    override suspend fun insertBook(book: Book): Long = withContext(Dispatchers.IO) {
        // Always save locally first
        val localEntity = BookEntity(
            title = book.title,
            author = book.author,
            category = book.categoriesFormatted, // Keep for backwards compatibility
            totalPages = book.totalPages,
            notes = book.notes,
            coverImageUrl = book.coverImageUrl,
            createdAt = Instant.now(),
            updatedAt = Instant.now(),
            syncStatus = SyncStatus.PENDING_CREATE
        )
        val localId = bookDao.insertBook(localEntity)

        // Save categories
        if (book.categories.isNotEmpty()) {
            val categoryIds = book.categories.map { it.id }
            val bookCategories = categoryIds.map { categoryId ->
                BookCategoryEntity(bookId = localId, categoryId = categoryId)
            }
            categoryDao.insertBookCategories(bookCategories)
        }

        // Try to sync immediately if online
        val userId = authRepository.getCurrentUserId()
        if (userId != null && syncManager.isOnline()) {
            try {
                val insertDto = BookInsertDto(
                    userId = userId,
                    title = book.title,
                    author = book.author,
                    category = book.categoriesFormatted,
                    totalPages = book.totalPages,
                    notes = book.notes,
                    coverImageUrl = book.coverImageUrl
                )
                val remoteBook = remoteDataSource.insertBook(insertDto)
                val remoteId = requireNotNull(remoteBook.id) {
                    "Supabase insert returned book without ID"
                }
                bookDao.markAsSynced(localId, remoteId)
            } catch (e: Exception) {
                // Stay as PENDING_CREATE, will sync later
                scheduleSync()
            }
        } else {
            // Schedule sync for when we're back online
            scheduleSync()
        }

        localId
    }
    
    override suspend fun updateBook(book: Book) = withContext(Dispatchers.IO) {
        val existingBook = bookDao.getBookById(book.id) ?: return@withContext

        // Update locally
        val updatedEntity = existingBook.copy(
            title = book.title,
            author = book.author,
            category = book.categoriesFormatted, // Keep for backwards compatibility
            totalPages = book.totalPages,
            notes = book.notes,
            coverImageUrl = book.coverImageUrl,
            updatedAt = Instant.now(),
            syncStatus = if (existingBook.syncStatus == SyncStatus.PENDING_CREATE) {
                SyncStatus.PENDING_CREATE // Keep as pending create
            } else {
                SyncStatus.PENDING_UPDATE
            }
        )
        bookDao.updateBook(updatedEntity)

        // Update categories - clear existing and add new ones
        categoryDao.deleteBookCategories(book.id)
        if (book.categories.isNotEmpty()) {
            val bookCategories = book.categories.map { category ->
                BookCategoryEntity(bookId = book.id, categoryId = category.id)
            }
            categoryDao.insertBookCategories(bookCategories)
        }

        // Try to sync immediately if online and already synced before
        val userId = authRepository.getCurrentUserId()
        if (userId != null && existingBook.remoteId != null && syncManager.isOnline()) {
            try {
                val updateDto = BookUpdateDto(
                    title = book.title,
                    author = book.author,
                    category = book.categoriesFormatted,
                    totalPages = book.totalPages,
                    notes = book.notes,
                    coverImageUrl = book.coverImageUrl
                )
                remoteDataSource.updateBook(userId, existingBook.remoteId, updateDto)
                bookDao.updateSyncStatus(book.id, SyncStatus.SYNCED)
            } catch (e: Exception) {
                scheduleSync()
            }
        } else {
            scheduleSync()
        }
    }
    
    override suspend fun deleteBook(bookId: Long) = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(bookId) ?: return@withContext

        // If never synced, just delete locally (atomically)
        if (book.syncStatus == SyncStatus.PENDING_CREATE) {
            database.withTransaction {
                sessionDao.deleteSessionsForBook(bookId)
                bookDao.deleteBookById(bookId)
            }
            return@withContext
        }

        // Mark sessions and book for deletion (atomically)
        database.withTransaction {
            val sessions = sessionDao.getSessionsForBook(bookId).first()
            sessions.forEach { session ->
                if (session.syncStatus == SyncStatus.PENDING_CREATE) {
                    sessionDao.deleteSessionById(session.id)
                } else {
                    sessionDao.updateSyncStatus(session.id, SyncStatus.PENDING_DELETE)
                }
            }
            bookDao.updateSyncStatus(bookId, SyncStatus.PENDING_DELETE)
        }

        // Try to sync immediately
        val userId = authRepository.getCurrentUserId()
        if (userId != null && book.remoteId != null && syncManager.isOnline()) {
            try {
                remoteDataSource.deleteBook(userId, book.remoteId)
                // Delete locally after successful remote delete (atomically)
                database.withTransaction {
                    sessionDao.deleteSessionsForBook(bookId)
                    bookDao.deleteBookById(bookId)
                }
            } catch (e: Exception) {
                scheduleSync()
            }
        } else {
            scheduleSync()
        }
    }
    
    // ============================================
    // READING SESSIONS
    // ============================================
    
    override fun getSessionsForBook(bookId: Long): Flow<List<ReadingSession>> {
        return sessionDao.getSessionsForBook(bookId).map { sessions ->
            sessions.map { it.toReadingSession() }
        }
    }
    
    override suspend fun insertSession(session: ReadingSession): Long = withContext(Dispatchers.IO) {
        val book = bookDao.getBookById(session.bookId) ?: return@withContext -1L
        // Calculate duration directly from timestamps for accuracy
        val durationSeconds = ((session.endTime.toEpochMilli() - session.startTime.toEpochMilli()) / 1000).toInt()
        
        // Always save locally first
        val localEntity = ReadingSessionEntity(
            bookId = session.bookId,
            startTime = session.startTime,
            endTime = session.endTime,
            durationSeconds = durationSeconds,
            startPage = session.startPage,
            endPage = session.endPage,
            syncStatus = SyncStatus.PENDING_CREATE
        )
        val localId = sessionDao.insertSession(localEntity)
        
        // Try to sync immediately if book is already synced and we're online
        val userId = authRepository.getCurrentUserId()
        if (userId != null && book.remoteId != null && syncManager.isOnline()) {
            try {
                val insertDto = ReadingSessionInsertDto(
                    userId = userId,
                    bookId = book.remoteId,
                    startTime = session.startTime.toIsoString(),
                    endTime = session.endTime.toIsoString(),
                    startPage = session.startPage,
                    endPage = session.endPage
                )
                val remoteSession = remoteDataSource.insertSession(insertDto)
                val remoteId = requireNotNull(remoteSession.id) {
                    "Supabase insert returned session without ID"
                }
                sessionDao.markAsSynced(localId, remoteId)
            } catch (e: Exception) {
                scheduleSync()
            }
        } else {
            scheduleSync()
        }
        
        localId
    }
    
    override suspend fun updateSession(session: ReadingSession) = withContext(Dispatchers.IO) {
        val localSession = sessionDao.getSessionById(session.id) ?: return@withContext
        // Calculate duration directly from timestamps for accuracy
        val durationSeconds = ((session.endTime.toEpochMilli() - session.startTime.toEpochMilli()) / 1000).toInt()
        
        val updatedEntity = localSession.copy(
            startPage = session.startPage,
            endPage = session.endPage,
            durationSeconds = durationSeconds,
            syncStatus = if (localSession.syncStatus == SyncStatus.PENDING_CREATE) {
                SyncStatus.PENDING_CREATE
            } else {
                SyncStatus.PENDING_UPDATE
            }
        )
        sessionDao.updateSession(updatedEntity)
        
        // Try to sync if already synced before
        val userId = authRepository.getCurrentUserId()
        if (userId != null && localSession.remoteId != null && syncManager.isOnline()) {
            try {
                val updateDto = ReadingSessionUpdateDto(
                    startPage = session.startPage,
                    endPage = session.endPage
                )
                remoteDataSource.updateSession(userId, localSession.remoteId, updateDto)
                sessionDao.updateSyncStatus(session.id, SyncStatus.SYNCED)
            } catch (e: Exception) {
                scheduleSync()
            }
        } else {
            scheduleSync()
        }
    }
    
    override suspend fun deleteSession(sessionId: Long) = withContext(Dispatchers.IO) {
        val session = sessionDao.getSessionById(sessionId) ?: return@withContext
        
        // If never synced, just delete locally
        if (session.syncStatus == SyncStatus.PENDING_CREATE) {
            sessionDao.deleteSessionById(sessionId)
            return@withContext
        }
        
        // Mark for deletion
        sessionDao.updateSyncStatus(sessionId, SyncStatus.PENDING_DELETE)

        // Try to sync immediately
        val userId = authRepository.getCurrentUserId()
        if (userId != null && session.remoteId != null && syncManager.isOnline()) {
            try {
                remoteDataSource.deleteSession(userId, session.remoteId)
                sessionDao.deleteSessionById(sessionId)
            } catch (e: Exception) {
                scheduleSync()
            }
        } else {
            scheduleSync()
        }
    }
    
    // ============================================
    // STATISTICS
    // ============================================
    
    override fun getReadingStatistics(): Flow<ReadingStatistics> = combine(
        bookDao.getAllBooks(),
        sessionDao.getAllSessions()
    ) { books, sessions ->
        val sessionsByBook = sessions.groupBy { it.bookId }
        val booksWithSessions = books.map { it.toBook(sessionsByBook[it.id] ?: emptyList()) }
        
        ReadingStatistics(
            totalBooks = booksWithSessions.size,
            booksFinished = booksWithSessions.count { it.isFinished },
            booksInProgress = booksWithSessions.count { it.isStarted && !it.isFinished },
            booksNotStarted = booksWithSessions.count { !it.isStarted },
            totalPagesRead = booksWithSessions.sumOf { it.pagesRead },
            totalPagesAllBooks = booksWithSessions.sumOf { it.totalPages },
            totalMinutesRead = booksWithSessions.sumOf { it.totalMinutesRead },
            totalSessions = sessions.size
        )
    }
    
    override fun getDailyChartData(): Flow<List<DailyChartData>> {
        return sessionDao.getAllSessions().map { sessions ->
            sessions.groupBy { session ->
                LocalDate.ofInstant(session.startTime, ZoneId.systemDefault())
            }.toSortedMap().map { (date, daySessions) ->
                DailyChartData(
                    date = date.format(DateTimeFormatter.ofPattern("MM/dd")),
                    pagesRead = daySessions.sumOf { it.endPage - it.startPage },
                    minutesRead = daySessions.sumOf { it.durationSeconds / 60 }
                )
            }
        }
    }

    override fun getDailyChartDataForBook(bookId: Long): Flow<List<DailyChartData>> {
        return sessionDao.getSessionsForBook(bookId).map { sessions ->
            sessions.groupBy { session ->
                LocalDate.ofInstant(session.startTime, ZoneId.systemDefault())
            }.toSortedMap().map { (date, daySessions) ->
                DailyChartData(
                    date = date.format(DateTimeFormatter.ofPattern("MM/dd")),
                    pagesRead = daySessions.sumOf { it.endPage - it.startPage },
                    minutesRead = daySessions.sumOf { it.durationSeconds / 60 }
                )
            }
        }
    }

    override fun getCategoryStatistics(): Flow<List<CategoryStatistics>> {
        return combine(
            bookDao.getAllBooks(),
            sessionDao.getAllSessions(),
            categoryDao.getAllCategories()
        ) { books, sessions, categories ->
            val sessionsByBook = sessions.groupBy { it.bookId }
            val booksWithSessions = books.map { book ->
                val bookCategories = categoryDao.getCategoriesForBookList(book.id)
                book.toBook(sessionsByBook[book.id] ?: emptyList(), bookCategories)
            }

            categories.map { categoryEntity ->
                val category = Category(id = categoryEntity.id, name = categoryEntity.name)
                val booksInCategory = booksWithSessions.filter { book ->
                    book.categories.any { it.id == categoryEntity.id }
                }
                CategoryStatistics(
                    category = category,
                    bookCount = booksInCategory.size,
                    totalPagesRead = booksInCategory.sumOf { it.pagesRead },
                    totalMinutesRead = booksInCategory.sumOf { it.totalMinutesRead },
                    pagesRemaining = booksInCategory.sumOf { it.pagesRemaining },
                    sessionCount = booksInCategory.sumOf { it.sessions.size }
                )
            }.sortedByDescending { it.totalMinutesRead }
        }
    }

    // ============================================
    // CATEGORIES
    // ============================================

    override fun getAllCategories(): Flow<List<Category>> {
        return categoryDao.getAllCategories().map { categories ->
            categories.map { Category(id = it.id, name = it.name) }
        }
    }

    override suspend fun getOrCreateCategories(names: List<String>): List<Category> = withContext(Dispatchers.IO) {
        names.map { name ->
            val trimmedName = name.trim()
            val existing = categoryDao.getCategoryByName(trimmedName)
            if (existing != null) {
                Category(id = existing.id, name = existing.name)
            } else {
                val id = categoryDao.insertCategory(CategoryEntity(name = trimmedName))
                Category(id = id, name = trimmedName)
            }
        }
    }

    override suspend fun insertCategory(name: String): Long = withContext(Dispatchers.IO) {
        val trimmedName = name.trim()
        val existing = categoryDao.getCategoryByName(trimmedName)
        existing?.id ?: categoryDao.insertCategory(CategoryEntity(name = trimmedName))
    }

    override suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        categoryDao.deleteCategoryById(categoryId)
    }

    override suspend fun updateBookCategories(bookId: Long, categoryIds: List<Long>) = withContext(Dispatchers.IO) {
        categoryDao.deleteBookCategories(bookId)
        val bookCategories = categoryIds.map { categoryId ->
            BookCategoryEntity(bookId = bookId, categoryId = categoryId)
        }
        categoryDao.insertBookCategories(bookCategories)
    }

    // ============================================
    // MERGE BOOKS
    // ============================================
    
    override suspend fun mergeBooks(targetBookId: Long, sourceBookIds: List<Long>) = withContext(Dispatchers.IO) {
        sourceBookIds.forEach { sourceBookId ->
            val sessions = sessionDao.getSessionsForBook(sourceBookId).first()
            sessions.forEach { session ->
                val updatedSession = session.copy(
                    bookId = targetBookId,
                    syncStatus = SyncStatus.PENDING_UPDATE
                )
                sessionDao.updateSession(updatedSession)
            }
            bookDao.updateSyncStatus(sourceBookId, SyncStatus.PENDING_DELETE)
        }
        scheduleSync()
    }
    
    // ============================================
    // EXPORT / IMPORT
    // ============================================
    
    override suspend fun exportToJson(): String = withContext(Dispatchers.IO) {
        val books = bookDao.getAllBooks().first()
        val sessions = sessionDao.getAllSessions().first()
        
        val exportData = mapOf(
            "books" to books.map { book ->
                mapOf(
                    "id" to book.id,
                    "title" to book.title,
                    "author" to book.author,
                    "category" to book.category,
                    "totalPages" to book.totalPages,
                    "notes" to book.notes,
                    "coverImageUrl" to book.coverImageUrl,
                    "createdAt" to book.createdAt.toString(),
                    "updatedAt" to book.updatedAt.toString()
                )
            },
            "sessions" to sessions.map { session ->
                mapOf(
                    "id" to session.id,
                    "bookId" to session.bookId,
                    "startTime" to session.startTime.toString(),
                    "endTime" to session.endTime.toString(),
                    "durationSeconds" to session.durationSeconds,
                    "startPage" to session.startPage,
                    "endPage" to session.endPage
                )
            }
        )
        
        Json.encodeToString(exportData)
    }
    
    override suspend fun importFromJson(json: String): Result<Int> = withContext(Dispatchers.IO) {
        try {
            Result.success(0)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    // ============================================
    // SYNC HELPERS
    // ============================================
    
    private fun scheduleSync() {
        SyncWorker.scheduleImmediateSync(context)
    }
    
    /**
     * Sync data from remote to local (for initial load / refresh).
     */
    private suspend fun syncFromRemote(
        remoteBooks: List<BookDto>,
        remoteSessions: List<ReadingSessionDto>
    ) {
        val sessionsByBook = remoteSessions.groupBy { it.bookId }
        
        remoteBooks.forEach { remoteBook ->
            val bookRemoteId = remoteBook.id ?: return@forEach // Skip entries without ID
            val existingLocal = bookDao.getBookByRemoteId(bookRemoteId)
            
            val localId = if (existingLocal != null) {
                // Only update if local is synced (not pending changes)
                if (existingLocal.syncStatus == SyncStatus.SYNCED) {
                    val updated = remoteBook.toLocalEntity(existingLocal.id)
                    bookDao.updateBook(updated)
                }
                existingLocal.id
            } else {
                val newEntity = remoteBook.toLocalEntity()
                bookDao.insertBook(newEntity)
            }
            
            // Sync sessions for this book (only if no pending local changes)
            val bookSessions = sessionsByBook[bookRemoteId] ?: emptyList()
            bookSessions.forEach { sessionDto ->
                val sessionRemoteId = sessionDto.id ?: return@forEach // Skip entries without ID
                val existingSession = sessionDao.getSessionByRemoteId(sessionRemoteId)
                if (existingSession == null) {
                    sessionDao.insertSession(sessionDto.toLocalEntity(localId))
                } else if (existingSession.syncStatus == SyncStatus.SYNCED) {
                    val updated = sessionDto.toLocalEntity(localId).copy(id = existingSession.id)
                    sessionDao.updateSession(updated)
                }
            }
        }
    }
    
    // ============================================
    // EXTENSION FUNCTIONS FOR MAPPING
    // ============================================
    
    private fun BookDto.toLocalEntity(localId: Long = 0L) = BookEntity(
        id = localId,
        remoteId = this.id,
        title = this.title,
        author = this.author,
        category = this.category,
        totalPages = this.totalPages,
        notes = this.notes,
        coverImageUrl = this.coverImageUrl,
        createdAt = this.createdAt?.toInstant() ?: Instant.now(),
        updatedAt = this.updatedAt?.toInstant() ?: Instant.now(),
        syncStatus = SyncStatus.SYNCED
    )
    
    private fun ReadingSessionDto.toLocalEntity(localBookId: Long) = ReadingSessionEntity(
        id = 0L,
        remoteId = this.id,
        bookId = localBookId,
        startTime = this.startTime.toInstant(),
        endTime = this.endTime.toInstant(),
        durationSeconds = this.durationSeconds,
        startPage = this.startPage,
        endPage = this.endPage,
        syncStatus = SyncStatus.SYNCED
    )
    
    private fun BookEntity.toBook(
        sessions: List<ReadingSessionEntity>,
        categories: List<CategoryEntity> = emptyList()
    ) = Book(
        id = this.id,
        title = this.title,
        author = this.author,
        categories = categories.map { Category(id = it.id, name = it.name) },
        totalPages = this.totalPages,
        notes = this.notes,
        coverImageUrl = this.coverImageUrl,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        sessions = sessions.map { it.toReadingSession() }
    )
    
    private fun ReadingSessionEntity.toReadingSession() = ReadingSession(
        id = this.id,
        bookId = this.bookId,
        startTime = this.startTime,
        endTime = this.endTime,
        durationMinutes = this.durationSeconds / 60,
        pagesRead = this.endPage - this.startPage,
        startPage = this.startPage,
        endPage = this.endPage
    )
}
