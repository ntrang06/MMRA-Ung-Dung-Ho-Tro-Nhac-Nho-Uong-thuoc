# Medication Reminders & Notification System Implementation Plan (BE2 & BE3)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Triển khai hệ thống lập, cập nhật, hủy thông báo nhắc uống thuốc chính xác trên Android qua `AlarmManager`, bấm thông báo mở đúng liều, lưu trữ danh sách thông báo trên máy độc lập theo từng `UID` (tách biệt tài khoản A và B không lẫn nhau), nối nút chuông trên Home và khôi phục lịch nhắc sau khởi động máy theo phân công BE2 (Deadline 20h 12/11) & BE3 (Deadline 8h 14/11).

**Architecture:**
1. `MedicationAlarmScheduler`: Quản lý đặt báo thức chính xác bằng `AlarmManager.setExactAndAllowWhileIdle()` hoặc `setAlarmClock()` cho các liều thuốc `PENDING` theo `scheduledAt` hoặc `nextReminderAt`, hủy và cập nhật linh hoạt theo `doseId`.
2. `MedicationAlarmReceiver`: `BroadcastReceiver` tiếp nhận sự kiện báo thức khi đến giờ uống thuốc, tạo thông báo hệ thống qua `NotificationHelper` kèm âm thanh/rung, và tự động lưu bản ghi thông báo vào `LocalNotificationStore`.
3. `LocalNotificationStore`: Bộ lưu trữ thông báo cục bộ được phân vùng tuyệt đối theo `UID` người dùng (`notifications_{uid}.json`), bảo đảm khi đổi tài khoản không bao giờ rò rỉ dữ liệu giữa tài khoản A và B.
4. `NotificationHistoryScreen`: Màn hình hộp thư thông báo được nối với icon Chuông trên `HomeHeader`, hiển thị danh sách thông báo nhận được theo thời gian, cho phép bấm vào để điều hướng thẳng đến chi tiết liều (`MMRADestinations.REMINDER`), hỗ trợ đánh dấu đã đọc và xóa thông báo.
5. `MedicationReminderSync`: Module kết nối đồng bộ giữa danh sách liều uống và hệ thống báo thức: tự động lập lịch cho liều `PENDING`, lập tức hủy báo thức cho liều đã uống (`TAKEN`), đã bỏ qua (`SKIPPED`) hoặc bị hủy (`CANCELLED`).
6. `MedicationBootReceiver`: Lắng nghe sự kiện `BOOT_COMPLETED` để tự động khôi phục lại toàn bộ báo thức cho các liều `PENDING` sau khi người dùng khởi động lại máy (yêu cầu BE3).
7. `AccountSessionHook`: Tích hợp với `AuthViewModel` khi đăng xuất hoặc chuyển tài khoản để hủy toàn bộ alarms và dọn sạch thông báo đang hiển thị của tài khoản cũ.

**Tech Stack:** Kotlin, Jetpack Compose, Android `AlarmManager`, `NotificationManagerCompat`, `BroadcastReceiver`, `RECEIVE_BOOT_COMPLETED`, AndroidX Security/SharedPreferences/JSON, JUnit4.

**Spec:** `docs/medication-api-contract-v1.md`, Bảng phân công BE2 (Deadline 20h 12/11) & BE3 (Deadline 8h 14/11).

---

## Global Constraints

- Android Application ID: `com.example.project_lttbd`
- Root package: `com.example.project_lttbd`
- Contract DTO: `com.example.project_lttbd.data.remote.dto.DoseResponseDto`, `DoseStatusDto`
- Channel ID thông báo: `NotificationHelper.CHANNEL_ID_REMINDERS` (`mmra_medication_reminders`)
- Điều hướng liều: `MMRADestinations.REMINDER` (`reminder/{doseId}?readOnly={readOnly}`)
- Tách biệt dữ liệu: Mọi thông báo và báo thức cục bộ phải gắn chặt với `UID` của người dùng hiện tại; đổi tài khoản phải làm mới hoàn toàn.
- Kiểm thử: Tất cả các thay đổi phải vượt qua `./gradlew testDebugUnitTest` và không phá vỡ bất kỳ màn hình nào hiện có.
- Nhánh phát triển: `feature/huy`.

