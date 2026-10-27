# CLAUDE.md

Rules, commands and pitfalls for working in this repo. Read this first.
Project: ATS Asset System, a personal Android asset tracker (one user, sideloaded).

## 1. Stage rules (Ruleset A)
- Say the current stage at the start of every response. Stages: Plan, Design, Build, Test, Deploy, Maintain.
- No code before an approved plan/plan.md. Iteration files are approved before their code.
- If the code departs from a plan file, update that plan file in the same commit.
- Run build, tests and lint and show the real output before saying anything is done.
- A bug fix starts with a failing test. Never weaken or delete a test.
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
- Style:  .\gradlew.bat spotlessCheck (read-only; CI runs it first). .\gradlew.bat spotlessApply rewrites files: run it only when asked, and show the diff first.
- All three, forced fresh: add --rerun-tasks (use it for lint too, see pitfalls)
- Install on the phone: & "$env:ANDROID_HOME\platform-tools\adb.exe" install -r app\build\outputs\apk\debug\app-debug.apk
  (adb is not on PATH on this machine. The APK path is confirmed. The install has been run on the phone:
  setup proof item 4 (2026-10-18), iteration 01 (2026-10-19) and the dependency-upgrade spike (2026-10-22).)
CI (.github/workflows/ci.yml) runs the style check and then the same three steps on ubuntu-26.04 with Java 25 (Temurin).

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
- Java: the project needs Java 25 (gradle/gradle-daemon-jvm.properties). core/ and app/ both compile at Java 11. The first reason (Kotlin 2.2.10 stopped at Java 24) is gone with Kotlin 2.4.21. The level stays 11 because app will use core/ from iteration 02. Revisit both together, not one.
- VS Code Kotlin extension (jetbrains.kotlin-server): after it was installed, the build failed with "jlink executable ... does not exist" under .vscode\extensions\jetbrains.kotlin-server-...\server\jbr. `.\gradlew.bat javaToolchains` showed two Java 25 installs: Android Studio's jbr (a JDK) and the extension's jbr ("Is JDK: false"). Fix that worked: `.\gradlew.bat --stop`, then rebuild. Cause is my reading, not proven: the daemon ran on the extension's Java. A property fix (org.gradle.java.installations.auto-detect=false plus .paths) is untested.
- Do not create local.properties: lint rejects the Windows path form. Use ANDROID_HOME.
- gradlew must stay LF and executable (.gitattributes, mode 100755). Do not retype it.
- In PowerShell, do not use .Split('text') to count a substring: it splits on each character.
- The CI runner is pinned to ubuntu-26.04 (moved from ubuntu-24.04 in the dependency-upgrade spike; CI green). Do not use ubuntu-latest.
- Lint after 02a shows 2 findings, both listed on purpose, neither silenced: NewerVersionAvailable for kotlinx-coroutines-test 1.9.0 (gradle/libs.versions.toml:17) and TypographyDashes at strings.xml:31 (the hyphens in the YYYY-MM-DD example). Do not silence a new notice: list it.
- Style checker: Spotless 8.10.4 with ktlint 1.8.0, targets set by hand in the root build.gradle.kts. @Composable functions are exempt from ktlint's function-naming rule through an override there; the property name is supported only by its effect.
- Dependabot has one group, kotlin (pattern org.jetbrains.kotlin.*). Whether it matches the Kotlin plugins is unverified until Dependabot has run once.
- A plain `.\gradlew.bat :app:lintDebug` after a change can leave the lint report stale (seen after commit L: the report time did not move). Use `--rerun-tasks` and check the report time.
- ktlint rules that Spotless enforced in 02a (not the full list; spotlessCheck decides): imports in ASCII order with java.* last, one parameter per line when a function has two or more, lines of at most 140 characters.
- developer.android.com reference pages and cs.android.com could not be fetched by my tools. Paste the page, or check the library with javap on the cached jar.
- A version-catalog entry cannot carry the @aar suffix (found in 02a for SQLCipher); check how the dependency is declared before moving it to the catalog.
- Room 3.0.3 (androidx.room3) has no allowDataLossOnRecovery (javap, 2026-10-27), so the code does not call it and calls no fallbackToDestructiveMigration. Room 3.1.0-alpha03 (2026-10-21) adds it; consider it once 3.1 is stable.
- SQLCipher: Logger.setTarget(NoopTarget()) switches off the Java client's Logcat output only, not the JNI layer or the SQLCipher core.
- The message "Unable to strip ..." appears in every release build and names libandroidx.graphics.path.so and libsqlcipher.so (both are packaged unstripped). It is known and not silenced.
- Dependency-upgrade spike: plan/spikes/dependency-upgrade.md, run before iteration 02. Its section 9 holds the results and what was not verified.

## 7. Standing checks for every iteration's screens
Criteria 9, 39 to 42, and the install-over-previous-build check (35).
