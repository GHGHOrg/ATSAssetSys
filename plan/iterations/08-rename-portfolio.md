# plan/iterations/08-rename-portfolio.md

Stage: Build. Status: approved 2026-10-30 with decisions 1 to 3 (section 10); not built.
Source: plan/plan.md section 4 (row 08), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
I tap a portfolio on the Portfolios tab, its screen opens, I choose Rename, change the name and tap Save; the new name shows on the screen and in the list. A name that is too short, has no letter, or matches another portfolio is refused with the reason, and what I typed stays.

## 2. Spec refs
- spec 4.7 (rename): the length, letter and duplicate rules of create (Portfolios.nameRefusal from 07), compared with the other portfolios only.
- Wireframe 10d: frame B and marker 4 (the field starts with the current name; Save and Cancel; "Only the name changes. Everything else stays as it is."; a name that differs from its own only in case or spaces is accepted; saving the unchanged name closes the dialog and changes nothing); frame C and marker 5 (the refusals and their order, as in 07).
- Wireframe 03: frame B, opened from a row (frame A, marker 1): "<" back, the name, the menu (marker 3, decision 1), the summary (marker 4, decision 2), the holdings heading (marker 5, decision 2).
- Criteria: 55 (rename part: the refusals; "on rename, a name that differs from the portfolio's own only in case is accepted"), 9, 35, 39 to 42.
- spec 14: K5 (the flow), K2 (the store), K9.

## 3. Out of scope
- Delete (29). The screen's real values, holdings, flags and price line (09, 16, 17).
- The links to Allocation (18), Transaction history (27) and Hidden holdings (15). Buy/Sell (09).

## 4. Files
Changed, core/: Portfolios.kt (rename(existing, id, typedName) giving Renamed(portfolio), Unchanged or Refused(reason)); RecordStore.kt (renamePortfolio(id, name)); PortfoliosTest.kt.
Changed, app/: AtsDatabase.kt (an UPDATE portfolios SET name = :name WHERE id = :id in PortfolioDao; no schema change, the database stays at version 2); RoomRecordStore.kt; AppViewModel.kt (a PORTFOLIO screen with the portfolio's id, the menu and the rename dialog state, the actions); PortfoliosScreen.kt (rows tappable, the dialog card shared with rename); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 8); AppViewModelTest.kt and the fake.
New, app/: PortfolioScreen.kt (the portfolio screen and its in-window menu).
Documents: plan.md (with the pull request #95 line), open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-08-rename-portfolio from main (53a97b8). This file and the plan.md line. Baseline run.
2. core/: tests first (the rules against the other portfolios only; a change of case or spaces only is accepted and the new casing saved; the unchanged name gives Unchanged; the trimmed name saved; an unknown id; criterion 55's names), then the code.
3. Data layer: renamePortfolio in the DAO, RoomRecordStore and the fake.
4. ViewModel: tests first (open and close the portfolio screen; the menu; Rename opens prefilled; a refusal keeps the text; Save renames on the screen and in the list; the unchanged name closes with no write; Cancel and Back; a failed save; an unlock keeps the dialog; Android Back from the screen goes to the list), then the code.
5. Screens and strings.
6. versionCode 8. Full run, release build, install over 07's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft, with the reminder of open item 33.

## 6. Stubs
- The summary shows $0.00 for value, cost and unrealized gain/loss, and the holdings line "No holdings yet", until 09 and 17 (decision 2).

## 7. Risks
- The menu and the dialog must stay inside the activity window (CLAUDE.md pitfall from 07): no DropdownMenu, Popup or AlertDialog. The menu is a new in-window component.
- The first update of a stored record: rename is the only change 4.7 allows to an existing portfolio. The UPDATE touches the name of one row by id and nothing else.
- The old and the new name are never logged (section 9); a Logcat check is planned (proof 10).

## 8. Proof
Commands: as in 07. CI green on the pull request.
By hand on the phone, with 07's data (Main, 投資信託):
1. Install over 07's build (versionCode 7): the data and both portfolios are still there (criterion 35).
2. Tap Main: its screen opens with "< Main", ⋮, the $0.00 card, "Holdings" and "No holdings yet". ⋮ opens the menu with Rename; a tap outside or Back closes it. Android Back and "<" return to the list.
3. Rename opens with "Main" in the field and the line "Only the name changes. Everything else stays as it is."
4. "Main" unchanged, Save: the dialog closes and nothing changes.
5. "MAIN", Save: the screen and the list show "MAIN" (a change of case only, criterion 55).
6. "投資信託", "  ab  ", "123" and "---": refused with their messages (the duplicate names 投資信託), the text kept.
7. "Main Account", Save: shown on the screen and listed first (the creation order is unchanged).
8. Close and reopen the app: the new name is kept.
9. Largest font, light and dark, TalkBack: a row reads as one button; ⋮ reads "Portfolio menu"; the menu and the dialog are read and swiping stays in them.
10. Logcat (log cleared first): no old or new name in the app's lines (counts only).

## 9. Research
None planned; no new library.

## 10. Decisions (all Build, settled by you 2026-10-30)
1. The portfolio screen has a ⋮ button; it opens a menu card drawn inside the activity window (no pop-up window) with Rename only. Delete joins it in 29. A tap outside or Back closes it.
2. The screen shows a summary card with "Portfolio value", "Cost" and "Unrealized gain/loss", all $0.00 and without color, then the heading "Holdings" and the line "No holdings yet". No price line and no links until their iterations (15, 16, 18, 27).
3. A portfolio row is one tap target, without ">"; TalkBack reads it as one button. This replaces 07 decision 1.

## 11. Wording to approve (D9; * = given by the wireframes)
- "Rename portfolio"*, "Name"*, "Save"*, "Cancel"* (existing), "Rename"*.
- "Only the name changes. Everything else stays as it is."*
- "Portfolio value"*, "Cost"*, "Unrealized gain/loss"*, "Holdings"*.
- New: "No holdings yet" (decision 2). For TalkBack only: ⋮ reads "Portfolio menu"; "<" reads "Back" (existing).
- The refusals: the 07 strings, reused.
Approved on paper with this file; confirmed on the phone (D9).

## 12. Status and notes
Approved 2026-10-30. Not built.
