package pl.dekrate.kofeino.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateUtilsTest {
    private val fixedMillis: Long = 1718461800000L

    @Test fun `formatTime should return HH mm format`() {
        val result = DateUtils.formatTime(fixedMillis)
        assertTrue(result.contains(":"))
        assertEquals(5, result.length)
    }
    @Test fun `formatDate should return dd MM yyyy format`() {
        val result = DateUtils.formatDate(fixedMillis)
        assertTrue(result.contains("."))
        assertEquals(10, result.length)
    }
    @Test fun `formatFullDate should contain day name`() {
        val result = DateUtils.formatFullDate(fixedMillis)
        assertTrue(result.contains(","))
        assertTrue(result.contains("."))
    }
    @Test fun `formatDayOfWeek should return day name`() {
        val result = DateUtils.formatDayOfWeek(fixedMillis)
        assertTrue(result.isNotBlank())
        assertTrue(result.length >= 3)
    }
    @Test fun `formatDate should be consistent`() {
        assertEquals(DateUtils.formatDate(fixedMillis), DateUtils.formatDate(fixedMillis))
    }
}
