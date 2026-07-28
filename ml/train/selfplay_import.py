"""Load Kotlin self-play JSONL into numpy arrays (Phase 6)."""

from __future__ import annotations

import json
from pathlib import Path

import numpy as np

SPEC_PATH = Path(__file__).resolve().parents[1] / "specs" / "encoder_spec.json"


def load_encoder_spec(spec_path: Path | None = None) -> dict:
    path = spec_path or SPEC_PATH
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def load_selfplay_data(jsonl_path, spec_path: Path | None = None):
    spec = load_encoder_spec(spec_path)
    channels = int(spec["channels"])
    height = int(spec["height"])
    width = int(spec["width"])
    max_actions = int(spec["max_actions"])
    expected_version = int(spec["encoder_version"])
    expected_state_size = channels * height * width

    states, pis, zs = [], [], []
    skipped = 0

    with open(jsonl_path, "r", encoding="utf-8") as f:
        for line_no, line in enumerate(f, start=1):
            if not line.strip():
                continue
            data = json.loads(line)
            version = int(data.get("encoderVersion", data.get("encoder_version", -1)))
            if version != expected_version:
                raise ValueError(
                    f"Line {line_no}: encoderVersion={version} != spec {expected_version}"
                )
            state = data["state"]
            pi = data["pi"]
            if len(state) != expected_state_size:
                raise ValueError(
                    f"Line {line_no}: state len={len(state)} != {expected_state_size}"
                )
            if len(pi) != max_actions:
                raise ValueError(
                    f"Line {line_no}: pi len={len(pi)} != {max_actions}"
                )
            pi_sum = float(np.sum(pi))
            if pi_sum <= 0:
                skipped += 1
                continue
            states.append(state)
            pis.append(pi)
            zs.append(data["z"])

    if not states:
        raise ValueError(f"No usable samples in {jsonl_path} (skipped empty pi={skipped})")

    states_arr = np.asarray(states, dtype=np.float32).reshape(-1, channels, height, width)
    pis_arr = np.asarray(pis, dtype=np.float32)
    zs_arr = np.asarray(zs, dtype=np.float32)
    return states_arr, pis_arr, zs_arr, spec


if __name__ == "__main__":
    path = Path(__file__).resolve().parents[1] / "data" / "selfplay_dataset.jsonl"
    s, p, z, spec = load_selfplay_data(path)
    print(
        f"Loaded {len(s)} samples. states={s.shape} pis={p.shape} zs={z.shape} "
        f"channels={spec['channels']} max_actions={spec['max_actions']}"
    )
