# plan/iterations/14a-reverse-and-reenter-trades.md

Stage: Build. Status: approved 2026-11-02, not built. Decisions 1 to 4 settled (section 10).
Source: plan/plan.md section 4 (row 14a), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a buy's or sell's detail (13a) I tap Reverse and re-enter. The normal buy or sell screen opens with the original's values, its date included, under a banner naming what is replaced; a field I change shows the original value beneath it. On a sell the picks start from the original's and the lot list is shown as it will be after the reversal. Review shows one combined screen: part 1 the reversal (its date the original's, with Change), part 2 the new entry, and the combined cash effect. Confirm and save both writes the reversal and the new entry together; I return to the original, which says "Reversed on …". A pair that breaks a rule saves nothing, says nothing was changed and names the part and the rule; only a refusal about the reversal date is shown on the review, where Change fixes it.

## 2. Spec refs
- spec 4.6 Reverse and re-enter (the pair as one confirmed action, the pair check with both entries applied, a block from the original still applies, the reversal under the rules of 13b, the new entry with the original's values and the next entry order, the ticker check on a buy only, a sell's picks from the original's, not offered on a reversal, an entry already reversed or a trade line), 4.1 (one replay with both entries), 4.2 (cash), 4.3 (the new buy), 4.4 (the new sell and its lot rules), 4.10.
- spec 3: Transaction, the `trade` cash line; C5.
- Example G (the buy, both or neither, blocked by the original, the sell), as automated tests.
- Section 6: "Reverse and re-enter entry", "Buy and sell entry", "Cash, split and reversal entry" (where it starts and where it is not offered), the date-field note, Number format and color, Accessibility rule 5, Error states.
- Wireframe 10c: frames A to D and F (markers 1 to 10 and 12).
- Criteria: 21 (the buy and sell parts), 54 (all of it), 60(b) in part (the sell pair is not blocked by a ticker check and the realized loss becomes −$150.00; tested on K1 and the ViewModel), and 9, 35, 39 (TalkBack included), 40, 41, 42, 38 (the log during a pair).
- spec 14: K1 (the pair check), K2 (the pair written as one action), K3, K5, K9.

## 3. Out of scope
- Reverse and re-enter on a split or a cash entry (14b; 10c frame E, marker 11).
- 60(b)'s other parts: the restore of the sample file (34), OLDCO staying hidden (15), the Realized gains total (26).
- Hide and unhide (15, criterion 60(a)); open item 44 (before 15).
- Realized gains (26); Transaction history (27).
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
Changed, core/: Reversals.kt (the pair: the reversal and the new entry checked in one replay, the refusal naming its part, a reversal-date refusal told apart from the others, decision 3); Sells.kt (lot choices as they will be after a reversal of a sell); RecordStore.kt (saving a pair); their tests.
Changed, app/: RoomRecordStore.kt (the reversal, the new entry and their cash lines in one database transaction); AppViewModel.kt; BuyScreen.kt and SellScreen.kt (the replacing mode: banner, "Original: …" hints, the lot list after the reversal); new ReplaceReviewScreen.kt (the combined review with Change) and ReenterFlowTest.kt; TransactionDetailScreen.kt (Reverse and re-enter); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 15); the fake store.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No new library, no schema change and no new network call (the buy's ticker check is 09's).

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-14a-reverse-and-reenter-trades from main (8d2769f, after the pull with your yes). This file and the plan.md lines: rows 14a and 14b, the 14 split, the #111 merge. Baseline run.
2. core/: tests first, then the code. Example G: the buy re-entered as 100 for $1,000.00 (cash $9,000.00, the first lot at 0, the new lot 100 at $10.00, realized 0); both or neither with $10,500.00 (refused, naming Jan 2 and $500.00, the reversal part not saved alone); blocked by the original (with the sell standing: reverse the sell first; after its reversal, the reversal dated Jan 2 refused as a reversal-date refusal naming Jan 3; dated Jan 3 with the new buy on Jan 2 allowed); the sell re-entered at $2,200.00 with the original picks (cash $9,700.00, cost $1,300.00, realized $900.00); the lot choices after a sell's reversal, with 1 for 3 and 1 for 7 splits and remainders that round up and down (CLAUDE.md pitfall from 12); the new entry's order after the reversal; a pair on a reversal, a reversed entry or a cash line refused; 60(b)'s sell at $0.00 with cost $200.00 re-entered at $50.00 (realized −$150.00).
3. Data layer: the pair saved in one transaction; a failure part-way leaves nothing (fake store and row-mapping tests).
4. ViewModel: tests first (Reverse and re-enter offered only on a buy or sell not reversed and not a reversal; the screen prefilled with the original's values; a changed field keeps the original for its hint; the sell's picks from the original; Review with the pair applied; a reversal-date refusal opens the review, every other refusal keeps the entry screen (decision 3); Change and its inline date with the effect updated as typed (decision 4); Confirm saves both and returns to the original's detail (decision 2); Back to edit keeps the values; the ticker check runs for the new buy and never for the new sell (60(b), offline); a bottom tab), then the code.
5. Wording questions, then the screens and strings: Reverse and re-enter on the detail; the buy and sell screens in replacing mode; the combined review for a buy and a sell; the blocked messages.
6. versionCode 15. Full run, release build, install over 13b's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- A split's or cash entry's detail has no Reverse and re-enter until 14b.
- A sell pair that leaves a hidden holding at 0 shares keeps it hidden (4.4), but nothing can be hidden before 15.

## 7. Risks
- The pair check (4.1, 4.6): one replay with the reversal at its date and order and the new entry after it; lots are checked one by one, so the new buy's shares never cover the original lot (Example G, blocked by the original).
- Decision 3: telling a reversal-date refusal apart from the others must not hide a second rule; K1 stops at the first rule that fails (CLAUDE.md pitfall from 05), so the review is opened only when the pair passes with the earliest date that works.
- The sell's lot list after the reversal: the original's picks put back with the splits since, and a pick's remainder still the least the lot holds at the new sell's place or later (13b departure 2), with the original's sell left out.
- Dates: the new entry keeps its own date when Change moves the reversal date, so the new entry may be dated before its reversal (criterion 54); my reading is that 4.6 allows it, as only the reversal has the date rule.
- Atomic save (K2): two transactions and two cash lines in one database transaction; a damaged pair must never be half-written.
- Rounding: a changed sell quantity with fractional lots (8 places), tested with 2/3, 5/3, 3/7, 1/7.

## 8. Proof
Commands: as in 13b, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 13b's data (AAPL in Main at 9.33333333 shares, cost $1,050.00, lots of 2026-10-31 at 8 and 1.33333333 left, both $112.50 per share; net worth $4,031.87; opening balance 2026-01-01; the five reversals of 13b). Each check names its start and the control (CLAUDE.md pitfall from 11). The exact figures are worked out before each check from the data then on the phone.
1. Install over 13b's build: the data is still there (criterion 35).
2. Reverse and re-enter is on a buy's and a sell's detail and not on a reversal, a reversed entry, a split, a cash entry or a trade line's detail; it opens the buy screen prefilled, the banner, "Original: …" under a changed field; Back leaves nothing saved.
3. Example G on the phone: a buy re-entered with a changed quantity; the combined review (parts 1 and 2, the combined cash effect); Confirm and save both returns to the original, "Reversed on …"; the reversal and the new buy in Holding detail and on the Cash tab.
4. Both or neither: a buy re-entered with a total above the cash: refused on the buy screen, "Nothing was changed", the date and the shortfall; nothing saved.
5. A sell re-entered with a changed total: the picks from the original, the lots as after the reversal, the realized gain and the combined cash effect on the review; saved.
6. Blocked by the original (set up on the phone with a new buy and a sell of all of it): refused, "Reverse the sell … first"; after the sell's reversal at a later date, the review opens with the reversal-date message; Change to that date, the new buy keeps its date; saved.
7. The ticker check: the new buy runs it online; the new sell offline is not blocked by it (60(b) in part).
8. Largest font and landscape, light and dark, TalkBack through a whole pair (criteria 39 to 41).
9. Close and reopen: the originals, reversals, new entries and their cash lines are kept.
10. Logcat during a pair (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-11-02)
1. Iteration 14 is split before it starts: 14a for a buy and a sell (criteria 21, 54 and 60(b) in part), 14b for a split and a cash entry (option B).
2. After Confirm and save both the app returns to the original's detail, as 13b does (option A).
3. Only a refusal about the reversal date opens the combined review, with the message under part 1 next to Change; every other refusal keeps the entry screen with what was typed (option A).
4. Change turns the reversal date row into the inline digits-only date field of 13b's reversal screen, with its note, the effect updating as typed (option A).
Settled by the spec, no question asked: the normal entry screen prefilled with a banner and "Original: …" hints, one combined review with a Change link, Confirm and save both as the only save, a sell's picks from the original's with the lot list as after the reversal (section 6, 10c); the pair checked in one replay and written as one action (4.6, K1, K2); the ticker check on a buy only (4.6); not offered on a reversal, an entry already reversed or a trade line (4.6).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Reverse and re-enter"* (spec 4.6), "< Buy (replacing)"*, "< Sell (replacing)"*, "Replacing the buy of 2026-01-02 ET: 10 AAPL, $1,000.00. Nothing changes until you confirm."*, "Original: 10"*, "Lots, as they will be after the reversal"*, "Picked 12 of 12 shares (the original's picks)"*.
- "< Review both"*, "1. Reverse the original"*, "2. Enter the replacement"*, "Reverses"*, "Reversal date"*, "Change"*, "Shares removed"*, "Lots put back"*, "Realized gain removed"*, "Cash effect"*, "Combined cash effect"*, "Both are saved together, or neither is."*, "Confirm and save both"*, "Back to edit"*.
- "Not saved. Nothing was changed. With the replacement, cash would be negative on 2026-01-02 by $500.00."*, "Not saved. Nothing was changed. The reversal would make the lot of 2026-01-02 ET hold −10 shares on 2026-01-03. Reverse the sell of 2026-01-03 first, then use Reverse and re-enter with the reversal dated on or after that reversal."*, "Not saved. Nothing was changed. The reversal, dated 2026-01-02, is applied before the sell of 2026-01-03 and would make the lot of 2026-01-02 ET hold −10 shares. Use Change to pick a reversal date of 2026-01-03 or later (the date the sell was reversed)."*, "Not saved. Nothing was changed. The lot of 2026-03-15 ET was bought after the sell date."* (frame F placeholders, D9).
- New, asked in step 5: the other refusals in the pair form (the new entry's own rules named as part 2), the reversal date refused as earlier than the original on the review, and the TalkBack sentences.

## 12. Status and notes
Approved 2026-11-02 (decisions 1 to 4). Not built.
