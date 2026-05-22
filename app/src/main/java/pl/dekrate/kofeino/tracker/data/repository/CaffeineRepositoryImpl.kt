package pl.dekrate.kofeino.tracker.data.repository

import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity
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

    override fun getRecentIntakes(limit: Int): Flow<List<CaffeineIntake>> {
        return intakeDao.getRecentIntakes(limit)
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

    override fun searchDrinks(query: String): Flow<List<DrinkEntity>> {
        return drinkDao.searchDrinks(query)
    }

    // --- Backup / Snapshot operations ---

    override suspend fun getAllIntakesSnapshot(): List<CaffeineIntake> =
        intakeDao.getAllIntakesSnapshot()

    override suspend fun getAllDrinksSnapshot(): List<DrinkEntity> =
        drinkDao.getAllDrinksSnapshot()

    override suspend fun getAllIntakeIds(): List<Long> =
        intakeDao.getAllIntakeIds()

    override suspend fun getAllDrinkNames(): List<String> =
        drinkDao.getAllDrinkNames()

    override suspend fun bulkInsertIntakes(intakes: List<CaffeineIntake>) {
        intakeDao.insertAll(intakes)
    }

    override suspend fun bulkInsertDrinks(drinks: List<DrinkEntity>) {
        drinkDao.insertAll(drinks)
    }

    /**
     * Returns a (startOfDay, endOfDay) pair for the given timestamp.
     *
     * Uses Calendar.add(DAY_OF_YEAR, 1) instead of +86400000
     * to correctly handle DST transitions.
     * On "fall back" days (25h) it covers all 25 hours;
     * on "spring forward" days (23h) it doesn't exceed the day.
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
