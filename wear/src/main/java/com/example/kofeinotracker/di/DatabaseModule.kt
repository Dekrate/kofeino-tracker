package com.example.kofeinotracker.di

import android.content.Context
import androidx.room.Room
import com.example.kofeinotracker.data.local.CaffeineDatabase
import com.example.kofeinotracker.data.local.CaffeineIntakeDao
import com.example.kofeinotracker.data.repository.CaffeineRepository
import com.example.kofeinotracker.data.repository.CaffeineRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): CaffeineDatabase {
        return Room.databaseBuilder(
            context,
            CaffeineDatabase::class.java,
            "caffeine_database"
        ).build()
    }

    @Provides
    fun provideDao(database: CaffeineDatabase): CaffeineIntakeDao {
        return database.caffeineIntakeDao()
    }

    @Provides
    @Singleton
    fun provideCaffeineRepository(impl: CaffeineRepositoryImpl): CaffeineRepository {
        return impl
    }
}
