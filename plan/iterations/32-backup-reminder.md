# plan/iterations/32-backup-reminder.md

Stage: Build. Status: approved 2026-11-15 (your A); decisions 1 to 3 settled the same day (section 11). Step 1 (documents) done; steps 2 to 6 not built. The phone check of the "due" banner waits until 2026-11-23 or later (decision 2).
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

## 11. Decisions (Build, settled by you 2026-11-15)
1. The result of the banner's Back up now shows on Overall: under the banner while a backup is still due (a cancel or a failure), or in its place once the banner is gone (a completed backup); the line goes when you leave Overall (a tab, a portfolio) or return to the app (your A). Not chosen: (B) Backup and restore opens and shows it there.
2. The phone check of the "due" banner is done on or after 2026-11-23, the phone's clock unchanged; the code, tests and pull request go ahead now, and 32 is done only after that check (your B). Not chosen: (A) the phone's date moved forward 8 days for the check, Dismiss only; (C) no phone check of the due banner.
3. "Now" is read at start, at each return to the app, at each unlock and after an export, as the zone in 31; no timer (your A). Not chosen: (B) also every minute while the app is in view.

## 12. Wording to approve (D9; proposed in step 4, approved on the phone)
- The banner for "more than 7 days" and for "no backup yet"; the buttons "Back up now" and "Dismiss"; the result lines (31's, reused); TalkBack's reading of the banner.
- Nothing proposed yet.

## 13. Status and notes
- Draft 2026-11-15; approved the same day (your A).

### 13.1 Steps
- Step 1 (2026-11-15): this file, plan.md (section 11's row for 32, the change log with #189's merge and main's runs). Only documents changed.

## 14. Departures from the plan
- None yet.
