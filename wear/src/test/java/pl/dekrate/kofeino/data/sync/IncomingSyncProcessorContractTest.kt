package pl.dekrate.kofeino.data.sync

import com.google.android.gms.wearable.MessageEvent
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.data.local.CaffeineIntakeDao
import pl.dekrate.kofeino.data.local.DrinkDao
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import java.util.concurrent.Executor

/**
 * Contract tests for wear-module [IncomingSyncProcessor].
 *
 * Tests verify the complete sync processing pipeline:
 * 1. Path parsing correctly extracts entity and operation type
 * 2. Incoming JSON is deserialized via [SyncPayloadSerializer]
 * 3. Conflict resolution is applied via [ConflictResolver] (real static methods)
 * 4. Correct DAO operations are performed based on conflict result
 * 5. Errors (malformed JSON, unknown path) result in graceful IGNORED
 *
 * Uses real [SyncPayloadSerializer] and [ConflictResolver] — only DAOs and
 * database are mocked. This makes these true contract tests that verify
 * the integration between processor, serializer, and resolver.
 */
class IncomingSyncProcessorContractTest {

    private val database: CaffeineDatabase = mockk(relaxed = true)
    private val intakeDao: CaffeineIntakeDao = mockk()
    private val drinkDao: DrinkDao = mockk()
    private val conflictLogDao: ConflictLogDao = mockk(relaxed = true)
    private lateinit var processor: IncomingSyncProcessor

    @Before
    fun setUp() {
        // Stub transactionExecutor so Room's withTransaction runs synchronously
        every { database.transactionExecutor } returns Executor { it.run() }
        processor = IncomingSyncProcessor(database, intakeDao, drinkDao, conflictLogDao)
    }

    // ------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------

    private fun mockMessageEvent(path: String, data: ByteArray = ByteArray(0)): MessageEvent {
        val event = mockk<MessageEvent>()
        every { event.path } returns path
        every { event.data } returns data
        every { event.sourceNodeId } returns "test-phone"
        return event
    }

    // ======================================================================
    // Contract 1: Path parsing
    // ======================================================================

