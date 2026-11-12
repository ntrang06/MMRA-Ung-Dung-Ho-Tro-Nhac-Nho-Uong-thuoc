# MMRA — Quy ước lịch uống và từng liều, phiên bản 1

## Công nghệ

- Python/FastAPI trong backend/: xử lý API và nghiệp vụ.

- Kotlin trong app/: gọi API, hiển thị và nhắc thuốc trên điện thoại.

- Firebase Authentication quản lý tài khoản.

- Firestore lưu thuốc, lịch uống và từng liều.

- PostgreSQL hiện dùng cho OTP.

## Cấu hình chung — Trang phụ trách

- Firebase Console, IAM, Rules, indexes và cấu hình server.

- Service account, biến môi trường và URL backend dùng chung.

- Trúc/Huy không cần quyền quản trị Firebase để gọi API từ app.

- Không đưa private key vào Git hoặc app Android.

- Thay đổi file dùng chung cần trao đổi với Trang.

## Xác thực API

- Android lấy header bằng BackendSession.authorizationHeader().

- Header: Authorization: Bearer <Firebase ID token>.

- Python lấy người dùng bằng AuthenticatedUser.

- UID chỉ lấy từ token đã xác minh.

- Không nhận uid/ownerUid từ request nghiệp vụ.

- Repository dùng user_collection hoặc user_document.

- Firebase Admin bỏ qua Rules, nên backend phải kiểm tra quyền.

- medicineId phải thuộc medicines của chính UID đang đăng nhập.

- Tài nguyên không tồn tại trong nhánh UID hiện tại trả 404.

## Đường dẫn dữ liệu

- users/{uid}/medicines/{medicineId}

- users/{uid}/schedules/{scheduleId}

- users/{uid}/doses/{doseId}

- doses là nguồn dữ liệu lịch sử; không sao chép sang collection history.

- Các collection schedules/doses mới được truy cập qua Python API.

- Rules hiện tại mặc định chặn Android truy cập trực tiếp các collection mới.

## ID và phiên bản

- scheduleId: server sinh bằng uuid4().hex, giữ nguyên khi sửa.

- version: bắt đầu 1, tăng sau mỗi lần sửa.

- Cập nhật gửi expectedVersion; lệch phiên bản trả 409.

- doseId dùng make_dose_id(scheduleId, version, localDate, minuteOfDay).

- Cùng lịch, phiên bản, ngày và giờ phải có cùng doseId.

- Android dùng doseId do API trả về.

## Thời gian và liều lượng

- timeZone: Asia/Ho_Chi_Minh.

- Ngày: yyyy-MM-dd, tính cả ngày bắt đầu và kết thúc.

- endDate = null nghĩa là chưa có ngày kết thúc.

- times: phút từ 00:00; 08:00 = 480, 20:00 = 1200.

- weekdays: ISO 1 = thứ Hai, ..., 7 = Chủ nhật.

- Calendar của Android dùng 1 = Chủ nhật.

- Đổi Calendar sang ISO: ((calendarDay + 5) % 7) + 1.

- Firestore lưu Timestamp; API trả ISO 8601 có múi giờ.

- createdAt/updatedAt/actionAt do server đặt.

- doseQuantity là lượng uống mỗi lần, dạng chuỗi số như "1", "0.5".

- strength/strengthUnit là hàm lượng thuốc, tách khỏi lượng uống.

- doseUnit: TABLET, SACHET, ML, DROP, DOSE.

## Trạng thái và nhắc lại

- PENDING: chưa xác nhận.

- TAKEN: đã uống.

- SKIPPED: chủ động bỏ qua.

- MISSED: chưa xác nhận khi ngày địa phương đã kết thúc.

- CANCELLED: liều tương lai bị hủy vì thay đổi lịch.

- NEXT/UPCOMING là trạng thái hiển thị, không lưu làm status.

- Nhắc lại cập nhật nextReminderAt, giữ nguyên scheduledAt và doseId.

- Thời gian nhắc lại: 5, 10, 15 hoặc 30 phút.

## Ghi nhận và lịch sử

- Xác nhận uống/bỏ qua phải dùng Firestore transaction.

- Gửi lại cùng hành động trả kết quả cũ, giữ nguyên actionAt.

- Đổi hành động của liều đã kết thúc trả 409.

- Liều MISSED/CANCELLED không nhận hành động mới.

- medicineSnapshot giữ thông tin thuốc tại lúc sinh liều.

- Sửa/xóa lịch phải giữ lịch sử và hủy các liều tương lai bị thay thế.

- Xóa thuốc cần xử lý lịch liên quan trước khi xóa document thuốc.

- Luồng xóa thuốc BE1 cần bổ sung bước phối hợp này khi nối lịch uống.

## Mạng và thông báo

- Phiên bản đầu cần mạng để lưu lịch và xác nhận hành động.

- Timeout phải tải lại trạng thái trước khi báo kết quả.

- Cache lịch trên máy phải tách theo UID.

- Thông báo mang UID và doseId; kiểm tra tài khoản khi mở.

- Đăng xuất/đổi tài khoản phải hủy nhắc của tài khoản cũ.

## Phạm vi đã có

- Xác thực API đã được kiểm tra từ Android.

- Đã định nghĩa schema Python, DTO Kotlin và hàm tạo doseId.

- Đã có helper đường dẫn Firestore theo UID.

- API nghiệp vụ, transaction và giao diện sẽ triển khai theo phân công tiếp theo.
