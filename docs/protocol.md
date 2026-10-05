# Tài liệu ghi chép: Thư mục `protocol`

> Dự án: **fivelinks-cmp**  
> Vị trí: `core/src/commonMain/kotlin/com/karasuma/fivelinks/fivelinks_cmp/protocol/`  
> Cập nhật: 05/10/2026 (giao thức **1.1**: phòng chơi online 1 đấu 1)

---

## 1. Tổng quan

Thư mục `protocol` định nghĩa **hợp đồng giao tiếp (communication contract)** giữa **client** và **server** qua mạng.

Vì nằm trong module **`core` → `commonMain`**, code ở đây được biên dịch cho **mọi nền tảng** (server, Android, iOS, Desktop...). Cả hai phía dùng **cùng một định nghĩa tin nhắn**, tránh lệch format JSON giữa client và server.

**Mục tiêu chính:**
- Một nguồn chân lý duy nhất cho giao thức mạng
- An toàn về kiểu (type-safe) với Kotlin + kotlinx.serialization
- Serialization JSON nhất quán
- Quản lý phiên bản giao thức và kiểm tra tương thích
- **Server nắm toàn bộ ván đấu**: client chỉ gửi nước đi, server kiểm tra bằng `GameEngine.applyMove` và chỉ gửi về phần mỗi người được phép thấy

---

## 2. Cấu trúc thư mục

| File | Vai trò |
|------|---------|
| `ClientMessage.kt` | Tin nhắn **client → server** |
| `ServerMessage.kt` | Tin nhắn **server → client** |
| `PlayerView.kt` | Phần ván đấu **một người được phép thấy** + hàm `GameState.viewFor(playerId)` |
| `Room.kt` | Thiết lập phòng (`RoomSettings`), trạng thái phòng (`RoomStatus`), người trong phòng (`RoomPlayer`) |
| `GameEvents.kt` | Sự kiện trong ván (`GameEvent`), lý do bỏ lượt, kết quả ván (`GameResult`) |
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
| `Hello` | `"hello"` | Bắt tay khi kết nối | `protocolVersion`, `playerName`, `sessionToken?` (gửi lại token cũ để **vào lại** sau khi rớt mạng) |
| `Ping` | `"ping"` | Kiểm tra độ trễ / heartbeat | `clientTime` |
| `CreateRoom` | `"create_room"` | Tạo phòng mới | `settings` (`RoomSettings`) |
| `JoinRoom` | `"join_room"` | Vào phòng bằng mã | `roomCode` (6 ký tự) |
| `SubmitMove` | `"submit_move"` | Gửi nước đi | `move` (`Move`), `turnNumber` (lượt mà nước đi này dành cho) |
| `LeaveRoom` | `"leave_room"` | Rời phòng (đang chơi thì **xử thua**) | — |
| `RequestRematch` | `"request_rematch"` | Xin chơi lại | — (đủ cả 2 người xin thì ván mới bắt đầu) |

`turnNumber` trong `SubmitMove` giúp server loại các nước **gửi trùng** (bấm 2 lần) hoặc **gửi trễ** (lượt đã qua) → `STALE_TURN`.

**Ví dụ JSON (Hello):**
```json
{
  "type": "hello",
  "messageId": "abc-123",
  "protocolVersion": "1.1",
  "playerName": "Player1",
  "sessionToken": null
}
```

---

### 3.2. `ServerMessage.kt` — Server gửi về Client

- Kiểu: `sealed interface`
- Mọi tin nhắn đều có: `messageId: String`

| Loại | `type` (JSON) | Mục đích | Các trường chính |
|------|---------------|----------|------------------|
| `Welcome` | `"Welcome"` | Chấp nhận kết nối | `protocolVersion`, `playerName`, `playerId`, `sessionToken` (client lưu lại để vào lại) |
| `Pong` | `"pong"` | Trả lời Ping | `serverTime` |
| `Rejected` | `"rejected"` | Từ chối yêu cầu | `reason` (ErrorCodes), `message`, `inReplyTo?` (messageId của tin bị từ chối) |
| `RoomState` | `"room_state"` | Ai đang trong phòng, phòng ở giai đoạn nào | `roomCode`, `settings`, `status`, `players` |
| `GameSnapshot` | `"game_snapshot"` | **Đồng bộ toàn bộ**: khi ván bắt đầu và khi vào lại giữa ván | `roomCode`, `board`, `view`, `turnTimeLeftMs` |
| `StateUpdate` | `"state_update"` | Ván đấu thay đổi | `view`, `events`, `turnTimeLeftMs` |
| `PlayerConnection` | `"player_connection"` | Một người rớt mạng / vào lại | `playerId`, `connected`, `forfeitInMs?` (đếm ngược tới lúc xử thua) |
| `GameOver` | `"game_over"` | Ván kết thúc (gửi sau `StateUpdate` cuối) | `result` (`GameResult`) |

