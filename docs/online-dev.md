# Chạy thử chơi online khi phát triển

> P0 đã hoàn thành luồng online trong app. Server chạy trên laptop, app kết nối tới `ws://localhost:8080/ws` (`ServerConfig.DEV_URL`).

## 1. Bật server

```bash
./gradlew :server:run
```

Server nghe ở cổng **8080**. Kiểm tra nhanh: mở `http://localhost:8080/health` → `Heath OK`.
Dừng server bằng `Ctrl+C` trong terminal đang chạy nó.

## 2. Kết nối từ từng nền tảng

| Nền tảng | Cách kết nối |
|---|---|
| Điện thoại Android (cắm USB) | Chạy `adb reverse tcp:8080 tcp:8080`. Khi đó `localhost:8080` trên điện thoại trỏ thẳng tới laptop, không cần cùng Wi-Fi hay mở tường lửa. **Rút cáp ra cắm lại thì phải chạy lại lệnh này.** Xem danh sách: `adb reverse --list` |
| Desktop (`:app:desktopApp`) | Cùng máy với server, kết nối thẳng `localhost` |
| Web (JS / Wasm) | Trình duyệt trên cùng máy, kết nối thẳng `localhost` |

Android chỉ cho phép kết nối không mã hóa tới `localhost` / `127.0.0.1` (file `app/androidApp/src/main/res/xml/network_security_config.xml`). Mọi địa chỉ khác vẫn bắt buộc mã hóa (`wss://`).

## 3. Các lớp phía app (`app/shared/.../online/`)

| Lớp | Vai trò |
|---|---|
| `OnlineClient` | WebSocket tới server: gửi `Hello`, ping 10 giây/lần, im lặng quá 25 giây coi như mất kết nối, **tự nối lại** (chờ 1, 2, 4, 8 rồi 10 giây/lần) kèm `sessionToken` để server trả về đúng phòng |
| `ConnectionState` | `Offline` → `Connecting` → `Connected` / `Reconnecting(lần thử, chờ bao lâu)` / `UpdateRequired` (server khác phiên bản chính) |
| `OnlineSession` | Theo dõi tin nhắn server thành `OnlineState`: kết nối, phòng, ván (`PlayerView` + bàn cờ), đồng hồ lượt, đối thủ rớt mạng, kết quả, lỗi (tiếng Việt). Thao tác: `createRoom`, `joinRoom`, `submitMove`, `leaveRoom`, `requestRematch` |
| `OnlineGameController` | Cho màn chơi hiện có (`GameScreen`) điều khiển ván online: chọn bài trên máy, nước đi gửi lên server |
| `OnlineLobbyScreen` | Nhập tên, kết nối, tạo phòng/chọn luật, vào phòng bằng mã, chờ đối thủ và báo trạng thái socket |

Chế độ offline (`GameViewModel`, đấu AI / 2 người một máy) **không đổi**; cả hai cùng dùng logic chọn bài trong `CardSelection.kt` và cùng triển khai `GameController`.

## 4. Kiểm thử

- `./gradlew :app:shared:jvmTest`: test logic online với kết nối giả, và test **đầu-cuối** (`OnlineEndToEndTest`) chạy server thật trong test: 2 người chơi tạo/vào phòng, đi nước, một người bị cắt mạng rồi tự nối lại, rời phòng.
- `./gradlew :server:test`: luật phòng chơi phía server.

## 5. Đã hoàn thành trong P0

- Menu có lựa chọn **Online** → nhập tên → kết nối → tạo hoặc vào phòng.
- Lobby hiển thị mã phòng, luật, người chơi, lỗi từ server và trạng thái reconnect.
- Khi server gửi `GameSnapshot`, app chuyển vào `GameScreen` qua `OnlineGameController`.
- Màn chơi online luôn hiển thị tay bài của người xem, khóa input ngoài lượt, hiển thị đồng hồ lượt/reconnect đối thủ và hỗ trợ rematch.
- Rời lobby hoặc trận online gửi `LeaveRoom` trước khi đóng socket, để đối thủ không phải chờ hết reconnect grace period.

## 6. Sẽ làm ở các giai đoạn sau

- **P1:** giữ phiên khi app chạy nền / bị Android đóng, đồng thời lưu tên và session token theo nền tảng.
- **P1:** bản release trỏ tới server staging/production qua `wss://…` thay vì `localhost`.
- **P4:** rate limit, giới hạn số phòng, observability và triển khai server production.
