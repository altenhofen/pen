# Settings

Settings is the first screen after the user opens pen. It shows the heading `Pen settings`, sliders for settle window, stroke width, and ambiguity threshold, a `Finger and passive pen` switch that enables finger and passive-pen input on the IME ink canvas, a `Calibrate` button, a `Set as default keyboard` button, an `Export profile` button, and an `Import profile` button.

## Sub-features

- `settings-launch` shows `Pen settings` after the user opens the app from the launcher.
- `settings-sliders` shows `Settle window`, `Stroke width`, and `Ambiguity threshold` on that same screen.
- `settings-finger-input` shows the `Finger and passive pen` switch (off by default on a fresh install).
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

- **Open the app.** The user taps the `pen` launcher icon. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Stdout contains `Starting: Intent { cmp=io.github.altenhofen.pen/.MainActivity }`.
- **Read the heading.** The screen shows `Pen settings`. Run `adb -s emulator-5556 exec-out uiautomator dump /dev/tty`. The dump contains `text="Pen settings"`.
- **Read the sliders.** The dump also contains `text="Settle window: 600 ms"`, `text="Stroke width: 6.0 dp"`, and `text="Ambiguity threshold: 0.15"` on a fresh install.
- **Read finger input.** The dump contains `text="Finger and passive pen"`. The switch is unchecked on a fresh install.
- **Read the actions.** The dump also contains `text="Calibrate"`, `text="Set as default keyboard"`, `text="Export profile"`, and `text="Import profile"`.
- **Proof.** Save the dump and a screenshot. Run `.cursor/skills/verify-pen/scripts/drive-settings.sh`. `artifacts/settings/hierarchy.xml` contains `text="Pen settings"` and `artifacts/settings/screen.png` shows the same words.

## Gotchas

- `uiautomator dump` with no path writes `/sdcard/window_dump.xml` on the device. Proof must use `exec-out uiautomator dump /dev/tty` or pull that file. A screenshot alone can miss the text node.
- Match `text="Pen settings"`. There is no `Hello Android!` text.
- Slider labels include the live value, so `Settle window` alone is not the node text. Match the full `Settle window: 600 ms` string on a default install, or grep `Settle window`.
- There is no `Calibrate 8 and 9` or `Fine-tune 0-9 and a-z` button.
- The IME ink canvas is a separate system keyboard. It is not on this screen. Calibration is a later screen behind `Calibrate`. Turning on `Finger and passive pen` only affects the IME after the keyboard is shown again (refocus a text field or dismiss and reopen the keyboard).
- Driving a serial other than `emulator-5556` can hit the user's own emulator. Doctor must pass first.
- After `Set as default keyboard`, the system IME screen stays on top. Drive scripts start MainActivity with `-f 0x10008000` so the next recipe actually shows Pen settings.
