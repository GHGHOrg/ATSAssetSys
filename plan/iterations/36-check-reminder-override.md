# plan/iterations/36-check-reminder-override.md

Stage: Build. Status: approved 2026-11-20 (your A); decisions 1 (your A) and 2 (your B, 10 minutes) settled the same day (section 11). Branch iter-36-check-reminder-override, stacked on iter-35-restore-over-data (672176f); not created yet.
Source: the 32 file, decision 2 and phone check part B (13.2, checks 6 to 13); the 34 file, decision 2 (the `check` build type, "ATS Check"); your choices of 2026-11-20 (the override in the check build only, your B; its own iteration, your B).

## 1. Outcome
ATS Check, the check build only, treats a backup as due after a short time (10 minutes, decision 2) instead of 7 days, so 32's phone check part B can run now instead of on or after 2026-11-23. Your app, debug or release, keeps the 7 days of spec section 9 unchanged. Nothing a user sees in your app changes.

## 2. Spec refs
- None changed. Section 9 (Backup reminder, "older than 7 days") and C15 hold for every build you use; the override exists only in the build type `check`, which is never your app (34 decision 2).
- K6 (section 14) keeps the rule: `BackupReminder.due` stays in core, its 7 days the default.
- Criteria proved through part B, as in the 32 file: AC-18's banner sentences, AC-56's banner-button export, AC-57; the standing checks AC-09, AC-39 to AC-42. AC-35 for ATS Check over its own versionCode 45.
- Not proved by part B on ATS Check: the 7-day boundary itself on a phone (unit tested since 32 step 2: exactly 7 days not due, one millisecond more due).

## 3. Out of scope
- A user setting for the number of days (would be a change request: a new intent file and a spec.md edit; not asked for).
- Any change to your app's behaviour, the banner's wording or the screens.
- 32's part B on your app with your data: it shows by itself after 2026-11-22 and needs no check (same code).

## 4. Analysis (2026-11-20, by reading the code; nothing run)
- `BackupReminder.due(lastBackup, now, holdsData)` compares with `AFTER` (7 days) directly; the app calls it once, `AppState.backupDue` (AppViewModel.kt:1198). `AppViewModel` is built in MainActivity (MainActivity.kt:396).
- The check build type is `initWith(debug)` plus an id and a name (app/build.gradle.kts:35); app/src/check holds only its name. CI builds debug only.
- `buildConfig` is not switched on in app/build.gradle.kts, so no BuildConfig class exists today.
- "Now" is read at start, each return to the app, each unlock and after an export, no timer (32 decision 3). With a 10-minute time, the banner shows at the first return or unlock more than 10 minutes after the last backup.
- ATS Check's last backup is none: it was last restored in 35 (a restore resets the last backup, AC-18), and it holds data, so it shows the "never backed up" banner at once. A Back up now there makes a last backup; the "more than … ago" banner then follows after the short time.
- The banner's sentence on ATS Check still says "more than 7 days ago" (the string is not touched). For the check that is the sentence to approve (D9); its being untrue on ATS Check is accepted for a check build.
- No database change.

