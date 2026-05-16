package pl.dekrate.kofeino.data.repository

import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.flow.Flow
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaffeineRepositoryImpl @Inject constructor(
    private val intakeDao: CaffeineIntakeDao,
    private val drinkDao: DrinkDao
) : CaffeineRepository {

    // --- Intake operations ---

    override suspend fun addIntake(intake: CaffeineIntake): Long {
        return intakeDao.insert(intake)
    }

    override suspend fun updateIntake(intake: CaffeineIntake) {
        intakeDao.update(intake)
    }

    override suspend fun deleteIntake(intake: CaffeineIntake) {
        intakeDao.delete(intake)
    }

    override fun getIntakesForDate(dateMillis: Long): Flow<List<CaffeineIntake>> {
        val (start, end) = dayBounds(dateMillis)
        return intakeDao.getIntakesByDate(start, end)
    }

    override fun getTotalCaffeineForDate(dateMillis: Long): Flow<Int> {
        val (start, end) = dayBounds(dateMillis)
        return intakeDao.getTotalCaffeineByDate(start, end)
    }

    override suspend fun getIntakeById(id: Long): CaffeineIntake? {
        return intakeDao.getIntakeById(id)
    }

    override suspend fun clearAll() {
        intakeDao.deleteAll()
    }

    // --- Drink operations ---

    override fun getAllDrinks(): Flow<List<DrinkEntity>> {
        return drinkDao.getAllDrinks()
    }

    override suspend fun getDrinkById(id: Long): DrinkEntity? {
        return drinkDao.getDrinkById(id)
    }

    override suspend fun addDrink(drink: DrinkEntity): Long {
        return drinkDao.insert(drink)
    }

    override suspend fun updateDrink(drink: DrinkEntity) {
        drinkDao.update(drink)
    }

    override suspend fun deleteDrink(drink: DrinkEntity) {
        drinkDao.delete(drink)
    }

    /**
     * Zwraca parę (początekDnia, koniecDnia) dla podanego timestampu.
     *
     * Używa Calendar.add(DAY_OF_YEAR, 1) zamiast +86400000,
     * aby poprawnie obsłużyć zmiany czasu (DST).
     * W dni "cofnięcia" (fall back, 25h) obejmuje wszystkie 25h,
     * w dni "przeskoku" (spring forward, 23h) nie wychodzi poza dobę.
     */
    private fun dayBounds(millis: Long): Pair<Long, Long> {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val end = calendar.timeInMillis
        return start to end
    }
}
