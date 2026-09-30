# Luật Chơi Five Links (Five Links Game Rules)

> Dự án: **fivelinks-cmp**  
> Vị trí: `docs/GAME-RULES.md`  
> Phiên bản: 1.1 (Cập nhật Chế độ Chiến thuật Mở rộng & Cơ chế Ẩn Thần thoại)

---

## 1. Giới thiệu tổng quan

**Five Links** là trò chơi board game chiến thuật thẻ bài kết hợp bàn cờ dành cho 2 đến 12 người chơi (chia thành 2 hoặc 3 đội đối kháng). Trò chơi lấy cảm hứng từ luật chơi kinh điển của **Sequence**, kết hợp thêm các yếu tố chiến thuật bài nâng cao nhằm gia tăng tính đấu trí và giảm sự phụ thuộc vào may rủi.

Mục tiêu tối thượng của mỗi đội là trở thành đội đầu tiên hoàn thành số lượng **chuỗi 5 quân liên tiếp (Five Links / Sequence)** theo quy định (theo hàng ngang, hàng dọc hoặc đường chéo).

---

## 2. Thành phần trò chơi

1. **Bàn cờ 10x10 (100 ô):**
   - **4 ô góc (Corners):** $(0,0), (0,9), (9,0), (9,9)$ là các ô tự do (Free/Wild Spaces). Các ô này thuộc về tất cả các đội và được tính như một quân chip sẵn có của bất kỳ ai để hoàn thành chuỗi 5.
   - **96 ô bài:** Mỗi ô in hình một lá bài tây tiêu chuẩn. Toàn bộ 48 lá bài từ $2$ đến $A$ (không bao gồm 8 quân Jack) xuất hiện **đúng 2 lần** trên bàn cờ ($48 \times 2 = 96$ ô).
2. **Bộ bài:** Gồm **2 bộ bài tây 52 lá tiêu chuẩn** được xáo trộn chung (tổng cộng 104 lá).
3. **Quân chip (Chips):** Chia theo màu sắc tương ứng với các đội:
   - 2 Đội: Đội Xanh lam (Blue) vs Đội Xanh lục (Green).
   - 3 Đội: Đội Xanh lam (Blue) vs Đội Xanh lục (Green) vs Đội Đỏ (Red).

---

## 3. Luật chơi Cổ điển (Classic Mode)

### 3.1. Chia bài và Số lượng bài trên tay (Hand Size)
Tùy thuộc vào số người chơi, mỗi người sẽ được chia một số lượng bài bí mật trên tay:
- **2 người chơi:** 7 lá/người.
- **3 hoặc 4 người chơi:** 6 lá/người.
- **6 người chơi:** 5 lá/người.
- **8 hoặc 9 người chơi:** 4 lá/người.
- **10 hoặc 12 người chơi:** 3 lá/người.

### 3.2. Tiến trình một lượt chơi
Người chơi lần lượt đi theo chiều kim đồng hồ. Trong mỗi lượt, người chơi thực hiện 3 bước:
1. **Chọn và đánh 1 lá bài** từ trên tay vào cọc bài bỏ (Discard Pile).
2. **Thực hiện hành động tương ứng** trên bàn cờ (Đặt hoặc Gỡ quân chip).
3. **Rút 1 lá bài mới** từ cọc bài úp để duy trì đủ số lượng bài trên tay.

### 3.3. Các loại nước đi cơ bản
- **Nước đi thường (`Move.Place`):** Đánh 1 lá bài thường (từ 2 đến A, trừ Jack) $\rightarrow$ Đặt 1 quân chip của đội mình vào 1 trong 2 ô tương ứng với lá bài đó trên bàn cờ (nếu ô đó còn trống).
- **Quân Jack 2 Mắt — Wild Card ($J\clubsuit, J\diamondsuit$):** Cho phép đặt 1 quân chip vào **bất kỳ ô trống nào** trên bàn cờ (trừ 4 ô góc tự do).
- **Quân Jack 1 Mắt — Anti-Wild / Remove Card ($J\spadesuit, J\heartsuit$):** Cho phép gỡ bỏ **1 quân chip bất kỳ của đối thủ** ra khỏi bàn cờ, **NGOẠI TRỪ** quân chip đó đã nằm trong một chuỗi 5 ô đã hoàn thành (Locked Sequence).
- **Đổi bài chết (`Move.SwapDeadCard`):** Khi cả 2 ô tương ứng với một lá bài trên tay bạn đều đã bị chiếm bởi các quân chip khác, lá bài đó trở thành "Bài chết" (Dead Card). Bạn có quyền thông báo đổi bài, bỏ lá bài đó đi và rút ngay 1 lá mới mà không mất lượt.

### 3.4. Điều kiện chiến thắng
- Chuỗi 5 ô liên tiếp (Five Links) gồm 5 quân chip cùng màu liền kề nhau theo hàng ngang, hàng dọc hoặc đường chéo.
- Một quân chip có thể được dùng chung cho tối đa 2 chuỗi khác nhau nếu chúng giao nhau.
- **Số chuỗi cần để thắng:**
  - 2 Đội: Cần hoàn thành **2 chuỗi 5**.
  - 3 Đội: Cần hoàn thành **1 chuỗi 5**.

---

## 4. Luật chơi Chiến thuật Mở rộng (Tactical Mode) ⭐

> **Mục tiêu:** Giảm tối đa sự may rủi khi bốc bài, biến mọi lá bài "xấu" trên tay thành cơ hội chiến thuật và tạo ra những khoảnh khắc lật kèo đỉnh cao.

Chế độ Mở rộng bổ sung 2 tầng cơ chế độc đáo:

