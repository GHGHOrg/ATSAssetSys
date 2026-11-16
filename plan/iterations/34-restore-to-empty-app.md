# plan/iterations/34-restore-to-empty-app.md

Stage: Build. Status: approved 2026-11-16 (your A); decisions 1 to 4 settled the same day (section 11).
Source: plan.md section 4 (row 34); spec section 8 (restore steps 5 and 7, App initialization, Known limits), section 9 (Restore, Last backup, Backup reminder), section 3 ("app holds data"), section 6 (Backup export and restore entry), C3, D6, D9; section 14 (K2, K4, K6, K9); wireframe 10e frames F (marker 15, the fresh-install form) and H (markers 18 and 19); the 33b file (the check, the summary, decisions 1 and 2: the records and instruments kept in memory for 34); open items 33 and 66.

## 1. Outcome
On an app that holds no data (section 3: no portfolio and no cash entry), a file that passed the check can now be restored. The summary's button reads Restore (frame F marker 15: four steps, no step 4). Restore deletes every setting and record the app holds and loads the file's, as one action, so the app is never left half loaded (section 8, step 7). Step 5 then shows what was loaded, that holdings show "no price" until the first price refresh and that More shows "never backed up"; Go to Overall opens the app as it now is, with the backup reminder showing. If the write fails, "Not restored. Nothing was changed." with Close.
On an app that holds data, the summary keeps its 33b form with no Restore button and a line that restoring over data comes later (35).

