# Hướng dẫn Vận hành, Triển khai và Xử lý Sự cố Máy chủ FiveLink

Tài liệu này cung cấp toàn bộ hướng dẫn cấu hình, vận hành, bảo trì, triển khai production/staging và quy trình xử lý sự cố cho máy chủ `server` của FiveLink (Ktor 3 + WebSocket).

---

## 1. Tổng quan Kiến trúc

- **Framework:** Ktor Server (Netty Engine) + Kotlinx Coroutines.
- **Giao thức:**
  - HTTP REST: Health check (`/health`), Metrics (`/metrics`), Protocol Version (`/protocol/version`).
  - WebSocket: `/ws` — Giao tiếp realtime hai chiều dạng văn bản khung JSON (dùng `ProtocolJson`).
- **Mô hình Trạng thái:** Toàn bộ trạng thái ván đấu (`GameState`) được quản lý tập trung và an toàn trên RAM máy chủ thông qua Mutex. Người chơi chỉ nhận `PlayerView` (ẩn bài của đối thủ và deck cọc rút).

---

## 2. Bảng Biến Môi trường (Environment Variables)

| Biến môi trường | Kiểu dữ liệu | Mặc định | Mô tả |
|---|---|---|---|
| `PORT` | Integer | `8080` | Cổng lắng nghe HTTP & WebSocket. Tiêu chuẩn trên Cloud Run / Heroku / Container. |
| `HOST` | String | `0.0.0.0` | Địa chỉ mạng bind socket. |
| `LOG_LEVEL` | String | `INFO` | Mức log hệ thống (`TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`). |
| `ALLOWED_ORIGINS` | String | `*` | Danh sách domain được phép kết nối WebSocket từ trình duyệt, phân tách bởi dấu phẩy (vd: `https://fivelinks.app,http://localhost:8080`). Khách vãng lai native (Android, Desktop, iOS không gửi Origin header) luôn được cho phép. |
| `MAX_ROOMS` | Integer | `1000` | Số phòng chơi đồng thời tối đa. Vượt quá ngưỡng sẽ phản hồi `SERVER_BUSY`. |
| `MAX_SESSIONS` | Integer | `5000` | Số phiên người chơi tối đa trên RAM trước khi từ chối bắt tay mới. |
| `TURN_TIME_SECONDS` | Long | `60` | Thời gian tối đa (giây) cho một lượt đánh trước khi hệ thống tự động bỏ lượt (`Pass`). |
| `RECONNECT_GRACE_SECONDS`| Long | `60` | Thời gian ân hạn (giây) chờ người chơi kết nối lại trước khi xử thua (`FORFEIT_DISCONNECTED`). |
| `WAITING_ROOM_TTL_MINUTES`| Long | `10` | Thời gian sống (phút) của phòng đang chờ đối thủ trước khi tự hủy (`RoomCloseReason.EXPIRED`). |
| `IDLE_SESSION_TTL_MINUTES`| Long | `30` | Thời gian dọn dẹp các session rác không có kết nối socket và không ở trong phòng. |
| `RATE_LIMIT_ENABLED` | Boolean | `true` | Bật/tắt bộ giới hạn tần suất yêu cầu. |
| `RATE_LIMIT_PER_MINUTE` | Integer | `120` | Giới hạn số thông điệp mỗi phút trên mỗi kết nối/session. |
| `MOVE_RATE_LIMIT_PER_SECOND`| Integer | `5` | Giới hạn số nước đi gửi lên mỗi giây để chống spam. |

---

## 3. Các Điểm cuối Giám sát (Observability Endpoints)

### `GET /health/liveness`
- **Mục đích:** Kubernetes/Docker liveness probe kiểm tra tiến trình server còn sống hay đã deadlock.
- **Phản hồi:** `200 OK`
  ```json
  { "status": "UP" }
  ```

### `GET /health/readiness`
- **Mục đích:** Load balancer kiểm tra server đã sẵn sàng nhận kết nối mới hay đang quá tải / đang tắt máy.
- **Phản hồi khi khỏe:** `200 OK`
  ```json
  {
    "status": "UP",
    "uptimeSeconds": 3600,
    "activeRooms": 14,
    "activeSessions": 32,
    "maxRooms": 1000,
    "maxSessions": 5000,
    "isShuttingDown": false
  }
  ```
- **Phản hồi khi quá tải hoặc đang bảo trì:** `503 Service Unavailable`
  ```json
  {
    "status": "SHUTTING_DOWN",
    "uptimeSeconds": 7200,
    "activeRooms": 0,
    "activeSessions": 0,
    "maxRooms": 1000,
    "maxSessions": 5000,
    "isShuttingDown": true
  }
  ```

### `GET /metrics`
- **Mục đích:** Thu thập số liệu vận hành phục vụ Prometheus hoặc monitoring dashboard.
- **Phản hồi:** `200 OK`
  ```json
  {
    "status": "UP",
    "uptimeSeconds": 3600,
    "activeRooms": 14,
    "activeSessions": 32,
    "maxRooms": 1000,
    "maxSessions": 5000,
    "totalRoomsCreated": 142,
    "totalGamesFinished": 128,
    "totalMovesSubmitted": 3840,
    "totalRateLimitHits": 2,
    "isShuttingDown": false,
    "jvmFreeMemoryMb": 180,
    "jvmTotalMemoryMb": 256,
    "jvmMaxMemoryMb": 512,
    "availableProcessors": 4
  }
  ```

