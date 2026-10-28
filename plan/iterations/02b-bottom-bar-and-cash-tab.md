# plan/iterations/02b-bottom-bar-and-cash-tab.md

Stage: Build. Status: approved 2026-10-28; decisions 1 to 7 settled the same day (section 10). Not started.
Source: plan/plan.md section 4 (row 02b), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
After unlocking, I see a bottom bar with four tabs (More, Cash, Portfolios, Overall) and the app opens on Overall. The Cash tab shows my cash balance and my cash entries, each with the balance after it.

## 2. Spec refs
- spec 6, navigation outline: the four tabs in the default order More, Cash, Portfolios, Overall; the app opens on Overall after unlock; the bar is always visible; Android's back button.
- spec 6, view 7 (Cash account), only the parts this iteration builds (section 3 lists the rest). Wireframe 07-cash.html: frame A, markers 1 and 4.
- spec 4.8, first bullet: the Cash tab shows the balance on the latest entry date, which includes entries dated after today. Overall keeps net worth as of today.
- spec 4.1: list order (newest first; entries on the same date in entry order).
- spec 6, first launch: before the opening balance exists, the Cash entry button opens it and the list shows one plain line (wireframe 07, marker 1).
- spec 6: number format (sign, no color for cash), the "ET" date suffix, accessibility, theme, and the empty state.
- Criteria: 9, 13 (part: the four tabs can be reached), 35, 39 to 42, 49 (part, section 3), 60(c) (part: a future-dated opening balance only; the deposit arrives in 03).
- spec 14: K3 (read only: the balance and the rows), K9.

## 3. Out of scope
- In view 7: the date, ticker and type filters, Clear filters and the "N entries shown" line (06, 28); trade lines (09); tapping a row to open its detail (05, 09); Cash entry types after the opening balance (03, 04).
- The Buy/Sell button (09). Tab reordering (22). Settings and the gear icon (20).
- The Portfolios tab beyond its empty state; the Create a portfolio button (07).
- More's four entries: Net Worth (Total) (17), Allocation (19), Realized gains (26), Backup and restore (31).
- The "Restore is in More only" line on Overall (wireframe 01, marker 8). The 02a file put it in 02b; it moves to 34, because no restore exists before then (decision 6).
- Icons in the bottom bar: a future idea (open item 30), not in this version.
- Criterion 37: deferred to a later iteration, chosen when its file is written (decision 5).

## 4. Files
New, core/ (app.ghghorg.atsassets.core):
- CashAccount.kt (K3, read only): the balance on the latest entry date, and the rows newest first with the balance after each. It uses the 4.1 order (date, then entered_seq).
- Test: CashAccountTest.kt.
New, app/ (ui/):
- BottomBar.kt: the four tabs, built from an ordered list so iteration 22 can reorder them.
- CashScreen.kt: the balance, the Cash entry button, the rows and the empty line.
- PortfoliosScreen.kt and MoreScreen.kt: the stubs in section 6.
Changed: ui/AppViewModel.kt (the selected tab, the tab the form was opened from, the reset to Overall on unlock), ui/AppContent.kt (the bar and the tab content), MainActivity.kt (only if the reset on unlock needs it), strings.xml, app/build.gradle.kts (versionCode raised by one for criterion 35), AppViewModelTest.kt, plan/plan.md, plan/open-items.md, and CLAUDE.md if a command or pitfall is added.
No new dependency (decision 7).

## 5. Order of work
Each step ends with build, tests and lint and the CLAUDE.md section 8 report, then I stop until you say go.
1. Branch iter-02b-bottom-bar-and-cash-tab from main (a35c67d). Baseline: spotlessCheck, build, tests, and lint with --rerun-tasks.
2. core/: CashAccountTest first and see it fail, then CashAccount.
3. ViewModel: tests first (opens on Overall; tab switching; the form returns to the tab it came from; unlock resets to Overall; typed text kept across a tab switch), then the code.
4. The bottom bar and the More and Portfolios stubs, with the approved wording.
5. The Cash screen.
6. Raise versionCode. Full run: spotlessCheck, build, tests, lint, release build. Install over the 02a build that holds data.
7. Phone checks (section 8); the results are written here.
8. Documents (this file, plan.md, open-items.md, CLAUDE.md if needed). Draft the pull request.

