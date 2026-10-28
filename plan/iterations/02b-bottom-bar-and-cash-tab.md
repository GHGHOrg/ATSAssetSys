# plan/iterations/02b-bottom-bar-and-cash-tab.md

Stage: Build. Status: built on branch iter-02b-bottom-bar-and-cash-tab (steps 1 to 6, commits 7e35b57 to 91fb492) and checked on the phone 2026-10-28 to 2026-10-29, all checks yes (section 13). Pull request not created; CI not run. Decisions 1 to 9 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
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
8. On a shared date the later entry comes first: the list is the exact reverse of the 4.1 order (settled 2026-10-28, step 2).
9. Unlock keeps the form or review open; it resets to Overall only from a tab (settled 2026-10-28, step 3). This refines "unlock resets to Overall" in sections 5 and 8, so a theme change keeps the open form (criterion 40).

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Tabs: More*, Cash*, Portfolios*, Overall*.
- Cash: "Cash balance"*, "Cash entry"*, "Opening balance"*, "Balance $X"*. Empty line: "No cash entries yet."
- Portfolios: "No portfolios yet"*.
- More: "Last backup: never backed up"*.
- TalkBack, Cash row: "Opening balance, 2026-01-01 Eastern Time, plus 10,000 dollars, balance 10,000 dollars". Balance: "Cash balance, 10,000 dollars".
Approved on paper with this file; confirmed on the phone (D9).
Added and approved 2026-10-28 (step 5): type labels Deposit*, Withdrawal*, Interest/dividend*, Adjustment* and Trade (mine; in 02b only the opening balance can appear, and trade lines get their own label in 09). TalkBack reads cents ("10,000.00 dollars", as 02a's net worth does) and "plus" or "minus" before a signed amount.

## 12. Status and notes
Built 2026-10-28; phone checks 2026-10-28 to 2026-10-29. Pull request and CI: not yet.

## 13. Build results
### 13.1 Commits
Newest first: 91fb492 step 6, ab6fa3b step 5, f247c25 step 4, 52bda4b step 3, 1bee316 step 2, 7e35b57 iteration file. main was a35c67d.

### 13.2 Local run
On 91fb492's code (2026-10-28, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 98 tests, 0 failures (Balance 8, CashAccount 8, MarketDate 10, Money 15, OpeningBalance 9, AppLock 12, AppViewModel 36); lint: 2 findings, both known, neither silenced (NewerVersionAvailable at libs.versions.toml:17; TypographyDashes at strings.xml:48, the same string as :31 before, moved by the new strings); release build green (52 tasks, R8 ran; unsigned). Debug APK versionCode 2 (aapt2).

### 13.3 Phone results (Android 17, debug build versionCode 2, your reports)
Checks 1 to 14 all yes. Notes:
- 11: installed over the 02a build (versionCode 1, adb install -r, firstInstallTime unchanged); the data was still there. Criterion 35 is now checked with a newer build.
- 12: log cleared at 20:37:36, read after use; only counts were shown. The app's process (53 lines) had 0 matches for 500, 500.00, $500, 50000, "Opening balance", "Cash balance" and "Balance $". The whole log had 0 for the formatted patterns and the labels; plain 500 and 50000 matched 106 and 96 lines from other processes (content not read). The amount was only 3 digits, so this is a weak test.
- 14b (check 8): with no screen lock the app shows the message and cannot be unlocked; check 13 afterwards passed.
- 3, 4 (second half), 5 and 8 were run on fresh installs (two adb uninstalls, 2026-10-29), so the data was deleted on purpose. The phone now holds only the future-dated test balance from check 5.

### 13.4 Not run or not checked
CI and the pull request; the release APK on a device (unsigned); contrast measurement (by eye only); criterion 37 (deferred, decision 5); Compose UI tests (the project has none; the layout was checked by hand).

## 14. Departures from the plan
1. The bar is a row of selectable text items (Role.Tab, minimum height 56 dp, in a selectableGroup), not Material 3's NavigationBarItem, whose fixed icon slot could cut text off at the largest font. The selected tab is bold with a line under it.
2. Screen.OVERALL is renamed Screen.TABS; the 7 test references were renamed and no assertion was removed.
3. The form's origin tab is the selected tab itself (unchanged while the form is open), not a separate field.
4. Between steps 4 and 5 the Cash tab showed only its title; step 5 replaced it.
5. The rows wrap as two columns (FlowRow), so the right column moves under the left at large fonts.
6. Write created new files with LF; spotlessCheck rejected them; they were converted to CRLF (CLAUDE.md pitfall).
