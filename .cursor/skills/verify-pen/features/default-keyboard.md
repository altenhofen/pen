# Default keyboard

Default keyboard is the shortcut behind `Set as default keyboard` on Pen settings. Tapping the button starts `android.settings.INPUT_METHOD_SETTINGS`. The system list includes `pen ink`. The app cannot mark itself default. The user completes enablement on that system screen.

## Sub-features

- `default-keyboard-open` shows `pen ink` on the system input-method list after the user taps `Set as default keyboard`.

## How to get to it (user POV)

- Open pen, then tap `Set as default keyboard` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Set as default keyboard"`.
- **Open keyboard settings.** The user taps `Set as default keyboard`. Run `.cursor/skills/verify-pen/scripts/drive-default-keyboard.sh`. The script taps the innermost clickable ancestor of that text node.
- **Read the row.** The list shows `pen ink`. `artifacts/default-keyboard/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the innermost clickable ancestor of `Set as default keyboard`. A greedy match from the first clickable node can hit `Calibrate` instead.
- This is the same system screen as `drive-ink-keyboard.sh`. The difference is the entry point. The settings button, not a raw `am start -a`.
- `ime_name` is `translatable="false"`, so the row stays `pen ink` even in pt or es.