**Ví dụ JSON (Rejected):**
```json
{
  "type": "rejected",
  "messageId": "xyz-456",
  "reason": "NOT_YOUR_TURN",
  "message": "It's not your turn",
  "inReplyTo": "abc-789"
}
```

---

### 3.3. `PlayerView.kt` — Che thông tin bí mật

Server giữ `GameState` đầy đủ; mỗi người chỉ nhận `PlayerView` của mình (`state.viewFor(playerId)`):

| Có trong `PlayerView` | **Không** gửi |
|---|---|
| Quân trên bàn (`chips`), hàng đã khóa, bài đã đánh (`discard`) | Bài trên tay **đối thủ** (chỉ gửi **số lá**: `handSizes`) |
| Bài trên tay **mình** (`hand`) | **Thứ tự bộ bài** (chỉ gửi **số lá còn lại**: `deckSize`) |
| Người chơi, lượt hiện tại, `turnNumber`, nước vừa đi (`lastMove`) | **`seed`** trong `config` (bị đặt về `0`: biết seed là suy ra được cả bộ bài) |
| Người thắng (`winner`) hoặc hòa (`isDraw`) | Bố cục bàn cờ (gửi **một lần** trong `GameSnapshot`) |

Bàn cờ hiện luôn là bố cục chuẩn (`Board.standard`), nhưng vẫn được gửi trong `GameSnapshot` để sau này có thể thêm biến thể "bàn cờ ngẫu nhiên" mà không phải đổi giao thức.

---

### 3.4. `Room.kt` và `GameEvents.kt`

**`RoomSettings`** — người tạo phòng chọn: `sequenceToWin` (1 hoặc 2 hàng), `tacticalCrafting` (bật/tắt chế độ chiến thuật).

**`RoomStatus`** — `WAITING` (chờ người thứ 2) → `PLAYING` → `FINISHED` (có thể xin chơi lại).

**`RoomPlayer`** — `playerId`, `name`, `team`, `connected`, `wantsRematch`.

**`GameEvent`** (trong `StateUpdate.events`, theo thứ tự xảy ra):

| Loại | `type` | Ý nghĩa |
|------|--------|---------|
| `MovePlayed` | `"move_played"` | Một người vừa đi `move` |
| `TurnPassed` | `"turn_passed"` | Một người bị bỏ lượt, `reason`: `NO_LEGAL_MOVES` (hết bài đánh được) hoặc `TIMEOUT` (hết giờ lượt) |

**`GameResult`** — `winner` (null nếu hòa) + `reason`:

| `GameEndReason` | Ý nghĩa |
|---|---|
| `COMPLETED_SEQUENCES` | Đủ số hàng để thắng |
| `DRAW_NO_MOVES` | Không ai còn nước đi → hòa |
| `FORFEIT_DISCONNECTED` | Người thua mất kết nối quá **60 giây** |
| `FORFEIT_LEFT` | Người thua rời phòng |

---

### 3.5. `ErrorCodes.kt` — Mã lỗi

| Mã | Ý nghĩa |
|----|---------|
| `PROTOCOL_VERSION_MISMATCH` | Client và server không cùng phiên bản MAJOR |
| `INTERNAL_ERROR` | Lỗi nội bộ phía server |
| `INVALID_REQUEST` | Yêu cầu sai định dạng / tham số không hợp lệ |
| `ROOM_NOT_FOUND` | Không có phòng với mã này |
| `ROOM_FULL` | Phòng đã đủ 2 người |
| `NOT_IN_ROOM` | Yêu cầu cần ở trong phòng nhưng người chơi chưa vào phòng nào |
| `NOT_YOUR_TURN` | Gửi nước đi khi chưa tới lượt |
| `ILLEGAL_MOVE` | Luật chơi từ chối nước đi (`message` nói lý do) |
| `STALE_TURN` | Nước đi dành cho một lượt đã qua (gửi trùng / gửi trễ) |
| `SESSION_EXPIRED` | `sessionToken` không còn hiệu lực → bắt đầu phiên mới |

---

### 3.6. `ProtocolVersion.kt` — Phiên bản giao thức

**Giá trị hiện tại:**
- `MAJOR = 1`
- `MINOR = 1`
- `STRING = "1.1"`

1.1 chỉ **thêm** tin nhắn và thêm trường **tùy chọn** (có giá trị mặc định), nên vẫn tương thích với 1.0.

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
| 1.1 | 1.1 | ✅ Chấp nhận |
| 1.1 | 1.0 | ✅ Chấp nhận (cùng MAJOR) |
| 1.0 | 1.7 | ✅ Chấp nhận (cùng MAJOR) |
| 1.1 | 2.0 | ❌ Từ chối → `PROTOCOL_VERSION_MISMATCH` |

