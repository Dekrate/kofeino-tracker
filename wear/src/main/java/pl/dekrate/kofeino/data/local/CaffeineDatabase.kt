package pl.dekrate.kofeino.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

@Database(
    entities = [CaffeineIntake::class, DrinkEntity::class],
    version = 2,
    exportSchema = false
)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun caffeineIntakeDao(): CaffeineIntakeDao
    abstract fun drinkDao(): DrinkDao
}
