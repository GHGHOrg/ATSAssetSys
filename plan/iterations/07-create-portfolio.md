# plan/iterations/07-create-portfolio.md

Stage: Build. Status: built on branch iter-07-create-portfolio (steps 1 to 6 and one fix, commits 449ae9b to dd85e96) and checked on the phone 2026-10-30, all checks yes except check 10, which is not run (decision 3, open item 33) (section 13). Pull request not created; CI not run. Decisions 1 to 4 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
Source: plan/plan.md section 4 (row 07), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On the Portfolios tab I tap New portfolio, type a name and tap Create; the portfolio appears last in the list. A name that is too short, has no letter, or matches another portfolio is refused with the reason, and what I typed stays in the field.

## 2. Spec refs
- spec 4.7 (create only): a name has at least 3 Unicode characters (code points) after leading and trailing spaces are removed, and at least one character of the Unicode category Letter; a name that matches another portfolio's name, ignoring case and spaces as in 4.9 (D7), is refused. spec 3: portfolios are listed in the order they were created.
- spec 4.2 and criterion 19 (part: unit tested; phone check in 09, open item 33): before the opening balance exists, New portfolio shows "Set the opening balance first".
- Wireframe 10d: frames A and C, markers 1, 2 and 5. The checks run in the order length, letter, duplicate, and the first failure is shown. Leading and trailing spaces are removed on saving; the typed casing is kept.
- Wireframe 03: frame A, marker 1 (the row: name, "no holdings", $0.00 with no color) and marker 2; "No portfolios yet" with a Create a portfolio button.
- Criteria: 26 (create part; its amount part is met since 03), 55 (create part: "投資信託" accepted; "投資", "123" and "---" refused), 24 (part: with cash and no portfolio, Overall shows a Create a portfolio button), 9, 35, 39 to 42.
- spec 14: K5 (the flow), K2 (the store), K9.

## 3. Out of scope
- Rename (08) and delete (29). The portfolio screen (frame B) arrives with 08 (decision 1).
- Holdings counts and values (09, 17); the return to Buy after create (09); the price-failure line (16).
- The name rules for a restore file (34); the core check is written so it can be reused there.