    @Test
    fun `returns IGNORED for non-sync path`() = runTest {
        val result = processor.processIncoming(mockMessageEvent("/data/something"))
        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    @Test
    fun `returns IGNORED for malformed sync path`() = runTest {
        val result = processor.processIncoming(mockMessageEvent("/sync/only-one-segment"))
        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    @Test
    fun `returns IGNORED for unknown entity type`() = runTest {
        val result = processor.processIncoming(
            mockMessageEvent("/sync/unknown/insert", """{}""".toByteArray())
        )
        assert(result == IncomingSyncProcessor.ProcessResult.IGNORED)
    }

    // ======================================================================
    // Contract 2: Intake INSERT — no existing local entity
    // ======================================================================

    @Test
    fun `applies intake INSERT when no local entity exists`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":2000,"sourceDeviceId":"phone"}"""
        coEvery { intakeDao.getIntakeById(42) } returns null
        coEvery { intakeDao.insert(any()) } returns 42L

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/insert", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.insert(match { it.id == 42L && it.caffeineMg == 63 }) }
    }

    // ======================================================================
    // Contract 3: Intake UPDATE — existing entity, incoming wins (newer)
    // ======================================================================

    @Test
    fun `applies intake UPDATE when incoming is newer`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":5000,"sourceDeviceId":"phone"}"""
        val existing = CaffeineIntake(
            id = 42, drinkName = "Old Espresso", caffeineMg = 50, volumeMl = 25,
            timestamp = 1000, lastModifiedTimestamp = 2000, sourceDeviceId = "watch"
        )
        coEvery { intakeDao.getIntakeById(42) } returns existing
        coEvery { intakeDao.update(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/update", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.update(match { it.caffeineMg == 63 && it.drinkName == "Espresso" }) }
    }

    @Test
    fun `applies intake UPDATE when incoming is newer than local`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":2000,"lastModifiedTimestamp":5000,"sourceDeviceId":"phone"}"""
        val local = CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 50, volumeMl = 30,
            timestamp = 2000, lastModifiedTimestamp = 2000, sourceDeviceId = "watch"
        )
        coEvery { intakeDao.getIntakeById(42) } returns local
        coEvery { intakeDao.update(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/update", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.update(match { it.lastModifiedTimestamp == 5000L }) }
    }

    // ======================================================================
    // Contract 4: Intake UPDATE — local keeps its version (local is newer)
    // ======================================================================

    @Test
    fun `keeps local version when local is newer`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Stale","caffeineMg":30,"volumeMl":20,"timestamp":1000,"lastModifiedTimestamp":1000,"sourceDeviceId":"phone"}"""
        val local = CaffeineIntake(
            id = 42, drinkName = "Current", caffeineMg = 63, volumeMl = 30,
            timestamp = 2000, lastModifiedTimestamp = 5000, sourceDeviceId = "watch"
        )
        coEvery { intakeDao.getIntakeById(42) } returns local
        coEvery { intakeDao.update(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/update", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        // Winner should be local (newer timestamp)
        coVerify { intakeDao.update(match { it.lastModifiedTimestamp == 5000L }) }
    }

    // ======================================================================
    // Contract 5: Intake DELETE
    // ======================================================================

    @Test
    fun `applies intake DELETE when entity exists`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":2000,"sourceDeviceId":"phone"}"""
        val existing = CaffeineIntake(
            id = 42, drinkName = "Espresso", caffeineMg = 63, volumeMl = 30,
            timestamp = 1000, lastModifiedTimestamp = 1000, sourceDeviceId = "watch"
        )
        coEvery { intakeDao.getIntakeById(42) } returns existing
        coEvery { intakeDao.delete(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/delete", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { intakeDao.delete(existing) }
    }

    @Test
    fun `handles intake DELETE when entity not found locally`() = runTest {
        val payload = """{"id":999,"drinkId":null,"drinkName":"Ghost","caffeineMg":0,"volumeMl":0,"timestamp":0,"lastModifiedTimestamp":0,"sourceDeviceId":"phone"}"""
        coEvery { intakeDao.getIntakeById(999) } returns null

        val result = processor.processIncoming(
            mockMessageEvent("/sync/intake/delete", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify(exactly = 0) { intakeDao.delete(any()) }
    }

    // ======================================================================
    // Contract 6: Drink INSERT, UPDATE, DELETE
    // ======================================================================

    @Test
    fun `applies drink INSERT when no local entity exists`() = runTest {
        val payload = """{"id":7,"name":"Latte","caffeineMg":63,"volumeMl":200,"isDefault":false,"lastModifiedTimestamp":1000,"sourceDeviceId":"phone"}"""
        coEvery { drinkDao.getDrinkById(7) } returns null
        coEvery { drinkDao.insert(any()) } returns 7L

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/insert", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.insert(match { it.id == 7L && it.name == "Latte" }) }
    }

    @Test
    fun `applies drink UPDATE when incoming is newer`() = runTest {
        val payload = """{"id":7,"name":"Latte Grande","caffeineMg":95,"volumeMl":350,"isDefault":false,"lastModifiedTimestamp":5000,"sourceDeviceId":"phone"}"""
        val existing = DrinkEntity(id = 7, name = "Latte", caffeineMg = 63, volumeMl = 200,
            lastModifiedTimestamp = 2000, sourceDeviceId = "watch")
        coEvery { drinkDao.getDrinkById(7) } returns existing
        coEvery { drinkDao.update(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/update", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.update(match { it.name == "Latte Grande" }) }
    }

    @Test
    fun `applies drink DELETE when entity exists`() = runTest {
        val payload = """{"id":7,"name":"Latte","caffeineMg":63,"volumeMl":200,"isDefault":false,"lastModifiedTimestamp":1000,"sourceDeviceId":"phone"}"""
        val existing = DrinkEntity(id = 7, name = "Latte", caffeineMg = 63, volumeMl = 200,
            lastModifiedTimestamp = 1000, sourceDeviceId = "watch")
        coEvery { drinkDao.getDrinkById(7) } returns existing
        coEvery { drinkDao.delete(any()) } returns Unit

        val result = processor.processIncoming(
            mockMessageEvent("/sync/drink/delete", payload.toByteArray())
        )

        assert(result == IncomingSyncProcessor.ProcessResult.APPLIED)
        coVerify { drinkDao.delete(existing) }
    }

    // ======================================================================
    // Contract 7: Malformed payload → IGNORED
    // ======================================================================

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

    // ======================================================================
    // Contract 8: Conflict logging
    // ======================================================================

    @Test
    fun `logs conflict when local entity exists and incoming differs`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":5000,"sourceDeviceId":"phone"}"""
        val local = CaffeineIntake(
            id = 42, drinkName = "Older", caffeineMg = 30, volumeMl = 20,
            timestamp = 1000, lastModifiedTimestamp = 1000, sourceDeviceId = "watch"
        )
        coEvery { intakeDao.getIntakeById(42) } returns local
        coEvery { intakeDao.update(any()) } returns Unit

        processor.processIncoming(
            mockMessageEvent("/sync/intake/update", payload.toByteArray())
        )

        coVerify(exactly = 1) { conflictLogDao.log(any()) }
    }

    @Test
    fun `no conflict logged when no local entity exists`() = runTest {
        val payload = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":2000,"sourceDeviceId":"phone"}"""
        coEvery { intakeDao.getIntakeById(42) } returns null
        coEvery { intakeDao.insert(any()) } returns 42L

        processor.processIncoming(
            mockMessageEvent("/sync/intake/insert", payload.toByteArray())
        )

        coVerify(exactly = 0) { conflictLogDao.log(any()) }
    }

    // ======================================================================
    // Contract 9: DELETE on non-existent entity is no-conflict
    // ======================================================================

    @Test
    fun `intake DELETE on non-existent entity does not log conflict`() = runTest {
        val payload = """{"id":999,"drinkId":null,"drinkName":"Ghost","caffeineMg":0,"volumeMl":0,"timestamp":0,"lastModifiedTimestamp":0,"sourceDeviceId":"phone"}"""
        coEvery { intakeDao.getIntakeById(999) } returns null

        processor.processIncoming(
            mockMessageEvent("/sync/intake/delete", payload.toByteArray())
        )

        coVerify(exactly = 0) { conflictLogDao.log(any()) }
    }
}
