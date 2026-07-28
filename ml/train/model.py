"""Tiny policy-value net for Five Links (Phase 6)."""

from __future__ import annotations

import json
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F

SPEC_PATH = Path(__file__).resolve().parents[1] / "specs" / "encoder_spec.json"


def load_spec(spec_path: Path | None = None) -> dict:
    path = spec_path or SPEC_PATH
    with path.open(encoding="utf-8") as f:
        return json.load(f)


class TinyPV(nn.Module):
    def __init__(self, channels: int = 12, max_actions: int = 10452):
        super().__init__()
        self.channels = channels
        self.max_actions = max_actions
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

    @classmethod
    def from_spec(cls, spec: dict | None = None) -> "TinyPV":
        spec = spec or load_spec()
        return cls(channels=int(spec["channels"]), max_actions=int(spec["max_actions"]))
