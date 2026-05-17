package pl.dekrate.kofeino.tracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

/**
 * Room database for the phone app.
 * Version 3 matches the wear module DB version for data parity.
 *
 * Phone-specific: schema unchanged from wear (v3 compatible).
 * Phone has more storage — no schema constraints removed.
 */
@Database(
    entities = [CaffeineIntake::class, DrinkEntity::class, OfficialDrinkCacheEntity::class],
    version = 3,
    exportSchema = false
)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun caffeineIntakeDao(): CaffeineIntakeDao
    abstract fun drinkDao(): DrinkDao
    abstract fun officialDrinkCacheDao(): OfficialDrinkCacheDao
}
