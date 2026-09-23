# Profile import

Profile import is the restore path behind `Import profile` on Pen settings. Tapping the button opens the system open-document picker. Choosing a `pen-profile.penbak` or legacy zip asks for a passphrase when the file is encrypted. A successful import replaces motor settings, prototype clusters, and word memory. This screen is not the IME.

## Sub-features

- `profile-import-open` shows the system document picker (`com.google.android.documentsui`) after the user taps `Import profile`.

## How to get to it (user POV)

- Open pen, then tap `Import profile` on the settings screen.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.
- Settings is showing. Start `MainActivity` first if it is not.

- **Open settings.** The user opens pen. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Wait until a dump contains `text="Import profile"`.
- **Open import.** The user taps `Import profile`. Run `.cursor/skills/verify-pen/scripts/drive-profile-import.sh`. The script taps the clickable parent of that text node.
- **Read the picker.** The picker is in front. `artifacts/profile-import/hierarchy.xml` contains `package="com.google.android.documentsui"`.

## Gotchas

- Tap the clickable parent of `Import profile`, not `Export profile` above it.
- Picker chrome is OEM DocumentsUI. It opens on the last folder used, so `Recent files` shows only on a fresh device. After the export drive it opens on `Downloads`. Proof keys on the package, not a folder title.
- Completing a file pick is optional for this proof. `Imported` appears only after a chosen zip decodes.
- The script sends Back after the dump so the picker does not linger.
