#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/settings"
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

for _ in $(seq 1 20); do
  if adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null; then
    if grep -q 'text="Pen settings"' "$OUT/hierarchy.xml"; then
      break
    fi
  fi
  sleep 1
done
grep -q 'text="Pen settings"' "$OUT/hierarchy.xml"
grep -q 'text="Calibrate"' "$OUT/hierarchy.xml"
grep -q 'text="Set as default keyboard"' "$OUT/hierarchy.xml"
grep -q 'text="My words"' "$OUT/hierarchy.xml"
grep -q 'text="Gestures"' "$OUT/hierarchy.xml"
grep -q 'Add space after full word' "$OUT/hierarchy.xml"
grep -q 'Space after suggestion pick' "$OUT/hierarchy.xml"
grep -q 'text="Off"' "$OUT/hierarchy.xml"
grep -q 'Recognize spaces in handwriting' "$OUT/hierarchy.xml"
grep -q 'Settle window' "$OUT/hierarchy.xml"
grep -q 'Stroke width' "$OUT/hierarchy.xml"
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"

for _ in $(seq 1 8); do
  if grep -q 'text="Finger and passive pen"' "$OUT/hierarchy.xml" \
    && grep -q 'text="Export profile"' "$OUT/hierarchy.xml" \
    && grep -q 'text="Import profile"' "$OUT/hierarchy.xml"; then
    break
  fi
  adb -s "$SERIAL" shell input swipe 540 1800 540 600 200
  sleep 1
  adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true
done
grep -q 'text="Finger and passive pen"' "$OUT/hierarchy.xml"
grep -q 'text="Export profile"' "$OUT/hierarchy.xml"
grep -q 'text="Import profile"' "$OUT/hierarchy.xml"
echo "proof: $OUT"
