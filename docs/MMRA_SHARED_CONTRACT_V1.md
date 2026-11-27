
# MMRA 1.0 – SHARED API & DATABASE CONTRACT

## 1. Mục đích

Tài liệu này thống nhất cấu trúc dữ liệu, API và phân quyền dùng chung
cho Android, Family, Web Admin và Backend.

Trạng thái: Bản thiết kế C1. Các API Family và Admin chưa được triển khai.

## 2. Công nghệ sử dụng

- Android: Kotlin, Jetpack Compose
- Backend: Python, FastAPI
- Authentication: Firebase Authentication
- Database chính: Cloud Firestore
- Database OTP: PostgreSQL
- Thông báo: Firebase Cloud Messaging
- Backend Hosting: Render
- PostgreSQL Hosting: Neon
- Web Admin dự kiến: React, TypeScript, Vite

## 3. Cấu trúc Database

### 3.1. Dữ liệu hiện tại – Giữ nguyên

users/{uid}/medicines/{medicineId}

users/{uid}/schedules/{scheduleId}

users/{uid}/doses/{doseId}

users/{uid}/medicineCreateRequests/{requestId}

Không thay đổi cấu trúc hiện tại nếu chưa có migration phù hợp.

### 3.2. Family Invitations – Mới

Collection: familyInvitations/{invitationId}

Các trường:

- ownerUid: string
- inviteCodeHash: string
- status: pending | accepted | revoked | expired
- createdAt: timestamp
- expiresAt: timestamp
- acceptedByUid: string | null

Yêu cầu:

- Mã mời được tạo bằng bộ sinh ngẫu nhiên an toàn.
- Chỉ lưu hash của mã mời.
- Mã có thời hạn, dự kiến 24 giờ.
- Một mã chỉ được chấp nhận một lần.
- Không cho phép tự mời chính mình.

### 3.3. Family Links – Mới

Collection: familyLinks/{linkId}

Các trường:

- ownerUid: string
- caregiverUid: string
- status: active | revoked
- permissions:
  viewSchedule: boolean
  viewDoseStatus: boolean
  receiveMissedDoseAlerts: boolean
- createdAt: timestamp
- updatedAt: timestamp

Yêu cầu:

- Người uống thuốc là chủ sở hữu dữ liệu.
- Người chăm sóc chỉ xem dữ liệu được cấp quyền.
- Không cho phép người chăm sóc sửa thuốc hoặc liều của chủ sở hữu
  trong phiên bản 1.0.
- Khi thu hồi quyền, API phải từ chối truy cập từ yêu cầu tiếp theo.
- Chống tạo quan hệ Family trùng bằng transaction.

### 3.4. Medicine Catalog – Mới

Collection: medicineCatalog/{catalogId}

Các trường:

- name: string
- strength: string
- dosageForm: string
- source: string
- active: boolean
- createdAt: timestamp
- updatedAt: timestamp
- updatedByUid: string

Đây là danh mục thuốc dùng chung do Admin quản lý.

Không được nhầm danh mục này với thuốc riêng của người dùng trong
users/{uid}/medicines.

### 3.5. Admin Audit Logs – Mới

Collection: adminAuditLogs/{logId}

Các trường:

- actorUid: string
- action: string
- resourceType: string
- resourceId: string
- createdAt: timestamp

Ghi nhận thao tác quản trị nhưng không lưu mật khẩu, token
hoặc dữ liệu sức khỏe nhạy cảm trong log.

## 4. Xác thực và phân quyền

Tất cả API cần đăng nhập sử dụng:

Authorization: Bearer &lt;Firebase ID Token&gt;

Backend xác minh Firebase ID Token trước khi xử lý.

### Người uống thuốc

- Quản lý thuốc cá nhân.
- Quản lý lịch uống và liều.
- Tạo lời mời Family.
- Cấp và thu hồi quyền chia sẻ.

### Người chăm sóc

- Chấp nhận lời mời Family.
- Xem lịch uống nếu được cấp quyền.
- Xem trạng thái liều nếu được cấp quyền.
- Nhận cảnh báo nếu được cấp quyền.
- Không được tự sửa dữ liệu thuốc của người khác.

### Admin

- Đăng nhập bằng Firebase Authentication.
- Phải được Backend xác minh custom claim admin = true
  hoặc danh sách cho phép chỉ do server kiểm soát.
- Quản lý danh mục thuốc dùng chung.
- Xem thống kê hệ thống phù hợp quyền hạn.
- Xem nhật ký quản trị.
- Không được tự do truy cập dữ liệu thuốc cá nhân.

Không cho phép người dùng tự đặt quyền Admin trong Firestore.

## 5. Family API

Base URL: /api/v1

### F01. Tạo lời mời

POST /family/invitations

Response:

{
"invitationId": "invitation_001",
"code": "ONE_TIME_INVITE_CODE",
"expiresAt": "2026-11-28T08:00:00Z"
}

Chỉ trả mã gốc khi tạo lời mời, không lưu mã gốc vào database.

### F02. Chấp nhận lời mời

POST /family/invitations/accept

Request:

{
"code": "ONE_TIME_INVITE_CODE"
}

Response:

{
"linkId": "link_001",
"status": "active"
}

Phải kiểm tra mã hết hạn, mã đã dùng, tự mời và liên kết trùng.

### F03. Danh sách thành viên

GET /family/members

Response:

{
"items": [
{
"linkId": "link_001",
"role": "owner",
"displayName": "Người thân",
"permissions": {
"viewSchedule": true,
"viewDoseStatus": true,
"receiveMissedDoseAlerts": true
}
}
]
}

### F04. Ngắt liên kết

DELETE /family/members/{linkId}

Thu hồi liên kết Family.

Sau khi thu hồi, các API chia sẻ dữ liệu phải từ chối truy cập.

### F05. Thay đổi quyền chia sẻ

PUT /family/members/{linkId}/permissions

Request:

{
"viewSchedule": true,
"viewDoseStatus": true,
"receiveMissedDoseAlerts": false
}

Chỉ chủ sở hữu dữ liệu được thay đổi quyền.

### F06. Xem liều của người thân

GET /family/members/{linkId}/doses?date=2026-11-27

Response:

{
"items": [
{
"doseId": "dose_001",
"scheduledAt": "2026-11-27T08:00:00Z",
"status": "pending"
}
]
}

Backend phải kiểm tra quan hệ Family và quyền xem liều.

Không cho phép truyền UID bất kỳ để xem dữ liệu người khác.

### F07. Cảnh báo Family

GET /family/alerts

Trả về các cảnh báo mà tài khoản hiện tại được phép xem.

Không được báo một liều là bỏ lỡ nếu dữ liệu chưa đồng bộ
hoặc trạng thái chưa được xác nhận.

## 6. Web Admin API

Base URL: /api/v1/admin

### W01. Tài khoản Admin

GET /admin/me

Kiểm tra Firebase ID Token và quyền Admin.

### W02. Dashboard

GET /admin/summary

Thống kê danh mục thuốc và trạng thái hệ thống.

Không trả dữ liệu sức khỏe cá nhân của người dùng.

### W03. Danh sách thuốc

GET /admin/medicines?search=metformin

Tìm kiếm danh mục thuốc dùng chung.

### W04. Thêm thuốc

POST /admin/medicines

Thêm bản ghi danh mục thuốc.

### W05. Cập nhật thuốc

PUT /admin/medicines/{catalogId}

Cập nhật bản ghi danh mục thuốc.

### W06. Ẩn thuốc

DELETE /admin/medicines/{catalogId}

Chỉ chuyển active = false, không xóa cứng.

### W07. Nhật ký quản trị

GET /admin/audit-logs

Chỉ Admin được truy cập.

Tất cả API Admin phải kiểm tra quyền trên Backend,
không chỉ kiểm tra ở giao diện Web.

## 7. Quy ước response lỗi

Lỗi ứng dụng trả theo cấu trúc:

{
"detail": {
"code": "permission_denied",
"message": "Không có quyền truy cập."
}
}

Mã HTTP:

- 200: Thành công
- 201: Tạo thành công
- 400: Dữ liệu hoặc mã mời không hợp lệ
- 401: Chưa đăng nhập
- 403: Không có quyền
- 404: Không tìm thấy tài nguyên
- 409: Trùng hoặc xung đột dữ liệu
- 422: Dữ liệu không hợp lệ
- 503: Dịch vụ tạm thời không khả dụng

## 8. Yêu cầu bảo mật

- Chỉ Backend được phép ghi các collection Family/Admin dùng chung.
- Kiểm tra quyền mỗi lần truy cập dữ liệu người thân.
- Không tin tưởng UID chủ sở hữu do Android gửi lên.
- Mã mời phải hết hạn và chỉ sử dụng một lần.
- Dùng transaction khi chấp nhận lời mời và thay đổi liên kết.
- API dữ liệu sức khỏe trả Cache-Control: no-store.
- Không ghi token hoặc dữ liệu nhạy cảm vào log.
- Không đưa Firebase Service Account hoặc file .env lên GitHub.

## 9. Kiểm thử bắt buộc

- Không có token -> 401.
- Người dùng thường truy cập Admin -> 403.
- Người không thuộc Family không đọc được dữ liệu người khác.
- Mã mời hết hạn bị từ chối.
- Mã mời đã dùng không được dùng lại.
- Không được tự mời chính mình.
- Không tạo quan hệ Family trùng.
- Thu hồi liên kết thì không còn xem được dữ liệu.
- Tắt quyền xem liều thì API xem liều bị từ chối.
- Admin không được đọc tùy ý thuốc cá nhân.
- API thuốc, lịch, liều, OTP và BE4 hiện tại vẫn hoạt động.

## 10. Thứ tự triển khai

C1. Thống nhất Database và API Contract.

C2. Hoàn thiện xác thực và phân quyền dùng chung.

C3. Tạo Backend Family API.

C4. Tạo Backend Web Admin API.

C5. Kiểm thử Family/Admin trên Backend.

C6. Kết nối Family Android và Web Admin.

C7. Triển khai Backend HTTPS và kiểm thử tích hợp.

C8. Hoàn thiện Android Release và Google Play.

## 11. Quy định nhóm

- Không tự thay đổi API Contract khi chưa thống nhất.
- Không sửa các API hiện tại nếu không cần thiết.
- Mỗi thành viên làm trên nhánh riêng.
- Commit từng phần nhỏ, có kiểm thử.
- Không merge code lỗi vào main.
- Không sử dụng dữ liệu mock trong bản Release.
- BE4 đã hoàn thành, chỉ sửa nếu kiểm thử phát hiện lỗi.
