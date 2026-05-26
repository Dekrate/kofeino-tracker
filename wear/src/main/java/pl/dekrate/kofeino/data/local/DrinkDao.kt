package pl.dekrate.kofeino.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrinkDao {

    @Query("SELECT * FROM drinks ORDER BY name ASC")
    fun getAllDrinks(): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks WHERE id = :id")
    suspend fun getDrinkById(id: Long): DrinkEntity?

    @Insert
    suspend fun insert(drink: DrinkEntity): Long

    @Update
    suspend fun update(drink: DrinkEntity)

    @Delete
    suspend fun delete(drink: DrinkEntity)

    @Query("SELECT COUNT(*) FROM drinks")
    suspend fun getDrinkCount(): Int

    @Query(
        "SELECT * FROM drinks WHERE name LIKE '%' || " +
        "REPLACE(REPLACE(:query, '%', '\\%'), '_', '\\_') || " +
        "'%' ESCAPE '\\' ORDER BY name ASC"
    )
    fun searchDrinks(query: String): Flow<List<DrinkEntity>>

    @Query("SELECT * FROM drinks ORDER BY name ASC")
    suspend fun getAllDrinksSnapshot(): List<DrinkEntity>
}
