#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/settings"
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

for _ in $(seq 1 20); do
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'text="Pen settings"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'text="Pen settings"' "$OUT/hierarchy.xml"
grep -q 'Settle window' "$OUT/hierarchy.xml"
grep -q 'Stroke width' "$OUT/hierarchy.xml"
grep -q 'Ambiguity threshold' "$OUT/hierarchy.xml"
grep -q 'text="Calibrate 8 and 9"' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
echo "proof: $OUT"
