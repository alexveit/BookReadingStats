package com.bookstats.data.local.database.converter

import androidx.room.TypeConverter
import com.bookstats.data.local.database.entity.SyncStatus
import java.time.Instant

/**
 * Type converters for Room database.
 */
class Converters {
    
    @TypeConverter
    fun fromInstant(instant: Instant?): Long? {
        return instant?.toEpochMilli()
    }
    
    @TypeConverter
    fun toInstant(epochMilli: Long?): Instant? {
        return epochMilli?.let { Instant.ofEpochMilli(it) }
    }
    
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }
    
    @TypeConverter
    fun toSyncStatus(value: String): SyncStatus {
        return SyncStatus.valueOf(value)
    }
}
