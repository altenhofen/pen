# Greeting

Greeting is the first screen after the user opens pen. It shows the words `Hello Android!` and nothing else the user can tap.

## Sub-features

- `greeting-launch` shows `Hello Android!` after the user opens the app from the launcher.

## How to get to it (user POV)

- Open the `pen` icon in the app drawer. The activity is `io.github.altenhofen.pen/.MainActivity`.

## Driving it with adb

Preconditions:

- `.cursor/skills/verify-pen/scripts/doctor.sh` exits `0` on serial `emulator-5556`.
- Package `io.github.altenhofen.pen` is installed at `versionName=1.0`.

- **Open the app.** The user taps the `pen` launcher icon. Run `adb -s emulator-5556 shell am start -n io.github.altenhofen.pen/.MainActivity`. Exit code `0`. Stdout contains `Starting: Intent { cmp=io.github.altenhofen.pen/.MainActivity }`.
- **Read the greeting.** The screen shows `Hello Android!`. Run `adb -s emulator-5556 exec-out uiautomator dump /dev/tty`. The dump contains `text="Hello Android!"`.
- **Proof.** Save the dump and a screenshot. Run `.cursor/skills/verify-pen/scripts/drive-greeting.sh`. `artifacts/greeting/hierarchy.xml` contains `text="Hello Android!"` and `artifacts/greeting/screen.png` shows the same words.

## Gotchas

- `uiautomator dump` with no path writes `/sdcard/window_dump.xml` on the device. Proof must use `exec-out uiautomator dump /dev/tty` or pull that file. A screenshot alone can miss the text node.
- The greeting has no content description. Match `text="Hello Android!"`.
- The ink keyboard is a separate system settings row named `pen ink`. Those controls are not on this screen. Do not look for them here.
- Driving a serial other than `emulator-5556` can hit the user's own emulator. Doctor must pass first.
