package pl.dekrate.kofeino.common.domain.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.common.domain.model.CaffeineIntake
import pl.dekrate.kofeino.common.domain.model.DrinkEntity

/**
 * Contract tests for [CaffeineRepository].
 *
 * These tests verify the expected behavior contract of the interface using
 * an in-memory fake implementation. Any real implementation should pass
 * these same tests to guarantee contract conformance.
 */
class CaffeineRepositoryContractTest {

    private lateinit var repository: InMemoryCaffeineRepository
    private val testDate = LocalDate(2026, 5, 23)

    @Before
    fun setUp() {
        repository = InMemoryCaffeineRepository()
    }

    @After
    fun tearDown() {
        repository.clear()
    }

    // ── Intake operation contract tests ─────────────────────────────────────

    @Test
    fun `addIntake returns positive ID and intake can be retrieved`() = runTest {
        val intake = aCaffeineIntake()
        val id = repository.addIntake(intake)
        assertTrue("ID must be positive", id > 0)
        val retrieved = repository.getIntakeById(id)
        assertNotNull("Retrieved intake must not be null", retrieved)
        assertEquals("Retrieved drinkName must match", intake.drinkName, retrieved!!.drinkName)
    }

    @Test
    fun `addIntake auto-increments IDs`() = runTest {
        val id1 = repository.addIntake(aCaffeineIntake(drinkName = "First"))
        val id2 = repository.addIntake(aCaffeineIntake(drinkName = "Second"))
        assertEquals("First ID should be 1", 1L, id1)
        assertEquals("Second ID should be 2", 2L, id2)
    }

    @Test
    fun `getIntakeById returns null for non-existent ID`() = runTest {
        val result = repository.getIntakeById(999L)
        assertNull("Non-existent intake should be null", result)
    }

    @Test
    fun `updateIntake modifies existing intake`() = runTest {
        val id = repository.addIntake(aCaffeineIntake(caffeineMg = 100))
        repository.updateIntake(
            repository.getIntakeById(id)!!.copy(caffeineMg = 200)
        )
        val updated = repository.getIntakeById(id)
        assertEquals("Updated caffeineMg must match", 200, updated!!.caffeineMg)
    }

    @Test
    fun `updateIntake with non-existent ID does nothing`() = runTest {
        val nonExistent = aCaffeineIntake(id = 999L)
        repository.updateIntake(nonExistent) // should not throw
        assertNull(repository.getIntakeById(999L))
    }

    @Test
    fun `deleteIntake removes the intake`() = runTest {
        val id = repository.addIntake(aCaffeineIntake())
        assertNotNull("Intake should exist before deletion", repository.getIntakeById(id))
        repository.deleteIntake(repository.getIntakeById(id)!!)
        assertNull("Intake should be null after deletion", repository.getIntakeById(id))
    }

    @Test
    fun `deleteIntake with non-existent ID does nothing`() = runTest {
        val nonExistent = aCaffeineIntake(id = 999L)
        repository.deleteIntake(nonExistent) // should not throw
    }

    @Test
    fun `getIntakesForDate returns intakes for the correct date`() = runTest {
        val today = epochMillisForDate(2026, 5, 1)
        val tomorrow = epochMillisForDate(2026, 5, 2)
        repository.addIntake(aCaffeineIntake(timestamp = today, drinkName = "Today"))
        repository.addIntake(aCaffeineIntake(timestamp = tomorrow, drinkName = "Tomorrow"))

        val todayIntakes = repository.getIntakesForDate(LocalDate(2026, 5, 1)).first()
        assertEquals("Should have 1 intake on May 1", 1, todayIntakes.size)
        assertEquals("Today", todayIntakes[0].drinkName)
    }

    @Test
    fun `getIntakesForDate returns empty list for date with no intakes`() = runTest {
        val result = repository.getIntakesForDate(LocalDate(2026, 1, 1)).first()
        assertTrue("Empty list for date with no intakes", result.isEmpty())
    }

    @Test
    fun `getTotalCaffeineForDate returns correct sum`() = runTest {
        val date = LocalDate(2026, 5, 15)
        val millis = epochMillisForDate(2026, 5, 15)
        repository.addIntake(aCaffeineIntake(caffeineMg = 100, timestamp = millis))
        repository.addIntake(aCaffeineIntake(caffeineMg = 200, timestamp = millis))

        val total = repository.getTotalCaffeineForDate(date).first()
        assertEquals("Total caffeine should be 300mg", 300, total)
    }

