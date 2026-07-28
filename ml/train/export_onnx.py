"""Export TinyPV checkpoint to ONNX and optionally verify vs PyTorch."""

from __future__ import annotations

import argparse
from pathlib import Path

import torch

from model import TinyPV, load_spec

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_CKPT = ROOT / "data" / "tinypv_v1.pt"
DEFAULT_ONNX = ROOT / "data" / "model_v1.onnx"


def load_model(ckpt_path: Path) -> TinyPV:
    spec = load_spec()
    model = TinyPV.from_spec(spec)
    if ckpt_path.exists():
        blob = torch.load(ckpt_path, map_location="cpu", weights_only=False)
        if blob.get("encoder_version") != int(spec["encoder_version"]):
            raise ValueError(
                f"Checkpoint encoder_version={blob.get('encoder_version')} "
                f"!= spec {spec['encoder_version']}"
            )
        model.load_state_dict(blob["model_state"])
        print(f"Loaded weights from {ckpt_path}")
    else:
        print(f"Warning: no checkpoint at {ckpt_path}; exporting randomly initialized model")
    model.eval()
    return model


def export_onnx(model: TinyPV, onnx_path: Path, channels: int):
    onnx_path.parent.mkdir(parents=True, exist_ok=True)
    dummy = torch.randn(1, channels, 10, 10)
    # Torch 2.x+ defaults to dynamo exporter (needs onnxscript). Prefer legacy for TinyPV.
    export_kwargs = dict(
        input_names=["state"],
        output_names=["policy_logits", "value"],
        dynamic_axes={
            "state": {0: "batch"},
            "policy_logits": {0: "batch"},
            "value": {0: "batch"},
        },
        opset_version=17,
    )
    try:
        torch.onnx.export(
            model,
            dummy,
            str(onnx_path),
            dynamo=False,
            **export_kwargs,
        )
    except TypeError:
        # Older torch without dynamo= kwarg
        torch.onnx.export(model, dummy, str(onnx_path), **export_kwargs)
    print(f"Exported ONNX -> {onnx_path}")


def verify_ort(model: TinyPV, onnx_path: Path, channels: int, atol: float = 1e-4):
    try:
        import numpy as np
        import onnxruntime as ort
    except ImportError:
        print("Skip ORT verify (install onnxruntime to enable)")
        return False

    x = torch.randn(2, channels, 10, 10)
    with torch.no_grad():
        pt_logits, pt_value = model(x)

    session = ort.InferenceSession(str(onnx_path), providers=["CPUExecutionProvider"])
    ort_logits, ort_value = session.run(None, {"state": x.numpy()})
    logits_ok = np.allclose(pt_logits.numpy(), ort_logits, atol=atol)
    value_ok = np.allclose(pt_value.numpy(), ort_value, atol=atol)
    print(f"ORT verify: logits={logits_ok} value={value_ok}")
    if not (logits_ok and value_ok):
        raise SystemExit("PyTorch vs ORT mismatch")
    return True


def main():
    parser = argparse.ArgumentParser(description="Export TinyPV to ONNX")
    parser.add_argument("--ckpt", type=Path, default=DEFAULT_CKPT)
    parser.add_argument("--out", type=Path, default=DEFAULT_ONNX)
    parser.add_argument("--verify", action="store_true", help="Compare ORT vs PyTorch")
    args = parser.parse_args()

    spec = load_spec()
    model = load_model(args.ckpt)
    export_onnx(model, args.out, channels=int(spec["channels"]))
    if args.verify:
        verify_ort(model, args.out, channels=int(spec["channels"]))


if __name__ == "__main__":
    main()
