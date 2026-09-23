# Profile export

Profile export is the share path behind `Export profile` on Pen settings. Tapping the button opens a passphrase dialog (`Protect this export`). After the user enters matching passphrases of at least 8 characters and taps `Encrypt and save`, the system save sheet opens with the suggested name `pen-profile.penbak`. Completing SAVE writes an encrypted vault archive (format v4 body with custom words and gestures). Legacy v1 and v2 zip imports still work from `Import profile`. This screen is not the IME.

## Sub-features

- `profile-export-passphrase` shows `Protect this export` after the user taps `Export profile`.
- `profile-export-open` shows `pen-profile.penbak` on the save sheet after the passphrase step.

## How to get to it (user POV)

- Open pen, then tap `Export profile` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Export profile"`.
- **Open export.** The user taps `Export profile`. Run `.cursor/skills/verify-pen/scripts/drive-profile-export.sh`. The script taps the clickable parent of that text node, fills the passphrase dialog, and opens the save sheet.
- **Read the passphrase step.** `artifacts/profile-export/hierarchy.xml` contains `text="Protect this export"` before the save sheet.
- **Read the save sheet.** The picker shows `pen-profile.penbak`. The hierarchy dump after the passphrase step contains that exact `text=` value.

## Gotchas

- Tap the clickable parent of `Export profile`, not `Import profile` below it.
- Export always asks for a passphrase first. The drive script uses a fixed test passphrase only on the emulator.
- The save sheet is DocumentsUI, not Pen settings. Proof is `pen-profile.penbak`, not `Exported`. `Exported` appears only after SAVE.
- The script sends Back after the dump so the picker does not linger.
