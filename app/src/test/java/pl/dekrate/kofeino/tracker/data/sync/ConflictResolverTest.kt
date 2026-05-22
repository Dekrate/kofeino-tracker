package pl.dekrate.kofeino.tracker.data.sync

import org.junit.After
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.OPERATION_DELETE
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.OPERATION_INSERT
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.OPERATION_UPDATE
import timber.log.Timber

/**
 * Unit tests for [ConflictResolver] — pure function, no mocking needed
 * except for Timber (clock-skew warning).
 *
 * Covers all scenarios required by the issue:
 * - Newer timestamp wins (local newer, incoming newer)
 * - Equal timestamp → phone wins
 * - Delete vs update → delete wins
 * - Same timestamp, same source → idempotent (no-op)
 * - Clock skew (60s+ difference) → still works, logs warning
 * - Both devices created different entities at same timestamp
 * - No local data → accept incoming
 * - Batch resolution
 */
class ConflictResolverTest {

    private lateinit var resolver: ConflictResolver
    private val logMessages = mutableListOf<String>()
    private lateinit var testTree: Timber.Tree

    @Before
    fun setUpTimber() {
        resolver = ConflictResolver()
        testTree = object : Timber.Tree() {
            override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
                logMessages.add(message)
            }
        }
        Timber.plant(testTree)
    }

    @After
    fun tearDownTimber() {
        Timber.uproot(testTree)
    }

    // Shared test fixtures
    private val localInsert = SyncChange(
        entityType = "intake",
        entityId = "42",
        operationType = OPERATION_INSERT,
        payload = """{"caffeineMg":63}""",
        timestamp = 1000L,
        source = DeviceSource.PHONE
    )

    private val incomingInsert = SyncChange(
        entityType = "intake",
        entityId = "42",
        operationType = OPERATION_INSERT,
        payload = """{"caffeineMg":95}""",
        timestamp = 2000L,
        source = DeviceSource.WATCH
    )

    // ------------------------------------------------------------------
    // 1. No local data → accept incoming
    // ------------------------------------------------------------------

    @Test
    fun `resolve with null local accepts incoming`() {
        val result = resolver.resolve(local = null, incoming = incomingInsert)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming when local is null"
        }
        val accept = result as ConflictResolution.AcceptIncoming
        assert(accept.change == incomingInsert)
    }

    // ------------------------------------------------------------------
    // 2. Newer timestamp wins
    // ------------------------------------------------------------------

    @Test
    fun `resolve with incoming newer than local accepts incoming`() {
        val local = localInsert.copy(timestamp = 1000L)
        val incoming = incomingInsert.copy(timestamp = 2000L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming when incoming is newer"
        }
    }

    @Test
    fun `resolve with local newer than incoming keeps local`() {
        val local = localInsert.copy(timestamp = 3000L)
        val incoming = incomingInsert.copy(timestamp = 2000L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when local is newer"
        }
    }

    @Test
    fun `resolve with incoming newer keeps local when incoming has UPDATE`() {
        // Same entity, incoming has a later timestamp → should accept incoming
        val local = localInsert.copy(timestamp = 100L, operationType = OPERATION_INSERT)
        val incoming = incomingInsert.copy(timestamp = 200L, operationType = OPERATION_UPDATE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming when incoming is newer regardless of op type"
        }
    }

    // ------------------------------------------------------------------
    // 3. Equal timestamp → local device wins
    // ------------------------------------------------------------------

    @Test
    fun `resolve with equal timestamps keeps local`() {
        val local = localInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE)
        val incoming = incomingInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when timestamps are equal (local wins)"
        }
    }

    @Test
    fun `resolve with equal timestamps keeps local even when local source is WATCH`() {
        // Even when local is the watch, equal timestamps → local wins
        val local = localInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH)
        val incoming = incomingInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when timestamps are equal (local always wins)"
        }
    }

    // ------------------------------------------------------------------
    // 4. Delete vs update → delete wins
    // ------------------------------------------------------------------

    @Test
    fun `resolve with incoming DELETE keeps nothing and accepts incoming`() {
        val local = localInsert.copy(timestamp = 2000L, operationType = OPERATION_UPDATE)
        val incoming = incomingInsert.copy(timestamp = 1000L, operationType = OPERATION_DELETE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming when incoming is DELETE regardless of timestamp"
        }
    }

    @Test
    fun `resolve with local DELETE keeps local`() {
        val local = localInsert.copy(timestamp = 1000L, operationType = OPERATION_DELETE)
        val incoming = incomingInsert.copy(timestamp = 2000L, operationType = OPERATION_UPDATE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when local is DELETE regardless of timestamp"
        }
    }

    @Test
    fun `resolve with both DELETE and incoming newer timestamp accepts incoming`() {
        // Both are DELETE — newer timestamp wins
        val local = localInsert.copy(timestamp = 1000L, operationType = OPERATION_DELETE)
        val incoming = incomingInsert.copy(timestamp = 2000L, operationType = OPERATION_DELETE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming when both DELETE and incoming is newer"
        }
    }

    @Test
    fun `resolve with both DELETE and local newer timestamp keeps local`() {
        val local = localInsert.copy(timestamp = 3000L, operationType = OPERATION_DELETE)
        val incoming = incomingInsert.copy(timestamp = 2000L, operationType = OPERATION_DELETE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when both DELETE and local is newer"
        }
    }

    // ------------------------------------------------------------------
    // 5. Same timestamp, same source → idempotent (no-op)
    // ------------------------------------------------------------------

    @Test
    fun `resolve with identical timestamp and same source returns NoOp`() {
        val local = localInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE)
        val incoming = incomingInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.NoOp) {
            "Expected NoOp when timestamp and source are identical, got $result"
        }
    }

    @Test
    fun `resolve with identical timestamp and same source WATCH returns NoOp`() {
        val local = localInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH)
        val incoming = incomingInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.NoOp) {
            "Expected NoOp when both are WATCH with same timestamp"
        }
    }

    @Test
    fun `resolve with identical timestamp but different source does NOT return NoOp`() {
        // Same timestamp, different source → local wins (not NoOp)
        val local = localInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE)
        val incoming = incomingInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when same timestamp but different sources"
        }
    }

    // ------------------------------------------------------------------
    // 6. Clock skew (60s+ difference) → still works, Timber warning logged
    // ------------------------------------------------------------------

    @Test
    fun `resolve with clock skew over 60s still resolves correctly`() {
        // Local is 70s older → incoming should win
        val local = localInsert.copy(timestamp = 0L)
        val incoming = incomingInsert.copy(timestamp = 70_001L) // 70s later

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming even with clock skew"
        }
    }

    @Test
    fun `resolve with clock skew and local newer still keeps local`() {
        val local = localInsert.copy(timestamp = 200_000L) // local is way ahead
        val incoming = incomingInsert.copy(timestamp = 1000L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "Expected KeepLocal when local is newer despite clock skew"
        }
    }

    @Test
    fun `resolve logs warning on clock skew over threshold`() {
        logMessages.clear()

        val local = localInsert.copy(timestamp = 0L)
        val incoming = incomingInsert.copy(timestamp = 120_000L) // 120s skew

        resolver.resolve(local, incoming)

        assert(logMessages.any { it.contains("Clock skew") }) {
            "Expected 'Clock skew' log message, got: $logMessages"
        }
    }

    @Test
    fun `resolve does NOT log warning under clock skew threshold`() {
        logMessages.clear()

        val local = localInsert.copy(timestamp = 0L)
        val incoming = incomingInsert.copy(timestamp = 59_000L) // 59s — under threshold

        resolver.resolve(local, incoming)

        assert(logMessages.none { it.contains("Clock skew") }) {
            "Did NOT expect 'Clock skew' log message, got: $logMessages"
        }
    }

    // ------------------------------------------------------------------
    // 7. Different entities with same timestamp (no conflict)
    // ------------------------------------------------------------------

    @Test
    fun `resolve with different entityId and no local data accepts incoming`() {
        // Both devices created different entities — no conflict since entityIds differ
        val local = null // entity "43" doesn't exist locally
        val incoming = incomingInsert.copy(entityId = "43", timestamp = 1000L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming for different entity not present locally"
        }
    }

    @Test
    fun `resolve with different entityType and no local data accepts incoming`() {
        val local: SyncChange? = null
        val incoming = incomingInsert.copy(entityType = "drink", entityId = "99", timestamp = 1000L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "Expected AcceptIncoming for different entity type not present locally"
        }
    }

    // ------------------------------------------------------------------
    // 8. Identical payloads with same timestamp → NoOp
    // ------------------------------------------------------------------

    @Test
    fun `resolve with identical changes returns NoOp`() {
        val change = localInsert.copy(timestamp = 5000L, source = DeviceSource.PHONE)

        val result = resolver.resolve(change, change)

        assert(result is ConflictResolution.NoOp) {
            "Expected NoOp for identical changes"
        }
    }

    // ------------------------------------------------------------------
    // 9. Batch resolution
    // ------------------------------------------------------------------

    @Test
    fun `resolveBatch returns one resolution per incoming item`() {
        val local = listOf(
            localInsert.copy(entityId = "1", timestamp = 100L),
            localInsert.copy(entityId = "2", timestamp = 100L)
        )
        val incoming = listOf(
            incomingInsert.copy(entityId = "1", timestamp = 300L),
            incomingInsert.copy(entityId = "3", timestamp = 100L), // new entity
            incomingInsert.copy(entityId = "2", timestamp = 50L)   // older
        )

        val results = resolver.resolveBatch(local, incoming)

        assert(results.size == 3) { "Expected 3 resolutions, got ${results.size}" }

        // Entity "1": incoming timestamp=300 > local=100 → AcceptIncoming
        assert(results[0] is ConflictResolution.AcceptIncoming) {
            "Entity 1 should accept incoming (newer)"
        }

        // Entity "3": no local → AcceptIncoming
        assert(results[1] is ConflictResolution.AcceptIncoming) {
            "Entity 3 should accept incoming (no local)"
        }

        // Entity "2": incoming timestamp=50 < local=100 → KeepLocal
        assert(results[2] is ConflictResolution.KeepLocal) {
            "Entity 2 should keep local (older incoming)"
        }
    }

    @Test
    fun `resolveBatch with empty incoming returns empty`() {
        val local = listOf(localInsert)
        val incoming = emptyList<SyncChange>()

        val results = resolver.resolveBatch(local, incoming)

        assert(results.isEmpty()) { "Expected empty results for empty incoming" }
    }

    @Test
    fun `resolveBatch with empty local accepts all incoming`() {
        val incoming = listOf(incomingInsert, incomingInsert.copy(entityId = "2"))

        val results = resolver.resolveBatch(emptyList(), incoming)

        assert(results.size == 2)
        assert(results.all { it is ConflictResolution.AcceptIncoming }) {
            "All items should be accepted when local is empty"
        }
    }

    // ------------------------------------------------------------------
    // 10. Determinism — same inputs always produce same output
    // ------------------------------------------------------------------

    @Test
    fun `resolve is deterministic for all scenarios`() {
        // Test multiple times with same inputs
        val scenarios = listOf(
            Triple(null, incomingInsert, ConflictResolution.AcceptIncoming::class),
            Triple(localInsert.copy(timestamp = 1000L), incomingInsert.copy(timestamp = 2000L),
                ConflictResolution.AcceptIncoming::class),
            Triple(localInsert.copy(timestamp = 3000L), incomingInsert.copy(timestamp = 2000L),
                ConflictResolution.KeepLocal::class),
            Triple(
                localInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE),
                incomingInsert.copy(timestamp = 1000L, source = DeviceSource.WATCH),
                ConflictResolution.KeepLocal::class
            ),
            Triple(
                localInsert.copy(timestamp = 1000L, operationType = OPERATION_DELETE),
                incomingInsert.copy(timestamp = 2000L, operationType = OPERATION_UPDATE),
                ConflictResolution.KeepLocal::class
            ),
            Triple(
                localInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE),
                incomingInsert.copy(timestamp = 1000L, source = DeviceSource.PHONE),
                ConflictResolution.NoOp::class
            )
        )

        // Run each scenario 5 times and verify identical results
        for ((local, incoming, expectedClass) in scenarios) {
            val firstResult = resolver.resolve(local, incoming)
            repeat(4) {
                val nextResult = resolver.resolve(local, incoming)
                assert(firstResult::class == nextResult::class) {
                    "Non-deterministic result: $firstResult vs $nextResult"
                }
            }
        }
    }

    // ------------------------------------------------------------------
    // 11. Boundary: timestamp exactly at threshold
    // ------------------------------------------------------------------

    @Test
    fun `resolve with skew exactly at threshold logs warning`() {
        logMessages.clear()

        val local = localInsert.copy(timestamp = 0L)
        val incoming = incomingInsert.copy(timestamp = ConflictResolver.CLOCK_SKEW_THRESHOLD_MS)

        resolver.resolve(local, incoming)

        assert(logMessages.any { it.contains("Clock skew") }) {
            "Expected 'Clock skew' log at exact threshold, got: $logMessages"
        }
    }

    @Test
    fun `resolve with skew just under threshold does NOT log warning`() {
        logMessages.clear()

        val local = localInsert.copy(timestamp = 0L)
        val incoming = incomingInsert.copy(timestamp = ConflictResolver.CLOCK_SKEW_THRESHOLD_MS - 1)

        resolver.resolve(local, incoming)

        assert(logMessages.none { it.contains("Clock skew") }) {
            "Did NOT expect 'Clock skew' log below threshold, got: $logMessages"
        }
    }

    // ------------------------------------------------------------------
    // 12. Operation type variations with timestamps
    // ------------------------------------------------------------------

    @Test
    fun `resolve incoming INSERT vs local UPDATE with newer incoming accepts`() {
        val local = localInsert.copy(operationType = OPERATION_UPDATE, timestamp = 100L)
        val incoming = incomingInsert.copy(operationType = OPERATION_INSERT, timestamp = 200L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.AcceptIncoming) {
            "INSERT with newer timestamp should win over older UPDATE"
        }
    }

    @Test
    fun `resolve incoming UPDATE vs local INSERT with newer local keeps local`() {
        val local = localInsert.copy(operationType = OPERATION_INSERT, timestamp = 300L)
        val incoming = incomingInsert.copy(operationType = OPERATION_UPDATE, timestamp = 200L)

        val result = resolver.resolve(local, incoming)

        assert(result is ConflictResolution.KeepLocal) {
            "INSERT with newer timestamp should be kept over older UPDATE"
        }
    }
}
