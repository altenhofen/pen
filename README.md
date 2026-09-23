# pen — handwriting keyboard for Android

**pen** is an Android keyboard where you write letters and numbers with a stylus instead of tapping small keys. It is built for people who read and write normally but find ordinary phone keyboards hard to use because of tremors, shaky strokes, or handwriting that does not match what generic recognizers expect.

The keyboard learns from how *you* write. When it gets something wrong and you delete it, it adjusts; when you keep typing, it reinforces the match. Over time it should fit your hand more closely.

> **Status:** Early prototype. The launcher app is a simple placeholder; the real product is the **pen ink** keyboard you enable in Android settings. See [docs/PRD.md](docs/PRD.md) for the full product plan.

## What you need

| Requirement | Details |
|-------------|---------|
| Device | Android phone or tablet (Android 8.0 / API 26 or newer) |
| Input | Stylus recommended (finger may work but is not the focus) |
| Build (optional) | [Android Studio](https://developer.android.com/studio) if you install from source |

## Install from source (developers and testers)

1. **Get the code**
   ```bash
   git clone https://github.com/altenhofen/pen.git
   cd pen
   ```

2. **Open in Android Studio**  
   Open the `pen` folder. Let Gradle sync finish (first time may take several minutes).

3. **Connect a device or start an emulator**  
   Enable **Developer options** and **USB debugging** on a physical device, or create an emulator in Device Manager.

4. **Run the app**  
   Click **Run** (green play) or from a terminal:
   ```bash
   ./gradlew :app:installDebug
   ```

5. **Run tests** (optional)
   ```bash
   ./gradlew test
   ```

## Turn on the pen ink keyboard (everyone)

After the app is installed:

1. Open **Settings → System → Languages & input** (names vary slightly by manufacturer).
2. Tap **On-screen keyboard** → **Manage on-screen keyboards**.
3. Enable **pen ink** (or **pen**).
4. Open any app with a text field, tap the field, then tap the keyboard icon in the navigation bar or status area.
5. Choose **pen ink** as your keyboard.

You should see a drawing area at the bottom of the screen. Write a character with the stylus; after you lift the pen and pause briefly, the best match is inserted into the text field. If the wrong letter appears, use **Backspace** soon after — the keyboard treats that as feedback and learns from it.

## How it works (short version)

1. Your strokes are captured on an ink canvas (stylus-focused).
2. Each finished shape is normalized and compared to stored examples of each letter and digit.
3. The closest match is committed to the app you are typing in.
4. Your personal examples are saved on the device (Room database) and updated when you accept or reject a guess.

Technical details, roadmap, and metrics are in [docs/PRD.md](docs/PRD.md).

## Project layout

```
app/          Android application and IME (keyboard service)
docs/         Product requirements and planning
gradle/       Build configuration (standard Android project)
```

Main code areas:

- `PenInputMethodService` — the keyboard service
- `DrawingCanvasView` — ink canvas and stroke timing
- `recognition/` — glyph matching and on-device prototype storage

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## Security

To report a security issue, see [SECURITY.md](SECURITY.md). Please do not open public issues for sensitive reports.

## License

This project is licensed under the [MIT License](LICENSE).
