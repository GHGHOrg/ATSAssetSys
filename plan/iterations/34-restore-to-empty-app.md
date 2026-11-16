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
- Step 3 (2026-11-16): the ViewModel's `restoreNow` (on the summary of a passed file, on an app with no data: `replaceAll` with the records and instruments 33b kept, then the state read again from the store through `readAll`, taken out of `start` so both read the same way; the result on the wizard; the first refresh), `finishRestore` (Go to Overall), `canRestore`; the wizard's steps RESTORING, RESULT and FAILED. A failed write leaves the store and the state as they were and shows FAILED; unreadable data shows the unreadable screen. Back and a tab wait while the write runs; Back does nothing on the result and closes a failure; a lock no longer resets the wizard once Restore was tapped, so the result shows after the unlock. RestoreLoadFlowTest, 9 tests: the restore on an empty app (the key gone and no longer used by the source, the last backup reset, a backup due), Go to Overall with the reminder, AC-04's round trip (portfolios, entries, transactions, holdings, the cash balance; no prices), AC-25's example file with its expected figures (two portfolios, $9,425.00, 8 transactions, 7 trade lines, AAPL 16 shares, OLDCO hidden, the overrides, the lists, the lock choice and tab order; AAPL stored as verified, OLDCO not), a failed write, an app that holds data, no passed file, a lock during the write, unreadable data. The tests were written before the code but not run red first; they passed at the first run after the code. The first full run was red on spotlessCheck only (a comment between a `when`'s branches); the second green: 1,484 tests (1 skipped), lint's 6 (report 20:20:51).
- Step 4 (2026-11-16): the model's `totalSteps` ("4" on an app with no data, wireframe 10e marker 15; "5" on one that holds data), `canRestore`, `restoreLater` (decision 3) and `loaded` (the result's counts); Back off while the restore is written and on its result. The screen: the step lines take the number of steps (33b's three strings now carry it; the shown text on an app with data is unchanged); the summary shows Restore and what it does on an app with no data, or 33b's line on an app with data; the writing line; frame H's result (Restore complete, the three counts, the "no price" and "never backed up" notes, a line that the price key must be entered again, decision 1, Go to Overall) and its failure (Not restored. Nothing was changed., Close). 11 strings proposed, not approved (section 12; strings.xml says so). RestoreModelTest, 2 tests; one wiring test in RestoreLoadFlowTest; 33b's screen tests name the step lines with their number. The full run green at the first try: 1,487 tests (1 skipped), lint's 6 (report 20:26:13).
- Step 5 (2026-11-16): the screen tests (AC-61), 7 new in RestoreScreenTest.kt: RestoreLoadTalkBackTest (5: on an app with no data the summary counts four steps and offers Restore with what it does; on an app that holds data no Restore; while the restore is written nothing can be tapped, Back included; the result as a heading with each count read with its label, its three notes and Go to Overall; a failure with Close) and two more in RestoreLargestFontLandscapeTest (the summary with Restore, and the result, every part reached and the button tapped). With step 2's three DAO tests, 169 `@Test` lines in androidTest (159 + 10). Compiled only: they run on GitHub, on the pull request. A whole restore on MainActivity is not tested there: it needs Android's file screen, which the tests do not drive; the phone check covers it (decision 2).

- Steps 1 to 5 committed on your yes, each from its held tree, each staged count matching its report (3, 9, 5, 9, 2): 1e8509c, 612cee6, 9a21af5, 2b66977, 333f984, all signed.
- Step 6 (2026-11-16): versionCode 44. The build type `check` (decision 2): `initWith(debug)`, `applicationIdSuffix = ".check"`, `versionNameSuffix = "-check"`, and app/src/check/res/values/strings.xml naming it "ATS Check". `assembleCheck` built app-check.apk (44,254,632 bytes): `package: name='app.ghghorg.atsassets.check' versionCode='44' versionName='1.0-check'`, `application-label:'ATS Check'` (aapt2 dump badging). Its own data folder and Keystore key come with its own app id (my reading of Android's per-app storage; seen on the phone in check 3). CI, lint and the screen tests still use debug only.

### 13.2 Phone check (versionCode 44; your app over 43, your data kept; "ATS Check" installed beside it, decision 2)
Before: your app (debug, 44) installed over 43; ATS Check installed new; copied to the phone's Download/ATS-restore-check: spec/example-backup.csv and two forms of it made from it, with a byte-order mark and CRLF (as Excel's CSV UTF-8 writes) and with CRLF only (as Notepad re-saves) (AC-25; made by script, not by Excel itself). Every restore is made in ATS Check, never in your app.
1. AC-35: your app opens with your data as before; no unreadable-data screen. No database change in 34.
2. Your app: More, Backup and restore, Restore from backup file, Choose file, your own backup file, Check file: the summary says "Step 3 of 5", has the line that restoring comes in a later version, and no Restore button (decision 3). Cancel.
3. ATS Check (a second icon): it asks for your phone's lock first, then shows a fresh app: no data, the guided steps. Its name is ATS Check on the icon and in recent apps.
4. Open item 33 in ATS Check: the Portfolios tab, New portfolio before any opening balance: "Set the opening balance first" (09 decision 9), as the spec words it.
5. ATS Check: Settings, the price source key: enter your key (the ticker check needs it).
6. ATS Check: More, Backup and restore: "Last backup: never backed up"; Restore from backup file, step 1: "Step 1 of 4. Nothing is changed until step 4."
7. Choose file, example-backup.csv, Check file: the summary, "Step 3 of 4": portfolios 2, transactions 8, cash entries 4, cash balance after the restore $9,425.00, holdings with shares 1 (AC-56); the line that the app holds no data and Restore loads the file; Restore and Cancel.
8. Restore: "Step 4 of 4. Restoring…" then "Restore complete": loaded 2, 8, 4; the "no price", "never backed up" and key lines; Back does nothing. Go to Overall: Overall with the backup reminder banner (AC-18).
9. The restored app against spec/example-backup-expected.md (AC-25): Portfolios Main and Retirement; Main's AAPL 16 shares, cost $1,200.00, "no price"; OLDCO under Hidden holdings; Realized gains total $600.00 (Mar 1 +$800.00, Mar 10 -$200.00); net worth $10,625.00 before a refresh; AAPL's sector Technology and geography United States; Settings: auto-lock 5 minutes, tab order Overall, Portfolios, Cash, More, no price key (decision 1); More: never backed up.
10. Settings: enter the key again; pull to refresh: AAPL gets a price.
11. AC-04: in ATS Check, Back up now, save the file. Then ATS Check is removed and installed again (I do this with adb: its data only, never your app's), its lock and key set again, and the saved file restored: every figure of check 9, the price aside, as before the backup.
12. AC-25's forms: on the fresh ATS Check of check 11 before its restore, or after another reinstall: each form of example-backup.csv checked: the same summary as check 7 (the restore itself of each form is the same records, 33a's tests).
13. AC-39 to AC-42 on the summary with Restore and on the result: the largest font in landscape, TalkBack (the counts read with their labels, Restore and Go to Overall as buttons), light and dark.
14. Logcat over one restore (check 8 or 11): no file content, id, ticker, amount, name or key.
15. At the end: ATS Check may stay for 35, or be removed (your choice).

Results:
- Not yet run.

## 14. Departures from the plan
1. Steps 2 and 3: step 2's tests ran red first; step 3's were written before the code but first run after it.
2. Files beyond section 5's: data/RestoreRows.kt (the restore's rows and its DAO, so AtsDatabase.kt and RoomRecordStore.kt only wire it); app/src/check/res/values/strings.xml (decision 2's name).
3. The step lines of 33b, approved with "of 5", now take the number of steps: unchanged on an app that holds data, "of 4" on one with none, as wireframe 10e marker 15 says.
4. The result shows one line the wireframe does not: the price source key must be entered again (decision 1).
5. A lock after Restore was tapped no longer sends the wizard to step 1: the write is one action that ends by itself, and its result shows after the unlock (section 9's "a lock during a restore returns to step 1" read as before the confirmation; your review).
