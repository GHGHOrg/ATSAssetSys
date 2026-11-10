# plan/iterations/14a-reverse-and-reenter-trades.md

Stage: Build. Status: approved 2026-11-02; done: built and checked 2026-11-02, pull request #112 merged as bcd5e3b (2026-11-02) (section 12). Decisions 1 to 5 settled (section 10).
Source: plan/plan.md section 4 (row 14a), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a buy's or sell's detail (13a) I tap Reverse and re-enter. The normal buy or sell screen opens with the original's values, its date included, under a banner naming what is replaced; a field I change shows the original value beneath it. On a sell the picks start from the original's and the lot list is shown as it will be after the reversal. Review shows one combined screen: part 1 the reversal (its date the original's, with Change), part 2 the new entry, and the combined cash effect. Confirm and save both writes the reversal and the new entry together; I return to the original, which says "Reversed on …". A pair that breaks a rule saves nothing, says nothing was changed and names the part and the rule; only a refusal about the reversal date is shown on the review, where Change fixes it.

## 2. Spec refs
- spec 4.6 Reverse and re-enter (the pair as one confirmed action, the pair check with both entries applied, a block from the original still applies, the reversal under the rules of 13b, the new entry with the original's values and the next entry order, the ticker check on a buy only, a sell's picks from the original's, not offered on a reversal, an entry already reversed or a trade line), 4.1 (one replay with both entries), 4.2 (cash), 4.3 (the new buy), 4.4 (the new sell and its lot rules), 4.10.
- spec 3: Transaction, the `trade` cash line; C5.
- Example G (the buy, both or neither, blocked by the original, the sell), as automated tests.
- Section 6: "Reverse and re-enter entry", "Buy and sell entry", "Cash, split and reversal entry" (where it starts and where it is not offered), the date-field note, Number format and color, Accessibility rule 5, Error states.
- Wireframe 10c: frames A to D and F (markers 1 to 10 and 12).
- Criteria: AC-21 (the buy and sell parts), AC-54 (all of it), AC-60(b) in part (the sell pair is not blocked by a ticker check and the realized loss becomes −$150.00; tested on K1 and the ViewModel), and AC-09, AC-35, AC-39 (TalkBack included), AC-40, AC-41, AC-42, AC-38 (the log during a pair).
- spec 14: K1 (the pair check), K2 (the pair written as one action), K3, K5, K9.

## 3. Out of scope
- Reverse and re-enter on a split or a cash entry (14b; 10c frame E, marker 11).
- 60(b)'s other parts: the restore of the sample file (34), OLDCO staying hidden (15), the Realized gains total (26).
- Hide and unhide (15, AC-60(a)); open item 44 (before 15).
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
- Dates: the new entry keeps its own date when Change moves the reversal date, so the new entry may be dated before its reversal (AC-54); my reading is that 4.6 allows it, as only the reversal has the date rule.
- Atomic save (K2): two transactions and two cash lines in one database transaction; a damaged pair must never be half-written.
- Rounding: a changed sell quantity with fractional lots (8 places), tested with 2/3, 5/3, 3/7, 1/7.

## 8. Proof
Commands: as in 13b, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 13b's data (AAPL in Main at 9.33333333 shares, cost $1,050.00, lots of 2026-10-31 at 8 and 1.33333333 left, both $112.50 per share; net worth $4,031.87; opening balance 2026-01-01; the five reversals of 13b). Each check names its start and the control (CLAUDE.md pitfall from 11). The exact figures are worked out before each check from the data then on the phone.
1. Install over 13b's build: the data is still there (AC-35).
2. Reverse and re-enter is on a buy's and a sell's detail and not on a reversal, a reversed entry, a split, a cash entry or a trade line's detail; it opens the buy screen prefilled, the banner, "Original: …" under a changed field; Back leaves nothing saved.
3. Example G on the phone: a buy re-entered with a changed quantity; the combined review (parts 1 and 2, the combined cash effect); Confirm and save both returns to the original, "Reversed on …"; the reversal and the new buy in Holding detail and on the Cash tab.
4. Both or neither: a buy re-entered with a total above the cash: refused on the buy screen, "Nothing was changed", the date and the shortfall; nothing saved.
5. A sell re-entered with a changed total: the picks from the original, the lots as after the reversal, the realized gain and the combined cash effect on the review; saved.
6. Blocked by the original (set up on the phone with a new buy and a sell of all of it): refused, "Reverse the sell … first"; after the sell's reversal at a later date, the review opens with the reversal-date message; Change to that date, the new buy keeps its date; saved.
7. The ticker check: the new buy runs it online; the new sell offline is not blocked by it (60(b) in part).
8. Largest font and landscape, light and dark, TalkBack through a whole pair (AC-39 to AC-41).
9. Close and reopen: the originals, reversals, new entries and their cash lines are kept.
10. Logcat during a pair (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-11-02)
1. Iteration 14 is split before it starts: 14a for a buy and a sell (AC-21, AC-54 and AC-60(b) in part), 14b for a split and a cash entry (option B).
2. After Confirm and save both the app returns to the original's detail, as 13b does (option A).
3. Only a refusal about the reversal date opens the combined review, with the message under part 1 next to Change; every other refusal keeps the entry screen with what was typed (option A).
4. Change turns the reversal date row into the inline digits-only date field of 13b's reversal screen, with its note, the effect updating as typed (option A).
5. (Asked after step 2, 2026-11-02.) When the reversal and the new entry share a date, the cash replay judges them as one point, after the new entry, so a sell's reversal may take cash below 0 for a moment before the new sell puts proceeds back (4.6 "checked as a unit"); on different dates every entry is judged, as 03 decision 5 (option B).
Settled by the spec, no question asked: the normal entry screen prefilled with a banner and "Original: …" hints, one combined review with a Change link, Confirm and save both as the only save, a sell's picks from the original's with the lot list as after the reversal (section 6, 10c); the pair checked in one replay and written as one action (4.6, K1, K2); the ticker check on a buy only (4.6); not offered on a reversal, an entry already reversed or a trade line (4.6).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Reverse and re-enter"* (spec 4.6), "< Buy (replacing)"*, "< Sell (replacing)"*, "Replacing the buy of 2026-01-02 ET: 10 AAPL, $1,000.00. Nothing changes until you confirm."*, "Original: 10"*, "Lots, as they will be after the reversal"*, "Picked 12 of 12 shares (the original's picks)"*.
- "< Review both"*, "1. Reverse the original"*, "2. Enter the replacement"*, "Reverses"*, "Reversal date"*, "Change"*, "Shares removed"*, "Lots put back"*, "Realized gain removed"*, "Cash effect"*, "Combined cash effect"*, "Both are saved together, or neither is."*, "Confirm and save both"*, "Back to edit"*.
- "Not saved. Nothing was changed. With the replacement, cash would be negative on 2026-01-02 by $500.00."*, "Not saved. Nothing was changed. The reversal would make the lot of 2026-01-02 ET hold −10 shares on 2026-01-03. Reverse the sell of 2026-01-03 first, then use Reverse and re-enter with the reversal dated on or after that reversal."*, "Not saved. Nothing was changed. The reversal, dated 2026-01-02, is applied before the sell of 2026-01-03 and would make the lot of 2026-01-02 ET hold −10 shares. Use Change to pick a reversal date of 2026-01-03 or later (the date the sell was reversed)."*, "Not saved. Nothing was changed. The lot of 2026-03-15 ET was bought after the sell date."* (frame F placeholders, D9).
- New, asked in step 5: the other refusals in the pair form (the new entry's own rules named as part 2), the reversal date refused as earlier than the original on the review, and the TalkBack sentences.

## 12. Status and notes
Done. Built 2026-11-02 (steps 1 to 6, one fix and one Deploy fix); phone checks 2026-11-02, all yes after the fix (section 14, items 11 and 12); pull request #112 merged as bcd5e3b (2026-11-02), CI green on the pull request (3 checks) and on main (2m 59s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: 422b17e fix, ddde6ce step 6, 2515270 step 5, cb3dee3 step 4, 6e47c1c step 3, c87b1c5 step 2 (decision 5), 42b6ea9 step 2, e6c17cd step 1 with this file. main was 8d2769f.

### 13.2 Local run
On 422b17e's code (2026-11-02, all with --rerun-tasks): spotlessCheck green; debug build green, no compiler warnings printed; 563 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 10, Reentries 20, Reversals 14, Sells 19, Splits 23, TickerCheck 16, Transactions 18, RowMapping 13, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, ReenterFlow 14, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint: the 4 listed findings, none silenced (AndroidGradlePluginVersion, open item 31; NewerVersionAvailable for kotlinx-coroutines-test and for sqlcipher-android, open item 43; TypographyDashes on date_not_a_date, strings.xml:86), report written 15:05:21. Release build on the same code green (R8 ran; unsigned; "Unable to strip" names libandroidx.graphics.path.so and libsqlcipher.so, as known). Debug APK versionCode 15 (aapt2 36.0.0). No schema change (still 5.json).

### 13.3 Phone results (Android 17, debug build versionCode 15, your reports)
Checks 1 to 10 all yes. Each check's figures were worked out from the data on the phone (your photo of AAPL's Transactions tab) before it was given; where my figures were wrong, the note says so.
- 1: installed over the 13b build (lastUpdateTime 2026-11-02 12:17:52, after the fix 13:20:28; firstInstallTime 2026-10-29 10:44:04 unchanged); net worth $4,031.87, cash $2,981.87, AAPL 9.33333333 shares at $1,050.00, lots 8 and 1.33333333 at $112.50 (AC-35).
- 2: Reverse and re-enter on a buy and a sell, not on a split, a reversal or a reversed entry; the 2026-10-31 buy of 1 share for $150.00 opened "< Buy (replacing)" with the banner, quantity 2 (the 2 for 1 entered after it that day applied; section 14, item 11) with "Original: 1", price $75.00; a changed quantity kept the hint; "<" saved nothing.
- 3: that buy re-entered as 3 for $150.00: the review opened with the reversal-date refusal (frame F, the third message: −0.66666667 shares at the 2026-11-01 sell, a date of 2026-11-01 or later), as decision 3 has it. My figures had missed that the 2026-11-01 sell, since reversed, took this lot's 0.66666667. Confirm refused; Change to 2026-11-01 removed the message (1.33333333 shares removed, +$150.00, combined $0.00); saved with the new buy dated 2026-10-31, before its reversal (AC-54's case); "Reversed on 2026-11-01 ET >"; AAPL 10 shares at $1,050.00, lots 8 at $112.50 and 2 at $75.00; cash and net worth unchanged.
- 4: both or neither: the new 3-share buy re-entered with a total of $4,000.00: "Not saved. Nothing was changed. With the replacement, cash would be negative on 2026-10-31 by $1,168.13."; nothing saved.
- 5: the 2026-10-31 sell of 3 for $480.00 re-entered at $540.00: quantity 6 with "Original: 3", the lots as after the reversal (8 left at $75.00 picked 6; 3 left at $50.00), "(the original's picks)"; review: 6 shares put back, −$30.00 removed, −$480.00; new sell +$90.00 realized, cost $450.00, +$540.00; combined +$60.00; saved; cash $3,041.87, net worth $4,091.87.
- 6: the 2026-10-31 buy of 10 refused on the entry screen: "… hold −8 shares on 2026-10-31. Reverse the sell of 2026-10-31 first, …"; nothing saved.
- 7: online, the new buy with JNJ ran the ticker check and opened the review; offline, the new buy with COST was refused with the offline message and no look-up; offline, the new sell opened its review with no check (AC-60(b) in part).
- 8: largest font, portrait and landscape, light and dark: the entry screens, the review and the inline date field with the keyboard readable, the gain green, the message red; TalkBack through a whole pair (the new 3-share buy re-entered unchanged and saved; nothing moved), read as wording 3.
- 9: closed and reopened: the pairs, their links and cash lines kept. My check said "<" on the cash line's detail, which has 05's "Back" (13a); the app was right.
- 10: Logcat cleared at 15:00:33, then a pair (the 3-share buy re-entered unchanged); the app's uid 10454 had 24 lines, 1 the log header and 23 D/WindowOnBackDispatcher; no ticker, portfolio name, amount, quantity, date, ratio, reversal or replacing word, or key; 13 "$ and digit" matches were all inner-class names (after a letter or digit). Counts only. Afterwards: AAPL 10 shares at $1,050.00, cash $3,041.87, net worth $4,091.87.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); a failed pair save (open item 14: not provoked; atomic by Room's @Transaction, as the other saves); on the phone, a reversal part refused for cash before the new entry's date and a sell pair judged as one point on a shared date (decision 5), unit tested only; contrast measurement (by eye on the phone); network traffic (open item 39); the screens in a JVM test (none in the project); a split's and a cash entry's Reverse and re-enter (14b); 60(b)'s restore, hidden and Realized gains parts (34, 15, 26).

## 14. Departures from the plan
1. The pair is in a new core file, Reentries.kt, not in Reversals.kt, which only made sharesRefusal internal; 14b extends it (step 2).
2. Sells.kt is unchanged: the lot list as after the reversal is Sells.lotChoices with the drafted reversal included (step 2).
3. RecordStore.savePair moved from step 2 to step 3, since adding it to the interface would not build without the data layer.
4. In steps 2 and 4 the tests were written before the code but not run red first; they passed on their first run. Decision 5 and the fix (item 11) were shown failing first.
5. Decision 5 was asked after step 2; it added an optional set of entries not judged on their own to CashRules.replay (OpeningBalance.kt, not in section 4's list). A new sell's own replay runs without the reversal's line, as a sell only adds cash; the pair's replay judges both.
6. A refusal that a reversal date typed with Change brings shows on the review, since only that date can change there (my reading of decision 3; step 4).
7. Back, a bottom tab or a save from a replacing entry clears the filled-in text, so a plain buy or sell typed earlier and not saved is not kept (one form for both; step 4).
8. Wording 1 is three strings and a helper that turns "Not saved." into "Not saved. Nothing was changed." in front of the 09 and 11 messages, not about 13 strings; the wording is as approved (step 5).
9. The new sell's lots on the review are one row, "Lots used 6 shares", as frame D shows, not the per-lot rows of the sell review (step 5).
10. replaceStartingPicks was added to the state in step 5 for "(the original's picks)".
11. Defect found while working out phone check 2's figures, before the check was given: the new entry goes after every entry of the original's date, so a split entered later that day applies to it; the picks carried that split but the quantity did not, so a re-entered sell opened refused (pick 6 against quantity 3) and an unchanged buy would have saved half its shares. Fixed in 422b17e (your decision B): the first quantity carries the splits since the original, as the picks do, and a sell's is its picks' sum; failing tests first at core (2) and ViewModel (2); reinstalled at versionCode 15. Limit: a buy's quantity is typed to 8 places, so after a 1 for 3 an unchanged pair can leave its lot short by less than 0.00000001 (open item 46).
12. Deploy finding 1 (Medium), fixed before the pull request (your decision A): an unfinished reversal date typed with Change and kept through Back to edit opened the review with the date as a plain row, so Confirm refused with no visible reason. The review now opens with the date field whenever the date is not whole, and a date refusal on Confirm opens it too; failing ViewModel test first. Findings 2 (more work per redraw on the replacing sell screen; open item 45) and 3 (open item 46; items 6 and 7 above) need no change.
