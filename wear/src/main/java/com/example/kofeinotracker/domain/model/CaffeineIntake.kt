package com.example.kofeinotracker.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Pojedynczy wpis spożycia kofeiny zapisany w bazie. */
@Entity(tableName = "caffeine_intakes")
data class CaffeineIntake(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val drinkName: String,
    val caffeineMg: Int,
    val volumeMl: Int,
    val timestamp: Long
)
