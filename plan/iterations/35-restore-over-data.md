# plan/iterations/35-restore-over-data.md

Stage: Build. Status: approved 2026-11-17 (your A); decisions 1 to 3 settled the same day (your A each; section 11).
Source: plan.md section 4 (row 35); spec section 8 (restore steps 5, 6 and 7, App initialization, Known limits), section 9 (Restore, Backup, Last backup, Android's file screens, the lock during a restore), section 3 ("app holds data"), section 6 (Backup export and restore entry), C3, D9; section 14 (K2, K6, K9); wireframe 10e frames F (markers 13 and 14, the Now column), G (markers 16 and 17) and H; the 34 file (the delete-and-load, the result, decision 3's stub, 13.3's lock rollback); the 31 file (the export and its results); the 32 file (decision 1: an export's result lasts only where it shows).

## 1. Outcome
On an app that holds data (section 3), the restore wizard has five steps. Step 3's summary shows the counts now beside the counts in the file (portfolios, transactions, cash entries), the cash balance after the restore and the holdings with shares, and Continue. Step 4 offers Back up now (optional; the same export as More's, with its result shown there, "Backup saved" when it completes) and the line that restoring cannot be undone; Replace all data stays inactive until RESTORE is typed (case and spaces ignored). Replace all data deletes everything and loads the file as one action, the same write as 34's, and step 5 shows 34's result or its failure. Nothing is changed before Replace all data. 34's line "restoring over data comes later" and its summary without Continue go.
After 35 your own app can replace its data from a file. The phone check does that only on ATS Check (decision 3).