```
┌─────────────────────────────────────────────────────────────┐
│                 TACTICAL FIVE LINKS SYSTEM                  │
├──────────────────────────────┬──────────────────────────────┤
│  TẦNG 1: ĐÚC JACK CHIẾN THUẬT │  TẦNG 2: TUYỆT CHIÊU ẨN      │
│  (Synthetic Crafting)        │  (Divine Straight Flush)     │
│                              │                              │
│  • Đôi cùng số  → Jack 2 mắt │  • 5 lá Sảnh Đồng Chất       │
│  • Sảnh 2 lá    → Jack 1 mắt │  • Thiên Phạt: Quét sạch     │
│    đồng chất                  │    toàn bộ chip đối thủ!     │
└──────────────────────────────┴──────────────────────────────┘
```

### 4.1. Tầng 1: Đúc Jack Nhân Tạo (Synthetic Jacks / Card Fusion)
Người chơi không còn bị phụ thuộc vào việc có bốc trúng Jack hay không. Bạn có thể chủ động gom bài trên tay để chế tạo ra quyền năng của Jack:

#### 1. Ghép Đôi (Pair) — Đúc thành Jack 2 Mắt (Wild Place)
- **Công thức:** Đánh ra **2 lá bài cùng số/rank** (ví dụ: $7\spadesuit + 7\diamondsuit$, hoặc $K\heartsuit + K\clubsuit$).
- **Tác dụng:** Tương đương quyền năng của Jack 2 mắt $\rightarrow$ Đặt 1 quân chip vào **bất kỳ ô trống nào** trên bàn cờ.
- **Kinh tế bài:** Bỏ 2 lá vào cọc bài hủy $\rightarrow$ Rút bù đủ **2 lá mới** từ cọc bài. Bạn chỉ được đặt **1 chip** trong lượt đó.

#### 2. Cặp Sảnh Đồng Chất (Suited Connector) — Đúc thành Jack 1 Mắt (Snipe / Remove)
- **Công thức:** Đánh ra **2 lá bài liên tiếp cùng chất** (ví dụ: $8\heartsuit + 9\heartsuit$, hoặc $Q\spadesuit + K\spadesuit$, $A\clubsuit + 2\clubsuit$).
- **Tác dụng:** Tương đương quyền năng của Jack 1 mắt $\rightarrow$ **Gỡ bỏ 1 quân chip bất kỳ của đối thủ** (chưa bị khóa trong chuỗi 5 hoàn chỉnh).
- **Kinh tế bài:** Bỏ 2 lá vào cọc bài hủy $\rightarrow$ Rút bù đủ **2 lá mới** từ cọc bài.

---

### 4.2. Tầng 2: Tuyệt Chiêu Ẩn Thần Thoại — "Thiên Phạt Hoàng Kim" (Divine Straight Flush) ⚡

Một cơ chế mang tính biểu tượng tương tự như *Thần bài Exodia* hay *Royal Flush*:

* **Điều kiện kích hoạt:**
  - Người chơi sở hữu trên tay **5 lá bài liên tiếp cùng chất (Straight Flush 5 lá)**.
  - *Ví dụ:* $5\diamondsuit - 6\diamondsuit - 7\diamondsuit - 8\diamondsuit - 9\diamondsuit$, hoặc $10\spadesuit - J\spadesuit - Q\spadesuit - K\spadesuit - A\spadesuit$.
* **Cơ chế kích hoạt (`Move.DivineWipe`):**
  - Đến lượt mình, người chơi tuyên bố kích hoạt Tuyệt chiêu ẩn và **ngửa toàn bộ 5 lá bài** cho cả bàn cờ cùng chứng kiến.
* **Hiệu lực Tối Thượng (Board Wipe):**
  1. **Quét sạch toàn bộ quân chip của đối thủ** trên toàn bộ bàn cờ 10x10!
  2. **Phá hủy toàn bộ chuỗi 5 ô đã khóa** của đối thủ (mọi thành quả trước đó của đối phương đều bị xóa sạch về 0).
  3. **Bảo toàn nguyên vẹn** tất cả các quân chip và chuỗi của bản thân/đội mình.
  4. Người chơi bỏ 5 lá bài đó vào cọc bài hủy và **rút bù đủ 5 lá bài mới** từ cọc bài.
* **Tính cân bằng:**
  - Để ôm 5 lá bài này, người chơi phải chấp nhận "tê liệt" gần như toàn bộ bài trên tay trong suốt 5–10 lượt đi, đối mặt nguy cơ thua trận trước khi kịp gom đủ. Đây là sự đánh đổi tột cùng giữa **Rủi ro cực đại** và **Phần thưởng hủy diệt**.

---

## 5. Bảng so sánh các Chế độ Chơi

| Đặc điểm | Chế độ Cổ điển (Classic) | Chế độ Chiến thuật Mở rộng (Tactical) |
| :--- | :--- | :--- |
| **Quy tắc cơ bản** | Luật chuẩn Sequence | Kế thừa 100% luật chuẩn Sequence |
| **Cách có Jack** | Chỉ trông chờ bốc từ cọc bài | Bốc tự nhiên HOẶC Tự đúc bằng Đôi/Sảnh 2 lá |
| **Tỷ lệ bài rác** | Cao (dễ bị kẹt bài khi bàn cờ bị chiếm) | Cực thấp (bài xấu ghép đôi vẫn thành Jack) |
| **Khả năng lật kèo** | Trung bình (phụ thuộc vào Jack 1 mắt) | Cao & Bùng nổ (có Đúc Jack và Tuyệt chiêu Thiên phạt) |
| **Cơ chế ẩn** | Không có | **Thiên Phạt Hoàng Kim (Clear toàn bộ chip đối thủ)** |
| **Độ sâu chiến thuật** | 6.5 / 10 | 9.5 / 10 |
