# plan/iterations/14b-reverse-and-reenter-split-cash.md

Stage: Build. Status: approved 2026-11-02, built 2026-11-02 and checked 2026-11-03 (section 12). Decision 1 settled (section 10).
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
- Approved in step 5 (your answers 2026-11-02):
  - Wording 1 (option A): "Cash entry (replacing)", "Split (replacing)"; "Replacing the deposit of 2026-10-29 ET: +$100.00. Nothing changes until you confirm."; "Replacing the split of 2026-11-01 ET: 2 for 1 AAPL. Nothing changes until you confirm."; part 1 for a cash entry as Type, Note and Cash effect, for a split as 13b's Ratio applied, Shares at this point and Shares after reversal; part 2 for a split as "Split AAPL, Main", the ratio, Shares at this point and Shares after split, with no combined cash effect; the refusals in the pair form as 14a's, "Not saved. Nothing was changed." in front, the cash one naming the replacement or the reversal; the TalkBack sentences "Reverse and re-enter deposit of 2026-01-10 Eastern Time", the banners read as one sentence, "Deposit, 2026-01-10 Eastern Time, plus 60.00 dollars".
  - Wording 2 (option A): the direction hint under a changed direction shows the chip's own label, "Original: Withdrawal (−)" or "Original: Deposit (+)", not "Original: Out".

## 12. Status and notes
Built 2026-11-02 (steps 1 to 6) and one fix 2026-11-03 (section 14, item 6); phone checks 2026-11-02 to 2026-11-03, all yes after the fix. Deploy review 2026-11-03: one High finding fixed on the branch and rechecked on the phone (section 14, item 8). Not yet merged.

## 13. Build results
### 13.1 Commits
Newest first: bcb2593 Deploy fix, d844c6d step 8, c7e67eb fix (step 7), 032e5cb step 6, f6726dc step 5, 797b766 step 4, c2c40f6 step 3, eb85840 step 2, 97f7995 step 1 with this file. main was c509d66.

### 13.2 Local run
On c7e67eb's code (2026-11-03, all with --rerun-tasks): spotlessCheck green; debug build green, no compiler warnings printed; 591 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 10, ReentriesSplitCash 14, Reentries 20, Reversals 14, Sells 19, Splits 23, TickerCheck 16, Transactions 18, RowMapping 14, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, ReenterFlow 27, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint: the 4 listed findings, none silenced (AndroidGradlePluginVersion, open item 31; NewerVersionAvailable for kotlinx-coroutines-test and for sqlcipher-android, open item 43; TypographyDashes on date_not_a_date, strings.xml:86), report written 01:09:14. Release build on the same code green (R8 ran; unsigned; its strip step was up to date, so "Unable to strip" was not printed again). Debug APK versionCode 16 (aapt2 36.0.0). No schema change (still 5.json).

