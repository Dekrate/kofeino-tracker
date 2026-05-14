package com.example.kofeinotracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.kofeinotracker.domain.model.CaffeineIntake

@Database(entities = [CaffeineIntake::class], version = 1, exportSchema = false)
abstract class CaffeineDatabase : RoomDatabase() {
    abstract fun caffeineIntakeDao(): CaffeineIntakeDao
}