---

### 3.7. `ProtocolJson.kt` — Cấu hình JSON

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
→ Mỗi tin nhắn JSON có trường `"type"` để biết deserialize thành class nào. Các object lồng bên trong (`Move`, `GameEvent`) cũng có `"type"` riêng của chúng.

---

## 4. Luồng giao tiếp

**Bắt tay và heartbeat:**
```
Client                          Server
  |                               |
  |--- Hello (protocolVersion) -->|
  |                               |-- Kiểm tra majorCompatible()
  |<-- Welcome (playerId, token) -|  hoặc
  |<-- Rejected (nếu lỗi) --------|
  |                               |
  |--- Ping (clientTime) -------->|
  |<-- Pong (serverTime) ---------|
```

**Tạo phòng, vào phòng, chơi:**
```
Người A                    Server                      Người B
 CreateRoom ────────────▶  RoomState(K7QP2M, WAITING)
                           ◀──────────────────────────  JoinRoom("K7QP2M")
 ◀─ RoomState(PLAYING)     RoomState(PLAYING) ────────▶
 ◀─ GameSnapshot(view A)   GameSnapshot(view B) ──────▶
 SubmitMove(move, lượt) ─▶ GameEngine.applyMove ✔
 ◀─ StateUpdate(view A)    StateUpdate(view B) ───────▶
                ...
 ◀─ GameOver(result)       GameOver(result) ──────────▶
```

**Rớt mạng và vào lại:**
```
Người B rớt mạng ─▶ Server ─▶ PlayerConnection(B, connected=false, forfeitInMs=60000) ─▶ Người A
Người B: Hello(sessionToken) ─▶ Server ─▶ Welcome + GameSnapshot ─▶ Người B
                                        └▶ PlayerConnection(B, connected=true) ─▶ Người A
Quá 60 giây chưa vào lại ─▶ GameOver(winner = A, FORFEIT_DISCONNECTED)
```

**Hết giờ lượt:** server bỏ lượt người đó → `StateUpdate` với `TurnPassed(reason = TIMEOUT)`.

---

## 5. Tích hợp hiện tại trong dự án

**Server** (`server/.../Application.kt`):
- Dùng `ProtocolJson` cho ContentNegotiation
- Có endpoint HTTP: `GET /protocol/version` → trả về `{ major, minor, name }`

**Core:**
- `GameEngine.skipStuckTurns(state)`: người tới lượt không còn nước nào đánh được thì bỏ lượt; không ai đánh được → `GameState.isDraw = true`. App (chơi offline) đang dùng; server sẽ gọi sau mỗi lần đổi lượt.

**Chưa hoàn thiện:**
- Server **chưa** có WebSocket và chưa xử lý phòng chơi (Giai đoạn 1 của kế hoạch online)
- App chưa kết nối server (Giai đoạn 2–3)

---

## 6. Hướng mở rộng sau này

Khi thêm tính năng, mở rộng theo pattern hiện có:

1. Thêm class mới vào `ClientMessage` hoặc `ServerMessage`
2. Gắn `@SerialName("ten_type")` cho trường `"type"` trong JSON
3. Nếu **breaking change** → tăng `MAJOR`
4. Nếu **thêm tính năng tương thích ngược** (trường mới phải có giá trị mặc định) → tăng `MINOR`

**Ví dụ có thể thêm sau:** ghép trận ngẫu nhiên, chơi 2v2 / 3 đội, chat / emote, tài khoản và xếp hạng.

---

## 7. Ghi chú nhanh (cheat sheet)

| Khái niệm | Giá trị / Ghi chú |
|-----------|-------------------|
| Phiên bản hiện tại | `1.1` |
| Module chứa protocol | `core` (`commonMain`) |
| Định dạng wire | JSON |
| Trường phân loại tin nhắn | `"type"` |
| Kiểm tra tương thích | Chỉ so sánh **MAJOR** |
| Handshake | `Hello` → `Welcome` hoặc `Rejected` |
| Heartbeat | `Ping` → `Pong` |
| Chơi online | `CreateRoom` / `JoinRoom` → `GameSnapshot` → `SubmitMove` ↔ `StateUpdate` → `GameOver` |
| Che thông tin | `state.viewFor(playerId)` — không gửi bài đối thủ, bộ bài, seed |
| Rớt mạng | Giữ chỗ **60 giây**, quá hạn **xử thua** |
| Hết giờ lượt | **Bỏ lượt** (`TurnPassed`, `TIMEOUT`) |
