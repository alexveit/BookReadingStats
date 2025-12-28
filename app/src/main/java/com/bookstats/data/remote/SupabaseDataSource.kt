package com.bookstats.data.remote

import com.bookstats.data.remote.dto.*
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Remote data source for Supabase operations.
 *
 * All queries include user_id filtering for defense-in-depth security.
 * This provides client-side protection in addition to Supabase RLS policies.
 */
@Singleton
class SupabaseDataSource @Inject constructor() {

    private val postgrest = SupabaseConfig.client.postgrest

    // ============================================
    // BOOKS
    // ============================================

    /**
     * Get all books for the specified user.
     * @param userId The authenticated user's ID (required for security)
     */
    suspend fun getAllBooks(userId: String): List<BookDto> {
        return postgrest.from("books")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<BookDto>()
    }

    /**
     * Get a single book by ID, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param bookId The book's remote ID
     */
    suspend fun getBookById(userId: String, bookId: String): BookDto? {
        return postgrest.from("books")
            .select {
                filter {
                    eq("id", bookId)
                    eq("user_id", userId)
                }
            }
            .decodeSingleOrNull<BookDto>()
    }

    /**
     * Insert a new book.
     */
    suspend fun insertBook(book: BookInsertDto): BookDto {
        return postgrest.from("books")
            .insert(book) {
                select()
            }
            .decodeSingle<BookDto>()
    }

    /**
     * Update an existing book, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param bookId The book's remote ID
     * @param update The fields to update
     */
    suspend fun updateBook(userId: String, bookId: String, update: BookUpdateDto): BookDto {
        return postgrest.from("books")
            .update(update) {
                filter {
                    eq("id", bookId)
                    eq("user_id", userId)
                }
                select()
            }
            .decodeSingle<BookDto>()
    }

    /**
     * Delete a book, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param bookId The book's remote ID
     */
    suspend fun deleteBook(userId: String, bookId: String) {
        postgrest.from("books")
            .delete {
                filter {
                    eq("id", bookId)
                    eq("user_id", userId)
                }
            }
    }

    // ============================================
    // READING SESSIONS
    // ============================================

    /**
     * Get all sessions for a book, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param bookId The book's remote ID
     */
    suspend fun getSessionsForBook(userId: String, bookId: String): List<ReadingSessionDto> {
        return postgrest.from("reading_sessions")
            .select {
                filter {
                    eq("book_id", bookId)
                    eq("user_id", userId)
                }
                order("start_time", Order.DESCENDING)
            }
            .decodeList<ReadingSessionDto>()
    }

    /**
     * Get all sessions for the specified user.
     * @param userId The authenticated user's ID
     */
    suspend fun getAllSessions(userId: String): List<ReadingSessionDto> {
        return postgrest.from("reading_sessions")
            .select {
                filter {
                    eq("user_id", userId)
                }
                order("start_time", Order.DESCENDING)
            }
            .decodeList<ReadingSessionDto>()
    }

    /**
     * Insert a new reading session.
     */
    suspend fun insertSession(session: ReadingSessionInsertDto): ReadingSessionDto {
        return postgrest.from("reading_sessions")
            .insert(session) {
                select()
            }
            .decodeSingle<ReadingSessionDto>()
    }

    /**
     * Update an existing session, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param sessionId The session's remote ID
     * @param update The fields to update
     */
    suspend fun updateSession(userId: String, sessionId: String, update: ReadingSessionUpdateDto): ReadingSessionDto {
        return postgrest.from("reading_sessions")
            .update(update) {
                filter {
                    eq("id", sessionId)
                    eq("user_id", userId)
                }
                select()
            }
            .decodeSingle<ReadingSessionDto>()
    }

    /**
     * Delete a session, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param sessionId The session's remote ID
     */
    suspend fun deleteSession(userId: String, sessionId: String) {
        postgrest.from("reading_sessions")
            .delete {
                filter {
                    eq("id", sessionId)
                    eq("user_id", userId)
                }
            }
    }

    /**
     * Delete all sessions for a book, verified to belong to the user.
     * @param userId The authenticated user's ID
     * @param bookId The book's remote ID
     */
    suspend fun deleteSessionsForBook(userId: String, bookId: String) {
        postgrest.from("reading_sessions")
            .delete {
                filter {
                    eq("book_id", bookId)
                    eq("user_id", userId)
                }
            }
    }
}