---

## Review Focus

1. **Doze Mode / Exact Alarm Trigger:** Thông báo nhắc thuốc phải nổ chuông và rung chính xác tại mốc thời gian đã định kể cả khi điện thoại tắt màn hình hoặc đang ở chế độ tiết kiệm pin (`setExactAndAllowWhileIdle`).
2. **Account Isolation (A/B Separation):** Khi User A đăng xuất và User B đăng nhập trên cùng thiết bị, toàn bộ chuông báo của User A bị hủy, hộp thư thông báo chỉ hiển thị danh sách của User B, không lẫn lộn bất kỳ thông tin nào.
3. **Immediate Alarm Cancellation on Action:** Khi một liều thuốc được xác nhận đã uống (`TAKEN`) hoặc bỏ qua (`SKIPPED`), báo thức của liều đó (`doseId`) phải lập tức bị hủy để không tiếp tục kêu sau khi đồng bộ.
4. **Cold Start & Deep Link Navigation:** Nhấn vào thông báo nhắc thuốc từ thanh trạng thái của Android khi ứng dụng đã đóng hoàn toàn phải khởi động app, kiểm tra phiên đăng nhập, và mở thẳng màn hình chi tiết liều (`DoseReminderScreen` tương ứng với `doseId`).
5. **Reboot Recovery:** Sau khi khởi động lại máy điện thoại, các liều `PENDING` còn lại trong ngày phải được khôi phục báo thức tự động mà không cần người dùng phải mở lại app.

---

## Task Decomposition

### Task 1: Local Notification Model & UID-Isolated Storage

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/notifications/model/LocalNotificationItem.kt`
- Create: `app/src/main/java/com/example/project_lttbd/notifications/storage/LocalNotificationStore.kt`
- Test: `app/src/test/java/com/example/project_lttbd/notifications/storage/LocalNotificationStoreTest.kt`

**Interfaces:**
- `LocalNotificationItem(id: String, doseId: String, title: String, body: String, timestampMillis: Long, isRead: Boolean, destinationRoute: String)`
- `LocalNotificationStore.getNotifications(context: Context, uid: String): List<LocalNotificationItem>`
- `LocalNotificationStore.addNotification(context: Context, uid: String, item: LocalNotificationItem)`
- `LocalNotificationStore.markAsRead(context: Context, uid: String, notificationId: String)`
- `LocalNotificationStore.markAllAsRead(context: Context, uid: String)`
- `LocalNotificationStore.clearAll(context: Context, uid: String)`

- [x] **Step 1: Write Unit Test verifying UID Isolation**
Tạo file test `LocalNotificationStoreTest.kt` kiểm tra:
1. Lưu thông báo cho User A và User B.
2. Kiểm tra `getNotifications(context, "uid_A")` chỉ trả về thông báo của A.
3. `getNotifications(context, "uid_B")` chỉ trả về thông báo của B.
4. Xóa thông báo của A không ảnh hưởng tới B.

- [x] **Step 2: Implement LocalNotificationItem data model**
Tạo file `LocalNotificationItem.kt` chứa cấu trúc bản ghi thông báo đầy đủ các trường phục vụ hiển thị và điều hướng.

- [x] **Step 3: Implement LocalNotificationStore**
Tạo file `LocalNotificationStore.kt` sử dụng file JSON cục bộ lưu trong thư mục `context.filesDir` theo tên `mmra_notifications_${uid}.json` để đảm bảo lưu trữ an toàn, nhẹ, không xung đột DB và cô lập 100% theo `uid`.

- [x] **Step 4: Run test and verify pass**
Chạy `./gradlew testDebugUnitTest --tests com.example.project_lttbd.notifications.storage.LocalNotificationStoreTest`.

- [x] **Step 5: Commit changes**
`git commit -m "feat(notifications): add UID-isolated local notification storage"`

---

### Task 2: Medication Alarm Scheduler & Alarm BroadcastReceiver

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/notifications/alarm/MedicationAlarmScheduler.kt`
- Create: `app/src/main/java/com/example/project_lttbd/notifications/alarm/MedicationAlarmReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml` (Khai báo Receiver và Permissions)
- Test: `app/src/test/java/com/example/project_lttbd/notifications/alarm/MedicationAlarmSchedulerTest.kt`

