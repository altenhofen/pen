# pen verification map

This directory is the maintained source for verifying the user-facing behavior of pen. Read the index before driving the app, then use the matching feature file as the recipe.

## Baseline preconditions

- Launcher activity `io.github.altenhofen.pen/.MainActivity` shows `Pen settings`.
- On-screen keyboard settings lists the input method `pen ink`.
- Launch with `.cursor/skills/verify-pen/scripts/launch.sh` so the run owns serial `emulator-5556`.
- Run `.cursor/skills/verify-pen/scripts/doctor.sh` and require serial `emulator-5556`, boot completed, and `versionName=1.0`.
- Never drive an emulator serial this run did not start.

## Driving conventions

- Start every recipe from a fresh `am start` of `MainActivity` unless its preconditions say otherwise.
- Prefer the visible text `Pen settings` over coordinates.
- Treat every command as literal.
- Run device actions through `adb -s emulator-5556`.
- Do not remove proof artifacts during cleanup.

## Proof and skip reporting

- Capture the user action and the resulting state, not only the final screen.
- UI proof includes a UI hierarchy dump and a screenshot.
- Record the feature ID (`settings`, `calibration`, or `ink-keyboard`) with every artifact.
- Report an unreachable path with the attempted command and the unmet precondition.
- Do not report a skipped entry point as verified through a different path.

## Feature entry contract

Each feature file starts with an H1 title and one paragraph describing the user-visible behavior. It then uses exactly four H2 sections in this order.

1. `Sub-features` lists short IDs with one line for each behavior.
2. `How to get to it (user POV)` lists every user entry point.
3. `Driving it with adb` starts with `Preconditions:` and uses labeled bullets that pair each user action with an exact command and observable result.
4. `Gotchas` lists traps that can waste or invalidate a verification run.

## Features

- [Settings](./settings.md) covers the launcher activity, the three motor sliders, and `Calibrate 8 and 9`.
- [Calibration](./calibration.md) covers the training canvas opened from that button.
- [Ink keyboard](./ink-keyboard.md) covers the system keyboard list entry `pen ink`.
