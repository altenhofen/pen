# Product Requirements Document (PRD)

## Project Name: AdaptiveStroke IME (Assistive Pen-Input Keyboard)

---

## 1. Executive Summary & Objective

### 1.1 Problem Statement

Standard mobile keyboards and commercial on-device OCR engines fail individuals with fine-motor impairments (tremors, irregular stroke velocity, or non-standard glyph geometry). For instance, an open loop on a handwritten `9` is routinely misclassified as an `8`, causing high typing fatigue, repetitive correction cycles, and frustration.

### 1.2 Target User Persona

* **Capabilities:** Fully literate; capable of reading, composing sentences, and targeting UI elements with a stylus.
* **Constraints:** Fine-motor control deficits; produces tremors, variable stroke pressure, and accidental lift-offs (pen-up jitter). Cannot reliably tap small virtual keys.
* **Goal:** Write naturally with an active or passive pen/stylus on an Android tablet or phone, having personal handwriting variations accurately translated into characters and words in real time.

### 1.3 Solution Vision

A custom Android `InputMethodService` (IME) featuring a full-width ink canvas that matches stroke trajectories against an **adaptive personalized prototype bank**. The system relies on **Contextual Bandit Reinforcement Learning** driven by implicit user signals (commit vs. backspace/re-select) to continuously tune classification boundaries to the user's specific motor output.

---

## 2. Core Architecture & Signal Flow

```
[ Stylus Down / Move / Up ]
         │
         ▼
[ Stroke Ingestion & Segmentation ]
  - Filter micro-jitter / debouncing
  - Settle window (e.g., 600 ms post-pen-up)
         │
         ▼
[ Normalization & Resampling Engine ]
  - Spatial bounding-box scaling (0.0 to 1.0)
  - Equidistant arc-length resampling (N = 32 points)
  - Delta feature extraction (dx, dy, stroke_state)
         │
         ▼
[ Dual-Tier Classification Engine ]
  ├─ Tier 1: User's Personalized Vector Prototypes (Distance / Metric Match)
  └─ Tier 2: Base Fallback Model (Pre-trained On-Device Classifier)
         │
         ▼
[ Top-K Candidate Generation (K=3) ]
  - Emit Rank 1 to target input field via InputConnection
  - Render Rank 1, 2, and 3 onto High-Affordance Suggestion Strip
         │
         ▼
[ Implicit Feedback & Bandit Update Loop ]
  ├─ User types next glyph / taps Rank 1 ──► Reward = +1.0 (Attract prototype)
  ├─ User taps Rank 2 or 3               ──► Reward = +0.5 / Reposition winner
  └─ User taps Backspace within 3s       ──► Reward = -1.0 (Repel misclassified prototype)

```

---

## 3. Functional Requirements

### 3.1 Input Capture & Ink Canvas

* **System Service:** Must extend `android.inputmethodservice.InputMethodService`.
* **Stylus Isolation:** Filter inputs where `MotionEvent.getToolType(0) == TOOL_TYPE_STYLUS` to reject accidental palm contact.
* **Low-Latency Rendering:** Real-time visual feedback using hardware-accelerated `Canvas` or AndroidX Ink API.
* **Temporal Settle Window (Debounce):** Motor control deficits frequently cause unintended micro pen-lifts mid-glyph. The canvas must enforce a configurable commit window (default: `600 ms`, range: `300–1200 ms`) before treating a stroke sequence as finished.

### 3.2 Preprocessing Pipeline

Every finalized stroke sequence must be normalized prior to inference:

1. **Resampling:** Convert raw, variable-density coordinate streams into a fixed array of $N = 32$ equidistant points along the trajectory arc-length.
2. **Spatial Normalization:** Min-max scale the $(x, y)$ coordinates into a unit bounding box $[0, 1] \times [0, 1]$ preserving aspect ratio.
3. **Feature Construction:** Vectorize into an input tensor of size $32 \times 3$:

$$\mathbf{f}_i = (\Delta x_i, \Delta y_i, p_i)$$



where $\Delta x_i = x_i - x_{i-1}$, $\Delta y_i = y_i - y_{i-1}$, and $p_i \in \{0, 1\}$ indicates pen-lift transitions for multi-stroke characters.

### 3.3 Classification Engine

* **Vocabulary Scope:** Alphanumeric (`a-z`, `A-Z`, `0-9`), basic punctuation (`.`, `,`, `?`, `!`, `-`), and common control gestures (space, backspace, enter).
* **Prototype Store:** Local SQLite database storing reference feature vectors for each class:

$$\mathcal{P}_c = \{ \mathbf{p}_c^{(1)}, \mathbf{p}_c^{(2)}, \dots \}$$


* **Distance Metric:** Dynamic Time Warping (DTW) or normalized Euclidean distance between input $\mathbf{s}$ and stored prototypes $\mathbf{p}$:

$$D(\mathbf{s}, \mathbf{p}) = \frac{1}{N} \sum_{i=1}^{N} \Vert{}\mathbf{s}_i - \mathbf{p}_i\Vert{}_2$$


* **Inference Budget:** Maximum end-to-end latency $\le 25\text{ ms}$ on mid-range Android hardware.

### 3.4 Reinforcement Learning (Contextual Bandit Adaptation)

To avoid manual calibration cycles, the engine adapts on-device using implicit user actions:

