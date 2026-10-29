# plan/iterations/03-deposit-withdrawal.md

Stage: Build. Status: built on branch iter-03-deposit-withdrawal (steps 1 to 6, commits ed5f8cb to 2654b9c) and checked on the phone 2026-10-29, all checks yes (section 13). Pull request #85 merged as 8d25892 on 2026-10-29; CI green on the pull request and on main (your reports). Decisions 1 to 5 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
Source: plan/plan.md section 4 (row 03), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
From the Cash tab I add a deposit or a withdrawal (date, amount, optional note, with a review step). A withdrawal that would make cash negative on its date or any later date is refused, with a message naming the date and the amount. The Cash tab and Overall update after a save.

## 2. Spec refs
- spec 4.1: the 4.1 order, replay from the entry's date through all later dates, cash < 0 blocks the save, future-dated entries count in net worth only from their date.
- spec 4.2: a withdrawal larger than the balance on that date or any later date is blocked; no entry before the opening balance; up to 2 decimals; never 0.
- spec 4.10 (integer cents) and 4.8 (net worth as of today; the Cash tab balance on the latest entry date).
- spec 6: Cash entry flow (wireframe 10b, frames A, D and J, markers 1 to 5 and 9, 10, 13); view 7 (the Cash entry button); the date note (C13); number format; error states (a blocked save keeps what you typed); accessibility; theme.
- Example C, cash only (section 3).
- Criteria: 9, 35, 39 to 42, 42 (the blocked withdrawal), 53 (part: deposit and withdrawal, the 0 and third-decimal refusals, Example C), 60(c) (a deposit dated tomorrow: Cash $1,500.00, net worth $1,000.00).
- spec 14: K1 (the rule), K2 (one-row save), K3 and K5 (the entry flow), K9.

## 3. Out of scope
- Interest/dividend and Adjustment (04); reversals and the cash entry detail screen (05); filters (06); trade lines (09).
- Example C with a buy: no buy exists before 09. 03 tests the same numbers with a withdrawal in place of the buy: opening $1,000 on Jan 1, withdrawal $800 on Jan 10, then a withdrawal of $300 on Jan 5 is blocked: negative on Jan 10 by $100.
- The multi-row "refused batch saves nothing" check (open item 14): moves to 09.
- Criterion 37: still deferred.

## 4. Files
New, core/:
- CashEntries.kt (K1): creates a deposit or withdrawal. Rules: the opening balance exists, the date is not before it, the amount is above 0, the note has no line break, the next enteredSeq, and a replay with an overflow guard.
- CashEntriesTest.kt.
Changed, core/: OpeningBalance.kt (the next-seq rule is shared); CashAccount.kt and CashRules.balanceOn (exact addition, Math.addExact; closes open item 9).
Changed, app/: AppViewModel.kt (the form gets a type: Opening balance, Deposit, Withdrawal; new refusals); OpeningBalanceScreen.kt renamed CashEntryScreen.kt (decision 3), with the type chips and the signed effect; CashScreen.kt (the Cash entry button is always shown: before the opening balance it opens that form, after it a deposit); strings.xml; app/build.gradle.kts (versionCode 3); AppViewModelTest.kt.
Documents: plan.md, open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-03-deposit-withdrawal from main (5e46dae). Baseline run.
2. core/: the overflow guard. Tests first (a sum that overflows is refused, not wrapped), then exact addition in balanceOn and CashAccount.
3. core/: CashEntries. Tests first: deposit, withdrawal, the date before the opening balance, 0, a line break, the next seq, Example C cash-only, same-date order, a future-dated deposit, overflow. Then the code.
4. ViewModel: tests first (the type in the form, the refusals, saving returns to Cash, typed text kept on a refusal), then the code.
5. Screens: the git mv rename (asks first), the type chips, the amount line, the review with the signed effect, the messages (wording in section 11).
6. versionCode 3. Full run, release build, install over 02b's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft.

## 6. Stubs
- The Interest/dividend and Adjustment chips are hidden until 04 (decision 2).
- Rows on the Cash tab still cannot be tapped (05).

## 7. Risks
- The replay is new K1 logic: every later date must be checked, not only the entry's date. Example C and same-date cases are tested.
- Same-date order on refusal: an entry on the same date as a withdrawal counts in entry order (4.1). My reading: a new entry goes after the existing entries of its date.
- The overflow guard changes balanceOn from wrapping to refusing. It is a K1 change; the existing tests must still pass unchanged.
- Generalising the form may break 02a's opening balance flow; its tests and phone checks are run again.
- Room saves one row; the multi-row atomicity stays unproven until 09.

## 8. Proof
Commands: as in 02b (build; tests with counts; lint with --rerun-tasks; spotlessCheck; release build). CI green on the pull request.
By hand on the phone:
1. With the opening balance present, Cash entry opens the form with Deposit selected, the Deposit and Withdrawal chips, today's New York date with the note, and the amount line.
2. A deposit of $500: the review shows Type Deposit, +$500.00 and the cash effect; confirm returns to Cash, the row is on top with its balance; Overall's net worth goes up.
3. A withdrawal within the balance: saved; the row shows a minus sign; nothing is colored.
4. Example C cash-only (fresh install): opening $1,000 on 2026-01-01, withdrawal $800 on 2026-01-10, then withdrawal $300 on 2026-01-05: refused with "negative on 2026-01-10 by $100.00"; the typed values stay; nothing is saved.
5. A date before the opening balance is refused with the date named.
6. 0, 1.234, abc, $5 and 1,000 are refused; the typed text stays.
7. A deposit dated tomorrow: Cash shows it in the balance, net worth does not (60(c)).
8. Two entries on the same date: the later one is on top and the balances chain (02b decision 8).
9. Install over the 02b build with data: the data is still there (criterion 35).
10. Largest font, light and dark, TalkBack on the chips, the form, the review and the message.
11. Logcat while saving: no amount.
12. The opening balance flow on a fresh install, unchanged.

