<div align="center">💊 MMRA
  
Medication Management and Reminder Assistant

ỨNG DỤNG HỖ TRỢ NHẮC NHỞ UỐNG THUỐC

Quản lý thuốc dễ dàng · Theo dõi lịch uống thuốc · Đồng hành cùng gia đình


Giới thiệu · Tính năng · Công nghệ · Cài đặt · Nhóm phát triển

</div>📖 Giới thiệu

MMRA (Medication Management and Reminder Assistant) là ứng dụng Android hỗ trợ người dùng tổ chức thông tin thuốc, theo dõi lịch uống và quản lý việc chăm sóc người thân. Dự án hướng đến 

người lớn tuổi, người sử dụng nhiều loại thuốc, người dễ quên lịch uống thuốc và người chăm sóc trong gia đình.

Mục tiêu: Đem đến trải nghiệm quản lý thuốc trực quan, dễ thao tác, giúp người dùng chủ động hơn trong sinh hoạt hằng ngày.

Đồ án môn học: Lập trình thiết bị di động

Nhóm: J

Giảng viên hướng dẫn: Trương Quang Tuấn

✨ Tính năng

Phân hệ	Nội dung

🏠 Trang chủ	Tổng quan lịch uống, liều tiếp theo và tiến độ trong ngày

💊 Thuốc	Danh sách thuốc, giao diện thêm và quản lý thông tin thuốc

📅 Lịch uống	Xem lịch, thiết lập thông tin lịch uống thuốc

📷 Quét thuốc	Luồng camera, xem trước ảnh và kết quả quét

🕘 Lịch sử	Giao diện theo dõi lịch sử uống thuốc

👨‍👩‍👧 Gia đình	Các màn hình thành viên, lời mời và quyền chăm sóc

👤 Tài khoản	Đăng nhập, đăng ký, thông tin cá nhân, ảnh đại diện và cài đặt
🔔 Nhắc nhở	Các thành phần hiển thị nhắc uống thuốc và hỗ trợ thông báo


Lưu ý tiến độ: Dự án đang phát triển. Một số màn hình và luồng đã được xây dựng nhưng vẫn có thể dùng dữ liệu mẫu (data/mock), hoặc chưa hoàn tất kết nối dữ liệu và kiểm thử toàn bộ quy 

trình. Không nên hiểu bảng trên là tuyên bố tất cả chức năng đã hoàn thiện ở mức sản phẩm.

🛠 Công nghệ

Thành phần	Công nghệ đang có trong mã nguồn

Ngôn ngữ	Kotlin

IDE	Android Studio

Giao diện	Jetpack Compose, Material 3

Điều hướng	Navigation Compose

Xác thực	Firebase Authentication

Lưu trữ đám mây	Cloud Firestore

Thông báo đẩy	Firebase Cloud Messaging (FCM)

Phân tích	Firebase Analytics

Camera	CameraX

Build	Gradle Kotlin DSL

Quản lý mã nguồn	Git & GitHub

Hướng mở rộng: Tăng cường nhận diện thuốc bằng hình ảnh/OCR, hoàn thiện cơ chế nhắc lịch và đồng bộ dữ liệu. Các hướng này không được coi là tính năng đã hoàn tất.

🧩 Sơ đồ hệ thống

<div align="center">
  
  <img src="docs/images/mmra-system-flow.png" alt="Sơ đồ luồng hoạt động hệ thống MMRA" width="850" />
  
  <p><i>Sơ đồ luồng hoạt động hệ thống MMRA (tài liệu của nhóm)</i></p>
  
</div>🗂 Cấu trúc dự án

MMRA-Ung-Dung-Ho-Tro-Nhac-Nho-Uong-thuoc/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/project_lttbd/
│   │   │   ├── data/
│   │   │   │   ├── device/
│   │   │   │   ├── mock/
│   │   │   │   ├── model/
│   │   │   │   ├── remote/
│   │   │   │   └── repository/
│   │   │   ├── notifications/
│   │   │   ├── ui/
│   │   │   │   ├── components/
│   │   │   │   ├── navigation/
│   │   │   │   ├── screens/
│   │   │   │   │   ├── account/  auth/  family/
│   │   │   │   │   ├── history/  home/  medicine/
│   │   │   │   │   ├── reminder/ scan/ schedule/ setup/
│   │   │   │   ├── theme/
│   │   │   │   └── viewmodel/
│   │   │   └── MainActivity.kt
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── docs/
│   ├── images/
│   ├── fcm-testing-guide.md
│   └── firebase-contract.md
├── gradle/
├── gradlew
├── gradlew.bat
└── README.md

