#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/profile-import"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

"$(dirname "$0")/doctor.sh"
mkdir -p "$OUT"

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
  if printf '%s' "$dump" | grep -q 'text="Import profile"'; then
    break
  fi
  sleep 1
done
printf '%s' "$dump" | grep -q 'text="Import profile"'

tap="$(printf '%s' "$dump" | python3 -c '
import re, sys
xml = sys.stdin.read()
needle = "text=\"Import profile\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit("no Import profile")
chunk = xml[:idx]
parents = list(re.finditer(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
    chunk,
))
if not parents:
    raise SystemExit("no clickable ancestor for Import profile")
x1, y1, x2, y2 = map(int, parents[-1].groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
')"
echo "tap: $tap" >>"$OUT/drive.log"
adb -s "$SERIAL" shell input tap $tap
for _ in $(seq 1 20); do
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'package="com.google.android.documentsui"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'package="com.google.android.documentsui"' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
adb -s "$SERIAL" shell input keyevent 4
echo "proof: $OUT"