**Interfaces:**
- `MedicationAlarmScheduler.schedule(context: Context, uid: String, doseId: String, triggerAtMillis: Long, title: String, message: String, medicineName: String)`
- `MedicationAlarmScheduler.cancel(context: Context, doseId: String)`
- `MedicationAlarmScheduler.cancelAll(context: Context, uid: String)`
- `MedicationAlarmReceiver`: Kế thừa `BroadcastReceiver`, nhận `doseId`, `uid`, `title`, `message`, gọi `NotificationHelper.showNotification()` và ghi vào `LocalNotificationStore`.

- [x] **Step 1: Write Unit Test for MedicationAlarmScheduler**
Viết unit test kiểm tra tính toán request code từ `doseId`, kiểm tra logic tính giờ phát báo thức.

- [x] **Step 2: Add permissions and receiver to AndroidManifest.xml**
Bổ sung `SCHEDULE_EXACT_ALARM`, `USE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED`, `VIBRATE` và khai báo `<receiver android:name=".notifications.alarm.MedicationAlarmReceiver" android:exported="false" />`.

- [x] **Step 3: Implement MedicationAlarmScheduler**
Xây dựng logic đặt báo thức chính xác `AlarmManager.setExactAndAllowWhileIdle()` với fallback phù hợp cho từng phiên bản Android, quản lý PendingIntent theo `doseId.hashCode()`.

- [x] **Step 4: Implement MedicationAlarmReceiver**
Xử lý sự kiện báo thức:
1. Trích xuất thông tin liều từ Intent Extras.
2. Kiểm tra nếu `uid` trùng khớp với `FirebaseProvider.auth.currentUser?.uid` (nếu khác -> bỏ qua vì tài khoản đã đổi).
3. Hiển thị thông báo qua `NotificationHelper`.
4. Lưu vào `LocalNotificationStore` cho `uid`.

- [x] **Step 5: Run tests and verify**
Chạy `./gradlew testDebugUnitTest` đảm bảo compile sạch và test pass.

- [x] **Step 6: Commit changes**
`git commit -m "feat(notifications): implement MedicationAlarmScheduler and MedicationAlarmReceiver"`

---

### Task 3: Deep-Link Notification Navigation & NotificationHelper Enhancements

**Files:**
- Modify: `app/src/main/java/com/example/project_lttbd/notifications/NotificationHelper.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/MainActivity.kt`
- Test: `app/src/test/java/com/example/project_lttbd/notifications/NotificationHelperRouteTest.kt`

**Interfaces:**
- `NotificationHelper.showMedicationReminderNotification(context, uid, doseId, title, message)`
- `NotificationHelper.cancelNotification(context, doseId)`
- `NotificationHelper.cancelAllNotifications(context)`

- [x] **Step 1: Write Unit Test for Notification Route Parsing**
Kiểm tra hàm tạo route từ `doseId` trỏ đúng vào `reminder/{doseId}?readOnly=false`.

- [x] **Step 2: Update NotificationHelper**
Bổ sung hàm tạo thông báo riêng cho nhắc thuốc với icon chuông, nút mở nhanh liều, âm thanh báo thức `NotificationCompat.CATEGORY_ALARM`, rung và gán tag theo `doseId` để có thể hủy trực tiếp khi liều đã hoàn thành.

- [x] **Step 3: Update MainActivity intent extraction**
Đảm bảo khi bấm vào thông báo nhắc thuốc:
1. `MainActivity` đọc được `EXTRA_DOSE_ID` hoặc `EXTRA_DESTINATION`.
2. Kiểm tra người dùng hiện tại có đúng UID không.
3. Điều hướng chính xác tới màn hình liều thuốc `MMRADestinations.REMINDER`.

