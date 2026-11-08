#!/usr/bin/env bash
set -euo pipefail
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
