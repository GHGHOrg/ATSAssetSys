# plan/iterations/03-deposit-withdrawal.md

Stage: Build. Status: approved 2026-10-29; decisions 1 to 4 settled the same day (section 10). Not started.
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

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Chips: "Deposit"*, "Withdrawal"*. Title: "Cash entry"*.
- The line under the amount: "A positive number. It is added to cash."* (deposit) and "A positive number. It is taken out of cash." (withdrawal).
- Messages: "Not saved. Cash would be negative on <date> by <amount>."*; "Not saved. Cash entries cannot be dated before the opening balance (<date>)."*; "Not saved. The balance would be too large." (overflow).
- Review: "Review cash entry"*; the type, the date with ET, the signed amount, the note, the cash effect.
Approved on paper with this file; confirmed on the phone (D9).

## 12. Status and notes
Approved 2026-10-29. Not started.
