#!/usr/bin/env bash
# Local replacement for the GitHub "Build" and "Emulator tests" workflows.
#
# Runs, in order: cosmos unit tests, the full app build (lint + JVM tests), then the instrumented tests on a
# dedicated emulator. It never touches other devices or emulators on this machine: it starts its own AVD on
# its own port and points Gradle and adb only at that serial.
#
# Usage: scripts/local-ci.sh [--no-emulator] [--keep-emulator]
# Needs: JDK 17, ANDROID_HOME (or ANDROID_SDK_ROOT) with cmdline-tools, platform-tools and emulator installed.
# Output: build/local-ci/<timestamp>/ with SUMMARY.md, screenshots/, the HTML test report and logcat.
set -uo pipefail

RUN_EMULATOR=1
KEEP_EMULATOR=0
for arg in "$@"; do
  case "$arg" in
    --no-emulator) RUN_EMULATOR=0 ;;
    --keep-emulator) KEEP_EMULATOR=1 ;;
    *) echo "Unknown option: $arg" >&2; exit 2 ;;
  esac
done

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
STAMP="$(date +%Y%m%d-%H%M%S)"
OUT="$ROOT/build/local-ci/$STAMP"
mkdir -p "$OUT"
SUMMARY="$OUT/SUMMARY.md"

# A port pair other projects are unlikely to use (the default first emulator is 5554)
PORT="${AUSPICE_EMULATOR_PORT:-5584}"
SERIAL="emulator-$PORT"
AVD="${AUSPICE_AVD:-auspice_test_api34}"
API=34
case "$(uname -m)" in
  arm64|aarch64) ABI=arm64-v8a ;;
  *) ABI=x86_64 ;;
esac
IMAGE="system-images;android-$API;google_apis;$ABI"

SDK="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK" ]; then echo "Set ANDROID_HOME to your Android SDK" >&2; exit 2; fi
export ANDROID_HOME="$SDK"
SDKMANAGER="$(ls "$SDK"/cmdline-tools/*/bin/sdkmanager 2>/dev/null | sort -r | head -1)"
AVDMANAGER="$(ls "$SDK"/cmdline-tools/*/bin/avdmanager 2>/dev/null | sort -r | head -1)"
ADB="$SDK/platform-tools/adb"
EMULATOR="$SDK/emulator/emulator"

status() { echo "| $1 | $2 |" >> "$SUMMARY"; }
{
  echo "# Local CI run $STAMP"
  echo
  echo "Commit: \`$(git rev-parse --short HEAD)\` on \`$(git rev-parse --abbrev-ref HEAD)\`"
  echo
  echo "| Step | Result |"
  echo "|---|---|"
} > "$SUMMARY"
FAILED=0

echo "== cosmos unit tests"
if ./gradlew :cosmos:test --console=plain > "$OUT/cosmos-test.log" 2>&1; then status "cosmos unit tests" "pass"
else status "cosmos unit tests" "**FAIL** (see cosmos-test.log)"; FAILED=1; fi

echo "== app build, lint and JVM tests"
if ./gradlew build --console=plain > "$OUT/build.log" 2>&1; then status "App build, lint, JVM tests" "pass"
else status "App build, lint, JVM tests" "**FAIL** (see build.log)"; FAILED=1; fi
cp -r app/build/reports/lint-results-debug.html "$OUT/" 2>/dev/null || true

if [ "$RUN_EMULATOR" = 1 ] && [ "$FAILED" = 0 ]; then
  for tool in "$SDKMANAGER" "$AVDMANAGER" "$ADB" "$EMULATOR"; do
    if [ ! -x "$tool" ]; then echo "Missing SDK tool: $tool" >&2; status "Emulator tests" "**not run**: missing $tool"; exit 1; fi
  done

  echo "== system image and AVD ($IMAGE)"
  yes | "$SDKMANAGER" --install "$IMAGE" > "$OUT/sdkmanager.log" 2>&1
  if ! "$EMULATOR" -list-avds | grep -qx "$AVD"; then
    echo no | "$AVDMANAGER" create avd -n "$AVD" -k "$IMAGE" -d pixel_6 > "$OUT/avdmanager.log" 2>&1
  fi

  if "$ADB" devices | grep -q "^$SERIAL"; then
    echo "== reusing running $SERIAL"
  else
    echo "== booting $AVD on $SERIAL"
    "$EMULATOR" -avd "$AVD" -port "$PORT" -no-window -no-audio -no-boot-anim -no-snapshot \
      -gpu swiftshader_indirect -camera-back none > "$OUT/emulator.log" 2>&1 &
  fi
  "$ADB" -s "$SERIAL" wait-for-device
  for _ in $(seq 1 120); do
    [ "$("$ADB" -s "$SERIAL" shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ] && break
    sleep 2
  done
  for key in window_animation_scale transition_animation_scale animator_duration_scale; do
    "$ADB" -s "$SERIAL" shell settings put global "$key" 0
  done
  "$ADB" -s "$SERIAL" shell rm -rf /sdcard/Pictures/AuspiceTests > /dev/null 2>&1

  echo "== instrumented tests on $SERIAL only"
  # ANDROID_SERIAL limits Gradle's connected tests to this emulator
  if ANDROID_SERIAL="$SERIAL" ./gradlew connectedDebugAndroidTest --console=plain > "$OUT/emulator-tests.log" 2>&1
  then status "Emulator tests (API $API, $ABI)" "pass"
  else status "Emulator tests (API $API, $ABI)" "**FAIL** (see emulator-tests.log and report/)"; FAILED=1; fi

  "$ADB" -s "$SERIAL" pull /sdcard/Pictures/AuspiceTests "$OUT/screenshots" > /dev/null 2>&1 || true
  "$ADB" -s "$SERIAL" logcat -d > "$OUT/logcat.txt" 2>/dev/null || true
  cp -r app/build/reports/androidTests/connected "$OUT/report" 2>/dev/null || true
  cp -r app/build/outputs/androidTest-results/connected "$OUT/results" 2>/dev/null || true
  status "Screenshots" "$(ls "$OUT/screenshots" 2>/dev/null | wc -l | tr -d ' ') in screenshots/"

  if [ "$KEEP_EMULATOR" = 0 ]; then "$ADB" -s "$SERIAL" emu kill > /dev/null 2>&1 || true; fi
elif [ "$RUN_EMULATOR" = 1 ]; then
  status "Emulator tests" "skipped because an earlier step failed"
fi

echo
cat "$SUMMARY"
echo
echo "Everything is in $OUT"
exit "$FAILED"
