---
name: verify-pen
description: Drive the pen Android app on a dedicated emulator and prove the launcher greeting and the pen ink keyboard list entry. Use when changing MainActivity, the Compose greeting, the input method, or any user-visible launcher behavior, and before claiming those screens work.
---

# Verify pen

`pen` is an Android app (`io.github.altenhofen.pen`). The launcher activity draws the text `Hello Android!`. The same package registers an input method the system lists as `pen ink` under on-screen keyboards. The ink canvas is not on the launcher screen. It appears only after the user enables that input method and focuses a text field in some app. Unit and instrumented tests exist. They are not a user path.

## Launch

One verification run owns emulator serial `emulator-5556` (AVD `Medium_Phone_API_37.0`, console port `5556`). A second copy of that AVD cannot run beside it. If `adb devices` already lists `emulator-5556` and this run did not start it, stop. Do not install over someone else's session.

From the repo root:

```bash
.cursor/skills/verify-pen/scripts/launch.sh
```

Ready means all of the following.

- `adb -s emulator-5556 shell getprop sys.boot_completed` prints `1`.
- `adb -s emulator-5556 shell pidof io.github.altenhofen.pen` prints a pid.
- A UI dump contains the text `Hello Android!`.

The script writes `/tmp/pen-verify/emulator.pid` and `/tmp/pen-verify/serial`. Gradle uses `JAVA_HOME=/opt/android-studio-canary/jbr` and `sdk.dir` from `local.properties` (`/home/altenhofen/Android/Sdk`). The debug APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Doctor

```bash
.cursor/skills/verify-pen/scripts/doctor.sh
```

Read-only. Exit `0` only when the serial is `emulator-5556`, `sys.boot_completed` is `1`, package `io.github.altenhofen.pen` is installed, and `dumpsys package io.github.altenhofen.pen` reports `versionName=1.0`. Run this before every drive when the screen looks wrong.

## Drive

Harness is `adb` on `emulator-5556`. Stable handle is the visible text `Hello Android!` from `Greeting` in `MainActivity`. There are no content descriptions or test tags.

```bash
.cursor/skills/verify-pen/scripts/drive-greeting.sh
```

The script force-stops the app, starts `io.github.altenhofen.pen/.MainActivity` with `am start -n`, dumps the hierarchy, and screenshots. Proof is the dump containing `text="Hello Android!"` after that start, plus the screenshot.

The keyboard list is a second drive.

```bash
.cursor/skills/verify-pen/scripts/drive-ink-keyboard.sh
```

Proof is the dump containing `text="pen ink"` after `android.settings.INPUT_METHOD_SETTINGS`, plus the screenshot.

Do not call Compose test APIs or `setContent` as a substitute. Do not treat `docs/PRD.md` controls as present.

## Evidence

Artifacts stay in `.cursor/skills/verify-pen/artifacts/<feature-id>/`.

- `hierarchy.xml` is the UI dump taken after the user action.
- `screen.png` is the framebuffer after the same action.
- `drive.log` records the `am start` command, its stdout, and its exit code.

A proof shows the action and the resulting state. For the greeting, the action is launching the activity. The resulting state is the text node `Hello Android!` in `hierarchy.xml` and the same words in `screen.png`.

## Cleanup

```bash
.cursor/skills/verify-pen/scripts/cleanup.sh
```

Kills the emulator pid stored in `/tmp/pen-verify/emulator.pid`, then `adb -s emulator-5556 emu kill` if that serial is still up. Deletes `/tmp/pen-verify`. Leaves `.cursor/skills/verify-pen/artifacts/` in place.

## Helpers

| Script | Role |
| --- | --- |
| `scripts/launch.sh` | Boot `emulator-5556`, install the debug APK, start `MainActivity`, wait until `Hello Android!` is in the hierarchy. |
| `scripts/doctor.sh` | Check serial, boot, package, and `versionName=1.0`. |
| `scripts/drive-greeting.sh` | Relaunch `MainActivity` and write proof under `artifacts/greeting/`. |
| `scripts/drive-ink-keyboard.sh` | Open on-screen keyboard settings and write proof under `artifacts/ink-keyboard/`. |
| `scripts/cleanup.sh` | Stop the emulator this run started. Keep artifacts. |
