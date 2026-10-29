# plan/iterations/07-create-portfolio.md

Stage: Build. Status: approved 2026-10-30 with decisions 1 to 4 (section 10); not built.
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
Approved on paper with this file; confirmed on the phone (D9).

## 12. Status and notes
Approved 2026-10-30. Not built.
