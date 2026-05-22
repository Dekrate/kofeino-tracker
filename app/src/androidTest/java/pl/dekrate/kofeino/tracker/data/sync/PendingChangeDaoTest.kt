package pl.dekrate.kofeino.tracker.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pl.dekrate.kofeino.tracker.data.local.CaffeineDatabase
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_FAILED
import pl.dekrate.kofeino.tracker.data.sync.PendingChangeEntity.Companion.STATUS_PENDING

/**
 * Integration tests for [PendingChangeDao] using a Room in-memory database.
 *
 * Verifies the persistence layer of the offline pending queue,
 * including insert, update, delete, dedup query, and status transitions.
 */
@RunWith(AndroidJUnit4::class)
class PendingChangeDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: PendingChangeDao

    private val sampleChange = PendingChangeEntity(
        entityType = "intake",
        entityId = "42",
        operationType = "UPDATE",
        payload = """{"caffeineMg":63}""",
        timestamp = 1_000_000L
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.pendingChangeDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------------------
    // Insert & Retrieve
    // ------------------------------------------------------------------

    @Test
    fun insertAndGetById() = runTest {
        val id = dao.insert(sampleChange)

        val retrieved = dao.getPendingChanges().find { it.id == id }
        assert(retrieved != null) { "Expected pending change, got null" }
        assert(retrieved!!.entityType == "intake")
        assert(retrieved.entityId == "42")
        assert(retrieved.operationType == "UPDATE")
        assert(retrieved.payload == """{"caffeineMg":63}""")
        assert(retrieved.timestamp == 1_000_000L)
        assert(retrieved.retryCount == 0)
        assert(retrieved.status == STATUS_PENDING)
    }

    @Test
    fun insertMultipleAndCount() = runTest {
        dao.insert(sampleChange)
        dao.insert(sampleChange.copy(entityId = "43", operationType = "DELETE"))
        dao.insert(sampleChange.copy(entityType = "drink", entityId = "7", operationType = "INSERT"))

        assert(dao.count() == 3) { "Expected 3 rows, got ${dao.count()}" }
    }

    @Test
    fun getPendingChangesReturnsOnlyPending() = runTest {
        val change1 = sampleChange
        val change2 = sampleChange.copy(entityId = "2", retryCount = 5, status = STATUS_FAILED)

        dao.insert(change1)
        dao.insert(change2)

        val pending = dao.getPendingChanges()
        assert(pending.size == 1) { "Expected 1 PENDING change, got ${pending.size}" }
        assert(pending[0].entityId == "42")
    }

    @Test
    fun getPendingChangesOrderedByTimestamp() = runTest {
        dao.insert(sampleChange.copy(entityId = "a", timestamp = 3000L))
        dao.insert(sampleChange.copy(entityId = "b", timestamp = 1000L))
        dao.insert(sampleChange.copy(entityId = "c", timestamp = 2000L))

        val pending = dao.getPendingChanges()
        assert(pending.size == 3)
        assert(pending[0].entityId == "b") { "Expected 'b' first (ts=1000), got '${pending[0].entityId}'" }
        assert(pending[1].entityId == "c") { "Expected 'c' second (ts=2000), got '${pending[1].entityId}'" }
        assert(pending[2].entityId == "a") { "Expected 'a' third (ts=3000), got '${pending[2].entityId}'" }
    }

    // ------------------------------------------------------------------
    // Update
    // ------------------------------------------------------------------

    @Test
    fun updateChangesRetryCountAndStatus() = runTest {
        val id = dao.insert(sampleChange)

        dao.update(sampleChange.copy(id = id, retryCount = 3, status = STATUS_FAILED))

        val all = dao.getPendingChanges()
        assert(all.isEmpty()) { "FAILED items should not appear in PENDING query" }

        val failed = dao.getFailedChanges()
        assert(failed.size == 1)
        assert(failed[0].retryCount == 3)
        assert(failed[0].status == STATUS_FAILED)
    }

    // ------------------------------------------------------------------
    // Delete
    // ------------------------------------------------------------------

    @Test
    fun deleteRemovesRow() = runTest {
        val id = dao.insert(sampleChange)

        dao.deleteById(id)

        assert(dao.count() == 0) { "Expected 0 after delete, got ${dao.count()}" }
    }

    @Test
    fun deleteAllClearsQueue() = runTest {
        dao.insert(sampleChange)
        dao.insert(sampleChange.copy(entityId = "2"))

        dao.deleteAll()

        assert(dao.count() == 0) { "Expected 0 after deleteAll, got ${dao.count()}" }
    }

    // ------------------------------------------------------------------
    // Dedup query — getPendingByEntity
    // ------------------------------------------------------------------

    @Test
    fun getPendingByEntityReturnsCorrectRow() = runTest {
        dao.insert(sampleChange)
        dao.insert(sampleChange.copy(entityType = "drink", entityId = "7"))

        val result = dao.getPendingByEntity("intake", "42")
        assert(result != null) { "Expected change for intake/42" }
        assert(result!!.entityId == "42")
    }

    @Test
    fun getPendingByEntityReturnsNullForNonExistent() = runTest {
        val result = dao.getPendingByEntity("intake", "999")
        assert(result == null) { "Expected null for non-existent entity" }
    }

    @Test
    fun getPendingByEntityIgnoresFailedItems() = runTest {
        dao.insert(sampleChange.copy(retryCount = 5, status = STATUS_FAILED))

        val result = dao.getPendingByEntity("intake", "42")
        assert(result == null) { "FAILED items should not be returned as pending" }
    }

    @Test
    fun getPendingByEntityMatchesSendingItems() = runTest {
        dao.insert(sampleChange.copy(status = "SENDING"))

        val result = dao.getPendingByEntity("intake", "42")
        assert(result != null) { "SENDING items should match dedup query" }
        assert(result!!.entityId == "42")
    }

    @Test
    fun getPendingByEntityMatchesPendingAndSending() = runTest {
        // Only SENDING item for this entity
        dao.insert(sampleChange.copy(entityId = "99", status = "SENDING"))

        val result = dao.getPendingByEntity("intake", "99")
        assert(result != null) { "Should find SENDING item" }
        assert(result!!.status == "SENDING")
    }

    // ------------------------------------------------------------------
    // Failed changes
    // ------------------------------------------------------------------

    @Test
    fun getFailedChangesReturnsOnlyFailed() = runTest {
        dao.insert(sampleChange)
        dao.insert(sampleChange.copy(entityId = "2", retryCount = 5, status = STATUS_FAILED))
        dao.insert(sampleChange.copy(entityId = "3", retryCount = 5, status = STATUS_FAILED))

        val failed = dao.getFailedChanges()
        assert(failed.size == 2) { "Expected 2 FAILED, got ${failed.size}" }
    }

    // ------------------------------------------------------------------
    // App restart simulation — data survives between instances
    // ------------------------------------------------------------------

    @Test
    fun dataSurvivesDatabaseReopen() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Write via first instance
        val id = dao.insert(sampleChange)

        // Close and reopen
        database.close()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java).build()
        dao = database.pendingChangeDao()

        // Read from second instance
        val pending = dao.getPendingChanges()
        assert(pending.size == 1) { "Expected 1 item after reopen, got ${pending.size}" }
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun emptyQueueReturnsEmptyPendingList() = runTest {
        val pending = dao.getPendingChanges()
        assert(pending.isEmpty()) { "Expected empty list for empty queue" }
    }

    @Test
    fun zeroFailedCountOnEmptyQueue() = runTest {
        assert(dao.countFailed() == 0) { "Expected 0 failed on empty queue" }
    }

    // ------------------------------------------------------------------
    // Retryable failed — items with budget left
    // ------------------------------------------------------------------

    @Test
    fun getRetryableFailedReturnsOnlyItemsWithRemainingBudget() = runTest {
        dao.insert(sampleChange) // PENDING, not FAILED
        dao.insert(sampleChange.copy(entityId = "2", retryCount = 5, status = STATUS_FAILED)) // exhausted
        dao.insert(sampleChange.copy(entityId = "3", retryCount = 2, status = STATUS_FAILED)) // budget left

        val retryable = dao.getRetryableFailed()
        assert(retryable.size == 1) { "Expected 1 retryable, got ${retryable.size}" }
        assert(retryable[0].entityId == "3")
    }

    @Test
    fun getRetryableFailedReturnsEmptyWhenAllExhausted() = runTest {
        dao.insert(sampleChange.copy(entityId = "e1", retryCount = 5, status = STATUS_FAILED))
        dao.insert(sampleChange.copy(entityId = "e2", retryCount = 5, status = STATUS_FAILED))

        val retryable = dao.getRetryableFailed()
        assert(retryable.isEmpty()) { "Expected empty when all exhausted" }
    }

    @Test
    fun getRetryableFailedReturnsEmptyWhenNoFailedItems() = runTest {
        dao.insert(sampleChange)

        val retryable = dao.getRetryableFailed()
        assert(retryable.isEmpty()) { "Expected empty when no FAILED items" }
    }
}
