package pl.dekrate.kofeino.tracker.di

import android.content.Context
import androidx.room.Room
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepository
import pl.dekrate.kofeino.tracker.data.repository.CaffeineRepositoryImpl
import pl.dekrate.kofeino.tracker.data.repository.OfficialDrinkRepository
import pl.dekrate.kofeino.tracker.data.repository.OfficialDrinkRepositoryImpl
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeDao
import pl.dekrate.kofeino.tracker.data.sync.PendingSyncQueue
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
import com.google.android.gms.wearable.MessageClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import timber.log.Timber
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
    @Singleton
    fun providePendingSyncQueue(
        dao: PendingChangeDao,
        messageClient: MessageClient
    ): PendingSyncQueue {
        // nodeId is a placeholder; the real node id is resolved at runtime
        // via CapabilityClient before flush() is called.
        return PendingSyncQueue(dao, messageClient, nodeId = "")
    }

    @Provides
    @Singleton
    fun provideCaffeineRepository(impl: CaffeineRepositoryImpl): CaffeineRepository {
        return impl
    }

    @Provides
    @Singleton
    fun provideOfficialDrinkRepository(impl: OfficialDrinkRepositoryImpl): OfficialDrinkRepository {
        return impl
    }

    private fun seedDatabaseCallback(context: Context) = object : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            Timber.i("Database created – seeding defaults")
            insertDefaults(db)
            insertOfficialDrinkCache(db)
        }

        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onOpen(db)
            // Seed official drink cache if empty (e.g. after upgrade with existing DB)
            val count = db.compileStatement(
                "SELECT COUNT(*) FROM official_drink_cache"
            ).simpleQueryForLong()
            if (count == 0L) {
                Timber.i("Official drink cache empty – seeding on open")
                insertOfficialDrinkCache(db)
            }
        }
    }

    private fun insertDefaults(db: androidx.sqlite.db.SupportSQLiteDatabase) {
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
        // Room already wraps onCreate in a transaction, so no BEGIN/COMMIT needed.
        try {
            val stmt = db.compileStatement(
                "INSERT OR IGNORE INTO drinks (name, caffeineMg, volumeMl, isDefault) VALUES (?, ?, ?, 1)"
            )
            for ((name, caffeine, volume) in defaults) {
                stmt.clearBindings()
                stmt.bindString(1, name)
                stmt.bindLong(2, caffeine.toLong())
                stmt.bindLong(3, volume.toLong())
                stmt.executeInsert()
            }
            stmt.close()
            Timber.i("Seeded ${defaults.size} default drinks")
        } catch (e: Exception) {
            Timber.e(e, "Failed to seed default drinks")
        }
    }

    private fun insertOfficialDrinkCache(db: androidx.sqlite.db.SupportSQLiteDatabase) {
        // Seed cache with common drinks (data sourced from Open Food Facts)
        val now = System.currentTimeMillis()
        val officialDrinks = listOf(
            arrayOf("5449000000996", "Coca-Cola", "Coca-Cola", "10.0", "0.0", "330ml"),
            arrayOf("5449000131805", "Coca-Cola Zero", "Coca-Cola", "10.0", "0.0", "330ml"),
            arrayOf("5900951071711", "Red Bull Energy Drink", "Red Bull", "32.0", "45.0", "250ml"),
            arrayOf("5900951071728", "Red Bull Sugarfree", "Red Bull", "32.0", "20.0", "250ml"),
            arrayOf("5900951071735", "Monster Energy", "Monster", "34.0", "50.0", "500ml"),
            arrayOf("5010023010008", "Nescafe Original", "Nestlé", "34.0", "2.0", "200ml"),
            arrayOf("8000130000000", "Illy Espresso", "Illy", "118.0", "0.0", "50ml"),
            arrayOf("8711000083699", "Douwe Egberts Kawa mielona", "Douwe Egberts", "55.0", "0.0", "250g"),
            arrayOf("5900951071742", "Tchibo Kawa ziarnista", "Tchibo", "60.0", "0.0", "500g"),
            arrayOf("2000000000000", "Starbucks Caffè Latte", "Starbucks", "23.0", "34.0", "250ml"),
            arrayOf("2000000000001", "Starbucks Cappuccino", "Starbucks", "20.0", "29.0", "250ml"),
            arrayOf("2000000000002", "Starbucks Espresso Roast", "Starbucks", "213.0", "0.0", "100g"),
            arrayOf("5900951071759", "Lipton Green Tea", "Lipton", "16.0", "1.0", "250ml"),
            arrayOf("5900951071766", "Lipton Black Tea", "Lipton", "20.0", "1.0", "250ml"),
            arrayOf("5900951071773", "Pepsi Cola", "Pepsi", "10.0", "42.0", "330ml"),
            arrayOf("5900951071780", "Pepsi Max", "Pepsi", "12.0", "0.0", "330ml")
        )

        try {
            val stmt = db.compileStatement(
                """INSERT OR IGNORE INTO official_drink_cache 
                   (barcode, name, brand, caffeineMgPer100ml, energyKcalPer100ml, quantity, fetchedAtMillis) 
                   VALUES (?, ?, ?, ?, ?, ?, ?)"""
            )
            for (drink in officialDrinks) {
                stmt.clearBindings()
                stmt.bindString(1, drink[0])   // barcode
                stmt.bindString(2, drink[1])   // name
                stmt.bindString(3, drink[2])   // brand
                stmt.bindDouble(4, drink[3].toDouble()) // caffeineMgPer100ml
                stmt.bindDouble(5, drink[4].toDouble()) // energyKcalPer100ml
                stmt.bindString(6, drink[5])   // quantity
                stmt.bindLong(7, now)
                stmt.executeInsert()
            }
            stmt.close()
            Timber.i("Seeded ${officialDrinks.size} official drink cache entries")
        } catch (e: Exception) {
            Timber.e(e, "Failed to seed official drink cache")
        }
    }
}
