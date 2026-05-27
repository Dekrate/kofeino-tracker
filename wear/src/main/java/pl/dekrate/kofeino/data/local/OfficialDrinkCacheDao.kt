package pl.dekrate.kofeino.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface OfficialDrinkCacheDao {

    @Query("SELECT * FROM official_drink_cache ORDER BY name ASC")
    suspend fun getAllCached(): List<OfficialDrinkCacheEntity>

    @Query("SELECT * FROM official_drink_cache WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): OfficialDrinkCacheEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(drinks: List<OfficialDrinkCacheEntity>)

    @Query("DELETE FROM official_drink_cache")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM official_drink_cache")
    suspend fun count(): Int
}
