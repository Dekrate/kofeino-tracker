package pl.dekrate.kofeino.presentation.tile

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.dekrate.kofeino.data.local.TileDataStorePreferences

@EntryPoint
@InstallIn(SingletonComponent::class)
interface TileServiceHiltEntryPoint {
    fun tileDataStorePreferences(): TileDataStorePreferences
}
