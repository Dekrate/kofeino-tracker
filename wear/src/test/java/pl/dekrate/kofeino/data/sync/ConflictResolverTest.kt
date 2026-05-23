package pl.dekrate.kofeino.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

class ConflictResolverTest {

    // ======================================================================
    // compareTimestamps
    // ======================================================================

    @Test
    fun `local newer timestamp wins`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now - 10_000
        )
        assertTrue(result.localWins)
        assertEquals(ConflictResolver.REASON_LOCAL_NEWER, result.reason)
    }

    @Test
    fun `incoming newer timestamp wins`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now - 10_000,
            localSourceId = "watch",
            incomingTimestamp = now
        )
        assertEquals(false, result.localWins)
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
    }

    @Test
    fun `equal timestamps phone wins as local`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = ConflictResolver.PHONE_DEVICE_ID,
            incomingTimestamp = now
        )
        assertTrue(result.localWins)
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
    }

    @Test
    fun `equal timestamps watch loses to phone`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = ConflictResolver.WATCH_DEVICE_ID,
            incomingTimestamp = now
        )
        assertEquals(false, result.localWins)
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
    }

    @Test
    fun `equal timestamps both watch phone wins as incoming`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now
        )
        assertEquals(false, result.localWins)
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
    }

    @Test
    fun `within 1ms tolerance triggers tiebreaker`() {
        val now = System.currentTimeMillis()
        // 0ms difference — exact tie
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now
        )
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
        assertEquals(false, result.localWins)
    }

    @Test
    fun `clock skew detected when diff exceeds 60s`() {
        val now = System.currentTimeMillis()
        val farFuture = now + 61_000L // 61 seconds ahead
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = farFuture
        )
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
        assertNotNull(result.clockSkewWarning)
        assertTrue(result.clockSkewWarning!!.contains("Clock skew"))
    }

    @Test
    fun `no clock skew warning for small diff`() {
        val now = System.currentTimeMillis()
        val slightFuture = now + 5_000 // 5 seconds ahead
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = slightFuture
        )
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
        assertNull(result.clockSkewWarning)
    }

    @Test
    fun `clock skew detected in both directions`() {
        val now = System.currentTimeMillis()
        val farPast = now - 70_000L
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "phone",
            incomingTimestamp = farPast
        )
        assertEquals(ConflictResolver.REASON_LOCAL_NEWER, result.reason)
        assertNotNull(result.clockSkewWarning)
    }

    @Test
    fun `within 1ms tolerance triggers tiebreaker even at 1ms difference`() {
        val now = System.currentTimeMillis()
        // 1ms difference — still within tolerance
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now + 1
        )
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
        assertEquals(false, result.localWins)
    }

    @Test
    fun `just over 1ms difference uses timestamp`() {
        val now = System.currentTimeMillis()
        // 2ms difference — outside tolerance
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now + 2
        )
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
        assertEquals(false, result.localWins)
    }

    @Test
    fun `clock skew warning at exactly 60s difference`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now + 60_000
        )
        // At exactly 60s, should NOT trigger (threshold is >60s)
        assertNull(result.clockSkewWarning)
    }

    @Test
    fun `clock skew warning at 60001ms difference`() {
        val now = System.currentTimeMillis()
        val result = ConflictResolver.compareTimestamps(
            localTimestamp = now,
            localSourceId = "watch",
            incomingTimestamp = now + 60_001
        )
        assertNotNull(result.clockSkewWarning)
    }

    // ======================================================================
    // resolveIntakeConflict
    // ======================================================================

    @Test
    fun `delete operation always wins for intake`() {
        val local = makeIntake(id = 1, timestamp = 1000, sourceId = "watch")
        val incoming = makeIntake(id = 1, timestamp = 500, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_DELETE
        )
        assertEquals(ConflictResolver.REASON_DELETE_WINS, result.reason)
        assertTrue(result.wasConflict)
        // Winner is the incoming entity (delete instruction)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `delete on non-existent local is no-conflict`() {
        val incoming = makeIntake(id = 1, timestamp = 1000, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = null,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_DELETE
        )
        assertEquals(ConflictResolver.REASON_DELETE_WINS, result.reason)
        assertEquals(false, result.wasConflict)
    }

    @Test
    fun `local null incoming wins no conflict for intake`() {
        val incoming = makeIntake(id = 1, timestamp = 1000, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = null,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_EQUAL_NO_CONFLICT, result.reason)
        assertEquals(false, result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `local timestamp newer wins intake conflict`() {
        val now = System.currentTimeMillis()
        val local = makeIntake(id = 1, timestamp = now, sourceId = "watch")
        val incoming = makeIntake(id = 1, timestamp = now - 10_000, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_LOCAL_NEWER, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(local, result.winner)
    }

    @Test
    fun `incoming timestamp newer wins intake conflict`() {
        val now = System.currentTimeMillis()
        val local = makeIntake(id = 1, timestamp = now - 10_000, sourceId = "watch")
        val incoming = makeIntake(id = 1, timestamp = now, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `equal timestamps phone wins intake tiebreaker`() {
        val now = System.currentTimeMillis()
        val local = makeIntake(id = 1, timestamp = now, sourceId = "watch")
        val incoming = makeIntake(id = 1, timestamp = now, sourceId = "phone")
        val result = ConflictResolver.resolveIntakeConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    // ======================================================================
    // resolveDrinkConflict
    // ======================================================================

    @Test
    fun `delete operation always wins for drink`() {
        val local = makeDrink(id = 1, timestamp = 1000, sourceId = "watch")
        val incoming = makeDrink(id = 1, timestamp = 500, sourceId = "phone")
        val result = ConflictResolver.resolveDrinkConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_DELETE
        )
        assertEquals(ConflictResolver.REASON_DELETE_WINS, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `local null incoming wins no conflict for drink`() {
        val incoming = makeDrink(id = 1, timestamp = 1000, sourceId = "phone")
        val result = ConflictResolver.resolveDrinkConflict(
            local = null,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_EQUAL_NO_CONFLICT, result.reason)
        assertEquals(false, result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `local timestamp newer wins drink conflict`() {
        val now = System.currentTimeMillis()
        val local = makeDrink(id = 1, timestamp = now, sourceId = "watch")
        val incoming = makeDrink(id = 1, timestamp = now - 5_000, sourceId = "phone")
        val result = ConflictResolver.resolveDrinkConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_LOCAL_NEWER, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(local, result.winner)
    }

    @Test
    fun `incoming timestamp newer wins drink conflict`() {
        val now = System.currentTimeMillis()
        val local = makeDrink(id = 1, timestamp = now - 5_000, sourceId = "watch")
        val incoming = makeDrink(id = 1, timestamp = now, sourceId = "phone")
        val result = ConflictResolver.resolveDrinkConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_INCOMING_NEWER, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    @Test
    fun `equal timestamps phone wins drink tiebreaker`() {
        val now = System.currentTimeMillis()
        val local = makeDrink(id = 1, timestamp = now, sourceId = "watch")
        val incoming = makeDrink(id = 1, timestamp = now, sourceId = "phone")
        val result = ConflictResolver.resolveDrinkConflict(
            local = local,
            incoming = incoming,
            operationType = PendingChangeEntity.OPERATION_UPDATE
        )
        assertEquals(ConflictResolver.REASON_PHONE_WINS_TIE, result.reason)
        assertTrue(result.wasConflict)
        assertEquals(incoming, result.winner)
    }

    // ======================================================================
    // serializeEntity
    // ======================================================================

    @Test
    fun `serializeEntity handles null`() {
        assertEquals("null", ConflictResolver.serializeEntity(null))
    }

    @Test
    fun `serializeEntity produces valid JSON`() {
        val intake = makeIntake(id = 1, timestamp = 1000, sourceId = "watch")
        val json = ConflictResolver.serializeEntity(intake)
        assertTrue(json.contains("\"id\":1"))
        assertTrue(json.contains("\"sourceDeviceId\":\"watch\""))
    }

    @Test
    fun `serializeEntity with DrinkEntity produces valid JSON`() {
        val drink = makeDrink(id = 5, timestamp = 2000, sourceId = "phone")
        val json = ConflictResolver.serializeEntity(drink)
        assertTrue(json.contains("\"id\":5"))
        assertTrue(json.contains("\"name\":\"test drink\""))
        assertTrue(json.contains("\"caffeineMg\":50"))
        assertTrue(json.contains("\"sourceDeviceId\":\"phone\""))
    }

    // ======================================================================
    // Helpers
    // ======================================================================

    private fun makeIntake(
        id: Long = 0,
        timestamp: Long = System.currentTimeMillis(),
        sourceId: String = ""
    ): CaffeineIntake {
        return CaffeineIntake(
            id = id,
            drinkName = "test",
            caffeineMg = 50,
            volumeMl = 250,
            timestamp = timestamp,
            lastModifiedTimestamp = timestamp,
            sourceDeviceId = sourceId
        )
    }

    private fun makeDrink(
        id: Long = 0,
        timestamp: Long = System.currentTimeMillis(),
        sourceId: String = ""
    ): DrinkEntity {
        return DrinkEntity(
            id = id,
            name = "test drink",
            caffeineMg = 50,
            volumeMl = 250,
            lastModifiedTimestamp = timestamp,
            sourceDeviceId = sourceId
        )
    }
}
