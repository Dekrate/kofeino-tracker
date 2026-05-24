package pl.dekrate.kofeino.data.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.dekrate.kofeino.domain.model.CaffeineIntake
import pl.dekrate.kofeino.domain.model.DrinkEntity

/**
 * Schema conformance tests for [SyncPayloadSerializer].
 *
 * These tests serve as a **contract** between the sync protocol's
 * serialization layer and the domain models. They verify that:
 *
 * - All fields of [CaffeineIntake] and [DrinkEntity] survive roundtrip
 * - JSON field names match Kotlin property names (Gson default behavior)
 * - No fields are silently dropped or mangled during serialization
 * - The structure is compatible with the peer device's expectations
 *
 * If any of these tests fail, the sync protocol has drifted and the
 * peer device (phone app) MUST be updated to match.
 */
class SyncPayloadSerializerSchemaTest {

    // ======================================================================
    // CaffeineIntake schema
    // ======================================================================

    @Test
    fun `serializeIntake includes all expected fields`() {
        val intake = CaffeineIntake(
            id = 42,
            drinkId = 1,
            drinkName = "Espresso",
            caffeineMg = 63,
            volumeMl = 30,
            timestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "phone"
        )

        val json = SyncPayloadSerializer.serializeIntake(intake)

        assertTrue("JSON must contain id field", json.contains("\"id\""))
        assertTrue("JSON must contain drinkId field", json.contains("\"drinkId\""))
        assertTrue("JSON must contain drinkName field", json.contains("\"drinkName\""))
        assertTrue("JSON must contain caffeineMg field", json.contains("\"caffeineMg\""))
        assertTrue("JSON must contain volumeMl field", json.contains("\"volumeMl\""))
        assertTrue("JSON must contain timestamp field", json.contains("\"timestamp\""))
        assertTrue("JSON must contain lastModifiedTimestamp field", json.contains("\"lastModifiedTimestamp\""))
        assertTrue("JSON must contain sourceDeviceId field", json.contains("\"sourceDeviceId\""))
    }

    @Test
    fun `serializeIntake includes correct field values`() {
        val intake = CaffeineIntake(
            id = 42,
            drinkId = 1,
            drinkName = "Cappuccino",
            caffeineMg = 80,
            volumeMl = 200,
            timestamp = 1000L,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "watch"
        )

        val json = SyncPayloadSerializer.serializeIntake(intake)

        assertTrue(json.contains("\"id\":42"))
        assertTrue(json.contains("\"drinkId\":1"))
        assertTrue(json.contains("\"drinkName\":\"Cappuccino\""))
        assertTrue(json.contains("\"caffeineMg\":80"))
        assertTrue(json.contains("\"volumeMl\":200"))
        assertTrue(json.contains("\"timestamp\":1000"))
        assertTrue(json.contains("\"lastModifiedTimestamp\":2000"))
        assertTrue(json.contains("\"sourceDeviceId\":\"watch\""))
    }

    @Test
    fun `serializeIntake handles null drinkId`() {
        val intake = CaffeineIntake(
            id = 1, drinkName = "Black Coffee", caffeineMg = 95, volumeMl = 250,
            timestamp = 1000L, drinkId = null
        )

        // Gson omits null fields by default — verify the field is absent in JSON
        // and that roundtrip preserves the null value
        val json = SyncPayloadSerializer.serializeIntake(intake)
        assertTrue("JSON must not contain drinkId when null (Gson default)",
            !json.contains("\"drinkId\""))

        val deserialized = SyncPayloadSerializer.deserializeIntake(json)
        assertEquals(null, deserialized.drinkId)
    }

