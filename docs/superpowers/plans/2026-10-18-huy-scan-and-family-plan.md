# Kế Hoạch Triển Khai Chức Năng Của Huy: Quét Thuốc & Quản Lý Gia Đình

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng hoàn chỉnh giao diện, dữ liệu mẫu và luồng tương tác người dùng cho hai phân hệ của Huy: Quét nhận diện vỏ hộp thuốc (Camera UI, preview, kết quả AI) và Quản lý kết nối gia đình / theo dõi sức khỏe người thân (Danh sách thành viên, mã QR kết nối, phân quyền, cảnh báo trễ liều và gửi nhắc nhở).

**Architecture:** Sử dụng kiến trúc Jetpack Compose hiện đại với State Hoisting; tách biệt lớp dữ liệu mẫu (`data/model/`, `data/mock/`) và lớp giao diện màn hình (`ui/screens/scan/`, `ui/screens/family/`); tái sử dụng triệt để hệ thống Theme và bộ Shared Components đã có (`MMRAButton`, `MMRATextField`, `MMRABottomBar`); tích hợp mượt mà vào `MMRANavHost`.

**Tech Stack:** Kotlin 2.2.10, Jetpack Compose, Material 3, Navigation Compose, Android ActivityResultContracts (Camera & Gallery picker), JUnit 4.

