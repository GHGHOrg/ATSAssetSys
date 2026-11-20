# plan/iterations/32-backup-reminder.md

Stage: Build. Status: approved 2026-11-15 (your A); decisions 1 to 3 settled the same day (section 11). Steps 1 to 6 built on iter-32-backup-reminder (versionCode 42); not merged. The phone check's part A passed 2026-11-15 (13.2). Pull request #190: its first run green, 145 of 145 screen tests; the Deploy review done, four low findings, three fixed (13.3). Part B passed 2026-11-20 on ATS Check through iteration 36 (decision 4, replacing decision 2's date; 13.2); the wording approved the same day (section 12). Done: pull request #190 merged as 0034aa5, main green (13.4).
Source: plan.md section 4 (row 32); spec section 9 (Backup reminder), section 3 ("app holds data"), section 6 (view 1, the navigation outline), section 14 (K6, K9), D9, C15; wireframe 01 (state A, marker 1); intent "Backup reminder"; the 31 file (the export flow; open item 67).

## 1. Outcome
When a backup is due, Overall shows a banner at the top, under its title. A backup is due when the last one is more than 7 days old, or there is none and the app holds data. The banner says why, with two buttons. Back up now starts the same export as on Backup and restore. Dismiss hides the banner until you next open the app: each return to the app, or a restart, shows it again while a backup is still due. The dismissal is not saved. Once a backup completes, the banner is gone until the next one is due.

## 2. Spec refs
- Section 9, Backup reminder: an in-app banner on Overall, not an Android notification; due when older than 7 days (elapsed time) or none while the app holds data; the dismissal lasts until the app is next opened and is not saved; known limit: the app knows only when it last exported.
- Section 3: "app holds data" is at least one portfolio or cash entry, so the opening balance exists.
- Section 6: view 1 "the backup reminder banner when due"; the navigation outline "its button starts a backup export"; the accessibility rules, and the states rule that no message rests on color alone.
- C15: the 7-day check uses elapsed time and no zone.
- Section 14: K6 supplies the reminder state; K9 shows the banner.
- D9: the banner's wording and its button are decided in Build and approved on the phone.
- Criteria: AC-18's banner sentences; AC-56's "the same export as the Overall banner button"; AC-57 (the file screen from the banner does not lock); AC-61; and the standing checks AC-09, AC-39 to AC-42 and AC-35.
- Not met here: AC-18's "after a restore the banner shows" comes with 34's restore; the rule already covers it, as a restore resets the last backup to none.

## 3. Out of scope
- Restore (33 to 35).
- An Android notification (intent: out of scope).
- Saving into Downloads without the file screen (open item 67).
- A reminder for an interrupted backup: More and Backup and restore already show that warning (31 decision 7); the banner follows the last completed backup only, as the spec says.

