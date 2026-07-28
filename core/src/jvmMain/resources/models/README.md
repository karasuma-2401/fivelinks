# Place exported TinyPV ONNX here as `model_v1.onnx`.
#
# From repo root (with ml/.venv activated):
#   python ml/train/export_onnx.py --out core/src/jvmMain/resources/models/model_v1.onnx
#   copy the same file to core/src/androidMain/resources/models/model_v1.onnx
#
# Without this file, createNeuralInferenceOrNull() returns null and Hybrid falls back to heuristic.