## 9. Research
None planned; no new library.

## 10. Decisions (all Build, settled by you 2026-10-29)
1. The refusal names the first date the cash goes negative and the shortfall on that date, as in frame J.
2. Interest/dividend and Adjustment are hidden until 04.
3. OpeningBalanceScreen.kt is renamed CashEntryScreen.kt with git mv in step 5 (asks first).
4. The form opens from the Cash tab only (spec 6, navigation outline).
5. The balance is judged after every entry in the 4.1 order (date, then entry order), not at the end of each date (settled 2026-10-29, step 3). A dip inside a later date is refused even if that date ends positive.

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Chips: "Deposit"*, "Withdrawal"*. Title: "Cash entry"*.
- The line under the amount: "A positive number. It is added to cash."* (deposit) and "A positive number. It is taken out of cash." (withdrawal).
- Messages: "Not saved. Cash would be negative on <date> by <amount>."*; "Not saved. Cash entries cannot be dated before the opening balance (<date>)."*; "Not saved. The balance would be too large." (overflow).
- Review: "Review cash entry"*; the type, the date with ET, the signed amount, the note, the cash effect.
Approved on paper with this file; confirmed on the phone (D9).
Confirmed on the phone 2026-10-29 (checks 1, 4, 5 and 10): the chips, the two amount lines and the three messages as written above.

## 12. Status and notes
Done. Built 2026-10-29; phone checks 2026-10-29; pull request #85 merged as 8d25892 (2026-10-29), CI green on the pull request (3 checks) and on main (1m 42s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: 2654b9c step 6, b9696e7 step 5, de8c9bb step 4, d820167 step 3, 4e2234e step 2, ed5f8cb iteration file. main was 5e46dae.

### 13.2 Local run
On 2654b9c's code (2026-10-29, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 132 tests (written 133 until 2026-10-30, a sum error), 0 failures (Balance 11, CashAccount 10, CashEntries 17, MarketDate 10, Money 15, OpeningBalance 9, AppLock 12, AppViewModel 48); lint: 2 findings, both known, neither silenced (NewerVersionAvailable at libs.versions.toml:17; TypographyDashes at strings.xml:53, the same string as :48 before, moved by the new strings); release build green (52 tasks, R8 ran; unsigned). Debug APK versionCode 3 (aapt2).

### 13.3 Phone results (Android 17, debug build versionCode 3, your reports)
Checks 1 to 12 all yes. Notes:
- 9: installed over the 02b build (versionCode 2, adb install -r, firstInstallTime unchanged); the future-dated test balance was still there (criterion 35).
- 4: Example C cash-only refused with "Not saved. Cash would be negative on 2026-01-10 by $100.00."; text kept; nothing saved (criteria 42, 53).
- 7: a deposit dated tomorrow: Cash $350.00, net worth $250.00 (60(c)).
- 11: log cleared at 11:01:34; a deposit of $4,321.87 saved. The app's process (89 lines) had 0 matches for 4321, 4,321, 432187, 4321.87 and the screen labels. The whole log had 0 for every pattern except plain 4321 (2 lines from other processes, content not read).
- Part B ran on a fresh install (adb uninstall, 2026-10-29); the 02b test data was deleted on purpose. The phone now holds the Example C test data and two later deposits.

### 13.4 Not run or not checked
CI and the pull request; the release APK on a device (unsigned); contrast measurement (by eye only); criterion 37 (deferred); a refused multi-row save (open item 14, moved to 09); Compose UI tests (none in the project).

## 14. Departures from the plan
1. 02a's test reviewIsRefusedWhenAnOpeningBalanceAlreadyExists was replaced by withAnOpeningBalanceASecondOneCannotBeStarted (your decision, step 4): the form opens as a deposit once the opening balance exists (wireframe 10b marker 16), so a second one cannot be started. K1 still refuses one (OpeningBalanceTest, unchanged).
2. AppState.cashEntryShown became cashEntryType; three 02b assertions changed accordingly (the button now opens a deposit instead of being hidden). With it, the test cashTabAfterSavingShowsTheBalanceAndOneRowAndHidesTheButton was renamed cashTabAfterSavingShowsTheBalanceAndOneRowAndTheButtonOpensADeposit (name added 2026-10-30).
3. Review runs K1 twice so all refusals show together: the note with a stand-in date and amount, then the date and balance rules with an empty note, only when the date and amount are valid.
4. Step 3 first judged the balance at the end of each date; on your decision 5 it was changed to after every entry before the commit. The new test aDipInsideALaterDate_isRefused was not run against the end-of-date code.
5. The overflow guard throws ArithmeticException from the sums (CashRules.sumExact); K1's replay turns it into the TooLarge refusal.
6. CashScreen's typeLabel and signed became the shared cashTypeLabel and signedAmount, used by the rows and the review.
7. The new refusal messages use the date as YYYY-MM-DD without "ET", as frame J shows.
