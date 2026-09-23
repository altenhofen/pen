# Calibration

Calibration is the training screen behind `Calibrate 8 and 9` on Pen settings. It asks the user to draw five samples of `8`, then five of `9`, on an in-app canvas. That canvas is not the IME.

## Sub-features

- `calibration-open` shows `Draw 8: 0 of 5 samples collected` after the user taps `Calibrate 8 and 9`.

## How to get to it (user POV)

- Open pen, then tap `Calibrate 8 and 9` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Calibrate 8 and 9"`.
- **Open calibration.** The user taps `Calibrate 8 and 9`. Run `.cursor/skills/verify-pen/scripts/drive-calibration.sh`. The script taps the clickable parent of that text node.
- **Read the prompt.** The screen shows `Draw 8: 0 of 5 samples collected`. `artifacts/calibration/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the clickable parent of `Calibrate 8 and 9`, not only the text bounds. `drive-calibration.sh` taps that point until the dump contains `text="Draw 8: 0 of 5 samples collected"`. A single tap can miss.
- Finger strokes work on this canvas. The IME canvas still rejects fingers.
- The training canvas uses the cream ink background. Proof is the hierarchy text, not whether the heading is easy to see in a screenshot.
- This screen is not the IME. Do not look for `pen ink` here.
