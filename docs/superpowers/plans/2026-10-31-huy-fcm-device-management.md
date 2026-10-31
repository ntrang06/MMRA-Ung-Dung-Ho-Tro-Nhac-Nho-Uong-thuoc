# FCM Notifications & Device Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai quản lý thiết bị (`users/{uid}/devices/{deviceId}`) và dịch vụ nhận thông báo Firebase Cloud Messaging (FCM) cho thành viên Huy theo đúng quy ước trong tài liệu `docs/firebase-contract.md`.

**Architecture:** 
1. `DeviceIdProvider` chịu trách nhiệm tạo và lưu trữ UUID cố định cho mỗi lần cài đặt ứng dụng vào `SharedPreferences`.
2. `DeviceManager` / `DeviceRepository` chịu trách nhiệm đồng bộ thông tin thiết bị (`DeviceDto`) lên Firestore tại `users/{uid}/devices/{deviceId}`, cập nhật FCM token khi thay đổi, và gỡ bỏ thiết bị khi đăng xuất.
3. `MMRAFirebaseMessagingService` kế thừa `FirebaseMessagingService` để nhận token mới (`onNewToken`) và hiển thị thông báo đẩy (`onMessageReceived`) với Notification Channel chuẩn, có `PendingIntent` điều hướng trực tiếp vào màn hình mục tiêu khi người dùng bấm vào thông báo.
4. `MainActivity` xử lý `Intent` từ thông báo (cả cold start và `onNewIntent`) để điều hướng chính xác.

**Tech Stack:** Kotlin, Jetpack Compose, Android SDK (API 24 - 37), AndroidX Core NotificationCompat, Firebase Messaging (`firebase-messaging`), Cloud Firestore (`firebase-firestore`), Firebase Auth (`firebase-auth`), JUnit4.

**Spec:** `docs/firebase-contract.md` (Mục 5: Thiết bị nhận thông báo — Huy) & `MMRA Dev — Tài liệu phân công & hướng dẫn tích hợp Firebase`.

## Global Constraints

- Android Application ID: `com.example.project_lttbd`
- Root package: `com.example.project_lttbd`
- Firebase project ID: `mmra-dev`
- Firestore path: `users/{uid}/devices/{deviceId}`
- Document schema: `DeviceDto(deviceId: String, fcmToken: String, platform: "android", notificationsEnabled: Boolean, updatedAt: Timestamp)`
- Permission required for Android 13+ (API 33+): `android.permission.POST_NOTIFICATIONS`
- No plaintext credentials, no hardcoded secrets, no mock UID when operating with Firebase Auth.
- Branch convention: Thực hiện trên nhánh `feature/huy`.

## Review Focus

1. **UUID Persistence:** `deviceId` phải giữ nguyên qua các lần khởi động ứng dụng và chỉ tạo mới duy nhất một lần khi app được cài đặt lần đầu.
2. **Multi-device Isolation:** Hai thiết bị khác nhau đăng nhập cùng một tài khoản UID phải sinh ra 2 document riêng biệt trong subcollection `devices`.
3. **Token Rotation:** Khi Firebase sinh token mới (`onNewToken`), token này phải tự động cập nhật lên Firestore nếu người dùng đang có phiên đăng nhập.
4. **Logout Cleanup:** Khi người dùng đăng xuất, document thiết bị cũ phải được gỡ bỏ hoặc vô hiệu hóa để tránh nhận nhầm thông báo từ tài khoản trước.
5. **Notification Click Navigation:** Bấm thông báo khi app đang chạy nền (background) hoặc đã tắt (cold start) phải mở `MainActivity` và chuyển đúng màn hình đích (không bị crash hay nuốt sự kiện).

---

### Task 1: Persistent Device ID Provider

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/data/device/DeviceIdProvider.kt`
- Test: `app/src/test/java/com/example/project_lttbd/data/device/DeviceIdProviderTest.kt`

**Interfaces:**
- Consumes: `android.content.Context`, `android.content.SharedPreferences`
- Produces: `DeviceIdProvider.getDeviceId(context: Context): String`

- [ ] **Step 1: Write unit test for DeviceIdProvider**

Tạo file `app/src/test/java/com/example/project_lttbd/data/device/DeviceIdProviderTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.project_lttbd.data.device.DeviceIdProviderTest`
Expected: FAIL với "Unresolved reference: DeviceIdProvider".

- [ ] **Step 3: Implement DeviceIdProvider**

Tạo file `app/src/main/java/com/example/project_lttbd/data/device/DeviceIdProvider.kt`:
```kotlin
package com.example.project_lttbd.data.device

import android.content.Context
import java.util.UUID

