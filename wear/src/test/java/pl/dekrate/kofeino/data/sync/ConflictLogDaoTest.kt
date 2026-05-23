package pl.dekrate.kofeino.data.sync

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import pl.dekrate.kofeino.data.local.CaffeineDatabase
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ConflictLogDaoTest {

    private lateinit var database: CaffeineDatabase
    private lateinit var conflictLogDao: ConflictLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, CaffeineDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        conflictLogDao = database.conflictLogDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `insert single entry and count`() = runTest {
        conflictLogDao.log(makeEntry(entityId = "1"))
        assertEquals(1, conflictLogDao.count())
    }

    @Test
    fun `insert multiple entries getAll returns all`() = runTest {
        conflictLogDao.log(makeEntry(entityId = "1"))
        conflictLogDao.log(makeEntry(entityId = "2"))
        conflictLogDao.log(makeEntry(entityId = "3"))

        val all = conflictLogDao.getAll()
        assertEquals(3, all.size)
    }

    @Test
    fun `getAll returns entries ordered by resolvedAt desc`() = runTest {
        conflictLogDao.log(makeEntry(entityId = "1", resolvedAt = 100))
        conflictLogDao.log(makeEntry(entityId = "2", resolvedAt = 200))
        conflictLogDao.log(makeEntry(entityId = "3", resolvedAt = 300))

        val all = conflictLogDao.getAll()
        assertEquals(3, all.size)
        // Should be ordered by resolvedAt DESC
        assertEquals("3", all[0].entityId)
        assertEquals("2", all[1].entityId)
        assertEquals("1", all[2].entityId)
    }

    @Test
    fun `count is zero for empty table`() = runTest {
        assertEquals(0, conflictLogDao.count())
    }

    @Test
    fun `clearAll removes all entries`() = runTest {
        conflictLogDao.log(makeEntry(entityId = "1"))
        conflictLogDao.log(makeEntry(entityId = "2"))
        assertEquals(2, conflictLogDao.count())

        conflictLogDao.clearAll()
        assertEquals(0, conflictLogDao.count())
    }

    @Test
    fun `entry stores all fields correctly`() = runTest {
        val entry = makeEntry(
            entityType = "drink",
            entityId = "42",
            reason = "incoming_timestamp_newer",
            winningSourceId = "phone"
        )
        conflictLogDao.log(entry)

        val all = conflictLogDao.getAll()
        assertEquals(1, all.size)
        val loaded = all[0]
        assertEquals("drink", loaded.entityType)
        assertEquals("42", loaded.entityId)
        assertEquals("incoming_timestamp_newer", loaded.decisionReason)
        assertEquals("phone", loaded.winningSourceDeviceId)
        assertTrue(loaded.localEntityJson.contains("\"name\""))
        assertTrue(loaded.localEntityJson.contains("\"id\":42"))
        assertTrue(loaded.incomingEntityJson.contains("\"name\""))
        assertTrue(loaded.incomingEntityJson.contains("\"id\":42"))
        assert(loaded.id > 0)
    }

    private val gson = Gson()

    private fun makeEntry(
        entityType: String = "intake",
        entityId: String = "1",
        reason: String = "local_timestamp_newer",
        winningSourceId: String = "watch",
        resolvedAt: Long = System.currentTimeMillis()
    ): ConflictLogEntity {
        val entity = if (entityType == "drink") {
            DrinkEntity(id = entityId.toLongOrNull() ?: 1, name = "test drink", caffeineMg = 50, volumeMl = 250)
        } else {
            CaffeineIntake(
                id = entityId.toLongOrNull() ?: 1,
                drinkName = "test",
                caffeineMg = 50,
                volumeMl = 250,
                timestamp = resolvedAt,
                lastModifiedTimestamp = resolvedAt,
                sourceDeviceId = winningSourceId
            )
        }
        return ConflictLogEntity(
            entityType = entityType,
            entityId = entityId,
            localEntityJson = gson.toJson(entity),
            incomingEntityJson = gson.toJson(entity),
            decisionReason = reason,
            resolvedAt = resolvedAt,
            winningSourceDeviceId = winningSourceId
        )
    }
}
