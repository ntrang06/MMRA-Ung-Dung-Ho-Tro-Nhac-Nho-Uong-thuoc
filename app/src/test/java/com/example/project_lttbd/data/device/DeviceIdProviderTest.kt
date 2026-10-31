package com.example.project_lttbd.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceIdProviderTest {

    @Test
    fun testGenerateAndFormatValidation() {
        val testId = DeviceIdProvider.generateNewDeviceId()
        assertTrue("Device ID must not be blank", testId.isNotBlank())
        // UUID format verification: 8-4-4-4-12 hex characters
        val uuidRegex = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
        assertTrue("Device ID must match standard UUID format: $testId", uuidRegex.matches(testId))
        assertFalse("Device ID must not contain slash", testId.contains("/"))
    }

    @Test
    fun testValidateDeviceIdString() {
        assertTrue(DeviceIdProvider.isValidDeviceId("123e4567-e89b-12d3-a456-426614174000"))
        assertFalse(DeviceIdProvider.isValidDeviceId(""))
        assertFalse(DeviceIdProvider.isValidDeviceId("invalid/slash"))
    }
}