## 4. Analysis (2026-11-15, by reading the code; nothing run)
- The export exists (31). `beginBackup` starts only on Backup and restore today, so it must also start from Overall; MainActivity's Back up now, with its lock exemption, is reused as it is.
- "Due" needs the time now, which the state does not keep. A `now` is kept and read where the zone is read (start, each return to the app, each unlock) and after an export (decision 3).
- The dismissal is a flag in the state, cleared at start and at each return to the app (MainActivity's onStart, `onAppShown`), never stored. A return from the file screen also clears it; that matters only after a cancelled export, when the banner showing again is right.
- K6: `BackupReminder.due(lastBackup, now, holdsData)` in core, pure. "Older than 7 days" is strictly more than 7 × 24 hours of elapsed time; a last backup dated after now (the phone's clock moved back) is not due.
- Screen tests on the emulator: the emulator's app holds data after the tests that set an opening balance and has never backed up, so the banner shows on Overall in every later MainActivity screen test. A test that finds or taps something on Overall by position or as the first match may be affected (risk 1).
- No database change.

## 5. Files
- core/: BackupReminder.kt (new; K6).
- app/: ui: AppViewModel.kt (`now`, the dismissal, the export from Overall and its result there), OverallModel.kt (the banner's model), OverallScreen.kt (the banner), AppContent.kt, strings.xml; MainActivity.kt (Overall's button reaches `backUpNow`); app/build.gradle.kts (versionCode 42).
- Tests: BackupReminderTest (core); BackupFlowTest (the export from Overall, the dismissal); OverallModelTest or a banner model test; screen tests (new or added to).
- Documents: this file; plan.md (row 32, section 11, the change log with #189's merge and main's runs).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-32-backup-reminder, one pull request.
1. This file, plan.md's lines, #189's merge record.
2. K6: `BackupReminder.due`; tests first: exactly 7 days not due, one millisecond more due; none with data due; none without data not due; a backup dated after now not due.
3. The ViewModel: `now` and when it is read; the dismissal and its reset; Back up now from Overall and its result there (decision 1); tests first.
4. The model and the screen: the banner (why it is due, Back up now, Dismiss), the result line, strings; a wiring test (CR2).
5. The screen tests (AC-61): the banner's TalkBack text and buttons; the largest font in landscape; then every existing MainActivity test on the emulator with the banner showing, any that breaks fixed in the test support only, never by hiding the banner.
6. Documents, versionCode 42, the phone check (decision 2), the wording, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. Existing screen tests: the banner shows on the emulator's Overall from the first test that sets an opening balance; tests that start on Overall may break if they match by position or count, or tap the first button. They run on GitHub only, so this shows in step 5 or on the pull request.
- 2. The 7-day boundary and the phone's clock: elapsed time between the stored moment and the phone's clock. Moving the clock back hides the banner until the time passes again; moving it forward shows it early. A known limit of C15.
- 3. "Due" is read at moments, not continuously (decision 3): a backup that turns due while the app stays in view shows at the next return or unlock.
- 4. The phone check of "more than 7 days": your last backup is 2026-11-15, so the banner does not show by itself until after 2026-11-22 (decision 2).
- 5. The export from Overall reuses 31's flow and lock exemption; open item 67's limits apply here too.
- 6. The local build's intermittent failures (open item 60): a failed run is re-run unchanged after reading its cause; before the install the APK and its dex files are compared.

## 9. Proof
- Unit tests: steps 2 to 4.
- Screen tests: the banner's TalkBack text and its two buttons; the largest font in landscape; all 136 earlier tests still green with the banner present.
- The phone check, over versionCode 41, your data kept (AC-35): no banner while the last backup is less than 7 days old (now); on or after 2026-11-23 (decision 2): the banner's text; Back up now (the file screen, Save, the banner gone, the result line, More's line moved); Dismiss (gone; back after leaving and returning; back after a restart while still due); the largest font in landscape, TalkBack, light and dark, airplane mode; Logcat.
- Not provable on your phone: "none and the app holds data" (you have a backup; unit and screen tested); after a restore (34).

## 10. Research
- None expected: the Android calls are 31's, reused.

## 11. Decisions (Build, settled by you 2026-11-15; decision 4 on 2026-11-20)
1. The result of the banner's Back up now shows on Overall: under the banner while a backup is still due (a cancel or a failure), or in its place once the banner is gone (a completed backup); the line goes when you leave Overall (a tab, a portfolio) or return to the app (your A). Not chosen: (B) Backup and restore opens and shows it there.
2. The phone check of the "due" banner is done on or after 2026-11-23, the phone's clock unchanged; the code, tests and pull request go ahead now, and 32 is done only after that check (your B). Not chosen: (A) the phone's date moved forward 8 days for the check, Dismiss only; (C) no phone check of the due banner. Its date is replaced by decision 4.
3. "Now" is read at start, at each return to the app, at each unlock and after an export, as the zone in 31; no timer (your A). Not chosen: (B) also every minute while the app is in view.
4. Part B runs now on ATS Check, not on or after 2026-11-23 on your app: iteration 36 (plan/iterations/36-check-reminder-override.md) makes the check build alone treat a backup as due after 10 minutes, your app keeping section 9's 7 days; the phone's clock unchanged (2026-11-20, 36 decisions; your B). Part B on ATS Check also shows the "never backed up" banner, which your phone could not (section 9). Not chosen: (A) wait until 2026-11-23; (C) a user setting for the days, a change request; (D) the phone's date moved forward.

## 12. Wording to approve (D9; proposed in step 4, approved on the phone)
- The banner for "more than 7 days" and for "no backup yet"; the buttons "Back up now" and "Dismiss"; the result lines (31's, reused); TalkBack's reading of the banner.
- Proposed in step 4 (2026-11-15); approved as written on the phone 2026-11-20, on ATS Check in part B (your A; the sentence of item 2 said "more than 7 days ago" there after 10 minutes, as expected of the check build):
  1. No backup yet (`backup_due_never`): "Back up your data: you have never backed up."
  2. An old backup (`backup_due_old`): "Back up your data: the last backup was <date> <zone>, more than 7 days ago.", the date and zone as More's line shows them (C15), for example "2026-11-15 GMT+08:00".
  3. The buttons: "Back up now" (31's `backup_now`) and "Dismiss" (`backup_due_dismiss`).
  4. The line of an export started from the banner: 31's five lines, unchanged (the 31 file, section 12).
  5. TalkBack reads the banner's sentence as written, then each button by its label; the export's line is read when it appears.

## 13. Status and notes
- Draft 2026-11-15; approved the same day (your A).

### 13.1 Steps
- Step 1 (2026-11-15): this file, plan.md (section 11's row for 32, the change log with #189's merge and main's runs). Only documents changed. Commit 80547f7.
- Steps 2 to 5 were built without a stop between them (your instruction, 2026-11-15), each step's state held for its own commit; committed together on your yes.
- Step 2 (commit 12da2d1): `BackupReminder.due` and its `AFTER` (7 days) in core; BackupReminderTest, 8 tests, written before the rule. The full run green at the first try: 1,337 tests (1 skipped), lint's 6 listed findings (report 17:44:17), 136 screen tests compiled.
- Step 3 (commit 4e40aa4): the state's `now` and `reminderDismissed`; `backupDue`, `backupReminderShown`; `onAppHidden`, `onAppShown`, `dismissBackupReminder`; `beginBackup` from Overall; the result kept only where it shows. BackupReminderFlowTest, 17 tests. The first run was red: spotlessCheck (one function's form) and two tests. One was the new test's own expectation (the file's first line is the header row, not the `format` row). The other was 31's `backUpNow_offTheScreen_orWithOneUnderWay_startsNothing`, which stood on Overall with no backup, where the reminder's button now starts an export on purpose: it now stands on Overall with a recent backup, on More and on Cash; nothing was removed from it. The second run green: 1,354 tests (1 skipped), lint's 6 (report 17:54:34); HoldingsScaleTest measured "11 history" again (pass on try 3).
- Step 4 (commit e0e6403): `BackupDue`, `BackupReminderModel`, `Backups.reminder`; the banner in OverallScreen; `BackupStatusLine` shared with Backup and restore; AppContent passes Back up now to Overall; three strings. BackupReminderModelTest, 7 tests, and one wiring test. The first run was red on the wiring test's own expectation (the test zone `ZoneOffset.UTC` prints "Z", as in 31); the second green: 1,362 tests (1 skipped), lint's 6 (report 18:01:34); "11 history" passed on try 2.
- Step 5 (commit 4b2039f): BackupReminderScreenTest, 9 tests (7 for TalkBack and the states, 1 at the largest font in landscape, 1 on MainActivity); 145 `@Test` lines. Every MainActivity test's path was read: none finds or taps Overall's items by position, and those that tap one scroll to it first. Two tests that draw Overall directly tapped without scrolling (AllocationTalkBackTest's cash row, NetWorthTalkBackTest's net worth); both now scroll first. The Gradle daemon crashed twice before the green run, each time in the JVM's JIT compiler (first `relocInfo_x86.cpp:110` in a C1 thread compiling an R8 method after 1 h 9 min of daemon life, then an access violation in a C2 thread compiling `URLClassLoader.findClass` 19 s into a new daemon); the logs were moved to the scratchpad. The third run of the same command green: 1,362 tests (1 skipped), lint's 6 (report 20:59:44); "11 history" passed on try 2. The screen tests are compiled only until the pull request runs them.
- Step 6: versionCode 42; this file; plan.md.

### 13.2 Phone check (versionCode 42 over 41, your data kept)
Part A, now (the last backup is 2026-11-15, under 7 days old):
1. AC-35: the app opens with your data as before, no unreadable-data screen. No database change in 32.
2. Overall (after the unlock): no banner and no line under the title; net worth, the allocation and the portfolios as before.
3. More: "Last backup: 2026-11-15 GMT+08:00" as before.
4. More, Backup and restore, Back up now, then cancel the file screen: "Backup not saved. The last backup date is unchanged." on that screen, as in 31. Then the Overall tab: no line there.
5. The home button, then the app again, unlock: Overall, still no banner.

Part B, on or after 2026-11-23, the phone's clock unchanged (decision 2; run instead on ATS Check on 2026-11-20, decision 4, as the 36 file's 13.2 orders them):
6. Overall after the unlock: the banner under the title, "Back up your data: the last backup was 2026-11-15 GMT+08:00, more than 7 days ago.", with Back up now and Dismiss.
7. Dismiss: the banner goes. The Cash tab and back: still gone. Turn the phone: still gone.
8. The home button, then the app again, unlock: the banner is back. Dismiss again; close the app from recent apps and open it: the banner is back.
9. Settings, Auto-lock: note your choice, then "every time you leave the app". Overall, Back up now on the banner: Android's file screen with backup-<today>.csv; cancel: back on Overall with no unlock asked (AC-57), the banner still there and "Backup not saved. The last backup date is unchanged." under it.
10. A portfolio's row, then "<" back to Overall: the line is gone, the banner stays.
11. Back up now, Save: back on Overall with no unlock asked; the banner gone and "Backup saved. Last backup: <today> GMT+08:00" in its place; More shows the same date. Back to Overall: the line is gone. Then Settings, Auto-lock: your own choice back.
12. AC-39 to AC-42 need the banner, so before check 11 if wanted: the largest font in landscape (the banner's text, both buttons and the line reached by scrolling), TalkBack (the sentence, then each button), light and dark, airplane mode (Back up now, Save works offline).
13. Logcat (spec section 9), as in 31's check 18, over one Back up now from the banner.

Results:
- Part A (2026-11-15, versionCode 42 installed over 41; before the install the APK's 10 dex files were compared with an earlier build and the names 32 added were found in them): checks 1 to 5 all as planned (your "1-5 ALL YES"): your data as before, no banner and no line on Overall, More's line unchanged, the cancelled export's line on Backup and restore only, and no banner after leaving and returning.
- Part B (2026-11-20, on ATS Check versionCode 46 from iter-36-check-reminder-override, ececeee, installed over its 45; your app untouched at 45; decision 4): all okay (your "1-10 ALL OKAY" and "12 ALL OKAY"; the 36 file, 13.2). In place of check 6, ATS Check (no backup since its restore in 35) showed "you have never backed up"; after Back up now and Save, no banner at once, and the "more than 7 days ago" sentence with today's date at the first return more than 10 minutes later. Checks 7 to 10 and 12 as written (ATS Check's auto-lock already "every time you leave the app"); 11 and 13: the save from the banner with no unlock asked, the banner gone, the line, More's date; Logcat clean of data (no ticker, portfolio name, note, amount, key or file name in ATS Check's lines or any other).

### 13.3 Pull request and Deploy review (2026-11-15)
- Pull request #190 (six commits, 80547f7 to de8e6e7; reviewer msfsgh; not a draft). Its first run was green at the first attempt (gh, read only): CI (3 min 45 s; "Scale test retries": none), CodeQL and Analyze (actions), and the screen tests, 145 of 145 (11 min 40 s): the 9 new tests passed, and the 136 earlier ones passed with the banner present (risk 1 did not come about).
- The Deploy review (Ruleset A rule 6), by reading the six commits against spec section 9, AC-18, AC-56, AC-57, wireframe 01 marker 1 and this file; nothing new was run. No high or medium finding; four low:
  1. A result that arrived while the app was out of view (the app left during the write) was cleared at the return, unseen; for a failed export that loses "an unfinished file may remain". Fixed, the failing test first (`aResultThatArrivesWhileTheAppIsAway_isShownAtTheReturn_andGoesAtTheNext`, red with "expected NOT_SAVED_FILE_MAY_REMAIN but was null"): only a result that was showing when the app left goes at the return.
  2. With a backup due and no data in the app, Dismiss was inactive with Back up now. Not reachable today (a backup needs data, and data cannot be removed; a restore resets the last backup). Fixed: the model has `canDismiss`, which needs no data.
  3. plan.md's row 32 named "9, AC-18" only. Fixed: it also names AC-56's banner-button part and AC-57's file screen from the banner.
  4. MainActivity's telling the ViewModel that the app left (`onAppHidden` in onStop, a turn excepted) has no automated test; part B's checks 7 and 8 prove it on the phone. No change.
- What held: the 7-day rule only in core (K6); the dismissal reset at "each return to the app after leaving it, or a restart" and never stored; the banner's button on 31's export and lock exemption, not a copy; no new logging, permission or manifest change; the banner shows a date and a zone only.
- For the wording's approval: wireframe 01 labels the button "Back up"; the app uses 31's "Back up now" on both screens.
- CLAUDE.md gained two pitfalls: `ZoneOffset.UTC` prints "Z" in tests, and an element on every Overall also shows where a test draws Overall directly.
- The phone had versionCode 42 without the two fixes until the next install (since 2026-11-17 your app has 45, from 35, with both); part B ran on ATS Check 46, which has them.
- #190's second run (run 43722810947, on 55de165): the screen tests red on one test, `PortfolioDeleteKeyboardLandscapeTest.theDialog_reachesDownToTheKeyboard_andShowsTheNameField_inLandscape` ("The dialog ends at 0.0 px …"), green on a re-run; a test-timing defect with no app change, recorded as open item 68 and fixed in 33b step 2 (on iter-33b, so not on this branch).

### 13.4 Merge (2026-11-20)
- The record of part B (885b55d) pushed; #190 green on it (CI "Scale test retries": none; the screen tests 145 of 145). GitHub then reported the branch out of date with main (1fff5e4, the avd-memory, dependabot-findings and two-emulators spike records, documents only; no conflict): you pressed "Update branch" (your B), GitHub's merge commit 0394bf0, signed by GitHub. #190 green again on 0394bf0 (CI 4m 37s, "Scale test retries": none; CodeQL; the screen tests 145 of 145, 16m 50s).
- Pull request #190 merged by you as 0034aa5, 10 commits (80547f7 to 0394bf0). On main after the merge: CI (3m 6s, "Scale test retries": none), CodeQL and the screen tests (145 of 145, 17m 24s) green (gh, read only). Merge record on docs/iter-32-merged, with CLAUDE.md's edits (open item 33's reminder dropped, the lint list at 10, seven pitfalls from 33b and 35).

## 14. Departures from the plan
- Step 3: a turn of the phone keeps the reminder dismissed. Section 4 reset the dismissal at each of MainActivity's onStart, which also runs after a turn. MainActivity's onStop now tells the ViewModel the app left (`onAppHidden`), a turn excepted, and `onAppShown` resets only after that.
- Step 3: an unlock alone does not reset the dismissal or drop Overall's line. After an idle lock in view the reminder stays dismissed until the app is left and returned to, or restarted; section 4 said the same, and this records it as chosen. The line stays through an unlock because the file may have been written while the app was locked (31 decision 7).
- Step 3: Dismiss also drops a result line under the banner (decision 1 did not say).
- Step 3: the result is dropped in one place for every way of leaving: `KeptStateFlow` applies `withBackupResultWhereShown` to each state it is given, so the result lasts only on Backup and restore or on Overall. Section 5 named no change to KeptFields.kt.
- Step 3: `now` is also set when the ViewModel is made, before the records load, so a state is never without it.
- Step 4: the reminder has its own model, `BackupReminderModel` in BackupModel.kt, read as `AppState.backupReminder`; OverallModel is unchanged (section 5 named OverallModel.kt).
- Step 4: while the file is written from Overall, the banner stays with both buttons inactive and "Writing the backup file…" under it.
- Step 5: the banner shows in every Overall state that holds data and has no backup, also where a test draws Overall directly, not only in MainActivity tests (risk 1 named those only); two such tests were given a scroll before their tap.
