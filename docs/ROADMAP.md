# Five Links — Roadmap hoàn thiện sản phẩm

> Cập nhật: 25/10/2026  
> Trạng thái: P0, P1, P2, P3, P4, P5 đã hoàn tất trong source và pass 100% test suites (`:core:jvmTest`, `:app:shared:jvmTest`, `:server:test`). P6 là định hướng tính năng hậu phát hành (post-launch scope).

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
| AI trong UI offline | P2 đã triển khai trong source | `GameViewModel` dùng `AiFacadeService` (tactical + MCTS/hybrid), chạy nền, có heuristic fallback và hủy lượt AI an toàn. |
| Online server | Có lõi 1v1 | Room, đồng hồ lượt, reconnect, rematch, che bài đối thủ và WebSocket đã triển khai. |
| Online client | Có hạ tầng | `OnlineClient`, `OnlineSession`, `OnlineGameController` và test có sẵn. |
| Online trong app | P0 đã triển khai | Menu → lobby → game, tạo/vào phòng, reconnect/rematch/rời phòng và `OnlineGameController` đã được nối vào app. |
| Cấu hình client online | P1 đã triển khai trong source | Android/iOS/Desktop/Web/Wasm có URL theo môi trường, token resume và lifecycle socket; staging/production thật còn thiếu. |
| Production vận hành | Chưa có | Cần endpoint thật, giới hạn tải, quan sát hệ thống và pipeline CI/release. |

### Nợ kỹ thuật đáng chú ý

- Tài liệu AI đã được đồng bộ với Search/MCTS, encoder, ONNX và xử lý priors cho Tactical.
- Bộ test `:core:jvmTest`, `:app:shared:jvmTest` và `:server:test` đã chạy xanh; cần tiếp tục kiểm thử thủ công/benchmark trên thiết bị Android và trình duyệt Web mục tiêu.

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

### P2 — Đồng nhất và hoàn thiện AI ✅

**Mục tiêu:** cấp độ AI trong menu thực sự phản ánh engine đã xây và không gây lag UI.

1. Đưa `GameViewModel` qua `AiService`/`AiFacadeService`; giữ heuristic làm fallback an toàn.
2. Cancellation-safe AI turn: rời game hoặc khởi tạo ván mới phải hủy tìm kiếm cũ.
3. Đo latency và sức mạnh cho Easy/Medium/Hard; tinh chỉnh budget theo thiết bị/server.
4. Xử lý neural cho Tactical một cách có chủ đích:
   - ngắn hạn: heuristic priors khi có crafting/Divine Wipe, không dựa vào exception fallback;
   - dài hạn: mở rộng codec, encoder, self-play và ONNX model để hỗ trợ toàn bộ action space Tactical.
5. Bổ sung regression/arena test cho cả Classic và Tactical.

**Đã triển khai:** `GameViewModel` gọi `AiFacadeService` trên dispatcher nền; khi facade lỗi thì heuristic chọn nước hợp lệ. Lượt AI được hủy khi tạo ván mới/rời game và generation token chặn kết quả cũ ghi đè state mới. Easy/Medium/Hard dùng budget search riêng (80/50 ms, 400/150 ms, 2000/600 ms). Neural policy chỉ áp dụng nước Classic, còn Craft/Divine Wipe giữ heuristic priors có chủ đích thay vì ném exception từ `ActionCodec`. `TacticalEngine` đã hỗ trợ chặn open-four bằng `CraftPlace`/`CraftRemove`/`DivineWipe`, và bộ test regression/arena (`AiRegressionTest`, `AiArenaTest`, `AiBenchmarkArenaTest`, `GameViewModelTest`) đã bao phủ cả Classic lẫn Tactical.

**Còn để xác nhận ngoài source:** chạy benchmark/arena trên Android, Desktop và Web mục tiêu để chốt budget thực tế; action space, training data và ONNX model đầy đủ cho Tactical là hạng mục dài hạn.

### P3 — Chất lượng trải nghiệm offline ✅

**Mục tiêu:** game có thể chơi, tiếp tục và hiểu được mà không cần giải thích ngoài app.

1. Lưu/khôi phục ván offline và cài đặt: tên, theme, luật gần đây, độ khó, âm thanh/rung qua `OfflineStorage` đa nền tảng (Android `SharedPreferences`, Desktop `java.util.prefs`, iOS `NSUserDefaults`, Web `localStorage`).
2. Onboarding walkthrough trực quan (`OnboardingDialog`) 4 bước cho Sequence, Jack 1 mắt/2 mắt, chế tạo chiến thuật (Crafting) và Thiên Phạt (Divine Wipe); đồng thời giữ nguyên `RulesDialog` chi tiết.
3. Hoàn thiện accessibility: semantic labels (`contentDescription`, `Role.Button`), hỗ trợ focus bàn phím trên Desktop/Web, kích thước chạm đạt chuẩn, và mô tả chi tiết bằng tiếng Việt cho quân bài và ô bàn cờ.
4. Responsive layout hai chế độ cho `GameScreen`: giao diện dọc cho điện thoại nhỏ, và giao diện 2 cột thích ứng (`BoxWithConstraints`) cho màn hình ngang/tablet/desktop (bàn cờ bên trái, thanh chế tạo và bài trên tay bên phải).
5. Tùy chọn âm thanh và rung (`SoundController`, `LocalSoundEnabled`, `LocalVibrationEnabled`) gắn vào `MenuScreen`, `BoardGrid`, `HandView`, và tắt rung khi người dùng tắt haptics.
6. Kiểm thử đơn vị lưu trữ và tuần tự hóa ngoại tuyến (`OfflineStorageTest`) đạt 100% pass.

