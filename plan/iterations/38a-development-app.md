# plan/iterations/38a-development-app.md

Stage: Deploy. Status: Done: pull request #216 merged as 9247e00, main green (13.3). Built 2026-11-21 (steps 1 to 4, phone checks 1 to 9 passed); Deploy review done (section 13.2), no High or Medium finding. Approved 2026-11-21; decisions 0 to 2d and the name settled the same day (sections 11 and 12). Branch iter-38a-development-app, from cr-04-release-process (fc92763).
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7 (AC-65, section 9's last bullet). CR4 is split into 38a (this file), 38b (the release build proven) and 38c (the first release); see plan.md section 4 and decision 0.

## 1. Outcome
The debug build becomes the development app: its own id (`app.ghghorg.atsassets.debug`), its own name on the phone, test data only, the backup reminder due after 10 minutes, installed next to your app. ATS Check and the `check` build go. Your app (the debug build of 45 under the base id, with your data) is not touched in 38a; your data moves to the release build in 38c.

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
- After 38a a debug build installs as the development app, not over your app. Your app stays at versionCode 45 until 38c (risk 1; 46 was ATS Check, read over adb in step 4).

## 5. Files
- app/build.gradle.kts: `debug { applicationIdSuffix = ".debug"; versionNameSuffix = "-dev"; BACKUP_DUE_AFTER_MINUTES 10L }`; `defaultConfig` keeps 0 for release; the `check` build type removed; versionCode 47 and versionName "0.38.0" (decisions 2 and 2d).
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
4. versionCode 47 and versionName "0.38.0" (decisions 2 and 2d); `assembleDebug`; the APK check (package `app.ghghorg.atsassets.debug`, versionName `0.38.0-dev.47` (decisions 2a and 2d), its label, dex sizes against 46's); installed as a new app (your app not touched); test data into it (section 9); the phone check.
5. The Deploy review of 38a against section 2, findings by severity.

## 7. Stubs
None.

## 8. Risks
1. Your app is frozen at 45 from 38a until 38c: a debug build no longer installs over it. A fix you need in that time waits for the first release. Keep 38a to 38c close together.
2. The 7 days reaching only the release build: debug now sets 10. Without decision 1's test, nothing run checks the release build's value.
3. Test data: the development app starts empty and needs a price key and internet for a restore (AC-08). ATS Check's data goes in by a backup from ATS Check and a restore into the development app, before ATS Check is uninstalled.
4. A second price key copy: the development app holds one if you enter it (CR4 accepts this; ATS Check's copy goes with it).
5. The local build's intermittent failures (open item 60): re-run unchanged after reading the cause; the APK check before the install.

## 9. Proof
- Unit tests: step 2 (decision 1).
- Screen tests: none new; the 178 run on the pull request under the new id.
- The phone check (step 4), your app untouched:
  1. Before: your app is at versionCode 45 and ATS Check at 46 (read over adb, `dumpsys package`; Settings, Apps shows only the versionName "1.0").
  2. ATS Check: More, Back up now, Save (a file for the development app).
  3. Install the development app (47). The home screen shows it next to your app and ATS Check, with its name (section 12). Your app still 45, opens with your data.
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
1a. Decision 1 departed in step 2 (settled 2026-11-21: your A): AGP 9.4.1 made no release unit-test task. (A, chosen) verify the AGP property in the cached jar, then set it; (B) decision 1's B instead; (C) a Gradle check of the generated BuildConfig. Verified and set (section 14).
2. Version names (settled 2026-11-21, before step 4; your wish for semantic versioning with three parts, and B for their meaning): versionName is MAJOR.MINOR.PATCH. The first release is `1.0.0`, tagged `v1.0.0` (AC-63: the tag equals versionName); the development app shows `1.0.0-dev` (a semver pre-release). versionCode stays a plain number raised with every build. Meaning (B, chosen): MAJOR a big milestone (for example performance over time, spec section 10); MINOR new features; PATCH fixes only; a backup format or schema change is shown in the release record (AC-63), not in the number. Not chosen: (A) MAJOR when the backup format version rises, MINOR for features or a schema change; (C) MAJOR for any format or schema change. versionName "1.0" becomes "1.0.0" in step 4; the rule is applied at each release from 38c.
2b. MAJOR revisited (settled 2026-11-21: your A; replaces decision 2's meaning of MAJOR, after your semver table's "breaking change → 2.0.0"): MAJOR rises for a big milestone (for example performance over time) and for a breaking change. A change is breaking when, after the release, (a) the previous release cannot read its backups (the backup format version rises, spec section 8; for example per-holding dividends), or (b) keeping your data needs more than `adb install -r` (backup, uninstall, install, restore; for example a new signing key, C21, or a change of encryption), or (c) your existing records show different figures than before (for example a changed rounding rule, 4.10, or realized gains worked out another way, 4.4), or (d) the app no longer runs on an Android version it supported (minSdk raised). A database schema change that the app migrates is not breaking (MINOR). MINOR and PATCH as in decision 2. Not chosen: (B) breaking means only (a); (C) milestones only, as decision 2 first said.
2c. Two gaps in 2b (settled 2026-11-21, after UI examples per part): (e) a release that removes or replaces a feature you use (for example Copy report, C16, or the Hidden holdings view) is breaking, so MAJOR, every time (your A; not chosen: (B) MINOR, (C) MAJOR only when no other way to the result remains). A figure shown differently while its stored and worked-out value stays the same is MAJOR under (c) only when the shown precision changes (fewer or more places, cents dropped or added, for example quantities at 4 places instead of 8); a change of style only (a thousands separator, a date format) is MINOR (your C; not chosen: (A) MAJOR for any changed text of a figure, (B) MINOR whenever the value stays the same).
2d. The first release's number (settled 2026-11-21, before step 4; replaces decision 2's `1.0.0` and `v1.0.0` and the example `1.0.0-dev.47` in 2a): the first release is not yet the productive app, and 0.1.0 would look like a project just started. The first release (38c) is `0.38.0`, tagged `v0.38.0`, the iteration number used once as the start; the development app shows `0.38.0-dev.47` from step 4. Until 1.0.0, 2b and 2c move down one place: a milestone or a breaking change, (a) to (e), raises MINOR (0.38.0 to 0.39.0); a feature or a fix raises PATCH (0.38.0 to 0.38.1). After the first release the middle number no longer follows the iteration numbers; each release record (AC-63) names its iteration. 1.0.0 comes when you call the app productive (what that means is asked at 38c); from then on 2b and 2c apply as written. Your B; not chosen: (A) the middle number always equals the iteration that ships the release (no warning for a breaking change; a fix-only iteration still raises it; two lettered iterations that both release break the link). Before that, also not chosen: start at 0.1.0 with the usual 0.x reading (features and breaking changes both raise MINOR), 0.1.0 with 2b and 2c moved down one place, and 1.0.0 kept for 38c.
2a. Pre-release labels (settled 2026-11-21: your A; after you showed a dev, test, rc and release table): the development app shows `x.y.z-dev.n`, n being the versionCode (`1.0.0-dev.47`, from `versionNameSuffix` built from `defaultConfig`'s versionCode, so no number is kept by hand); your daily app shows `x.y.z`. No `-test` (tests run in CI on the debug build; nothing installed) and no `-rc` (a candidate signed with your key would go onto your real-data app, and the shipped `x.y.z` would be a second build). Acceptance is the smoke check (AC-64) before the GitHub Release is published; a failed one marks that version withdrawn in its record, and the fix ships as the next PATCH. Not chosen: (B) dev, rc and release; (C) all four.

## 12. Wording to approve (D9)
- The development app's name on the phone: "ATS Dev" (approved 2026-11-21, your A; not chosen: "ATS Test", "ATS Asset System Dev"). Your app keeps "ATS Asset System". Seen on the phone at check 3 (D9).

## 13. Status and notes
- Draft 2026-11-21; approved the same day.

### 13.1 Steps
- Step 1 (2026-11-21): this file; plan.md (section 2's file list, rows 38a to 38c, section 8's install-route risk, section 11's rows, the change log, section 13). Only documents changed. The full run with --rerun-tasks green in 2m 39s: 1,511 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 findings as CLAUDE.md lists (report 09:35:03).
- Step 2 (2026-11-21): tests first: BackupDueDebugTest (app/src/testDebug, 10 minutes) shown red (`AssertionError at BackupDueDebugTest.kt:13`, the debug build still 0); BackupDueReleaseTest (app/src/testRelease, 0 = the 7 days) could not run: AGP 9.4.1 made no `testReleaseUnitTest` (decision 1a, your A). With `android.onlyEnableUnitTestForTheTestedBuildType=false` in gradle.properties (the name found in gradle-9.4.1.jar's BooleanOption class), the release test ran and passed (a guard for a value already right). Then app/build.gradle.kts (debug: `.debug`, `-dev`, 10L; `check` removed), app/src/debug's name "ATS Dev", ci.yml's step "Release build tests"; BackupDueBuildTest and app/src/check removed (asked). BackupReminderScreenTest draws from an AppState whose time defaults to the 7 days (AppViewModel.kt:749), so the debug build's 10 minutes reach only MainActivity (MainActivity.kt:407). The full run with --rerun-tasks green in 1m 45s: 1,511 unit tests (1 skipped; BackupDueBuildTest out, BackupDueDebugTest in), 178 screen tests compiled, lint the same 10 (report 09:46:06); the release test alone, forced, 32 s locally (1 test, passed). The debug APK: package app.ghghorg.atsassets.debug, versionName 1.0-dev, label "ATS Dev".
- Step 3 (2026-11-21): CLAUDE.md (section 3: the test command with the release test, the install line and CI line naming the development app; section 4: the versioning convention, decisions 2 to 2c; section 5: CR4's intent file and spec.md v7; section 6: the uid line for both ids; section 7: the standing checks on the development app) and plan.md section 9 item 4. No heading changed, so neither table of contents did. CLAUDE.md named ATS Check nowhere. Only documents changed. The first full run failed in `:app:kspDebugKotlin` (FileNotFoundException on the generated AtsDatabase_AutoMigration_1_2_Impl.kt; open item 60); the same command re-run unchanged was green in 1m 57s: 1,512 unit tests (1 skipped; the 1,511 plus the release test, now in the run), 178 screen tests compiled, lint the same 10 (report 12:24:12).
- Step 4 (2026-11-21): app/build.gradle.kts: versionCode 47, versionName "0.38.0" (decisions 2 and 2d), debug suffix `-dev.${defaultConfig.versionCode}` (decision 2a). The full run with --rerun-tasks green in 3m 1s: 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 14:57:01). The APK: aapt2 reads package app.ghghorg.atsassets.debug, versionCode 47, versionName 0.38.0-dev.47, label "ATS Dev"; 44,619,614 bytes against 46's 44,619,606; every classesN.dex the same size as 46's but classes8.dex (61,968 against 61,964), which holds `0.38.0-dev.47`; no dex holds `1.0-dev`. Phone checks, all passed (your reports):
  1. Over adb: your app versionCode 45 (1.0), ATS Check 46 (1.0-check), no `.debug`. The file had said 46 for your app; corrected to 45 in sections 1, 4, 8 and 9 (46 was ATS Check).
  2. ATS Check backed up. Saving shows only "Backup saved. Last backup: …", not a summary (my instruction was wrong); you noted the Cash balance and each portfolio's cost instead.
  3. `adb install -r`: Success; ATS Dev 47 (`0.38.0-dev.47`, uid 10446) next to your app (45, uid 10454) and ATS Check (46, uid 10444), both unchanged; ATS Dev on the home screen; your app opens with your data.
  4. ATS Dev: lock set up, Overall's three steps, the key entered, Restore of check 2's file; the Cash balance and each portfolio's cost match ATS Check's.
  5. ATS Dev over itself (`adb install -r`, 47; firstInstallTime 15:12:02, lastUpdateTime 15:17:07): opens with its data (AC-35 for the development app).
  6. Back up now, then more than 10 minutes later the "more than 7 days ago" banner (10L reaches the development app).
  7. AC-09 and AC-39 to AC-42 on ATS Dev: all okay.
  8. ATS Check uninstalled; your app and ATS Dev both open.
  9. Logcat of uid 10446 during checks 4 and 5 (264 lines, 15:13:58 to 15:17:36, processes 13788 and 14719): only Android's and libraries' tags, no line from the app's code; none of finnhub, token, http, key, ticker, portfolio, amount, note, quantity, symbol, backup, restore, password ("api" only in Android's hiddenapi notice, "sqlcipher" only the library load). Not searched for the test data's own tickers, names and notes (not known to me). Seen, for the Deploy review: W/System "A resource failed to call release" 13 times at 15:15:29 (during check 4; the log does not name the resource) and E/BiometricPromptCompat "Unable to cancel authentication. BiometricFragment not found." twice.

### 13.2 Deploy review (step 5, 2026-11-21)
Against sections 1 to 3, AC-65 (spec.md:470) and section 9: the code diff from main read; no workflow, screen test or manifest names the app id; BuildConfig read only at MainActivity.kt:407; backup, restore and Finnhub streams closed with `use`; `allowBackup="false"`. AC-65 met in every part and checked on the phone (that ATS Check is gone rests on your report, not on adb). No High or Medium finding; nothing needs your approval as risky.
1. Low: the 178 screen tests have not run under the new id, and CI's "Release build tests" step has not run on GitHub (32 s locally). Both run on the pull request; merge only when it is green, and its time is recorded.
2. Low: "A resource failed to call release" 13 times during check 4's restore; the source is not named. Older than 38a (no code that opens anything changed). Open item 71.
3. Low: this file's status line still said "Not built"; rewritten (your yes).
4. Info: E/BiometricPromptCompat "Unable to cancel authentication" twice, a library message when a cancel finds no prompt; no effect seen.
5. Info: the development app is debuggable and may hold a copy of the price key (CR4 accepts it); your daily app is itself the debuggable debug build 45 until 38c (risk 1).
6. Info: with decision 1a's property, a plain `gradlew test` also runs every release unit test (my reading, not run); CLAUDE.md and CI name their tasks.
7. Info: check 9 did not search for the test data's own terms.

### 13.3 Pull request and merge (2026-11-21)
- Pull request #216 (iter-38a-development-app at 974fda9, pushed once and created with your yes; reviewer msfsgh), green at the first attempt: build 4m 33s with "Release build tests" 26 s; the screen tests 178 under the new id over 3 shards (8m 1s, 8m 54s, 9m 28s) and the count job; CodeQL and Analyze (actions). Finding 1 of 13.2 closed.
- Merged by you as 9247e00 (07:56:27 UTC). Main green on 9247e00 (gh, read only): CI 44882534206 in 5m 9s, "Release build tests" 28 s; Screen tests 44882534137 in 10m 41s (shards 8m 28s, 10m 29s, 8m 42s, the count job green); Push on main 44882533769. CodeQL on main not read.

## 14. Departures from the plan
- Step 2: gradle.properties gains `android.onlyEnableUnitTestForTheTestedBuildType=false`, not listed in section 5: AGP 9.4.1 makes unit tests only for the tested build type, so the release build had no `testReleaseUnitTest` (decision 1a, 2026-11-21, your A: the name verified in the cached AGP jar first).