    @Test
    fun `getTotalCaffeineForDate returns 0 for date with no intakes`() = runTest {
        val total = repository.getTotalCaffeineForDate(LocalDate(2026, 1, 1)).first()
        assertEquals("Total should be 0 for date with no intakes", 0, total)
    }

    @Test
    fun `clearAll removes all intakes`() = runTest {
        repository.addIntake(aCaffeineIntake())
        repository.addIntake(aCaffeineIntake())
        repository.clearAll()
        val intakes = repository.getIntakesForDate(testDate).first()
        assertTrue("All intakes should be removed", intakes.isEmpty())
    }

    @Test
    fun `intake with explicit drinkId is preserved`() = runTest {
        val id = repository.addIntake(aCaffeineIntake(drinkId = 42L))
        val retrieved = repository.getIntakeById(id)
        assertEquals("drinkId must be preserved", 42L, retrieved!!.drinkId)
    }

    @Test
    fun `multiple intakes same date returns correct count`() = runTest {
        val millis = epochMillisForDate(2026, 5, 20)
        repository.addIntake(aCaffeineIntake(timestamp = millis))
        repository.addIntake(aCaffeineIntake(timestamp = millis))
        repository.addIntake(aCaffeineIntake(timestamp = millis))

        val intakes = repository.getIntakesForDate(LocalDate(2026, 5, 20)).first()
        assertEquals("Should have 3 intakes", 3, intakes.size)
    }

    // ── Drink operation contract tests ──────────────────────────────────────

    @Test
    fun `getAllDrinks returns empty list initially`() = runTest {
        val drinks = repository.getAllDrinks().first()
        assertTrue("Initial drinks should be empty", drinks.isEmpty())
    }

    @Test
    fun `getAllDrinks returns all drinks`() = runTest {
        repository.addDrink(aDrinkEntity(name = "Coffee"))
        repository.addDrink(aDrinkEntity(name = "Tea"))
        val drinks = repository.getAllDrinks().first()
        assertEquals("Should have 2 drinks", 2, drinks.size)
    }

    @Test
    fun `addDrink returns positive ID and drink can be retrieved`() = runTest {
        val drink = aDrinkEntity()
        val id = repository.addDrink(drink)
        assertTrue("Drink ID must be positive", id > 0)
        val retrieved = repository.getDrinkById(id)
        assertNotNull("Retrieved drink must not be null", retrieved)
        assertEquals("Name must match", drink.name, retrieved!!.name)
    }

    @Test
    fun `addDrink auto-increments drink IDs`() = runTest {
        val id1 = repository.addDrink(aDrinkEntity(name = "First"))
        val id2 = repository.addDrink(aDrinkEntity(name = "Second"))
        assertEquals("First drink ID should be 1", 1L, id1)
        assertEquals("Second drink ID should be 2", 2L, id2)
    }

    @Test
    fun `getDrinkById returns null for non-existent ID`() = runTest {
        val result = repository.getDrinkById(999L)
        assertNull("Non-existent drink should be null", result)
    }

    @Test
    fun `updateDrink modifies existing drink`() = runTest {
        val id = repository.addDrink(aDrinkEntity(name = "Original"))
        repository.updateDrink(repository.getDrinkById(id)!!.copy(name = "Updated"))
        val updated = repository.getDrinkById(id)
        assertEquals("Updated name must match", "Updated", updated!!.name)
    }

    @Test
    fun `updateDrink with non-existent ID does nothing`() = runTest {
        val nonExistent = aDrinkEntity(id = 999L)
        repository.updateDrink(nonExistent) // should not throw
        assertNull(repository.getDrinkById(999L))
    }

    @Test
    fun `deleteDrink removes the drink`() = runTest {
        val id = repository.addDrink(aDrinkEntity())
        assertNotNull(repository.getDrinkById(id))
        repository.deleteDrink(repository.getDrinkById(id)!!)
        assertNull(repository.getDrinkById(id))
    }

    @Test
    fun `deleteDrink with non-existent ID does nothing`() = runTest {
        val nonExistent = aDrinkEntity(id = 999L)
        repository.deleteDrink(nonExistent) // should not throw
    }

    @Test
    fun `clearAll removes all drinks`() = runTest {
        repository.addDrink(aDrinkEntity())
        repository.addDrink(aDrinkEntity())
        repository.clearAll()
        val drinks = repository.getAllDrinks().first()
        assertTrue("All drinks should be removed", drinks.isEmpty())
    }

