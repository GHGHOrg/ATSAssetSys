# plan/iterations/38a-development-app.md

Stage: Build. Status: approved 2026-11-21; decisions 0 and 1 (your A each) and the name (your A) settled the same day (sections 11 and 12). Not built. Branch iter-38a-development-app, from cr-04-release-process (fc92763); not created yet.
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7 (AC-65, section 9's last bullet). CR4 is split into 38a (this file), 38b (the release build proven) and 38c (the first release); see plan.md section 4 and decision 0.

## 1. Outcome
The debug build becomes the development app: its own id (`app.ghghorg.atsassets.debug`), its own name on the phone, test data only, the backup reminder due after 10 minutes, installed next to your app. ATS Check and the `check` build go. Your app (the debug build of 46 under the base id, with your data) is not touched in 38a; your data moves to the release build in 38c.

## 2. Spec refs
- AC-65 (the development app), section 9 (the last bullet: real data only in the release build, phone checks on the development app).
- Section 9 (Backup reminder, 7 days) and C15 for the release build, unchanged; the 10 minutes only in the development app (AC-65).
- K6 (section 14): `BackupReminder` stays in core; only the build's value changes.
- Standing checks: AC-35 for the development app over itself; AC-09 and AC-39 to AC-42 on the development app (no screen changes).

## 3. Out of scope
- The release build, its signing, R8 (open items 3 and 4), the CI job for tags, the release hook and the rollback rehearsal: 38b.
- The release key, the first tag and record, and the move of your data: 38c.
- Any change to a screen, a rule or a message other than the app's name in the development app.

## 4. Analysis (2026-11-21, by reading the code; nothing run)
- app/build.gradle.kts:16-46: `defaultConfig` sets `BACKUP_DUE_AFTER_MINUTES` 0 (7 days); `debug` is not declared and so uses AGP's defaults; `check` is `initWith(debug)` with `.check`, `-check` and 10.
- The name: `app_name` is "ATS Asset System" in app/src/main and "ATS Check" in app/src/check. A debug name goes in app/src/debug/res/values/strings.xml (new).
- BackupDueBuildTest (app/src/test) asserts that the debug build's value is 0. AC-65 makes it 10, so this test changes with the spec, and the 7 days of the release build need a test of their own (decision 1). The release build is not unit tested today (36 note 1).
- BackupReminderScreenTest draws Overall from a state, not from BuildConfig, so the debug build's 10 minutes should not reach it; checked in step 2 by reading how its state gets its time.
- No workflow, test or manifest names the app id (only package declarations; searched 2026-11-21). The screen tests run `connectedDebugAndroidTest`, which installs whatever id the debug build has.
- No database change. Each app id has its own Keystore entries and its own encrypted database, so the development app starts empty.
- After 38a a debug build installs as the development app, not over your app. Your app stays at versionCode 46 until 38c (risk 1).

## 5. Files
- app/build.gradle.kts: `debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-dev"; BACKUP_DUE_AFTER_MINUTES 10L }`; `defaultConfig` keeps 0 for release; the `check` build type removed; versionCode 47.
- app/src/debug/res/values/strings.xml (new): `app_name` (wording, section 12).
- app/src/check/ removed (its strings.xml; deleting asks first).
- Tests: BackupDueBuildTest (app/src/test) replaced by BackupDueDebugTest (app/src/testDebug, 10 minutes) and BackupDueReleaseTest (app/src/testRelease, the 7 days) (decision 1).
- .github/workflows/ci.yml: one step after the unit tests, the release test (decision 1).
- Documents: this file; plan.md (row 38a, section 9 item 4, section 11, the change log); CLAUDE.md (section 3's test command and CI line with the release test; section 3's install line names the development app; section 5: spec.md v7 and CR4's intent file; section 7 standing checks on the development app; the pitfalls that name ATS Check).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop.
1. This file, plan.md's lines (on branch iter-38a-development-app).
2. Tests first: the build tests per decision 1 (debug 10 minutes; release the 7 days), shown red. Then app/build.gradle.kts (debug suffix, name suffix, 10L; `check` removed), app/src/debug's name, app/src/check removed (asks first), ci.yml's release test step. BackupReminderScreenTest read as in section 4. The release test's added time measured locally (decision 1).
3. CLAUDE.md and plan.md section 9 item 4: the standing check AC-35 and the phone checks run on the development app; ATS Check's mentions updated.
4. versionCode 47; `assembleDebug`; the APK check (package `app.ghghorg.atsassets.debug`, its label, dex sizes against 46's); installed as a new app (your app not touched); test data into it (section 9); the phone check.
5. The Deploy review of 38a against section 2, findings by severity.

## 7. Stubs
None.

## 8. Risks
1. Your app is frozen at 46 from 38a until 38c: a debug build no longer installs over it. A fix you need in that time waits for the first release. Keep 38a to 38c close together.
2. The 7 days reaching only the release build: debug now sets 10. Without decision 1's test, nothing run checks the release build's value.
3. Test data: the development app starts empty and needs a price key and internet for a restore (AC-08). ATS Check's data goes in by a backup from ATS Check and a restore into the development app, before ATS Check is uninstalled.
4. A second price key copy: the development app holds one if you enter it (CR4 accepts this; ATS Check's copy goes with it).
5. The local build's intermittent failures (open item 60): re-run unchanged after reading the cause; the APK check before the install.

## 9. Proof
- Unit tests: step 2 (decision 1).
- Screen tests: none new; the 178 run on the pull request under the new id.
- The phone check (step 4), your app untouched:
  1. Before: your app shows versionCode 46 (Settings, Apps) and ATS Check is installed.
  2. ATS Check: More, Back up now, Save (a file for the development app).
  3. Install the development app (47). The home screen shows it next to your app and ATS Check, with its name (section 12). Your app still 46, opens with your data.
  4. The development app: the lock set up, Overall's three steps (empty). Settings, Prices: the key entered (your choice). More, Restore, the file of check 2: the summary and the result as on ATS Check.
  5. The development app over itself (47 again, `adb install -r`): opens with its data (AC-35 for the development app).
  6. The backup reminder: Back up now, then more than 10 minutes later a return and unlock shows the "more than 7 days ago" banner (the string is unchanged, as in 36).
  7. Standing checks on the development app: AC-09, AC-39 to AC-42 (no screen changed; a short pass).
  8. You uninstall ATS Check. Your app and the development app both still open.
  9. Logcat during checks 4 and 5 (`adb devices` first): no ticker, amount, note or portfolio name of the test data.

## 10. Research
- None from the web. `applicationIdSuffix`, `versionNameSuffix` and per-build-type source sets are AGP's; checked against the installed AGP 9.4.1 when built (the build script compiles), not from memory.

## 11. Decisions needed from you (Build)
0. The split of CR4 (Build; plan.md section 4; settled 2026-11-21: your A):
   - (A, chosen) Three iterations: 38a the development app, 38b the release build proven (open items 3 and 4, the CI job for tags, the release hook, the rollback rehearsal, AC-66), 38c the first release (key, signing, tag, record, the move of your data; AC-35, AC-63, AC-64). Each gives you something on its own and can be stopped after.
   - (B) Two: 38a, then the release build and the first release together. A larger iteration with a key and your data in it.
   - (C) One iteration for all of CR4. Against plan.md section 5's rule that an iteration with more than one capability is split before it starts.
1. How the release build's 7 days are tested (criteria per CLAUDE.md section 8; settled 2026-11-21: your A):
   - (A, chosen) Two small tests in variant source sets: app/src/testDebug (10 minutes) and app/src/testRelease (0, the 7 days), with CI and CLAUDE.md section 3's test command running `:app:testReleaseUnitTest --tests "*BackupDueReleaseTest"` as well (the filter, so the 1,511 shared unit tests don't run twice). BackupDueBuildTest leaves app/src/test, which every variant's unit tests include. Maintainability: two files of a few lines, each next to the build it checks, plus one CI step and one local command. Test run time: the release build's app and unit-test code compiled once more; expected 1 to 3 minutes more per CI run, measured in step 2 and brought back to you if clearly more; CI runs beside the screen tests (about 10 minutes), so a pull request's wait is expected not to grow. APK size: no difference (test code).
   - (B) One test on debug (10 minutes) and the release's value checked by hand in the release APK at each release (38c's APK check). Maintainability: one file, but a check that lives in a procedure. Test run time and APK size: no difference.
   - (C) One test on debug only; the release's 0 kept by review. Maintainability: least; nothing run checks the release value (risk 2). Test run time and APK size: no difference.

## 12. Wording to approve (D9)
- The development app's name on the phone: "ATS Dev" (approved 2026-11-21, your A; not chosen: "ATS Test", "ATS Asset System Dev"). Your app keeps "ATS Asset System". Seen on the phone at check 3 (D9).

## 13. Status and notes
- Draft 2026-11-21; approved the same day.

### 13.1 Steps
- Step 1 (2026-11-21): this file; plan.md (section 2's file list, rows 38a to 38c, section 8's install-route risk, section 11's rows, the change log, section 13). Only documents changed. The full run with --rerun-tasks green in 2m 39s: 1,511 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 findings as CLAUDE.md lists (report 09:35:03).

## 14. Departures from the plan
None yet.
