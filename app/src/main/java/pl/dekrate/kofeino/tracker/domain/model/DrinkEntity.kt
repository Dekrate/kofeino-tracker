package pl.dekrate.kofeino.tracker.domain.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A saved drink with caffeine content stored in the database.
 *
 * Phone-specific: added index on [name] for faster search.
 */
@Entity(
    tableName = "drinks",
    indices = [
        Index(value = ["name"], name = "idx_drinks_name")
    ]
)
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val isDefault: Boolean = false,
    val lastModifiedTimestamp: Long = 0L,
    val sourceDeviceId: String = ""
)
