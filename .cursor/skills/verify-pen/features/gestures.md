# Gesture calibration

Gesture training is the top-level `Gestures` button on Pen settings (next to `My words`). The user picks text actions such as `Delete last word` or `Undo last gesture`. Each action needs at least five drawings before `Next` enables. After each drawing settles, the canvas clears like the IME ink canvas and that stroke is saved right away so the IME can pick it up without leaving the training screen. Chips show sample progress like `Delete last word (0/5)`. `Clear selected` sits beside `Select all` and `Select none`. It is enabled when at least one selected action has saved samples and removes training for those actions only. Trained gestures are stored in the profile backup. In the IME they run after the home-screen settle timer, on that same settle, with no extra merge or delete-line wait. Draw multi-stroke gestures such as `//` in one canvas settle. A match still needs the action gap, max-fire distance, and a DTW score at least as close as the best glyph template. Gesture prototypes also require compatible stroke count, open-versus-closed path, and corner count. A triangle does not match a 5.

## Sub-features

- `gestures-entry` shows `Gesture actions` after the user taps `Gestures` on Pen settings.
- `gestures-picker` shows `Select actions to train` on that screen.
- `gestures-clear-selected` shows `Clear selected` when a selected chip has saved samples.

## How to get to it (user POV)

- Open pen, tap `Gestures` on the settings screen.

## Driving it with adb

Preconditions match settings in `features/settings.md`.

- Run `.cursor/skills/verify-pen/scripts/drive-calibration-gestures.sh`.
- Proof is `artifacts/gestures/hierarchy.xml` containing `text="Gesture actions"` and `text="Select actions to train. Each needs at least 5 drawings."`.

## Gotchas

- `Calibrate` is only for glyph templates. Do not open it to reach gesture training.
- `Start` needs at least one action selected.
- Gesture training ink uses the same pointer rules as the IME ink canvas. A passive pen is accepted when `Finger and passive pen` is off, including when the device reports finger with no contact size. Wide finger touches still need that toggle.
- Draw both strokes of `//` in one settle. The IME does not wait past the home-screen settle timer for a second slash.
- A matched gesture never falls through to handwriting, even when the action cannot run on the current field.
- Training several similar gestures can shrink the gap between the top two actions and stop firing until you clear or retrain the closest rival.
- Live harness covers the picker only. Proving a trained gesture in the IME needs pen ink enabled in another app and is out of scope for adb drives here.
