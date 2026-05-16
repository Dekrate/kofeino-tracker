package pl.dekrate.kofeino.di

import android.content.Context
import androidx.room.Room
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.repository.CaffeineRepository
import pl.dekrate.kofeino.data.repository.CaffeineRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [DatabaseModule::class]
)
@Module
object TestDatabaseModule {

    @Provides
    @Singleton
    fun provideInMemoryDatabase(@ApplicationContext context: Context): CaffeineDatabase {
        return Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @Provides
    fun provideIntakeDao(database: CaffeineDatabase): CaffeineIntakeDao {
        return database.caffeineIntakeDao()
    }

    @Provides
    fun provideDrinkDao(database: CaffeineDatabase): DrinkDao {
        return database.drinkDao()
    }

    @Provides
    fun provideOfficialDrinkCacheDao(database: CaffeineDatabase): OfficialDrinkCacheDao {
        return database.officialDrinkCacheDao()
    }

    @Provides
    @Singleton
    fun provideCaffeineRepository(impl: CaffeineRepositoryImpl): CaffeineRepository {
        return impl
    }
}