    // ── Helper factory methods ──────────────────────────────────────────────

    private fun aCaffeineIntake(
        id: Long = 0L,
        drinkId: Long? = null,
        drinkName: String = "Test Coffee",
        caffeineMg: Int = 100,
        volumeMl: Int = 250,
        timestamp: Long = epochMillisForDate(2026, 5, 23),
        lastModifiedTimestamp: Long = 0L,
        sourceDeviceId: String = "",
    ): CaffeineIntake = CaffeineIntake(
        id = id,
        drinkId = drinkId,
        drinkName = drinkName,
        caffeineMg = caffeineMg,
        volumeMl = volumeMl,
        timestamp = timestamp,
        lastModifiedTimestamp = lastModifiedTimestamp,
        sourceDeviceId = sourceDeviceId,
    )

    private fun aDrinkEntity(
        id: Long = 0L,
        name: String = "Test Drink",
        caffeineMg: Int = 100,
        volumeMl: Int = 250,
        isDefault: Boolean = false,
        lastModifiedTimestamp: Long = 0L,
        sourceDeviceId: String = "",
    ): DrinkEntity = DrinkEntity(
        id = id,
        name = name,
        caffeineMg = caffeineMg,
        volumeMl = volumeMl,
        isDefault = isDefault,
        lastModifiedTimestamp = lastModifiedTimestamp,
        sourceDeviceId = sourceDeviceId,
    )

    companion object {
        /** Convert a date to epoch milliseconds (UTC) using kotlinx-datetime. */
        fun epochMillisForDate(year: Int, month: Int, dayOfMonth: Int): Long {
            return LocalDate(year, month, dayOfMonth)
                .atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        }
    }
}

/**
 * In-memory fake implementation of [CaffeineRepository] for contract testing.
 *
 * Stores data in mutable lists and uses auto-incrementing IDs.
 * Date filtering is done by converting epoch millis to [LocalDate] via
 * kotlinx-datetime.
 */
class InMemoryCaffeineRepository : CaffeineRepository {

    private val intakes = mutableListOf<CaffeineIntake>()
    private val drinks = mutableListOf<DrinkEntity>()
    private var nextIntakeId = 1L
    private var nextDrinkId = 1L

    fun clear() {
        intakes.clear()
        drinks.clear()
        nextIntakeId = 1L
        nextDrinkId = 1L
    }

    override suspend fun addIntake(intake: CaffeineIntake): Long {
        val id = nextIntakeId++
        intakes.add(intake.copy(id = id))
        return id
    }

    override suspend fun updateIntake(intake: CaffeineIntake) {
        val index = intakes.indexOfFirst { it.id == intake.id }
        if (index >= 0) {
            intakes[index] = intake
        }
    }

    override suspend fun deleteIntake(intake: CaffeineIntake) {
        intakes.removeAll { it.id == intake.id }
    }

    override suspend fun getIntakeById(id: Long): CaffeineIntake? {
        return intakes.find { it.id == id }
    }

    override fun getIntakesForDate(date: LocalDate): Flow<List<CaffeineIntake>> {
        return flowOf(intakes.filter { intake ->
            val instant = Instant.fromEpochMilliseconds(intake.timestamp)
            val localDate = instant.toLocalDateTime(TimeZone.UTC).date
            localDate == date
        })
    }

    override fun getTotalCaffeineForDate(date: LocalDate): Flow<Int> {
        return flowOf(intakes.filter { intake ->
            val instant = Instant.fromEpochMilliseconds(intake.timestamp)
            val localDate = instant.toLocalDateTime(TimeZone.UTC).date
            localDate == date
        }.sumOf { it.caffeineMg })
    }

    override suspend fun clearAll() {
        intakes.clear()
        drinks.clear()
    }

    override fun getAllDrinks(): Flow<List<DrinkEntity>> {
        return flowOf(drinks.toList())
    }

    override suspend fun getDrinkById(id: Long): DrinkEntity? {
        return drinks.find { it.id == id }
    }

    override suspend fun addDrink(drink: DrinkEntity): Long {
        val id = nextDrinkId++
        drinks.add(drink.copy(id = id))
        return id
    }

    override suspend fun updateDrink(drink: DrinkEntity) {
        val index = drinks.indexOfFirst { it.id == drink.id }
        if (index >= 0) {
            drinks[index] = drink
        }
    }

    override suspend fun deleteDrink(drink: DrinkEntity) {
        drinks.removeAll { it.id == drink.id }
    }
}
