# Auspice

Birthday reminder app (Kotlin, Android) derived from Birday, plus a pure-Kotlin traditions engine in `cosmos/`.
Roadmap and status: `docs/PLAN.md`. Testing: `docs/TESTING.md`.

## Standing rules

- **Emulator testing after every major update.** A major update is a roadmap phase, a release, or any change to
  the database, notifications, or a main screen (home, details, settings, insert sheet). After one:
  1. Run `scripts/local-ci.sh` on the owner's computer (GitHub Actions has no credits until further notice).
     It runs the cosmos tests, the app build with lint, and the emulator tests on its own AVD and port.
  2. Read `build/local-ci/<timestamp>/SUMMARY.md` and the test report, and look at every screenshot.
  3. Record the result in `docs/test-reports/` and report it to the user with failures and what the
     screenshots show.
  4. Add or update tests in `app/src/androidTest` for the screens the update touched.
- **The owner's computer is shared with other projects.** Work only in this repository's folder. Never use,
  wipe or kill another emulator or device: `scripts/local-ci.sh` only talks to its own serial (`emulator-5584`
  by default). Do not change global SDK, Gradle or git settings.
- The cloud container cannot run the emulator (no KVM, `dl.google.com` blocked). Never claim a local emulator
  run from there; ask the owner to run the local session instead.
- When Actions credits return, the "Build" and "Emulator tests" workflows do the same checks on push.
- Keep tradition logic in `cosmos/` with JVM tests; the app only renders it. Run `./gradlew :cosmos:test`
  before pushing (on a JDK 21-only machine, point the module at toolchain 21 in a scratch copy).
- Unverified app changes go to a `phase-*` branch first; fast-forward `master` once `scripts/local-ci.sh`
  (or CI) passes completely.
- Present traditions as cultural trivia, never as advice or prediction.