## 6. Stubs
- Portfolios tab: "No portfolios yet" only. The Create a portfolio button arrives in 07.
- More tab: the last-backup line only, "Last backup: never backed up" (my reading: true while no backup exists). The four entries and the gear arrive later (section 3).
- Cash entry button: shown only while the opening balance is missing, and it opens the opening balance form. After that it is hidden until 03 brings the deposit.
- The rows cannot be tapped until 05 and 09.

## 7. Risks
- Largest font: four labels in one bar ("Portfolios" is the longest) may wrap or be cut off (criterion 39). Checked on the phone.
- The form's scrolling over the keyboard (commit L, imePadding()) may change with a bar under it.
- MainActivity already has a Scaffold; a second one for the bar may double the insets. Checked on the branch.
- Text-only bar items (decision 7) may sit oddly in Material 3's layout. Checked on the branch; if it fails I stop and show you.
- Iteration 01's cover and lock behaviour sits around this content, so its checks are run again.
- The latest-balance sum has no overflow guard. It cannot overflow with one opening balance; 03 adds the guard (open item 9).
- One ViewModel holds every tab. My reading: acceptable until a later iteration needs more.

## 8. Proof
Commands: the same as the 02a file, section 8 (build; tests with the test count; lint with --rerun-tasks; spotlessCheck; :app:assembleRelease). CI green on the pull request (your report).
By hand on the phone, each result written as "checked on the phone" or "not checked":
1. After unlock the app opens on Overall. The bar order is More, Cash, Portfolios, Overall, and the selected tab is visible without relying on color.
2. Each tab opens its screen. Overall is unchanged from 02a.
3. With no data, Cash shows $0.00, the Cash entry button and one plain line. The button opens the opening balance form, and Back returns to Cash.
4. Save from Cash. Cash then shows the balance and one row: "Opening balance", the date with "ET", "+$X", "Balance $X". No color, no Cash entry button.
5. A future-dated opening balance: Overall shows $0.00 and Cash shows the amount (part of 60(c)).
6. Back on each tab (decision 3).
7. Leave the app while on Cash, then unlock: the app opens on Overall.
8. Half-fill the form, switch tabs, return: the typed text is kept. Switch to dark mode while on Cash.
9. Largest font: the bar labels and the Cash screen, nothing cut off.
10. Light and dark on all four tabs.
11. Install the newer build (higher versionCode) over the 02a build that holds data: the data is still there (criterion 35).
12. Logcat while using Cash: no amount.
13. Iteration 01 checks 1 to 14.
14. TalkBack: each tab reads its name and whether it is selected; the Cash balance and each row read in full (section 11 wording); the form and review as in 02a. Check 8 (no screen lock) on this build.

## 9. Research
None planned. No new library, so no version lookup. If a later change brings in a library, its version is looked up on the day and shown to you before use.

## 10. Decisions (all Build, settled by you 2026-10-28)
1. The bar shows on the form and review screens; the typed text is kept across a tab switch.
2. Saving returns to the tab the form was opened from.
3. Back on More, Cash or Portfolios goes to Overall; Back on Overall leaves the app.
4. The Cash list is built now: rows newest first, the balance after each, one plain line when there are none.
5. TalkBack and check 8 in 02b; criterion 37 in a later iteration, chosen when its file is written.
6. The "Restore is in More only" line moves to iteration 34.
7. A text-only bar with no new library; icons are a future idea (open item 30).

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Tabs: More*, Cash*, Portfolios*, Overall*.
- Cash: "Cash balance"*, "Cash entry"*, "Opening balance"*, "Balance $X"*. Empty line: "No cash entries yet."
- Portfolios: "No portfolios yet"*.
- More: "Last backup: never backed up"*.
- TalkBack, Cash row: "Opening balance, 2026-01-01 Eastern Time, plus 10,000 dollars, balance 10,000 dollars". Balance: "Cash balance, 10,000 dollars".
Approved on paper with this file; confirmed on the phone (D9).

## 12. Status and notes
Approved 2026-10-28. Not started.