    @Test
    fun `deserializeIntake roundtrip preserves all fields`() {
        val original = CaffeineIntake(
            id = 99,
            drinkId = 3,
            drinkName = "Latte Macchiato",
            caffeineMg = 120,
            volumeMl = 300,
            timestamp = 5000L,
            lastModifiedTimestamp = 6000L,
            sourceDeviceId = "phone"
        )

        val json = SyncPayloadSerializer.serializeIntake(original)
        val deserialized = SyncPayloadSerializer.deserializeIntake(json)

        assertEquals("Roundtrip must preserve id", original.id, deserialized.id)
        assertEquals("Roundtrip must preserve drinkId", original.drinkId, deserialized.drinkId)
        assertEquals("Roundtrip must preserve drinkName", original.drinkName, deserialized.drinkName)
        assertEquals("Roundtrip must preserve caffeineMg", original.caffeineMg, deserialized.caffeineMg)
        assertEquals("Roundtrip must preserve volumeMl", original.volumeMl, deserialized.volumeMl)
        assertEquals("Roundtrip must preserve timestamp", original.timestamp, deserialized.timestamp)
        assertEquals("Roundtrip must preserve lastModifiedTimestamp", original.lastModifiedTimestamp, deserialized.lastModifiedTimestamp)
        assertEquals("Roundtrip must preserve sourceDeviceId", original.sourceDeviceId, deserialized.sourceDeviceId)
    }

    @Test
    fun `deserializeIntake handles default values`() {
        val intake = CaffeineIntake(
            id = 1, drinkName = "Black Coffee", caffeineMg = 95, volumeMl = 250,
            timestamp = 1000L
        )
        // lastModifiedTimestamp defaults to System.currentTimeMillis()
        // sourceDeviceId defaults to ""
        // drinkId defaults to null

        val json = SyncPayloadSerializer.serializeIntake(intake)
        val deserialized = SyncPayloadSerializer.deserializeIntake(json)

        // Roundtrip preserves the runtime default of lastModifiedTimestamp
        assertEquals(intake.lastModifiedTimestamp, deserialized.lastModifiedTimestamp)
        assertEquals("", deserialized.sourceDeviceId)
    }

    // ======================================================================
    // DrinkEntity schema
    // ======================================================================

    @Test
    fun `serializeDrink includes all expected fields`() {
        val drink = DrinkEntity(
            id = 7,
            name = "Latte",
            caffeineMg = 63,
            volumeMl = 200,
            isDefault = true,
            lastModifiedTimestamp = 1000L,
            sourceDeviceId = "phone"
        )

        val json = SyncPayloadSerializer.serializeDrink(drink)

        assertTrue("JSON must contain id field", json.contains("\"id\""))
        assertTrue("JSON must contain name field", json.contains("\"name\""))
        assertTrue("JSON must contain caffeineMg field", json.contains("\"caffeineMg\""))
        assertTrue("JSON must contain volumeMl field", json.contains("\"volumeMl\""))
        assertTrue("JSON must contain isDefault field", json.contains("\"isDefault\""))
        assertTrue("JSON must contain lastModifiedTimestamp field", json.contains("\"lastModifiedTimestamp\""))
        assertTrue("JSON must contain sourceDeviceId field", json.contains("\"sourceDeviceId\""))
    }

    @Test
    fun `serializeDrink includes correct field values`() {
        val drink = DrinkEntity(
            id = 7,
            name = "Cappuccino",
            caffeineMg = 80,
            volumeMl = 200,
            isDefault = false,
            lastModifiedTimestamp = 2000L,
            sourceDeviceId = "watch"
        )

        val json = SyncPayloadSerializer.serializeDrink(drink)

        assertTrue(json.contains("\"id\":7"))
        assertTrue(json.contains("\"name\":\"Cappuccino\""))
        assertTrue(json.contains("\"caffeineMg\":80"))
        assertTrue(json.contains("\"volumeMl\":200"))
        assertTrue(json.contains("\"isDefault\":false"))
        assertTrue(json.contains("\"lastModifiedTimestamp\":2000"))
        assertTrue(json.contains("\"sourceDeviceId\":\"watch\""))
    }

