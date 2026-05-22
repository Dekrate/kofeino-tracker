package pl.dekrate.kofeino.tracker.data.sync

import com.google.android.gms.wearable.MessageEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.tracker.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.tracker.data.local.DrinkDao
import pl.dekrate.kofeino.tracker.domain.model.CaffeineIntake
import pl.dekrate.kofeino.tracker.domain.model.DrinkEntity

/**
 * Unit tests for [IncomingSyncProcessor].
 *
 * Mock SyncChange instances use [mockk] with explicit property stubbing for
 * all properties accessed by [IncomingSyncProcessor.logConflict].
 */
@Suppress("TooGenericExceptionCaught")
class IncomingSyncProcessorTest {

    private val resolver: ConflictResolver = mockk()
    private val intakeDao: CaffeineIntakeDao = mockk()
    private val drinkDao: DrinkDao = mockk()
    private val conflictLogDao: ConflictLogDao = mockk(relaxed = true)
    private lateinit var processor: IncomingSyncProcessor

    @Before
    fun setUp() {
        processor = IncomingSyncProcessor(resolver, intakeDao, drinkDao, conflictLogDao)
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private val intakeJson = """{"id":42,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000}"""
    private val drinkJson = """{"id":7,"name":"Latte","caffeineMg":63,"volumeMl":200}"""

    private fun mockMessageEvent(path: String, data: ByteArray = ByteArray(0)): MessageEvent {
        val event = mockk<MessageEvent>()
        every { event.path } returns path
        every { event.data } returns data
        every { event.sourceNodeId } returns "test-watch"
        return event
    }

    /**
     * Create a fully stubbed [SyncChange] for use as a mock conflict-resolution
     * result. All properties accessed by [IncomingSyncProcessor.logConflict]
     * must be stubbed to avoid unstubbed-property exceptions.
     */
    private fun mockSyncChange(
        entityType: String = "intake",
        entityId: String = "42",
        operationType: String = PendingChangeEntity.OPERATION_INSERT,
        timestamp: Long = 1000L,
        source: DeviceSource = DeviceSource.WATCH,
        payload: String = intakeJson
    ): SyncChange = mockk {
        every { this@mockk.entityType } returns entityType
        every { this@mockk.entityId } returns entityId
        every { this@mockk.operationType } returns operationType
        every { this@mockk.timestamp } returns timestamp
        every { this@mockk.source } returns source
        every { this@mockk.payload } returns payload
    }

    // ------------------------------------------------------------------
    // 1. Path parsing — non-sync and malformed
    // ------------------------------------------------------------------

    @Test
    fun `processIncoming returns IGNORED for non-sync path`() = runTest {
        val result = processor.processIncoming(mockMessageEvent("/data/something"))

        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    @Test
    fun `processIncoming returns IGNORED for malformed sync path`() = runTest {
        val result = processor.processIncoming(mockMessageEvent("/sync/only-one-segment"))

        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    @Test
    fun `processIncoming returns IGNORED for unknown entity type`() = runTest {
        val result = processor.processIncoming(
            mockMessageEvent("/sync/unknown/insert", """{}""".toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    // ------------------------------------------------------------------
    // 2. Intake — AcceptIncoming → applied via DAO
    // ------------------------------------------------------------------

    @Test
    fun `applies intake INSERT when AcceptIncoming`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns null
        coEvery { intakeDao.insert(any()) } returns 42L
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(operationType = PendingChangeEntity.OPERATION_INSERT)
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/insert", intakeJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.insert(match { it.id == 42L && it.caffeineMg == 63 }) }
    }

    @Test
    fun `applies intake UPDATE when AcceptIncoming and entity exists`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000
        )
        coEvery { intakeDao.update(any()) } returns Unit
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(operationType = PendingChangeEntity.OPERATION_UPDATE)
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/update", intakeJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.update(match { it.id == 42L }) }
    }

    @Test
    fun `applies intake DELETE when AcceptIncoming`() = runTest {
        val existing = CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000
        )
        coEvery { intakeDao.getIntakeById(42) } returns existing
        coEvery { intakeDao.delete(any()) } returns Unit
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(operationType = PendingChangeEntity.OPERATION_DELETE)
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/delete", intakeJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.delete(existing) }
    }

    // ------------------------------------------------------------------
    // 3. KeepLocal → skip
    // ------------------------------------------------------------------

    @Test
    fun `skips intake when KeepLocal`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 3000
        )
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.KeepLocal(
            mockSyncChange(operationType = PendingChangeEntity.OPERATION_UPDATE)
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/update", intakeJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.SKIPPED)
        coVerify(exactly = 0) { intakeDao.insert(any()) }
        coVerify(exactly = 0) { intakeDao.update(any()) }
        coVerify(exactly = 0) { intakeDao.delete(any()) }
    }

    // ------------------------------------------------------------------
    // 4. NoOp → skip, no conflict log
    // ------------------------------------------------------------------

    @Test
    fun `does not log conflict for NoOp`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 1000
        )
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.NoOp(
            mockSyncChange()
        )

