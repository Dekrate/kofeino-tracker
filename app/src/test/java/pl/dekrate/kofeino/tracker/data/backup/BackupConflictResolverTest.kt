package pl.dekrate.kofeino.tracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BackupConflictResolver].
 *
 * Covers:
 * - Intake dedup by [BackupIntake.id]
 * - Drink dedup by [BackupDrink.name]
 * - Skipped items are counted correctly
 * - Domain model conversion resets IDs to 0
 * - Empty imports handled gracefully
 * - Mixed: some items skipped, some inserted
 */
class BackupConflictResolverTest {

    private lateinit var resolver: BackupConflictResolver

    @Before
    fun setUp() {
        resolver = BackupConflictResolver()
    }

    // ------------------------------------------------------------------
    // 1. No local data → all imported
    // ------------------------------------------------------------------

    @Test
    fun `resolve with empty local DB imports all`() {
        val intakes = listOf(
            BackupIntake(id = 1, drinkName = "A", caffeineMg = 50, volumeMl = 100, timestamp = 1000L),
            BackupIntake(id = 2, drinkName = "B", caffeineMg = 75, volumeMl = 200, timestamp = 2000L)
        )
        val drinks = listOf(
            BackupDrink(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30)
        )

        val result = resolver.resolve(intakes, drinks, emptySet(), emptySet())

        assertEquals(2, result.intakesToInsert.size)
        assertEquals(0, result.intakesSkipped)
        assertEquals(1, result.drinksToInsert.size)
        assertEquals(0, result.drinksSkipped)
    }

    // ------------------------------------------------------------------
    // 2. Intake dedup by ID
    // ------------------------------------------------------------------

    @Test
    fun `resolve skips intakes whose IDs exist locally`() {
        val intakes = listOf(
            BackupIntake(id = 10, drinkName = "Skip", caffeineMg = 50, volumeMl = 100, timestamp = 1000L),
            BackupIntake(id = 20, drinkName = "Keep", caffeineMg = 75, volumeMl = 200, timestamp = 2000L)
        )

        val result = resolver.resolve(intakes, emptyList(), existingIntakeIds = setOf(10L), emptySet())

        assertEquals(1, result.intakesToInsert.size)
        assertEquals(1, result.intakesSkipped)
        assertEquals("Keep", result.intakesToInsert[0].drinkName)
        assertEquals(75, result.intakesToInsert[0].caffeineMg)
    }

    @Test
    fun `resolve keeps intakes with id 0 even if 0 exists locally`() {
        val intakes = listOf(
            BackupIntake(id = 0, drinkName = "Unsaved", caffeineMg = 30, volumeMl = 150, timestamp = 3000L)
        )

        val result = resolver.resolve(intakes, emptyList(), existingIntakeIds = setOf(0L), emptySet())

        assertEquals(1, result.intakesToInsert.size)
        assertEquals(0, result.intakesSkipped)
    }

    @Test
    fun `resolve keeps intakes with non-matching IDs`() {
        val intakes = listOf(
            BackupIntake(id = 100, drinkName = "Unique", caffeineMg = 40, volumeMl = 200, timestamp = 4000L)
        )

        val result = resolver.resolve(intakes, emptyList(), existingIntakeIds = setOf(1L, 2L, 3L), emptySet())

        assertEquals(1, result.intakesToInsert.size)
        assertEquals(0, result.intakesSkipped)
    }

    // ------------------------------------------------------------------
    // 3. Drink dedup by name
    // ------------------------------------------------------------------

    @Test
    fun `resolve skips drinks whose names exist locally`() {
        val drinks = listOf(
            BackupDrink(id = 5, name = "Espresso", caffeineMg = 63, volumeMl = 30),
            BackupDrink(id = 6, name = "Latte", caffeineMg = 63, volumeMl = 250)
        )

        val result = resolver.resolve(emptyList(), drinks, emptySet(), existingDrinkNames = setOf("Espresso"))

        assertEquals(1, result.drinksToInsert.size)
        assertEquals(1, result.drinksSkipped)
        assertEquals("Latte", result.drinksToInsert[0].name)
    }

    @Test
    fun `resolve keeps drinks with unique names`() {
        val drinks = listOf(
            BackupDrink(name = "Flat White", caffeineMg = 100, volumeMl = 200)
        )

        val result = resolver.resolve(emptyList(), drinks, emptySet(), existingDrinkNames = setOf("Espresso", "Latte"))

        assertEquals(1, result.drinksToInsert.size)
        assertEquals(0, result.drinksSkipped)
        assertEquals("Flat White", result.drinksToInsert[0].name)
    }

