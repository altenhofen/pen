# Contributing to pen

Thank you for helping improve pen. This project aims to make stylus handwriting input more reliable for people with fine-motor challenges; clear issues, small focused changes, and respectful discussion all help.

## Before you start

1. Read the [README](README.md) to understand what exists today versus what is planned.
2. Skim [docs/PRD.md](docs/PRD.md) if your change touches recognition, the IME, or user-facing behavior.
3. Check [GitHub Issues](https://github.com/altenhofen/pen/issues) for duplicates or ongoing work.

## Development setup

1. Clone the repository and open it in Android Studio (or use the command line with the Gradle wrapper).
2. Ensure you have a JDK compatible with the project (see `gradle/libs.versions.toml` and Android Studio’s JDK settings).
3. Build and install:
   ```bash
   ./gradlew :app:assembleDebug :app:installDebug
   ```
4. Run unit tests:
   ```bash
   ./gradlew test
   ```

For automated checks of launcher and keyboard listing on an emulator, see `.cursor/skills/verify-pen/SKILL.md` (optional; used by maintainers with Cursor).

## How to contribute

### Reporting bugs

Include:

- Android version and device model
- Whether you used a stylus or finger
- Steps to reproduce
- What you expected vs what happened
- Screenshots or a short screen recording if helpful

### Suggesting features

Open an issue describing the user problem, not only the solution. Tie suggestions to the PRD when possible so scope stays clear.

### Pull requests

1. Fork the repo and create a branch from `master` (e.g. `fix/settle-window`, `feat/suggestion-strip`).
2. Keep changes focused; unrelated formatting or drive-by refactors make review harder.
3. Add or update tests when you change recognition logic, stroke handling, or persistence.
4. Ensure `./gradlew test` passes.
5. Write a short PR description: **what**, **why**, and **how to verify**.

### Code style

- Follow existing Kotlin and Android patterns in the module you touch.
- Prefer clear names over comments that restate the code.
- Keep IME and recognition logic testable; avoid heavy work on the UI thread.

## Community standards

Be kind and constructive. Harassment and discrimination are not tolerated. See [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md).

## Questions

Use GitHub Issues for questions that might help others. For security-sensitive topics, see [SECURITY.md](SECURITY.md).
