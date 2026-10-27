# Testing Auspice

Three layers, from fastest to most realistic.

| Layer | What it covers | Where it runs | Command |
|---|---|---|---|
| cosmos unit tests | Every tradition calculation, the report, compatibility. Includes a day-by-day Hebrew calendar check from 1900 to 2100 | Anywhere with a JDK | `./gradlew :cosmos:test` |
| App build | Compiles the app, runs lint and the app's JVM unit tests | Owner's computer via `scripts/local-ci.sh`; CI "Build" workflow when Actions has credits | `./gradlew build` |
| Emulator tests | The real app on Android 14: database migration from Birday's schema, home list, traditions section, compatibility dialogs, settings, insert sheet. Saves a screenshot of each screen | Owner's computer via `scripts/local-ci.sh`; CI "Emulator tests" workflow when Actions has credits | `scripts/local-ci.sh` |

## Running everything locally

`scripts/local-ci.sh` replaces both GitHub workflows. It needs JDK 17 and an Android SDK (`ANDROID_HOME`) with
cmdline-tools, platform-tools and the emulator. On first run it downloads the Android 14 system image for the
machine's CPU (x86_64 or arm64) and creates the `auspice_test_api34` AVD.

It is safe on a machine that other projects use: it boots its own emulator on port 5584 (`emulator-5584`),
sets `ANDROID_SERIAL` so Gradle installs only there, and shuts only that emulator down at the end. Override
with `AUSPICE_EMULATOR_PORT` or `AUSPICE_AVD` if those clash. Options: `--no-emulator` for a quick build-only
check, `--keep-emulator` to leave it running between runs.

Results land in `build/local-ci/<timestamp>/`: `SUMMARY.md`, `screenshots/`, `report/` (HTML), logs and
logcat. Write a short summary of each major run to `docs/test-reports/`.

## When the emulator tests run

After every major update (a roadmap phase, a release, or a change to the database, notifications or a main
screen): locally with `scripts/local-ci.sh`. When GitHub Actions has credits, the "Emulator tests" workflow
also runs on every push to `master` or a `phase-*` branch, every `v*` tag, and on demand.

## Reading CI results

Each GitHub run uploads two artifacts:

- `emulator-screenshots`: one PNG per tested screen, named in order (`01_home.png`, `02_details_top.png`...).
- `emulator-test-report`: the HTML test report and the emulator's logcat.

A failing test fails the workflow, but the screenshots and logcat are still uploaded.

## Why not in the cloud dev container

The Claude Code cloud container has no hardware virtualisation (no `/dev/kvm`), which the Android emulator
needs, and its network policy blocks `dl.google.com`, where the SDK and system images come from. GitHub's
Linux runners have both, so the emulator runs there.

## Requirements

GitHub Actions must be able to start jobs. On a private repository the jobs use the account's Actions
minutes; an emulator run takes roughly 15 to 25 minutes. Public repositories run on standard runners for free.

## Adding tests

Put new instrumented tests in `app/src/androidTest/java/app/auspice/`. Call `TestSupport.prepareApp()` before
launching the activity: it skips the welcome screen and the rating prompt, stops looping animations and seeds
two birthdays. Use `TestSupport.screenshot("NN_name")` to add a screenshot to the artifact.
