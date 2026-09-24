# Settings

Settings is the first screen after the user opens pen. Under the heading `Pen settings`, outlined cards run in this order. The first card holds `Calibrate` and `Set as default keyboard`. The second holds `My words` and `Gestures`. The third holds `Add space after full word`, `Space after suggestion pick` with Off/On/Smart (Off on a fresh install), `Recognize spaces in handwriting`, the settle and stroke sliders, `Finger and passive pen`, and `Double-tap for space`. Language pickers sit in the next card. `Export profile` and `Import profile` are outlined buttons in the last card, below the fold on a phone.

## Sub-features

- `settings-launch` shows `Pen settings` after the user opens the app from the launcher.
- `settings-toggles` shows `Add space after full word` and `Recognize spaces in handwriting` in the motor card, under the action cards.
- `settings-space-after-suggestion` shows `Space after suggestion pick` with the selected value `Off` on a fresh install.
- `settings-sliders` shows `Settle window` and `Stroke width` on that same screen.
- `settings-finger-input` shows the `Finger and passive pen` switch (off by default on a fresh install).
- `settings-double-tap-space` shows the `Double-tap for space` switch (on by default on a fresh install).
- `settings-my-words-entry` shows the `My words` button.
- `settings-gestures-entry` shows the `Gestures` button.
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
- **Read the suggestion space picker.** The dump contains `text="Space after suggestion pick"` and a selected `text="Off"`.
- **Read the sliders.** The dump contains `text="Settle window: 600 ms"` and `text="Stroke width: 6.0 dp"` on a fresh install.
- **Proof.** Run `.cursor/skills/verify-pen/scripts/drive-settings.sh`.

## Gotchas

- The motor card is taller with `Space after suggestion pick`. Swipe before grepping `Finger and passive pen`, `Double-tap for space`, `Export profile`, or `Import profile`.
- Slider labels include the live value. Match `Settle window` rather than a frozen `600 ms` on an AVD that already has prefs.
- The ambiguity threshold slider was removed; do not grep for it.
- `Export profile` and `Import profile` sit below the fold. Swipe the settings list before grepping those labels.
