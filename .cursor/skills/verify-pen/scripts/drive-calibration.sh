#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/calibration"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

"$(dirname "$0")/doctor.sh"
mkdir -p "$OUT"

adb -s "$SERIAL" shell am force-stop io.github.altenhofen.pen
set +e
start_out="$(adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity 2>&1)"
start_code=$?
set -e
{
  echo "command: adb -s $SERIAL shell am start -n io.github.altenhofen.pen/.MainActivity"
  echo "exit: $start_code"
  printf '%s\n' "$start_out"
} >"$OUT/drive.log"
[[ "$start_code" -eq 0 ]]

dump=""
for _ in $(seq 1 20); do
  dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
  if printf '%s' "$dump" | grep -q 'text="Calibrate 8 and 9"'; then
    break
  fi
  sleep 1
done
printf '%s' "$dump" | grep -q 'text="Calibrate 8 and 9"'

tap="$(printf '%s' "$dump" | python3 -c '
import re, sys
xml = sys.stdin.read()
pat = re.compile(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"[^>]*>.*?text=\"Calibrate 8 and 9\"",
    re.DOTALL,
)
m = pat.search(xml)
if not m:
    pat = re.compile(
        r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"[^>]*clickable=\"true\"[^>]*>.*?text=\"Calibrate 8 and 9\"",
        re.DOTALL,
    )
    m = pat.search(xml)
if not m:
    raise SystemExit("no clickable Calibrate 8 and 9 node")
x1, y1, x2, y2 = map(int, m.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
')"
echo "tap: $tap" >>"$OUT/drive.log"
for _ in $(seq 1 20); do
  adb -s "$SERIAL" shell input tap $tap
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'text="Draw 8: 0 of 5 samples collected"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'text="Draw 8: 0 of 5 samples collected"' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
echo "proof: $OUT"
