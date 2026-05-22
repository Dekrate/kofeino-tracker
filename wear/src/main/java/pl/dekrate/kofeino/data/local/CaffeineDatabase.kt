package pl.dekrate.kofeino.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.dekrate.kofeino.data.sync.ConflictLogDao
import pl.dekrate.kofeino.data.sync.ConflictLogEntity
import pl.dekrate.kofeino.data.sync.PendingChangeDao
import pl.dekrate.kofeino.data.sync.PendingChangeEntity
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

@Database(
    entities = [
        CaffeineIntake::class,
        DrinkEntity::class,
        OfficialDrinkCacheEntity::class,
        PendingChangeEntity::class,
        ConflictLogEntity::class
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