## 4. Files
New, core/: Portfolios.kt (Portfolio(id, name, createdSeq); the name key; create(existing, cashEntries, id, typedName) giving the portfolio or a refusal: NoOpeningBalance, TooShort, NoLetter, Duplicate(otherName)); PortfoliosTest.kt.
Changed, core/: RecordStore.kt (loadPortfolios, savePortfolio).
Changed, app/: AtsDatabase.kt (PortfolioRow, PortfolioDao, version 2 with a migration from 1); RoomRecordStore.kt; schemas/app.ghghorg.atsassets.data.AtsDatabase/2.json (generated); AppViewModel.kt (the portfolios in state, the dialog's text and refusal, the actions); PortfoliosScreen.kt (the list, New portfolio, the dialog, the line before the opening balance); OverallScreen.kt (the Create a portfolio button, decision 2); strings.xml; app/build.gradle.kts (versionCode 7); AppViewModelTest.kt and the test store fakes.
Documents: plan.md (with the pull request #92 line), open-items.md (item 33), CLAUDE.md (the reminder line for item 33, section 8).

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-07-create-portfolio from main (1c4b85c). This file, the plan.md line, open item 33 and the CLAUDE.md line. Baseline run. javap on room3-runtime and room3-common: how a version 1 to 2 migration is declared (automatic or by hand); the result is shown to you.
2. core/: tests first (length after trimming, in code points, "投資" against "投資信託"; letters: "123", "---" and emoji refused, kana, Han and Hangul accepted; duplicates by case, outer spaces and repeated inner spaces; the refusal order; no opening balance; creation order; the trimmed name kept), then the code.
3. Data layer: PortfolioRow, the DAO, version 2 with the migration, the RoomRecordStore methods, the generated 2.json. Unit tests use the fake store; the migration is proved on the phone in step 6.
4. ViewModel: tests first (open the dialog, type, Create refused with the text kept, Create saved with the dialog closed and the portfolio last, Cancel; the message before the opening balance; the portfolios loaded at start; a line break typed or pasted becomes a space, decision 4), then the code.
5. Screens: the Portfolios list, New portfolio, the dialog, the Overall button, the strings.
6. versionCode 7. Full run, release build, install over 06's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft, with the reminder of open item 33.

## 6. Stubs
- A row's "no holdings" and $0.00 are fixed until 09.

## 7. Risks
- The first schema change: version 1 to 2 adds the portfolios table. If the upgrade fails on the phone, the app shows the unreadable screen; the file is not deleted (no destructive fallback). There is no backup until 31, and no device test runner for a Room migration test, so the install over 06's build with data (criterion 35) is the only real proof. The migration API is checked with javap, not written from memory.
- "Spaces" and "case" (my reading): trimming and collapsing use Unicode whitespace; case is ignored by a lowercase(uppercase(...)) fold in Locale.ROOT, so "ß" and "SS" match. Spec 4.9 does not say how far case folding goes.
- The dialog at the largest font must scroll (criterion 39).
- A portfolio name is never logged (section 9). Room and the SQLCipher Java client are already silent; the JNI layer is as before.

## 8. Proof
Commands: as in 06. CI green on the pull request.
By hand on the phone, with 06's data:
1. Install over 06's build (versionCode 6): the data is still there; Portfolios shows "No portfolios yet" and a Create a portfolio button (criterion 35 and the migration).
2. Overall shows the Create a portfolio button; it opens the dialog.
3. "Main" is created: listed with "no holdings" and $0.00; Overall still shows the net worth.
4. "投資" and "  ab  ": the length message; "123" and "---": the letter message; the typed text stays each time.
5. "投資信託" is accepted and listed second.
6. " main " and "MAIN" are refused, naming "Main".
7. Cancel saves nothing.
8. Close and reopen the app: both portfolios are there, in creation order.
9. Largest font, light and dark, TalkBack: the dialog scrolls, the refusal is read when it appears, the rows are read as text.
10. Before the opening balance: not run in 07 (decision 3, open item 33).
11. A pasted name with a line break shows on one line, with a space (decision 4).

## 9. Research
javap only (step 1). No web search planned.

## 10. Decisions (all Build, settled by you 2026-10-29 to 2026-10-30)
1. The rows are display-only in 07; the portfolio screen (frame B) and its menu arrive with 08. TalkBack reads a row as text, not a button.
2. Overall gets a Create a portfolio button in 07 (criterion 24, part). It opens the same dialog over Overall; after Create you stay on Overall.
3. "Set the opening balance first" is unit tested in 07. The phone check moves to 09 with the buy refusal, on one uninstall you approve; 34 at the latest. Open item 33; a CLAUDE.md line reminds you of it at the end of every iteration until it is closed.
4. A line break typed or pasted into a name becomes a space as you type. No new rule and no new message (design reading).

## 11. Wording to approve (D9; * = given by the wireframes)
- "New portfolio"*, "Name"*, "Create"*, "Cancel"*, "Create a portfolio"*.
- Helper line: "At least 3 characters, including a letter of any alphabet or script. Names are compared ignoring case and spaces."*
- "Name: at least 3 characters."*
- "Name: must include a letter of any alphabet or script."*
- "Not saved. Another portfolio is already named "<name>". Names are compared ignoring case and spaces."*
- "Set the opening balance first."* with the existing "Set opening balance" button.
- The row: "no holdings"* and "$0.00".
Approved on paper with this file; confirmed on the phone (D9): checks 2 to 7, 9 and 11.

## 12. Status and notes
Built 2026-10-30; phone checks 2026-10-30. Pull request and CI: not yet.

## 13. Build results
### 13.1 Commits
Newest first: dd85e96 fix, 717b21d step 6, b1a47b8 step 5, d7d6a22 step 4, 6d9f94e step 3, ea59bf7 step 2, 2d50683 merge of main (pull request #93, the test-count fix), 449ae9b step 1 with this file. main was 1c4b85c.

### 13.2 Local run
On dd85e96's code (2026-10-30, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 243 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, MarketDate 10, Money 15, OpeningBalance 9, Portfolios 16, AppLock 12, AppViewModel 104, DateDigits 7); lint: 3 findings, all listed, none silenced (AndroidGradlePluginVersion at libs.versions.toml:2, open item 31; NewerVersionAvailable at :17; TypographyDashes on date_not_a_date, strings.xml:86). Release build on 717b21d's code green (52 tasks, R8 ran; unsigned); the fix was not rebuilt as release. Debug APK versionCode 7 (aapt2).
Step 1, javap on room3-common 3.0.3 and room3-runtime 3.0.3: @Database has autoMigrations (AutoMigration from, to, spec); Migration(startVersion, endVersion) with a suspend migrate(SQLiteConnection) and Builder.addMigrations exist too. Room generated AtsDatabase_AutoMigration_1_2_Impl with one statement, CREATE TABLE IF NOT EXISTS `portfolios` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdSeq` INTEGER NOT NULL, PRIMARY KEY(`id`)), registered in AtsDatabase_Impl.createAutoMigrations. 2.json: version 2, identity hash fd3791ba...

### 13.3 Phone results (Android 17, debug build versionCode 7, your reports)
Checks 1 to 9 and 11 all yes; check 10 not run. Notes:
- 1: installed over the 06 build (versionCode 6, adb install -r at 01:24:22, firstInstallTime 2026-10-29 10:44:04 unchanged). The migration ran when the app opened: the 03 to 06 data was still there and Portfolios showed "No portfolios yet" with Create a portfolio (criterion 35).
- 2: failed on the first build: the dialog and the cover screen alternated and no input was possible; the app had to be force-stopped. Fixed (section 14, item 1); the fixed build was installed at 01:37:08 (firstInstallTime unchanged) and check 2 passed: the dialog opens steadily over Overall, takes typing, and a tap outside does nothing.
- 3 to 7: "Main" created and listed with "no holdings" and $0.00, the row does nothing when tapped; "投資", "  ab  ", "123" and "---" refused with their messages, the text kept and the message gone on typing; "投資信託" accepted and listed second; " main " and "MAIN" refused naming "Main"; Cancel and Back save nothing and the dialog reopens empty.
- 8: after a force stop both portfolios and the cash data were there, in creation order.
- 9: largest font, light and dark (typed name kept across the switch), TalkBack (a row reads as one item, not a button; the dialog is announced and swiping stays in the card; the refusal is read when it appears).
- 11: a pasted two-line text showed as "Line Break" on one line; cancelled, nothing saved.
- Logcat (not planned; Definition of done item 5): a first count found 0 for 投資信託 and 投資 and no whole-word Main in the app's lines, but the main buffer only reached back to 02:03, after the creates. Re-check: log cleared at 02:06:39; you force-stopped and reopened the app (both names loaded), typed 投資信託 and main, each refused naming the portfolio, then Cancel. App uid 10454, 81 lines: 0 for 投資信託, 0 for 投資, 0 for Main or main as a word except logcat's own "beginning of main" header; the whole log (5,792 lines) had no 投資信託 or 投資, and its other "main" lines came from system components. Only counts and tags were printed. The save path was not repeated in the re-check.
- No uninstall in 07; the phone holds the 03 to 06 data plus the portfolios Main and 投資信託.

### 13.4 Not run or not checked
CI and the pull request; check 10 (open item 33, due in 09); the release APK on a device (unsigned, open item 4), so R8's effect on the generated migration is not shown; a release build of the fix commit; a failed migration on the phone (only the successful one was seen); the save path in the Logcat re-check; contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project).

## 14. Departures from the plan
1. Defect found in phone check 2: Material3's AlertDialog opens a separate window, which takes the focus; MainActivity shows the cover screen whenever its window loses focus (the experiment after 02a phone check 7), the cover removed the dialog, the focus came back and the dialog opened again, in a loop. Fixed on your decision (A) by drawing the dialog inside the activity window over a dimmed layer that covers the tab and the bottom bar, takes every tap, and hides the screen under it from TalkBack (commit dd85e96). MainActivity is unchanged. No failing test was written first: the defect lives in Android's windows and focus and cannot be reproduced in a JVM test, and the project has no device UI tests; check 2 on the phone is the proof. A CLAUDE.md pitfall records the rule for later dialogs (08, 29).
2. RecordStore's two new methods and the test fakes moved from step 2 to step 3: RoomRecordStore implements RecordStore, so adding them in step 2 would have needed a production stub for one commit. Step 2 stayed pure core/.
3. An unlock keeps an open New portfolio dialog and its tab, as for the form and the review (02b decision 9). Not written in this file before the build; a ViewModel test covers it.
4. A tap outside the dialog does nothing; Back closes it like Cancel (my design choice, so typed text is not lost by accident).
5. Overall shows "No portfolios yet" only while there is no portfolio; before 07 the line was always shown.
6. 2.json is kept as Room writes it (LF in the working copy); git stores it with LF, like 1.json.
7. The step 1 baseline found the test totals of 03 to 06 one too high; they were corrected in a separate pull request (#93, your decision C) and merged into this branch before step 2.
8. In step 1 the Room aar was unpacked into %TEMP%\claude\room3aar, not my scratchpad; the folder was deleted with your approval.
9. The Logcat check (section 13.3) was not planned in section 8; it was added for Definition of done item 5, with your approval.
