package pl.dekrate.kofeino.tracker.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [BackupSerializer].
 *
 * Covers:
 * - Roundtrip: serialize → deserialize produces equal data
 * - Version conformance: rejects old/new versions
 * - Malformed JSON: missing version field, non-numeric version
 * - Empty collections: serialization of empty backup
 * - Schema evolution: unknown extra fields should be silently ignored
 */
class BackupSerializerTest {

    private lateinit var serializer: BackupSerializer

    @Before
    fun setUp() {
        serializer = BackupSerializer()
    }

    // ------------------------------------------------------------------
    // 1. Roundtrip
    // ------------------------------------------------------------------

    @Test
    fun `roundtrip produces equal BackupData`() {
        val original = BackupData(
            version = BackupData.CURRENT_VERSION,
            exportedAt = "2026-05-22T10:00:00Z",
            intakes = listOf(
                BackupIntake(id = 1, drinkId = 2, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000L),
                BackupIntake(id = 3, drinkName = "Tea", caffeineMg = 47, volumeMl = 250, timestamp = 2000L)
            ),
            drinks = listOf(
                BackupDrink(id = 1, name = "Espresso", caffeineMg = 63, volumeMl = 30, isDefault = true),
                BackupDrink(id = 2, name = "Latte", caffeineMg = 63, volumeMl = 250)
            ),
            settings = BackupSettings(
                language = "pl",
                themeMode = "dark",
                notifLiveEnabled = true,
                notifMorningEnabled = true,
                notifRegularEnabled = false,
                notifEveningEnabled = false
            )
        )

        val json = serializer.serialize(original)
        val restored = serializer.deserialize(json)

        assertEquals(original.version, restored.version)
        assertEquals(original.exportedAt, restored.exportedAt)
        assertEquals(original.intakes.size, restored.intakes.size)
        assertEquals(original.drinks.size, restored.drinks.size)
        assertEquals(original.settings.language, restored.settings.language)
        assertEquals(original.settings.themeMode, restored.settings.themeMode)

        // Verify intake content
        assertEquals("Espresso", restored.intakes[0].drinkName)
        assertEquals(63, restored.intakes[0].caffeineMg)
        assertEquals(1L, restored.intakes[0].id)

        // Verify drink content
        assertEquals("Espresso", restored.drinks[0].name)
        assertTrue(restored.drinks[0].isDefault)
    }

    @Test
    fun `roundtrip empty collections`() {
        val original = BackupData(
            exportedAt = "2026-05-22T10:00:00Z",
            intakes = emptyList(),
            drinks = emptyList()
        )

        val json = serializer.serialize(original)
        val restored = serializer.deserialize(json)

        assertTrue(restored.intakes.isEmpty())
        assertTrue(restored.drinks.isEmpty())
    }

    // ------------------------------------------------------------------
    // 2. Version rejection
    // ------------------------------------------------------------------

    @Test(expected = BackupVersionException::class)
    fun `rejects version older than MIN_SUPPORTED_VERSION`() {
        val json = """{"version":0,"exportedAt":"now","intakes":[],"drinks":[]}"""
        serializer.deserialize(json)
    }

    @Test(expected = BackupVersionException::class)
    fun `rejects version newer than CURRENT_VERSION`() {
        val tooNew = BackupData.CURRENT_VERSION + 1
        val json = """{"version":$tooNew,"exportedAt":"now","intakes":[],"drinks":[]}"""
        serializer.deserialize(json)
    }

    @Test
    fun `accepts exactly MIN_SUPPORTED_VERSION`() {
        val json = """{"version":1,"exportedAt":"now","intakes":[],"drinks":[]}"""
        val data = serializer.deserialize(json)
        assertEquals(1, data.version)
    }

    @Test
    fun `accepts exactly CURRENT_VERSION`() {
        val json = """{"version":${BackupData.CURRENT_VERSION},"exportedAt":"now","intakes":[],"drinks":[]}"""
        val data = serializer.deserialize(json)
        assertEquals(BackupData.CURRENT_VERSION, data.version)
    }

    // ------------------------------------------------------------------
    // 3. Malformed input
    // ------------------------------------------------------------------

    @Test(expected = BackupVersionException::class)
    fun `rejects missing version field`() {
        val json = """{"exportedAt":"now","intakes":[],"drinks":[]}"""
        serializer.deserialize(json)
    }

    @Test(expected = BackupVersionException::class)
    fun `rejects non-numeric version field`() {
        val json = """{"version":"abc","exportedAt":"now","intakes":[],"drinks":[]}"""
        serializer.deserialize(json)
    }

    @Test(expected = Exception::class)
    fun `rejects completely malformed JSON`() {
        serializer.deserialize("not-json-at-all")
    }

    // ------------------------------------------------------------------
    // 4. Schema evolution — forward compat
    // ------------------------------------------------------------------

    @Test
    fun `ignores unknown extra fields gracefully`() {
        val json = """
            {
                "version": 1,
                "exportedAt": "2026-05-22T10:00:00Z",
                "intakes": [],
                "drinks": [],
                "unknownField": "should be ignored",
                "nestedUnknown": {"foo": 1}
            }
        """.trimIndent()

        val data = serializer.deserialize(json)
        assertNotNull(data)
        assertEquals(1, data.version)
    }

    // ------------------------------------------------------------------
    // 5. createBackup helper
    // ------------------------------------------------------------------

    @Test
    fun `createBackup stamps correct version`() {
        val data = serializer.createBackup(
            intakes = emptyList(),
            drinks = emptyList(),
            settings = BackupSettings()
        )

        assertEquals(BackupData.CURRENT_VERSION, data.version)
        assertNotNull(data.exportedAt)
        assertTrue(data.exportedAt.isNotEmpty())
    }

    @Test
    fun `createBackup preserves domain data`() {
        val intakes = listOf(
            BackupIntake(drinkName = "Test Drink", caffeineMg = 50, volumeMl = 200, timestamp = 100L)
        )
        val drinks = listOf(
            BackupDrink(name = "Custom", caffeineMg = 100, volumeMl = 300)
        )

        val data = serializer.createBackup(intakes, drinks, BackupSettings())

        assertEquals(1, data.intakes.size)
        assertEquals(1, data.drinks.size)
        assertEquals("Test Drink", data.intakes[0].drinkName)
        assertEquals("Custom", data.drinks[0].name)
    }
}
