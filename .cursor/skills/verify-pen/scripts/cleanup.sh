#!/usr/bin/env bash
set -euo pipefail

STATE="/tmp/pen-verify"
SERIAL="emulator-5556"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

if [[ -f "$STATE/emulator.pid" ]]; then
  pid="$(cat "$STATE/emulator.pid")"
  if kill -0 "$pid" 2>/dev/null; then
    kill "$pid" 2>/dev/null || true
    for _ in $(seq 1 20); do
      kill -0 "$pid" 2>/dev/null || break
      sleep 0.5
    done
    kill -9 "$pid" 2>/dev/null || true
  fi
fi

if adb devices | awk '{print $1}' | grep -qx "$SERIAL"; then
  adb -s "$SERIAL" emu kill || true
fi

rm -rf "$STATE"
echo "cleaned $SERIAL state. artifacts kept under .cursor/skills/verify-pen/artifacts/"
