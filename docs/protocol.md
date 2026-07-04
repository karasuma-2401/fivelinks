# Tài liệu ghi chép: Thư mục `protocol`

> Dự án: **fivelinks-cmp**  
> Vị trí: `core/src/commonMain/kotlin/com/karasuma/fivelinks/fivelinks_cmp/protocol/`  
> Cập nhật: 04/07/2026

---

## 1. Tổng quan

Thư mục `protocol` định nghĩa **hợp đồng giao tiếp (communication contract)** giữa **client** và **server** qua mạng.

Vì nằm trong module **`core` → `commonMain`**, code ở đây được biên dịch cho **mọi nền tảng** (server, Android, iOS, Desktop...). Cả hai phía dùng **cùng một định nghĩa tin nhắn**, tránh lệch format JSON giữa client và server.

**Mục tiêu chính:**
- Một nguồn chân lý duy nhất cho giao thức mạng
- An toàn về kiểu (type-safe) với Kotlin + kotlinx.serialization
- Serialization JSON nhất quán
- Quản lý phiên bản giao thức và kiểm tra tương thích

---

## 2. Cấu trúc thư mục

| File | Vai trò |
|------|---------|
| `ClientMessage.kt` | Tin nhắn **client → server** |
| `ServerMessage.kt` | Tin nhắn **server → client** |
| `ErrorCodes.kt` | Mã lỗi chuẩn hóa |
| `ProtocolVersion.kt` | Quản lý phiên bản giao thức |
| `ProtocolJson.kt` | Cấu hình JSON encode/decode dùng chung |

---

## 3. Chi tiết từng file

### 3.1. `ClientMessage.kt` — Client gửi lên Server

- Kiểu: `sealed interface` → danh sách loại tin nhắn **cố định, biết trước**
- Mọi tin nhắn đều có: `messageId: String`

| Loại | `type` (JSON) | Mục đích | Các trường chính |
|------|---------------|----------|------------------|
| `Hello` | `"hello"` | Bắt tay khi kết nối | `protocolVersion`, `playerName` |
| `Ping` | `"ping"` | Kiểm tra độ trễ / heartbeat | `clientTime` |

**Ví dụ JSON (Hello):**
```json
{
  "type": "hello",
  "messageId": "abc-123",
  "protocolVersion": "1.0",
  "playerName": "Player1"
}
```

---

### 3.2. `ServerMessage.kt` — Server gửi về Client

- Kiểu: `sealed interface`
- Mọi tin nhắn đều có: `messageId: String`

| Loại | `type` (JSON) | Mục đích | Các trường chính |
|------|---------------|----------|------------------|
| `Welcome` | `"Welcome"` | Chấp nhận kết nối | `protocolVersion`, `playerName` |
| `Pong` | `"pong"` | Trả lời Ping | `serverTime` |
| `Rejected` | `"rejected"` | Từ chối yêu cầu | `reason` (ErrorCodes), `message` |

**Ví dụ JSON (Rejected):**
```json
{
  "type": "rejected",
  "messageId": "xyz-456",
  "reason": "PROTOCOL_VERSION_MISMATCH",
  "message": "Client version không tương thích"
}
```

---

### 3.3. `ErrorCodes.kt` — Mã lỗi

| Mã | Ý nghĩa |
|----|---------|
| `PROTOCOL_VERSION_MISMATCH` | Client và server không cùng phiên bản MAJOR |
| `INTERNAL_ERROR` | Lỗi nội bộ phía server |

---

### 3.4. `ProtocolVersion.kt` — Phiên bản giao thức

**Giá trị hiện tại:**
- `MAJOR = 1`
- `MINOR = 0`
- `STRING = "1.0"`

**Ý nghĩa MAJOR / MINOR:**

