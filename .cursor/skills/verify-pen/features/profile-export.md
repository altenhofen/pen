# Profile export

Profile export is the share path behind `Export profile` on Pen settings. Tapping the button opens the system save document sheet with the suggested name `pen-configuration.zip`. Completing SAVE writes a zip whose required entry is `profile.json`. This screen is not the IME.

## Sub-features

- `profile-export-open` shows `pen-configuration.zip` on the save sheet after the user taps `Export profile`.

## How to get to it (user POV)

- Open pen, then tap `Export profile` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Export profile"`.
- **Open export.** The user taps `Export profile`. Run `.cursor/skills/verify-pen/scripts/drive-profile-export.sh`. The script taps the clickable parent of that text node.
- **Read the save sheet.** The picker shows `pen-configuration.zip`. `artifacts/profile-export/hierarchy.xml` contains that exact `text=` value.

## Gotchas

- Tap the clickable parent of `Export profile`, not `Import profile` below it.
- The save sheet is DocumentsUI, not Pen settings. Proof is `pen-configuration.zip`, not `Exported`. `Exported` appears only after SAVE.
- The script sends Back after the dump so the picker does not linger.
