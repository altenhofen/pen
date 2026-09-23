# Character fine-tune

Character fine-tune is the training screen behind `Fine-tune 0-9 and a-z` on Pen settings. It walks the user through three drawn samples per character for digits `0` through `9` and lowercase `a` through `z`, on the same in-app canvas as calibration. Each sample becomes its own prototype cluster so recognition can match several shapes per letter.

## Sub-features

- `fine-tune-open` shows `Draw 0: 0 of 3 samples collected` after the user taps `Fine-tune 0-9 and a-z`.

## How to get to it (user POV)

- Open pen, then tap `Fine-tune 0-9 and a-z` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Fine-tune 0-9 and a-z"`.
- **Open fine-tune.** The user taps `Fine-tune 0-9 and a-z`. Run `.cursor/skills/verify-pen/scripts/drive-character-fine-tune.sh`. The script taps the clickable parent of that text node.
- **Read the prompt.** The screen shows `Draw 0: 0 of 3 samples collected`. `artifacts/character-fine-tune/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the clickable parent of `Fine-tune 0-9 and a-z`, not the calibrate button above it. `drive-character-fine-tune.sh` resolves the innermost clickable ancestor of the label text. A regex that spans from the first clickable node can hit `Calibrate 8 and 9` instead.
- Finger strokes work on this canvas. The IME canvas still rejects fingers.
- Completing all characters is optional for verification. Proof is only the first prompt after open.
- This screen is not the IME. Do not look for `pen ink` here.