- [x] **Step 4: Verify build and tests**
Chạy `./gradlew testDebugUnitTest`.

- [x] **Step 5: Commit changes**
`git commit -m "feat(notifications): support deep linking to dose reminder from notification"`

---

### Task 4: Notification History Screen & Home Bell Button Connection

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/notifications/NotificationHistoryScreen.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/MMRADestinations.kt` (thêm `NOTIFICATIONS`)
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/MMRANavHost.kt` (thêm composable destination)
- Modify: `app/src/main/java/com/example/project_lttbd/ui/screens/home/HomeScreen.kt` (nối sự kiện `onNotificationsClick`)

- [x] **Step 1: Add NOTIFICATIONS route in MMRADestinations**
Thêm `const val NOTIFICATIONS = "notifications"` vào `MMRADestinations.kt`.

- [x] **Step 2: Implement NotificationHistoryScreen UI**
Xây dựng giao diện Composable `NotificationHistoryScreen`:
- Thanh tiêu đề TopBar với nút Quay lại (`onBackClick`) và nút "Xóa tất cả" / "Đánh dấu đã đọc".
- Danh sách thông báo (`LazyColumn`) hiển thị: Tiêu đề thuốc, thời gian nhận, trạng thái đã đọc/chưa đọc.
- Bấm vào thông báo -> Đánh dấu đã đọc và gọi `onNotificationClick(item.doseId)` -> điều hướng đến liều thuốc tương ứng.
- Trạng thái rỗng (Empty state) khi chưa có thông báo nào với icon chuông tinh tế.

- [x] **Step 3: Connect in MMRANavHost and HomeScreen**
- Trong `MMRANavHost.kt`, thêm `composable(MMRADestinations.NOTIFICATIONS)`.
- Trong `HomeScreen.kt`, truyền callback `onNotificationsClick` điều hướng tới `navController.navigate(MMRADestinations.NOTIFICATIONS)`.

- [x] **Step 4: Verify UI build**
Chạy `./gradlew assembleDebug` để đảm bảo không lỗi giao diện.

- [x] **Step 5: Commit changes**
`git commit -m "feat(ui): add NotificationHistoryScreen and connect Home bell icon"`

---

