# Calibration

Calibration is the training minigame behind `Calibrate` on Pen settings. It first shows a grid of `0-9`, `A-Z`, and `a-z`. The user selects glyphs, then taps `Start`. The writing canvas asks for three samples of the first selected glyph. `Next` stays disabled until those three samples exist. That canvas is not the IME.

## Sub-features

- `calibration-picker` shows `Select characters to train` after the user taps `Calibrate`.
- `calibration-write-open` shows `Draw 0: 0 of 3 samples collected` after the user selects `0` and taps `Start`.

## How to get to it (user POV)

- Open pen, then tap `Calibrate` on the settings screen, pick at least one glyph, then tap `Start`.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Calibrate"`.
- **Open calibration.** The user taps `Calibrate`, taps `0`, then taps `Start`. Run `.cursor/skills/verify-pen/scripts/drive-calibration.sh`. The script taps the innermost clickable ancestor of each of those labels.
- **Read the prompt.** The screen shows `Draw 0: 0 of 3 samples collected`. `artifacts/calibration/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the innermost clickable ancestor of `Calibrate`. A regex from the first clickable node can hit the wrong button.
- `Start` is not clickable until at least one glyph is selected. Tap `0` first.
- Finger strokes work on this canvas. The IME canvas still rejects fingers.
- Completing three samples and `Next` is optional for verification. Proof is the first write prompt after Start.
- This screen is not the IME. Do not look for `pen ink` here.