---

## 4. Hướng dẫn Đóng gói & Triển khai Docker

### 4.1. Build Image Docker
Từ thư mục gốc dự án:
```bash
docker build -f server/Dockerfile -t fivelinks-server:latest .
```

### 4.2. Khởi chạy Container
```bash
docker run -d \
  --name fivelinks-server \
  -p 8080:8080 \
  -e PORT=8080 \
  -e LOG_LEVEL=INFO \
  -e ALLOWED_ORIGINS="https://fivelinks.app,http://localhost:8080" \
  fivelinks-server:latest
```

### 4.3. Docker Compose mẫu
```yaml
version: '3.8'
services:
  fivelinks-server:
    image: fivelinks-server:latest
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      - PORT=8080
      - LOG_LEVEL=INFO
      - ALLOWED_ORIGINS=*
      - MAX_ROOMS=2000
      - MAX_SESSIONS=8000
      - RATE_LIMIT_ENABLED=true
    deploy:
      resources:
        limits:
          memory: 512M
          cpus: '1.0'
```

---

## 5. Cấu hình Reverse Proxy (Caddy / Nginx)

### Caddyfile (Tự động HTTPS + WSS)
```caddy
fivelinks.app {
    reverse_proxy 127.0.0.1:8080 {
        # Ktor websockets cần giữ connection mở
        transport http {
            keepalive 120s
        }
    }
}
```

### Nginx
```nginx
server {
    listen 443 ssl http2;
    server_name api.fivelinks.app;

    ssl_certificate /etc/letsencrypt/live/api.fivelinks.app/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/api.fivelinks.app/privkey.pem;

    location / {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # WebSocket timeouts
        proxy_read_timeout 120s;
        proxy_send_timeout 120s;
    }
}
```

---

## 6. Chính sách Khởi động lại Máy chủ & Tắt máy An toàn (Graceful Shutdown)

- Khi tiến trình nhận tín hiệu `SIGTERM` hoặc `SIGINT`, Ktor kích hoạt sự kiện `ApplicationStopping`.
- `GameServer.shutdown()` được gọi tự động:
  1. Đổi cờ `isShuttingDown = true`. Điểm cuối `/health/readiness` lập tức chuyển sang trả về `503 Service Unavailable`, báo cho Load Balancer ngừng điều hướng kết nối mới tới pod này.
  2. Gửi thông điệp `ServerMessage.RoomClosed(reason = SERVER_SHUTDOWN)` tới toàn bộ các phòng đang hoạt động.
  3. Ứng dụng client nhận được thông báo này sẽ hiển thị thông điệp *"Máy chủ đang bảo trì hoặc khởi động lại"* và đưa người chơi về màn hình chính một cách êm đẹp.
  4. Đóng toàn bộ socket kết nối sau khi xả hàng đợi tin nhắn.

---

## 7. Quy trình Rollback

Nếu phiên bản mới phát sinh lỗi hồi quy nghiêm trọng:
1. **Kiểm tra trạng thái pod/container:**
   ```bash
   curl -i http://localhost:8080/health/readiness
   ```
2. **Kích hoạt rollback phiên bản container trước:**
   ```bash
   # Nếu dùng Docker tag:
   docker stop fivelinks-server
   docker rm fivelinks-server
   docker run -d --name fivelinks-server -p 8080:8080 fivelinks-server:v1.0.0-previous
   ```
3. **Xác nhận client:** Kiểm tra `/protocol/version` để đảm bảo tương thích ngược với client app đã phát hành trên các kho ứng dụng.

---

## 8. Sổ tay Xử lý Sự cố (Incident Runbook)

| Hiện tượng | Nguyên nhân có thể | Hành động khắc phục |
|---|---|---|
| Client nhận lỗi `SERVER_BUSY` khi tạo phòng | Đã đạt ngưỡng `MAX_ROOMS` (mặc định 1000) | Kiểm tra `/metrics`. Nếu là do tăng trưởng người chơi bình thường, tăng biến `MAX_ROOMS` và cấp thêm RAM cho container. |
| Client nhận lỗi `RATE_LIMITED` | Gửi thao tác hoặc đánh bài quá nhanh | Kiểm tra log cảnh báo `Rate limit exceeded`. Client tự động hiển thị thông báo yêu cầu thử lại sau vài giây. |
| Trình duyệt web bị đóng kết nối WebSocket ngay lập tức (`VIOLATED_POLICY`) | `ALLOWED_ORIGINS` không chứa domain của web client | Thêm domain của web app vào biến `ALLOWED_ORIGINS` (vd: `ALLOWED_ORIGINS="https://myweb.com"`). |
| Trận đấu bị xử thua do đứt mạng dù kết nối lại sớm | Thời gian `RECONNECT_GRACE_SECONDS` quá ngắn | Mặc định là 60s. Trên môi trường mạng 3G/4G chập chờn, có thể tăng lên 90s hoặc 120s qua env var. |
| Container bị OOMKilled bởi hệ điều hành | Bộ nhớ RAM vượt ngưỡng gán cho Docker | Khởi chạy JVM với `-Xmx384m` hoặc tăng hạn ngạch bộ nhớ container lên ít nhất 512MB. |

