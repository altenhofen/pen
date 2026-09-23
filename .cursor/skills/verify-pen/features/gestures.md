# Gesture calibration

Gesture training is the top-level `Gestures` button on Pen settings (next to `My words`). The user picks text actions such as `Delete last word` or `Undo last gesture`. Each action needs at least five drawings before `Next` enables. Chips show sample progress like `Delete last word (0/5)`. Trained gestures are stored in the profile backup and run in the IME before glyph or word recognition when the match gap is confident.

## Sub-features

- `gestures-entry` shows `Gesture actions` after the user taps `Gestures` on Pen settings.
- `gestures-picker` shows `Select actions to train` on that screen.

## How to get to it (user POV)

- Open pen, tap `Gestures` on the settings screen.

## Driving it with adb

Preconditions match settings in `features/settings.md`.

- Run `.cursor/skills/verify-pen/scripts/drive-calibration-gestures.sh`.
- Proof is `artifacts/gestures/hierarchy.xml` containing `text="Gesture actions"` and `text="Select actions to train. Each needs at least 5 drawings."`.

## Gotchas

- `Calibrate` is only for glyph templates. Do not open it to reach gesture training.
- `Start` needs at least one action selected.
