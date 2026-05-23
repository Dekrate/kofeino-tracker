package pl.dekrate.kofeino.common.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncDataMapKeysTest {

    // ─── Common keys ─────────────────────────────────────────────────────

    @Test
    fun `KEY_ID should be id`() {
        assertEquals("id", SyncDataMapKeys.KEY_ID)
    }

    @Test
    fun `KEY_LAST_MODIFIED_TIMESTAMP should be lastModifiedTimestamp`() {
        assertEquals("lastModifiedTimestamp", SyncDataMapKeys.KEY_LAST_MODIFIED_TIMESTAMP)
    }

    @Test
    fun `KEY_SOURCE_DEVICE_ID should be sourceDeviceId`() {
        assertEquals("sourceDeviceId", SyncDataMapKeys.KEY_SOURCE_DEVICE_ID)
    }

    @Test
    fun `common keys should be unique`() {
        val keys = setOf(
            SyncDataMapKeys.KEY_ID,
            SyncDataMapKeys.KEY_LAST_MODIFIED_TIMESTAMP,
            SyncDataMapKeys.KEY_SOURCE_DEVICE_ID,
        )
        assertEquals(3, keys.size)
    }

    // ─── Intake keys ─────────────────────────────────────────────────────

    @Test
    fun `Intake DRINK_ID should be drinkId`() {
        assertEquals("drinkId", SyncDataMapKeys.Intake.DRINK_ID)
    }

    @Test
    fun `Intake DRINK_NAME should be drinkName`() {
        assertEquals("drinkName", SyncDataMapKeys.Intake.DRINK_NAME)
    }

    @Test
    fun `Intake CAFFEINE_MG should be caffeineMg`() {
        assertEquals("caffeineMg", SyncDataMapKeys.Intake.CAFFEINE_MG)
    }

    @Test
    fun `Intake VOLUME_ML should be volumeMl`() {
        assertEquals("volumeMl", SyncDataMapKeys.Intake.VOLUME_ML)
    }

    @Test
    fun `Intake TIMESTAMP should be timestamp`() {
        assertEquals("timestamp", SyncDataMapKeys.Intake.TIMESTAMP)
    }

    @Test
    fun `intake keys should be unique`() {
        val keys = setOf(
            SyncDataMapKeys.Intake.DRINK_ID,
            SyncDataMapKeys.Intake.DRINK_NAME,
            SyncDataMapKeys.Intake.CAFFEINE_MG,
            SyncDataMapKeys.Intake.VOLUME_ML,
            SyncDataMapKeys.Intake.TIMESTAMP,
        )
        assertEquals(5, keys.size)
    }

    @Test
    fun `intake keys should use camelCase`() {
        assertTrue(SyncDataMapKeys.Intake.DRINK_ID[0].isLowerCase())
        assertTrue(SyncDataMapKeys.Intake.DRINK_NAME[0].isLowerCase())
        assertTrue(SyncDataMapKeys.Intake.CAFFEINE_MG[0].isLowerCase())
    }

    // ─── Drink keys ──────────────────────────────────────────────────────

    @Test
    fun `Drink NAME should be name`() {
        assertEquals("name", SyncDataMapKeys.Drink.NAME)
    }

    @Test
    fun `Drink CAFFEINE_MG should be caffeineMg`() {
        assertEquals("caffeineMg", SyncDataMapKeys.Drink.CAFFEINE_MG)
    }

    @Test
    fun `Drink VOLUME_ML should be volumeMl`() {
        assertEquals("volumeMl", SyncDataMapKeys.Drink.VOLUME_ML)
    }

    @Test
    fun `Drink IS_DEFAULT should be isDefault`() {
        assertEquals("isDefault", SyncDataMapKeys.Drink.IS_DEFAULT)
    }

    @Test
    fun `Drink IS_OFFICIAL should be isOfficial`() {
        assertEquals("isOfficial", SyncDataMapKeys.Drink.IS_OFFICIAL)
    }

    @Test
    fun `drink keys should be unique`() {
        val keys = setOf(
            SyncDataMapKeys.Drink.NAME,
            SyncDataMapKeys.Drink.CAFFEINE_MG,
            SyncDataMapKeys.Drink.VOLUME_ML,
            SyncDataMapKeys.Drink.IS_DEFAULT,
            SyncDataMapKeys.Drink.IS_OFFICIAL,
        )
        assertEquals(5, keys.size)
    }

    // ─── Settings keys ───────────────────────────────────────────────────

    @Test
    fun `Settings CAFFEINE_LIMIT_PROFILE should be caffeineLimitProfile`() {
        assertEquals("caffeineLimitProfile", SyncDataMapKeys.Settings.CAFFEINE_LIMIT_PROFILE)
    }

    @Test
    fun `Settings CUSTOM_LIMIT_MG should be customLimitMg`() {
        assertEquals("customLimitMg", SyncDataMapKeys.Settings.CUSTOM_LIMIT_MG)
    }

    @Test
    fun `Settings LANGUAGE should be language`() {
        assertEquals("language", SyncDataMapKeys.Settings.LANGUAGE)
    }

    @Test
    fun `Settings THEME should be theme`() {
        assertEquals("theme", SyncDataMapKeys.Settings.THEME)
    }

    @Test
    fun `Settings NOTIFICATION_ENABLED should be notificationEnabled`() {
        assertEquals("notificationEnabled", SyncDataMapKeys.Settings.NOTIFICATION_ENABLED)
    }

    @Test
    fun `settings keys should be unique`() {
        val keys = setOf(
            SyncDataMapKeys.Settings.CAFFEINE_LIMIT_PROFILE,
            SyncDataMapKeys.Settings.CUSTOM_LIMIT_MG,
            SyncDataMapKeys.Settings.LANGUAGE,
            SyncDataMapKeys.Settings.THEME,
            SyncDataMapKeys.Settings.NOTIFICATION_ENABLED,
        )
        assertEquals(5, keys.size)
    }

    // ─── Sync metadata keys ──────────────────────────────────────────────

    @Test
    fun `Sync STATE_HASH should be stateHash`() {
        assertEquals("stateHash", SyncDataMapKeys.Sync.STATE_HASH)
    }

    @Test
    fun `Sync LAST_SYNC_TIMESTAMP should be lastSyncTimestamp`() {
        assertEquals("lastSyncTimestamp", SyncDataMapKeys.Sync.LAST_SYNC_TIMESTAMP)
    }

    @Test
    fun `Sync ENTITY_TYPE should be entityType`() {
        assertEquals("entityType", SyncDataMapKeys.Sync.ENTITY_TYPE)
    }

    @Test
    fun `Sync OPERATION_TYPE should be operationType`() {
        assertEquals("operationType", SyncDataMapKeys.Sync.OPERATION_TYPE)
    }

    @Test
    fun `Sync ENTITIES_JSON should be entitiesJson`() {
        assertEquals("entitiesJson", SyncDataMapKeys.Sync.ENTITIES_JSON)
    }

    @Test
    fun `sync metadata keys should be unique`() {
        val keys = setOf(
            SyncDataMapKeys.Sync.STATE_HASH,
            SyncDataMapKeys.Sync.LAST_SYNC_TIMESTAMP,
            SyncDataMapKeys.Sync.ENTITY_TYPE,
            SyncDataMapKeys.Sync.OPERATION_TYPE,
            SyncDataMapKeys.Sync.ENTITIES_JSON,
        )
        assertEquals(5, keys.size)
    }

    // ─── Cross-cutting tests ─────────────────────────────────────────────

    @Test
    fun `keys should be unique within each group`() {
        val intakeKeys = setOf(
            SyncDataMapKeys.Intake.DRINK_ID,
            SyncDataMapKeys.Intake.DRINK_NAME,
            SyncDataMapKeys.Intake.CAFFEINE_MG,
            SyncDataMapKeys.Intake.VOLUME_ML,
            SyncDataMapKeys.Intake.TIMESTAMP,
        )
        assertEquals("Intake keys should be unique", 5, intakeKeys.size)

        val drinkKeys = setOf(
            SyncDataMapKeys.Drink.NAME,
            SyncDataMapKeys.Drink.CAFFEINE_MG,
            SyncDataMapKeys.Drink.VOLUME_ML,
            SyncDataMapKeys.Drink.IS_DEFAULT,
            SyncDataMapKeys.Drink.IS_OFFICIAL,
        )
        assertEquals("Drink keys should be unique", 5, drinkKeys.size)

        val settingsKeys = setOf(
            SyncDataMapKeys.Settings.CAFFEINE_LIMIT_PROFILE,
            SyncDataMapKeys.Settings.CUSTOM_LIMIT_MG,
            SyncDataMapKeys.Settings.LANGUAGE,
            SyncDataMapKeys.Settings.THEME,
            SyncDataMapKeys.Settings.NOTIFICATION_ENABLED,
        )
        assertEquals("Settings keys should be unique", 5, settingsKeys.size)

        val syncKeys = setOf(
            SyncDataMapKeys.Sync.STATE_HASH,
            SyncDataMapKeys.Sync.LAST_SYNC_TIMESTAMP,
            SyncDataMapKeys.Sync.ENTITY_TYPE,
            SyncDataMapKeys.Sync.OPERATION_TYPE,
            SyncDataMapKeys.Sync.ENTITIES_JSON,
        )
        assertEquals("Sync metadata keys should be unique", 5, syncKeys.size)

        val commonKeys = setOf(
            SyncDataMapKeys.KEY_ID,
            SyncDataMapKeys.KEY_LAST_MODIFIED_TIMESTAMP,
            SyncDataMapKeys.KEY_SOURCE_DEVICE_ID,
        )
        assertEquals("Common keys should be unique", 3, commonKeys.size)
    }

    @Test
    fun `key overlap between Intake and Drink groups is intentional`() {
        assertEquals(
            SyncDataMapKeys.Intake.CAFFEINE_MG,
            SyncDataMapKeys.Drink.CAFFEINE_MG,
        )
        assertEquals(
            SyncDataMapKeys.Intake.VOLUME_ML,
            SyncDataMapKeys.Drink.VOLUME_ML,
        )
    }

    @Test
    fun `all keys should be non-blank`() {
        assertTrue(SyncDataMapKeys.KEY_ID.isNotBlank())
        assertTrue(SyncDataMapKeys.KEY_LAST_MODIFIED_TIMESTAMP.isNotBlank())
        assertTrue(SyncDataMapKeys.KEY_SOURCE_DEVICE_ID.isNotBlank())
        assertTrue(SyncDataMapKeys.Intake.DRINK_ID.isNotBlank())
        assertTrue(SyncDataMapKeys.Intake.DRINK_NAME.isNotBlank())
        assertTrue(SyncDataMapKeys.Intake.CAFFEINE_MG.isNotBlank())
        assertTrue(SyncDataMapKeys.Intake.VOLUME_ML.isNotBlank())
        assertTrue(SyncDataMapKeys.Intake.TIMESTAMP.isNotBlank())
        assertTrue(SyncDataMapKeys.Drink.NAME.isNotBlank())
        assertTrue(SyncDataMapKeys.Drink.CAFFEINE_MG.isNotBlank())
        assertTrue(SyncDataMapKeys.Drink.VOLUME_ML.isNotBlank())
        assertTrue(SyncDataMapKeys.Drink.IS_DEFAULT.isNotBlank())
        assertTrue(SyncDataMapKeys.Drink.IS_OFFICIAL.isNotBlank())
        assertTrue(SyncDataMapKeys.Settings.CAFFEINE_LIMIT_PROFILE.isNotBlank())
        assertTrue(SyncDataMapKeys.Settings.CUSTOM_LIMIT_MG.isNotBlank())
        assertTrue(SyncDataMapKeys.Settings.LANGUAGE.isNotBlank())
        assertTrue(SyncDataMapKeys.Settings.THEME.isNotBlank())
        assertTrue(SyncDataMapKeys.Settings.NOTIFICATION_ENABLED.isNotBlank())
        assertTrue(SyncDataMapKeys.Sync.STATE_HASH.isNotBlank())
        assertTrue(SyncDataMapKeys.Sync.LAST_SYNC_TIMESTAMP.isNotBlank())
        assertTrue(SyncDataMapKeys.Sync.ENTITY_TYPE.isNotBlank())
        assertTrue(SyncDataMapKeys.Sync.OPERATION_TYPE.isNotBlank())
        assertTrue(SyncDataMapKeys.Sync.ENTITIES_JSON.isNotBlank())
    }

    @Test
    fun `keys should match domain model field names`() {
        // CaffeineIntake field names
        assertEquals("id", SyncDataMapKeys.KEY_ID)
        assertEquals("drinkId", SyncDataMapKeys.Intake.DRINK_ID)
        assertEquals("drinkName", SyncDataMapKeys.Intake.DRINK_NAME)
        assertEquals("caffeineMg", SyncDataMapKeys.Intake.CAFFEINE_MG)
        assertEquals("volumeMl", SyncDataMapKeys.Intake.VOLUME_ML)
        assertEquals("timestamp", SyncDataMapKeys.Intake.TIMESTAMP)
        assertEquals("lastModifiedTimestamp", SyncDataMapKeys.KEY_LAST_MODIFIED_TIMESTAMP)
        assertEquals("sourceDeviceId", SyncDataMapKeys.KEY_SOURCE_DEVICE_ID)

        // DrinkEntity field names
        assertEquals("name", SyncDataMapKeys.Drink.NAME)
        assertEquals("isDefault", SyncDataMapKeys.Drink.IS_DEFAULT)
    }
}
