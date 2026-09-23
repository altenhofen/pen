# Settings

Settings is the first screen after the user opens pen. It shows the heading `Pen settings`, toggles for `Add space after full word` and `Recognize spaces in handwriting`, sliders for settle window and stroke width, a `Finger and passive pen` switch, a `My words` button, a `Calibrate` button, a `Set as default keyboard` button, an `Export profile` button, and an `Import profile` button.

## Sub-features

- `settings-launch` shows `Pen settings` after the user opens the app from the launcher.
- `settings-toggles` shows `Add space after full word` and `Recognize spaces in handwriting` at the top of the screen.
- `settings-sliders` shows `Settle window` and `Stroke width` on that same screen.
- `settings-finger-input` shows the `Finger and passive pen` switch (off by default on a fresh install).
- `settings-my-words-entry` shows the `My words` button.
- `settings-calibrate-entry` shows the `Calibrate` button.
- `settings-default-keyboard-entry` shows the `Set as default keyboard` button.
- `settings-export-entry` shows the `Export profile` button.
- `settings-import-entry` shows the `Import profile` button.

## How to get to it (user POV)

- Open the `pen` icon in the app drawer. The activity is `io.github.altenhofen.pen/.MainActivity`.
- Open the pen ink row in system keyboard settings. `method.xml` sets `android:settingsActivity` to `io.github.altenhofen.pen.MainActivity`.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.

- **Open the app.** Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`.
- **Read the heading.** The dump contains `text="Pen settings"`.
- **Read the toggles.** The dump contains `text="Add space after full word"` and `text="Recognize spaces in handwriting"`.
- **Read the sliders.** The dump contains `text="Settle window: 600 ms"` and `text="Stroke width: 6.0 dp"` on a fresh install.
- **Proof.** Run `.cursor/skills/verify-pen/scripts/drive-settings.sh`.

## Gotchas

- Slider labels include the live value, so match the full `Settle window: 600 ms` string on a default install, or grep `Settle window`.
- The ambiguity threshold slider was removed; do not grep for it.
