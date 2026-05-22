package pl.dekrate.kofeino.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogDao
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogEntry
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeDao
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

/**
 * Room database for the phone app.
 * Version 4 adds the [PendingChangeEntity] table for offline sync queue.
 */
@Database(
    entities = [
        CaffeineIntake::class,
        DrinkEntity::class,
        OfficialDrinkCacheEntity::class,
        PendingChangeEntity::class,
        ConflictLogEntry::class
    ],
    version = 5,
    exportSchema = false
)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun caffeineIntakeDao(): CaffeineIntakeDao
    abstract fun drinkDao(): DrinkDao
    abstract fun officialDrinkCacheDao(): OfficialDrinkCacheDao
    abstract fun pendingChangeDao(): PendingChangeDao
    abstract fun conflictLogDao(): ConflictLogDao
}
