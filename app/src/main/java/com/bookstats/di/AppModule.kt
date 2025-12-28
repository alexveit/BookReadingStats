package com.bookstats.di

import android.content.Context
import androidx.room.Room
import androidx.work.WorkManager
import com.bookstats.data.local.database.BookDatabase
import com.bookstats.data.local.database.dao.BookDao
import com.bookstats.data.local.database.dao.ReadingSessionDao
import com.bookstats.data.remote.GoogleBooksApi
import com.bookstats.data.remote.SupabaseDataSource
import com.bookstats.data.repository.AuthRepository
import com.bookstats.data.repository.BookRepositoryImpl
import com.bookstats.data.sync.SyncManager
import com.bookstats.domain.repository.BookRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): BookDatabase {
        return Room.databaseBuilder(
            context,
            BookDatabase::class.java,
            BookDatabase.DATABASE_NAME
        )
        .addMigrations(
            BookDatabase.MIGRATION_1_2,
            BookDatabase.MIGRATION_2_3,
            BookDatabase.MIGRATION_3_4,
            BookDatabase.MIGRATION_4_5,
            BookDatabase.MIGRATION_5_6,
            BookDatabase.MIGRATION_6_7
        )
        // Note: No fallbackToDestructiveMigration() - never silently delete user data.
        // Always write proper migrations when schema changes.
        .build()
    }
    
    @Provides
    @Singleton
    fun provideBookDao(database: BookDatabase): BookDao {
        return database.bookDao()
    }
    
    @Provides
    @Singleton
    fun provideReadingSessionDao(database: BookDatabase): ReadingSessionDao {
        return database.readingSessionDao()
    }

    @Provides
    @Singleton
    fun provideSupabaseDataSource(): SupabaseDataSource {
        return SupabaseDataSource()
    }

    @Provides
    @Singleton
    fun provideAuthRepository(
        @ApplicationContext context: Context
    ): AuthRepository {
        return AuthRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideSyncManager(
        @ApplicationContext context: Context,
        bookDao: BookDao,
        sessionDao: ReadingSessionDao,
        remoteDataSource: SupabaseDataSource,
        authRepository: AuthRepository
    ): SyncManager {
        return SyncManager(context, bookDao, sessionDao, remoteDataSource, authRepository)
    }
    
    @Provides
    @Singleton
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    @Provides
    @Singleton
    fun provideGoogleBooksApi(): GoogleBooksApi {
        return GoogleBooksApi()
    }
}

/**
 * Hilt module for repository bindings.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindBookRepository(
        bookRepositoryImpl: BookRepositoryImpl
    ): BookRepository
}
