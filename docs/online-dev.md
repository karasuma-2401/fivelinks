# Chạy thử chơi online khi phát triển

> Áp dụng từ Giai đoạn 2 của kế hoạch online. Server chạy trên laptop, app kết nối tới `ws://localhost:8080/ws` (`ServerConfig.DEV_URL`).

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

Chế độ offline (`GameViewModel`, đấu AI / 2 người một máy) **không đổi**; cả hai cùng dùng logic chọn bài trong `CardSelection.kt` và cùng triển khai `GameController`.

## 4. Kiểm thử

- `./gradlew :app:shared:jvmTest`: test logic online với kết nối giả, và test **đầu-cuối** (`OnlineEndToEndTest`) chạy server thật trong test: 2 người chơi tạo/vào phòng, đi nước, một người bị cắt mạng rồi tự nối lại, rời phòng.
- `./gradlew :server:test`: luật phòng chơi phía server.

## 5. Sẽ làm ở các giai đoạn sau

- **Giai đoạn 3:** màn hình online (nhập tên, tạo phòng / nhập mã, phòng chờ, đồng hồ lượt, trạng thái đối thủ, chơi lại).
- **Giai đoạn 4:** giữ phiên khi app chạy nền / bị Android đóng.
- **Giai đoạn 5:** bản release trỏ tới server trên Fly.io (`wss://…`).
