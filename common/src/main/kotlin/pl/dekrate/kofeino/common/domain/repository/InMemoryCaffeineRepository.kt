package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity

/**
 * In-memory implementation of [CaffeineRepository] for use in contract tests.
 *
 * This fake repository uses [MutableStateFlow] for its backing stores so that
 * every emission from `Flow`-returning methods reflects the latest in-memory
 * state without any database or I/O overhead.
 *
 * @param initialIntakes  Seed values for the intake backing store.
 * @param initialDrinks   Seed values for the drink backing store.
 */
class InMemoryCaffeineRepository(
    initialIntakes: List<CaffeineIntake> = emptyList(),
    initialDrinks: List<DrinkEntity> = emptyList(),
) : CaffeineRepository {

    private val _intakes = MutableStateFlow(initialIntakes)
    private val _drinks = MutableStateFlow(initialDrinks)

    private var nextIntakeId: Long = (initialIntakes.maxOfOrNull { it.id } ?: 0L) + 1L
    private var nextDrinkId: Long = (initialDrinks.maxOfOrNull { it.id } ?: 0L) + 1L

    // ------------------------------------------------------------------
    // Intake operations
    // ------------------------------------------------------------------

    override suspend fun addIntake(intake: CaffeineIntake): Long {
        val id = nextIntakeId++
        val copy = intake.copy(id = id)
        _intakes.value = _intakes.value.toMutableList().apply { add(copy) }
        return id
    }

    override suspend fun updateIntake(intake: CaffeineIntake) {
        _intakes.value = _intakes.value.toMutableList().apply {
            val index = indexOfFirst { it.id == intake.id }
            if (index >= 0) {
                set(index, intake)
            }
        }
    }

    override suspend fun deleteIntake(intake: CaffeineIntake) {
        _intakes.value = _intakes.value.toMutableList().apply {
            removeAll { it.id == intake.id }
        }
    }

    override suspend fun getIntakeById(id: Long): CaffeineIntake? {
        return _intakes.value.firstOrNull { it.id == id }
    }

    override fun getIntakesForDate(date: LocalDate): Flow<List<CaffeineIntake>> {
        return _intakes.map { intakes ->
            intakes.filter { intake ->
                Instant.fromEpochMilliseconds(intake.timestamp)
                    .toLocalDateTime(TimeZone.UTC)
                    .date == date
            }
        }
    }

    override fun getTotalCaffeineForDate(date: LocalDate): Flow<Int> {
        return getIntakesForDate(date).map { intakes ->
            intakes.sumOf { it.caffeineMg }
        }
    }

    override suspend fun clearAll() {
        _intakes.value = emptyList()
        _drinks.value = emptyList()
    }

    // ------------------------------------------------------------------
    // Drink operations
    // ------------------------------------------------------------------

    override fun getAllDrinks(): Flow<List<DrinkEntity>> {
        return _drinks.map { it }
    }

    override suspend fun getDrinkById(id: Long): DrinkEntity? {
        return _drinks.value.firstOrNull { it.id == id }
    }

    override suspend fun addDrink(drink: DrinkEntity): Long {
        val id = nextDrinkId++
        val copy = drink.copy(id = id)
        _drinks.value = _drinks.value.toMutableList().apply { add(copy) }
        return id
    }

    override suspend fun updateDrink(drink: DrinkEntity) {
        _drinks.value = _drinks.value.toMutableList().apply {
            val index = indexOfFirst { it.id == drink.id }
            if (index >= 0) {
                set(index, drink)
            }
        }
    }

    override suspend fun deleteDrink(drink: DrinkEntity) {
        _drinks.value = _drinks.value.toMutableList().apply {
            removeAll { it.id == drink.id }
        }
    }
}
