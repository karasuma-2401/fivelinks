# Five Links AI System Documentation

> Project: **fivelinks-cmp**  
> Purpose: This document provides a comprehensive overview of the Five Links AI system, its architecture, implementation details, training workflow, and performance benchmarks. It serves as the primary reference for understanding and maintaining the AI.  
> Last Updated: [Current Date]

---

## Table of Contents

1.  [Overview & Goals](#1-overview--goals)
2.  [AI Architecture](#2-ai-architecture)
    *   [Overall Diagram](#21-overall-diagram)
    *   [Core Components](#22-core-components)
3.  [Detailed Component Breakdown](#3-detailed-component-breakdown)
    *   [AiFacadeService](#31-aifacadeservice)
    *   [DifficultyConfig](#32-difficultyconfig)
    *   [TacticalEngine](#33-tacticalengine)
    *   [SearchEngine](#34-searchengine)
    *   [HeuristicEvaluator](#35-heuristicevaluator)
    *   [EvaluationEngine (Interface & Implementations)](#36-evaluationengine-interface--implementations)
    *   [NeuralInference (ONNX Runtime)](#37-neuralinference-onnx-runtime)
4.  [Data Encoding for Neural Networks](#4-data-encoding-for-neural-networks)
    *   [StateEncoder](#41-stateencoder)
    *   [ActionCodec](#44-actioncodec)
    *   [Encoder Specification (`encoder_spec.json`)](#43-encoder-specification-encoder_specjson)
5.  [AI Training Workflow](#5-ai-training-workflow)
    *   [Data Generation (Self-Play)](#51-data-generation-self-play)
    *   [Training on Google Colab/Kaggle (GPU)](#52-training-on-google-colabkaggle-gpu)
    *   [Exporting to ONNX](#53-exporting-to-onnx)
    *   [Model Promotion & Evaluation](#54-model-promotion--evaluation)
6.  [Testing & Benchmarking](#6-testing--benchmarking)
    *   [Regression Tests (`AiRegressionTest`)](#61-regression-tests-airegressiontest)
    *   [Performance Benchmarking (`AiBenchmarkArenaTest`)](#62-performance-benchmarking-aibenchmarkarenatest)
    *   [Current Performance Baseline](#63-current-performance-baseline)
7.  [Future Work & Considerations](#7-future-work--considerations)

---

## 1. Overview & Goals

The Five Links AI system is designed to provide an intelligent opponent for the Sequence/Five Links game. It employs a simplified AlphaZero-like approach, combining tactical rules, Monte Carlo Tree Search (MCTS) with determinization for imperfect information games, and a hybrid evaluation engine that can leverage both heuristics and neural networks.

**Key AI Components:**

*   **`AiFacadeService`**: The primary entry point for UI or HTTP requests to get an AI move.
*   **`DifficultyConfig`**: Manages AI parameters (simulations, temperature, evaluation mode) based on selected difficulty.
*   **`TacticalEngine`**: Identifies and executes immediate forced moves (winning, blocking critical threats).
*   **`SearchEngine`**: Implements MCTS with determinization to explore future game states under hidden information.
*   **`EvaluationEngine`**: Provides value and policy priors for the MCTS, using either heuristics or a neural network.
*   **`ONNX Runtime`**: Enables efficient cross-platform inference of the neural network model.

The game's characteristics (hidden hands, random draws, wild Jacks) necessitate a determinization + MCTS approach, as opposed to perfect-information game AI like traditional AlphaZero.

## 2. AI Architecture

### 2.1 Overall Diagram

The AI system follows a layered architecture, ensuring a clear separation of concerns and a unified core logic accessible from different clients.

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

### 2.2 Core Components

*   **`AiFacadeService`**: Acts as the orchestrator, receiving game states and difficulty, then delegating to tactical or search engines.
*   **`DifficultyConfig`**: Translates abstract difficulty levels (EASY, MEDIUM, HARD) into concrete parameters for the underlying AI engines.
*   **`TacticalEngine`**: A fast, rule-based engine that identifies immediate winning or critical blocking moves. It acts as a "short-circuit" to prevent blunders and speed up obvious decisions.
*   **`SearchEngine`**: The core decision-making unit for complex scenarios, employing a Monte Carlo Tree Search (MCTS) variant.
*   **`EvaluationEngine`**: An interface for evaluating game states, providing both a value (how good the state is) and policy priors (which moves are promising). This allows for interchangeable heuristic and neural network implementations.
*   **`ONNX Runtime`**: A cross-platform inference engine used to run pre-trained neural network models efficiently on various targets (JVM, Android, iOS).

## 3. Detailed Component Breakdown

### 3.1 AiFacadeService

The `AiFacadeService` is the primary entry point for any client requesting an AI move. It implements the `AiService` interface, ensuring a consistent API.

*   **Role**:
    *   Receives `GameState`, `PlayerId`, and `Difficulty`.
    *   Converts `Difficulty` into a detailed `DifficultyConfig`.
    *   First, attempts to find a forced move using `TacticalEngine` (if `useTacticalForced` is enabled in config).
    *   If no forced move is found, it initiates a search using `SearchEngine`.
    *   Selects the final move based on visit counts from the search or falls back to heuristic if search yields no results.

### 3.2 DifficultyConfig

This data class centralizes all configurable parameters for the AI's behavior, allowing fine-grained control over difficulty levels.

*   **Parameters**: `maxSimulations`, `timeBudgetMs`, `temperature`, `topK`, `useTacticalForced`, `determinizations`, `evalMode`, `cPuct`.
*   **`from(difficulty: Difficulty)`**: A factory method that maps the simple `Difficulty` enum to a specific `DifficultyConfig` instance.
    *   **`Difficulty.HARD` Configuration (Current Optimized)**:
        *   `maxSimulations = 2000`
        *   `timeBudgetMs = 600`
        *   `temperature = 0.0` (deterministic choice)
        *   `topK = 1`
        *   `useTacticalForced = true`
        *   `determinizations = 6` (Optimized for latency)
        *   `evalMode = EvalMode.Hybrid`
    *   **`selfPlay()` Configuration (Optimized for Data Generation)**:
        *   `maxSimulations = 100`
        *   `timeBudgetMs = 100`
        *   `temperature = 1.0` (high for move diversity)
        *   `topK = 15`
        *   `useTacticalForced = true`
        *   `determinizations = 2` (Optimized for speed)
        *   `evalMode = EvalMode.HeuristicOnly`

### 3.3 TacticalEngine

The `TacticalEngine` is responsible for identifying immediate, high-priority moves that can drastically change the game state.

*   **Role**:
    *   **Winning Moves**: Immediately returns a move that leads to a win.
    *   **Blocking Moves**: Identifies and returns a move that blocks an opponent's immediate winning threat (open-four). This includes both `Place` and `Remove` moves (using Jacks).
    *   **Creating Open-Fours**: Identifies and returns a move that creates a new open-four for the current player.
*   **Decision Logic for `Remove` Moves**: When multiple `Remove` moves can block a threat, the engine prioritizes:
    1.  Moves that remove a chip closer to the center of the board (`centerBias`).
    2.  If `centerBias` is equal, it uses the `HeuristicEvaluator.score` as a tie-breaker.

### 3.4 SearchEngine

The `SearchEngine` implements a root-parallel Monte Carlo Tree Search (MCTS) algorithm, designed for games with imperfect information (hidden hands, random draws).

*   **Role**:
    *   Performs multiple "determinizations" (sampling possible hidden information states).
    *   For each determinized "world", it runs a specified number of MCTS simulations.
    *   Aggregates visit counts for root moves across all determinizations.
    *   Selects a move based on these aggregated visit counts, potentially using `temperature` for exploration.
*   **Key Features**:
    *   **Determinizer**: Samples unknown information (opponent hands, deck) to create a "world" for MCTS.
    *   **MctsNode**: Represents a node in the search tree, storing visit counts, total value, and child nodes.
    *   **PUCT Formula**: Used for child selection during MCTS.
    *   **Time Budget & Cancellation**: Incorporates `TimeMark` and `yield()` for responsive execution within a time limit.

### 3.5 HeuristicEvaluator

The `HeuristicEvaluator` provides a fast, rule-based evaluation of game states and moves. It serves as a baseline, a fallback for the neural network, and a scoring mechanism for the `TacticalEngine` and `SearchEngine`'s leaf nodes.

*   **Role**:
    *   Calculates a numerical `score` for any given `Move` in a `GameState`.
    *   **Critical Priority**: Assigns extremely high scores (`1_000_000.0`) to moves that immediately win the game.
    *   **Blocking Priority**: Assigns high scores to moves that block opponent threats.
    *   **Major Priorities**: Scores moves based on creating new sequences, creating new open-fours, and double threats.
    *   **Minor Priorities & Penalties**: Considers factors like extending existing lines, blocking opponent's potential lines, center control (`centerBias`), adjacency to corners, and wasting wild Jacks.
*   **`HeuristicWeights`**: A configurable set of weights used to fine-tune the importance of different heuristic factors.

### 3.6 EvaluationEngine (Interface & Implementations)

This interface decouples the `SearchEngine` from the specific evaluation method, allowing for flexible integration of different models.

*   **`IEvaluationEngine`**: Defines the `evaluate` method, which returns an `EvalResult` containing a state `value` and `policyPriors` for legal moves.
*   **`HeuristicEvaluationEngine`**: An implementation that uses the `HeuristicEvaluator` to provide value and policy priors.
*   **`HybridEvaluationEngine`**: An implementation that attempts to use a `NeuralInference` model first, falling back to `HeuristicEvaluationEngine` if the neural model is unavailable or fails.

### 3.7 NeuralInference (ONNX Runtime)

This component enables the integration of pre-trained neural network models into the AI system across different platforms.

*   **`NeuralInference` Interface**: Defines the `infer` method, which takes an encoded game state and returns `NeuralOutput` (policy logits and value).
*   **`expect fun createNeuralInferenceOrNull()`**: A multiplatform `expect`/`actual` function that provides platform-specific implementations.
    *   **JVM/Android**: `OnnxNeuralInference` uses the ONNX Runtime library to load and run `model_v1.onnx` from resources/assets.
    *   **JS/Wasm**: `UnsupportedNeuralInference` returns `null`, indicating that neural inference is not supported locally on these platforms (they would rely on HTTP calls to a JVM server).

## 4. Data Encoding for Neural Networks

To use neural networks, game states and actions must be converted into numerical tensors.

### 4.1 StateEncoder

The `StateEncoder` converts a `GameState` into a flat `FloatArray` (tensor) suitable for neural network input.

*   **Tensor Shape**: `NCHW` (Channels, Height, Width)
    *   `Channels (C)`: 12
    *   `Height (H)`: 10
    *   `Width (W)`: 10
*   **Encoding Logic**: Each channel represents a specific feature of the board from the perspective of the current player.

### 4.2 ActionCodec

The `ActionCodec` maps game `Move` objects to a flat integer index in the action space, and vice-versa. This is crucial for the neural network's policy head.

*   **`maxActions`**: The total size of the flattened action space (e.g., 10452).
*   **Layout**: `place[card*100+cell] | remove[...] | swap[card]`
*   **`encode(move: Move)`**: Converts a `Move` object into its corresponding integer index.
*   **`legalMask(state: GameState, playerId: PlayerId)`**: Generates a boolean array indicating which actions are legal in the current state, used to mask illegal moves in the neural network's policy output.

### 4.3 Encoder Specification (`encoder_spec.json`)

This JSON file defines the exact structure and meaning of the encoded state and action spaces, ensuring consistency between the Kotlin encoding logic and the Python training scripts.

```json
{
  "encoder_version": 1,
  "channels": 12,
  "height": 10,
  "width": 10,
  "layout": "NCHW",
  "channel_meanings": [
    "me_chips",
    "opp_chips",
    "empty",
    "corners",
    "locked",
    "my_hand_playable",
    "team1_seq_count",
    "team2_seq_count",
    "team3_seq_count",
    "team1_to_win",
    "team2_to_win",
    "deck_size_norm"
  ],
  "max_actions": 10452,
  "action_layout": "place[card*100+cell] | remove[...] | swap[card]"
}
```

## 5. AI Training Workflow

The neural network model is trained offline using self-play data generated by the Kotlin engine, and then exported to ONNX format.

### 5.1 Data Generation (Self-Play)

*   **Tool**: `SelfPlayRunnerTest` (JVM test).
*   **Purpose**: To generate high-quality game data (`state_tensor`, `policy_distribution`, `outcome`) by having the AI play against itself.
*   **Execution**: Run from the project root:
    ```bash
    ./gradlew :core:jvmTest --tests "com.karasuma.fivelinks.fivelinks_cmp.ai.SelfPlayRunnerTest" -Dselfplay.run=true -Dselfplay.games=1000
    ```
*   **Optimized `selfPlay()` Config**: For faster data generation, `DifficultyConfig.selfPlay()` uses lighter parameters:
    *   `maxSimulations = 100`
    *   `timeBudgetMs = 100`
    *   `determinizations = 2`
*   **Output**: `.jsonl` files (e.g., `ml/data/selfplay_dataset.jsonl`) containing `TrainSample` objects.
*   **Monitoring Progress**: Gradle is configured (`testLogging.showStandardStreams = true`) to display real-time progress of game generation.

### 5.2 Training on Google Colab/Kaggle (GPU)

The training process is typically offloaded to cloud environments with GPU support for faster execution.

*   **Environment Setup**:
    1.  Create a new Colab/Kaggle Notebook with GPU runtime enabled (e.g., T4 GPU).
    2.  Clone your Git repository into the environment.
    3.  Upload the generated `selfplay_dataset.jsonl` to `fivelinks-cmp/ml/data/`.
    4.  Install necessary Python libraries (`pip install torch torchvision torchaudio onnx onnxruntime`).
*   **Model**: `TinyPV` (defined in `ml/train/model.py`) is a small policy-value network.
*   **Execution**: From the `fivelinks-cmp/ml/train` directory in Colab:
    ```bash
    python train.py
    ```
*   **Loss Function**: Combines Cross-Entropy for the policy head and Mean Squared Error for the value head.

### 5.3 Exporting to ONNX

After training, the PyTorch model is converted to the ONNX format for cross-platform inference.

*   **Execution**: From the `fivelinks-cmp/ml/train` directory in Colab:
    ```bash
    python export_onnx.py
    ```
*   **Output**: `model_v1.onnx` file (e.g., `ml/data/model_v1.onnx`).
*   **Verification**: The ONNX model should produce identical outputs to the PyTorch model for the same input.

### 5.4 Model Promotion & Evaluation

New models are promoted to production only after demonstrating a clear improvement.

*   **Process**:
    1.  Copy the new `model_v1.onnx` to `core/src/jvmMain/resources/models/` and `core/src/androidMain/assets/models/`.
    2.  Run `AiBenchmarkArenaTest` to compare the new model's performance against the current baseline.
*   **Promotion Criteria**: A new model is typically promoted if it achieves a significant win-rate improvement (e.g., >5%) over a large number of games (e.g., 400+).

## 6. Testing & Benchmarking

A robust testing suite ensures the AI's correctness and performance.

### 6.1 Regression Tests (`AiRegressionTest`)

These tests verify critical tactical behaviors of the AI, ensuring it doesn't regress on fundamental decisions.

*   **Purpose**: To confirm the AI can consistently perform essential moves like winning, blocking, and using special cards.
*   **Test Cases**:
    *   `mustWin_whenOneMoveAway`: AI correctly identifies and executes an immediate winning move.
    *   `mustBlock_whenOpponentHasOpenFour`: AI correctly identifies and executes a move to block an opponent's open-four threat.
    *   `mustUseJack_toRemoveThreat`: AI correctly uses a Jack card to remove an opponent's chip that poses a threat, especially when direct blocking is not possible.
*   **Location**: `core/src/commonTest/kotlin/com/karasuma/fivelinks/fivelinks_cmp/ai/AiRegressionTest.kt`

### 6.2 Performance Benchmarking (`AiBenchmarkArenaTest`)

This test measures the AI's strength (win-rate) and speed (latency) against a known baseline.

*   **Purpose**: To evaluate the effectiveness of new AI models or parameter tunings.
*   **Methodology**: Two AI instances (e.g., Hybrid vs. Heuristic-only) play a series of games against each other.
*   **Metrics**: Win-rate, average latency, P95 latency, and maximum latency.
*   **Location**: `core/src/commonTest/kotlin/com/karasuma/fivelinks/fivelinks_cmp/ai/AiBenchmarkArenaTest.kt`

### 6.3 Current Performance Baseline

After extensive tuning and debugging, the current best-performing AI configuration (Hybrid HARD with `determinizations = 6`) yields the following benchmark results against a Heuristic HARD opponent over 100 games:

*   **Hybrid Wins**: 58%
*   **Heuristic Wins**: 42%
*   **Draws**: 0%
*   **Average Latency (Hybrid AI)**: 542 ms
*   **P95 Latency (Hybrid AI)**: 1372 ms
*   **Max Latency (Hybrid AI)**: 6283 ms

This baseline demonstrates a strong, responsive AI capable of consistently outperforming a heuristic-only approach.

## 7. Future Work & Considerations

*   **Increased Self-Play Data**: To significantly improve the neural network's strength, generating a much larger dataset (e.g., 5,000-10,000 games) through self-play is crucial. This would likely require dedicated compute resources or longer generation times.
*   **Advanced Neural Network Architectures**: Experimenting with more complex or specialized neural network models could yield further performance gains.
*   **Difficulty Tuning**: Further fine-tuning of `DifficultyConfig` parameters (especially for EASY and MEDIUM modes) based on user feedback or more extensive arena testing.
*   **UX Improvements**: Integrating AI debug information (e.g., `AiDebugInfo`) into development tools for better insights during gameplay.
*   **Server-Side Inference**: Optimizing the `/ai/move` HTTP route to efficiently leverage the ONNX Runtime on the JVM server for all clients, including Web/JS.
