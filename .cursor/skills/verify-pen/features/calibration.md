# Calibration

Calibration is the training minigame behind `Calibrate` on Pen settings. It first shows a grid of `0-9`, `A-Z`, and `a-z`. The user selects glyphs, then taps `Start`. The writing canvas fills the screen and shows the current character as a large watermark. The user may draw any number of examples. After each drawing settles, the canvas clears like the IME ink canvas and that stroke becomes a sample saved right away. `Next` moves on once at least one sample exists for the current glyph. Saved samples add to earlier calibrations for the same glyph instead of replacing them. `Cancel` keeps every glyph already confirmed with `Next`. Rotating the screen keeps the session. That canvas is not the IME.

## Sub-features

- `calibration-picker` shows `Select characters to train` after the user taps `Calibrate`.
- `calibration-write-open` shows `Now writing 0` after the user selects `0` and taps `Start`.
- `calibration-hold-still` keeps `Now writing 0` on screen after the user holds the pen still on the canvas for 3 seconds. A motionless press records no sample.

## How to get to it (user POV)

- Open pen, then tap `Calibrate` on the settings screen, pick at least one glyph, then tap `Start`.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Calibrate"`.
- **Open calibration.** The user taps `Calibrate`, taps `0`, then taps `Start`. Run `.cursor/skills/verify-pen/scripts/drive-calibration.sh`. The script taps the innermost clickable ancestor of each of those labels.
- **Hold still.** The script presses one spot for 3 seconds, then fails if the crash log buffer names `io.github.altenhofen.pen`.
- **Read the prompt.** The screen still shows `Now writing 0`. `artifacts/calibration/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the innermost clickable ancestor of `Calibrate`. A regex from the first clickable node can hit the wrong button.
- `Start` is not clickable until at least one glyph is selected. Tap `0` first.
- Finger strokes work on this canvas. The IME canvas rejects fingers until the user turns on `Finger and passive pen` on Pen settings.
- Completing ink and `Next` is optional for verification. Proof is the write screen after Start.
- This screen is not the IME. Do not look for `pen ink` here.
