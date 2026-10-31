package com.example.project_lttbd.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceManagerTest {

    @Test
    fun testCreateDeviceDtoPayload() {
        val deviceId = "test-device-uuid-1234"
        val token = "sample_fcm_token_xyz"
        val dto = DeviceManager.createDeviceDto(
            deviceId = deviceId,
            fcmToken = token,
            notificationsEnabled = true
        )

        assertEquals(deviceId, dto.deviceId)
        assertEquals(token, dto.fcmToken)
        assertEquals("android", dto.platform)
        assertTrue(dto.notificationsEnabled)
        assertNotNull(dto.updatedAt)
    }
}
