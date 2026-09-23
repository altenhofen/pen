# Gesture calibration

Gesture training lives on the `Gestures` tab inside `Calibrate` on Pen settings. The tab row shows `Glyphs` and `Gestures`. On `Gestures`, the user picks text actions such as `Delete last word` or `Undo last gesture`. Each action needs at least five drawings before `Next` enables. Chips show sample progress like `Delete last word (0/5)`. Trained gestures are stored in the profile backup and run in the IME before glyph or word recognition when the match gap is confident.

## Sub-features

- `gestures-tab` shows `Gesture actions` after the user taps `Calibrate`, then `Gestures`.
- `gestures-picker` shows `Select actions to train` on the gestures tab.

## How to get to it (user POV)

- Open pen, tap `Calibrate`, then tap the `Gestures` tab.

## Driving it with adb

Preconditions match calibration in `features/calibration.md`.

- Run `.cursor/skills/verify-pen/scripts/drive-calibration-gestures.sh`.
- Proof is `artifacts/gestures/hierarchy.xml` containing `text="Gesture actions"` and `text="Select actions to train. Each needs at least 5 drawings."`.

## Gotchas

- The `Gestures` tab is a sibling of `Glyphs`. Glyph picker strings are not on this tab.
- `Start` needs at least one action selected.