### Task 5: Synchronization & Auto-Cancellation (Dose Actions & Status Changes)

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/notifications/sync/MedicationReminderSync.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/MMRANavHost.kt` (tích hợp khi xác nhận liều)
- Test: `app/src/test/java/com/example/project_lttbd/notifications/sync/MedicationReminderSyncTest.kt`

**Interfaces:**
- `MedicationReminderSync.syncDoses(context: Context, uid: String, doses: List<DoseResponseDto>)`
- `MedicationReminderSync.onDoseAction(context: Context, doseId: String, action: DoseActionDto)`

- [x] **Step 1: Write Unit Test for Synchronization Logic**
Kiểm tra:
1. Liều `PENDING` có giờ uống trong tương lai -> Gọi `scheduler.schedule()`.
2. Liều `TAKEN` / `SKIPPED` / `CANCELLED` -> Gọi `scheduler.cancel()` và hủy thông báo trên thanh trạng thái.

- [x] **Step 2: Implement MedicationReminderSync**
Cài đặt hàm `syncDoses` duyệt qua danh sách liều trả về từ API/cache:
- Đặt báo thức cho các liều `PENDING`.
- Hủy báo thức cho các liều không còn ở trạng thái `PENDING`.
- Xử lý hàm `onDoseAction`: khi người dùng bấm "Đã uống" hoặc "Bỏ qua", ngay lập tức hủy báo thức và thông báo của liều đó mà không cần chờ mạng đồng bộ.

- [x] **Step 3: Connect into MMRANavHost dose actions**
Khi `onTaken` hoặc `onSkip` được xác nhận trên `DoseReminderScreen` hoặc `HomeScreen`, gọi `MedicationReminderSync.onDoseAction(context, dose.id, action)`.

- [x] **Step 4: Run tests and verify**
Chạy `./gradlew testDebugUnitTest`.

- [x] **Step 5: Commit changes**
`git commit -m "feat(notifications): auto-cancel reminders when dose is taken or skipped"`

---

### Task 6: Account Sign-Out & Switch Cleanup

**Files:**
- Modify: `app/src/main/java/com/example/project_lttbd/ui/viewmodel/AuthViewModel.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/LogoutDestination.kt`

- [x] **Step 1: Integrate cleanup on signOut()**
Trong `AuthViewModel.signOut()`, trước hoặc ngay khi phiên đăng nhập kết thúc:
1. Gọi `MedicationAlarmScheduler.cancelAll(appContext, currentUid)`.
2. Gọi `NotificationHelper.cancelAllNotifications(appContext)`.
3. Đảm bảo toàn bộ báo thức và thông báo thanh trạng thái của tài khoản cũ được dọn dẹp sạch sẽ.

- [x] **Step 2: Integrate cleanup on SwitchAccount**
Trong `SwitchAccountDestination.kt` và luồng đổi tài khoản, thực hiện thao tác hủy báo thức tương tự.

- [x] **Step 3: Run unit tests to verify auth flow remains healthy**
Chạy `./gradlew testDebugUnitTest`.

- [x] **Step 4: Commit changes**
`git commit -m "fix(auth): cancel all reminders and clear notifications on sign out"`

---

### Task 7: Boot Restoration Receiver & Snooze Reminder (BE3 Prep)

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/notifications/alarm/MedicationBootReceiver.kt`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/java/com/example/project_lttbd/notifications/alarm/MedicationAlarmScheduler.kt` (hỗ trợ snooze)
- Test: `app/src/test/java/com/example/project_lttbd/notifications/alarm/MedicationBootReceiverTest.kt`

- [x] **Step 1: Implement MedicationBootReceiver**
Tạo `MedicationBootReceiver`:
- Lắng nghe `Intent.ACTION_BOOT_COMPLETED`.
- Kiểm tra `FirebaseProvider.auth.currentUser?.uid`. Nếu có người dùng đang đăng nhập:
  - Tải danh sách liều lưu trong cache hoặc đồng bộ lại từ API.
  - Gọi `MedicationReminderSync.syncDoses()` để tái thiết lập toàn bộ báo thức cho các liều `PENDING` còn lại trong ngày.

- [x] **Step 2: Register BootReceiver in AndroidManifest.xml**
Khai báo receiver với intent filter `BOOT_COMPLETED`.

- [x] **Step 3: Implement Snooze Reminders logic**
Thêm hàm `scheduleSnooze(context, uid, doseId, minutes, ...)` cho phép đặt chuông nhắc lại sau 5, 10, 15 hoặc 30 phút theo chuẩn `medication-api-contract-v1.md`.

- [x] **Step 4: Run tests and verify**
Chạy `./gradlew testDebugUnitTest`.

- [x] **Step 5: Commit changes**
`git commit -m "feat(notifications): implement boot restoration receiver and snooze reminders"`

---

### Task 8: End-to-End Verification & Real-Device Testing Guide

**Files:**
- Create: `docs/notification-testing-guide.md`

- [x] **Step 1: Run full test suite**
Chạy `./gradlew testDebugUnitTest` và `./gradlew assembleDebug` đảm bảo toàn bộ dự án compile và test pass 100%.

- [x] **Step 2: Document Real-Device Testing Steps**
Soạn hướng dẫn kiểm thử chi tiết trong `docs/notification-testing-guide.md`:
1. Cách thiết lập quyền thông báo và kiểm tra trên Android 13+.
2. Cách kích hoạt báo thức thử nghiệm 1 phút để test nổ chuông trên máy thật.
3. Kiểm tra bấm thông báo mở đúng màn hình liều thuốc.
4. Kiểm tra bấm "Đã uống" -> thông báo biến mất và không báo lại.
5. Kiểm tra đăng xuất và đổi sang tài khoản khác -> hộp thư thông báo và báo thức tách biệt hoàn toàn.

- [x] **Step 3: Commit documentation**
`git commit -m "docs: add medication reminders and notification testing guide"`