### 13.3 Phone results (Android 17, debug build versionCode 16, your reports)
Checks 1 to 9 all yes. Each check's figures were worked out from the data on the phone (your photos of the Cash tab and of AAPL's Transactions and Lots tabs) before it was given.
- 1: installed over the 14a build (lastUpdateTime 2026-11-02 20:59:33, after the fix 2026-11-03 01:11:58; firstInstallTime 2026-10-29 10:44:04 unchanged); net worth $4,091.87, cash $3,041.87, AAPL 10 shares at $1,050.00, lots 8 at $112.50 and 2 at $75.00 (criterion 35).
- 2: Reverse and re-enter not on the opening balance, a reversal (the 2026-10-30 adjustment and zqxrevcheck), or an entry already reversed (the Fee, the interest/dividend); on the 2026-10-29 deposit of $100.00 it opened "Cash entry (replacing)" with the banner and the original's values; a changed amount showed "Original: $100.00"; Back saved nothing; on a split it opened "Split (replacing)" with its banner; Back saved nothing. A trade line on the Cash tab opens its trade (criterion 29), so "not on a trade line" is unit tested only.
- 3: that deposit re-entered as $150.00: part 2 showed the note "check" where none was typed; fixed (section 14, item 6) and checked again: part 1 an Adjustment of −$100.00 with the fixed note "Reversal of the deposit of 2026-10-29 ET", part 2 +$150.00 with no note, combined +$50.00; saved; "Reversed on 2026-10-29 ET"; cash $3,091.87, net worth $4,141.87, the balances after it $50.00 higher.
- 4: both or neither: the 2026-01-06 withdrawal of $500.00 re-entered as $801.00: "Not saved. Nothing was changed. With the replacement, cash would be negative on 2026-01-10 by $101.00."; the 2026-01-05 deposit re-entered on 2026-01-11: "Not saved. Nothing was changed. The reversal would make cash negative on 2026-01-10 by $300.00."; nothing saved.
- 5: the 2026-11-01 split 2 for 1 (every entry after it reversed) re-entered as 3 for 1: part 1 ratio applied 1 for 2, 10 to 5 shares; part 2 5 to 15 shares; no cash; saved; "Reversed on 2026-11-01 ET"; AAPL 15 shares at $1,050.00, lots 12 at $75.00 and 3 at $50.00.
- 6: the 2026-10-31 split 2 for 1 re-entered as 1 for 4: "Not saved. Nothing was changed. With this split, the sell of 2026-11-01 would take more shares than its lot holds." (that sell, reversed later that day, still counts at its place); nothing saved.
- 7: largest font and display size, landscape, light and dark: the replacing cash form, the split screen, the review and the inline date field with the keyboard readable and reachable; TalkBack through a whole pair (the 2026-01-10 deposit of $50.00 re-entered as $60.00, saved), read as wording 1; cash $3,101.87, net worth $4,151.87.
- 8: closed and reopened: both cash pairs, the split pair and their links kept; AAPL 15 shares, lots unchanged.
- 9: Logcat cleared at 02:01:26, then a cash pair (the "Match broker balance" adjustment of $20.00 re-entered as $23.45 with the note zqxpaircheck, saved) and a refused split pair; 6,613 lines, 473 from the app's uid 10454; no note, ticker, portfolio name, ratio, new amount or balance in the app's lines; its "Main" hits were all the class name MainActivity and its "20.00" hits all frame rates (120.0000x). Counts only. Afterwards: cash $3,105.32, net worth $4,155.32.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); a failed pair save on the phone (open item 14; unit tested, and atomic by Room's @Transaction or one batch); "not on a trade line" for a cash line on the phone (unreachable, see check 2); a shared-date cash pair judged as one point and a split pair refused by its reversal alone (unit tested only); contrast measurement (by eye on the phone); network traffic (open item 39); the screens in a JVM test (none in the project).

## 14. Departures from the plan
1. CashEntries.kt (not in section 4's list) was split into drafts and one replay, so a cash pair is judged in one replay; create and reverse behave as before, and their 39 tests are unchanged (step 2).
2. Reentries.offered now uses Reversals.offered, which includes a split; 14a's test that a split is not offered was changed to expect it, as 14b intends (steps 2 and 4).
3. TransactionDetailScreen.kt (in section 4's list) is unchanged: its Reverse and re-enter button already follows the ViewModel's offer, which now includes a split (step 4).
4. Steps 2 to 4 were built before their commits on your request ("continue now, commit later"), but each step was committed on its own, in order.
5. Wording 2 was asked in step 5 because the direction hint differed from the wireframe's "Original: Out" (option A, the chip's label).
6. Defect found in phone check 3: the combined review of a cash pair showed the stand-in note "check" instead of the typed note, because the review showed the pair from the check with stand-ins (used so the note's refusal and the cash rule show together). Confirm saved the real note. Fixed in c7e67eb (your decision A): the stand-in is used only when the note itself is refused; failing ViewModel tests first (2, plus 1 that the note and cash refusals still show together); reinstalled at versionCode 16; check 3 passed again.
7. Phone check 6 ran on the phone: the data had a later sell left short by a split pair.
8. Deploy finding 1 (High), fixed before the pull request (your decision A): a split re-entered on another ticker or portfolio was saved even when its reversal alone left the original holding short, because the new split's check covers only its own holding. The original holding is now checked with both entries applied, and a sell left short is part 1's refusal (13b's message). Failing core test first (the pair came back saved), plus a test that a move leaving the original whole still saves, and a ViewModel test written after the code; 594 tests, all green, lint the 4 listed findings (report 02:16:43); bcb2593, reinstalled at versionCode 16 (2026-11-03 02:18:28). On the phone, the 2026-10-31 split 2 for 1 moved to 投資信託 (SPY) was refused: "… the sell of 2026-11-01 would take more shares than its lot holds." (AAPL at 19 shares that day, halved to 9.5, after the 1 for 3 about 3.17, short of the 4.66666667 sold); nothing saved. Finding 2 (Low, a stale comment in TransactionDetailScreen.kt) was fixed in the same commit.
