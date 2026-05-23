package pl.dekrate.kofeino.common.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaffeineConstantsTest {
    @Test fun `ADULT_LIMIT_MG should be 400`() { assertEquals(400, CaffeineConstants.ADULT_LIMIT_MG) }
    @Test fun `PREGNANT_LIMIT_MG should be 200`() { assertEquals(200, CaffeineConstants.PREGNANT_LIMIT_MG) }
    @Test fun `SENSITIVE_LIMIT_MG should be 100`() { assertEquals(100, CaffeineConstants.SENSITIVE_LIMIT_MG) }
    @Test fun `MAX_SAFE_SINGLE_DOSE_MG should be 200`() { assertEquals(200, CaffeineConstants.MAX_SAFE_SINGLE_DOSE_MG) }
    @Test fun `DEFAULT_LIMIT_MG should be 400`() { assertEquals(400, CaffeineConstants.DEFAULT_LIMIT_MG) }
    @Test fun `all limits should be positive`() {
        assertTrue(CaffeineConstants.ADULT_LIMIT_MG > 0)
        assertTrue(CaffeineConstants.PREGNANT_LIMIT_MG > 0)
        assertTrue(CaffeineConstants.SENSITIVE_LIMIT_MG > 0)
        assertTrue(CaffeineConstants.MAX_SAFE_SINGLE_DOSE_MG > 0)
        assertTrue(CaffeineConstants.DEFAULT_LIMIT_MG > 0)
    }
    @Test fun `adult limit should be highest`() {
        assertTrue(CaffeineConstants.ADULT_LIMIT_MG > CaffeineConstants.PREGNANT_LIMIT_MG)
        assertTrue(CaffeineConstants.ADULT_LIMIT_MG > CaffeineConstants.SENSITIVE_LIMIT_MG)
    }
    @Test fun `single dose should not exceed adult daily limit`() {
        assertTrue(CaffeineConstants.MAX_SAFE_SINGLE_DOSE_MG <= CaffeineConstants.ADULT_LIMIT_MG)
    }
}