## 5. Files
- core/: BackupReminder.kt (`due` takes the time as a parameter, `AFTER` its default).
- app/: build.gradle.kts (`buildConfig = true`; a build field `BACKUP_DUE_AFTER_MINUTES`, 0 in defaultConfig, meaning core's 7 days, and 10 in `check`; versionCode 46); MainActivity.kt (passes the override, if any, to AppViewModel); ui/AppViewModel.kt (the time kept in AppState and used by `backupDue`).
- Tests: BackupReminderTest (core); BackupReminderFlowTest (app; a ViewModel made with a short time); a test that the debug build's field is 0.
- Documents: this file; plan.md (row 36, section 11, the change log); the 32 file gets decision 4 with part B's results (on iter-32, with its record, as planned).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-36-check-reminder-override, one pull request (draft until #195 is merged).
1. This file, plan.md's lines.
2. K6 and the app, tests first: `due` with a time; exactly that time not due, one millisecond more due; the default still 7 days (32's 8 tests unchanged); a ViewModel made with 10 minutes shows no banner at exactly 10 minutes after a backup and shows it 1 ms later; the debug build's field is 0. Then the build field, MainActivity, AppState.
3. versionCode 46; `assembleCheck`; the APK check (pitfall: dex sizes, a name step 2 added); ATS Check installed over its 45 (your app not installed); 32's part B on ATS Check (section 9); the results into the 32 file (decision 4) on iter-32 and into this file.

## 7. Stubs
None.

## 8. Risks
- 1. A short time reaching your app: the field is 0 in defaultConfig and set only in `check`; a unit test (run on debug) checks it is 0. The release build is not unit tested; it inherits defaultConfig, as debug does.
- 2. BuildConfig switched on for every variant adds a small class (see decision 1).
- 3. The local build's intermittent failures (open item 60): re-run unchanged after reading the cause; the APK check before the install.
- 4. Part B on ATS Check proves the banner, its buttons, the dismissal and the export from Overall on a phone, not your data's 2026-11-15 sentence; that sentence is the same string with another date.

## 9. Proof
- Unit tests: step 2.
- Screen tests: none new (no screen changes); the 178 of 35 run on the pull request.
- The phone check (step 3), 32's part B adapted to ATS Check, your phone's clock unchanged:
  1. ATS Check over its 45 (AC-35): opens with its data, no unreadable-data screen.
  2. Overall after the unlock: "Back up your data: you have never backed up." with Back up now and Dismiss (32 wording 1).
  3. Back up now, Save: the banner gone, "Backup saved. Last backup: <today> GMT+08:00" in its place.
  4. The home button, the app again at once, unlock: no banner. Then wait more than 10 minutes, the home button, the app again, unlock: "Back up your data: the last backup was <today> GMT+08:00, more than 7 days ago." (32 wording 2).
  5. Then 32's checks 7 to 13 as written, on ATS Check (for check 9, ATS Check's auto-lock is already "every time you leave the app").

## 10. Research
- None: `buildConfigField` and `buildFeatures.buildConfig` are AGP's; checked against the installed AGP when built, not from memory.

## 11. Decisions (Build, settled by you 2026-11-20)
1. How the check build passes its time (settled 2026-11-20: your A; criteria per CLAUDE.md section 8):
   - (A, chosen) A BuildConfig field, 0 = core's 7 days, 10 in `check`. Maintainability: one line per build type, read in one place. Test run time: no real difference. APK size: the generated BuildConfig class, expected under 1 KB in debug; R8 inlines the constant in release.
   - (B) A small Kotlin object with the same name in app/src/debug, app/src/release and app/src/check. Maintainability: three files to keep the same. Test run time and APK size: no real difference.
   - (C) An integer resource overridden in app/src/check/res. Needs a Context in AppViewModel, which takes none today. APK size: one resource, no real difference.
2. The short time (settled 2026-11-20): 10 minutes (your B). Not chosen: (A) 2 minutes; (C) 1 hour.

## 12. Wording to approve
None new. 32's section 12 sentences are approved during part B.

## 13. Status and notes
- Draft 2026-11-20; approved the same day (your A).

### 13.1 Steps
- Step 1 (2026-11-20): this file; plan.md (section 2's file list, row 36, K6's row, section 11's row, the change log). Only documents changed. The full run green: 1,505 tests (1 skipped), 178 screen tests compiled; "11 history" passed on try 2. Lint 10 findings (report 01:48:53): the 6 listed in CLAUDE.md and 4 new GradleDependency notices from versions published since 35, not from this step: lifecycle-runtime-ktx and lifecycle-viewmodel-compose 2.11.0 (2.11.1 available, gradle/libs.versions.toml:8), compose-bom 2026.09.00 (2026.11.01, :11), KSP 2.3.12 (2.3.14, :14). Listed, not silenced; for the dependency-upgrade round, and CLAUDE.md's lint list with its next edit.
- Step 2 (2026-11-20): tests first, shown red (compilation: `after`, `BuildConfig`, `backupDueAfter` not found): BackupReminderTest 4 new (exactly 10 minutes not due, 1 ms more due; no backup due only with data; 0 minutes means the 7 days; below 0 refused), BackupReminderFlowTest 1 new (a ViewModel given 10 minutes: not due at 10 minutes, shown 1 ms later, at a return to the app), BackupDueBuildTest 1 (the debug build's field is 0 and gives the 7 days). Then `BackupReminder.due(…, after)` and `BackupReminder.after(minutes)` in core; `AppState.backupDueAfter`, set from the ViewModel's new parameter; MainActivity passes `BackupReminder.after(BuildConfig.BACKUP_DUE_AFTER_MINUTES)`; app/build.gradle.kts: `buildConfig = true`, the field 0 in defaultConfig and 10 in `check` (accepted by AGP 9.4.1: the build script compiled). The full run green: 1,511 tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 01:56:37); "6 lotChoices", "7 Hide" and "11 history" passed on try 2. The Kotlin daemon failed once and Gradle compiled without it (open item 60's family).

## 14. Departures from the plan
- Step 2: core also gains `BackupReminder.after(minutes)`, turning the build's minutes into the time (0 = the 7 days, below 0 refused), so MainActivity holds no rule of its own and the conversion is unit tested. Section 5 named only `due`'s parameter.
- Step 2: the shared test helper `started` (AppViewModelTest.kt) takes the time, its default the 7 days; the build test is its own file, BackupDueBuildTest.kt.