**Spec:** [AGENTS.md](file:///G:/hacker/MMRA-Ung-Dung-Ho-Tro-Nhac-Nho-Uong-thuoc/AGENTS.md) và thiết kế Figma `https://www.figma.com/design/s3aKA81AO2e8mqd8eck0zG/Untitled?node-id=0-1`.

---

## Global Constraints

- Phạm vi: Chỉ tập trung **Mobile Frontend (UI/UX, Điều hướng, Tương tác)** bằng dữ liệu mẫu (**Mock Data**). Không gọi backend API thật.
- Giữ nguyên cấu trúc package hiện tại trên nhánh `main`: `com.example.project_lttbd`.
- Tận dụng hệ màu `Color.kt` và font `Inter` trong `Type.kt` đã cấu hình.
- Tuân thủ quy ước cấu trúc thư mục: `ui/screens/scan/`, `ui/screens/family/`, `data/model/`, `data/mock/`.

## Review Focus

1. **Camera Permission Denial**: Người dùng từ chối cấp quyền camera phải hiển thị giao diện hướng dẫn mở cài đặt rõ ràng, không crash.
2. **Scan Result Editing**: Người dùng có thể kiểm tra, sửa trực tiếp các trường thông tin thuốc đọc từ AI trước khi bấm "Đúng, tiếp tục".
3. **Mock Data Integrity**: Dữ liệu thuốc mẫu và thành viên gia đình phải đầy đủ các trường khớp với UI Figma (tên, hàm lượng, hoạt chất, NSX, độ khớp %, v.v.).
4. **Navigation Backstack**: Khi bấm quay lại từ màn hình con (như chi tiết lời mời, chi tiết cảnh báo, kết quả quét) phải quay về đúng tab tương ứng không làm mất trạng thái BottomBar.
5. **No Visual Clipping / Overflow**: Tất cả các màn hình danh sách và form phải hỗ trợ cuộn (`verticalScroll` / `LazyColumn`) để không bị tràn trên màn hình nhỏ.

---

## Chi Tiết Các Tác Vụ (Tasks)

### Task 1: Xây Dựng Models & Dữ Liệu Mẫu (Mock Data) Trong `data/mock/`

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/data/mock/MockScanMedicine.kt` (chứa `ScanResult` & dữ liệu mẫu)
- Create: `app/src/main/java/com/example/project_lttbd/data/mock/MockFamilyData.kt` (chứa `FamilyMember`, `DoseScheduleItem` & dữ liệu mẫu)
- Create: `app/src/main/java/com/example/project_lttbd/data/mock/MockAlertData.kt` (chứa `CaregiverAlert` & dữ liệu mẫu)
- Test: `app/src/test/java/com/example/project_lttbd/MockDataUnitTest.kt`

**Interfaces:**
- `ScanResult(id, medicineName, dosage, dosageForm, matchScore, activeIngredient, manufacturer, packaging, verifiedSource)`
- `FamilyMember(id, fullName, relationship, role, avatarUrl, phone, todayTakenCount, todayTotalCount, hasLateDose, doseSchedules)`
- `CaregiverAlert(id, memberId, memberName, medicineName, scheduledTime, minutesLate, severity, isRead)`

- [ ] **Step 1: Viết Unit Test kiểm tra tính toàn vẹn của Mock Data**
  Tạo test trong `MockDataUnitTest.kt` xác nhận danh sách mock thuốc, gia đình, cảnh báo không rỗng và các trường dữ liệu bắt buộc không null.

- [ ] **Step 2: Chạy test để xác nhận test fail do chưa có class**
  Chạy: `./gradlew test --tests com.example.project_lttbd.MockDataUnitTest`
  Kỳ vọng: FAIL vì chưa tạo file model.

- [ ] **Step 3: Triển khai các Data Class và Mock Collections**
  Tạo `ScanResult.kt`, `FamilyMember.kt`, `CaregiverAlert.kt` và các danh sách dữ liệu mẫu trong `data/mock/`.

- [ ] **Step 4: Chạy lại test để xác nhận pass**
  Chạy: `./gradlew test --tests com.example.project_lttbd.MockDataUnitTest`
  Kỳ vọng: PASS.

- [ ] **Step 5: Commit task 1**
  `git add app/src/main/java/com/example/project_lttbd/data/ app/src/test/`
  `git commit -m "feat(data): add models and mock data for scan and family modules"`

---

### Task 2: Giao Diện Quét Vỏ Hộp Thuốc (Camera Viewfinder, Preview & Kết Quả AI)

**Files:**
- Modify: `app/src/main/AndroidManifest.xml` (thêm khai báo `<uses-permission android:name="android.permission.CAMERA" />`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/scan/ScanCameraScreen.kt` (Giao diện Viewfinder theo Figma `2:4405`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/scan/ScanResultScreen.kt` (Giao diện kết quả AI theo Figma `2:4452`)
- Modify: `app/src/main/java/com/example/project_lttbd/ui/screens/scan/ScanScreen.kt` (Quản lý luồng chuyển đổi trạng thái: Camera -> Đang xử lý -> Kết quả)

**Interfaces:**
- `ScanCameraScreen(onCaptureClick: () -> Unit, onPickImageClick: () -> Unit, onManualInputClick: () -> Unit, onCloseClick: () -> Unit)`
- `ScanResultScreen(scanResult: ScanResult, onRescanClick: () -> Unit, onConfirmClick: (ScanResult) -> Unit, onBackClick: () -> Unit)`

- [ ] **Step 1: Thêm quyền Camera vào AndroidManifest.xml**
  Khai báo quyền camera và `camera.autofocus` (feature optional để không hạn chế thiết bị).

- [ ] **Step 2: Xây dựng UI `ScanCameraScreen`**
  Khung ngắm căn chỉnh bo góc, đường kẻ tâm, badges *"GIỮ MÁY ỔN ĐỊNH"* và *"Đủ sáng • Không bị lóa"*, nút chụp tròn màu xanh ngọc, nút nhập thủ công và chọn ảnh từ thư viện.

- [ ] **Step 3: Xây dựng UI `ScanResultScreen`**
  Banner cảnh báo y tế màu hổ phách, thẻ kết quả thuốc (Metformin 500mg, 92%, xác thực Cục Quản lý Dược), bảng thông tin hoạt chất / NSX / quy cách đóng gói cho phép chỉnh sửa, cặp nút *"Quét lại"* và *"Đúng, tiếp tục"*.

- [ ] **Step 4: Lắp ráp vào `ScanScreen` với State Machine mô phỏng AI scan**
  Khi bấm chụp hoặc chọn ảnh, hiển thị hiệu ứng Loading quét AI (1.5 giây), sau đó chuyển sang `ScanResultScreen` với dữ liệu mock.

- [ ] **Step 5: Kiểm tra Preview và biên dịch**
  Chạy: `./gradlew compileDebugKotlin` xác nhận không có lỗi cú pháp.

- [ ] **Step 6: Commit task 2**
  `git commit -m "feat(scan): implement medicine camera scan viewfinder and AI result screens"`

---

### Task 3: Giao Diện Quản Lý Gia Đình & Lời Mời Kết Nối (Family & Invites)

**Files:**
- Modify: `app/src/main/java/com/example/project_lttbd/ui/screens/family/FamilyScreen.kt` (Danh sách thành viên theo Figma `217:476`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/InviteFamilyMemberScreen.kt` (Mã QR & Link mời theo Figma `221:278`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/FamilyInvitationDetailScreen.kt` (Figma `225:184`, `225:236`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/FamilyPermissionsScreen.kt` (Figma `335:1720`)

**Interfaces:**
- `FamilyScreen(onMemberClick: (String) -> Unit, onAddMemberClick: () -> Unit, onAlertsClick: () -> Unit)`
- `InviteFamilyMemberScreen(onBackClick: () -> Unit, onShareLinkClick: (String) -> Unit)`
- `FamilyInvitationDetailScreen(inviteId: String, onAcceptClick: () -> Unit, onDeclineClick: () -> Unit, onBackClick: () -> Unit)`
- `FamilyPermissionsScreen(memberId: String, onSaveClick: () -> Unit, onBackClick: () -> Unit)`

- [ ] **Step 1: Xây dựng màn hình danh sách thành viên `FamilyScreen`**
  Hiển thị danh sách người thân đã kết nối từ `MockFamilyData`, thẻ tóm tắt tuân thủ thuốc hôm nay, badge trễ liều nổi bật nếu có, nút kết nối thêm người thân.

- [ ] **Step 2: Xây dựng màn hình `InviteFamilyMemberScreen`**
  Hiển thị khung mã QR kết nối, mã liên kết dạng text kèm nút sao chép, ô nhập mã để kết nối và nút chia sẻ.

- [ ] **Step 3: Xây dựng màn hình `FamilyInvitationDetailScreen` & `FamilyPermissionsScreen`**
  Hiển thị lời mời kết nối, chọn vai trò và các switch cấu hình phân quyền xem lịch uống, nhận cảnh báo, xem lịch sử.

- [ ] **Step 4: Kiểm tra biên dịch**
  Chạy: `./gradlew compileDebugKotlin` xác nhận code sạch không cảnh báo.

- [ ] **Step 5: Commit task 3**
  `git commit -m "feat(family): implement family members list, QR invite, and permission settings"`

---

### Task 4: Bảng Điều Khiển Theo Dõi Người Thân & Cảnh Báo Trễ Liều

**Files:**
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/CaregiverMemberDetailScreen.kt` (Figma `310:1086`, `2:4909`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/CaregiverAlertsScreen.kt` (Figma `316:2089`)
- Create: `app/src/main/java/com/example/project_lttbd/ui/screens/family/CaregiverAlertDetailScreen.kt` (Figma `316:2201`)

**Interfaces:**
- `CaregiverMemberDetailScreen(memberId: String, onSendReminderClick: () -> Unit, onViewHistoryClick: () -> Unit, onBackClick: () -> Unit)`
- `CaregiverAlertsScreen(onAlertClick: (String) -> Unit, onBackClick: () -> Unit)`
- `CaregiverAlertDetailScreen(alertId: String, onCallMemberClick: (String) -> Unit, onSendUrgentReminderClick: () -> Unit, onBackClick: () -> Unit)`

- [ ] **Step 1: Xây dựng `CaregiverMemberDetailScreen`**
  Thanh tiến độ thuốc hôm nay của người thân, danh sách các liều thuốc sáng/trưa/tối, trạng thái đã uống/trễ, nút thao tác nhanh *"Gửi nhắc nhở"* với Dialog/Toast xác nhận.

- [ ] **Step 2: Xây dựng `CaregiverAlertsScreen` & `CaregiverAlertDetailScreen`**
  Danh sách thông báo trễ liều từ `MockAlertData`, giao diện chi tiết lý do trễ liều, nút gọi điện thoại nhanh (`Intent.ACTION_DIAL`) và nút gửi nhắc nhở khẩn cấp.

- [ ] **Step 3: Kiểm tra biên dịch**
  Chạy: `./gradlew compileDebugKotlin`.

- [ ] **Step 4: Commit task 4**
  `git commit -m "feat(caregiver): add member detail dashboard and delay alerts handling"`

---

### Task 5: Hoàn Thiện Điều Hướng & Đăng Ký Route Trong `MMRANavHost`

**Files:**
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/MMRADestinations.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/ui/navigation/MMRANavHost.kt`
- Modify: `app/src/main/java/com/example/project_lttbd/MainActivity.kt` (nếu cần xử lý tham số ẩn/hiện BottomBar cho các màn hình con)

- [ ] **Step 1: Mở rộng `MMRADestinations.kt`**
  Thêm các route: `SCAN_RESULT`, `FAMILY_INVITE`, `FAMILY_INVITE_DETAIL`, `FAMILY_MEMBER_DETAIL/{memberId}`, `FAMILY_PERMISSIONS/{memberId}`, `FAMILY_ALERTS`, `FAMILY_ALERT_DETAIL/{alertId}`.

- [ ] **Step 2: Cấu hình `MMRANavHost.kt`**
  Kết nối các composable screens vừa tạo với `navController`, truyền callback điều hướng giữa các màn hình.

- [ ] **Step 3: Đảm bảo ẩn BottomBar trên các màn hình con**
  Cập nhật logic `showBottomBar` trong `MainActivity.kt` chỉ hiển thị ở 6 tab gốc, ẩn đi khi vào các màn hình con như camera toàn màn hình hoặc chi tiết cảnh báo.

- [ ] **Step 4: Chạy test và build toàn bộ project**
  Chạy: `./gradlew assembleDebug`
  Kỳ vọng: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit task 5**
  `git commit -m "feat(navigation): integrate scan and family sub-screens into MMRANavHost"`
