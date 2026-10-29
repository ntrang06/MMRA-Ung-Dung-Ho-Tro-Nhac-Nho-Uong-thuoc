# MMRA — Quy ước Firebase cho mốc BE đầu tiên

## 1. Cấu hình chung

- Firebase project ID: mmra-dev

- Android applicationId: com.example.project_lttbd

- Firestore database: (default)

- Dùng cấu hình Firebase và phiên bản thư viện đang có trên main.

- Giữ một module app, một MainActivity và MMRANavHost chung

- Không tự đổi tên collection hoặc trường dữ liệu.

- Khi cần sửa phần của người khác hoặc file dùng chung, báo nhóm trước.

## 2. Quy ước dữ liệu

- uid lấy từ Firebase Authentication, không dùng email làm document ID

- Tên trường viết camelCase

- createdAt và updatedAt dùng Firestore Timestamp từ server

- createdAt chỉ đặt khi tạo mới; updatedAt cập nhật mỗi lần sửa.

- Không lưu mật khẩu trong Firestore, SharedPreferences hoặc log.

- Không đưa đường dẫn ảnh cục bộ content://, file:// lên Firestore.

- Các trường ảnh chưa có URL dùng null.

- Rules hiện tại kiểm tra quyền sở hữu; sẽ bổ sung kiểm tra từng trường

## 3. Hồ sơ — Trúc

Đường dẫn: users/{uid}

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| displayName | string | Tên hiển thị |
| email | string | Email tài khoản từ Firebase Auth |
| usageMode | string hoặc null | patient / caregiver; null khi chưa chọn |
| avatarUrl | string hoặc null | URL ảnh đã tải lên; chưa có thì null |
| createdAt | timestamp | Thời điểm tạo hồ sơ |
| updatedAt | timestamp | Thời điểm cập nhật |

usageMode dùng chọn giao diện, không cấp quyền đọc dữ liệu người khác.

Yêu cầu:

- Đăng ký, đăng nhập bằng email/mật khẩu thật

- Đăng xuất, gửi email đặt lại mật khẩu

- Khôi phục phiên đăng nhập khi mở lại app

- Tạo hồ sơ theo UID; đăng nhập lại không ghi đè hồ sơ đã có

- Nếu tạo Auth thành công nhưng lưu hồ sơ lỗi, cho thử lại lưu hồ sơ

- Có loading, thông báo lỗi và chặn bấm gửi lặp

## 4. Thuốc cá nhân — Trang

Đường dẫn: users/{uid}/medicines/{medicineId}

medicineId: ID tự sinh của Firestore.

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| medicineName | string | Tên thuốc |
| strength | string | Hàm lượng, ví dụ "500" |
| strengthUnit | string | Đơn vị hàm lượng, ví dụ "mg" |
| dosageForm | string | Dạng thuốc, ví dụ "Viên nén" |
| note | string | Ghi chú; không có dùng "" |
| imageUrl | string hoặc null | URL ảnh đã tải lên |
| createdAt | timestamp | Thời điểm tạo |
| updatedAt | timestamp | Thời điểm cập nhật |

Yêu cầu:
- Thêm, xem danh sách, sửa, xóa thuốc của tài khoản hiện tại

- Không hiển thị dữ liệu tài khoản cũ khi đổi tài khoản

- Có loading, danh sách trống và xử lý lỗi

- Hàm lượng thuốc và liều dùng mỗi lần là hai thông tin riêng

- Lịch uống sẽ bổ sung ở mốc tiếp theo

## 5. Thiết bị nhận thông báo — Huy

Đường dẫn: users/{uid}/devices/{deviceId}

deviceId: UUID tạo một lần cho mỗi bản cài đặt, lưu tại máy

Một tài khoản có thể có nhiều document thiết bị

| Trường | Kiểu | Ý nghĩa |
|---|---|---|
| fcmToken | string | Token FCM của bản cài đặt |
| platform | string | "android" |
| notificationsEnabled | boolean | Trạng thái cho phép thông báo |
| updatedAt | timestamp | Lần cập nhật gần nhất |

Yêu cầu:

- Lưu token dưới UID hiện đang đăng nhập

- Cập nhật khi token thay đổi và khi đăng nhập

- Phối hợp Trúc gỡ liên kết thiết bị khỏi tài khoản cũ khi đăng xuất

- Nhận thông báo khi app đang mở và chạy nền

- Bấm thông báo mở đúng màn hình đã thống nhất với Trang

- Kiểm thử bằng thông báo từ Firebase Console

- Không đặt thông tin thuốc hoặc sức khỏe nhạy cảm trong thông báo thử

- Chưa làm cảnh báo tự động cho người thân ở mốc này

## 6. Kiểm thử và bàn giao

Mỗi người bàn giao:

- Nhánh và commit chứa thay đổi

- Các file đã sửa, cấu hình cần thiết và cách chạy

- Các bước thử cùng kết quả, lỗi còn tồn tại

- Dịch vụ có thể phát sinh chi phí

Kiểm thử tích hợp:

- Hai tài khoản A/B không đọc hoặc sửa dữ liệu riêng của nhau

- Cùng tài khoản trên máy ảo và điện thoại có dữ liệu thuốc đồng bộ

- Hai thiết bị có hai document thiết bị riêng

- Đổi tài khoản không còn hiển thị dữ liệu tài khoản trước

Chưa nằm trong mốc này:

- Google/Phone login, AI/OCR thật, chia sẻ gia đình

- Lịch uống, lịch sử uống và cảnh báo tự động

- Trước khi triển khai các phần này cần bổ sung schema và Rules