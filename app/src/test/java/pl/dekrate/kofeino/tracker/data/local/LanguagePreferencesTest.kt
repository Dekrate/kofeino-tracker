package pl.dekrate.kofeino.tracker.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Test

class LanguagePreferencesTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private lateinit var languagePreferences: LanguagePreferences

    @Before
    fun setUp() {
        prefs = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        context = mockk()

        every { context.getSharedPreferences("kofeino_language_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor

        languagePreferences = LanguagePreferences(context)
    }

    @Test
    fun `getLanguage returns default when no preference saved`() {
        every { prefs.getString("selected_language", "en") } returns "en"

        val lang = languagePreferences.getLanguage()

        assert(lang == "en") { "Expected 'en', got '$lang'" }
    }

    @Test
    fun `getLanguage returns saved preference`() {
        every { prefs.getString("selected_language", "en") } returns "pl"

        val lang = languagePreferences.getLanguage()

        assert(lang == "pl") { "Expected 'pl', got '$lang'" }
    }

    @Test
    fun `setLanguage saves to shared preferences`() {
        languagePreferences.setLanguage("pl")

        verify { editor.putString("selected_language", "pl") }
        verify { editor.apply() }
    }

    @Test
    fun `static getLanguage returns default when no preference`() {
        every { context.getSharedPreferences("kofeino_language_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getString("selected_language", "en") } returns "en"

        val lang = LanguagePreferences.getLanguage(context)

        assert(lang == "en") { "Expected 'en', got '$lang'" }
    }

    @Test
    fun `static getLanguage returns saved preference`() {
        every { context.getSharedPreferences("kofeino_language_prefs", Context.MODE_PRIVATE) } returns prefs
        every { prefs.getString("selected_language", "en") } returns "pl"

        val lang = LanguagePreferences.getLanguage(context)

        assert(lang == "pl") { "Expected 'pl', got '$lang'" }
    }

    @Test
    fun `DEFAULT_LANGUAGE is en`() {
        assert(LanguagePreferences.DEFAULT_LANGUAGE == "en")
    }

    @Test
    fun `LANGUAGE_PL is pl`() {
        assert(LanguagePreferences.LANGUAGE_PL == "pl")
    }
}
