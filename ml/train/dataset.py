"""PyTorch Dataset wrapping Kotlin self-play JSONL."""

from __future__ import annotations

import torch
from torch.utils.data import Dataset

from selfplay_import import load_selfplay_data


class SelfPlayDataset(Dataset):
    def __init__(self, jsonl_path, spec_path=None):
        s, p, z, spec = load_selfplay_data(jsonl_path, spec_path=spec_path)
        self.spec = spec
        self.states = torch.tensor(s, dtype=torch.float32)
        self.pis = torch.tensor(p, dtype=torch.float32)
        self.zs = torch.tensor(z, dtype=torch.float32)

    def __len__(self):
        return len(self.states)

    def __getitem__(self, idx):
        return self.states[idx], self.pis[idx], self.zs[idx]