## 2. Spec refs
- Section 8: restore step 5 (the counts now beside the file's when the app holds data; what is counted), step 6 (a backup export offered first, then a typed confirmation), step 7 (delete and load as one action, as in 34); App initialization (one confirmed action; all-or-nothing); Known limits (a bad file that loads cleanly is undone only by restoring a backup).
- Section 9: Restore replaces all current data, no merge; Backup (the same export); Last backup (only a completed export sets it; the restore then resets it); Android's file screens do not lock the app, and any other lock during a restore ends the wizard at step 1 with nothing changed.
- Section 3: "app holds data".
- Section 6 and wireframe 10e: frame F (marker 13: Now and In the file; marker 14: the balance and the holdings), frame G (marker 16: the export offer, "Backup saved"; marker 17: the typed word, Replace all data inactive until it matches), frame H (as 34), the note under the markers (Android's Back returns one step in steps 1 to 4).
- C3 (nothing changes before the last step), D9 (the wording).
- Section 14: K2 (the store, unchanged from 34), K6 (the restore), K9 (the screens).
- Criteria: AC-17 (restoring onto an app that holds data shows the count summary, offers a backup export first and requires a typed confirmation); AC-56's remaining parts (the summary's counts now, Replace all data inactive until RESTORE is typed, case and spaces ignored); AC-57 (the file screen of step 4's export does not lock under "every time you leave the app"; a lock on step 4 returns to step 1 with nothing changed); AC-61; the standing checks AC-09, AC-39 to AC-42 and AC-35.

## 3. Out of scope
- A merge, or restoring part of a file (section 8: none).
- A change to the backup file's format or to the store's delete-and-load (34).
- More than one price source (section 7).

## 4. Analysis (2026-11-17, by reading the code and the spec; nothing run)
- 34's write already deletes everything the app holds, data or not (34 decision 4), and rolls back on a lock (34 13.3). 35 needs no store change: Replace all data calls the same write. `canRestore` (RestoreModel.kt:238) and the view model's guard drop their "no data" condition once step 4 is passed.
- The counts now: section 8 counts the same things in the app as in the file (transactions are buys, sells, splits and their reversals; cash entries and their reversals; trade lines not counted). core's check counts the file's records; the app's own records, as the export reads them (`backupRecords`), can be counted by the same code, so both columns follow one rule. Taken when the summary shows. The wizard ends when its screen is left (AppViewModel.kt:1380), so the data cannot change while the counts show.
- Step 4's Back up now: the export of 31 refuses to start outside Backup and restore and the reminder (`beginBackup`, AppViewModel.kt:3434), and its result is dropped outside those screens (`withBackupResultWhereShown`, AppViewModel.kt:308; 32 decision 1). Both gain step 4. MainActivity's `backUpNow` already tells the lock about the file screen (AC-57), so it needs no change. While an export is under way, Replace all data and Back wait. A completed export sets the last backup; the restore then resets it (section 8, step 7).
- The typed word: "case and spaces ignored" is every space anywhere and any letter case ("restore", " Re Store "); decision 2. The word typed is held by the wizard and cleared whenever step 4 is left, so each arrival at step 4 starts empty.
- A lock on step 4 (idle time, the phone locking) ends the wizard at step 1 as on steps 1 to 3 (`onLocked`, 34). A lock while step 4's file screen is open for the export is not a leaving under "every time you leave" (31).
- Step 4 has a text field: at the largest font in landscape the keyboard must leave the field and Replace all data reachable (AC-61; CLAUDE.md pitfalls on the emulator's lower keyboard).
- Android's Back: step 4 returns to step 3; step 3 to step 1 as now (the wireframe's note).
- Database version: no schema change expected (no entity changes); checked in step 2.
- After 35, a real file over your real data is possible. The phone check restores only on ATS Check, which holds spec/example-backup.csv's made-up data (34 check 15).

## 5. Files
- core/: RestoreReport.kt or the check (the counts of a set of records, shared by the file's column and the app's).
- app/: ui/: AppViewModel.kt (Continue to step 4, the typed word, Back up now from step 4 and its result there, Replace all data, Back and Cancel, the lock), RestoreWizard.kt (step 4), RestoreModel.kt (the Now column, step 4's model; `restoreLater` goes), RestoreScreen.kt (frame F's Now column and Continue, frame G), AppContent.kt (the new actions), strings.xml (frame G's lines; 34's "comes later" line removed); app/build.gradle.kts (versionCode 45).
- Tests: core's count test; a flow test for restoring over data (RestoreOverDataFlowTest, or in RestoreLoadFlowTest); BackupFlowTest (the export from step 4); RestoreModelTest; RestoreScreenTest (frames F with Now, G; the keyboard).
- Documents: this file; plan.md (row 35, section 11, the change log); plan/open-items.md if anything is recorded.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-35-restore-over-data, stacked on iter-34-restore-to-empty-app at 245350d (as 34 on 33b); one pull request.
1. This file and plan.md's lines.
2. The counts now (core: one count for the file's records and the app's); the model's Now column and Continue on an app with data; tests first.
3. The view model: step 4, the typed word, Back up now from step 4 with its result there, Replace all data (34's write), Back, Cancel, a lock on step 4; tests first.
4. The models and the screens: frame F's Now column and Continue, frame G; 34's stub removed; the wording proposed (section 12); a wiring test.
5. The screen tests (AC-61): frame F with the Now column and frame G at the largest font in landscape, the typed field above the keyboard, TalkBack (Replace all data read as unavailable until the word matches).
6. versionCode 45, both installs, documents, the phone check, the wording, the pull request draft.

## 7. Stubs
- None planned.

## 8. Risks
1. Data loss: from 35 on, your own app can replace its data. Typed word, the export offer, nothing changed before Replace all data; the write is 34's, proven on the emulator and on ATS Check; the phone check's restores only on ATS Check (decision 3).
2. Step 4's export and the restore must not overlap: Replace all data waits for an export under way; tests.
3. The export from inside the wizard: its file screen and the lock (AC-57), its result shown on step 4 and dropped elsewhere (32 decision 1).
4. The keyboard over step 4's field at the largest font in landscape (AC-61; the emulator's keyboard is lower than the phone's).
5. Stacked on 34, 33b and 32: their merges and part B's results are carried in by merges.
6. Open item 60 (the local build's intermittent failures).

## 9. Proof
- Unit tests: the counts (the file's and the app's, one rule), the flow (step 4, the word, the export from step 4, Replace all data, Back, Cancel, a lock on step 4, a failed write), the models.
- Emulator: the screen tests.
- The phone check (decision 3): your app over 44 (AC-35), the wizard to step 4 on your own data and Cancel, nothing changed; on ATS Check, which holds the example's data: step 3's Now column, step 4's Back up now (the file saved; "Backup saved"), the word in several forms, Replace all data with another small file, the result, then the step-4 backup restored over it, every figure of 34 check 9 back (AC-17, AC-04); auto-lock "every time you leave the app" and step 4's file screen (AC-57); an idle lock on step 4; the largest font in landscape with the keyboard, TalkBack, light and dark; Logcat.

## 10. Research
- None expected.

## 11. Decisions (Build, settled by you 2026-11-17)
1. One iteration or a split (plan.md section 8: 33 to 35 are split before starting if the file shows more than one capability). My reading: one capability, restoring onto an app that holds data; the store's write, the result and the lock rollback exist from 34, and 35 adds the summary's Now column, step 4 and its wiring. Settled: one iteration (your A, 2026-11-17). Not chosen: (B) 35a (the Now column and step 4 with Replace all data inactive) and 35b (Replace all data working).
2. "Case and spaces ignored" (section 6, AC-56). Settled: every space anywhere and any letter case ("restore", " RESTORE ", "Re Store" match), the words as written (your A, 2026-11-17). Not chosen: (B) only leading and trailing spaces, any case.
3. The phone check and your own data. Settled: restores only on ATS Check; on your app the install over 44 (AC-35), the wizard to step 4 with your real counts, and Cancel (your A, 2026-11-17). Not chosen: (B) also a full restore on your app from a backup made at its own step 4.

## 12. Wording to approve (D9; proposed in step 4, approved on the phone)
- Proposed in step 4 (strings.xml under its 35 comment, not approved):
  - Frame F with data: the column head "Now" (`restore_now_column`; "In the file" is 33b's); TalkBack's row "Portfolios: 1 now, 2 in the file" (`restore_count_row`); the line under the summary "The file passed every check. Continue leads to the last step, where you confirm that the app's data is replaced." (`restore_data_note`, departure 1); "Continue".
  - Frame G: "Step 4 of 5. Nothing has been changed.", "Back up what is in the app now", "Optional. Recommended: restoring cannot be undone.", "Everything in the app, data and settings, is deleted and replaced by the file. Only a backup restore brings it back.", "Type RESTORE to confirm (case and spaces ignored)", "Replace all data". Back up now, Cancel and the export's results are 31's approved strings ("Backup saved. Last backup: <date> <zone>" and the others).
  - Changed: 34's "Step 4 of 4. Restoring…" takes the number of steps ("Step 5 of 5. Restoring…" over data).
- Removed: 33b's line that restoring over data comes later (`restore_summary_note`).

## 13. Status and notes
- Draft 2026-11-17, in the working tree on iter-34-restore-to-empty-app at 245350d; decisions 1 to 3 settled and the file approved the same day (your A each); the branch iter-35-restore-over-data is created with your yes before step 1's commit.

### 13.1 Steps
- Steps 1 to 5 are built without stops between them, each held as a tree when its full run is green and committed one per step after step 5 (your instruction, 2026-11-17).
- Step 1 (2026-11-17): this file; plan.md (row 35's refs, section 11's row, the change log). Branch iter-35-restore-over-data created from 245350d (your A). The full run green: 1,489 tests (1 skipped), lint's 6 (report 09:57:33), 170 `@Test` lines compiled.
- Step 2 (2026-11-17): core's `RestoreCounts.of(portfolios, transactions, cashEntries)` (section 8, step 5's three counts; the trade lines left out), now used by the check's summary too, so both columns follow one rule. The wizard's `now`, taken from the screen state when the summary shows on an app that holds data; the model's `RestoreNowModel` and `canContinue`. Tests first: BackupCheckTest `theCountsOfRecords_areTheSummarysCounts`, RestoreModelTest `theSummary_showsTheCountsNow_andContinue_onAnAppThatHoldsData`, and the new RestoreOverDataFlowTest (2) did not compile ("Unresolved reference 'RestoreCounts'", "No parameter with name 'now' found"). The full run green at the first try: 1,493 tests (1 skipped), lint's 6 (report 10:05:40); HoldingsScaleTest retried four checks once each (netWorth and lots ONE_HOLDING, realizedGains and history SPREAD), each passing at try 2 (24a decision 1).
- Step 3 (2026-11-17): the wizard's step CONFIRM (step 4) and `typed`; `RestoreWord.matches` (decision 2: every whitespace character removed, then RESTORE in any case). The view model: `continueRestore` (the summary of an app that holds data, to step 4 with nothing typed), `typeRestoreWord` (step 4 only), `canRestore` (34's Restore on the summary of an app with no data, or Replace all data on step 4 once the word matches, never while an export is under way), `restoreNow` (34's write, from either), Back on step 4 to the summary with the counts kept and the word cleared, Back and Cancel waiting for step 4's export; Back up now starts from step 4 too (`beginBackup`), and its result shows there and goes when step 4 is left (`withBackupResultWhereShown`, 32 decision 1). A lock on step 4 returns to step 1 through 33b's path (`onUnlocked`); a lock while the restore over data is written rolls it back through 34's. MainActivity unchanged: its `backUpNow` already tells the lock about the file screen (AC-57). RestoreScreen draws nothing for step 4 yet (step 4). Tests first: RestoreOverDataFlowTest's 9 new tests did not compile ("Unresolved reference 'continueRestore'", "'CONFIRM'", "'RestoreWord'"). The first two full runs were red on spotlessCheck only (a comment in RestoreScreen's `when`, then the same comment at its line's end); the comment removed, the third run green: 1,502 tests (1 skipped), lint's 6 (report 10:31:32); HoldingsScaleTest retried Hide SPREAD once, passing at try 2.
- Step 4 (2026-11-17): the model's `RestoreConfirmModel` (step 4: the word typed; Replace all data active once it matches with no export under way and no save running; Back up now; the writing indicator; the export's result with the last backup) and `Restores.screen`'s new inputs (the export's stage and result, the last backup as shown, a save running); Back waits on step 4 while an export is under way; `restoreLater` gone; the step count stays five to the end of a restore over data (a wizard with counts now), four onto an empty app. The screen: frame F's Now column (the heads "Now" and "In the file", each count row read by TalkBack as one sentence), the line under the summary and Continue; frame G (the export offer in a card with 31's result line, `BackupStatusLine`; what Replace all data does, the field for the word with capitals and no autocorrect; Replace all data dashed while inactive, as `InactiveDashedButton`, now internal; Cancel); "Restoring…" with the number of steps; the screen keeps clear of the keyboard with `imePadding()`, as the entry screens do. AppContent wires Continue, the word and MainActivity's export. strings.xml: 10 strings proposed, not approved (section 12), `restore_restoring` takes the number of steps, 33b's `restore_summary_note` removed. Tests first: RestoreModelTest `step4_theExportOffer_andTheTypedWord` and `aRestoreOverData_countsFiveSteps_toItsEnd`, RestoreOverDataFlowTest `theState_wiresStep4sModel` did not compile ("No parameter with name 'stage' found", "Unresolved reference 'confirm'"); RestoreModelTest's 34 test lost its two `restoreLater` lines (the stub is gone, departure 2). Three screen tests named 34's stub or the old "Restoring…" text; changed here so the androidTest code compiles (departure 3): `onAnAppThatHoldsData_theSummaryHasNoRestore` became `onAnAppThatHoldsData_theSummaryReadsTheCountsNow_andOffersContinue`, the landscape summary test draws the Now column, Continue and the line, and the "Restoring…" test names "4". The full run green at the first try: 1,505 tests (1 skipped), lint's 6 (report 10:38:39), 170 `@Test` lines compiled.

## 14. Departures from the plan
1. Step 4: the summary on an app that holds data has a line the wireframe does not show (frame F has none): that the file passed every check and that Continue leads to the confirmation, as 34's summary on an empty app has its own line. Proposed for your approval (section 12).
2. Step 4: RestoreModelTest's 34 test `theSummary_offersRestore_onlyOnAnAppWithNoData` lost its two `restoreLater` lines: the stub they checked (34 decision 3) is removed as planned (section 1); the test's other lines stay. Not a weakened test: what it checked no longer exists.
3. Step 4, not step 5: three screen tests that named 34's stub or the old "Restoring…" text were changed with the strings, so that the androidTest code still compiles at the end of step 4 (section 13.1).
