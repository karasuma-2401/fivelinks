# AI training (Phase 6)

Offline pipeline: Kotlin self-play → JSONL → TinyPV train → ONNX export → promote via arena.

## Contract

Source of truth for tensor layout:

- Kotlin: `core/.../ai/encode/StateEncoder.kt` + `ActionCodec.kt`
- Spec (synced copy): `ml/specs/encoder_spec.json`

| Field | Value |
|-------|-------|
| `encoder_version` | `1` |
| `channels` | `12` (NCHW, 10×10) |
| `max_actions` | `10452` (`52*100` place + `52*100` remove + `52` swap) |
| Sample JSON keys | `encoderVersion`, `state`, `pi`, `z` |

## 1. Generate self-play data

From repo root:

```bash
./gradlew :core:jvmTest --tests "com.karasuma.fivelinks.fivelinks_cmp.ai.SelfPlayRunnerTest.generateSelfPlayData" -Dselfplay.run=true -Dselfplay.games=50
```

Optional:

- `-Dselfplay.games=100`
- `-Dselfplay.out=ml/data/selfplay_dataset.jsonl`

Without `-Dselfplay.run=true` the test no-ops (keeps CI fast).

Output: `ml/data/selfplay_dataset.jsonl` (one `TrainSample` per line).

Self-play uses `DifficultyConfig.selfPlay()` (temperature > 0 for visit diversity). Only positions with non-empty MCTS visits are written; `pi` is visit distribution masked to legal actions; `z` is +1 / -1 / 0 from that player’s team.

## 2. Train

```bash
cd ml
# Prefer Windows CPython (py -3), NOT MSYS python — torch has no MSYS wheels.
py -3 -m venv .venv
.\.venv\Scripts\Activate.ps1   # Windows PowerShell
pip install -r requirements.txt
python train/train.py --data data/selfplay_dataset.jsonl --ckpt data/tinypv_v1.pt --epochs 5
```

Nếu gặp `ModuleNotFoundError: No module named 'torch'`: bạn đang chạy Python chưa cài deps (hoặc MSYS python). Activate `.venv` rồi chạy lại, hoặc gọi thẳng:

```bash
.\.venv\Scripts\python.exe train/train.py --data data/selfplay_dataset.jsonl --ckpt data/tinypv_v1.pt
```

Loss: soft policy CE (`-Σ π log softmax`) + MSE(value, z) + AdamW weight decay.

## 3. Export ONNX

```bash
python train/export_onnx.py --ckpt data/tinypv_v1.pt --out data/model_v1.onnx --verify
```

I/O names: `state` → `policy_logits`, `value` (opset 17). `--verify` checks PyTorch ≈ ONNX Runtime when `onnxruntime` is installed.

## 4. Promote rule

Do **not** ship a new `model_v1.onnx` to production until an arena vs baseline MCTS+heuristic wins clearly on fixed seeds (guide target: ≥ +5% over ~400 games). Until Phase 7 wires ORT into KMP, keep the ONNX artifact under `ml/data/` for offline eval only.

## Smoke import

```bash
python train/selfplay_import.py
```

Fails fast if `encoderVersion` / state size / `pi` length disagree with `ml/specs/encoder_spec.json`.
