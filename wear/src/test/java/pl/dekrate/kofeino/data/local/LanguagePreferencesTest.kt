package pl.dekrate.kofeino.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class LanguagePreferencesTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private lateinit var languagePreferences: LanguagePreferences

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        prefs = mockk(relaxed = true) {
            every { edit() } returns editor
        }
        context = mockk(relaxed = true) {
            every { getSharedPreferences(any(), any<Int>()) } returns prefs
        }
        languagePreferences = LanguagePreferences(context)
    }

    @Test
    fun `default language should be System (empty string)`() {
        every { prefs.getString(any(), any()) } returns null
        assertEquals("", LanguagePreferences.DEFAULT_LANGUAGE)
        assertEquals("", languagePreferences.getLanguage())
    }

    @Test
    fun `getLanguage returns saved language`() {
        every { prefs.getString("selected_language", any()) } returns "pl"
        assertEquals("pl", languagePreferences.getLanguage())
    }

    @Test
    fun `getLanguage returns empty string for saved System language`() {
        every { prefs.getString("selected_language", any()) } returns ""
        assertEquals("", languagePreferences.getLanguage())
    }

    @Test
    fun `setLanguage saves to preferences`() {
        languagePreferences.setLanguage("pl")
        verify { editor.putString("selected_language", "pl") }
        verify { editor.apply() }
    }

    @Test
    fun `setLanguage with empty string saves system language`() {
        languagePreferences.setLanguage("")
        verify { editor.putString("selected_language", "") }
        verify { editor.apply() }
    }

    @Test
    fun `setLanguage overwrites previous value`() {
        languagePreferences.setLanguage("pl")
        verify { editor.putString("selected_language", "pl") }

        languagePreferences.setLanguage("en")
        verify { editor.putString("selected_language", "en") }
    }

    @Test
    fun `static getLanguage returns default when no preference saved`() {
        every { prefs.getString("selected_language", "") } returns ""
        assertEquals("", LanguagePreferences.getLanguage(context))
    }

    @Test
    fun `static getLanguage returns saved preference`() {
        every { prefs.getString("selected_language", "") } returns "pl"
        assertEquals("pl", LanguagePreferences.getLanguage(context))
    }

    @Test
    fun `constants should be correct`() {
        assertEquals("", LanguagePreferences.LANGUAGE_SYSTEM)
        assertEquals("en", LanguagePreferences.LANGUAGE_EN)
        assertEquals("pl", LanguagePreferences.LANGUAGE_PL)
        assertEquals(LanguagePreferences.LANGUAGE_SYSTEM, LanguagePreferences.DEFAULT_LANGUAGE)
    }
}
