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
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogEntry.Companion.RESOLUTION_ACCEPT_INCOMING
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogEntry.Companion.RESOLUTION_KEEP_LOCAL
import pl.dekrate.kofeino.tracker.data.sync.ConflictLogEntry.Companion.RESOLUTION_NO_OP

/**
 * Integration tests for [ConflictLogDao] using a Room in-memory database.
 *
 * Verifies that conflict resolution history is correctly persisted,
 * queried by entity, and retrievable for audit/debug purposes.
 */
@RunWith(AndroidJUnit4::class)
class ConflictLogDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var dao: ConflictLogDao

    private val sampleEntry = ConflictLogEntry(
        entityType = "intake",
        entityId = "42",
        localOperationType = "UPDATE",
        incomingOperationType = "UPDATE",
        localTimestamp = 1000L,
        incomingTimestamp = 2000L,
        localSource = "PHONE",
        incomingSource = "WATCH",
        resolution = RESOLUTION_ACCEPT_INCOMING,
        clockSkewMs = 0L,
        resolvedAt = 5000L
    )

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            CaffeineDatabase::class.java
        ).build()
        dao = database.conflictLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------------------
    // Insert & Retrieve
    // ------------------------------------------------------------------

    @Test
    fun insertAndRetrieveById() = runTest {
        val id = dao.insert(sampleEntry)

        val all = dao.getAll()
        assert(all.size == 1) { "Expected 1 entry, got ${all.size}" }
        assert(all[0].id == id)
        assert(all[0].entityType == "intake")
        assert(all[0].entityId == "42")
        assert(all[0].localOperationType == "UPDATE")
        assert(all[0].incomingOperationType == "UPDATE")
        assert(all[0].localTimestamp == 1000L)
        assert(all[0].incomingTimestamp == 2000L)
        assert(all[0].localSource == "PHONE")
        assert(all[0].incomingSource == "WATCH")
        assert(all[0].resolution == RESOLUTION_ACCEPT_INCOMING)
        assert(all[0].clockSkewMs == 0L)
    }

    @Test
    fun countReturnsCorrectNumberOfEntries() = runTest {
        assert(dao.count() == 0) { "Expected 0 in empty log" }

        dao.insert(sampleEntry)
        assert(dao.count() == 1)

        dao.insert(sampleEntry.copy(entityId = "43"))
        assert(dao.count() == 2)
    }

    // ------------------------------------------------------------------
    // Query by entity
    // ------------------------------------------------------------------

    @Test
    fun getByEntityReturnsEntriesForSpecificEntity() = runTest {
        dao.insert(sampleEntry) // intake/42
        dao.insert(sampleEntry.copy(entityId = "43")) // intake/43
        dao.insert(sampleEntry.copy(entityType = "drink", entityId = "7")) // drink/7

        val intake42 = dao.getByEntity("intake", "42")
        assert(intake42.size == 1) { "Expected 1 entry for intake/42, got ${intake42.size}" }

        val drink7 = dao.getByEntity("drink", "7")
        assert(drink7.size == 1) { "Expected 1 entry for drink/7, got ${drink7.size}" }

        val nonExistent = dao.getByEntity("intake", "999")
        assert(nonExistent.isEmpty()) { "Expected 0 for non-existent entity" }
    }

    @Test
    fun getByEntityReturnsMultipleEntriesOrderedByResolvedAtDesc() = runTest {
        dao.insert(sampleEntry.copy(resolvedAt = 1000L)) // oldest
        dao.insert(sampleEntry.copy(resolvedAt = 3000L)) // newest
        dao.insert(sampleEntry.copy(resolvedAt = 2000L)) // middle

        val entries = dao.getByEntity("intake", "42")

        assert(entries.size == 3)
        assert(entries[0].resolvedAt == 3000L) { "Expected newest first, got ${entries[0].resolvedAt}" }
        assert(entries[1].resolvedAt == 2000L)
        assert(entries[2].resolvedAt == 1000L)
    }

    // ------------------------------------------------------------------
    // getAll ordering
    // ------------------------------------------------------------------

    @Test
    fun getAllReturnsAllEntriesOrderedByResolvedAtDesc() = runTest {
        dao.insert(sampleEntry.copy(resolvedAt = 100L))
        dao.insert(sampleEntry.copy(entityType = "drink", resolvedAt = 300L))
        dao.insert(sampleEntry.copy(entityId = "99", resolvedAt = 200L))

        val all = dao.getAll()

        assert(all.size == 3)
        assert(all[0].resolvedAt == 300L) // newest first
        assert(all[1].resolvedAt == 200L)
        assert(all[2].resolvedAt == 100L)
    }

    // ------------------------------------------------------------------
    // Clock-skew queries
    // ------------------------------------------------------------------

    @Test
    fun getClockSkewEntriesReturnsOnlyEntriesWithSkew() = runTest {
        dao.insert(sampleEntry) // clockSkewMs = 0
        dao.insert(sampleEntry.copy(entityId = "skew1", clockSkewMs = 120_000L))
        dao.insert(sampleEntry.copy(entityId = "skew2", clockSkewMs = 300_000L))

        val skewEntries = dao.getClockSkewEntries()

        assert(skewEntries.size == 2) { "Expected 2 clock-skew entries, got ${skewEntries.size}" }
        assert(skewEntries.all { it.clockSkewMs > 0L })
    }

    @Test
    fun getClockSkewEntriesReturnsEmptyWhenNoSkew() = runTest {
        dao.insert(sampleEntry)
        dao.insert(sampleEntry.copy(entityId = "2"))

        val skewEntries = dao.getClockSkewEntries()
        assert(skewEntries.isEmpty()) { "Expected 0 clock-skew entries" }
    }

    // ------------------------------------------------------------------
    // Resolution type variants
    // ------------------------------------------------------------------

    @Test
    fun insertAllResolutionTypes() = runTest {
        val keepLocal = sampleEntry.copy(resolution = RESOLUTION_KEEP_LOCAL)
        val acceptIncoming = sampleEntry.copy(resolution = RESOLUTION_ACCEPT_INCOMING)
        val noOp = sampleEntry.copy(resolution = RESOLUTION_NO_OP)

        dao.insert(keepLocal)
        dao.insert(acceptIncoming)
        dao.insert(noOp)

        assert(dao.count() == 3)

        val all = dao.getAll()
        val resolutions = all.map { it.resolution }.toSet()
        assert(resolutions.containsAll(setOf(RESOLUTION_KEEP_LOCAL, RESOLUTION_ACCEPT_INCOMING, RESOLUTION_NO_OP)))
    }

    // ------------------------------------------------------------------
    // Delete all (cleanup)
    // ------------------------------------------------------------------

    @Test
    fun deleteAllClearsLog() = runTest {
        dao.insert(sampleEntry)
        dao.insert(sampleEntry.copy(entityId = "2"))

        assert(dao.count() == 2)

        dao.deleteAll()

        assert(dao.count() == 0) { "Expected 0 after deleteAll" }
        assert(dao.getAll().isEmpty())
    }

    // ------------------------------------------------------------------
    // Edge cases
    // ------------------------------------------------------------------

    @Test
    fun emptyLogReturnsEmptyForAllQueries() = runTest {
        assert(dao.getAll().isEmpty())
        assert(dao.getByEntity("intake", "1").isEmpty())
        assert(dao.getClockSkewEntries().isEmpty())
        assert(dao.count() == 0)
    }

    @Test
    fun insertWithMaxResolutionValues() = runTest {
        val entry = ConflictLogEntry(
            entityType = "a".repeat(50),
            entityId = "b".repeat(50),
            localOperationType = "UPDATE",
            incomingOperationType = "DELETE",
            localTimestamp = Long.MAX_VALUE,
            incomingTimestamp = Long.MIN_VALUE,
            localSource = "WATCH",
            incomingSource = "PHONE",
            resolution = RESOLUTION_KEEP_LOCAL,
            clockSkewMs = Long.MAX_VALUE,
            resolvedAt = Long.MAX_VALUE
        )

        val id = dao.insert(entry)
        assert(id > 0) { "Expected positive id, got $id" }

        val all = dao.getAll()
        assert(all.size == 1)
        assert(all[0].entityType.length == 50)
        assert(all[0].clockSkewMs == Long.MAX_VALUE)
    }
}
