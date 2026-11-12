package com.example.project_lttbd.notifications.storage

import com.example.project_lttbd.notifications.model.LocalNotificationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class LocalNotificationStoreTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var baseDir: File

    @Before
    fun setUp() {
        baseDir = tempFolder.newFolder("notifications_test")
    }

    @Test
    fun testUidIsolation_betweenUserA_and_UserB() {
        val uidA = "user_A_123"
        val uidB = "user_B_456"

        val itemA = LocalNotificationItem(
            id = "notif_a_1",
            doseId = "dose_101",
            title = "Uống thuốc Metformin",
            body = "1 viên sau ăn sáng",
            timestampMillis = 1000L,
            isRead = false,
            destinationRoute = "reminder/dose_101?readOnly=false"
        )

        val itemB = LocalNotificationItem(
            id = "notif_b_1",
            doseId = "dose_202",
            title = "Uống thuốc Amlodipine",
            body = "1 viên trước ăn tối",
            timestampMillis = 2000L,
            isRead = false,
            destinationRoute = "reminder/dose_202?readOnly=false"
        )

        LocalNotificationStore.addNotificationToDir(baseDir, uidA, itemA)
        LocalNotificationStore.addNotificationToDir(baseDir, uidB, itemB)

        val listA = LocalNotificationStore.getNotificationsFromDir(baseDir, uidA)
        val listB = LocalNotificationStore.getNotificationsFromDir(baseDir, uidB)

        assertEquals(1, listA.size)
        assertEquals("notif_a_1", listA[0].id)
        assertEquals("dose_101", listA[0].doseId)

        assertEquals(1, listB.size)
        assertEquals("notif_b_1", listB[0].id)
        assertEquals("dose_202", listB[0].doseId)

        // Clear A does not affect B
        LocalNotificationStore.clearAllInDir(baseDir, uidA)
        assertTrue(LocalNotificationStore.getNotificationsFromDir(baseDir, uidA).isEmpty())
        assertEquals(1, LocalNotificationStore.getNotificationsFromDir(baseDir, uidB).size)
    }

    @Test
    fun testMarkAsRead_and_MarkAllAsRead() {
        val uid = "user_test_read"

        val item1 = LocalNotificationItem(
            id = "n1",
            doseId = "d1",
            title = "T1",
            body = "B1",
            timestampMillis = 1000L,
            isRead = false
        )
        val item2 = LocalNotificationItem(
            id = "n2",
            doseId = "d2",
            title = "T2",
            body = "B2",
            timestampMillis = 2000L,
            isRead = false
        )

        LocalNotificationStore.addNotificationToDir(baseDir, uid, item1)
        LocalNotificationStore.addNotificationToDir(baseDir, uid, item2)

        LocalNotificationStore.markAsReadInDir(baseDir, uid, "n1")
        var list = LocalNotificationStore.getNotificationsFromDir(baseDir, uid)
        assertTrue(list.first { it.id == "n1" }.isRead)
        assertFalse(list.first { it.id == "n2" }.isRead)

        LocalNotificationStore.markAllAsReadInDir(baseDir, uid)
        list = LocalNotificationStore.getNotificationsFromDir(baseDir, uid)
        assertTrue(list.all { it.isRead })
    }
}
