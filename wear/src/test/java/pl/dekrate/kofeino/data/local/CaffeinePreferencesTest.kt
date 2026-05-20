package pl.dekrate.kofeino.data.local

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CaffeinePreferencesTest {

    private lateinit var prefs: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor
    private lateinit var context: Context
    private lateinit var caffeinePrefs: CaffeinePreferences

    @Before
    fun setup() {
        editor = mockk(relaxed = true)
        every { editor.putString(any(), any()) } returns editor
        every { editor.putInt(any(), any()) } returns editor
        prefs = mockk(relaxed = true) {
            every { edit() } returns editor
        }
        context = mockk(relaxed = true) {
            every { getSharedPreferences(any(), any<Int>()) } returns prefs
        }
        caffeinePrefs = CaffeinePreferences(context)
    }

    @Test
    fun `default profile should be ADULT`() {
        every { prefs.getString("caffeine_profile", null) } returns null
        assertEquals(pl.dekrate.kofeino.domain.model.CaffeineLimitProfile.ADULT, caffeinePrefs.getProfile())
    }

    @Test
    fun `getProfile returns saved profile`() {
        every { prefs.getString("caffeine_profile", null) } returns "PREGNANT"
        assertEquals(
            pl.dekrate.kofeino.domain.model.CaffeineLimitProfile.PREGNANT,
            caffeinePrefs.getProfile()
        )
    }

    @Test
    fun `setProfile saves to preferences`() {
        caffeinePrefs.setProfile(pl.dekrate.kofeino.domain.model.CaffeineLimitProfile.SENSITIVE)
        verify { editor.putString("caffeine_profile", "SENSITIVE") }
        verify { editor.apply() }
    }

    @Test
    fun `default custom limit should be 400`() {
        every { prefs.getInt("custom_caffeine_limit", 400) } returns 400
        assertEquals(400, caffeinePrefs.getCustomLimit())
    }

    @Test
    fun `setCustomLimit saves to preferences`() {
        caffeinePrefs.setCustomLimit(150)
        verify { editor.putInt("custom_caffeine_limit", 150) }
        verify { editor.apply() }
    }

    @Test
    fun `getLimitMg returns 400 for ADULT profile`() {
        every { prefs.getString("caffeine_profile", null) } returns "ADULT"
        assertEquals(400, caffeinePrefs.getLimitMg())
    }

    @Test
    fun `getLimitMg returns 200 for PREGNANT profile`() {
        every { prefs.getString("caffeine_profile", null) } returns "PREGNANT"
        assertEquals(200, caffeinePrefs.getLimitMg())
    }

    @Test
    fun `getLimitMg returns 100 for SENSITIVE profile`() {
        every { prefs.getString("caffeine_profile", null) } returns "SENSITIVE"
        assertEquals(100, caffeinePrefs.getLimitMg())
    }

    @Test
    fun `getLimitMg returns custom limit for CUSTOM profile`() {
        every { prefs.getString("caffeine_profile", null) } returns "CUSTOM"
        every { prefs.getInt("custom_caffeine_limit", 400) } returns 250
        assertEquals(250, caffeinePrefs.getLimitMg())
    }

    @Test
    fun `invalid profile name falls back to ADULT`() {
        every { prefs.getString("caffeine_profile", null) } returns "INVALID_PROFILE"
        assertEquals(pl.dekrate.kofeino.domain.model.CaffeineLimitProfile.ADULT, caffeinePrefs.getProfile())
    }

    @Test
    fun `setCustomLimit clamps to minimum value`() {
        caffeinePrefs.setCustomLimit(10)
        verify { editor.putInt("custom_caffeine_limit", 25) }
        verify { editor.apply() }
    }

    @Test
    fun `setCustomLimit clamps to maximum value`() {
        caffeinePrefs.setCustomLimit(5000)
        verify { editor.putInt("custom_caffeine_limit", 2000) }
        verify { editor.apply() }
    }

    @Test
    fun `constants should be correct`() {
        assertEquals(400, CaffeinePreferences.DEFAULT_CUSTOM_LIMIT)
        assertEquals(25, CaffeinePreferences.MIN_CUSTOM_LIMIT)
        assertEquals(2000, CaffeinePreferences.MAX_CUSTOM_LIMIT)
    }
}
