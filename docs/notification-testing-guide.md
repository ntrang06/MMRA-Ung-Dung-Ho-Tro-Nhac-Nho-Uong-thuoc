# Hướng Dẫn Kiểm Thử Hệ Thống Nhắc Thuốc & Thông Báo (BE2 & BE3)
**Người phụ trách:** Huy (Phân hệ Quét thuốc, Gia đình & Nhắc nhở Uống thuốc)  
**Nhánh:** `feature/huy`  
**Dự án:** MMRA - Ứng Dụng Hỗ Trợ Nhắc Nhở Uống Thuốc

---

## 📌 1. Tổng quan Kiến trúc

Hệ thống nhắc nhở và thông báo thuốc được hoàn thiện đầy đủ theo yêu cầu của **BE (2)** và sẵn sàng cho **BE (3)** với các thành phần cốt lõi:

| Thành phần | Đường dẫn | Chức năng chính |
| :--- | :--- | :--- |
| **`MedicationAlarmScheduler`** | `notifications/alarm/MedicationAlarmScheduler.kt` | Đặt báo thức chính xác qua `AlarmManager.setExactAndAllowWhileIdle()`, hủy chuông theo liều hoặc hủy toàn bộ theo `uid`. Hỗ trợ tính toán hoãn chuông (`scheduleSnooze`). |
| **`MedicationAlarmReceiver`** | `notifications/alarm/MedicationAlarmReceiver.kt` | Nhận intent báo thức, kiểm tra UID người dùng hiện tại, phát Notification có rung/âm thanh và lưu vào kho lưu trữ nội bộ. |
| **`LocalNotificationStore`** | `notifications/storage/LocalNotificationStore.kt` | Lưu lịch sử thông báo cục bộ tách biệt hoàn toàn theo UID (`notifications_{uid}.json`). |
| **`NotificationHistoryScreen`** | `ui/screens/notifications/NotificationHistoryScreen.kt` | Màn hình xem danh sách thông báo, đánh dấu đã đọc, xóa từng thông báo hoặc xóa tất cả. |
| **`MainActivity` Deep-Link** | `MainActivity.kt` | Nhận Intent từ thông báo và điều hướng thẳng vào màn hình liều thuốc (`reminder/{doseId}`) hoặc hộp thư thông báo (`notifications`). |
| **`MedicationReminderSync`** | `notifications/sync/MedicationReminderSync.kt` | Tự động hủy báo thức và xóa notification trên thanh trạng thái khi liều chuyển sang `TAKEN` hoặc `SKIPPED`. |
| **`MedicationBootReceiver`** | `notifications/alarm/MedicationBootReceiver.kt` | Lắng nghe `BOOT_COMPLETED` để tự động lập lịch lại các liều `PENDING` trong ngày sau khi khởi động lại máy. |
| **Sign-Out Cleanup** | `ui/viewmodel/AuthViewModel.kt` | Khi người dùng Đăng xuất hoặc Chuyển tài khoản, tự động hủy toàn bộ báo thức và xóa sạch các thông báo đang hiển thị của tài khoản đó. |

---

## 📱 2. Hướng Dẫn Kiểm Thử Trên Thiết Bị Thật (ADB)

Thiết bị hiện đang kết nối: `af7a32ac` (Android 16). Ứng dụng đã được build và cài đặt thành công (`com.example.project_lttbd`).

### 2.1 Cấp quyền thông báo (Notification Permission)
Trên Android 13 trở lên, ứng dụng cần quyền hiển thị thông báo:
1. Mở app **MMRA** trên điện thoại.
2. Nếu app hỏi quyền *"Cho phép gửi thông báo"*, nhấn **Cho phép (Allow)**.
3. Hoặc vào **Cài đặt điện thoại** > **Ứng dụng** > **MMRA** > **Thông báo** > Bật **Tất cả thông báo**.

---

### 2.2 Kiểm tra Icon Chuông trên Trang Chủ (HomeHeader)
1. Mở ứng dụng MMRA vào màn hình chính (**Trang chủ**).
2. Góc trên bên phải thanh tiêu đề có icon quả chuông thông báo.
3. Nhấn vào quả chuông -> Ứng dụng sẽ chuyển sang màn hình **Thông báo** (`NotificationHistoryScreen`).
4. Tại đây hiển thị danh sách các thông báo đã nhận. Bạn có thể nhấn *"Đánh dấu đã đọc tất cả"* hoặc xóa thông báo.

---

### 2.3 Thử nghiệm Kích hoạt Báo thức & Nổ Thông Báo bằng ADB
Để kiểm tra `MedicationAlarmReceiver` nổ chuông và phát thông báo ngay lập tức, chạy lệnh ADB:

```bash
adb -s af7a32ac shell "am broadcast -a com.example.project_lttbd.ACTION_MEDICATION_ALARM --es extra_alarm_uid test_uid --es extra_alarm_dose_id 1_today_morning --es extra_alarm_title 'Đến giờ uống thuốc: Metformin' --es extra_alarm_message '1 viên • Uống sau ăn sáng' --es extra_alarm_medicine_name 'Metformin' -p com.example.project_lttbd"
```

**Kết quả mong đợi:**
- Điện thoại rung và hiển thị một thông báo trên thanh trạng thái:
  - **Tiêu đề:** *Đến giờ uống thuốc: Metformin*
  - **Nội dung:** *1 viên • Uống sau ăn sáng*
