package pl.dekrate.kofeino.tracker.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single caffeine intake entry stored in the database.
 *
 * Phone-specific improvements over [wear module]: added indices
 * for faster date-range queries (the phone typically has more data).
 */
@Entity(
    tableName = "caffeine_intakes",
    indices = [
        Index(value = ["timestamp"], name = "idx_intakes_timestamp")
    ]
)
data class CaffeineIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val drinkId: Long? = null,
    val drinkName: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val timestamp: Long,
    val lastModifiedTimestamp: Long = 0L,
    val sourceDeviceId: String = ""
)
