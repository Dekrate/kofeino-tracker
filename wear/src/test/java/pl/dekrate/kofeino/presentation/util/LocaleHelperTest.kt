package pl.dekrate.kofeino.presentation.util

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for the Wear OS module's [LocaleHelper] using Robolectric.
 *
 * Verifies that:
 * - [LocaleHelper.wrapContext] produces a context whose resources configuration
 *   reflects the specified language (Decorator pattern via
 *   [android.content.Context.createConfigurationContext]).
 * - Empty language returns the original context unchanged (system-default strategy).
 * - [LocaleHelper.getCurrentLanguage] reports the correct locale.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class LocaleHelperTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `wrapContext with English sets configuration locale to English`() {
        val wrapped = LocaleHelper.wrapContext(context, "en")

        val actual = wrapped.resources.configuration.locales[0].language
        assertEquals("en", actual)
    }

    @Test
    fun `wrapContext with Polish sets configuration locale to Polish`() {
        val wrapped = LocaleHelper.wrapContext(context, "pl")

        val actual = wrapped.resources.configuration.locales[0].language
        assertEquals("pl", actual)
    }

    @Test
    fun `wrapContext with empty string returns the same context (system default)`() {
        val wrapped = LocaleHelper.wrapContext(context, "")

        assertSame(
            "Empty language must return the original context unchanged",
            context, wrapped
        )
    }

    @Test
    fun `wrapContext with English returns a different Context instance`() {
        val wrapped = LocaleHelper.wrapContext(context, "en")

        assertNotSame(
            "Wrapped context must be a different instance (decorator)",
            context, wrapped
        )
    }

    @Test
    fun `wrapContext with Polish sets Locale getDefault to Polish`() {
        LocaleHelper.wrapContext(context, "pl")

        assertEquals("pl", java.util.Locale.getDefault().language)
    }

    @Test
    fun `getCurrentLanguage returns en for English-wrapped context`() {
        val wrapped = LocaleHelper.wrapContext(context, "en")

        val actual = LocaleHelper.getCurrentLanguage(wrapped)
        assertEquals("en", actual)
    }

    @Test
    fun `getCurrentLanguage returns pl for Polish-wrapped context`() {
        val wrapped = LocaleHelper.wrapContext(context, "pl")

        val actual = LocaleHelper.getCurrentLanguage(wrapped)
        assertEquals("pl", actual)
    }
}
