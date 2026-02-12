package com.bookstats.data.local.database.dao

import androidx.room.*
import com.bookstats.data.local.database.entity.BookCategoryEntity
import com.bookstats.data.local.database.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO for category operations.
 */
@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY name ASC")
    fun getAllCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY name ASC")
    suspend fun getAllCategoriesList(): List<CategoryEntity>

    @Query("SELECT * FROM categories WHERE id = :categoryId")
    suspend fun getCategoryById(categoryId: Long): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name = :name")
    suspend fun getCategoryByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE name IN (:names)")
    suspend fun getCategoriesByNames(names: List<String>): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: CategoryEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<CategoryEntity>): List<Long>

    @Update
    suspend fun updateCategory(category: CategoryEntity)

    @Delete
    suspend fun deleteCategory(category: CategoryEntity)

    @Query("DELETE FROM categories WHERE id = :categoryId")
    suspend fun deleteCategoryById(categoryId: Long)

    // Book-Category relationship queries
    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN book_categories bc ON c.id = bc.category_id
        WHERE bc.book_id = :bookId
        ORDER BY c.name ASC
    """)
    fun getCategoriesForBook(bookId: Long): Flow<List<CategoryEntity>>

    @Query("""
        SELECT c.* FROM categories c
        INNER JOIN book_categories bc ON c.id = bc.category_id
        WHERE bc.book_id = :bookId
        ORDER BY c.name ASC
    """)
    suspend fun getCategoriesForBookList(bookId: Long): List<CategoryEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookCategory(bookCategory: BookCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBookCategories(bookCategories: List<BookCategoryEntity>)

    @Query("DELETE FROM book_categories WHERE book_id = :bookId")
    suspend fun deleteBookCategories(bookId: Long)

    @Query("DELETE FROM book_categories WHERE book_id = :bookId AND category_id = :categoryId")
    suspend fun deleteBookCategory(bookId: Long, categoryId: Long)

    // Statistics queries
    @Query("""
        SELECT c.*, COUNT(bc.book_id) as bookCount
        FROM categories c
        LEFT JOIN book_categories bc ON c.id = bc.category_id
        GROUP BY c.id
        ORDER BY bookCount DESC
    """)
    fun getCategoriesWithBookCount(): Flow<List<CategoryWithCount>>

    @Query("""
        SELECT bc.book_id FROM book_categories bc
        WHERE bc.category_id = :categoryId
    """)
    suspend fun getBookIdsForCategory(categoryId: Long): List<Long>
}

data class CategoryWithCount(
    val id: Long,
    val name: String,
    val bookCount: Int
)
