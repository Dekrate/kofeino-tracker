package pl.dekrate.kofeino.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import pl.dekrate.kofeino.data.encryption.EncryptedPreferencesSerializer
import androidx.room.Room
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.MessageClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import pl.dekrate.kofeino.data.encryption.KofeinoEncryptionManager
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.data.local.OfficialDrinkCacheDao
import pl.dekrate.kofeino.data.sync.ConflictLogDao
import pl.dekrate.kofeino.data.sync.PendingChangeDao
import pl.dekrate.kofeino.data.sync.PendingSyncQueue
import java.io.File
import timber.log.Timber
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TileConfigDataStore

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncStateDataStore

@Suppress("TooManyFunctions")
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        encryptionManager: KofeinoEncryptionManager
    ): CaffeineDatabase {
        val passphrase = encryptionManager.getDatabasePassphrase()

        migrateIfUnencrypted(context, passphrase)

        val factory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            CaffeineDatabase::class.java,
            "caffeine_database"
        )
            .openHelperFactory(factory)
            .fallbackToDestructiveMigration(true)
            .addCallback(seedDatabaseCallback(context))
            .build()
    }

    /**
     * Migrate an unencrypted [caffeine_database] to an encrypted one using
     * SQLCipher's [sqlcipher_export]. The strategy:
     *
     * 1. Rename the unencrypted DB to a backup path.
     * 2. Create a fresh encrypted DB at the original path.
     * 3. ATTACH the backup as a plaintext database and export all data.
     * 4. DETACH and delete the backup.
     *
     * This is a no-op if the database is already encrypted or doesn't exist.
     */
    private fun migrateIfUnencrypted(context: Context, passphrase: ByteArray) {
        val dbFile = context.getDatabasePath("caffeine_database")
        if (!dbFile.exists()) return

        // Check whether the database is already encrypted by attempting to
        // open it with SQLCipher and the provided passphrase.
        try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                passphrase,
                null, // CursorFactory
                net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READONLY,
                null, // DatabaseErrorHandler
                null  // SQLiteDatabaseHook
            ).close()
            // Opened successfully → already encrypted, nothing to do.
            return
        } catch (_: Exception) {
            // Expected when the database is still plaintext.
        }

        // Verify we can open it as plaintext before proceeding.
        try {
            android.database.sqlite.SQLiteDatabase.openDatabase(
                dbFile.absolutePath,
                null,
                android.database.sqlite.SQLiteDatabase.OPEN_READONLY
            ).close()
        } catch (e: Exception) {
            Timber.w(e, "Cannot open legacy database as plaintext — migration aborted")
            return
        }

        Timber.i("Starting migration from unencrypted to encrypted database")

        // Step 1 – rename the old plaintext database so we can still read it.
        val backupFile = File(dbFile.absolutePath + ".legacy")
        if (backupFile.exists()) backupFile.delete()
        File(dbFile.absolutePath + "-wal").takeIf { it.exists() }?.delete()
        File(dbFile.absolutePath + "-shm").takeIf { it.exists() }?.delete()
        check(dbFile.renameTo(backupFile)) { "Failed to rename legacy database" }

        // Step 2 – create a fresh encrypted database and export data from the backup.
        try {
            net.zetetic.database.sqlcipher.SQLiteDatabase.openOrCreateDatabase(
                dbFile.absolutePath,
                passphrase,
                null, // CursorFactory
                null  // DatabaseErrorHandler
            ).use { encryptedDb ->
                encryptedDb.execSQL("ATTACH DATABASE '${backupFile.absolutePath}' AS legacy KEY '';")
                encryptedDb.execSQL("SELECT sqlcipher_export('legacy');")
                encryptedDb.execSQL("DETACH DATABASE legacy;")
            }
            Timber.i("Database migration completed successfully")
        } catch (e: Exception) {
            Timber.e(e, "Database migration failed — restoring legacy backup")
            backupFile.renameTo(dbFile)
            throw e
        }

        // Step 3 – clean up the backup.
        backupFile.delete()
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

    // ======================================================================
    // Encrypted DataStore providers
    // ======================================================================

    @Provides
    @Singleton
    @TileConfigDataStore
    fun provideTileConfigDataStore(
        @ApplicationContext context: Context,
        encryptionManager: KofeinoEncryptionManager
    ): DataStore<Preferences> {
        val encryptedSerializer = EncryptedPreferencesSerializer(
            aead = encryptionManager.aead,
            associatedData = "wear_tile_config".encodeToByteArray()
        )
        return DataStoreFactory.create(
            serializer = encryptedSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(context.filesDir, "datastore/wear_tile_config.preferences_pb") }
        )
    }

    @Provides
    @Singleton
    @SyncStateDataStore
    fun provideSyncStateDataStore(
        @ApplicationContext context: Context,
        encryptionManager: KofeinoEncryptionManager
    ): DataStore<Preferences> {
        val encryptedSerializer = EncryptedPreferencesSerializer(
            aead = encryptionManager.aead,
            associatedData = "wear_sync_state".encodeToByteArray()
        )
        return DataStoreFactory.create(
            serializer = encryptedSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
            produceFile = { File(context.filesDir, "datastore/wear_sync_state.preferences_pb") }
        )
    }

    private fun seedDatabaseCallback(context: Context) = object : androidx.room.RoomDatabase.Callback() {
        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
            super.onCreate(db)
            Timber.i("Database created – seeding default drinks")
            insertDefaults(db, context)
        }
    }

    @Suppress("InjectDispatcher")
    @Provides
    @Singleton
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

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