- Thông báo này được tự động lưu vào màn hình Thông báo của ứng dụng.

---

### 2.4 Thử nghiệm Nhấn Thông Báo để Mở Màn hình Chi tiết Liều (Deep Linking)
1. Kéo thanh thông báo của điện thoại xuống và chạm vào thông báo vừa nhận.
2. Ứng dụng MMRA lập tức mở ra và chuyển thẳng vào màn hình **Nhắc nhở liều thuốc** (`DoseReminderScreen`) tương ứng với liều thuốc đó.
3. Bạn cũng có thể kiểm tra trực tiếp luồng Deep-Link bằng lệnh ADB:
```bash
adb -s af7a32ac shell "am start -n com.example.project_lttbd/.MainActivity --es extra_notification_route 'reminder/1|today|morning?readOnly=false' --es extra_dose_id '1|today|morning'"
```

---

### 2.5 Kiểm tra Tự Động Hủy Thông Báo Khi "Đã Uống" hoặc "Bỏ Qua"
1. Tại màn hình liều thuốc (`DoseReminderScreen`), nhấn nút **"Đã uống"** hoặc **"Bỏ qua"**.
2. Xác nhận hành động trong hộp thoại.
3. **Kết quả mong đợi:**
   - Trạng thái liều được cập nhật.
   - Thông báo nhắc liều thuốc đó trên thanh trạng thái biến mất ngay lập tức (`MedicationReminderSync.onDoseAction` tự động gọi `NotificationHelper.cancelNotification`).
   - Báo thức tương ứng trong `AlarmManager` được hủy để không bị báo lại.

---

### 2.6 Kiểm tra Chức Năng Hoãn Báo Thức ("Nhắc Lại" / Snooze)
1. Tại màn hình liều thuốc, nhấn nút **"Nhắc lại"** (Snooze).
2. **Kết quả mong đợi:**
   - Ứng dụng đặt báo thức mới sau 10 phút (hoặc theo cấu hình snooze) bằng `MedicationAlarmScheduler.scheduleSnooze`.
   - Hiển thị thông báo phản hồi cho người dùng.

---

### 2.7 Kiểm tra Đăng Xuất & Cách Ly Tài Khoản (UID Isolation)
1. Vào mục **Tài khoản** > chọn **Đăng xuất** hoặc **Chuyển tài khoản**.
2. Xác nhận Đăng xuất.
3. **Kết quả mong đợi:**
   - Toàn bộ báo thức trong `AlarmManager` của tài khoản vừa đăng xuất bị hủy sạch (`MedicationAlarmScheduler.cancelAll`).
   - Toàn bộ thông báo đang hiển thị trên thanh trạng thái bị xóa (`NotificationHelper.cancelAllNotifications`).
   - Khi đăng nhập tài khoản khác (User B), User B sẽ không thấy lịch sử thông báo của User A (`LocalNotificationStore` lưu trữ độc lập theo tên file `notifications_{uid}.json`).

---

### 2.8 Kiểm tra Khôi Phục Báo Thức Sau Khi Khởi Động Máy (Boot Restoration)
Trên Android, sự kiện `BOOT_COMPLETED` là broadcast đặc quyền chỉ hệ điều hành (system/root) được phép phát. Để kiểm thử tính năng này qua ADB mà không cần khởi động lại máy điện thoại, chạy lệnh:
```bash
adb -s af7a32ac shell "am broadcast -a com.example.project_lttbd.ACTION_TEST_BOOT --es extra_uid test_uid -p com.example.project_lttbd"
```
Hoặc khi khởi động lại máy thật bằng `adb reboot`, hệ thống Android sẽ tự động phát `BOOT_COMPLETED` kích hoạt `MedicationBootReceiver`.

**Kết quả mong đợi:**
- `MedicationBootReceiver` nhận sự kiện, kiểm tra tài khoản đang đăng nhập và quét các liều thuốc `PENDING` trong ngày còn thời gian ở tương lai để tự động đặt lại `AlarmManager`.

---

## 🧪 3. Kết Quả Chạy Unit Tests

Toàn bộ Unit Test của hệ thống nhắc nhở và thông báo đều pass 100%:
- `LocalNotificationStoreTest`: Kiểm tra cách ly thông báo theo UID giữa User A và User B, đọc/ghi JSON, xóa thông báo.
- `MedicationAlarmSchedulerTest`: Kiểm tra mã requestCode duy nhất, lọc thời gian quá khứ, tính toán hoãn giờ (snooze 5/10/15/30 phút).
- `NotificationHelperRouteTest`: Kiểm tra whitelist điều hướng an toàn trong `MainActivity` (cho phép `reminder/{doseId}`, `notifications`, chặn route độc hại).
- `MedicationReminderSyncTest`: Kiểm tra logic kích hoạt / hủy báo thức tự động theo trạng thái liều (`PENDING`, `TAKEN`, `SKIPPED`, `CANCELLED`, `MISSED`).
- `MedicationBootReceiverTest`: Kiểm tra chỉ khôi phục các liều chưa hoàn thành và có thời gian ở tương lai.

Lệnh chạy kiểm tra tự động:
```bash
.\gradlew.bat testDebugUnitTest
```
Kết quả: **`BUILD SUCCESSFUL`**
