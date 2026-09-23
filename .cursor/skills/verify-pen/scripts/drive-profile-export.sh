#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
OUT="$ROOT/.cursor/skills/verify-pen/artifacts/profile-export"
TEST_PASS="testpass12"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

"$(dirname "$0")/doctor.sh"
mkdir -p "$OUT"

clean_dump() {
  sed 's/UI hierchary dumped to:.*//' "$1"
}

tap_text_center() {
  local label="$1"
  local file="$2"
  python3 -c '
import re, pathlib, sys
label = sys.argv[1]
xml = re.sub(r"UI hierchary dumped to:.*", "", pathlib.Path(sys.argv[2]).read_text())
needle = f"text=\"{label}\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit(f"no {label}")
window = xml[idx : idx + 1200]
m = re.search(r"bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", window)
if not m:
    raise SystemExit(f"no bounds for {label}")
x1, y1, x2, y2 = map(int, m.groups())
print((x1 + x2) // 2, (y1 + y2) // 2)
' "$label" "$file"
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

for _ in $(seq 1 20); do
  adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true
  clean_dump "$OUT/hierarchy.xml" | grep -q 'text="Export profile"' && break
  sleep 1
done
clean_dump "$OUT/hierarchy.xml" | grep -q 'text="Export profile"'

tap="$(tap_text_center "Export profile" "$OUT/hierarchy.xml")"
echo "tap export: $tap" >>"$OUT/drive.log"
adb -s "$SERIAL" shell input tap $tap
sleep 1

for _ in $(seq 1 20); do
  adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true
  if clean_dump "$OUT/hierarchy.xml" | grep -q 'text="Protect this export"'; then
    break
  fi
  sleep 1
done
clean_dump "$OUT/hierarchy.xml" | grep -q 'text="Protect this export"'

adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true

tap="$(python3 -c '
import re, pathlib
xml = re.sub(r"UI hierchary dumped to:.*", "", pathlib.Path("'"$OUT/hierarchy.xml"'").read_text())
for m in re.finditer(r"class=\"android.widget.EditText\"[^>]*password=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"", xml):
    x1, y1, x2, y2 = map(int, m.groups())
    print((x1 + x2) // 2, (y1 + y2) // 2)
    break
else:
    raise SystemExit("no passphrase field")
')"
adb -s "$SERIAL" shell input tap $tap
sleep 0.3
adb -s "$SERIAL" shell input text "$TEST_PASS"
sleep 0.3
adb -s "$SERIAL" shell input keyevent 61
sleep 0.3
adb -s "$SERIAL" shell input text "$TEST_PASS"
sleep 0.5

encrypt_tap=""
for _ in $(seq 1 20); do
  adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true
  encrypt_tap="$(python3 -c '
import re, pathlib
xml = re.sub(r"UI hierchary dumped to:.*", "", pathlib.Path("'"$OUT/hierarchy.xml"'").read_text())
needle = "text=\"Encrypt and save\""
idx = xml.find(needle)
if idx < 0:
    raise SystemExit("no Encrypt and save")
before = xml[:idx]
for m in reversed(list(re.finditer(
    r"clickable=\"true\"[^>]*bounds=\"\[(\d+),(\d+)\]\[(\d+),(\d+)\]\"",
    before,
))):
    tag = before[m.start() : idx]
    if "enabled=\"true\"" in tag:
        x1, y1, x2, y2 = map(int, m.groups())
        print((x1 + x2) // 2, (y1 + y2) // 2)
        raise SystemExit(0)
raise SystemExit("encrypt not enabled")
')" && break
  sleep 0.5
done
[[ -n "$encrypt_tap" ]]
adb -s "$SERIAL" shell input tap $encrypt_tap
sleep 1

for _ in $(seq 1 20); do
  adb -s "$SERIAL" exec-out uiautomator dump /dev/tty >"$OUT/hierarchy.xml" 2>/dev/null || true
  if clean_dump "$OUT/hierarchy.xml" | grep -q 'text="pen-profile.penbak"'; then
    break
  fi
  sleep 1
done
clean_dump "$OUT/hierarchy.xml" | grep -q 'text="pen-profile.penbak"'
adb -s "$SERIAL" exec-out screencap -p >"$OUT/screen.png"
adb -s "$SERIAL" shell input keyevent 4
echo "proof: $OUT"
