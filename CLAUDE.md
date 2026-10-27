# Auspice

Birthday reminder app (Kotlin, Android) derived from Birday, plus a pure-Kotlin traditions engine in `cosmos/`.
Roadmap and status: `docs/PLAN.md`. Testing: `docs/TESTING.md`.

## Standing rules

- **Emulator testing after every major update.** A major update is a roadmap phase, a release, or any change to
  the database, notifications, or a main screen (home, details, settings, insert sheet). After one:
  1. Make sure it reaches a branch the "Emulator tests" workflow runs on (`master`, `phase-*`, or a `v*` tag),
     or start the workflow by hand.
  2. Wait for the run, then read the test report and look at every screenshot in the `emulator-screenshots`
     artifact.
  3. Report the result to the user with the failures, if any, and what the screenshots show.
  4. Add or update tests in `app/src/androidTest` for the screens the update touched.
- The cloud container cannot run the emulator (no KVM, `dl.google.com` blocked). Use the workflow, never claim a
  local emulator run.
- Keep tradition logic in `cosmos/` with JVM tests; the app only renders it. Run `./gradlew :cosmos:test`
  before pushing (on a JDK 21-only machine, point the module at toolchain 21 in a scratch copy).
- Unverified app changes go to a `phase-*` branch first; fast-forward `master` once CI is green.
- Present traditions as cultural trivia, never as advice or prediction.
