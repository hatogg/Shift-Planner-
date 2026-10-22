#!/usr/bin/env bash
# Запускається всередині емулятора (reactivecircus/android-emulator-runner): ставить застосунок, вмикає
# демо-режим рядка стану (рівний годинник, повна батарея, без сповіщень), знімає екрани й забирає PNG.
set -euo pipefail
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
demo() { adb shell am broadcast -a com.android.systemui.demo -e command "$@" > /dev/null; }
adb shell settings put global sysui_demo_allowed 1
demo enter
demo clock -e hhmm 0941
demo battery -e level 100 -e plugged false
demo network -e wifi show -e level 4 -e mobile show -e datatype none -e level 4
demo notifications -e visible false
adb shell am instrument -w -e class ua.x44to.shiftplanner.ScreenshotTest \
  ua.x44to.shiftplanner.test/androidx.test.runner.AndroidJUnitRunner | tee instrument.log
grep -q "OK (1 test)" instrument.log
mkdir -p docs/screens
adb exec-out run-as ua.x44to.shiftplanner tar -cf - files/screens | tar -xf - -C docs/screens --strip-components=2
ls -la docs/screens
