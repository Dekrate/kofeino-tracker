package pl.dekrate.kofeino.di

import android.content.Context
import androidx.room.Room
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.sync.ConflictLogDao
import pl.dekrate.kofeino.data.sync.PendingChangeDao
import pl.dekrate.kofeino.data.sync.PendingSyncQueue
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import timber.log.Timber

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
        )
            .fallbackToDestructiveMigration(true)
            .addCallback(seedDatabaseCallback(context))
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
    fun providePendingChangeDao(database: CaffeineDatabase): PendingChangeDao {
        return database.pendingChangeDao()
    }

    @Provides
    fun provideConflictLogDao(database: CaffeineDatabase): ConflictLogDao {
        return database.conflictLogDao()
    }

    @Provides
    @Singleton
    fun providePendingSyncQueue(
        dao: PendingChangeDao,
        messageClient: MessageClient,
        capabilityClient: CapabilityClient
    ): PendingSyncQueue {
        return PendingSyncQueue(dao, messageClient, capabilityClient)
    }

    private fun seedDatabaseCallback(context: Context) = object : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            Timber.i("Database created – seeding default drinks")
            insertDefaults(db, context)
        }
    }

    private fun insertDefaults(db: androidx.sqlite.db.SupportSQLiteDatabase, context: Context) {
        val defaults = listOf(
            Triple("Espresso", 63, 30),
            Triple("Podwójne espresso", 126, 60),
            Triple("Czarna kawa", 95, 250),
            Triple("Cappuccino", 75, 200),
            Triple("Latte", 63, 250),
            Triple("Herbata", 47, 250),
            Triple("Zielona herbata", 28, 250),
            Triple("Energy drink", 80, 250),
            Triple("Cola", 34, 330)
        )
        db.execSQL("BEGIN TRANSACTION")
        try {
            for ((name, caffeine, volume) in defaults) {
                val stmt = db.compileStatement(
                    "INSERT OR IGNORE INTO drinks (name, caffeineMg, volumeMl, isDefault) VALUES (?, ?, ?, 1)"
                )
                stmt.bindString(1, name)
                stmt.bindLong(2, caffeine.toLong())
                stmt.bindLong(3, volume.toLong())
                stmt.executeInsert()
                stmt.close()
            }
            db.execSQL("COMMIT")
            Timber.i("Seeded ${defaults.size} default drinks")
        } catch (e: Exception) {
            db.execSQL("ROLLBACK")
            Timber.e(e, "Failed to seed default drinks")
        }
    }
}
