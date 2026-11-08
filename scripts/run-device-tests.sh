#!/usr/bin/env bash
set -euo pipefail
./gradlew :office-engine:assembleDebug --stacktrace
adb install -r office-engine/build/outputs/apk/debug/office-engine-x86_64-debug.apk
set +e
./gradlew :app:connectedDebugAndroidTest --stacktrace
RESULT=$?
set -e
mkdir -p app/build/outputs
adb pull /sdcard/Android/data/com.abhisheksati.mydoc.debug/files/ui-captures app/build/outputs/ || true
exit "$RESULT"
