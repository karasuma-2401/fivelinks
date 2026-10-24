# Quy trình Phát hành và Danh mục Kiểm tra Chất lượng (Release Checklist)

Tài liệu này định nghĩa tiêu chuẩn phát hành sản phẩm, các cổng kiểm soát chất lượng (Quality Gates), quy trình tạo bản dựng đa nền tảng và quyết định Go/No-Go cho FiveLink.

---

## 1. Chiến lược Đánh số Phiên bản (Versioning Strategy)

- **Semantic Versioning:** `MAJOR.MINOR.PATCH`
  - `MAJOR`: Thay đổi phá vỡ giao thức (`ProtocolVersion.MAJOR`), cấu trúc bàn cờ hoặc luật chơi cốt lõi không tương thích ngược.
  - `MINOR`: Tính năng mới tương thích ngược (chế độ chơi mới, bổ sung màn hình, cải tiến UI/UX).
  - `PATCH`: Sửa lỗi, tối ưu hiệu năng, tinh chỉnh AI heuristic.
- **Android Version Code:** Tăng tuần tự theo quy tắc `MAJOR * 10000 + MINOR * 100 + PATCH` (vd: `1.0.1` -> `10001`).

---

## 2. Cổng Kiểm soát Chất lượng Tiền Phát hành (Pre-Release Quality Gates)

Trước khi gắn thẻ release (`git tag vX.Y.Z`), mọi tiêu chí sau **bắt buộc** phải đạt:

- [ ] **Toàn bộ Test Suite xanh (100% Pass):**
  ```bash
  ./gradlew :core:jvmTest :app:shared:jvmTest :server:test --no-daemon --console=plain
  ```
- [ ] **Biên dịch thử toàn bộ các target đa nền tảng:**
  - [ ] Desktop JAR: `./gradlew :app:desktopApp:jar`
  - [ ] Android Debug APK: `./gradlew :app:androidApp:assembleDebug`
  - [ ] Web Browser Distribution: `./gradlew :app:webApp:jsBrowserDistribution`
  - [ ] Server Image: `docker build -f server/Dockerfile -t fivelinks-server:latest .`
- [ ] **Kiểm tra an toàn bí mật (Secret Isolation):**
  - [ ] Không có file `.jks`, `.keystore`, `keystore.properties`, hoặc token cá nhân trong git commit (`git status` và `git log -p -n 5` sạch).
  - [ ] Thông tin ký phát hành được cấp qua biến môi trường CI (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).

---

## 3. Danh mục Kiểm thử Khói Thủ công (Manual Smoke Test Checklist)

### 3.1. Thiết bị Android thật
- [ ] Mở ứng dụng lần đầu: Hướng dẫn nhanh (`OnboardingDialog`) hiển thị 4 bước rõ ràng, bấm hoàn thành sẽ lưu trạng thái.
- [ ] Chơi 1 ván ngoại tuyến với AI (độ khó Vừa/Khó): Không giật lag khung hình khi AI suy nghĩ.
- [ ] Thoát ra menu chính giữa trận: Thẻ *"TIẾP TỤC VÁN ĐẤU"* hiển thị đúng tóm tắt lượt và độ khó. Bấm vào tiếp tục ván đấu bình thường.
- [ ] Chuyển đổi Dark Mode / Light Mode: Thanh trạng thái (Status bar) và Navigation bar đổi màu mượt mà theo giao diện.
- [ ] Tắt/Bật tùy chọn Âm thanh và Rung: Thao tác đặt cờ tôn trọng cài đặt (không rung khi đã tắt).
- [ ] Chế độ Online 1v1: Tạo phòng, nhập mã phòng từ máy khác, đánh bài, ngắt WiFi 10s rồi bật lại -> phòng khôi phục tự động (`ConnectionState.Connected`).

