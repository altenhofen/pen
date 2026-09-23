---
name: verify-pen
description: Drive the pen Android app on a dedicated emulator and prove the launcher settings, the Calibrate glyph picker and writing canvas, profile export and import pickers, Set as default keyboard, and the pen ink keyboard list entry. Use when changing MainActivity, settings, calibration, profile transfer, the input method, or any user-visible launcher behavior, and before claiming those screens work.
---

# Verify pen

`pen` is an Android app (`io.github.altenhofen.pen`). The launcher activity shows `Pen settings` with toggles for auto-space after full words and recognizing spaces in handwriting, settle and stroke width sliders, a `Finger and passive pen` switch, `My words`, plus `Calibrate`, `Set as default keyboard`, `Export profile`, and `Import profile`. Calibrate opens a glyph picker, then a training canvas. Export and import open system document sheets. Set as default keyboard opens system input-method settings. The same package registers an input method the system lists as `pen ink` under on-screen keyboards. The IME ink canvas is not on the launcher. It appears only after the user enables that input method and focuses a text field in some other app. Unit and instrumented tests exist. They are not a user path.

## Launch

One verification run owns emulator serial `emulator-5556` (AVD `Medium_Phone_API_37.0`, console port `5556`). A second copy of that AVD cannot run beside it. If `adb devices` already lists `emulator-5556` and this run did not start it, stop. Do not install over someone else's session.

From the repo root:

```bash
.cursor/skills/verify-pen/scripts/launch.sh
```

Ready means all of the following.

- `adb -s emulator-5556 shell getprop sys.boot_completed` prints `1`.
- `adb -s emulator-5556 shell pidof io.github.altenhofen.pen` prints a pid.
- A UI dump contains the text `Pen settings`.

The script writes `/tmp/pen-verify/emulator.pid` and `/tmp/pen-verify/serial`. Gradle uses `JAVA_HOME=/opt/android-studio-canary/jbr` and `sdk.dir` from `local.properties` (`/home/altenhofen/Android/Sdk`). The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Doctor

```bash
.cursor/skills/verify-pen/scripts/doctor.sh
```

Read-only. Exit `0` only when the serial is `emulator-5556`, `sys.boot_completed` is `1`, package `io.github.altenhofen.pen` is installed, and `dumpsys package io.github.altenhofen.pen` reports `versionName=1.0`. Run this before every drive when the screen looks wrong.

## Drive

Harness is `adb` on `emulator-5556`. Stable handles are the visible strings `Pen settings`, `Finger and passive pen`, `Calibrate`, `Set as default keyboard`, `Export profile`, `Import profile`, `Select characters to train`, `Start`, `Now writing 0`, `pen-configuration.zip`, and `pen ink`. There are no content descriptions or test tags. Drive against English `values/strings.xml`. `app_name` and `ime_name` are not translated.

```bash
.cursor/skills/verify-pen/scripts/drive-settings.sh
```

The script force-stops the app, starts `io.github.altenhofen.pen/.MainActivity` with `am start -n`, dumps the hierarchy, and screenshots. Proof is the dump containing `text="Pen settings"` after that start, plus the screenshot.

Calibration is a second drive from the same activity.

```bash
.cursor/skills/verify-pen/scripts/drive-calibration.sh
```

Proof is the dump containing `text="Now writing 0"` after a tap on `Calibrate`, a tap on `0`, a tap on `Start`, and a 3 second motionless press on the canvas with no pen crash in `logcat -b crash`, plus the screenshot.

Default keyboard is a third drive from settings.

```bash
.cursor/skills/verify-pen/scripts/drive-default-keyboard.sh
```

Proof is the dump containing `text="pen ink"` after a tap on `Set as default keyboard`, plus the screenshot.

Profile export is a fourth drive from settings.

```bash
.cursor/skills/verify-pen/scripts/drive-profile-export.sh
```

Proof is the dump containing `text="pen-configuration.zip"` after a tap on `Export profile`, plus the screenshot.

Profile import is a fifth drive from settings.

```bash
.cursor/skills/verify-pen/scripts/drive-profile-import.sh
```

Proof is the dump containing `package="com.google.android.documentsui"` after a tap on `Import profile`, plus the screenshot. The picker opens on the last folder used, so do not key on `Recent files`.

The keyboard list is a sixth drive.

```bash
.cursor/skills/verify-pen/scripts/drive-ink-keyboard.sh
```

Proof is the dump containing `text="pen ink"` after `android.settings.INPUT_METHOD_SETTINGS`, plus the screenshot.

Do not call Compose test APIs or `setContent` as a substitute. Do not treat `docs/PRD.md` controls as present unless the matching feature file lists them.

## Evidence

Artifacts stay in `.cursor/skills/verify-pen/artifacts/<feature-id>/`.

- `hierarchy.xml` is the UI dump taken after the user action.
- `screen.png` is the framebuffer after the same action.
- `drive.log` records the `am start` or tap command, its stdout, and its exit code.

A proof shows the action and the resulting state. For settings, the action is launching the activity. The resulting state is the text node `Pen settings` in `hierarchy.xml`.

## Cleanup

```bash
.cursor/skills/verify-pen/scripts/cleanup.sh
```

Kills the emulator pid stored in `/tmp/pen-verify/emulator.pid`, then `adb -s emulator-5556 emu kill` if that serial is still up. Deletes `/tmp/pen-verify`. Leaves `.cursor/skills/verify-pen/artifacts/` in place.

## Helpers

| Script | Role |
| --- | --- |
| `scripts/launch.sh` | Boot `emulator-5556`, install the debug APK, start `MainActivity`, wait until `Pen settings` is in the hierarchy. |
| `scripts/doctor.sh` | Check serial, boot, package, and `versionName=1.0`. |
| `scripts/drive-settings.sh` | Relaunch `MainActivity` and write proof under `artifacts/settings/`. |
| `scripts/drive-calibration.sh` | Open Calibrate, pick `0`, Start, hold the pen still for 3 seconds, and write proof under `artifacts/calibration/`. |
| `scripts/drive-default-keyboard.sh` | Open system IME settings from `Set as default keyboard` and write proof under `artifacts/default-keyboard/`. |
| `scripts/drive-profile-export.sh` | Open the save sheet from `Export profile` and write proof under `artifacts/profile-export/`. |
| `scripts/drive-profile-import.sh` | Open the document picker from `Import profile` and write proof under `artifacts/profile-import/`. |
| `scripts/drive-ink-keyboard.sh` | Open on-screen keyboard settings and write proof under `artifacts/ink-keyboard/`. |
| `scripts/cleanup.sh` | Stop the emulator this run started. Keep artifacts. |
