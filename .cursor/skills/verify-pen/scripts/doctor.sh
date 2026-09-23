#!/usr/bin/env bash
set -euo pipefail

SERIAL="emulator-5556"
export ANDROID_HOME="${ANDROID_HOME:-/home/altenhofen/Android/Sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"

adb devices | awk '{print $1}' | grep -qx "$SERIAL" || {
  echo "doctor: $SERIAL is not attached" >&2
  exit 1
}
boot="$(adb -s "$SERIAL" shell getprop sys.boot_completed | tr -d '\r')"
[[ "$boot" == "1" ]] || {
  echo "doctor: boot_completed=$boot" >&2
  exit 1
}
adb -s "$SERIAL" shell pm path io.github.altenhofen.pen >/dev/null
adb -s "$SERIAL" shell cmd locale set-app-locales io.github.altenhofen.pen en >/dev/null 2>&1 || true
dump="$(adb -s "$SERIAL" shell dumpsys package io.github.altenhofen.pen | tr -d '\r')"
version=""
if [[ "$dump" =~ versionName=([^[:space:]]+) ]]; then
  version="${BASH_REMATCH[1]}"
fi
[[ "$version" == "1.0" ]] || {
  echo "doctor: versionName=$version" >&2
  exit 1
}
echo "doctor ok serial=$SERIAL versionName=$version"
