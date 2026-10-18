# CLAUDE.md

Rules, commands and pitfalls for working in this repo. Read this first.
Project: ATS Asset System, a personal Android asset tracker (one user, sideloaded).

## 1. Stage rules (Ruleset A)
- Say the current stage at the start of every response. Stages: Plan, Design, Build, Test, Deploy, Maintain.
- No code before an approved plan/plan.md. Iteration files are approved before their code.
- If the code departs from a plan file, update that plan file in the same commit.
- Run build, tests and lint and show the real output before saying anything is done.
- A bug fix starts with a failing test. Never weaken or delete a test.
  Exception: plan/setup.md S3. The placeholder test in core/ is replaced by real K1 tests in iteration 02.
- Say plainly what was not run or not verified.

## 2. Question rules (Ruleset B)
- Every question asked must be tagged with a specific stage of the playbook, for example [Build].
- Do not ask questions or accept answers that are not meant for the tagged stage.

## 3. Commands (Windows PowerShell, repo root)
Environment (user variables, set once): JAVA_HOME = Android Studio's jbr folder; ANDROID_HOME = the Android SDK folder.
There is no local.properties file on purpose (see pitfalls).
- Build:  .\gradlew.bat :core:assemble :app:assembleDebug
- Test:   .\gradlew.bat :core:test :app:testDebugUnitTest
- Lint:   .\gradlew.bat :app:lintDebug
- All three, forced fresh: add --rerun-tasks
- Install on the phone: & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
  (adb is not on PATH on this machine. The APK path is confirmed. The install itself has NOT been run yet;
  setup proof item 4 runs it first.)
CI (.github/workflows/ci.yml) runs the same three steps on ubuntu-24.04 with Java 25.

## 4. Conventions
- One branch per iteration: iter-NN-name. One pull request per iteration.
- Commit messages name the iteration and the spec refs.
- Every write to transactions or cash goes through K1 (spec section 14, rule 1).
- core/ is pure Kotlin/JVM: no Android classes, no database, no network.
- K3 and K9 only read data. K4 only writes prices and instrument data. These boundaries are kept by review and checked at Deploy.
- Exact arithmetic only (spec 4.10).

## 5. Where things are
- intent/intent.md (v52, final), spec/spec.md (v4, final, one file), spec/wireframes/, plan/plan.md, plan/setup.md.
- Spec sections: find them by heading, for example "## 4. Business rules".
  Sections 2 (concerns, C-numbers), 3 (domain), 4 (rules), 6 (views), 7 (prices), 8 (backup file), 9 (security), 11 (criteria), 14 (components, K-numbers).
- Each iteration file in plan/iterations/ lists its own spec sections, criteria and examples.

## 6. Known pitfalls
- No floating point for money or quantity (4.10).
- Never log or send amounts, tickers, quantities, notes or portfolio names (spec section 9).
- No analytics or crash-reporting library.
- Docs in this repo use CRLF. Keep line endings, do not retype files.
- [UNVERIFIED] items in spec.md are not facts. Check them on the phone before relying on them.
- Message wording is D9: decided on the phone, not on paper.
- The Compose theme file in app/ is the source of truth for colors and type. Figma is only the starting point.
- Java: the project needs Java 25 (gradle/gradle-daemon-jvm.properties). Kotlin 2.2.10 only targets up to 24, so core/ pins Java and Kotlin to 11.
- Do not create local.properties: lint rejects the Windows path form. Use ANDROID_HOME.
- gradlew must stay LF and executable (.gitattributes, mode 100755). Do not retype it.
- In PowerShell, do not use .Split('text') to count a substring: it splits on each character.
- The CI runner is pinned to ubuntu-24.04. ubuntu-latest is moving to Ubuntu 26.04 (Oct 19 to Nov 19, 2026).
- Lint currently shows 8 library-version notices. They are cleared by the dependency-upgrade spike, not by silencing the checks.
- The Kotlin style checker is not chosen yet. The dependency-upgrade spike picks it together with the Kotlin version.
- Planned spike: plan/spikes/dependency-upgrade.md, after iteration 01 and before iteration 02.

## 7. Standing checks for every iteration's screens
Criteria 9, 39 to 42, and the install-over-previous-build check (35).
