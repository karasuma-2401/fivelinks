"""Train TinyPV on Kotlin self-play JSONL (Phase 6)."""

from __future__ import annotations

import argparse
from pathlib import Path

import torch
import torch.nn.functional as F
from torch.utils.data import DataLoader

from dataset import SelfPlayDataset
from model import TinyPV

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_DATA = ROOT / "data" / "selfplay_dataset.jsonl"
DEFAULT_CKPT = ROOT / "data" / "tinypv_v1.pt"


def policy_value_loss(logits, values, pis, zs, value_weight: float = 1.0):
    """AlphaZero-style: -Σ π log softmax(logits) + MSE(v, z)."""
    log_probs = F.log_softmax(logits, dim=-1)
    # Zero-mass illegal actions in π contribute nothing.
    loss_p = -(pis * log_probs).sum(dim=-1).mean()
    loss_v = F.mse_loss(values, zs)
    return loss_p + value_weight * loss_v, loss_p.detach(), loss_v.detach()


def train(
    data_path: Path,
    ckpt_path: Path,
    epochs: int = 5,
    batch_size: int = 32,
    lr: float = 1e-3,
    weight_decay: float = 1e-4,
):
    dataset = SelfPlayDataset(data_path)
    loader = DataLoader(dataset, batch_size=batch_size, shuffle=True)
    model = TinyPV.from_spec(dataset.spec)
    optimizer = torch.optim.AdamW(model.parameters(), lr=lr, weight_decay=weight_decay)

    model.train()
    for epoch in range(epochs):
        total = 0.0
        total_p = 0.0
        total_v = 0.0
        for states, pis, zs in loader:
            optimizer.zero_grad()
            logits, values = model(states)
            loss, loss_p, loss_v = policy_value_loss(logits, values, pis, zs)
            loss.backward()
            optimizer.step()
            total += loss.item()
            total_p += loss_p.item()
            total_v += loss_v.item()
        n = max(len(loader), 1)
        print(
            f"Epoch {epoch + 1}/{epochs}  "
            f"loss={total / n:.4f}  policy={total_p / n:.4f}  value={total_v / n:.4f}"
        )

    ckpt_path.parent.mkdir(parents=True, exist_ok=True)
    torch.save(
        {
            "model_state": model.state_dict(),
            "encoder_version": int(dataset.spec["encoder_version"]),
            "channels": int(dataset.spec["channels"]),
            "max_actions": int(dataset.spec["max_actions"]),
        },
        ckpt_path,
    )
    print(f"Saved checkpoint -> {ckpt_path}")
    return ckpt_path


def main():
    parser = argparse.ArgumentParser(description="Train TinyPV from self-play JSONL")
    parser.add_argument("--data", type=Path, default=DEFAULT_DATA)
    parser.add_argument("--ckpt", type=Path, default=DEFAULT_CKPT)
    parser.add_argument("--epochs", type=int, default=5)
    parser.add_argument("--batch-size", type=int, default=32)
    parser.add_argument("--lr", type=float, default=1e-3)
    parser.add_argument("--weight-decay", type=float, default=1e-4)
    args = parser.parse_args()
    if not args.data.exists():
        raise SystemExit(
            f"Missing dataset: {args.data}\n"
            "Generate with: ./gradlew :core:jvmTest --tests \"*SelfPlayRunnerTest*\""
        )
    train(args.data, args.ckpt, args.epochs, args.batch_size, args.lr, args.weight_decay)


if __name__ == "__main__":
    main()
