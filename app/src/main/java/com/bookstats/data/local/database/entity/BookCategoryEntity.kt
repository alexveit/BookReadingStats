package com.bookstats.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table for many-to-many relationship between books and categories.
 */
@Entity(
    tableName = "book_categories",
    primaryKeys = ["book_id", "category_id"],
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["book_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["category_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("book_id"),
        Index("category_id")
    ]
)
data class BookCategoryEntity(
    @ColumnInfo(name = "book_id")
    val bookId: Long,
    @ColumnInfo(name = "category_id")
    val categoryId: Long
)