| User Action | Detected State | Reward ($R$) | Policy Update |
| --- | --- | --- | --- |
| Writes next character | Accepted Rank 1 prediction | $+1.0$ | Move winning prototype toward input vector |
| Taps Rank 2 or 3 suggestion | Corrected prediction | $+1.0$ to chosen, $-0.8$ to Rank 1 | Attract chosen class prototype; repel false-positive prototype |
| Hits Backspace ($\le 3.0\text{s}$) | Rejection of last commit | $-1.0$ | Repel false-positive prototype away from input vector |

#### Mathematical Update Rule:

For winning/accepted prototype $\mathbf{p}_w$ with input $\mathbf{s}$ and learning rate $\alpha = 0.05$:


$$\mathbf{p}_w \leftarrow \mathbf{p}_w + \alpha \cdot R \cdot (\mathbf{s} - \mathbf{p}_w)$$

For misclassified prototype $\mathbf{p}_m$ when corrected to target class $t$:


$$\mathbf{p}_m \leftarrow \mathbf{p}_m - \beta \cdot (\mathbf{s} - \mathbf{p}_m) \quad (\text{repulsion, where } \beta = 0.02)$$

$$\mathbf{p}_t \leftarrow \mathbf{p}_t + \alpha \cdot (\mathbf{s} - \mathbf{p}_t) \quad (\text{attraction})$$

---

## 4. UI/UX Specifications

```
+-------------------------------------------------------------+
| [Esc]  |  "9" (Rank 1)  |  "8" (Rank 2)  |  "g" (Rank 3)  |  [⌫]  | <- Top Bar (Min 48dp height)
+-------------------------------------------------------------+
|                                                             |
|                                                             |
|                    STYLUS DRAWING CANVAS                    |
|                (Low-latency stroke trail)                   |
|                                                             |
|                                                             |
+-------------------------------------------------------------+
| [Mode: 123]   [Space / Commit]   [Clear]   [Enter / Action] | <- Bottom Action Bar
+-------------------------------------------------------------+

```

* **Touch Targets:** All buttons must have a minimum touch target size of $56 \times 56\text{ dp}$ with minimum $12\text{ dp}$ margins to prevent mis-clicks.
* **Suggestion Bar:** Displays top 3 candidate predictions prominently. Tapping any candidate overrides the text field via `InputConnection.setComposingText()` / `commitText()`.
* **Visual Disambiguation Indicator:** When the distance between Rank 1 and Rank 2 is below an ambiguity threshold ($\delta \le 0.15$), highlight both candidates on the ribbon to prompt an intentional choice without interrupting workflow.

---

## 5. Technical Stack

| Layer | Technology | Purpose |
| --- | --- | --- |
| **Platform** | Android SDK (API Level 26+), Kotlin | Base OS integration |
| **System Service** | `android.inputmethodservice.InputMethodService` | Keyboard lifecycle & text injection |
| **Stylus & Ink** | Android `MotionEvent` + Custom Hardware-Accelerated `View` | Stroke capture and visual ink rendering |
| **Local Storage** | Android Jetpack Room (SQLite) | Storing and updating user prototype vectors |
| **Math & Matrix Ops** | Native Kotlin / C++ via JNI (optional for scale) | Resampling, vector normalization, and DTW distance |
| **Serialization** | Protocol Buffers or Kotlinx Serialization | Backup and restore of user adaptation profiles |

---

## 6. Implementation Milestones

### Phase 1: Barebones IME & Raw Stroke Capture

* Implement `InputMethodService` registering correctly in Android System Settings.
* Build full-screen `DrawingCanvasView` tracking `ACTION_DOWN`, `ACTION_MOVE`, `ACTION_UP`.
* Enforce stylus-only event filtering.

### Phase 2: Signal Normalization & Initial Seed Bank

* Implement arc-length equidistant point resampling ($N = 32$).
* Seed initial prototype bank for numbers `0-9` and letters `a-z` using standard clean handwriting samples.
* Implement Euclidean / DTW matching to output single-character predictions to `InputConnection.commitText()`.

### Phase 3: Suggestion Bar & Bandit Adaptation Loop

* Construct candidate ribbon displaying top 3 inferences.
* Implement implicit reward tracking:
* Listener on standard Backspace key.
* Timeout listener on subsequent stroke inputs.


* Implement vector update rules (attraction/repulsion) persisting to Room DB.

### Phase 4: Ergonomics & Motor Tuning

* Add user-configurable settings: Settle Window slider ($300\text{ ms} - 1200\text{ ms}$), Stroke Width, and Ambiguity Threshold.
* Add manual calibration screen allowing the user to explicitly draw problematic characters (e.g., 5 samples of `9` vs `8`) to seed distinct prototype clusters.

### Phase 5: fine tuning based on characters

### Phase 6: data and configuration export

---

## 7. Verification & Success Metrics

* **Target Character Accuracy:** $\ge 95\%$ recognition accuracy on ambiguous pairs (`9` vs `8`, `1` vs `l`, `0` vs `O`) after 10 user corrections.
* **Typing Speed Efficiency:** Reduction in backspace-per-character ratio by $\ge 60\%$ compared to the default Google Gboard handwriting keyboard.
* **Input-to-Commit Latency:** Under $30\text{ ms}$ elapsed between settle-window expiration and character insertion on the target text field.