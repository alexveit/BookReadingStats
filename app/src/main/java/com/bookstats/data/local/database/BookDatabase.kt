package com.bookstats.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bookstats.data.local.database.converter.Converters
import com.bookstats.data.local.database.dao.BookDao
import com.bookstats.data.local.database.dao.CategoryDao
import com.bookstats.data.local.database.dao.ReadingSessionDao
import com.bookstats.data.local.database.entity.BookCategoryEntity
import com.bookstats.data.local.database.entity.BookEntity
import com.bookstats.data.local.database.entity.CategoryEntity
import com.bookstats.data.local.database.entity.ReadingSessionEntity

/**
 * Main Room database for the Book Reading Stats app.
 */
@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        CategoryEntity::class,
        BookCategoryEntity::class
    ],
    version = 8,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BookDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun categoryDao(): CategoryDao
    
    companion object {
        const val DATABASE_NAME = "book_stats_db"
        
        /**
         * Migration from version 1 to 2: Add remote_id columns for Supabase sync.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN remote_id TEXT DEFAULT NULL"
                )
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN currentPage INTEGER NOT NULL DEFAULT 0"
                )
                database.execSQL(
                    "ALTER TABLE reading_sessions ADD COLUMN remote_id TEXT DEFAULT NULL"
                )
            }
        }
        
        /**
         * Migration from version 2 to 3: Add sync_status columns for offline support.
         */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'"
                )
                database.execSQL(
                    "ALTER TABLE reading_sessions ADD COLUMN sync_status TEXT NOT NULL DEFAULT 'SYNCED'"
                )
            }
        }
        
        /**
         * Migration from version 3 to 4: Remove unused currentPage column.
         * Note: Requires SQLite 3.35.0+ (Android 12+)
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE books DROP COLUMN currentPage")
            }
        }
        
        /**
         * Migration from version 4 to 5: Add cover_image_url column.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN cover_image_url TEXT DEFAULT NULL"
                )
            }
        }
        
        /**
         * Migration from version 5 to 6: Add author and category columns.
         */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN author TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "ALTER TABLE books ADD COLUMN category TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        /**
         * Migration from version 6 to 7: Add indices for better query performance.
         */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add indices to books table
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_books_remote_id ON books(remote_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_books_sync_status ON books(sync_status)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_books_createdAt ON books(createdAt)")

                // Add indices to reading_sessions table
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_reading_sessions_remote_id ON reading_sessions(remote_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_reading_sessions_sync_status ON reading_sessions(sync_status)")
            }
        }

        /**
         * Migration from version 7 to 8: Add categories table and book_categories junction table.
         */
        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create categories table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS categories (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL
                    )
                """)
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_categories_name ON categories(name)")

                // Create book_categories junction table
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS book_categories (
                        book_id INTEGER NOT NULL,
                        category_id INTEGER NOT NULL,
                        PRIMARY KEY(book_id, category_id),
                        FOREIGN KEY(book_id) REFERENCES books(id) ON DELETE CASCADE,
                        FOREIGN KEY(category_id) REFERENCES categories(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_categories_book_id ON book_categories(book_id)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_book_categories_category_id ON book_categories(category_id)")

                // Migrate existing category data from books table to new tables
                database.execSQL("""
                    INSERT OR IGNORE INTO categories (name)
                    SELECT DISTINCT category FROM books WHERE category != ''
                """)
                database.execSQL("""
                    INSERT OR IGNORE INTO book_categories (book_id, category_id)
                    SELECT b.id, c.id FROM books b
                    INNER JOIN categories c ON b.category = c.name
                    WHERE b.category != ''
                """)
            }
        }
    }
}
