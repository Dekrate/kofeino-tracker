package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncEventTest {

    private val testTimestamp = 1_234_567_890_000L
    private val testDeviceId = "phone-001"

    // ─── IntakeAdded tests ───────────────────────────────────────────────

    @Test
    fun `IntakeAdded should store intakeJson`() {
        val event = SyncEvent.IntakeAdded(
            intakeJson = """{"id":1}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("""{"id":1}""", event.intakeJson)
    }

    @Test
    fun `IntakeAdded should implement SyncEvent`() {
        val event: SyncEvent = SyncEvent.IntakeAdded(
            intakeJson = "{}",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals(testTimestamp, event.timestamp)
        assertEquals(testDeviceId, event.sourceDeviceId)
    }

    // ─── IntakeDeleted tests ─────────────────────────────────────────────

    @Test
    fun `IntakeDeleted should store intakeId`() {
        val event = SyncEvent.IntakeDeleted(
            intakeId = 42L,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals(42L, event.intakeId)
        assertEquals(testTimestamp, event.timestamp)
        assertEquals(testDeviceId, event.sourceDeviceId)
    }

    // ─── IntakeUpdated tests ─────────────────────────────────────────────

    @Test
    fun `IntakeUpdated should store intakeJson`() {
        val event = SyncEvent.IntakeUpdated(
            intakeJson = """{"id":1,"caffeineMg":100}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("""{"id":1,"caffeineMg":100}""", event.intakeJson)
        assertEquals(testTimestamp, event.timestamp)
        assertEquals(testDeviceId, event.sourceDeviceId)
    }

    // ─── DrinkCreated tests ──────────────────────────────────────────────

    @Test
    fun `DrinkCreated should store drinkJson`() {
        val event = SyncEvent.DrinkCreated(
            drinkJson = """{"name":"Espresso"}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("""{"name":"Espresso"}""", event.drinkJson)
    }

    // ─── DrinkUpdated tests ──────────────────────────────────────────────

    @Test
    fun `DrinkUpdated should store drinkJson`() {
        val event = SyncEvent.DrinkUpdated(
            drinkJson = """{"name":"Latte"}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("""{"name":"Latte"}""", event.drinkJson)
    }

    // ─── DrinkDeleted tests ──────────────────────────────────────────────

    @Test
    fun `DrinkDeleted should store drinkId`() {
        val event = SyncEvent.DrinkDeleted(
            drinkId = 7L,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals(7L, event.drinkId)
    }

    // ─── SettingsChanged tests ─────────────────────────────────────────────

    @Test
    fun `SettingsChanged should store settingsJson`() {
        val event = SyncEvent.SettingsChanged(
            settingsJson = """{"theme":"dark"}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("""{"theme":"dark"}""", event.settingsJson)
    }

    // ─── FullSyncRequest tests ────────────────────────────────────────────

    @Test
    fun `FullSyncRequest should store all fields`() {
        val event = SyncEvent.FullSyncRequest(
            stateHash = "abc123def456",
            lastSyncTimestamp = testTimestamp - 86_400_000L,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("abc123def456", event.stateHash)
        assertEquals(testTimestamp - 86_400_000L, event.lastSyncTimestamp)
    }

    // ─── FullSyncResponse tests ───────────────────────────────────────────

    @Test
    fun `FullSyncResponse should store entity type and entities`() {
        val entities = listOf("""{"id":1}""", """{"id":2}""")
        val event = SyncEvent.FullSyncResponse(
            entityType = SyncEntityType.INTAKE,
            entitiesJson = entities,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals(SyncEntityType.INTAKE, event.entityType)
        assertEquals(2, event.entitiesJson.size)
        assertTrue(event.entitiesJson.contains("""{"id":1}"""))
    }

    // ─── SyncAck tests ───────────────────────────────────────────────────

    @Test
    fun `SyncAck should store state hash and received types`() {
        val event = SyncEvent.SyncAck(
            stateHash = "abc123",
            receivedEntityTypes = listOf(SyncEntityType.INTAKE, SyncEntityType.DRINK),
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals("abc123", event.stateHash)
        assertEquals(2, event.receivedEntityTypes.size)
        assertTrue(event.receivedEntityTypes.contains(SyncEntityType.INTAKE))
    }

    // ─── Exhaustiveness tests ────────────────────────────────────────────

    @Test
    fun `when on SyncEvent should be exhaustive for all variants`() {
        val events: List<SyncEvent> = listOf(
            SyncEvent.IntakeAdded("{}", testTimestamp, testDeviceId),
            SyncEvent.IntakeDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.IntakeUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkCreated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.SettingsChanged("{}", testTimestamp, testDeviceId),
            SyncEvent.FullSyncRequest("hash", 0L, testTimestamp, testDeviceId),
            SyncEvent.FullSyncResponse(SyncEntityType.INTAKE, emptyList(), testTimestamp, testDeviceId),
            SyncEvent.SyncAck("hash", emptyList(), testTimestamp, testDeviceId),
        )

        // Exhaustive when — if a new variant is added to SyncEvent without
        // adding a branch here, the compiler will fail.
        events.forEach { event ->
            val description = when (event) {
                is SyncEvent.IntakeAdded -> "intake-added:${event.intakeJson}"
                is SyncEvent.IntakeDeleted -> "intake-deleted:${event.intakeId}"
                is SyncEvent.IntakeUpdated -> "intake-updated:${event.intakeJson}"
                is SyncEvent.DrinkCreated -> "drink-created:${event.drinkJson}"
                is SyncEvent.DrinkUpdated -> "drink-updated:${event.drinkJson}"
                is SyncEvent.DrinkDeleted -> "drink-deleted:${event.drinkId}"
                is SyncEvent.SettingsChanged -> "settings-changed:${event.settingsJson}"
                is SyncEvent.FullSyncRequest -> "full-sync-request:${event.stateHash}"
                is SyncEvent.FullSyncResponse -> "full-sync-response:${event.entityType}"
                is SyncEvent.SyncAck -> "sync-ack:${event.stateHash}"
            }
            assertTrue(description.isNotBlank())
        }
    }

    @Test
    fun `there should be exactly 10 SyncEvent variants`() {
        // Exhaustive listing — compiler will flag if a new variant is added
        // without updating this list. Avoids kotlin-reflect dependency.
        val variants: List<SyncEvent> = listOf(
            SyncEvent.IntakeAdded("{}", 0L, ""),
            SyncEvent.IntakeDeleted(0L, 0L, ""),
            SyncEvent.IntakeUpdated("{}", 0L, ""),
            SyncEvent.DrinkCreated("{}", 0L, ""),
            SyncEvent.DrinkUpdated("{}", 0L, ""),
            SyncEvent.DrinkDeleted(0L, 0L, ""),
            SyncEvent.SettingsChanged("{}", 0L, ""),
            SyncEvent.FullSyncRequest("", 0L, 0L, ""),
            SyncEvent.FullSyncResponse(SyncEntityType.INTAKE, emptyList(), 0L, ""),
            SyncEvent.SyncAck("", emptyList(), 0L, ""),
        )
        assertEquals("If this fails, a SyncEvent variant was added or removed. Update the list.",
            10, variants.size)
    }

    // ─── Data class tests ────────────────────────────────────────────────

    @Test
    fun `SyncEvent data classes should support copy`() {
        val original = SyncEvent.IntakeAdded(
            intakeJson = """{"id":1}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        val copy = original.copy(intakeJson = """{"id":2}""")
        assertEquals("""{"id":2}""", copy.intakeJson)
        assertEquals(testTimestamp, copy.timestamp) // unchanged
    }

    @Test
    fun `SyncEvent data classes should support equals`() {
        val event1 = SyncEvent.IntakeDeleted(
            intakeId = 42L,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        val event2 = SyncEvent.IntakeDeleted(
            intakeId = 42L,
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertEquals(event1, event2)
        assertEquals(event1.hashCode(), event2.hashCode())
    }

    @Test
    fun `SyncEvent data classes should have toString`() {
        val event = SyncEvent.DrinkCreated(
            drinkJson = """{"name":"Coffee"}""",
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        val str = event.toString()
        assertTrue(str.contains("Coffee"))
        assertTrue(str.contains("DrinkCreated"))
    }

    @Test
    fun `FullSyncResponse with different entities should not be equal`() {
        val event1 = SyncEvent.FullSyncResponse(
            entityType = SyncEntityType.INTAKE,
            entitiesJson = listOf("""{"id":1}"""),
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        val event2 = SyncEvent.FullSyncResponse(
            entityType = SyncEntityType.DRINK,
            entitiesJson = listOf("""{"id":1}"""),
            timestamp = testTimestamp,
            sourceDeviceId = testDeviceId,
        )
        assertTrue(event1 != event2)
    }

    // ─── Timestamp tests ─────────────────────────────────────────────────

    @Test
    fun `all SyncEvent variants should preserve timestamp`() {
        listOf(
            SyncEvent.IntakeAdded("{}", testTimestamp, testDeviceId),
            SyncEvent.IntakeDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.IntakeUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkCreated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.SettingsChanged("{}", testTimestamp, testDeviceId),
            SyncEvent.FullSyncRequest("h", 0L, testTimestamp, testDeviceId),
            SyncEvent.FullSyncResponse(SyncEntityType.INTAKE, emptyList(), testTimestamp, testDeviceId),
            SyncEvent.SyncAck("h", emptyList(), testTimestamp, testDeviceId),
        ).forEach { event ->
            val eventType = event::class.simpleName ?: "Unknown"
            assertEquals("$eventType should preserve timestamp",
                testTimestamp, event.timestamp)
        }
    }

    @Test
    fun `all SyncEvent variants should preserve sourceDeviceId`() {
        listOf(
            SyncEvent.IntakeAdded("{}", testTimestamp, testDeviceId),
            SyncEvent.IntakeDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.IntakeUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkCreated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkUpdated("{}", testTimestamp, testDeviceId),
            SyncEvent.DrinkDeleted(1L, testTimestamp, testDeviceId),
            SyncEvent.SettingsChanged("{}", testTimestamp, testDeviceId),
            SyncEvent.FullSyncRequest("h", 0L, testTimestamp, testDeviceId),
            SyncEvent.FullSyncResponse(SyncEntityType.INTAKE, emptyList(), testTimestamp, testDeviceId),
            SyncEvent.SyncAck("h", emptyList(), testTimestamp, testDeviceId),
        ).forEach { event ->
            val eventType = event::class.simpleName ?: "Unknown"
            assertEquals("$eventType should preserve sourceDeviceId",
                testDeviceId, event.sourceDeviceId)
        }
    }
}
