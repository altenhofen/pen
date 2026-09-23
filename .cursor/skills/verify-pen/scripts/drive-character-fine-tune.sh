#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/character-fine-tune"
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
  if printf '%s' "$dump" | grep -q 'text="Fine-tune 0-9 and a-z"'; then
    break
  fi
  sleep 1
done
printf '%s' "$dump" | grep -q 'text="Fine-tune 0-9 and a-z"'

tap="$(printf '%s' "$dump" | python3 -c '
import re, sys
xml = sys.stdin.read()
label = "Fine-tune 0-9 and a-z"
needle = f"text=\"{label}\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit("no Fine-tune label in hierarchy")
chunk = xml[:idx]
parents = list(re.finditer(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
    chunk,
))
if not parents:
    raise SystemExit("no clickable ancestor for Fine-tune")
x1, y1, x2, y2 = map(int, parents[-1].groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
')"
echo "tap: $tap" >>"$OUT/drive.log"
for _ in $(seq 1 20); do
  adb -s "$SERIAL" shell input tap $tap
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'text="Draw 0: 0 of 3 samples collected"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'text="Draw 0: 0 of 3 samples collected"' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
echo "proof: $OUT"