## 2. Spec refs
- Section 8: App initialization (delete all settings and data, then load the file, one confirmed action; all-or-nothing); restore step 5 (on a fresh install only the file's counts), step 7 (delete and load as one action; the app's own ids; the file's price source becomes the selected source; "no price" until the first refresh; the last backup reset: "never backed up"); Known limits (a restored lock setting can be weaker; a bad file that loads cleanly is undone only by restoring a backup).
- Section 9: Restore replaces all current data, needs internet, resets the last-backup state; the backup reminder shows when there is no backup and the app holds data.
- Section 3: "app holds data".
- Section 6 and wireframe 10e: frame F marker 15 (fresh install: no Now column, Continue reads Restore, four steps), frame H (marker 18: the result; marker 19: the failure, Close only; no way back into the wizard).
- C3 (nothing changes before the last step), D6 (the file's source takes effect after loading), D9 (the wording).
- Section 14: K2 (the store: one delete-and-load), K4 (the instruments the check verified), K6 (the restore), K9 (the screens).
- Criteria: AC-04 (backup then restore on a clean install reproduces portfolios, transactions, cash entries, lots, cash balance and holdings; "no price" until the first refresh; "never backed up"); AC-25 (spec/example-backup.csv restores with the expected results, from Excel and Notepad forms); AC-56's restore result parts (the result shows the counts, More shows "never backed up", the Overall banner shows); AC-18's "after a restore the banner shows" (from 32); AC-57 (a lock during the restore); AC-61; the standing checks AC-09, AC-39 to AC-42 and AC-35.
- Not met here: AC-17 and AC-56's step 4 (an app that holds data: the export offer and the typed word) are 35's.

## 3. Out of scope
- Restoring onto an app that holds data: step 4, the Now column, the typed RESTORE (35).
- More than one price source (section 7: one source; the file's `price_source` names the only one).
- A change to the backup file's format.

## 4. Analysis (2026-11-16, by reading the code and the spec; nothing run)
- 33b keeps a passed file's records and the held tickers' instruments in memory (`restoreReady`, 33b decision 2); 34 stores exactly those (33a decision 1).
- The store has no call that replaces everything. Room's multi-table saves are `@Transaction` DAO methods (AtsDatabase.kt), and 29's PortfolioDeleteDaoTest checks one on the emulator. A new `replaceAll` follows both: every table cleared and the file's rows inserted in one transaction, so a failure leaves the store as it was (C3).
- "Delete all settings and data" on an app with no data still meets stored things: the settings row (auto-lock, tab order, the price key, the backup moment and mark), instruments and prices, the sector and geography lists with their remembered texts, hidden holdings and overrides (decision 4). The price key is decision 1 (open item 66).
- After the write the screen state reloads from the store, as at start, so every view shows what is stored; prices are none ("no price"), and the first refresh runs (section 8, step 7).
- The lock's settings change with the file's auto-lock (section 8, Known limits: it can be weaker); the lock follows the stored choice as it does today (20).
- A clean install on your phone would delete your data. The phone check needs a separate install (decision 2).
- Database version: no schema change expected (new DAO calls only); checked in step 2.

## 5. Files
- core/: RecordStore.kt (`replaceAll`).
- app/: data/ (AtsDatabase.kt's DAO: the delete-and-load; RoomRecordStore.kt); ui/: AppViewModel.kt (Restore on an empty app, the write, the reload, the result), RestoreWizard.kt, RestoreModel.kt, RestoreScreen.kt (frame F's Restore, frame H), strings.xml; app/build.gradle.kts (versionCode 44; decision 2's install).
- Tests: a DAO test on the emulator (androidTest, as PortfolioDeleteDaoTest); RestoreFlowTest (the restore, the failure, the reload); RestoreModelTest; the round trip of AC-04 and AC-25 in unit tests (the writer's file and spec/example-backup.csv checked, loaded through a store in memory, and compared); RestoreScreenTest (frames F and H).
- Documents: this file; plan.md (row 34, section 11, the change log); plan/open-items.md (66 closed by decision 1; 33 by the phone check if decision 2 allows).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-34-restore-to-empty-app, stacked on iter-33b-restore-check-screens at 1f610cf (as 33b on 32); one pull request.
1. This file and plan.md's lines.
2. K2: `replaceAll` in RecordStore and the Room store, one transaction (decisions 1 and 4); tests first, the DAO test on the emulator.
3. The ViewModel: Restore on an empty app, the write, the reload, the result and the failure; on an app with data no Restore (35); tests first, AC-04's and AC-25's round trips.
4. The models and the screens: frame F's Restore, frame H; the wording proposed (section 12); a wiring test.
5. The screen tests (AC-61).
6. Decision 2's install, versionCode 44, documents, the phone check, the wording, the pull request draft, and the open item 33 reminder.

## 7. Stubs
- On an app that holds data, the summary has no Restore and a line that restoring over data comes later (35).

## 8. Risks
1. Data loss: a defect in the delete-and-load deletes data. Tests first; the store's transaction proven on the emulator; the phone check never on your own data (decision 2).
2. A file that passes the check but loads differently from the replay (33a decision 1 says the replay's records are loaded as made). AC-04's and AC-25's round trips in unit tests compare every record.
3. The price key (open item 66): after a restore with no key, the first refresh fails until a key is entered.
4. The lock after a restore can be weaker (section 8, Known limits).
5. Stacked on 33b, itself stacked on 32: part B's fixes and #193's changes are carried in by merges.
6. Open item 60 (the local build's intermittent failures).

## 9. Proof
- Unit tests: the store call (with a store in memory), the flow, the round trips (AC-04, AC-25).
- Emulator: the DAO test; the screen tests.
- The phone check on decision 2's install (never your own data): a backup of a small app, then a restore of it onto the empty install; spec/example-backup.csv with its expected figures; the Excel and Notepad forms (AC-25); "no price" then a refresh; More's "never backed up"; Overall's reminder; a failure if it can be made; a lock during the restore; the largest font in landscape, TalkBack, light and dark; Logcat. Open item 33 ("Set the opening balance first" for New portfolio) on the same fresh install.

## 10. Research
- None expected.

## 11. Decisions (Build, settled by you 2026-11-16)
1. The price source key at a restore (open item 66): deleted with every other setting, as section 8's App initialization says: after a restore, Settings asks for the key again, and prices show the no-key line until then; within the spec (your A, 2026-11-16; closes open item 66). Not chosen: (B) kept across the restore, a change to spec section 8 (Design).
2. The phone check without your data: a second install for checks only: a build type `check` builds the same app under the id `app.ghghorg.atsassets.check`, named "ATS Check", with its own encrypted database and Keystore key, installed beside yours, so a restore there never touches your data; it also gives open item 33 its fresh app. Built only when asked (`assembleCheck`); CI and the screen tests unchanged (your A, 2026-11-16). Not chosen: (B) the emulator only; (C) your own app, backed up, reinstalled and restored.
3. On an app that holds data, in 34: the summary as in 33b with a line that restoring over data comes in a later version, and no Restore; any file can still be checked against the app (your A, 2026-11-16). Not chosen: (B) Restore from backup file inactive on an app with data until 35.
4. What "delete all settings and data" clears (section 8): every stored table and setting: records, instruments and prices, the lists and remembered texts, hidden holdings, overrides, auto-lock, tab order, the price key (decision 1), the backup moment and its mark; then the file's lists, settings and records are loaded with the instruments the check verified (33b decision 2). The restored app is exactly the file (your A, 2026-11-16). Not chosen: (B) the lists and remembered texts from price refreshes kept when the file has none.

## 12. Wording to approve (D9; proposed in step 4, approved on the phone)
- Frame F on a fresh install: the button "Restore", the step line ("Step 3 of 4"?), the line under it.
- Frame H: "Restore complete", the counts loaded, the "no price" line, the "never backed up" line, Go to Overall.
- The failure: "Not restored. Nothing was changed.", Close.
- On an app with data (decision 3): the line that restoring over data comes later.

## 13. Status and notes
- Draft 2026-11-16, on iter-34-restore-to-empty-app at 1f610cf (stacked on 33b, your A); approved the same day (your A).

### 13.1 Steps
- Steps 1 to 5 are built without stops between them, each held as a tree when its full run is green and committed one per step after step 5 (your instruction, 2026-11-16).
- Step 1 (2026-11-16): this file; plan.md (row 34's refs: section 9, AC-18's banner after a restore and AC-56's result named, AC-08 left to 33b; section 11's row; the change log); plan/open-items.md (item 66 closed by decision 1). Only documents changed.
- Step 2 (2026-11-16): K2's `RecordStore.replaceAll(records, instruments)`. data/RestoreRows.kt: `restoreRows` (the file's records as rows; the list values numbered, sectors first, with the texts and overrides naming their value's row; settings with the file's lock choice and tab order, no price key, no backup moment or mark; instruments with ticker and type only), `RestoreRows`, and `RestoreDao`, whose `replaceAll` clears every table and writes the rows in one Room transaction. RoomRecordStore works the rows out before anything is deleted. Test first: RestoreRowsTest (5 tests) did not compile ("Unresolved reference 'restoreRows'"). RestoreDaoTest on the emulator, 3 tests: a restore leaves exactly its rows; onto an empty store; a failed load rolls back, every table as it was. The fake stores gain the call. No schema change: database version 14 stays (no entity changed). The first full run was red on RestoreRowsTest's own expectation: I wrote an allocation's quantity as "4", but the store keeps it as a reduced fraction, "4/1", through the mapper every saved sell uses; the expectation was corrected (still one exact value). The second run green: 1,475 tests (1 skipped), lint's 6 (report 20:12:56), 162 `@Test` lines in androidTest (159 + 3), compiled; app/schemas unchanged.

## 14. Departures from the plan
- None yet.
