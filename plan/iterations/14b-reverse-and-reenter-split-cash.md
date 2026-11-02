# plan/iterations/14b-reverse-and-reenter-split-cash.md

Stage: Build. Status: approved 2026-11-02, not built. Decision 1 settled (section 10).
Source: plan/plan.md section 4 (row 14b), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a split's detail (13a) or a direct cash entry's detail (05) I tap Reverse and re-enter. The split screen or the cash entry form opens with the original's values, its date included, under a banner naming what is replaced; a field I change shows the original value beneath it. Review shows the combined review of 14a: part 1 the reversal (its date the original's, with Change; for a cash entry an Adjustment of the opposite amount with the fixed note naming the original), part 2 the new split or cash entry, and the combined cash effect (none for a split). Confirm and save both writes the reversal and the new entry together; I return to the original, which says "Reversed on …". A pair that breaks a rule saves nothing, says nothing was changed and names the part and the rule.

## 2. Spec refs
- spec 4.6 Reverse and re-enter (as in 14a), reversing a split (the inverse ratio, 13b decision A) and reversing a cash entry (an Adjustment of the opposite amount, the note naming the original by type and date; never the opening balance or a trade line), 4.1 (one replay with both entries), 4.2 (cash entries, the note required on an adjustment), 4.5 (the new split), 4.10.
- spec 3: CashEntry (reverses_id), Transaction; C5.
- Section 6: "Reverse and re-enter entry", "Cash, split and reversal entry" (where it starts and where it is not offered), the date-field note, Number format and color, Accessibility rule 5, Error states.
- Wireframe 10c: frame E and marker 11 (a cash entry; a split works the same way), with frames B, D and F as built in 14a; wireframe 10b marker 19 (the reversal note).
- Criteria: 21 (the split and cash parts: not offered on the opening balance, a reversal, a trade line or an entry already reversed; both or neither), and 9, 35, 39 (TalkBack included), 40, 41, 42, 38 (the log during a pair).
- spec 14: K1 (the pair check), K2 (the pair written as one action), K3, K5, K9.

