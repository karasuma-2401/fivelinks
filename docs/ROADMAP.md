# Five Links — Roadmap hoàn thiện sản phẩm

> Cập nhật: 23/10/2026  
> Trạng thái: P0 đã triển khai trong source, đang chờ build/test trên thiết bị; P1 đã triển khai trong source, đang chờ cấu hình endpoint thật và kiểm thử trên thiết bị; P2–P6 chưa bắt đầu.

## 1. Mục tiêu phát hành

Phát hành một bản **Five Links** chơi được trọn vẹn trên Android và Web, với các chế độ:

- Chơi với AI, hai người cùng thiết bị và online 1 đấu 1.
- Luật Classic và Tactical (Craft Place, Craft Remove, Divine Wipe).
- Trải nghiệm ổn định khi mất mạng, có thể chơi lại và có cấu hình server cho môi trường phát hành.

Desktop và iOS tiếp tục được giữ khả năng biên dịch; mức độ kiểm thử/phát hành chính thức sẽ được quyết định ở giai đoạn release.

## 2. Hiện trạng đã kiểm chứng

| Khu vực | Trạng thái | Ghi chú |
|---|---|---|
| Game domain | Hoàn thành nền tảng | Bàn 10×10, Sequence, Jack, bỏ lượt khi kẹt, Classic/Tactical và luật Divine Wipe đã có test. |
| UI offline | Có thể chơi | Menu hỗ trợ AI hoặc pass-and-play; màn game, hiệu ứng, dark theme và luật chơi đã có. |
| AI | Có lõi nâng cao | Tactical, determinization/MCTS, heuristic, hybrid evaluation, ONNX JVM/Android, self-play và test hiện diện. |
| AI trong UI offline | Cần tích hợp | `GameViewModel` hiện gọi `HeuristicEvaluator` trực tiếp, chưa dùng `AiFacadeService` (MCTS/ONNX). |
| Online server | Có lõi 1v1 | Room, đồng hồ lượt, reconnect, rematch, che bài đối thủ và WebSocket đã triển khai. |
| Online client | Có hạ tầng | `OnlineClient`, `OnlineSession`, `OnlineGameController` và test có sẵn. |
| Online trong app | P0 đã triển khai | Menu → lobby → game, tạo/vào phòng, reconnect/rematch/rời phòng và `OnlineGameController` đã được nối vào app. |
| Cấu hình client online | P1 đã triển khai trong source | Android/iOS/Desktop/Web/Wasm có URL theo môi trường, token resume và lifecycle socket; staging/production thật còn thiếu. |
| Production vận hành | Chưa có | Cần endpoint thật, giới hạn tải, quan sát hệ thống và pipeline CI/release. |

### Nợ kỹ thuật đáng chú ý

- `ActionCodec` của neural model chỉ hỗ trợ nước đi Classic. Khi game Tactical cần prior neural, lỗi được bắt rồi fallback heuristic; cần làm fallback có chủ đích hoặc mở rộng action space.
- Tài liệu AI đã lạc hậu so với source: Search/MCTS, encoder và ONNX đã tồn tại.
- Chưa có kết quả test mới trong phiên rà soát này vì Gradle phải tải wrapper/dependency trước; cần chạy lại bộ test đầy đủ khi môi trường đã sẵn sàng.

## 3. Thứ tự triển khai

### P0 — Multiplayer khả dụng trong ứng dụng ✅

**Mục tiêu:** người dùng có thể tạo hoặc vào phòng, chơi một trận online hoàn chỉnh và quay lại menu an toàn.

1. Thêm điều hướng `MENU → ONLINE_LOBBY → GAME` trong `App`.
2. Xây màn nhập/lưu tên người chơi và chọn “Chơi online”.
3. Xây lobby: tạo phòng, nhập mã phòng, chọn `sequenceToWin`, bật/tắt Tactical, trạng thái chờ đối thủ.
4. Nối `OnlineSession` và `OnlineGameController` với `GameScreen`.
5. Hiển thị trạng thái kết nối, retry, đối thủ rớt mạng, thời gian xử thua, lỗi từ server, rematch và rời phòng.
6. Bổ sung test cho navigation/state mapping và các luồng lỗi UI chính.

**Còn để xác nhận:** build/test hai client thật, reconnect trong thời gian cho phép và kiểm tra không còn session/job treo sau khi về menu.

### P1 — Cấu hình môi trường và vòng đời kết nối 🚧

**Mục tiêu:** online không phụ thuộc `localhost` và chịu được vòng đời thực của mobile/web.

1. Thay `ServerConfig.DEV_URL` cố định bằng cấu hình build cho development, staging và production.
2. Dùng `wss://` ở môi trường phát hành; chỉ giữ `ws://localhost` cho development.
3. Lưu tên và session token an toàn theo nền tảng; khôi phục phiên khi app mở lại.
4. Quản lý start/stop socket theo app lifecycle và hiển thị trạng thái khôi phục rõ ràng.
5. Viết tài liệu local/staging/prod cho client và server.

**Đã triển khai:** Android debug/release URL theo Gradle property, Desktop URL theo JVM/env, iOS URL theo Xcode config, Web/Wasm URL theo thẻ meta trong `index.html`, profile store Android/iOS/Desktop/Web, lưu/khôi phục opaque session token và pause/resume socket theo lifecycle.

**Còn để hoàn thành ngoài source:** provisioning staging/production thật, secret/secure-cookie cho hệ thống tài khoản sau này và kiểm thử lifecycle trên thiết bị Android/iOS/Web.

### P2 — Đồng nhất và hoàn thiện AI

**Mục tiêu:** cấp độ AI trong menu thực sự phản ánh engine đã xây và không gây lag UI.

