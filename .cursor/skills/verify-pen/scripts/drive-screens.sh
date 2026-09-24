#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
DIR="$(dirname "$0")"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/screens"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

"$DIR/doctor.sh"
mkdir -p "$OUT"
: >"$OUT/drive.log"

shot() {
  local id="$1"
  local marker="$2"
  local dump="$OUT/$id.xml"
  for _ in $(seq 1 20); do
    adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$dump" 2>/dev/null || true
    if grep -q "$marker" "$dump"; then
      adb -s "$SERIAL" exec-out screencap -p >"$OUT/$id.png"
      echo "shot $id" >>"$OUT/drive.log"
      return 0
    fi
    sleep 1
  done
  echo "missing $id marker $marker" >>"$OUT/drive.log"
  return 1
}

tap_label() {
  local label="$1"
  local file="$2"
  python3 -c '
import re, pathlib, sys
label, path = sys.argv[1], sys.argv[2]
xml = re.sub(r"UI hierchary dumped to:.*", "", pathlib.Path(path).read_text())
needle = f"text=\"{label}\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit(f"no {label}")
parents = list(re.finditer(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
    xml[:idx],
))
if parents:
    x1, y1, x2, y2 = map(int, parents[-1].groups())
else:
    window = xml[idx : idx + 800]
    m = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", window)
    if not m:
        raise SystemExit(f"no bounds for {label}")
    x1, y1, x2, y2 = map(int, m.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
' "$label" "$file"
}

adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000
shot settings 'text="Pen settings"'

tap="$(tap_label "My words" "$OUT/settings.xml")"
adb -s "$SERIAL" shell input tap $tap
shot my-words 'text="My words"'

adb -s "$SERIAL" shell input keyevent 4
shot settings 'text="Pen settings"'

tap="$(tap_label Calibrate "$OUT/settings.xml")"
adb -s "$SERIAL" shell input tap $tap
shot calibrate-picker 'text="Select characters to train"'

tap="$(tap_label 0 "$OUT/calibrate-picker.xml")"
adb -s "$SERIAL" shell input tap $tap
sleep 1
adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/calibrate-picker.xml"
tap="$(tap_label Start "$OUT/calibrate-picker.xml")"
adb -s "$SERIAL" shell input tap $tap
shot calibrate-canvas 'text="Now writing 0"'

adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000
shot settings 'text="Pen settings"'

tap="$(tap_label Gestures "$OUT/settings.xml")"
adb -s "$SERIAL" shell input tap $tap
shot gestures 'text="Gesture actions"'

adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000
shot settings 'text="Pen settings"'

tap="$(tap_label "Export profile" "$OUT/settings.xml")"
adb -s "$SERIAL" shell input tap $tap
shot export-passphrase 'text="Protect this export"'

adb -s "$SERIAL" shell input keyevent 4

copy_drive() {
  local script="$1"
  local id="$2"
  "$DIR/$script"
  cp "$ROOT/.cursor/skills/verify-pen/artifacts/$id/screen.png" "$OUT/$id.png"
  echo "copied $id" >>"$OUT/drive.log"
}

copy_drive drive-profile-export.sh profile-export
copy_drive drive-profile-import.sh profile-import
copy_drive drive-default-keyboard.sh default-keyboard
copy_drive drive-ink-keyboard.sh ink-keyboard

echo "gallery: $OUT"
ls -1 "$OUT"/*.png
