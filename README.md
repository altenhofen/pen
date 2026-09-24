# pen - handwriting keyboard for Android

**Language:** English · [Português](README_PT.md)

**pen** is an Android keyboard you write on with a stylus instead of tapping small keys. It is for people who read and write normally but struggle with ordinary phone keyboards because of tremors, shaky strokes, accidental pen lifts, or letter shapes that generic recognizers keep misreading.

The product is the **pen ink** keyboard. Open the **pen** app to calibrate your letters, train gestures, add personal words, and back up that profile.

The keyboard learns how *you* write. Train letters in **Calibrate**. When a guess is wrong and you delete it, the stored shape moves away from that ink. When you keep typing, the match is reinforced. Over time the keyboard should fit your hand more closely than a one-size-fits-all model.

## How it differs from Google handwriting typing

Android’s usual handwriting path is **Gboard handwriting** (the older standalone Google Handwriting Input app was retired in favor of Gboard). Google describes that layout as a blank pad where you **handwrite words**. Recognition is a neural network trained on many writers. Touch points become Bézier curves, a recurrent network proposes letters, and a **language model** prefers common sequences in that language (for example “sch” in German). The models run on the device. Gboard supports handwriting in many languages. Finger and stylus both write. In most languages it also tries to insert spaces for you.

Sources for that description are Google’s [Gboard handwriting help](https://support.google.com/gboard/answer/9108773) and the [2019 Gboard handwriting research post](https://research.google/blog/rnn-based-handwriting-recognition-in-gboard/).

**pen** solves a different problem. A generic model is strong on typical handwriting and whole words. It is weak when your `9` looks like an `8`, when a tremor splits one letter into two strokes, or when you need the same invented flick every time to delete a word.

| | Gboard handwriting | pen |
|---|--------------------|-----|
| Goal | Convert typical handwriting to text in many languages | Fit *your* motor output, including irregular strokes |
| Unit of writing | Words on a blank pad, with a language model | Letters, words, or a trained gesture, after a settle pause |
| Personalization | A shared model per script. Google documents on-device personalization for Gboard *typing and voice*, not a per-letter handwriting bank you train yourself | **Calibrate** stores *your* drawings. Keep/delete while typing also nudges those shapes. **My words** covers names the generic model does not know |
| Motor control | Handwriting speed and stroke width in Gboard settings | **Settle window** waits out accidental lifts. Stylus-only canvas by default so a resting palm is ignored |
| Offline | On-device Gboard models | Downloads a Google **ML Kit Digital Ink** model once, then runs it on device. If that model is missing, the keyboard still uses your calibration |
| Backup | Gboard account / on-device learned typing data | Encrypted **Export profile** / **Import profile** of *your* shapes, words, gestures, and motor settings |
| Gestures | Built-in scribble actions on some tablet stylus modes | You draw the shape. **Calibrate → Gestures** binds it to an action (delete word, copy, hide keyboard, and others) |

pen still uses Google technology for word-level guesses. [ML Kit Digital Ink](https://developers.google.com/ml-kit/vision/digital-ink) is the first-pass recognizer when the model is downloaded. The difference is the layer around it. Your calibrated letters, word memory, and custom dictionary can change the ranking. When ML Kit has nothing to say, calibration can still insert a letter.

## Features

### pen ink keyboard

Enable **pen ink** in Android’s on-screen keyboard list, then focus a text field.

- **Ink canvas.** Write with an active stylus. Finger and passive pen are off until you turn on **Finger and passive pen** in settings. Keys at the bottom still accept a finger.
- **Settle pause.** After you lift the pen, the canvas waits (default 600 ms, range 300–1200 ms) so a jittery lift does not cut a letter in half. Then the ink clears and the best match is inserted.
- **Suggestion strip.** Up to five alternatives. The inserted guess is shown in bold. Tap another chip to replace the last insert when it is still the text before the cursor.
- **space**, **⌫**, **↵.** Space inserts a space. Double-tap the ink canvas (two quick taps without drawing) inserts a space too. Backspace deletes and, if you do it soon after a guess, treats that guess as wrong. Enter runs the field’s action (or a newline).
- **Model status.** While the handwriting model downloads you see **Downloading handwriting model**. If the model cannot run you see **Offline, using calibration**.
- **Private fields.** Password fields, and apps that opt out of personalized learning, still receive text. They do not teach the recognizer.

### Pen settings

Open the **pen** app (or the keyboard’s settings entry). The screen title is **Pen settings**.

- **Add space after full word.** After a guess longer than one character, insert a space. A following punctuation mark takes that space and puts it after the mark. Off by default.
- **Recognize spaces in handwriting.** When on, gaps the word model sees become spaces. When off, one settled ink group is treated as one token (spaces from the model are stripped). Off by default.
- **Double-tap for space.** When on, two quick taps on the ink canvas insert a space. On by default.
- **Settle window.** 300–1200 ms. Default 600 ms. Also used on the Calibrate and My words canvases.
- **Stroke width.** 2.0–16.0 dp. Default 6.0 dp.
- **Finger and passive pen.** Allow touch on the *keyboard* ink area. Off by default. Calibrate and My words training already accept finger, stylus, and mouse.
- **Set as default keyboard.** Opens Android’s input-method list so you can enable **pen ink**. It does not flip the switch by itself.
- **Language.** **App language** is System default, English, Portuguese, or Spanish. **Handwriting language** can follow the app or be set to English, Portuguese, Spanish, French, German, or Italian for the ML Kit model.

### Calibrate

**Calibrate** teaches the keyboard your shapes.

- **Glyphs.** Pick any of `0–9`, `A–Z`, and `a–z`. Write each selected character. Each sample is stored as a personal template. Built-in seed shapes for `0–9` and `a–z` stay in place. Your samples are extra.
- **Gestures.** Bind a drawing of your choice to a text action. Each action needs at least five samples before it is saved as trained. If a settled drawing matches a trained gesture more clearly than a letter, the keyboard runs the action instead of inserting text.

Gesture actions you can train:

- Case of the last word (lower, upper, capitalize, cycle)
- Delete last word, delete line, delete all
- Undo last gesture
- Cursor to line start/end and field start/end
- Select last word, select all
- Copy, cut, paste
- Insert newline, insert tab
- Swap last two characters
- Hide keyboard, switch keyboard

### My words

**My words** is a personal dictionary for names and tokens the word model does not know. Optional training (up to five drawings per word) helps the keyboard recognize that word from shape, not only from spelling rescue against the model’s guesses.

### Profile export and import

**Export profile** writes an encrypted `pen-profile.penbak` file. You choose a passphrase of at least eight characters. The passphrase cannot be recovered. Without it the file cannot be read.

The archive includes calibrated letter shapes, word samples, My words, trained gestures, and motor settings.

**Import profile** replaces the on-device profile with that file. Newer exports ask for the passphrase. Older unencrypted backups still import if you pick them.

## How it works

1. Strokes are captured on the ink canvas. Rejected tool types (a finger while stylus-only is on) never become ink.
2. After the settle window, the keyboard looks for a **trained gesture**. A confident gesture that fits better than a letter runs the bound action.
3. Otherwise it asks several sources at once.
   - **ML Kit Digital Ink** (downloaded model, then on-device) proposes strings, using a short stretch of text before the cursor as context.
   - **Glyph templates** compare the ink to stored letter shapes (nearest-neighbor match on a normalized trajectory). They matter most when the word model is silent or the top guess is a single character.
   - **Word memory** recalls whole words you previously accepted.
   - **My words** can boost close spellings of your dictionary entries.
4. The highest blended score is **committed immediately** into the app you are typing in. The strip is for correction, not a confirm step.
5. The next thing you do teaches the letter templates (when learning is allowed).
   - Keep typing, or hit space/enter → the last letter match is pulled toward that ink.
   - Backspace (or the cursor jumping left) within about three seconds → the last letter match is pushed away.
   - Tapping another suggestion replaces the text. Word-shaped ink can be remembered for next time.

Nothing in that personal bank is uploaded by this app. The only Google network step is downloading the ML Kit handwriting model for the chosen language.

Planning notes and older milestone language live in [docs/PRD.md](docs/PRD.md). That document describes the product intent. This README describes what the app does now. Some PRD items (a live ambiguity slider, a full-screen IME canvas, a “contextual bandit” recognizer) are not how the shipped keyboard works.

## What you need

| Requirement | Details |
|-------------|---------|
| Device | Android phone or tablet (Android 8.0 / API 26 or newer) |
| Input | Active stylus recommended. Finger works on the keyboard if you enable **Finger and passive pen** |
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

1. Open **Settings → System → Languages & input** (names vary slightly by manufacturer), or tap **Set as default keyboard** in the pen app.
2. Tap **On-screen keyboard** → **Manage on-screen keyboards**.
3. Enable **pen ink**.
4. Open any app with a text field, tap the field, then tap the keyboard icon in the navigation bar or status area.
5. Choose **pen ink**.

You should see a drawing area, a suggestion strip, and **space** / backspace / enter. Write a character or word with the stylus. After you lift the pen and pause, the best match is inserted. If the wrong text appears, tap another chip or use **Backspace** soon after.

For best results on letters the generic model confuses, open **Calibrate** and write those characters yourself.

## Project layout

```
app/          Android application, settings UI, and IME (keyboard service)
docs/         Product requirements and planning
gradle/       Build configuration (standard Android project)
```

Main code areas:

- `PenInputMethodService` — the keyboard service
- `DrawingCanvasView` — ink canvas and settle timing
- `InkModel` — ML Kit Digital Ink wrapper
- `recognition/` — glyph and gesture matching, word memory, suggestion blend, on-device stores
- `calibration/` — Calibrate sessions for glyphs and gestures
- `profile/` — encrypted export and import

## Contributing

Contributions are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) before opening a pull request.

## Security

To report a security issue, see [SECURITY.md](SECURITY.md). Please do not open public issues for sensitive reports.

## License

This project is licensed under the [MIT License](LICENSE).