1. Đưa `GameViewModel` qua `AiService`/`AiFacadeService`; giữ heuristic làm fallback an toàn.
2. Cancellation-safe AI turn: rời game hoặc khởi tạo ván mới phải hủy tìm kiếm cũ.
3. Đo latency và sức mạnh cho Easy/Medium/Hard; tinh chỉnh budget theo thiết bị/server.
4. Xử lý neural cho Tactical một cách có chủ đích:
   - ngắn hạn: heuristic priors khi có crafting/Divine Wipe, không dựa vào exception fallback;
   - dài hạn: mở rộng codec, encoder, self-play và ONNX model để hỗ trợ toàn bộ action space Tactical.
5. Bổ sung regression/arena test cho cả Classic và Tactical.

**Definition of Done:** Easy/Medium/Hard khác biệt rõ, UI không bị block, mọi nước AI hợp lệ, và Tactical không phát sinh fallback do exception.

### P3 — Chất lượng trải nghiệm offline

**Mục tiêu:** game có thể chơi, tiếp tục và hiểu được mà không cần giải thích ngoài app.

1. Lưu/khôi phục ván offline và cài đặt: tên, theme, luật gần đây, độ khó.
2. Onboarding ngắn cho Sequence, Jack, crafting và Divine Wipe; vẫn giữ Rules Dialog đầy đủ.
3. Hoàn thiện accessibility: semantic labels, tương phản, cỡ chạm, screen-reader và điều hướng bàn phím Web/Desktop.
4. Rà soát responsive cho điện thoại nhỏ, tablet và web ngang.
5. Thiết kế tùy chọn âm thanh/rung (chỉ thêm asset/audio khi đã chốt phong cách âm thanh).

**Definition of Done:** người chơi mới có thể bắt đầu một ván, hiểu lượt đi đầu tiên và tiếp tục ván offline sau khi mở lại app.

### P4 — Server sẵn sàng vận hành

**Mục tiêu:** chạy server 1v1 an toàn và có thể quan sát trong môi trường staging/production.

1. Đưa port, CORS/origin WebSocket, thời hạn room/session và logging vào environment config.
2. Thêm rate limit cho handshake, tạo/vào phòng và submit move; thêm room/session capacity rõ ràng.
3. Thêm readiness/liveness health check, structured logging, metrics cơ bản và cảnh báo lỗi.
4. Chọn chính sách khởi động lại server: thông báo ván online bị hủy, hoặc bổ sung persistence nếu muốn phục hồi room.
5. Viết hướng dẫn deploy, rollback và xử lý sự cố.

**Definition of Done:** staging chịu được kết nối lỗi/phát lại request phổ biến, có log/metric để chẩn đoán, và không mở WebSocket tùy ý ngoài policy đã định.

### P5 — Release engineering và đảm bảo chất lượng

**Mục tiêu:** tạo bản phát hành lặp lại được và giảm lỗi hồi quy.

1. Thiết lập CI chạy `core:jvmTest`, `server:test`, `app:shared:jvmTest` và build các target phát hành.
2. Thêm smoke/E2E test cho WebSocket, offline AI, reconnect và serialization compatibility.
3. Kiểm thử thủ công trên Android thật, các trình duyệt mục tiêu và ít nhất một target desktop; iOS trên macOS nếu phát hành iOS.
4. Di chuyển signing key và thông tin nhạy cảm khỏi workspace/repository vào secret store của CI.
5. Lập release checklist: versioning, changelog, crash/log review, privacy policy và kênh phản hồi người dùng.

**Definition of Done:** một pipeline tạo được artifact phát hành, test xanh, signing không chứa bí mật trong repo, và có checklist go/no-go.

### P6 — Tính năng sau bản phát hành đầu tiên

Các hạng mục này không chặn bản chơi hoàn chỉnh, nên chỉ mở sau P0–P5:

- Matchmaking ngẫu nhiên.
- Tài khoản/guest identity, lịch sử trận và thống kê.
- Leaderboard, rank/season và anti-abuse tương ứng.
- Friends/invite, chat hoặc emote (cần moderation, report và chính sách lưu dữ liệu trước khi làm).
- Chế độ đội nhiều người nếu gameplay và UX được thiết kế riêng; online hiện là 1v1.

## 4. Phạm vi triển khai gần nhất

Sau khi tài liệu này được chấp thuận, bắt đầu theo lát cắt dọc P0 theo thứ tự:

1. Model navigation + dependency/lifecycle cho online session.
2. Màn Online Lobby và create/join room.
3. Nối lobby với `GameScreen` qua `OnlineGameController`.
4. Room/reconnect/rematch/exit UI.
5. Test, chạy test hiện có và sửa các lỗi phát hiện được.

Không thêm tài khoản, leaderboard, chat, matchmaking hay persistence server trong lát cắt đầu tiên, để hoàn thành luồng online 1v1 đang dang dở trước.

## 5. Các quyết định cần chốt trước P4–P6

| Quyết định | Vì sao cần chốt |
|---|---|
| Nền tảng phát hành ưu tiên | Xác định mức kiểm thử và CI cho Android, Web, Desktop, iOS. |
| Nơi host server và domain production | Quyết định `wss`, secrets, observability, chi phí và quy trình deploy. |
| Guest-only hay tài khoản | Ảnh hưởng auth, quyền riêng tư, persistence và ranking. |
| Có xếp hạng/chat hay không | Xác định yêu cầu moderation, anti-cheat và vận hành cộng đồng. |
| Persist room hay hủy khi server restart | Ảnh hưởng kiến trúc dữ liệu và trải nghiệm reconnect. |