## 3. Out of scope
- Hide and unhide (15, criterion 60(a)); open items 44 and 46 (before 15).
- Realized gains (26); Transaction history (27).
- The `reversal` row of the backup file (31, 33, 34).
- The digits-only date for the cash entry form (open item 32): the form keeps its YYYY-MM-DD field; the reversal date on the review is digits only, as in 14a.
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
Changed, core/: Reentries.kt (reenterSplit and reenterCash: the reversal under the rules of 13b or 05, then the new split or cash entry as a normal one after it, one replay; the refusals naming their part; offered on a split and on a direct cash entry; a split's first ratio is the original's); RecordStore.kt (saving a split pair; a cash pair is two cash entries saved by saveCashEntries, already one batch); their tests.
Changed, app/: AtsDatabase.kt and RoomRecordStore.kt (a split pair in one database transaction); AppViewModel.kt; CashEntryScreen.kt and SplitScreen.kt (the replacing mode: title, banner, "Original: …" hints, the pair's messages); ReplaceReviewScreen.kt (part 1 and part 2 for a split and a cash entry); CashEntryDetailScreen.kt and TransactionDetailScreen.kt (Reverse and re-enter); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 16); ReenterFlowTest.kt; RowMappingTest.kt; the fake stores.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No new library, no schema change and no new network call.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-14b-reverse-and-reenter-split-cash from main (c509d66, after the pull with your yes). This file and the plan.md lines (with the #113 merge). Baseline run.
2. core/: tests first, then the code. A split re-entered with a corrected ratio (a 2 for 1 entered as 3 for 1: the lots before it end at the new ratio, exact); a split pair refused when the new ratio leaves a later sell short, naming the new split; a split pair blocked by the original's reversal (13b's SellShort), naming part 1; a deposit re-entered with a changed amount (Frame E: a deposit of $500.00 re-entered as $50.00: −$500.00 and +$50.00, combined −$450.00); a cash pair refused when cash would go negative (with the replacement, the date and shortfall), and on a shared date judged as one point (14a decision 5); the reversal's fixed note (decision 1); a type changed in part 2 (a deposit re-entered as a withdrawal); not offered on the opening balance, a trade line, a reversal or an entry already reversed; rounding: a split re-entered after a 1 for 3 and a 1 for 7 with remainders that round up and down (CLAUDE.md pitfall from 12).
3. Data layer: a split pair saved in one transaction; a cash pair through saveCashEntries; row-mapping tests.
4. ViewModel: tests first (offered on a split and a direct cash entry only; the screens prefilled; a changed field keeps the original for its hint; Review with the pair applied, a reversal-date refusal opening the review (14a decision 3); Change; Confirm saves both and returns to the original's detail; Back to edit, a bottom tab and a failed save, as in 14a), then the code.
5. Wording questions, then the screens and strings: Reverse and re-enter on the two details; the split screen and the cash form in replacing mode; the combined review's parts for a split and a cash entry; the messages.
6. versionCode 16. Full run, release build, install over 14a's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- None planned beyond section 3.

## 7. Risks
- Two pair results: a cash pair holds two cash entries and no transaction, a split pair two transactions and no cash line; 14a's ReentryResult holds a transaction and a cash line each, so it grows or gets siblings (step 2).
- A split pair applies the inverse ratio and the new ratio at nearly the same place; every lot placed before it changes twice, so the replay must judge later sells after both (4.1, 4.5; 13b decision A).
- A cash pair's reversal note is fixed (decision 1), so it can never be blank; the adjustment rule of 04 stays in K1 for the plain reversal.
- The cash form and its refusals: K1 stops at the first rule that fails, so the form's stand-ins (04, 05; CLAUDE.md pitfall) must still show every field refusal together while replacing.
- Dates: the cash form's date is typed with dashes (open item 32), the reversal date on the review as digits (14a); the two must not be confused in the parsing.

## 8. Proof
Commands: as in 14a, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 14a's data (AAPL in Main at 10 shares, $1,050.00; the splits of 2026-10-31 and 2026-11-01; net worth $4,091.87; cash $3,041.87; opening balance 2026-01-01). Each check names its start and the control (CLAUDE.md pitfall from 11). The exact figures are worked out before each check from the data then on the phone, every sell that ever took from a lot included (CLAUDE.md pitfall from 14a).
1. Install over 14a's build: the data is still there (criterion 35).
2. Reverse and re-enter is on a split's and a direct cash entry's detail, and not on the opening balance, a trade line, a reversal or an entry already reversed; the screens open prefilled with the banner; Back saves nothing.
3. A cash entry re-entered with a changed amount: the combined review (part 1 the Adjustment with its fixed note, part 2, the combined cash effect); saved; back on the original, "Reversed on …"; the Cash tab and net worth.
4. Both or neither: a cash entry re-entered so cash goes negative: refused, "Nothing was changed", the date and shortfall.
5. A split re-entered with a changed ratio: the review (the inverse ratio and the shares before and after; the new split with its shares before and after; no cash); saved; the lots.
6. A split pair refused for a later sell left short, if the phone's data has one; otherwise unit tested only.
7. Largest font and landscape, light and dark, TalkBack through a whole pair (criteria 39 to 41).
8. Close and reopen: the pairs and their links kept.
9. Logcat during a pair (log cleared first): no amount, note, ticker or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (Build, settled by you 2026-11-02)
1. In a cash entry's pair, part 1's note is the pre-filled "Reversal of the … of … ET" and cannot be edited; a plain Reverse (05) still lets it be edited (option A).
Settled by the spec or by 14a and 05, no question asked: the normal cash form or split screen prefilled, with the banner and "Original: …" hints, every field editable, the type chip included (10c markers 1 to 3); the combined review with Change for the reversal date, and only a reversal-date refusal opening it (14a decisions 3 and 4); a shared date judged as one point (14a decision 5); after the save, the original's detail (14a decision 2, 05); the new entry's first values carry the splits since the original (14a fix).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Reverse and re-enter"*, "Replacing the … of … ET: … Nothing changes until you confirm."* (14a), "Original: …"*, "< Review both"*, "1. Reverse the original"*, "2. Enter the replacement"*, "Type"*, "Adjustment"*, "Note"*, "Reversal of the deposit of 2026-04-20 ET"* (05), "Combined cash effect"*, "Confirm and save both"*, "Back to edit"*, "Change"*, the 14a messages*.
- New, asked in step 5: the titles of the replacing cash form and split screen, the banner for a cash entry and a split, part 1 and part 2 for a split (the ratios and shares), the cash and split refusals in the pair form, and the TalkBack sentences.

## 12. Status and notes
Approved 2026-11-02 (decision 1). Not built.