object DeviceIdProvider {
    private const val PREFS_NAME = "mmra_device_preferences"
    private const val KEY_DEVICE_ID = "device_installation_id"

    @Volatile
    private var cachedDeviceId: String? = null

    fun getDeviceId(context: Context): String {
        cachedDeviceId?.let { return it }

        synchronized(this) {
            cachedDeviceId?.let { return it }

            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var id = prefs.getString(KEY_DEVICE_ID, null)

            if (id.isNullOrBlank() || !isValidDeviceId(id)) {
                id = generateNewDeviceId()
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }

            cachedDeviceId = id
            return id
        }
    }

    internal fun generateNewDeviceId(): String {
        return UUID.randomUUID().toString()
    }

    internal fun isValidDeviceId(id: String): Boolean {
        if (id.isBlank() || '/' in id) return false
        return try {
            UUID.fromString(id)
            true
        } catch (_: IllegalArgumentException) {
            false
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.project_lttbd.data.device.DeviceIdProviderTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git checkout -B feature/huy
git add app/src/main/java/com/example/project_lttbd/data/device/DeviceIdProvider.kt app/src/test/java/com/example/project_lttbd/data/device/DeviceIdProviderTest.kt
git commit -m "feat(device): implement persistent DeviceIdProvider with UUID validation"
```

---

### Task 2: Device Manager for Firestore Device Registration & FCM Token Sync

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/data/device/DeviceManager.kt`
- Test: `app/src/test/java/com/example/project_lttbd/data/device/DeviceManagerTest.kt`

**Interfaces:**
- Consumes: `FirebaseProvider`, `DeviceDto`, `DeviceIdProvider`, `Context`
- Produces: 
  - `DeviceManager.syncCurrentDevice(context: Context, uid: String, onComplete: ((Boolean) -> Unit)?)`
  - `DeviceManager.updateFcmToken(uid: String, deviceId: String, token: String)`
  - `DeviceManager.removeDevice(context: Context, uid: String, onComplete: ((Boolean) -> Unit)?)`
  - `DeviceManager.createDeviceDto(deviceId: String, fcmToken: String, enabled: Boolean): DeviceDto`

- [ ] **Step 1: Write unit test for DeviceManager data creation**

Tạo file `app/src/test/java/com/example/project_lttbd/data/device/DeviceManagerTest.kt`:
```kotlin
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
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.project_lttbd.data.device.DeviceManagerTest`
Expected: FAIL với "Unresolved reference: DeviceManager".

- [ ] **Step 3: Implement DeviceManager**

Tạo file `app/src/main/java/com/example/project_lttbd/data/device/DeviceManager.kt`:
```kotlin
package com.example.project_lttbd.data.device

import android.content.Context
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.example.project_lttbd.data.remote.dto.DeviceDto
import com.google.firebase.Timestamp

object DeviceManager {
    private const val TAG = "DeviceManager"

    fun createDeviceDto(
        deviceId: String,
        fcmToken: String,
        notificationsEnabled: Boolean
    ): DeviceDto {
        return DeviceDto(
            deviceId = deviceId,
            fcmToken = fcmToken,
            platform = "android",
            notificationsEnabled = notificationsEnabled,
            updatedAt = Timestamp.now()
        )
    }

    fun syncCurrentDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val deviceId = DeviceIdProvider.getDeviceId(context)
        val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()

        FirebaseProvider.messaging.token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                onComplete?.invoke(false)
                return@addOnCompleteListener
            }

            val token = task.result.orEmpty()
            val dto = createDeviceDto(
                deviceId = deviceId,
                fcmToken = token,
                notificationsEnabled = notificationsEnabled
            )

            FirebaseProvider.devicesCollection(uid)
                .document(deviceId)
                .set(dto)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully synced device $deviceId for user $uid")
                    onComplete?.invoke(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to sync device to Firestore", e)
                    onComplete?.invoke(false)
                }
        }
    }

    fun updateFcmToken(
        uid: String,
        deviceId: String,
        token: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank() || deviceId.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val updates = mapOf(
            "fcmToken" to token,
            "updatedAt" to Timestamp.now()
        )

        FirebaseProvider.devicesCollection(uid)
            .document(deviceId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "FCM token updated for device $deviceId")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to update FCM token", e)
                onComplete?.invoke(false)
            }
    }

    fun removeDevice(
        context: Context,
        uid: String,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        if (uid.isBlank()) {
            onComplete?.invoke(false)
            return
        }

        val deviceId = DeviceIdProvider.getDeviceId(context)
        FirebaseProvider.devicesCollection(uid)
            .document(deviceId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Device $deviceId removed for user $uid on sign out")
                onComplete?.invoke(true)
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Could not delete device doc during sign out", e)
                onComplete?.invoke(false)
            }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.project_lttbd.data.device.DeviceManagerTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/project_lttbd/data/device/DeviceManager.kt app/src/test/java/com/example/project_lttbd/data/device/DeviceManagerTest.kt
git commit -m "feat(device): add DeviceManager to register, update token, and unlink device on Firestore"
```

---

### Task 3: Firebase Messaging Service & System Notification Display

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/notifications/MMRAFirebaseMessagingService.kt`
- Create: `app/src/main/java/com/example/project_lttbd/notifications/NotificationHelper.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Test: `app/src/test/java/com/example/project_lttbd/notifications/NotificationPayloadParserTest.kt`

**Interfaces:**
- Consumes: `FirebaseMessagingService`, `RemoteMessage`, `NotificationCompat`, `DeviceIdProvider`, `DeviceManager`
- Produces: System notification channels, push notification handler, and pending intent payload.

- [ ] **Step 1: Write test for Notification Payload parsing**

Tạo file `app/src/test/java/com/example/project_lttbd/notifications/NotificationPayloadParserTest.kt`:
```kotlin
package com.example.project_lttbd.notifications

import com.example.project_lttbd.ui.navigation.MMRADestinations
import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPayloadParserTest {

    @Test
    fun testParseDestinationRoute() {
        val dataWithRoute = mapOf("destination" to MMRADestinations.SCHEDULE)
        val route1 = NotificationHelper.resolveDestination(dataWithRoute)
        assertEquals(MMRADestinations.SCHEDULE, route1)

        val dataEmpty = emptyMap<String, String>()
        val routeDefault = NotificationHelper.resolveDestination(dataEmpty)
        assertEquals(MMRADestinations.HOME, routeDefault)
    }

    @Test
    fun testExtractDoseId() {
        val data = mapOf("doseId" to "dose_morning_123")
        assertEquals("dose_morning_123", NotificationHelper.extractDoseId(data))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests com.example.project_lttbd.notifications.NotificationPayloadParserTest`
Expected: FAIL với "Unresolved reference: NotificationHelper".

- [ ] **Step 3: Implement NotificationHelper**

Tạo file `app/src/main/java/com/example/project_lttbd/notifications/NotificationHelper.kt`:
```kotlin
package com.example.project_lttbd.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.project_lttbd.MainActivity
import com.example.project_lttbd.R
import com.example.project_lttbd.ui.navigation.MMRADestinations

object NotificationHelper {
    const val CHANNEL_ID_REMINDERS = "mmra_medication_reminders"
    const val CHANNEL_NAME_REMINDERS = "Nhắc nhở uống thuốc MMRA"
    const val CHANNEL_DESC_REMINDERS = "Thông báo giờ uống thuốc và cảnh báo sức khỏe"

    const val EXTRA_DESTINATION = "extra_destination"
    const val EXTRA_DOSE_ID = "extra_dose_id"

    fun resolveDestination(data: Map<String, String>): String {
        return data["destination"]?.ifBlank { null } ?: MMRADestinations.HOME
    }

    fun extractDoseId(data: Map<String, String>): String? {
        return data["doseId"]?.ifBlank { null }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                CHANNEL_NAME_REMINDERS,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC_REMINDERS
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap()
    ) {
        ensureNotificationChannel(context)

        val destination = resolveDestination(data)
        val doseId = extractDoseId(data)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_DESTINATION, destination)
            doseId?.let { putExtra(EXTRA_DOSE_ID, it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.chuong)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = (System.currentTimeMillis() % 100000).toInt()
        notificationManager.notify(notificationId, notification)
    }
}
```

- [ ] **Step 4: Implement MMRAFirebaseMessagingService**

Tạo file `app/src/main/java/com/example/project_lttbd/notifications/MMRAFirebaseMessagingService.kt`:
```kotlin
package com.example.project_lttbd.notifications

import android.util.Log
import com.example.project_lttbd.data.device.DeviceIdProvider
import com.example.project_lttbd.data.device.DeviceManager
import com.example.project_lttbd.data.remote.FirebaseProvider
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MMRAFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM registration token: $token")

        val currentUid = FirebaseProvider.auth.currentUser?.uid
        if (!currentUid.isNullOrBlank()) {
            val deviceId = DeviceIdProvider.getDeviceId(applicationContext)
            DeviceManager.updateFcmToken(currentUid, deviceId, token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Message received from: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title
            ?: remoteMessage.data["title"]
            ?: "Nhắc nhở từ MMRA"

        val body = remoteMessage.notification?.body
            ?: remoteMessage.data["body"]
            ?: "Bạn có thông báo mới về lịch dùng thuốc."

        NotificationHelper.showNotification(
            context = applicationContext,
            title = title,
            body = body,
            data = remoteMessage.data
        )
    }

    companion object {
        private const val TAG = "MMRAFirebaseMsgService"
    }
}
```

- [ ] **Step 5: Register service and permissions in AndroidManifest.xml**

Cập nhật `app/src/main/AndroidManifest.xml`:
- Thêm quyền: `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />`
- Đăng ký service trong `<application>`:
```xml
        <service
            android:name=".notifications.MMRAFirebaseMessagingService"
            android:exported="false">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
            </intent-filter>
        </service>
```

- [ ] **Step 6: Run tests and verify**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/example/project_lttbd/notifications/ app/src/test/java/com/example/project_lttbd/notifications/ app/src/main/AndroidManifest.xml
git commit -m "feat(fcm): add MMRAFirebaseMessagingService and NotificationHelper with Android 13+ support"
```

---

### Task 4: Notification Permission Request & Notification Intent Routing in MainActivity

**Files:**
- Modify: `app/src/main/java/com/example/project_lttbd/MainActivity.kt`
- Test: `app/src/test/java/com/example/project_lttbd/MainActivityNavigationTest.kt`

**Interfaces:**
- Consumes: `NotificationHelper.EXTRA_DESTINATION`, `NotificationHelper.EXTRA_DOSE_ID`, `DeviceManager.syncCurrentDevice`
- Produces: Runtime permission request for `POST_NOTIFICATIONS`, deep link routing when notification is tapped.

- [ ] **Step 1: Modify MainActivity to request notification permissions & route intents**

Chỉnh sửa `app/src/main/java/com/example/project_lttbd/MainActivity.kt`:
- Thêm `rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission())` cho `POST_NOTIFICATIONS` trên Android 13+ (Build.VERSION.SDK_INT >= 33).
- Đồng bộ thiết bị với Firestore khi khởi động nếu đã có `FirebaseProvider.auth.currentUser`.
- Bổ sung `override fun onNewIntent(intent: Intent)` để hứng Intent khi bấm thông báo lúc app đang chạy ngầm.
- Xử lý điều hướng tới `extra_destination` nếu có trong Intent.

- [ ] **Step 2: Run unit tests and assembleDebug**

Run: `.\gradlew.bat testDebugUnitTest`
Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL (tạo file APK debug hợp lệ).

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/example/project_lttbd/MainActivity.kt
git commit -m "feat(navigation): handle notification deep linking and runtime POST_NOTIFICATIONS permission"
```

---

### Task 5: Integration Hooks & Documentation for Testing

**Files:**
- Create: `docs/fcm-testing-guide.md`
- Test: Full unit test suite verification

**Interfaces:**
- Consumes: Firebase Console Messaging interface, Device ID, FCM Token
- Produces: Detailed step-by-step test instructions for Huy and team members.

- [ ] **Step 1: Write FCM Testing Guide for Firebase Console**

Tạo file `docs/fcm-testing-guide.md`:
- Hướng dẫn lấy `deviceId` và `fcmToken` từ Logcat (`tag:MMRAFirebaseMsgService` hoặc `tag:DeviceManager`).
- Các bước gửi thông báo test từ Firebase Console:
  1. Vào Firebase Console -> MMRA Dev -> **Messaging** -> **Create your first campaign** (hoặc **New campaign** -> **Firebase Notification messages**).
  2. Điền Title: `Nhắc uống thuốc (Thử nghiệm)` và Body: `Đến giờ uống Metformin 500mg`.
  3. Bấm **Send test message**, nhập FCM token của thiết bị hoặc chọn ứng dụng `com.example.project_lttbd`.
  4. Thêm custom data (tùy chọn): key `destination` value `schedule`.
  5. Gửi thông báo và xác nhận:
     - Khi app đang mở (foreground): Hiển thị thông báo trên thanh trạng thái với icon chuông.
     - Khi app chạy nền (background): Hiển thị thông báo; khi bấm vào sẽ mở màn hình `schedule`.
- Hướng dẫn kiểm tra Firestore Database: `users/{uid}/devices/{deviceId}` hiển thị đúng token, platform `android`, `notificationsEnabled` và `updatedAt`.

- [ ] **Step 2: Run full build and test verification**

Run: `.\gradlew.bat testDebugUnitTest`
Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add docs/fcm-testing-guide.md
git commit -m "docs(fcm): add step-by-step testing guide for Firebase Console notifications"
```