        processor.processIncoming(
            mockMessageEvent("/sync/intake/update", intakeJson.toByteArray())
        )

        coVerify(exactly = 0) { conflictLogDao.insert(any()) }
    }

    // ------------------------------------------------------------------
    // 5. Drink — AcceptIncoming → applied
    // ------------------------------------------------------------------

    @Test
    fun `applies drink INSERT when AcceptIncoming`() = runTest {
        coEvery { drinkDao.getDrinkById(7) } returns null
        coEvery { drinkDao.insert(any()) } returns 7L
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(
                entityType = "drink", entityId = "7",
                operationType = PendingChangeEntity.OPERATION_INSERT,
                payload = drinkJson
            )
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/insert", drinkJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.insert(match { it.id == 7L && it.name == "Latte" }) }
    }

    @Test
    fun `applies drink DELETE when AcceptIncoming`() = runTest {
        val existing = DrinkEntity(id = 7, name = "Latte", caffeineMg = 63, volumeMl = 200)
        coEvery { drinkDao.getDrinkById(7) } returns existing
        coEvery { drinkDao.delete(any()) } returns Unit
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(
                entityType = "drink", entityId = "7",
                operationType = PendingChangeEntity.OPERATION_DELETE,
                payload = drinkJson
            )
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/delete", drinkJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.delete(existing) }
    }

    @Test
    fun `applies drink UPDATE when AcceptIncoming and entity exists`() = runTest {
        coEvery { drinkDao.getDrinkById(7) } returns DrinkEntity(
            id = 7, name = "Latte", caffeineMg = 63, volumeMl = 200
        )
        coEvery { drinkDao.update(any()) } returns Unit
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(
                entityType = "drink", entityId = "7",
                operationType = PendingChangeEntity.OPERATION_UPDATE,
                payload = drinkJson
            )
        )

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/update", drinkJson.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.update(match { it.id == 7L }) }
    }

    // ------------------------------------------------------------------
    // 6. Malformed payload → IGNORED
    // ------------------------------------------------------------------

    @Test
    fun `returns IGNORED for malformed intake payload`() = runTest {
        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/insert", """not-json""".toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    @Test
    fun `returns IGNORED for malformed drink payload`() = runTest {
        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/update", """{bad json""".toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    // ------------------------------------------------------------------
    // 7. Conflict logging
    // ------------------------------------------------------------------

    @Test
    fun `logs conflict on AcceptIncoming`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns null
        coEvery { intakeDao.insert(any()) } returns 42L
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(
                entityType = "intake", entityId = "42",
                operationType = PendingChangeEntity.OPERATION_INSERT,
                timestamp = 1000L, source = DeviceSource.WATCH,
                payload = intakeJson
            )
        )

        processor.processIncoming(
            mockMessageEvent("/sync/intake/insert", intakeJson.toByteArray())
        )

        coVerify {
            conflictLogDao.insert(match {
                it.entityType == "intake" &&
                    it.resolution == ConflictLogEntry.RESOLUTION_ACCEPT_INCOMING
            })
        }
    }

    @Test
    fun `logs conflict on KeepLocal`() = runTest {
        coEvery { intakeDao.getIntakeById(42) } returns CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30, timestamp = 3000
        )
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.KeepLocal(
            mockSyncChange(
                entityType = "intake", entityId = "42",
                operationType = PendingChangeEntity.OPERATION_UPDATE,
                timestamp = 1000L, source = DeviceSource.WATCH,
                payload = intakeJson
            )
        )

        processor.processIncoming(
            mockMessageEvent("/sync/intake/update", intakeJson.toByteArray())
        )

        coVerify {
            conflictLogDao.insert(match {
                it.resolution == ConflictLogEntry.RESOLUTION_KEEP_LOCAL
            })
        }
    }

    // ------------------------------------------------------------------
    // 8. Edge: DELETE when entity does not exist locally
    // ------------------------------------------------------------------

    @Test
    fun `handles intake DELETE when entity not found locally`() = runTest {
        coEvery { intakeDao.getIntakeById(999) } returns null
        coEvery { resolver.resolve(any(), any()) } returns ConflictResolution.AcceptIncoming(
            mockSyncChange(
                entityId = "999",
                operationType = PendingChangeEntity.OPERATION_DELETE
            )
        )

        val payload = """{"id":999,"drinkName":"Ghost","caffeineMg":0,"volumeMl":0,"timestamp":0}"""
        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/delete", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify(exactly = 0) { intakeDao.delete(any()) }
    }
}
