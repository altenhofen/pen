#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/calibration"
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

set +e
start_out="$(adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000 2>&1)"
start_code=$?
set -e
{
  echo "command: adb -s $SERIAL shell am start -n io.github.altenhofen.pen/.MainActivity -f 0x10008000"
  echo "exit: $start_code"
  printf '%s\n' "$start_out"
} >"$OUT/drive.log"
[[ "$start_code" -eq 0 ]]
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
echo "tap calibrate: $tap" >>"$OUT/drive.log"
adb -s "$SERIAL" shell input tap $tap

dump=""
for _ in $(seq 1 20); do
  dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
  if printf '%s' "$dump" | grep -q 'text="Select characters to train"'; then
    break
  fi
  sleep 1
done
printf '%s' "$dump" | grep -q 'text="Select characters to train"'

tap="$(printf '%s' "$dump" | tap_label 0)"
echo "tap 0: $tap" >>"$OUT/drive.log"
adb -s "$SERIAL" shell input tap $tap
sleep 1
dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
tap="$(printf '%s' "$dump" | tap_label Start)"
echo "tap start: $tap" >>"$OUT/drive.log"
for _ in $(seq 1 20); do
  adb -s "$SERIAL" shell input tap $tap
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'text="Now writing 0"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'text="Now writing 0"' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
echo "proof: $OUT"
