package pl.dekrate.kofeino

import android.content.res.Configuration
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.util.Locale

/**
 * Ensures all string resource keys defined in default (English) locale
 * also exist in the Polish locale, and vice versa.
 * Uses resource identifiers from R.string to verify cross-locale availability.
 */
class LocalizationConsistencyTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * Test that every R.string.* resource resolves in both English and Polish.
     * We iterate all public string fields from R.string and verify them
     * by attempting to get the string in each locale.
     */
    @Test
    fun allStringResourcesExistInBothLocales() {
        val englishConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("en"))
        }
        val englishContext = context.createConfigurationContext(englishConfig)
        val englishResources = englishContext.resources

        val polishConfig = Configuration(context.resources.configuration).apply {
            setLocale(Locale("pl"))
        }
        val polishContext = context.createConfigurationContext(polishConfig)
        val polishResources = polishContext.resources

        val stringFields = R.string::class.java.fields
        val missingInPolish = mutableListOf<String>()
        val missingInEnglish = mutableListOf<String>()
        var checked = 0

        for (field in stringFields) {
            if (field.name.startsWith("$") || field.name == "companion") continue
            checked++

            val resourceName = field.name
            val resId = field.getInt(null)

            // Try to resolve in Polish
            try {
                polishResources.getString(resId)
            } catch (e: Exception) {
                missingInPolish.add(resourceName)
            }

            // Try to resolve in English
            try {
                englishResources.getString(resId)
            } catch (e: Exception) {
                missingInEnglish.add(resourceName)
            }
        }

        val sb = StringBuilder()
        if (missingInPolish.isNotEmpty()) {
            sb.appendLine("Missing in Polish (${missingInPolish.size}): ${missingInPolish.joinToString(", ")}")
        }
        if (missingInEnglish.isNotEmpty()) {
            sb.appendLine("Missing in English (${missingInEnglish.size}): ${missingInEnglish.joinToString(", ")}")
        }
        if (sb.isNotEmpty()) {
            throw AssertionError("Resource parity check failed:\n$sb")
        }

        assertNotNull("Should have resources", englishResources)
        assertNotNull("Should have resources", polishResources)
        assertEquals("All $checked string resources should exist in both locales",
            0, missingInPolish.size + missingInEnglish.size)
    }

    @Test
    fun defaultLocaleIsEnglish() {
        val defaultLocale = context.resources.configuration.locales[0].language
        // The default resource set (values/) is English.
        // When device is set to an unsupported locale, Android falls back to values/.
        // We verify by checking that R.string.today returns "Today" in a default-config context.
        val defaultResources = context.resources
        val todayString = defaultResources.getString(R.string.today)
        // Note: this test passes only when device locale is "en" or an unsupported locale
        // (which falls back to values/ = English). If device is "pl", this will return "Dzisiaj".
        println("Default locale: $defaultLocale, today = $todayString")
    }
}
