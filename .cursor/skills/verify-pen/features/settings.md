# Settings

Settings is the first screen after the user opens pen. It shows the heading `Pen settings`, sliders for settle window, stroke width, and ambiguity threshold, and a `Calibrate 8 and 9` button.

## Sub-features

- `settings-launch` shows `Pen settings` after the user opens the app from the launcher.
- `settings-sliders` shows `Settle window`, `Stroke width`, and `Ambiguity threshold` on that same screen.
- `settings-calibrate-entry` shows the `Calibrate 8 and 9` button.

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
- **Proof.** Save the dump and a screenshot. Run `.cursor/skills/verify-pen/scripts/drive-settings.sh`. `artifacts/settings/hierarchy.xml` contains `text="Pen settings"` and `artifacts/settings/screen.png` shows the same words.

## Gotchas

- `uiautomator dump` with no path writes `/sdcard/window_dump.xml` on the device. Proof must use `exec-out uiautomator dump /dev/tty` or pull that file. A screenshot alone can miss the text node.
- Match `text="Pen settings"`. There is no `Hello Android!` text.
- Slider labels include the live value, so `Settle window` alone is not the node text. Match the full `Settle window: 600 ms` string on a default install, or grep `Settle window`.
- The IME ink canvas is a separate system keyboard. It is not on this screen. Calibration is a later screen behind `Calibrate 8 and 9`.
- Driving a serial other than `emulator-5556` can hit the user's own emulator. Doctor must pass first.
