# AI-GUIDE — Triển khai lõi AI Five Links (học tập & tự code)

> Dự án: **fivelinks-cmp**  
> Mục đích: hướng dẫn **tự tay triển khai** từ đầu đến cuối (AlphaZero tối giản: Tactical → ISMCTS → Heuristic/Neural + ONNX).  
> Tài liệu này **không thay thế** source hiện tại — bạn thêm/sửa code theo từng phase khi học.  
> Cập nhật: 23/07/2026 (Phase 2 tests)  
> Đồng bộ với: BE HTTP (`/game/new`, `/ai/move`), Facade + Tactical, `GameState` `@Serializable`, package `model/`

---

## Mục lục

1. [Tổng quan & mục tiêu](#1-tổng-quan--mục-tiêu)
2. [Hiện trạng repo](#2-hiện-trạng-repo)
3. [Kiến trúc đích](#3-kiến-trúc-đích)
4. [Cấu trúc thư mục đề xuất](#4-cấu-trúc-thư-mục-đề-xuất)
5. [Phase 0 — Chuẩn bị domain & harness](#phase-0--chuẩn-bị-domain--harness)
6. [Phase 1 — Facade + DifficultyConfig](#phase-1--facade--difficultyconfig)
7. [Phase 2 — Tactical Engine](#phase-2--tactical-engine)
8. [Phase 3 — Search (ISMCTS / root-parallel) + heuristic leaf](#phase-3--search-ismcts--root-parallel--heuristic-leaf)
9. [Phase 4 — Evaluation Engine thống nhất](#phase-4--evaluation-engine-thống-nhất)
10. [Phase 5 — ActionCodec + StateEncoder](#phase-5--actioncodec--stateencoder)
11. [Phase 6 — Neural + training + ONNX export](#phase-6--neural--training--onnx-export)
12. [Phase 7 — ONNX Runtime trên KMP](#phase-7--onnx-runtime-trên-kmp)
13. [Phase 8 — Tune difficulty & UX](#phase-8--tune-difficulty--ux)
14. [Phase 9 — Tests, arena, docs](#phase-9--tests-arena-docs)
15. [Checklist Definition of Done](#15-checklist-definition-of-done)
16. [Phụ lục](#16-phụ-lục)
17. [Changelog guide](#17-changelog-guide)

---

## 1. Tổng quan & mục tiêu

### 1.1 Bạn sẽ xây gì?

Bot AI cho game kiểu **Sequence / Five Links**:

| Lớp | Vai trò |
|-----|---------|
| **AiFacadeService** | Caller (UI **hoặc** HTTP route) chỉ gọi 1 API (`AiService`) |
| **DifficultyConfig** | Budget sim / time / temperature / mode |
| **TacticalEngine** | Nước forced (win ngay, chặn open-4) |
| **SearchEngine** | MCTS trên thế giới đã determinize (bài ẩn) |
| **EvaluationEngine** | Value + policy prior (Heuristic và/hoặc Neural) |
| **ONNX Runtime** | Inference model nhẹ trên Android / iOS / **JVM server** |
| **HTTP `/ai/move`** | (đã có) Client gửi `GameState` lên server nhờ bot chọn nước |

### 1.2 Vì sao không copy AlphaZero “thuần”?

Game có:

- Bài tay đối thủ **ẩn**
- Rút bài sau mỗi nước (**ngẫu nhiên**)
- Jack wild / remove → **branching lớn**

→ Dùng **determinization + MCTS** (root-parallel hoặc ISMCTS), không giả định thông tin hoàn hảo như Go/Chess.

### 1.3 Nguyên tắc học tập

1. Mỗi phase **chạy được** (bot chọn được nước hợp lệ).
2. Benchmark trước khi train NN.
3. `commonMain` = logic thuần Kotlin; ONNX = `expect`/`actual` hoặc module platform.
4. Giữ `HeuristicEvaluator` làm **baseline + fallback** mãi.
5. Không phá public API `domain` trừ khi Phase 0 phát hiện bug bắt buộc.

### 1.4 Timeline gợi ý

| Tuần | Phase | Milestone |
|------|-------|-----------|
| 1 | 0–2 | Facade + tactical |
| 2 | 3 | Bot search mạnh hơn 1-ply |
| 3 | 4–5 | Eval interface + encoder |
| 4–5 | 6 | Train + export ONNX |
| 5–6 | 7–8 | ORT + hybrid + tune |
| Song song | 9 | Tests / arena |

**MVP “đã xịn”:** kết thúc Phase 3. Neural là lớp tăng sức mạnh tiếp theo.

---

## 2. Hiện trạng repo

### 2.1 Package AI hiện có

```
core/src/commonMain/kotlin/.../ai/
  AiService.kt
  HeuristicEvaluator.kt / HeuristicWeights.kt
  SoftmaxPicker.kt
  DifficultyConfig.kt          # Phase 1 ✅
  AiFacadeService.kt           # Phase 1 ✅ (wire tactical)
  tactical/
    ThreatDetector.kt          # Phase 2 ✅
    TacticalEngine.kt          # Phase 2 ✅  (API: findForceMove)

core/src/commonTest/kotlin/.../
  domain/Phase0DomainTest.kt
  ai/AiArenaTest.kt            # Phase 0.3 ✅
  ai/tactical/TacticalEngineTest.kt  # Phase 2 ✅

server/.../AiHttpArenaTest.kt  # Phase 0.4 ✅
```

**Đã xong Phase 0–2** (domain harden + Facade + Tactical + tests).  
**Chưa có:** Search/MCTS, EvaluationEngine, Encoder, Neural/ONNX.

### 2.2 Contract `AiService` hiện tại

```kotlin
// AiService.kt (hiện có)
@Serializable
enum class Difficulty(val topK: Int, val temperature: Double) {
    EASY(5, 0.9),
    MEDIUM(3, 0.35),
    HARD(1, 0.0),
}

interface AiService {
    suspend fun chooseMove(state: GameState, playerId: PlayerId, difficulty: Difficulty): Move
}
```

`HeuristicEvaluator` chấm từng nước bằng feature (complete sequence, open-4, block, center, waste jack…) rồi:

- HARD → argmax
- EASY/MEDIUM → softmax trên top-K

### 2.3 Domain bạn sẽ gọi lại

| API | Dùng cho |
|-----|----------|
| `GameEngine.legalMoves(state, playerId)` | Expand MCTS / mask policy |
| `GameEngine.applyMove(state, move)` | Simulate |
| `Move.Place / Remove / SwapDeadCard` | Action space |
| `BoardPosition` 10×10, `flatIndex` | Encode board |
| `ChipSequence(team, positions)` | Locked cells / encoder channel |
| `lineStatus` / `BoardLines` | Threat / open-4 |
| `WinDetector` / `SequenceDetector` | Terminal / tactical |
| `Player(id, name, team, isAi)` | `PlayerId = String`; flag AI |
| `GameState` (`@Serializable`) | Gửi qua HTTP / self-play export |

### 2.4 HTTP API & model DTO (đã có trên BE)

Package `core/.../model/`:

| File | Vai trò |
|------|---------|
| `AiMoveRequest` | `gameState` + `playerId` + `difficulty` (default EASY) |
| `AiMoveResponse` | wrap `move: Move` (nên dùng khi respond) |
| `NewGameRequest` | `playerCount`, `teamCount`, `seed` |
| `ErrorResponse` | `code: ErrorCodes`, `message`, `details?` |

`ErrorCodes` hiện có: `PROTOCOL_VERSION_MISMATCH`, `INTERNAL_ERROR`, **`INVALID_REQUEST`**.

Server routes (`server/.../route/`):

| Method | Path | Hành vi hiện tại |
|--------|------|------------------|
| `POST` | `/game/new` | `GameConfig.forPlayer` → tạo `Player(id="p$i", …, isAi=true)` → `GameEngine.initialize` → `201` + `GameState` |
| `POST` | `/ai/move` | `receive<AiMoveRequest>` → `HeuristicEvaluator().chooseMove(...)` → `200` + **body là `Move` thô** |

Điểm cần nhớ khi học / refactor:

1. **Mỗi request đang `new HeuristicEvaluator()`** — sau Phase 1 hãy inject **một** `AiFacadeService` (singleton / DI) vào `aiRoute`.
2. Có `AiMoveResponse` nhưng route đang `respond(move)` — nên thống nhất:

```kotlin
call.respond(HttpStatusCode.OK, AiMoveResponse(move))
```

3. `GameState` đã `@Serializable` → client có thể round-trip state qua JSON (`ProtocolJson`) rất thuận cho arena HTTP và self-play dump.
4. `/game/new` hiện gán **mọi** player `isAi = true` (tiện test BE); client thật có thể trộn human/AI sau.

Ví dụ gọi nhanh (sau `./gradlew :server:run`, port `8080`):

```http
POST /game/new
Content-Type: application/json

{ "playerCount": 2, "teamCount": 2, "seed": 42 }

POST /ai/move
Content-Type: application/json

{
  "gameState": { /* full GameState từ /game/new */ },
  "playerId": "p0",
  "difficulty": "HARD"
}
```

### 2.5 Phase 0 domain — **đã sửa trong repo**

| Mục | Trạng thái |
|-----|------------|
| `applyPlace` dùng `move.playerId` | ✅ |
| `twoShuffleDeck` gán kết quả `shuffled(Random(seed))` | ✅ |
| `legalMoves` Remove bỏ ô trống | ✅ |
| Tests | `EngineTest`, `AiArenaTest`, `AiHttpArenaTest` |
---

## 3. Kiến trúc đích

Hai lối vào cùng một lõi (tránh fork logic):

```
[Compose UI / client]          [HTTP client]
        │                            │
        │                     POST /ai/move
        │                            │
        └──────────┬─────────────────┘
                   ▼
            AiFacadeService  (implements AiService)
       ├─ DifficultyConfig          // budget, temperature, evalMode
       ├─ TacticalEngine            // forced / near-forced → return sớm
       └─ SearchEngine              // root-parallel MCTS / ISMCTS
              ├─ Determinizer       // sample hands ẩn + deck
              ├─ policy prior  ← EvaluationEngine
              └─ leaf value    ← EvaluationEngine
                                   ├─ HeuristicModel
                                   └─ NeuralModel → ONNX Runtime (ưu tiên JVM server + mobile)
                                         └─ fallback Heuristic
```

**Hiện tại:** `/ai/move` inject **`AiFacadeService`** (tactical → heuristic). Search/MCTS chưa có (Phase 3).

**Difficulty không phải engine ngang hàng** — chỉ là config truyền vào Tactical + Search.

**Tactical chạy trước Search** (pipeline), không parallel rồi cùng đổ vào Evaluation.

**Evaluation có 2 cổng:**

- `value(state) ∈ [-1, 1]`
- `priors(state, legalMoves)` — dùng trong PUCT

**Gợi ý vận hành:** search nặng (HARD + nhiều sim / ONNX) chạy trên **server JVM**; client Wasm/JS gọi HTTP hoặc fallback heuristic local.

---

## 4. Cấu trúc thư mục đề xuất

Bạn tự tạo dần theo phase (không bắt buộc tạo hết từ đầu):

```
core/src/commonMain/kotlin/.../ai/
  AiService.kt                 # giữ / mở rộng nhẹ
  HeuristicEvaluator.kt        # giữ làm baseline
  HeuristicWeights.kt

  AiFacadeService.kt           # Phase 1 ✅
  DifficultyConfig.kt          # Phase 1 ✅
  SoftmaxPicker.kt

  tactical/
    TacticalEngine.kt          # Phase 2 ✅  findForceMove
    ThreatDetector.kt          # Phase 2 ✅

  search/
    SearchEngine.kt            # Phase 3
    MctsNode.kt
    MctsConfig.kt
    Determinizer.kt
    MoveKey.kt

  eval/
    EvaluationEngine.kt        # Phase 4
    PositionEvaluator.kt
    PolicyPrior.kt
    HeuristicPositionEvaluator.kt
    HeuristicPolicyPrior.kt
    HybridEvaluationEngine.kt  # Phase 7

  encode/
    ActionCodec.kt             # Phase 5
    StateEncoder.kt
    EncoderSpec.kt

  inference/
    NeuralInference.kt         # Phase 7 (expect)
    NeuralOutput.kt

core/src/androidMain/.../inference/OnnxNeuralInference.android.kt
core/src/jvmMain/.../inference/OnnxNeuralInference.jvm.kt
core/src/iosMain/.../inference/OnnxNeuralInference.ios.kt
core/src/jsMain/.../inference/UnsupportedNeuralInference.js.kt
core/src/wasmJsMain/.../inference/UnsupportedNeuralInference.wasm.kt

# Đã có — DTO + HTTP (đừng nhân bản contract)
core/src/commonMain/kotlin/.../model/
  AiMoveRequest.kt
  AiMoveResponse.kt
  NewGameRequest.kt
  ErrorResponse.kt

server/src/main/kotlin/.../route/
  AiRoute.kt      # POST /ai/move  → inject AiService (Phase 1+)
  GameRoute.kt    # POST /game/new

# Train offline (repo con hoặc thư mục riêng)
ml/
  train/
    model.py
    dataset.py
    selfplay_import.py
    export_onnx.py
  specs/
    encoder_spec.json
```

Package AI giữ nguyên:

`com.karasuma.fivelinks.fivelinks_cmp.ai`

---

## Phase 0 — Chuẩn bị domain & harness

### Mục tiêu

Engine đủ deterministic + có cách đo sức mạnh AI trước khi viết search.

### 0.1 Checklist domain

- [x] `applyPlace` dùng `move.playerId`
- [x] `twoShuffleDeck` thật sự shuffle theo seed
- [x] `applyMove` → validation → place/remove/swap → draw → sequence → win → `advanceTurn`
- [x] `legalMoves` khớp validator (Remove không target ô trống)
- [x] State là data class immutable (copy) — thuận lợi cho tree search
- [x] `GameState` round-trip JSON qua `ProtocolJson` (`EngineTest`)
- [x] HTTP arena: `AiHttpArenaTest` (in-process vs `/game/new` + `/ai/move`)

### 0.2 Softmax helper dùng chung

Tách khỏi `HeuristicEvaluator` để Facade/Search tái sử dụng:

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import kotlin.math.exp
import kotlin.random.Random

object SoftmaxPicker {
    fun pick(
        scored: List<Pair<Move, Double>>,
        temperature: Double,
        random: Random = Random.Default,
        scoreScale: Double = 1_000.0,
    ): Move {
        require(scored.isNotEmpty())
        if (scored.size == 1 || temperature <= 0.0) {
            return scored.maxBy { it.second }.first
        }
        val maxScore = scored.maxOf { it.second }
        val weights = scored.map { (_, s) ->
            exp((s - maxScore) / (scoreScale * temperature))
        }
        val total = weights.sum()
        var r = random.nextDouble() * total
        for (i in scored.indices) {
            r -= weights[i]
            if (r <= 0.0) return scored[i].first
        }
        return scored.last().first
    }
}
```

### 0.3 Arena tối thiểu (JVM test)

```kotlin
// core/src/jvmTest/.../AiArenaTest.kt (ví dụ)
class AiArenaTest {
    @Test
    fun heuristicSelfPlay_runsWithoutCrash() {
        val ai = HeuristicEvaluator(random = Random(42))
        var winsRed = 0
        var winsBlue = 0
        repeat(20) { gameIndex ->
            val config = GameConfig.soloVsAi(seed = 1000L + gameIndex)
            // PlayerId = String; khớp kiểu /game/new dùng id "p0", "p1", ...
            val players = listOf(
                Player(id = "p0", name = "Human", team = Team.RED, isAi = false),
                Player(id = "p1", name = "AI", team = Team.BLUE, isAi = true),
            )
            var state = GameEngine.initialize(config, players)
            var guard = 0
            while (!state.isGameOver && guard++ < 500) {
                val pid = state.currentPlayer.id
                // runBlocking vì chooseMove là suspend
                val move = kotlinx.coroutines.runBlocking {
                    ai.chooseMove(state, pid, Difficulty.HARD)
                }
                state = GameEngine.applyMove(state, move).getOrThrow()
            }
            when (state.winner) {
                Team.RED -> winsRed++
                Team.BLUE -> winsBlue++
                else -> Unit
            }
        }
        println("RED=$winsRed BLUE=$winsBlue")
    }
}
```

### 0.4 Arena qua HTTP (optional, sau khi server chạy)

1. `POST /game/new` với seed cố định → lưu `GameState`.
2. Loop: `POST /ai/move` với `playerId = state.currentPlayer.id` → áp `move` bằng `GameEngine.applyMove` (client hoặc test JVM).
3. So sánh winrate với arena in-process — phải gần nhau nếu cùng seed + cùng AI (sau khi đã fix shuffle deck).

### Done Phase 0

- Arena chạy được với `HeuristicEvaluator`
- Ghi lại baseline (winrate / số ván hòa / độ dài ván trung bình)
- (Khuyến nghị) đã xác nhận `/ai/move` trả nước hợp lệ trên state từ `/game/new`

---

## Phase 1 — Facade + DifficultyConfig

### Mục tiêu

Caller (UI **và** `POST /ai/move`) chỉ biết `AiService`; bên trong chuyển dần sang tactical + search mà **không đổi chữ ký**.

### 1.1 `DifficultyConfig.kt`

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai

enum class EvalMode {
    HeuristicOnly,
    Hybrid,      // NN + heuristic fallback (Phase 7)
    NeuralFirst, // ưu tiên NN khi có
}

data class DifficultyConfig(
    val maxSimulations: Int,
    val timeBudgetMs: Long,
    val temperature: Double,
    val topK: Int,
    val useTacticalForced: Boolean,
    val determinizations: Int,
    val evalMode: EvalMode,
    val cPuct: Double = 1.5,
) {
    companion object {
        fun from(difficulty: Difficulty): DifficultyConfig = when (difficulty) {
            Difficulty.EASY -> DifficultyConfig(
                maxSimulations = 80,
                timeBudgetMs = 50,
                temperature = 0.9,
                topK = 5,
                useTacticalForced = true,
                determinizations = 2,
                evalMode = EvalMode.HeuristicOnly,
            )
            Difficulty.MEDIUM -> DifficultyConfig(
                maxSimulations = 400,
                timeBudgetMs = 150,
                temperature = 0.35,
                topK = 3,
                useTacticalForced = true,
                determinizations = 4,
                evalMode = EvalMode.HeuristicOnly,
            )
            Difficulty.HARD -> DifficultyConfig(
                maxSimulations = 2000,
                timeBudgetMs = 600,
                temperature = 0.0,
                topK = 1,
                useTacticalForced = true,
                determinizations = 8,
                evalMode = EvalMode.HeuristicOnly, // đổi Hybrid ở Phase 7
            )
        }
    }
}
```

Giữ enum `Difficulty` cũ để UI không đổi; map nội bộ sang config.

### 1.2 `AiFacadeService.kt` (skeleton Phase 1)

Ban đầu Facade **ủy quyền** cho heuristic — architecture đúng chỗ, hành vi chưa đổi:

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId

class AiFacadeService(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    // Phase 2+: private val tactical: TacticalEngine = TacticalEngine(heuristic),
    // Phase 3+: private val search: SearchEngine = ...,
) : AiService {

    override suspend fun chooseMove(
        state: GameState,
        playerId: PlayerId,
        difficulty: Difficulty,
    ): Move {
        val config = DifficultyConfig.from(difficulty)
        val legal = GameEngine.legalMoves(state, playerId)
        require(legal.isNotEmpty()) { "No legal moves for $playerId" }

        // Phase 2:
        // if (config.useTacticalForced) {
        //     tactical.findForcedMove(state, playerId)?.let { return it }
        // }

        // Phase 3:
        // val visitCounts = search.search(state, playerId, config)
        // return selectByVisits(visitCounts, config)

        // Phase 1 fallback = hành vi cũ
        return heuristic.chooseMove(state, playerId, difficulty)
    }
}
```

### 1.3 Cách gắn vào app / server (khi bạn sẵn sàng)

**In-process (Compose / test):** chỗ đang tạo `HeuristicEvaluator()` → đổi thành `AiFacadeService()`.

**HTTP (`AiRoute.kt` hiện tại):**

```kotlin
fun Route.aiRoute(ai: AiService = AiFacadeService()) {
    route("/ai") {
        post("/move") {
            val request = call.receive<AiMoveRequest>()
            val move = runCatching {
                ai.chooseMove(request.gameState, request.playerId, request.difficulty)
            }.getOrElse {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ErrorResponse(ErrorCodes.INVALID_REQUEST, "Invalid request: ${it.message}")
                )
                return@post
            }
            // Nên wrap DTO (đã có sẵn trong model/)
            call.respond(HttpStatusCode.OK, AiMoveResponse(move))
        }
    }
}
```

Trong `Application.module()`:

```kotlin
val aiService: AiService = AiFacadeService() // một instance dùng lại
routing {
    aiRoute(aiService)
    gameRoute()
    // ...
}
```

Vẫn implement `AiService` → client HTTP không cần biết bên trong là heuristic hay MCTS.

### Done Phase 1

- [ ] `DifficultyConfig.from` có unit test mapping
- [ ] Facade + heuristic cho cùng kết quả (cùng seed Random) trên vài state cố định

---

## Phase 2 — Tactical Engine

### Mục tiêu

Trả lời ngay các nước **bắt buộc / gần bắt buộc**, giảm blunder và giảm tải search.

### Trạng thái triển khai: ✅ hoàn thành

| File | API thực tế trong repo |
|------|------------------------|
| `tactical/ThreatDetector.kt` | `countOpenFours`, `openFoursEmptyCells` |
| `tactical/TacticalEngine.kt` | `findForceMove`, `orderMoves` |
| `AiFacadeService` | nếu `useTacticalForced` → `findForceMove` rồi mới heuristic |
| Tests | `commonTest/.../tactical/TacticalEngineTest.kt` |

> Tên method trong code là **`findForceMove`** / **`openFoursEmptyCells`** (khác nhẹ so với bản nháp guide cũ `findForcedMove` / `openFourEmptyCells`). Khi đọc code, bám tên trong repo.

### 2.1 `ThreatDetector` (đã có)

Dùng `BoardLines` + `lineStatus` để đếm open-4 và lấy ô trống hoàn thành đe dọa.

### 2.2 `TacticalEngine` — thứ tự ưu tiên (đã có)

1. Win ngay (`winner == myTeam` hoặc đủ `sequenceToWin`)  
2. Chặn open-4 đối thủ (Place vào danger cell, hoặc Remove giảm threat)  
3. Tạo open-4 của mình  
4. `null` → Facade fallback heuristic  

`dangerCells` gộp bằng:

```kotlin
oppTeams.flatMap { ThreatDetector.openFoursEmptyCells(state, it) }.toSet()
```

### 2.3 Gắn Facade (đã có)

```kotlin
if (config.useTacticalForced) {
    tactical.findForceMove(state, playerId)?.let { return it }
}
return heuristic.chooseMove(state, playerId, difficulty)
```

### 2.4 Test fixtures (đã có — học cách dựng state tay)

File: `core/src/commonTest/.../ai/tactical/TacticalEngineTest.kt`

Ý tưởng: **không** chơi random từ `initialize`. Dựng `GameState` với chips + hand cố định.

Fixture chuẩn: line row 0 cols 1..5 (`6D`…`10D`). Dùng **3 đội** (`GameConfig.forPlayer(3, 3, …)`) để `sequenceToWin = 1`.

| Test | Ý nghĩa |
|------|---------|
| `mustCompleteSequenceWhenAvailable` | 4 chip RED + hand có bài ô trống → Place thắng |
| `mustBlockOpponentOpenFour` | 4 chip BLUE open-4 → RED Place đúng danger cell |
| `facade_usesTacticalForcedMove` | Facade EASY vẫn trả forced win |

Chạy:

```bash
./gradlew :core:jvmTest --tests "*TacticalEngineTest"
```

### Done Phase 2

- [x] Win/block fixtures pass  
- [x] Facade: tactical → heuristic fallback  
- [x] `ThreatDetector` + `TacticalEngine` trong `commonMain`  
- [ ] (Sau Phase 3) cảm giác HARD ít bỏ chặn hơn khi có search — tactical đã cover forced  

---
## Phase 3 — Search (ISMCTS / root-parallel) + heuristic leaf

### Mục tiêu

Nhìn trước nhiều nước dưới **thông tin ẩn**, leaf dùng heuristic. Đây là milestone ROI cao nhất.

### 3.1 Chiến lược đơn giản trước: Root-parallel determinization

```
Mỗi lần chooseMove:
  visits = Map<MoveKey, Int>()
  lặp d = 1..determinizations:
      world = Determinizer.sample(state, playerId, rng)
      rootVisits = MCTS.run(world, sims = maxSimulations / determinizations, ...)
      cộng dồn rootVisits vào visits theo MoveKey
  chọn Move theo visits (+ temperature)
```

ISMCTS “đúng sách” gắn node theo information set — làm sau khi bản root-parallel ổn.

### 3.2 `MoveKey.kt`

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.BoardPosition
import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move

sealed interface MoveKey {
    data class Place(val card: Card, val position: BoardPosition) : MoveKey
    data class Remove(val card: Card, val position: BoardPosition) : MoveKey
    data class Swap(val card: Card) : MoveKey

    companion object {
        fun from(move: Move): MoveKey = when (move) {
            is Move.Place -> Place(move.card, move.position)
            is Move.Remove -> Remove(move.card, move.position)
            is Move.SwapDeadCard -> Swap(move.card)
        }
    }
}

fun MoveKey.toMove(playerId: com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId): Move = when (this) {
    is MoveKey.Place -> Move.Place(playerId, card, position)
    is MoveKey.Remove -> Move.Remove(playerId, card, position)
    is MoveKey.Swap -> Move.SwapDeadCard(playerId, card)
}
```

### 3.3 `Determinizer.kt`

Ý tưởng: từ góc nhìn `playerId`, biết hand mình; phần bài “chưa thấy” = deck hiện tại + hands đối thủ. Sample lại hands đối thủ (đúng kích thước) + phần còn lại là deck.

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.Card
import com.karasuma.fivelinks.fivelinks_cmp.domain.Deck
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Hand
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import kotlin.random.Random

class Determinizer(private val random: Random = Random.Default) {

    fun sample(state: GameState, viewerId: PlayerId): GameState {
        val viewer = state.players.first { it.id == viewerId }
        val unknown = mutableListOf<Card>()
        unknown += state.deck.cards
        for (p in state.players) {
            if (p.id != viewerId) unknown += state.handOf(p).cards
        }
        unknown.shuffle(random)

        val newHands = state.hands.toMutableMap()
        var idx = 0
        for (p in state.players) {
            if (p.id == viewerId) continue
            val n = state.handOf(p).size
            val dealt = unknown.subList(idx, idx + n).toList()
            idx += n
            newHands[p.id] = Hand(dealt)
        }
        val rest = unknown.subList(idx, unknown.size).toList()
        return state.copy(
            hands = newHands,
            deck = Deck(rest),
        )
    }
}
```

> Lưu ý: với 2 bộ bài giống nhau, “card identity” không phân biệt bản sao — đủ cho học tập. Sau này có thể tinh chỉnh theo discard đã thấy.

### 3.4 `MctsNode.kt` + PUCT

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import kotlin.math.ln
import kotlin.math.sqrt

class MctsNode(
    val parent: MctsNode? = null,
    val moveFromParent: Move? = null,
) {
    var visitCount: Int = 0
    var totalValue: Double = 0.0 // tổng value từ góc nhìn root team
    val children: MutableMap<MoveKey, MctsNode> = mutableMapOf()
    var prior: Double = 1.0
    var untried: MutableList<Move> = mutableListOf()
    var expanded: Boolean = false

    val q: Double get() = if (visitCount == 0) 0.0 else totalValue / visitCount

    fun puctScore(cPuct: Double, parentVisits: Int): Double {
        val u = cPuct * prior * sqrt(parentVisits.toDouble().coerceAtLeast(1.0)) / (1 + visitCount)
        return q + u
    }
}

fun selectChild(node: MctsNode, cPuct: Double): MctsNode {
    val parentVisits = node.visitCount
    return node.children.values.maxBy { it.puctScore(cPuct, parentVisits) }
}
```

### 3.5 Vòng MCTS (heuristic leaf)

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.search

import com.karasuma.fivelinks.fivelinks_cmp.ai.HeuristicEvaluator
import com.karasuma.fivelinks.fivelinks_cmp.ai.DifficultyConfig
import com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.TacticalEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameEngine
import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team
import kotlin.math.tanh

class SearchEngine(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
    private val tactical: TacticalEngine = TacticalEngine(heuristic),
    private val determinizer: Determinizer = Determinizer(),
) {
    fun search(
        realState: GameState,
        playerId: PlayerId,
        config: DifficultyConfig,
    ): Map<MoveKey, Int> {
        val aggregate = mutableMapOf<MoveKey, Int>()
        val simsPerWorld = (config.maxSimulations / config.determinizations.coerceAtLeast(1))
            .coerceAtLeast(1)
        val deadline = TimeSource.Monotonic.markNow() + config.timeBudgetMs.milliseconds
        // Dùng kotlin.time; hoặc System.currentTimeMillis() trên JVM

        repeat(config.determinizations) {
            if (/* elapsed > budget */) return@repeat
            val world = determinizer.sample(realState, playerId)
            val rootVisits = runMcts(world, playerId, simsPerWorld, config)
            for ((key, n) in rootVisits) {
                aggregate[key] = (aggregate[key] ?: 0) + n
            }
        }
        return aggregate
    }

    private fun runMcts(
        rootState: GameState,
        rootPlayerId: PlayerId,
        simulations: Int,
        config: DifficultyConfig,
    ): Map<MoveKey, Int> {
        val rootTeam = rootState.players.first { it.id == rootPlayerId }.team
        val root = MctsNode()
        root.untried = GameEngine.legalMoves(rootState, rootState.currentPlayer.id).toMutableList()
        root.expanded = true

        repeat(simulations) {
            var state = rootState
            var node = root
            val path = mutableListOf(node)

            // SELECT
            while (node.children.isNotEmpty() && node.untried.isEmpty() && !state.isGameOver) {
                node = selectChild(node, config.cPuct)
                val move = node.moveFromParent!!
                state = GameEngine.applyMove(state, move).getOrThrow()
                path += node
            }

            // EXPAND
            if (!state.isGameOver) {
                val toPlay = state.currentPlayer.id
                if (!node.expanded) {
                    node.untried = GameEngine.legalMoves(state, toPlay).toMutableList()
                    node.expanded = true
                }
                if (node.untried.isNotEmpty()) {
                    // move ordering
                    node.untried = tactical.orderMoves(state, toPlay, node.untried).toMutableList()
                    val move = node.untried.removeAt(0)
                    val key = MoveKey.from(move)
                    val child = MctsNode(parent = node, moveFromParent = move)
                    // prior heuristic
                    val score = heuristic.score(state, move, toPlay)
                    child.prior = softPrior(score)
                    node.children[key] = child
                    state = GameEngine.applyMove(state, move).getOrThrow()
                    node = child
                    path += node
                }
            }

            // EVALUATE
            val value = evaluateLeaf(state, rootTeam)

            // BACKUP
            for (n in path) {
                n.visitCount += 1
                n.totalValue += value
            }
        }

        return root.children.mapValues { it.value.visitCount }
    }

    private fun evaluateLeaf(state: GameState, rootTeam: Team): Double {
        val winner = state.winner
        if (winner != null) {
            return when (winner) {
                rootTeam -> 1.0
                else -> -1.0
            }
        }
        // Heuristic state value: chênh lệch “best move score” hoặc feature đơn giản
        // Bản học tập: tanh(sequences diff + open4 diff)
        val mySeq = state.sequencesOf(rootTeam)
        val oppSeq = state.config.teams.filter { it != rootTeam }.sumOf { state.sequencesOf(it) }
        val myOpen = ThreatDetectorLike.countOpenFours(state, rootTeam) // dùng ThreatDetector
        val oppOpen = state.config.teams.filter { it != rootTeam }
            .sumOf { ThreatDetectorLike.countOpenFours(state, it) }
        val raw = (mySeq - oppSeq) * 2.0 + (myOpen - oppOpen) * 0.5
        return tanh(raw)
    }

    private fun softPrior(score: Double): Double {
        // map score → (0,1]; normalize thật sự nên làm trên toàn bộ siblings
        return 1.0 / (1.0 + kotlin.math.exp(-score / 1000.0))
    }
}

// Trong code thật: import ThreatDetector, dùng kotlin.time.TimeSource
private object ThreatDetectorLike {
    fun countOpenFours(state: GameState, team: Team): Int =
        com.karasuma.fivelinks.fivelinks_cmp.ai.tactical.ThreatDetector.countOpenFours(state, team)
}
```

> Đoạn trên là **khung học tập**. Khi code thật hãy:
>
> - Chuẩn hóa prior trên **tất cả** children cùng parent (softmax).
> - Flip value theo side-to-move nếu bạn backup theo góc nhìn người vừa đi (có nhiều biến thể; bản trên backup luôn theo `rootTeam` nên leaf phải luôn theo `rootTeam`).
> - Tôn trọng `timeBudgetMs` bằng `markNow()`.

### 3.6 Chọn nước từ visit counts (Facade)

```kotlin
fun selectByVisits(
    visits: Map<MoveKey, Int>,
    playerId: PlayerId,
    config: DifficultyConfig,
    random: Random,
): Move {
    require(visits.isNotEmpty())
    val scored = visits.entries.map { (k, n) -> k.toMove(playerId) to n.toDouble() }
    return if (config.temperature <= 0.0 || config.topK <= 1) {
        scored.maxBy { it.second }.first
    } else {
        SoftmaxPicker.pick(
            scored.sortedByDescending { it.second }.take(config.topK),
            temperature = config.temperature,
            random = random,
            scoreScale = 1.0, // visit counts khác scale heuristic
        )
    }
}
```

### 3.7 Facade sau Phase 3

```kotlin
override suspend fun chooseMove(...): Move {
    val config = DifficultyConfig.from(difficulty)
    val legal = GameEngine.legalMoves(state, playerId)
    require(legal.isNotEmpty())

    if (config.useTacticalForced) {
        tactical.findForcedMove(state, playerId)?.let { return it }
    }

    val visits = search.search(state, playerId, config)
    if (visits.isEmpty()) {
        return heuristic.chooseMove(state, playerId, difficulty)
    }
    return selectByVisits(visits, playerId, config, random)
}
```

### Done Phase 3

- [ ] Arena: `Facade+MCTS` vs `Heuristic HARD` — winrate cải thiện rõ
- [ ] Latency HARD trong budget (đo trên JVM trước)
- [ ] Không crash khi determinizations > 1

---

## Phase 4 — Evaluation Engine thống nhất

### Mục tiêu

Search không phụ thuộc trực tiếp heuristic hay ONNX — chỉ gọi interface.

### 4.1 Interfaces

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.eval

import com.karasuma.fivelinks.fivelinks_cmp.domain.GameState
import com.karasuma.fivelinks.fivelinks_cmp.domain.Move
import com.karasuma.fivelinks.fivelinks_cmp.domain.PlayerId
import com.karasuma.fivelinks.fivelinks_cmp.domain.Team

fun interface PositionEvaluator {
    /** Value từ góc nhìn `perspective`, khoảng [-1, 1]. */
    fun value(state: GameState, perspective: Team): Float
}

fun interface PolicyPrior {
    /** Prior > 0 cho từng move; Search sẽ normalize. */
    fun priors(state: GameState, playerId: PlayerId, moves: List<Move>): FloatArray
}

data class EvalResult(
    val value: Float,
    val priors: FloatArray?, // null = uniform
)

interface EvaluationEngine {
    fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean,
    ): EvalResult
}
```

### 4.2 Heuristic adapters

```kotlin
class HeuristicPositionEvaluator(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
) : PositionEvaluator {
    override fun value(state: GameState, perspective: Team): Float {
        if (state.winner == perspective) return 1f
        if (state.winner != null) return -1f
        // Dùng feature giống leaf Phase 3, hoặc 1-ply: max score của perspective player
        val player = state.players.firstOrNull { it.team == perspective && it.id == state.currentPlayer.id }
        // Đơn giản: sequence/open4 tanh như Phase 3
        val mySeq = state.sequencesOf(perspective)
        val opp = state.config.teams.filter { it != perspective }.sumOf { state.sequencesOf(it) }
        val raw = (mySeq - opp).toFloat()
        return kotlin.math.tanh(raw.toDouble()).toFloat()
    }
}

class HeuristicPolicyPrior(
    private val heuristic: HeuristicEvaluator = HeuristicEvaluator(),
) : PolicyPrior {
    override fun priors(state: GameState, playerId: PlayerId, moves: List<Move>): FloatArray {
        if (moves.isEmpty()) return floatArrayOf()
        val scores = moves.map { heuristic.score(state, it, playerId) }
        val max = scores.max()
        val exps = scores.map { kotlin.math.exp((it - max) / 1000.0) }
        val sum = exps.sum()
        return FloatArray(moves.size) { i -> (exps[i] / sum).toFloat() }
    }
}

class HeuristicEvaluationEngine(
    private val values: PositionEvaluator = HeuristicPositionEvaluator(),
    private val policy: PolicyPrior = HeuristicPolicyPrior(),
) : EvaluationEngine {
    override fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean,
    ): EvalResult = EvalResult(
        value = values.value(state, perspective),
        priors = if (wantPriors) policy.priors(state, playerId, moves) else null,
    )
}
```

### 4.3 Refactor Search

Thay `evaluateLeaf` / `softPrior` bằng:

```kotlin
val eval = evaluationEngine.evaluate(state, rootTeam, toPlay, legal, wantPriors = true)
// gán child.prior từ eval.priors[i]
// leaf value = eval.value
```

### Done Phase 4

- [ ] Search chỉ phụ thuộc `EvaluationEngine`
- [ ] Hành vi gần Phase 3 (regression arena)

---

## Phase 5 — ActionCodec + StateEncoder

### Mục tiêu

Tensor cố định cho NN; Kotlin và Python **cùng spec**.

### 5.1 Spec (gợi ý v1)

**Board:** 10×10

**Channels (ví dụ 12):**

| Index | Ý nghĩa |
|-------|---------|
| 0 | Chip của “me” (perspective) |
| 1 | Chip đối thủ (gop nếu 2 đội; hoặc tách 1–2) |
| 2 | Empty playable |
| 3 | Locked (trong completedSequence) |
| 4 | Corner stars |
| 5.. | Hand card presence planes (rút gọn) |
| … | deckSize norm, seq counts, to-win |

Bạn có thể bắt đầu **ít channel** (6–8) rồi mở rộng — ghi `encoder_version = 1`.

### 5.2 `ActionCodec` — flat action space

Cách học tập dễ mask:

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.encode

import com.karasuma.fivelinks.fivelinks_cmp.domain.*

/**
 * Layout ví dụ (tự chốt và ghi vào encoder_spec.json):
 * - PLACE: cardIndex * 100 + flatIndex          → size = NUM_CARDS * 100
 * - REMOVE: OFFSET_REMOVE + cardIndex * 100 + flatIndex
 * - SWAP: OFFSET_SWAP + cardIndex
 *
 * NUM_CARDS: đánh số ổn định mọi Card trong Cards.fullDeck (52).
 */
class ActionCodec(
    private val cardIndex: Map<Card, Int> = Cards.fullDeck.withIndex().associate { it.value to it.index },
) {
    val numCards: Int get() = Cards.fullDeck.size // 52
    val placeSize: Int get() = numCards * 100
    val removeSize: Int get() = numCards * 100
    val swapSize: Int get() = numCards
    val maxActions: Int get() = placeSize + removeSize + swapSize

    fun encode(move: Move): Int {
        val ci = cardIndex.getValue(move.card)
        return when (move) {
            is Move.Place -> ci * 100 + move.position.flatIndex
            is Move.Remove -> placeSize + ci * 100 + move.position.flatIndex
            is Move.SwapDeadCard -> placeSize + removeSize + ci
        }
    }

    fun legalMask(state: GameState, playerId: PlayerId): BooleanArray {
        val mask = BooleanArray(maxActions)
        for (m in GameEngine.legalMoves(state, playerId)) {
            mask[encode(m)] = true
        }
        return mask
    }
}
```

### 5.3 `StateEncoder`

```kotlin
class StateEncoder(
    val version: Int = 1,
    val channels: Int = 8,
    val height: Int = 10,
    val width: Int = 10,
) {
    val size: Int get() = channels * height * width

    /** NCHW flat float array. */
    fun encode(state: GameState, perspective: Team): FloatArray {
        val out = FloatArray(size)
        fun idx(c: Int, row: Int, col: Int) = c * 100 + row * 10 + col

        for (row in 0..9) for (col in 0..9) {
            val pos = BoardPosition(row, col)
            val team = state.chips.at(pos)
            when {
                team == perspective -> out[idx(0, row, col)] = 1f
                team != null -> out[idx(1, row, col)] = 1f
                !state.board.isCorner(pos) -> out[idx(2, row, col)] = 1f
            }
            if (state.board.isCorner(pos)) out[idx(3, row, col)] = 1f
        }
        for (seq in state.completedSequence) {
            for (p in seq.positions) out[idx(4, p.row, p.column)] = 1f
        }
        // ChipSequence = data class (team, positions) — đã khớp domain hiện tại
        // channels 5+ : hand / meta — tự thiết kế và ghi spec
        return out
    }
}
```

### 5.4 `encoder_spec.json` (bắt buộc đồng bộ Python)

```json
{
  "encoder_version": 1,
  "channels": 8,
  "height": 10,
  "width": 10,
  "layout": "NCHW",
  "channel_meanings": [
    "me_chips",
    "opp_chips",
    "empty",
    "corners",
    "locked",
    "...",
    "...",
    "..."
  ],
  "max_actions": 5456,
  "action_layout": "place[card*100+cell] | remove[...] | swap[card]"
}
```

### Done Phase 5

- [ ] Unit test: encode hai state khác → tensor khác
- [ ] `legalMask` chỉ true đúng `legalMoves`
- [ ] Spec JSON committed

---

## Phase 6 — Neural + training + ONNX export

### Mục tiêu

Policy + value nhỏ, train offline, export `.onnx`.

### 6.1 Model PyTorch (ví dụ)

```python
# ml/train/model.py
import torch
import torch.nn as nn
import torch.nn.functional as F

class TinyPV(nn.Module):
    def __init__(self, channels=8, max_actions=5456):
        super().__init__()
        self.conv1 = nn.Conv2d(channels, 64, 3, padding=1)
        self.conv2 = nn.Conv2d(64, 64, 3, padding=1)
        self.conv3 = nn.Conv2d(64, 64, 3, padding=1)
        self.policy = nn.Linear(64 * 10 * 10, max_actions)
        self.value = nn.Sequential(
            nn.Linear(64 * 10 * 10, 128),
            nn.ReLU(),
            nn.Linear(128, 1),
        )

    def forward(self, x):
        # x: [B, C, 10, 10]
        h = F.relu(self.conv1(x))
        h = F.relu(self.conv2(h))
        h = F.relu(self.conv3(h))
        flat = h.flatten(1)
        logits = self.policy(flat)
        value = torch.tanh(self.value(flat))
        return logits, value.squeeze(-1)
```

### 6.2 Self-play data (khuyến nghị)

**Source of truth = Kotlin engine.**

Pipeline học tập:

1. JVM tool / test: chạy MCTS+heuristic, mỗi vị trí ghi:
   - `state_tensor` (float32)
   - `pi` (visit distribution trên max_actions, đã mask)
   - `z` (outcome từ góc nhìn player: +1/-1/0)
2. Export `.npz` hoặc folder mẫu `.jsonl`
3. Python đọc và train

Pseudo Kotlin recorder:

```kotlin
data class TrainSample(
    val encoderVersion: Int,
    val state: FloatArray,
    val pi: FloatArray, // length maxActions
    val z: Float,
)
```

### 6.3 Loss

```python
loss = CE(logits masked, pi) + mse(value, z) + weight_decay
```

Mask illegal logits bằng `-inf` trước softmax khi cần.

### 6.4 Export ONNX

```python
# ml/train/export_onnx.py
dummy = torch.randn(1, 8, 10, 10)
torch.onnx.export(
    model, dummy, "model_v1.onnx",
    input_names=["state"],
    output_names=["policy_logits", "value"],
    dynamic_axes={"state": {0: "batch"}, "policy_logits": {0: "batch"}, "value": {0: "batch"}},
    opset_version=17,
)
```

Verify: cùng input → PyTorch ≈ ORT.

### 6.5 Promote rule

Chỉ thay model production khi arena vs baseline MCTS-heuristic **thắng rõ** trên seed cố định (ví dụ ≥ +5% over 400 games).

### Done Phase 6

- [ ] `model_v1.onnx` + `encoder_spec.json`
- [ ] Bảng winrate vs baseline
- [ ] Metadata `encoder_version` khớp Kotlin

---

## Phase 7 — ONNX Runtime trên KMP

### Mục tiêu

Inference trên Android / JVM / iOS; Web fallback heuristic **hoặc** gọi `POST /ai/move` (server JVM chạy ORT).

Với BE đã có `/ai/move`, đường đi thực dụng nhất:

1. **Ưu tiên:** ORT trên **server JVM** + client (kể cả Wasm) gửi state lên HTTP.  
2. **Song song:** ORT trên Android/iOS khi chơi offline.  
3. **Wasm/JS local:** `createNeuralInferenceOrNull() = null` → heuristic, hoặc không chạy AI local.

### 7.1 Common expect API

```kotlin
package com.karasuma.fivelinks.fivelinks_cmp.ai.inference

data class NeuralOutput(
    val policyLogits: FloatArray,
    val value: Float,
)

interface NeuralInference {
    val encoderVersion: Int
    val isAvailable: Boolean
    suspend fun infer(stateNchw: FloatArray): NeuralOutput
}

/** Factory gọi từ common. */
expect fun createNeuralInferenceOrNull(): NeuralInference?
```

### 7.2 Unsupported (JS / Wasm)

```kotlin
// jsMain / wasmJsMain
actual fun createNeuralInferenceOrNull(): NeuralInference? = null
```

### 7.3 JVM / Android (ý tưởng)

```kotlin
// Dùng onnxruntime Maven dependency ở androidMain/jvmMain
class OnnxNeuralInference(
    private val session: OrtSession, // minh họa
    override val encoderVersion: Int = 1,
) : NeuralInference {
    override val isAvailable: Boolean = true
    override suspend fun infer(stateNchw: FloatArray): NeuralOutput {
        // tạo tensor shape [1, C, 10, 10]
        // session.run → policy_logits, value
        TODO("Wire ORT API theo dependency bạn chọn")
    }
}
```

Đóng gói model:

- Android: `assets/models/model_v1.onx`
- JVM: `resources/models/model_v1.onx`
- iOS: bundle resource + ORT Mobile

### 7.4 Hybrid EvaluationEngine

```kotlin
class HybridEvaluationEngine(
    private val heuristic: HeuristicEvaluationEngine,
    private val encoder: StateEncoder,
    private val codec: ActionCodec,
    private val neural: NeuralInference?,
) : EvaluationEngine {

    override fun evaluate(
        state: GameState,
        perspective: Team,
        playerId: PlayerId,
        moves: List<Move>,
        wantPriors: Boolean,
    ): EvalResult {
        val fallback = heuristic.evaluate(state, perspective, playerId, moves, wantPriors)
        val nn = neural
        if (nn == null || !nn.isAvailable || nn.encoderVersion != encoder.version) {
            return fallback
        }
        return try {
            // NOTE: infer là suspend — có thể đổi EvaluationEngine sang suspend
            // hoặc chạy infer blocking trong context search có giới hạn thời gian
            error("Implement suspend path in Search when wiring ORT")
        } catch (_: Throwable) {
            fallback
        }
    }
}
```

**Gợi ý thiết kế sạch hơn:** đổi `EvaluationEngine.evaluate` thành `suspend`, vì `chooseMove` đã `suspend`.

Priors từ logits:

```kotlin
fun priorsFromLogits(logits: FloatArray, moves: List<Move>, codec: ActionCodec): FloatArray {
    val idxs = moves.map { codec.encode(it) }
    val max = idxs.maxOf { logits[it] }
    val exps = idxs.map { kotlin.math.exp((logits[it] - max).toDouble()) }
    val sum = exps.sum()
    return FloatArray(moves.size) { (exps[it] / sum).toFloat() }
}
```

### 7.5 DifficultyConfig Phase 7

```kotlin
Difficulty.HARD -> ... copy(evalMode = EvalMode.Hybrid)
Difficulty.EASY -> ... HeuristicOnly  // nhanh, đỡ tốn pin
```

### Done Phase 7

- [ ] Android (hoặc JVM) chạy được hybrid
- [ ] **Server** `/ai/move` dùng cùng `AiFacadeService` + hybrid (không `new HeuristicEvaluator()` mỗi request)
- [ ] Web không crash — null inference local → heuristic **hoặc** ủy quyền HTTP
- [ ] Timeout / exception → fallback
- [ ] Response thống nhất `AiMoveResponse` (nếu client đã migrate)

---

## Phase 8 — Tune difficulty & UX

### Việc cần đo

| Metric | Công cụ |
|--------|---------|
| P50/P95 latency `chooseMove` | android profiler / measureTime |
| Winrate EASY < MEDIUM < HARD | arena |
| % tactical short-circuit | counter trong Facade |

### Gợi ý tune

- HARD: tăng `determinizations` trước khi tăng sims mù quáng.
- MEDIUM: sims vừa + temperature > 0 để “lệch người”.
- EASY: có thể **không** luôn lấy forced win (optional “human-like”) — cân nhắc fairness.
- Trong vòng MCTS: mỗi N sims check deadline; return best-so-far.

### Cancel / coroutine

```kotlin
suspend fun chooseMove(...) {
    yield() // cho UI thở
    // trong MCTS loop:
    ensureActive()
}
```

### Done Phase 8

- [ ] HARD cảm giác mạnh, latency chấp nhận được trên máy thật
- [ ] Ba mức difficulty phân biệt rõ trên arena

---

## Phase 9 — Tests, arena, docs

### 9.1 Kim tự tháp test

| Tầng | Ví dụ |
|------|--------|
| Unit | `ActionCodec`, `ThreatDetector`, PUCT chọn child, `DifficultyConfig` |
| Integration | Facade trả move ∈ `legalMoves` mọi difficulty |
| Regression | must-win / must-block fixtures |
| Arena | 200–1000 games seed cố định mỗi khi đổi search/model |

### 9.2 Docs nên có (bạn tự viết khi làm)

- `docs/ai-architecture.md` — sơ đồ runtime đã chốt
- `docs/ai-encoder-spec.md` — copy từ `encoder_spec.json` + giải thích
- `docs/ai-training.md` — lệnh train / export / promote

### 9.3 Logging debug (dev only)

```kotlin
data class AiDebugInfo(
    val tacticalHit: Boolean,
    val simulationsRun: Int,
    val determinizations: Int,
    val chosenKey: MoveKey,
    val evalMode: EvalMode,
)
```

Không bắt buộc expose ra UI production.

---

## 15. Checklist Definition of Done

- [ ] UI / HTTP chỉ gọi `AiService` / `AiFacadeService` (không new heuristic trong route)
- [ ] EASY < MEDIUM < HARD (đo arena)
- [ ] HARD dùng search; tactical bắt forced win/block
- [ ] Heuristic luôn fallback an toàn
- [ ] Neural+ONNX trên ≥ Android hoặc JVM server; Web không regress (local fallback hoặc HTTP)
- [ ] `/ai/move` + `/game/new` smoke test ổn với AI mới
- [ ] Response AI thống nhất (`AiMoveResponse` khuyến nghị)
- [ ] `encoder_version` khớp model
- [ ] Promote model có số liệu
- [ ] Domain public API không phá vỡ không cần thiết

---

## 16. Phụ lục

### A. Công thức PUCT

\[
a_t = \arg\max_a \left( Q(s,a) + c_{\mathrm{puct}}\, P(s,a)\, \frac{\sqrt{N(s)}}{1+N(s,a)} \right)
\]

- \(Q\): giá trị trung bình backup (góc nhìn root team)
- \(P\): prior (heuristic softmax hoặc NN)
- \(N\): visit count

### B. Thứ tự implement file (checklist copy)

```
Phase 0  [x] fix applyPlace / shuffle deck / legalMoves Remove
         [x] SoftmaxPicker
         [x] Phase0DomainTest + AiArenaTest baseline
         [x] AiHttpArenaTest

Phase 1  [x] DifficultyConfig
         [x] AiFacadeService → tactical + heuristic
         [x] Wire AiRoute(aiService)
         [ ] (optional) unit test DifficultyConfig mapping

Phase 2  [x] ThreatDetector
         [x] TacticalEngine (`findForceMove`)
         [x] fixtures must-win/block + Facade
         [x] Facade gọi tactical

Phase 3  [x] MoveKey
         [x] Determinizer
         [x] MctsNode + SearchEngine
         [x] Facade gọi search
         [x] arena vs heuristic

Phase 4  [x] PositionEvaluator / PolicyPrior / EvaluationEngine
         [x] Heuristic* adapters
         [x] Search dùng EvaluationEngine

Phase 5  [x] ActionCodec
         [x] StateEncoder
         [x] encoder_spec.json
         [x] Unit test: encode hai state khác → tensor khác
         [x] `legalMask` chỉ true đúng `legalMoves`
         [x] Spec JSON committed

Phase 6  [ ] self-play export
         [ ] TinyPV train
         [ ] export ONNX + promote

Phase 7  [ ] NeuralInference expect/actual
         [ ] HybridEvaluationEngine
         [ ] bundle model

Phase 8  [ ] tune budgets
         [ ] ensureActive / deadline

Phase 9  [ ] regression suite
         [ ] docs ai-* 
```

### C. Rủi ro thường gặp

| Rủi ro | Cách tránh |
|--------|------------|
| Search chậm mobile | time budget + tactical + ít determinization |
| Train lệch engine | self-play bằng Kotlin; cùng encoder_version |
| Wasm không ORT | `createNeuralInferenceOrNull() = null` |
| Jack branching | move ordering từ tactical/heuristic |
| Over-tune weights tay | arena cố định; đổi 1 biến/lần |
| Value backup sai phía | luôn evaluate theo `rootTeam` (như guide) hoặc flip có chủ đích |

### D. Liên hệ code hiện có

| Hiện có | Tái sử dụng thế nào |
|---------|---------------------|
| `HeuristicEvaluator.score` | policy prior + tactical tie-break + baseline |
| `HeuristicWeights` | giữ / tune; NN không thay thế ngay |
| `Difficulty` enum (`@Serializable`) | map → `DifficultyConfig`; field trong `AiMoveRequest` |
| `GameEngine.legalMoves/applyMove` | xương sống MCTS |
| `BoardLines` / `lineStatus` | ThreatDetector |
| `ChipSequence(team, positions)` | locked channel trong encoder; check remove jack |
| `GameState` `@Serializable` | HTTP body + self-play dump JSON |
| `Player.isAi` / `PlayerId = String` | phân biệt bot; id kiểu `"p0"` như `GameRoute` |
| `AiMoveRequest` / `AiMoveResponse` | contract HTTP — đừng tạo DTO song song |
| `POST /ai/move`, `POST /game/new` | entry server; inject Facade ở Phase 1 |
| `ErrorCodes.INVALID_REQUEST` | lỗi body / chooseMove fail |

### E. Gợi ý học từng bước nhỏ

1. Chỉ viết `Determinizer` + in ra hand đối thủ sample — hiểu imperfect info.  
2. Chỉ viết MCTS **perfect-info** trên state đã mở hết bài (debug).  
3. Bật determinization.  
4. Mới thêm NN.

---

## Kết

Hãy coi **Phase 3** là đích gần: bot đã “biết nghĩ”. Phase 6–7 là lớp AlphaZero tối giản khi bạn đã có harness và encoder ổn định.

Với BE hiện tại: mỗi lần nâng `AiService`, nhớ wire lại **`AiRoute`** (một instance) để HTTP và in-process cùng một bộ não.

Khi implement, giữ guide này cạnh PR/commit nhỏ theo từng phase; mỗi phase một milestone có arena số liệu — đó là cách học chắc và tránh rewrite lớn.

Chúc bạn triển khai vui và “xịn” dần theo đúng nhịp học tập.

---

## 17. Changelog guide

| Ngày | Thay đổi |
|------|----------|
| 20/07/2026 | Bản đầu: Phase 0–9, kiến trúc Facade → Tactical → ISMCTS → Eval → ONNX |
| 23/07/2026 | Đồng bộ code BE mới: `model/*`, `POST /game/new`, `POST /ai/move`, serializable domain; dual-entry UI+HTTP |
| 23/07/2026 | Phase 0–2 trong repo: domain fixes, arena, Facade, Tactical; thêm `TacticalEngineTest`; guide §2/Phase 2 phản ánh API thật (`findForceMove`) |