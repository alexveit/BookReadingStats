package com.bookstats.data.local.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for book categories/topics.
 */
@Entity(
    tableName = "categories",
    indices = [Index("name", unique = true)]
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String
)
