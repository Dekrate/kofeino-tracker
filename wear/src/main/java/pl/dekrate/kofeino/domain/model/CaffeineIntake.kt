package pl.dekrate.kofeino.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pojedynczy wpis spożycia kofeiny zapisany w bazie. */
@Entity(tableName = "caffeine_intakes")
data class CaffeineIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val drinkId: Long? = null,
    val drinkName: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val timestamp: Long,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val sourceDeviceId: String = ""
)
