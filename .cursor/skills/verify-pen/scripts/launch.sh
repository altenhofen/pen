#!/usr/bin/env bash
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/../../../.." && pwd)"
SERIAL="emulator-5556"
AVD="Medium_Phone_API_37.0"
STATE="/tmp/pen-verify"
export JAVA_HOME="${JAVA_HOME:-/opt/android-studio-canary/jbr}"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH"

mkdir -p "$STATE"

if adb devices | awk '{print $1}' | grep -qx "$SERIAL"; then
  if [[ ! -f "$STATE/emulator.pid" ]] || ! kill -0 "$(cat "$STATE/emulator.pid")" 2>/dev/null; then
    echo "refusing: $SERIAL is already up and this run did not start it" >&2
    exit 1
  fi
else
  emulator -avd "$AVD" -port 5556 -no-snapshot-save -no-boot-anim -gpu swiftshader_indirect \
    >"$STATE/emulator.log" 2>&1 &
  echo $! >"$STATE/emulator.pid"
fi
echo "$SERIAL" >"$STATE/serial"

adb -s "$SERIAL" wait-for-device
for _ in $(seq 1 90); do
  boot="$(adb -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')"
  [[ "$boot" == "1" ]] && break
  sleep 2
done
boot="$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')"
[[ "$boot" == "1" ]]

(
  cd "$ROOT"
  ./gradlew :app:assembleDebug
)
adb -s "$SERIAL" install -r "$ROOT/app/build/outputs/apk/debug/app-debug.apk"
adb -s "$SERIAL" shell am start -n io.github.altenhofen.pen/.MainActivity

for _ in $(seq 1 20); do
  dump="$(adb -s "$SERIAL" exec-out uiautomator dump /dev/tty 2>/dev/null || true)"
  if printf '%s' "$dump" | grep -q 'text="Hello Android!"'; then
    echo "ready: $SERIAL shows Hello Android!"
    exit 0
  fi
  sleep 1
done
echo "launch failed: Hello Android! not in hierarchy" >&2
exit 1