**Đã triển khai:** Toàn bộ tính năng lưu trữ ván đấu và cài đặt ngoại tuyến, onboarding tương tác, a11y semantic labels, responsive 2-column layout, bộ chuyển đổi âm thanh/rung, và kiểm thử JVM/Android/Desktop đã hoàn tất và vượt qua toàn bộ test suite.

**Definition of Done:** Người chơi mới có thể bắt đầu một ván, hiểu luật và nước đi ngay trong app thông qua onboarding, và tiếp tục ván đấu dở dang sau khi khởi động lại ứng dụng.


### P4 — Server sẵn sàng vận hành ✅

**Mục tiêu:** chạy server 1v1 an toàn và có thể quan sát trong môi trường staging/production.

1. Đưa port, CORS/origin WebSocket, thời hạn room/session, giới hạn dung lượng và logging vào environment config (`OnlineConfig.fromEnvironment()`).
2. Thêm rate limit (`RateLimiter`) trượt thời gian chống flood tin nhắn và thao tác đánh bài; thêm ngưỡng room (`MAX_ROOMS`) và session capacity (`MAX_SESSIONS`) trả về mã lỗi chuẩn `SERVER_BUSY` và `RATE_LIMITED`.
3. Bổ sung các điểm cuối giám sát tiêu chuẩn production: `/health`, `/health/liveness`, `/health/readiness` (kiểm tra trạng thái tải và tắt máy), `/metrics` (uptime, RAM JVM, số phòng, số nước đi, số lượt hit rate-limit).
4. Chuẩn hóa structured logging (`logback.xml`) với biến môi trường `LOG_LEVEL` linh hoạt cho console/container log.
5. Thiết lập cơ chế Graceful Shutdown tự động qua `ApplicationStopping` của Ktor: báo hiệu trạng thái `SHUTTING_DOWN` cho Load Balancer ngắt traffic mới, gửi `RoomClosed(SERVER_SHUTDOWN)` cho toàn bộ phòng đang chơi để client trở về menu êm đẹp, sau đó đóng socket sạch sẽ.
6. Đóng gói container đa tầng (`server/Dockerfile`) siêu nhẹ dựa trên Alpine JRE 21 với non-root user và healthcheck tích hợp.
7. Soạn thảo tài liệu vận hành chi tiết `docs/SERVER_OPERATIONS.md` bao gồm bảng biến môi trường, Docker Compose, cấu hình Caddy/Nginx reverse proxy, quy trình rollback và sổ tay xử lý sự cố.
8. Hoàn thiện bộ kiểm thử `ProductionReadinessTest`, `ApplicationTest`, và mở rộng `GameServerTest` bao phủ toàn bộ tính năng vận hành mới.

**Đã triển khai:** Đầy đủ hạ tầng cấu hình, rate limiting, giám sát health/metrics, graceful shutdown, containerization và tài liệu vận hành; toàn bộ test suite máy chủ đạt 100% pass.

**Definition of Done:** Staging và production chịu được kết nối lỗi, flood request, có log và metric giám sát đầy đủ, từ chối origin trái phép và tự động tắt máy/khởi động lại an toàn không làm đơ ứng dụng người chơi.


### P5 — Release engineering và đảm bảo chất lượng ✅

**Mục tiêu:** tạo bản phát hành lặp lại được và giảm lỗi hồi quy.

1. Thiết lập CI tự động hóa (`.github/workflows/ci.yml`):
   - Chạy toàn bộ test suite (`:core:jvmTest`, `:app:shared:jvmTest`, `:server:test`).
   - Kiểm tra build artifacts đa nền tảng: Desktop JAR (`:app:desktopApp:jar`), Server distribution (`:server:installDist`), Web distribution (`:app:webApp:jsBrowserDistribution`).
   - Kiểm tra assemble Android APK (`:app:androidApp:assembleDebug`).
   - Kiểm tra đóng gói Docker image cho server (`server/Dockerfile`).
2. Cách ly signing key và bí mật (`app/androidApp/build.gradle.kts`):
   - Không commit `.jks`, `.keystore` hay `keystore.properties` vào repository (đã có trong `.gitignore`).
   - Cung cấp cấu hình nạp keystore động từ environment variables trong CI (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) hoặc file `keystore.properties` ngoài Git.
3. Bộ test hồi quy và tương thích serialization (`SerializationCompatibilityTest.kt`):
   - Kiểm thử round-trip JSON serialization cho tất cả message client/server và `GameState`.
   - Kiểm thử khả năng chịu lỗi và tương thích ngược/tiến khi payload JSON có thêm các trường chưa biết (unknown keys).
   - Kiểm tra các mã lỗi mới (`RATE_LIMITED`, `SERVER_BUSY`) và lý do đóng phòng (`SERVER_SHUTDOWN`).
4. Xây dựng tài liệu quy trình phát hành (`docs/RELEASE_CHECKLIST.md`):
   - Chiến lược versioning (SemVer: Major.Minor.Patch) và `versionCode` mapping.
   - Quality gates bắt buộc trước khi phát hành (Unit test, Integration test, Lint/Build, Security scan).
   - Bảng checklist kiểm thử thủ công đa nền tảng (Android APK/AAB, Web Chrome/Safari/Firefox, Desktop JVM Windows/macOS/Linux).
   - Quy trình Go/No-Go, quản lý rollback và chính sách quyền riêng tư / dữ liệu người dùng.

**Đã triển khai:** Workflow CI/CD hoàn chỉnh trên GitHub Actions, cách ly secret signing, bộ test tương thích giao thức, và tài liệu release checklist toàn diện.

**Definition of Done:** Một pipeline tạo được artifact phát hành, test xanh 100%, signing không chứa bí mật trong repo, và có checklist go/no-go chi tiết.

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

