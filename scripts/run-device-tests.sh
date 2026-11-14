#!/usr/bin/env bash
set -euo pipefail
if [[ -z "${ANDROID_SERIAL:-}" ]]; then
    mapfile -t emulators < <(adb devices | awk '$1 ~ /^emulator-[0-9]+$/ && $2 == "device" {print $1}')
    if [[ ${#emulators[@]} -ne 1 ]]; then
        echo "Select one running emulator with ANDROID_SERIAL." >&2
        exit 1
    fi
    export ANDROID_SERIAL="${emulators[0]}"
fi
if [[ ! "$ANDROID_SERIAL" =~ ^emulator-[0-9]+$ ]] ||
    [[ "$(adb shell getprop ro.kernel.qemu | tr -d '\r')" != 1 ]]; then
    echo "Device tests require an Android emulator." >&2
    exit 1
fi
./gradlew :office-engine:assembleDebug --stacktrace
adb install -r office-engine/build/outputs/apk/debug/office-engine-x86_64-debug.apk
# The Google APIs image's first-boot setup helper can display an unrelated ANR over test UI.
adb shell settings put global device_provisioned 1
adb shell settings put secure user_setup_complete 1
adb shell am force-stop com.google.android.googlesdksetup || true
adb shell pm disable-user --user 0 com.google.android.googlesdksetup || true
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace
RESULT=$?
set -e
mkdir -p app/build/outputs
adb pull /sdcard/Pictures/MyDocTestCaptures app/build/outputs/ui-captures || true
exit "$RESULT"
