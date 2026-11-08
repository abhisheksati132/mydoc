#!/usr/bin/env bash
# Release smoke test, run on the emulator by .github/workflows/release.yml.
#
# It lives in a file rather than inline in the workflow because the emulator action executes its
# `script:` input one line at a time, each in its own `sh -c` (measured: the job log shows
# `[command]/usr/bin/sh -c set -eu`, then `sh -c : > release-assets/smoke.txt`, then `sh -c for APK in ...`
# dying with "Syntax error: end of file unexpected"). A loop cannot span those invocations, so the
# whole test has to be a single command that hands the script to bash.
#
# Nothing is published unless this passes: a signed, checksum-matched APK is not proof that the
# R8-shrunk release build actually starts.
set -euo pipefail
adb install -r office-engine/build/outputs/apk/release/office-engine-x86_64-release.apk

PACKAGE=com.abhisheksati.mydoc
OUTPUT_DIR=app/build/outputs/apk/release
EVIDENCE=release-assets/smoke.txt
APPS=(
  "$OUTPUT_DIR/app-x86_64-release.apk"
  "$OUTPUT_DIR/app-universal-release.apk"
)

mkdir -p release-assets
: > "$EVIDENCE"

for APK in "${APPS[@]}"; do
  if [ ! -f "$APK" ]; then
    echo "ERROR: expected $APK to exist for the smoke test."
    exit 1
  fi
  NAME=$(basename "$APK")

  if ! adb install -r -g "$APK"; then
    echo "ERROR: the runner could not install $NAME."
    exit 1
  fi
  adb logcat -c
  adb shell am start -n "$PACKAGE/$PACKAGE.MainActivity" >/dev/null
  sleep 20

  if [ -z "$(adb shell pidof -s "$PACKAGE")" ]; then
    echo "ERROR: the release build of $NAME did not stay running."
    adb logcat -d -v brief | tail -n 200
    exit 1
  fi
  if adb logcat -d -v brief | grep -q "FATAL EXCEPTION"; then
    echo "ERROR: the release build of $NAME crashed on launch."
    adb logcat -d -v brief | grep -A 30 "FATAL EXCEPTION" | head -n 60
    exit 1
  fi

  adb exec-out screencap -p > "release-smoke-$(echo "$NAME" | tr '.' '_').png"
  echo "$NAME: installed, launched and survived 20s on API 35 x86_64." | tee -a "$EVIDENCE"
  adb uninstall "$PACKAGE" > /dev/null
done

# Stated rather than implied: an x86_64 emulator cannot execute arm code, so the arm splits are
# signature-, ABI- and metadata-verified by the workflow but never executed here.
{
  echo "arm64-v8a and armeabi-v7a splits: signature, ABI contents and package metadata"
  echo "verified; not executed, because this runner has no arm64 or armv7 system image."
} | tee -a "$EVIDENCE"