    // ------------------------------------------------------------------
    // 4. Counts are correct
    // ------------------------------------------------------------------

    @Test
    fun `resolve returns correct counts for mixed scenario`() {
        val intakes = listOf(
            BackupIntake(id = 1, drinkName = "A", caffeineMg = 50, volumeMl = 100, timestamp = 1L),
            BackupIntake(id = 2, drinkName = "B", caffeineMg = 60, volumeMl = 200, timestamp = 2L),
            BackupIntake(id = 3, drinkName = "C", caffeineMg = 70, volumeMl = 300, timestamp = 3L)
        )
        val drinks = listOf(
            BackupDrink(name = "Existing", caffeineMg = 63, volumeMl = 30),
            BackupDrink(name = "New", caffeineMg = 80, volumeMl = 250)
        )

        val result = resolver.resolve(intakes, drinks,
            existingIntakeIds = setOf(1L, 3L),
            existingDrinkNames = setOf("Existing"))

        assertEquals(1, result.intakesToInsert.size)   // id=2 is new
        assertEquals(2, result.intakesSkipped)          // id=1 and id=3 exist
        assertEquals(1, result.drinksToInsert.size)     // "New" is new
        assertEquals(1, result.drinksSkipped)           // "Existing" exists
    }

    // ------------------------------------------------------------------
    // 5. Domain model conversion resets IDs
    // ------------------------------------------------------------------

    @Test
    fun `resolve resets intake IDs to 0 for fresh insert`() {
        val intakes = listOf(
            BackupIntake(id = 42, drinkName = "Test", caffeineMg = 50, volumeMl = 100, timestamp = 1L)
        )

        val result = resolver.resolve(intakes, emptyList(), emptySet(), emptySet())

        assertEquals(1, result.intakesToInsert.size)
        assertEquals(0L, result.intakesToInsert[0].id) // auto-generated
    }

    @Test
    fun `resolve resets drink IDs to 0 for fresh insert`() {
        val drinks = listOf(
            BackupDrink(id = 99, name = "Matcha", caffeineMg = 30, volumeMl = 200)
        )

        val result = resolver.resolve(emptyList(), drinks, emptySet(), emptySet())

        assertEquals(1, result.drinksToInsert.size)
        assertEquals(0L, result.drinksToInsert[0].id) // auto-generated
    }

    // ------------------------------------------------------------------
    // 6. Edge cases
    // ------------------------------------------------------------------

    @Test
    fun `resolve with empty imported lists returns empty result`() {
        val result = resolver.resolve(emptyList(), emptyList(), emptySet(), emptySet())

        assertEquals(0, result.intakesToInsert.size)
        assertEquals(0, result.intakesSkipped)
        assertEquals(0, result.drinksToInsert.size)
        assertEquals(0, result.drinksSkipped)
    }

    @Test
    fun `resolve skips all when everything exists locally`() {
        val intakes = listOf(
            BackupIntake(id = 1, drinkName = "A", caffeineMg = 50, volumeMl = 100, timestamp = 1L)
        )
        val drinks = listOf(
            BackupDrink(name = "Existing", caffeineMg = 63, volumeMl = 30)
        )

        val result = resolver.resolve(intakes, drinks,
            existingIntakeIds = setOf(1L),
            existingDrinkNames = setOf("Existing"))

        assertTrue(result.intakesToInsert.isEmpty())
        assertEquals(1, result.intakesSkipped)
        assertTrue(result.drinksToInsert.isEmpty())
        assertEquals(1, result.drinksSkipped)
    }

    @Test
    fun `resolve handles duplicate IDs in backup file`() {
        val intakes = listOf(
            BackupIntake(id = 1, drinkName = "A", caffeineMg = 50, volumeMl = 100, timestamp = 1L),
            BackupIntake(id = 1, drinkName = "B", caffeineMg = 60, volumeMl = 200, timestamp = 2L) // same ID as above
        )

        val result = resolver.resolve(intakes, emptyList(), existingIntakeIds = setOf(1L), emptySet())

        // Both have id=1 which exists locally → both skipped
        assertEquals(0, result.intakesToInsert.size)
        assertEquals(2, result.intakesSkipped)
    }
}
