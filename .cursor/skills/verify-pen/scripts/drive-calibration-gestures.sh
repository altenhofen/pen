#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/gestures"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

"$(dirname "$0")/doctor.sh"
mkdir -p "$OUT"

tap_label() {
  local label="$1"
  python3 -c '
import re, sys
xml = sys.stdin.read()
label = sys.argv[1]
needle = f"text=\"{label}\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit(f"no {label}")
parents = list(re.finditer(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
    xml[:idx],
))
if not parents:
    raise SystemExit(f"no clickable ancestor for {label}")
x1, y1, x2, y2 = map(int, parents[-1].groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
' "$label"
}

adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000 >>"$OUT/drive.log"
sleep 1

dump=""
for _ in $(seq 1 20); do
  dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
  if printf '%s' "$dump" | grep -q 'text="Calibrate"'; then
    break
  fi
  sleep 1
done
printf '%s' "$dump" | grep -q 'text="Calibrate"'

tap="$(printf '%s' "$dump" | tap_label Calibrate)"
adb -s "$SERIAL" shell input tap $tap
sleep 1

dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
tap="$(printf '%s' "$dump" | tap_label Gestures)"
echo "tap gestures tab: $tap" >>"$OUT/drive.log"
adb -s "$SERIAL" shell input tap $tap
sleep 1

adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml"
grep -q 'text="Gesture actions"' "$OUT/hierarchy.xml"
grep -q 'text="Select actions to train. Each needs at least 5 drawings."' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
echo "proof: $OUT"
