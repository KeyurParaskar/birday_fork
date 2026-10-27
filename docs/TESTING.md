# Testing Auspice

Three layers, from fastest to most realistic.

| Layer | What it covers | Where it runs | Command |
|---|---|---|---|
| cosmos unit tests | Every tradition calculation, the report, compatibility. Includes a day-by-day Hebrew calendar check from 1900 to 2100 | Anywhere with a JDK | `./gradlew :cosmos:test` |
| App build | Compiles the app, runs lint and the app's JVM unit tests | CI "Build" workflow, every push | `./gradlew build` |
| Emulator tests | The real app on Android 14: database migration from Birday's schema, home list, traditions section, compatibility dialogs, settings, insert sheet. Saves a screenshot of each screen | CI "Emulator tests" workflow | `./gradlew connectedDebugAndroidTest` with an emulator or device attached |

## When the emulator tests run

After every major update, which means:

- every push to `master` or to a `phase-*` branch,
- every `v*` release tag,
- on demand from the Actions tab ("Emulator tests", then "Run workflow").

## Reading the results

Each run uploads three artifacts:

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