### 3.2. Môi trường Trình duyệt Web (Chrome, Edge, Safari, Firefox)
- [ ] Khởi chạy web, màn hình tải nhanh, không bị lỗi font hoặc vỡ layout canvas.
- [ ] Thao tác rê chuột (hover) và click chọn bài trực quan, viền vàng kim hiển thị chuẩn xác.
- [ ] F5 (Reload) trang web: Tùy chọn cài đặt và ván cờ dở dang được nạp lại nguyên vẹn từ `localStorage`.
- [ ] Kết nối WebSocket `/ws` qua giao thức an toàn `wss://` không bị chặn bởi CORS/Origin policy.

### 3.3. Desktop (Windows / macOS / Linux)
- [ ] Cửa sổ co giãn tự do (Responsive): Khi kéo rộng màn hình ngang (>= 760dp), giao diện tự động chuyển sang chế độ 2 cột thích ứng (Bàn cờ bên trái, Bài trên tay & Thanh chế tạo bên phải).
- [ ] Hỗ trợ điều hướng bàn phím: Phím Tab di chuyển tiêu điểm (`focus`), phím Space/Enter để kích hoạt nút hoặc chọn quân cờ.

---

## 4. Chính sách Quyền riêng tư & Bảo mật Dữ liệu (Privacy & Data Governance)

- **Không thu thập dữ liệu nhạy cảm:** FiveLink không yêu cầu đăng ký tài khoản, email, số điện thoại hay quyền truy cập danh bạ/vị trí.
- **Danh tính người chơi:** Sử dụng định danh tạm thời (`Guest ID` dạng `p_xxxxxxxx`), lưu trữ cục bộ trên máy thiết bị và tự xóa khi người dùng xóa dữ liệu ứng dụng.
- **Dữ liệu trận đấu trực tuyến:** Chỉ lưu trên RAM máy chủ trong suốt thời gian diễn ra ván đấu. Sau khi ván kết thúc và hai người chơi rời phòng, toàn bộ trạng thái sẽ được giải phóng hoàn toàn khỏi bộ nhớ máy chủ.

---

## 5. Quy trình Đánh giá Go / No-Go

| Vai trò | Trách nhiệm kiểm tra | Tiêu chí Go |
|---|---|---|
| **Tech Lead / Core Dev** | Code review, test coverage, static analysis | Toàn bộ PR được merge, không có cảnh báo nghiêm trọng, test pass 100%. |
| **QA / Tester** | Smoke test trên thiết bị thật, kiểm tra mạng yếu | Không phát sinh Crash (P0), không kẹt luồng (Deadlock), khôi phục offline hoạt động tốt. |
| **Release Engineer** | Pipeline CI/CD, Signing, Checksum artifact | CI sinh artifact tự động, SHA256 checksum khớp, release notes đầy đủ. |

**Quyết định:** Nếu có bất kỳ lỗi P0/P1 nào chưa được giải quyết, kích hoạt lệnh **NO-GO** và hoãn phát hành để sửa chữa.

---

## 6. Mẫu Ghi chú Phát hành (Release Notes / Changelog)

```markdown
## FiveLink - Phiên bản [X.Y.Z] - [YYYY-MM-DD]

### Tính năng mới
- Hướng dẫn nhập môn tương tác 4 bước cho người chơi mới.
- Lưu trữ và tiếp tục ván đấu ngoại tuyến tự động trên mọi nền tảng (Android, Web, Desktop, iOS).
- Giao diện 2 cột thích ứng thông minh cho màn hình ngang, máy tính bảng và máy tính để bàn.
- Hỗ trợ toàn diện khả năng tiếp cận (Accessibility), nhãn mô tả tiếng Việt và điều hướng bàn phím.
- Máy chủ WebSocket sản xuất hỗ trợ giới hạn tần suất (Rate Limiting), kiểm tra sức khỏe (/health/readiness) và ngắt kết nối an toàn (Graceful Shutdown).

### Cải tiến & Tối ưu
- Cải thiện thuật toán tìm kiếm AI (Heuristic & Search Engine) không chặn luồng giao diện.
- Giảm thiểu dung lượng APK, tách riêng mô hình server và client.
- Tự động khôi phục phiên phòng khi mất kết nối mạng tạm thời.
```