    @Test
    fun `deserializeDrink roundtrip preserves all fields`() {
        val original = DrinkEntity(
            id = 12,
            name = "Americano",
            caffeineMg = 150,
            volumeMl = 300,
            isDefault = true,
            lastModifiedTimestamp = 5000L,
            sourceDeviceId = "phone"
        )

        val json = SyncPayloadSerializer.serializeDrink(original)
        val deserialized = SyncPayloadSerializer.deserializeDrink(json)

        assertEquals("Roundtrip must preserve id", original.id, deserialized.id)
        assertEquals("Roundtrip must preserve name", original.name, deserialized.name)
        assertEquals("Roundtrip must preserve caffeineMg", original.caffeineMg, deserialized.caffeineMg)
        assertEquals("Roundtrip must preserve volumeMl", original.volumeMl, deserialized.volumeMl)
        assertEquals("Roundtrip must preserve isDefault", original.isDefault, deserialized.isDefault)
        assertEquals("Roundtrip must preserve lastModifiedTimestamp", original.lastModifiedTimestamp, deserialized.lastModifiedTimestamp)
        assertEquals("Roundtrip must preserve sourceDeviceId", original.sourceDeviceId, deserialized.sourceDeviceId)
    }

    @Test
    fun `deserializeDrink handles default values`() {
        val drink = DrinkEntity(
            id = 1, name = "Black Coffee", caffeineMg = 95, volumeMl = 250
        )
        // isDefault defaults to false, lastModifiedTimestamp defaults to System.currentTimeMillis()
        // sourceDeviceId defaults to ""

        val json = SyncPayloadSerializer.serializeDrink(drink)
        val deserialized = SyncPayloadSerializer.deserializeDrink(json)

        assertEquals(false, deserialized.isDefault)
        assertEquals(drink.lastModifiedTimestamp, deserialized.lastModifiedTimestamp)
        assertEquals("", deserialized.sourceDeviceId)
    }

    // ======================================================================
    // Cross-format compatibility
    // ======================================================================

    @Test
    fun `intake JSON produced by phone module can be consumed by wear module`() {
        // This simulates what the phone module's SyncPayloadSerializer produces
        // Both modules use the same Gson serialization, so they should be compatible
        val phoneProducedJson = """{"id":42,"drinkId":null,"drinkName":"Espresso","caffeineMg":63,"volumeMl":30,"timestamp":1000,"lastModifiedTimestamp":2000,"sourceDeviceId":"phone"}"""

        val deserialized = SyncPayloadSerializer.deserializeIntake(phoneProducedJson)

        assertNotNull("Should deserialize phone-produced JSON", deserialized)
        assertEquals(42L, deserialized.id)
        assertEquals("Espresso", deserialized.drinkName)
    }

    @Test
    fun `drink JSON produced by phone module can be consumed by wear module`() {
        val phoneProducedJson = """{"id":7,"name":"Latte","caffeineMg":63,"volumeMl":200,"isDefault":false,"lastModifiedTimestamp":1000,"sourceDeviceId":"phone"}"""

        val deserialized = SyncPayloadSerializer.deserializeDrink(phoneProducedJson)

        assertNotNull("Should deserialize phone-produced JSON", deserialized)
        assertEquals(7L, deserialized.id)
        assertEquals("Latte", deserialized.name)
    }

    // ======================================================================
    // Edge cases
    // ======================================================================

    @Test
    fun `serializeIntake handles zero values`() {
        val intake = CaffeineIntake(
            id = 0, drinkName = "", caffeineMg = 0, volumeMl = 0, timestamp = 0L
        )
        val json = SyncPayloadSerializer.serializeIntake(intake)
        val deserialized = SyncPayloadSerializer.deserializeIntake(json)

        assertEquals(0L, deserialized.id)
        assertEquals("", deserialized.drinkName)
        assertEquals(0, deserialized.caffeineMg)
    }

    @Test
    fun `serializeDrink handles zero values`() {
        val drink = DrinkEntity(
            id = 0, name = "", caffeineMg = 0, volumeMl = 0
        )
        val json = SyncPayloadSerializer.serializeDrink(drink)
        val deserialized = SyncPayloadSerializer.deserializeDrink(json)

        assertEquals(0L, deserialized.id)
        assertEquals("", deserialized.name)
        assertEquals(0, deserialized.caffeineMg)
    }
}
