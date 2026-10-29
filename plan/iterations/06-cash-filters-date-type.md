# plan/iterations/06-cash-filters-date-type.md

Stage: Build. Status: approved 2026-10-29; decisions 1 to 4 settled the same day (section 10). Not started.
Source: plan/plan.md section 4 (row 06), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On the Cash tab I narrow the list by a From and To date and by one type chip (All, Deposit, Withdrawal, Interest/dividend, Adjustment). A line says how many entries are shown, Clear filters resets both, and each row still shows the real balance after it.

## 2. Spec refs
- spec 6, view 7: the filters are optional, combine (an entry must match all of them) and are not saved; From and To are inclusive with the date note; the type chips are one at a time; Clear filters; one line saying how many entries are shown; the real balance on every row; one plain line when nothing matches.
- Wireframe 07: markers 2, 3, 5, 6 and 7; frames B and C. The opening balance shows only under All (marker 3).
- spec 6: the date note (C13), the error and empty states, accessibility, theme.
- Criteria: 29 (the date range filter with the date note), 49 (part: the chips without Buy and Sell, Clear filters, the count line, the real balance, the no-match line), 9, 35, 39 to 42.
- spec 14: K3 (read only), K9.

## 3. Out of scope
- The ticker picker, the Buy and Sell chips, the disabled-chip rule and reversal trade lines (28).
- Transaction history filters (27). Criterion 37: still deferred.

## 4. Files
Changed, core/: CashAccount.kt (K3: a new CashFilter(from, to, type) and rows(entries, filter); it filters after the balances are worked out, so every row keeps its real balance); CashAccountTest.kt.
Changed, app/: AppViewModel.kt (the filter's typed From and To text and the chosen chip, in memory only, decision 4); CashScreen.kt (the filter card, the chips, Clear filters, the count line, the no-match line); strings.xml (with a plural for the count line); app/build.gradle.kts (versionCode 6); AppViewModelTest.kt.
Documents: plan.md, open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-06-cash-filters-date-type from main (da9f26e). Baseline run.
2. core/: tests first (no filter gives all rows; From only, To only and both, both ends inclusive; each type; the opening balance only under All; type and dates combined; From after To gives nothing; the balances stay the real ones), then the code.
3. ViewModel: tests first (typing the dates, a bad date per decision 1, choosing a chip; Clear filters; the filters kept across a tab switch and the detail, decision 4; the count), then the code.
4. Screens: the filter card under the balance with From, To, the date note, the chips, Clear filters, the count line and the no-match line.
5. versionCode 6. Full run, release build, install over 05's build with data.
6. Phone checks (section 8).
7. Documents and the pull request draft.

## 6. Stubs
- The ticker field and the Buy and Sell chips are not shown until 28.

## 7. Risks
- The balances must be computed on the whole list before filtering; filtering first would show wrong balances. A test covers it.
- The filter card adds height above the list; at the largest font the list starts far down (criterion 39), but the screen scrolls.
- A typed date is incomplete while typing; its message must not flicker on every key (decision 1).
- The count line needs a plural ("1 entry shown", "2 entries shown").

## 8. Proof
Commands: as in 05. CI green on the pull request.
By hand on the phone, with 05's data:
1. Cash shows the filter card: From, To, the date note and five chips with All selected; no count line and no Clear filters (decision 3).
2. From 2026-01-05, To 2026-01-10: 4 rows (the Jan 5 deposit, the Jan 6 withdrawal, the two Jan 10 entries); "4 entries shown"; each row shows its real balance (the Jan 10 deposit shows $250.00).
3. Add the Deposit chip: 2 rows (Jan 5 and the Jan 10 deposit), "2 entries shown".
4. Clear the dates and choose Adjustment: the 04 adjustments and the 05 reversals.
5. The opening balance appears only with All.
6. Withdrawal with From 2026-02-01 and To 2026-02-28: "No entries match these filters".
7. Clear filters: both dates empty, All selected, every row back.
8. A From date of "2026-13-01": the date message under From; the list is not filtered by it (decision 1).
9. From after To: the no-match line (decision 2).
10. Switch to Overall and back: the filters are still set (decision 4).
11. Install over the 05 build with data: the data is still there (criterion 35).
12. Largest font, light and dark, TalkBack: each chip reads its name and whether it is selected; the count line and the no-match line are read.
13. Open an entry's detail with filters set, then go back: the filters are still set (decision 4).

## 9. Research
None planned; no new library.

## 10. Decisions (all Build, settled by you 2026-10-29)
1. An invalid From or To shows the existing date message under its field, once the field holds 10 characters; that bound is ignored until valid; an empty field means no bound.
2. From later than To shows the no-match line; no new message.
3. The count line and Clear filters appear only while a filter is active (a valid date bound, or a chip other than All).
4. The filters last while the app runs (across tab switches, the detail and a re-lock) and are never stored.

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- "From"*, "To"*, "Enter date in US market time (New York)"*.
- Chips: "All"*, "Deposit"*, "Withdrawal"*, "Interest/dividend"*, "Adjustment"*.
- "Clear filters"*. The count line: "1 entry shown"* and "<N> entries shown".
- The no-match line: "No entries match these filters"*.
- A bad date: "Date: use a real date as YYYY-MM-DD, like 2026-01-01." (approved in 02a).
Approved on paper with this file; confirmed on the phone (D9).

## 12. Status and notes
Approved 2026-10-29. Not started.
