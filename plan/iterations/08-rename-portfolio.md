# plan/iterations/08-rename-portfolio.md

Stage: Build. Status: built on branch iter-08-rename-portfolio (steps 1 to 6 and one fix, commits 398177f to 74aa71b) and checked on the phone 2026-10-30, all checks yes (section 13). Pull request not created; CI not run. Decisions 1 to 3 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
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
Approved on paper with this file; confirmed on the phone (D9): checks 2 to 6 and 9.

## 12. Status and notes
Built 2026-10-30; phone checks 2026-10-30. Pull request and CI: not yet.

## 13. Build results
### 13.1 Commits
Newest first: 74aa71b fix, 31fbde5 step 6, d1fa7db step 5, 9b3521e step 4, b437ae8 step 3, 37afe4e step 2, 398177f step 1 with this file. main was 53a97b8.

### 13.2 Local run
On 74aa71b's code (2026-10-30, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 266 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, MarketDate 10, Money 15, OpeningBalance 9, Portfolios 23, AppLock 12, AppViewModel 120, DateDigits 7); lint: 3 findings, all listed, none silenced (AndroidGradlePluginVersion at libs.versions.toml:2, open item 31; NewerVersionAvailable at :17, whose message now names kotlinx-coroutines-test 1.11.0 as available, seen 2026-10-30; TypographyDashes on date_not_a_date, strings.xml:86). Release build on 31fbde5's code green (52 tasks, R8 ran; unsigned); the fix was not rebuilt as release. Debug APK versionCode 8 (aapt2).
Room generated the rename as "UPDATE portfolios SET name = ? WHERE id = ?" (bound parameters), run as a write in a transaction, returning getTotalChangedRows; RoomRecordStore accepts only 1. No schema change: 2.json unchanged, database version 2.

### 13.3 Phone results (Android 17, debug build versionCode 8, your reports)
Checks 1 to 10 all yes; check 9a.2 after the fix. Notes:
- 1: installed over the 07 build (versionCode 7, adb install -r at 10:40:52, firstInstallTime 2026-10-29 10:44:04 unchanged); the cash data and Main and 投資信託 were there (criterion 35).
- 2 to 8: the screen with "<", the name, ⋮, the $0.00 card and "No holdings yet"; the in-window menu opens steadily and closes on a tap outside and on Back; Back and "<" return to the list; Rename opens with the name and the wording of section 11; the unchanged name closes without a change; "MAIN" saved (criterion 55); 投資信託, 投資, "  ab  ", 123 and --- refused with the text kept; "Main Account" saved and still first; kept after a force stop.
- 9a.2: failed on the first build: at the largest font the keyboard covered half of the Rename dialog and it could not be scrolled. Fixed (section 14, item 1); the fixed build was installed at 11:54:39 (firstInstallTime unchanged) and the recheck passed for Rename and New portfolio, and a tap on the dim still does nothing. 9a.1, 9b and 9c passed: the screen readable at the largest font with ⋮ reachable with the long name; light and dark with the typed name kept; TalkBack reads a row as one button, "<" as Back, ⋮ as Portfolio menu, and stays in the menu and in the dialog, and reads the refusal.
- 10: log cleared at 12:06:48 (after two earlier clears at your request), read at 12:08:22. You force-stopped and reopened the app, tried 投資信託 (refused) and renamed Main Account to Main. App uid 10454, 87 lines from 12:07:10 to 12:08:18: 0 for Main Account, 投資信託, 投資 and the word main; the whole log (7,038 lines) had no Main Account, 投資信託 or 投資, and its 8 lines with the word main came from system components. The app's lines are all Android's own tags (ImeTracker, InsetsController, WindowOnBackDispatcher and others), and the source has no logging call. The main buffer reached back only to 12:07:49, so whether the moment of the Save is inside it is not shown; the system and events buffers cover the whole run. Only counts and tags were printed.
- No uninstall in 08; the phone holds the 03 to 06 data and the portfolios Main and 投資信託.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (due in 09); the release APK on a device (unsigned, open item 4); a release build of the fix commit; a rename that changes no row or more than one (the one-row check is code, not shown); contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project).

## 14. Departures from the plan
1. Defect found in phone check 9a.2: the app draws edge to edge, so the window does not shrink for the keyboard; the cash form and the reversal screen use imePadding, but the in-window dialog card of 07 did not, so at the largest font the keyboard covered half of it and it could not be scrolled. It affected both dialogs (New portfolio from 07 and Rename). Fixed on your decision (A) with imePadding on the dialog layer: the dim still covers the screen and takes every tap, and the card is centred and scrolls in the space above the keyboard (commit 74aa71b). No failing test was written first: the defect lives in Android's keyboard and window insets and cannot be reproduced in a JVM test, and the project has no device UI tests; the recheck on the phone is the proof. The CLAUDE.md dialog pitfall now names imePadding. 07's check 9a passed, probably because the shorter helper text and name fit above the keyboard; not proven.
2. 07's NewPortfolioDialog became PortfolioNameDialog with its title, helper line and confirm label as parameters, shared by New portfolio and Rename (step 5). The CLAUDE.md pitfall now names it.
3. Rename has its own result type (PortfolioRenameResult: Renamed, Unchanged, Refused, NotFound), so 07's PortfolioRefusal and the screen code that matches on it did not change.
4. The ViewModel's line-break rule is one shared helper for create and rename.
5. "<" and ⋮ are text buttons with TalkBack labels, matching the existing Back buttons, with no icon library added.
6. A tab tapped on the portfolio screen leaves it and closes the menu and the dialog; an unlock keeps the screen and an open dialog (as for the form, 02b decision 9). Not written in this file before the build; ViewModel tests cover both.
7. Between steps 4 and 5 the PORTFOLIO screen showed the Portfolios list; no build was installed in between.
8. The log was cleared three times for check 10, at your request, before the run that was counted.
