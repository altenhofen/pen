# Ink keyboard

After pen is installed, Android keyboard settings lists an input method named `pen ink`. The row is off until the user turns it on. The launcher screen does not show the ink canvas.

## Sub-features

- `ink-keyboard-listed` shows `pen ink` on the system on-screen keyboard list.

## How to get to it (user POV)

- Open Settings, then System, then Keyboards, then On-screen keyboard. The activity intent is `android.settings.INPUT_METHOD_SETTINGS`.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- The debug build includes `PenInputMethodService`.

- **Open keyboard settings.** The user opens the on-screen keyboard list. Run `adb -s emulator-5556 shell am start -a android.settings.INPUT_METHOD_SETTINGS`. Exit code `0`. Stdout contains `act=android.settings.INPUT_METHOD_SETTINGS`.
- **Read the row.** The list shows `pen ink`. Run `adb -s emulator-5556 exec-out uiautomator dump /dev/tty`. The dump contains `text="pen ink"`.
- **Proof.** Save the dump and a screenshot. Run `.cursor/skills/verify-pen/scripts/drive-ink-keyboard.sh`. `artifacts/ink-keyboard/hierarchy.xml` contains `text="pen ink"` and `artifacts/ink-keyboard/screen.png` shows the same words.

## Gotchas

- The ink canvas appears only while this input method is selected and some other app has a focused text field. `MainActivity` has no text field, so the greeting screen never shows the canvas.
- Match `text="pen ink"`. The launcher label is `pen`.
- `uiautomator dump` with no path writes `/sdcard/window_dump.xml` on the device. Proof must use `exec-out uiautomator dump /dev/tty`.