🚀 Cài đặt và chạy ứng dụng

1. Yêu cầu
   
- Android Studio có hỗ trợ Kotlin và Jetpack Compose.
- 
- JDK 11 tương thích với cấu hình dự án (có thể sử dụng JDK đi kèm Android Studio nếu phù hợp).

- Android SDK 37 để biên dịch theo cấu hình hiện tại.
- 
- Thiết bị Android hoặc máy ảo có Android 7.0 (API 24) trở lên.
- 
- Kết nối Internet để tải dependency và sử dụng các dịch vụ Firebase.
- 
2. Tải mã nguồn
  
git clone -b readme https://github.com/ntrang06/MMRA-Ung-Dung-Ho-Tro-Nhac-Nho-Uong-thuoc.git

cd MMRA-Ung-Dung-Ho-Tro-Nhac-Nho-Uong-thuoc

Lệnh trên lấy nhánh readme đang được sử dụng để biên soạn tài liệu. Có thể thay readme bằng nhánh phù hợp khi triển khai chính thức.

3. Cấu hình Firebase
Ứng dụng sử dụng Firebase Authentication, Firestore, Cloud Messaging và Analytics. Để chạy với một dự án Firebase riêng:
1. Tạo dự án trong Firebase Console.
2. Đăng ký ứng dụng Android với application ID com.example.project_lttbd (hoặc sửa đồng bộ application ID và cấu hình Firebase).
3. Tải google-services.json của dự án Firebase được cấp quyền và đặt vào thư mục app/.
4. Bật và cấu hình các dịch vụ Firebase mà bản demo sử dụng; áp dụng quy tắc bảo mật Firestore phù hợp.
Bảo mật: Không chia sẻ khóa quản trị Firebase, tài khoản dịch vụ, token hoặc thông tin đăng nhập riêng tư. File cấu hình Firebase không thay thế việc thiết lập các quy tắc truy cập dữ liệu đúng cách.
4. Mở và chạy
1. Trong Android Studio chọn Open, trỏ đến thư mục repository.
2. Đợi Gradle Sync hoàn tất.
3. Chọn thiết bị Android hoặc emulator.
4. Nhấn Run ▶ để chạy ứng dụng.
Trên Windows, có thể kiểm tra build bằng:
.\gradlew.bat assembleDebug
Trên macOS/Linux:
./gradlew assembleDebug
APK debug (nếu build thành công) thường nằm tại app/build/outputs/apk/debug/.
📚 Tài liệu kỹ thuật
- [Sơ đồ luồng hệ thống](docs/images/mmra-system-flow.png)
- [Hướng dẫn kiểm thử FCM](docs/fcm-testing-guide.md)
- [Quy ước dữ liệu Firebase](docs/firebase-contract.md)
👥 Nhóm phát triển
Nhóm J — Môn Lập trình thiết bị di động
Thành viên	Vai trò
Bùi Ngọc Trang	Thành viên nhóm
Thái Thanh Trúc	Thành viên nhóm
Đặng Phạm Gia Huy	Thành viên nhóm


Giảng viên hướng dẫn: Trương Quang Tuấn
🧭 Định hướng phát triển
- [ ] Hoàn thiện liên kết dữ liệu thật cho tất cả màn hình cần thiết.
- [ ] Kiểm thử đầy đủ quy trình quản lý và nhắc lịch uống thuốc.
- [ ] Hoàn thiện nhận diện thuốc từ hình ảnh và đánh giá độ chính xác.
- [ ] Kiểm thử ứng dụng trên nhiều thiết bị và kích thước màn hình.
- [ ] Chuẩn bị bản build phát hành và tài liệu hướng dẫn sử dụng.
<div align="center">💙 MMRA — Đồng hành cùng bạn trong hành trình chăm sóc sức khỏe

<sub>Dự án học tập · Nhóm J · Lập trình thiết bị di động</sub>
</div>
