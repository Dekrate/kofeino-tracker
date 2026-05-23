package pl.dekrate.kofeino.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Napój z zawartością kofeiny – przechowywany w bazie danych. */
@Entity(tableName = "drinks")
data class DrinkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val isDefault: Boolean = false,
    val lastModifiedTimestamp: Long = System.currentTimeMillis(),
    val sourceDeviceId: String = ""
)
