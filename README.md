# Five Links — Compose Multiplatform (CMP)

[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.11.1-purple.svg)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![Ktor](https://img.shields.io/badge/Ktor-3.5.0-orange.svg?logo=ktor)](https://ktor.io)
[![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-1.22.0-blue.svg)](https://onnxruntime.ai/)

**Five Links** là trò chơi board game chiến thuật đa nền tảng kết hợp giữa bài tây 52 lá và bàn cờ 10x10 (dựa trên board game kinh điển **Sequence**), được xây dựng bằng **Kotlin Multiplatform** và **Compose Multiplatform** nhắm tới Android, iOS, Web (Wasm / JS) và Backend Ktor.

Dự án tích hợp hệ thống trí tuệ nhân tạo **AI (AlphaZero-like Hybrid)** sử dụng Monte Carlo Tree Search (ISMCTS / Determinization) kết hợp mô hình học sâu **ONNX Runtime** và Heuristic Evaluator.

---

## 📁 Cấu trúc Dự án

```
fivelinks-cmp/
├── app/                      # Ứng dụng giao diện người dùng (Compose Multiplatform)
│   ├── shared/               # UI & Logic dùng chung (Common Compose UI cho Android, iOS, Web)
│   ├── androidApp/           # Entry point cho ứng dụng Android
│   ├── iosApp/               # Entry point cho ứng dụng iOS (SwiftUI wrapper)
│   └── webApp/               # Entry point cho ứng dụng Web (WasmJs & Js)
├── core/                     # Lõi dùng chung cho toàn bộ hệ thống (KMP)
│   ├── domain/               # Luật chơi, bàn cờ 10x10, quản lý ván bài, xác định chuỗi thắng
│   ├── ai/                   # AI Engine: Tactical, MCTS Determinization, Heuristic, ONNX Inference
│   └── protocol/             # Giao thức truyền tin Client-Server định dạng JSON
├── server/                   # Backend Ktor Server (Netty) phục vụ API tạo ván và tính toán AI
├── ml/                       # Machine Learning: Huấn luyện PyTorch, xuất mô hình ONNX
└── docs/                     # Tài liệu thiết kế hệ thống, luật chơi và AI
```

---

## 📖 Tài liệu Tham khảo

- [📜 Luật chơi Five Links (Cổ điển & Chiến thuật Mở rộng)](./docs/GAME-RULES.md)
- [🤖 Tài liệu Hệ thống AI & Kiến trúc AlphaZero](./docs/AI-DOCUMENTATION.md)
- [📘 Hướng dẫn Triển khai & Tự học AI Core](./docs/AI-GUIDE.md)
- [🌐 Hợp đồng Giao tiếp & Giao thức Mạng](./docs/protocol.md)

---

## 🚀 Hướng dẫn Cài đặt & Chạy Dự án

### Yêu cầu Tiên quyết (Prerequisites)
- **JDK 17 hoặc JDK 21** (Khuyến nghị JDK 21).
- **Android Studio** (Ladybug / Koala hoặc mới hơn) kèm Android SDK 36.
- **Xcode** (nếu muốn build & chạy iOS trên macOS).
- **Node.js & Trình duyệt hiện đại** (hỗ trợ WebAssembly GC nếu chạy Wasm).

---

### 1. Chạy Backend Server (Ktor)

Khởi động Ktor HTTP server tại `http://localhost:8080`:

```bash
# Chạy server
./gradlew :server:run
```

Kiểm tra server đang chạy:
- Truy cập trình duyệt hoặc curl: `http://localhost:8080/health` (Trả về: `Heath OK`)
- Kiểm tra phiên bản protocol: `http://localhost:8080/protocol/version`
- API nước đi AI: `POST http://localhost:8080/ai/move`

---

### 2. Chạy Ứng dụng Web (Web Browser)

Ứng dụng Web hỗ trợ cả hai mục tiêu biên dịch:

```bash
# Mục tiêu Wasm (Khuyên dùng: Hiệu năng cao, chạy trên trình duyệt hỗ trợ Wasm GC như Chrome/Firefox mới)
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun

# Mục tiêu JavaScript thuần (Tương thích với các trình duyệt cũ hơn)
./gradlew :app:webApp:jsBrowserDevelopmentRun
```

---

### 3. Chạy Ứng dụng Android

```bash
# Build gói APK Debug
./gradlew :app:androidApp:assembleDebug

# Cài đặt trực tiếp lên thiết bị/giả lập Android đang kết nối
./gradlew :app:androidApp:installDebug
```
*Hoặc mở thư mục dự án bằng Android Studio, chọn run configuration `app.androidApp` và bấm nút **Run ▶**.*

---

### 4. Chạy Ứng dụng iOS

Mở thư mục `app/iosApp` bằng Xcode trên macOS:
1. Mở file `app/iosApp/iosApp.xcodeproj`.
2. Chọn máy ảo iOS Simulator (ví dụ iPhone 16 Pro).
3. Bấm **Cmd + R** để biên dịch và khởi chạy.

---

## 🧪 Chạy Kiểm thử (Running Tests)

Dự án đi kèm bộ test toàn diện từ Domain, AI Engine, Server đến Serialization:

```bash
# Chạy toàn bộ Unit Test cho module Core (trên môi trường JVM)
./gradlew :core:jvmTest

# Chạy Unit Test kiểm tra Server Ktor
./gradlew :server:test

# Chạy riêng bộ kiểm thử AI Regression (Đảm bảo bot luôn bắt nước thắng/chặn)
./gradlew :core:jvmTest --tests "com.karasuma.fivelinks.fivelinks_cmp.ai.AiRegressionTest"

# Chạy đấu trường benchmark AI (Arena: Hybrid vs Heuristic)
./gradlew :core:jvmTest --tests "com.karasuma.fivelinks.fivelinks_cmp.ai.AiBenchmarkArenaTest"
```

---

## 🧠 Huấn luyện & Phát triển AI (Machine Learning)

Hệ thống AI sử dụng dữ liệu tự chơi (Self-play) để huấn luyện mạng nơ-ron đánh giá nước đi:

1. **Sinh dữ liệu Self-Play từ Engine Kotlin:**
   ```bash
   ./gradlew :core:jvmTest --tests "com.karasuma.fivelinks.fivelinks_cmp.ai.SelfPlayRunnerTest" -Dselfplay.run=true -Dselfplay.games=1000
   ```
   Dữ liệu được lưu tại `ml/data/selfplay_dataset.jsonl`.

2. **Huấn luyện mô hình PyTorch:**
   ```bash
   cd ml/train
   python train.py
   ```

3. **Xuất mô hình sang ONNX:**
   ```bash
   python export_onnx.py
   ```
   File `model_v1.onnx` sau đó được đồng bộ vào thư mục `core/src/jvmMain/resources/models/` và `core/src/androidMain/resources/models/`.