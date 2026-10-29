# plan/iterations/06-cash-filters-date-type.md

Stage: Build. Status: built on branch iter-06-cash-filters-date-type (steps 1 to 5 and one change, commits d93f99a to 42121aa) and checked on the phone 2026-10-29, all checks yes (section 13). Pull request #91 merged as 0d5e2f4 on 2026-10-29; CI green on the pull request and on main (your reports). Decisions 1 to 4 settled; decision 1 changed after phone check 12 (section 14). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
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
1. An invalid From or To shows the existing date message under its field, once the field holds 10 characters; that bound is ignored until valid; an empty field means no bound. Changed 2026-10-29 after phone check 12 (your request): the fields take digits only (YYYYMMDD) on a number keyboard, the dashes are shown as you type, any other character is refused at once with "Date: digits only, like 20260105.", eight digits that are no real date get the date message, fewer digits get nothing (section 14, item 1).
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
Added and approved 2026-10-29 (after phone check 12): "Date: digits only, like 20260105." The empty fields show "YYYY-MM-DD"* as a placeholder. Confirmed on the phone (checks 1 to 10, 12, 13 and D1 to D6).

## 12. Status and notes
Done. Built 2026-10-29; phone checks 2026-10-29; pull request #91 merged as 0d5e2f4 (2026-10-29), CI green on the pull request (3 checks) and on main (1m 51s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: 42121aa change, ed3111e step 5, c256698 step 4, f36dd0e step 3, 66b3b82 step 2, d93f99a iteration file. main was da9f26e.

### 13.2 Local run
On 42121aa's code (2026-10-29, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 213 tests (written 214 until 2026-10-30, a sum error), 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, MarketDate 10, Money 15, OpeningBalance 9, AppLock 12, AppViewModel 90, DateDigits 7); lint: 3 findings, all listed, none silenced (AndroidGradlePluginVersion at libs.versions.toml:2, new on 2026-10-29 between two runs: AGP 9.5.0 available, 9.4.1 in use, open item 31; NewerVersionAvailable at :17; TypographyDashes on date_not_a_date, now strings.xml:86). Release build on ed3111e's code green (52 tasks, R8 ran; unsigned); the change was not rebuilt as release. Debug APK versionCode 6 (aapt2).

### 13.3 Phone results (Android 17, debug build versionCode 6, your reports)
Checks 1 to 13 all yes, and D1 to D6 after the change. Notes:
- 11: installed over the 05 build (versionCode 5, adb install -r, firstInstallTime unchanged); the data was still there (criterion 35). The changed build was installed over it again at 21:10:44.
- 2 and 3: 2026-01-05 to 2026-01-10 showed 4 rows with "4 entries shown"; the Jan 10 deposit kept Balance $250.00; with Deposit, 2 rows.
- 4: Adjustment showed the 04 adjustments and the 05 reversals (6 rows).
- D1 to D6: number keyboard; 20260105 shown as 2026-01-05; no 9th digit; a non-digit gives the digits message at once; 20261301 gives the date message; the filter works with digits; readable at the largest font.
- No uninstall in 06; the phone holds the 03 to 05 data.

### 13.4 Not run or not checked
CI and the pull request; the release APK on a device (unsigned); a release build of the change commit; contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project); the cursor mapping on the phone beyond D2 (the mapping is unit tested).

## 14. Departures from the plan
1. Decision 1 changed after phone check 12, on your request: digits only on a number keyboard, the dashes shown by the field (a VisualTransformation; the rules in a new plain-Kotlin DateDigits with its own 7 tests), a ninth digit not accepted, any other character refused at once with a new message (DateRefusal.NOT_DIGITS), a pasted date with dashes refused too. Tests first (they failed to compile), then the code; commit 42121aa. Five 06 ViewModel tests from step 3 had their typed dates changed from "2026-01-05" to "20260105" (no assertion changed); four new ViewModel tests. Only the two filter fields changed (your decision A); the cash entry form's date and the reversal date are open item 32.
2. With no entries at all, Cash shows "No cash entries yet." and no filter card (step 4; my reading: filters are pointless on an empty list).
3. The four filter callbacks are passed to CashScreen together as CashFilterActions.
4. The empty date fields show the placeholder "YYYY-MM-DD", as wireframe 07 draws in its fields.
5. A new lint notice appeared between the baseline and step 2 (AGP 9.5.0); listed, not silenced, upgrade left to its own change (your decision; open item 31).
