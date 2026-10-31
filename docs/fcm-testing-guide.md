# Hướng dẫn Kiểm thử Thông báo FCM & Quản lý Thiết bị (MMRA)

Tài liệu hướng dẫn dành cho thành viên Huy và nhóm kiểm thử chức năng nhận thông báo đẩy Firebase Cloud Messaging (FCM) và quản lý thiết bị trên Cloud Firestore.

---

## 1. Cơ chế hoạt động

1. **Khởi tạo Device ID:**
   - Mỗi thiết bị cài đặt ứng dụng MMRA sẽ được cấp một UUID cố định qua `DeviceIdProvider`.
   - ID này được lưu vĩnh viễn trong `SharedPreferences` (`mmra_device_preferences`) và không đổi qua các lần mở app.

2. **Lưu trữ trên Firestore:**
   - Khi người dùng đăng nhập hoặc mở app có sẵn phiên, `DeviceManager.syncCurrentDevice` sẽ lấy FCM Token và lưu vào Firestore tại:
     ```text
     users/{uid}/devices/{deviceId}
     ```
   - Schema `DeviceDto`:
     - `deviceId`: String (UUID của máy)
     - `fcmToken`: String (Token nhận thông báo đẩy từ Firebase)
     - `platform`: "android"
     - `notificationsEnabled`: Boolean (trạng thái quyền thông báo)
     - `updatedAt`: Timestamp (thời gian cập nhật gần nhất)

3. **Gỡ bỏ khi đăng xuất:**
   - Khi người dùng bấm đăng xuất, `DeviceManager.removeDevice` được gọi để xóa tài liệu thiết bị khỏi Firestore của tài khoản cũ, ngăn chặn nhận thông báo nhầm.

---

## 2. Các bước lấy FCM Token & Device ID để kiểm thử

1. Chạy ứng dụng trên máy thật hoặc máy ảo Android (kết nối Android Studio hoặc cáp USB).
2. Mở cửa sổ **Logcat**, lọc theo từ khóa:
   ```text
   package:mine (tag:DeviceManager | tag:MMRAFirebaseMsgService)
   ```
3. Khởi động app và đăng nhập:
   - Logcat sẽ in ra log thành công:
     ```text
     D/DeviceManager: Successfully synced device <DEVICE_ID> for user <UID>
     ```
   - Hoặc khi token được làm mới:
     ```text
     D/MMRAFirebaseMsgService: Refreshed FCM registration token: <FCM_TOKEN>
     ```
4. Sao chép chuỗi `<FCM_TOKEN>` này để sử dụng gửi thông báo thử nghiệm.

---

## 3. Các bước gửi thông báo thử từ Firebase Console

1. Truy cập vào Firebase Console của dự án:
   👉 **[Firebase Console MMRA Dev](https://console.firebase.google.com/project/mmra-dev/overview)**
2. Ở thanh menu bên trái, vào mục **Engage (Tương tác)** > **Messaging (Nhắn tin qua đám mây)**.
3. Bấm **Create your first campaign** (hoặc nút **New campaign** > chọn **Firebase Notification messages**).
4. **Bước 1: Soạn thông báo (Notification message):**
   - **Notification title:** `Nhắc nhở uống thuốc`
   - **Notification text:** `Đến giờ uống Metformin 500mg (sau bữa ăn)`
5. **Gửi tin nhắn thử (Test message):**
   - Bấm nút **Send test message** ở góc bên phải.
   - Dán chuỗi **FCM registration token** đã lấy ở Bước 2 vào ô trống, bấm dấu `+`, sau đó bấm **Test**.
6. **Kiểm tra kết quả nhận thông báo trên thiết bị:**
   - **Trường hợp 1 (App đang mở / Foreground):**
     - Ứng dụng tự động kích hoạt `NotificationHelper.showNotification()`.
     - Thông báo hiện trên thanh thông báo với icon chuông và tiêu đề/nội dung tương ứng.
   - **Trường hợp 2 (App đang chạy ngầm / Background hoặc đã đóng):**
     - Hệ thống Android hiển thị thông báo với Notification Channel `mmra_medication_reminders` ("Nhắc nhở uống thuốc MMRA").
     - Khi người dùng nhấn vào thông báo: Ứng dụng mở lên và hiển thị màn hình chính `home`.

---

## 4. Kiểm thử điều hướng màn hình cụ thể qua Custom Data

Khi tạo thông báo trên Firebase Console:
1. Chuyển sang phần **Additional options (Tùy chọn bổ sung)** > **Custom data**.
2. Thêm cặp key-value sau:
   - Key: `destination` — Value: `schedule` (hoặc `medicine`, `history`, `family`)
3. Bấm gửi thông báo thử.
4. Khi nhấn vào thông báo trên điện thoại, `MainActivity` sẽ đọc `extra_destination` và tự động điều hướng thẳng đến màn hình **Lịch uống (`schedule`)**.

---

## 5. Kiểm tra phân quyền đa thiết bị (Multi-device Check)

1. Đăng nhập cùng một tài khoản email trên Thiết bị A (máy ảo) và Thiết bị B (điện thoại thật).
2. Vào Firestore Database trên Firebase Console > Collection `users/{uid}/devices`:
   - Xác nhận có **2 documents riêng biệt** tương ứng với 2 UUID khác nhau của 2 thiết bị.
   - Mỗi document chứa đúng `fcmToken` của thiết bị đó.
3. Gửi thông báo đến token của Thiết bị A: Chỉ Thiết bị A nhận được thông báo.