| Thành phần | Khi nào tăng | Tác động |
|------------|--------------|----------|
| **MAJOR** | Thay đổi **phá vỡ tương thích** (breaking change): xóa/đổi tin nhắn, đổi cấu trúc bắt buộc... | Client/server khác MAJOR → **không tương thích** |
| **MINOR** | Thay đổi **tương thích ngược**: thêm tin nhắn mới, thêm trường optional... | Client/server cùng MAJOR → **vẫn tương thích** |

**Hàm kiểm tra tương thích:**
```kotlin
fun majorCompatible(remote: String): Boolean
```
- Chỉ so sánh phần **MAJOR** (trước dấu `.`)
- Bỏ qua phần MINOR

**Bảng tương thích:**

| Server | Client | Kết quả |
|--------|--------|---------|
| 1.0 | 1.0 | ✅ Chấp nhận |
| 1.0 | 1.7 | ✅ Chấp nhận (cùng MAJOR) |
| 1.5 | 1.0 | ✅ Chấp nhận (cùng MAJOR) |
| 1.0 | 2.0 | ❌ Từ chối → `PROTOCOL_VERSION_MISMATCH` |

---

### 3.5. `ProtocolJson.kt` — Cấu hình JSON

```kotlin
val ProtocolJson: Json = Json {
    prettyPrint = true
    ignoreUnknownKeys = true      // Bỏ qua trường lạ khi deserialize
    isLenient = true
    encodeDefaults = true
    useArrayPolymorphism = false
    classDiscriminator = "type" // Trường phân biệt loại tin nhắn
}
```

**Điểm quan trọng:** `classDiscriminator = "type"`  
→ Mỗi tin nhắn JSON có trường `"type"` để biết deserialize thành class nào (`hello`, `ping`, `Welcome`, `pong`, `rejected`).

---

## 4. Luồng giao tiếp cơ bản

```
Client                          Server
  |                               |
  |--- Hello (protocolVersion) -->|
  |                               |-- Kiểm tra majorCompatible()
  |                               |
  |<-- Welcome (nếu OK) ----------|  hoặc
  |<-- Rejected (nếu lỗi) --------|
  |                               |
  |--- Ping (clientTime) -------->|
  |<-- Pong (serverTime) ---------|
```

---

## 5. Tích hợp hiện tại trong dự án

**Server** (`server/.../Application.kt`):
- Dùng `ProtocolJson` cho ContentNegotiation
- Có endpoint HTTP: `GET /protocol/version` → trả về `{ major, minor, name }`

**Chưa hoàn thiện:**
- Server **chưa** xử lý `ClientMessage` / gửi `ServerMessage` qua WebSocket
- Giao thức messaging đã được **định nghĩa**, nhưng chưa được **sử dụng đầy đủ** ở runtime

---

## 6. Hướng mở rộng sau này

Khi thêm tính năng gameplay, mở rộng theo pattern hiện có:

1. Thêm class mới vào `ClientMessage` hoặc `ServerMessage`
2. Gắn `@SerialName("ten_type")` cho trường `"type"` trong JSON
3. Nếu **breaking change** → tăng `MAJOR`
4. Nếu **thêm tính năng tương thích ngược** → tăng `MINOR`

**Ví dụ tin nhắn có thể thêm sau:**
- `ClientMessage.Move` — gửi hành động di chuyển
- `ServerMessage.GameState` — đồng bộ trạng thái game
- `ServerMessage.PlayerJoined` — thông báo người chơi mới

---

## 7. Ghi chú nhanh (cheat sheet)

| Khái niệm | Giá trị / Ghi chú |
|-----------|-------------------|
| Phiên bản hiện tại | `1.0` |
| Module chứa protocol | `core` (`commonMain`) |
| Định dạng wire | JSON |
| Trường phân loại tin nhắn | `"type"` |
| Kiểm tra tương thích | Chỉ so sánh **MAJOR** |
| Handshake | `Hello` → `Welcome` hoặc `Rejected` |
| Heartbeat | `Ping` → `Pong` |
