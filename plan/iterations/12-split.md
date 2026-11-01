# plan/iterations/12-split.md

Stage: Build. Status: approved 2026-11-01, built and checked 2026-11-01 (section 12). Decisions 1 to 4 settled (section 10).
Source: plan/plan.md section 4 (row 12), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
In Holding detail I tap Split, beside Buy and Sell. The Split screen has the portfolio and ticker already chosen, today's date, and the ratio as two whole numbers ("2 for 1"). Review shows the shares held at that point before and after ("6 → 12"), says total cost does not change and that there is no cash effect; Confirm saves the split and returns to the holding. Every lot of that ticker in that portfolio that comes before the split has its shares multiplied and its cost per share divided; the lot cost and the cash do not change. The split shows in the holding's Transactions. A buy dated before a split counts split-adjusted; a buy on the same date entered after it does not. After a split such as 1 for 3 a lot can hold a quantity with more than 8 places, and a sell can still empty it exactly. Quantities read "1 share" and "2 shares" (open item 41). A split that breaks a rule is refused with the reason, and what I typed stays.

## 2. Spec refs
- spec 4.5 (entered by hand: date, ticker, ratio; applies to every lot of that ticker in that portfolio that comes before it in the order of 4.1; total cost per lot unchanged), 4.1 (order; the replay of shares per lot and per holding from the split's date on), 4.3 (a buy before a split is entered as traded and split-adjusted; a buy after it on the same date is in post-split shares), 4.4 (a lot's remainder for a pick in the units of the sell date; the lot picker's quantity and cost per share include the splits recorded so far, my reading: the splits up to the sell's place in the 4.1 order; the pick limit at 8 places and the exact remainder after a split; the realized gain per piece after a split), 4.6 (a split never triggers the ticker check), 4.8 (a split leaves holding cost and net worth unchanged), 4.10 (split-adjusted quantity and cost per share are exact fractions; a split multiplies quantity by split_to / split_from and leaves lot cost unchanged; a piece's cost is the lot's cost per share on the sell's date times the exact quantity taken; quantities shown to 8 places).
- spec 3: Transaction (split, ratio), Lot (splits multiply quantity, never change lot cost); no `trade` cash line for a split; C9 (the split history makes a forgotten split easy to spot); C13 (a split's date is a New York market date).
- Example B (split and a forgotten earlier buy), as an automated test.
- Section 6: view 3 (shares with splits; the "share" lines of decision 4); view 6 (Split in the header; splits in Transactions, newest first; quantities and cost per share with splits); "Cash, split and reversal entry" (split entry and its review, Design additions); the date-field note; Number format; Accessibility rule 5; Error states 1 and 4; the navigation outline (Holding detail starts a split).
- Wireframe 10b: frames E and F (markers 10 and 11), and the split message of the error frame.
- Criteria: 53 (the split part: 2 for 1 on Example B's holding shows 10 to 20 on its review and changes no cash), 59(a) without the reversal, 59(c) without the backup, 59(d) with the split, 48 (the Split button), and 9, 35, 39 (TalkBack through a split included), 40, 42, 38 (the log during a split).
- spec 14: K1 (split rules and the replay), K2 (store), K3 (split-adjusted lots and holdings), K5 (the flow), K9.
- Open item 41: singular and plural "share" in every quantity line and its TalkBack sentence.

## 3. Out of scope
- Reversing a split, and Example F (13); criterion 59(a)'s reversal part (13).
- The split history on the Classification tab (23, as 10 decision 1), and criterion 48's split-history part with the sample file (23, 34); until then the holding's Transactions tab lists its splits.
- Transaction history with its Split chip and split rows (view 8, criterion 50; 27).
- The `split` row of the backup file, its ids and its checks (section 8), and 59(c)'s backup part (31, 33, 34).
- Opening a split from a Transactions row (13).
- Prices (16): lots still show "no price".
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
Changed, core/: Transactions.kt (SPLIT with split_to and split_from); Holdings.kt (lots and holdings with splits applied in the order of 4.1); Sells.kt (a lot's remainder in the units of the sell date, the least it holds on that date or later, with splits); new Splits.kt (the split rules on K1 and the replay; shares before and after for the review); RecordStore.kt if needed; their tests.
Changed, app/: AtsDatabase.kt (version 5, an auto migration adding split_to and split_from to transactions; schema file 5.json); RoomRecordStore.kt; AppViewModel.kt; new SplitScreen.kt (the entry and the review) and SplitFlowTest.kt; HoldingDetailScreen.kt (Split in the header, split rows); AppContent.kt; the quantity lines of BuyScreen.kt, SellScreen.kt, HoldingDetailScreen.kt and PortfolioScreen.kt (open item 41); strings.xml; app/build.gradle.kts (versionCode 12); RowMappingTest.kt; the fake store.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No new library and no new network call.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-12-split from main (dde7568). This file and the plan.md lines (with the #105 merge). Baseline run.
2. core/: tests first, then the code. Example B (2 for 1 on 10 shares: 20 at $50.00, cost $1,000.00; the forgotten Feb 15 buy of 5 for $500.00 counts as 10 at $50.00); a buy on the split date entered after it stays as traded; a split dated before a lot's buy does not touch it; another portfolio or ticker untouched; 59(a) without the reversal (1 for 3 on 10 shares for $1,000.00: 10/3 shares shown as 3.33333333, cost $1,000.00, cost per share exactly $300.00); 59(c) without the backup (a pick of 3.33333333 takes the exact 10/3, the lot ends at exactly 0, realized −$600.00 for a $400.00 total); 59(d) with the split (Sell all on two such lots: 6.66666666, both at exactly 0); a sell after a split is in post-split shares, and its allocation is stored in the units of its date; a backdated split before a sell: its lot's remainder for a later pick in the right units, and a reverse split that would leave a later sell taking more than its lot holds is refused, naming the date; the ratio rules (whole numbers above 0, not equal, decision 3); the ticker must have a buy in that portfolio; not before the opening balance; the shares before and after for the review.
3. Data layer: version 5, the DAO, RoomRecordStore and the fake; a split row with its two numbers; row-mapping tests (a split without its numbers, or a buy or sell with them, is unreadable data).
4. ViewModel: tests first (Split from the holding with its portfolio and ticker chosen; the chips, decision 1; the ratio and date fields; Review shows every refusal together, K1 with stand-ins; the review's before and after; Confirm checks again, saves and returns to the holding; "<" and Back return to the holding; a refusal keeps the text), then the code.
5. Wording questions (the Split screen and its review, the split row, the refusals, the "share" and "shares" rule of decision 4), then the screens and the strings, with the quantity lines of Buy, Sell, Holding detail and the portfolio screen moved to the rule.
6. versionCode 12. Full run, release build, install over 11's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- Lots show "no price" until 16.
- Transactions rows, the split row included, open nothing until 13.
- No split history on Classification until 23.

## 7. Risks
- The replay with splits (4.1): a split dated before existing sells changes what each later sell's stored quantity means (quantities are as traded on their date, spec 3). A reverse split can make a saved sell take more than its lot holds; the split must then be refused. This is the hardest rule in 12.
- Units: a lot's remainder for a pick is in the units of the sell date (4.4), while allocations of later-dated sells are in their own dates' units. The figure is the least the lot holds on the sell date or any later date, converted through the splits between.
- Exact fractions after splits (4.10): quantities with more than 8 places, shown rounded to 8 and never used rounded; the pick limit and the exact remainder (built in 11, first seen on the phone here).
- Schema version 5 adds two columns to an existing table (the first change to a table that holds data); install over 11 (criterion 35).
- Holding detail's header gets a third button; at the largest font and in landscape it must wrap, not cut (criterion 39; the CLAUDE.md pitfall from 10).
- The "share" and "shares" rule touches strings approved in 09 to 11; each changed line is shown again (D9).

## 8. Proof
Commands: as in 11, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 11's data (AAPL in Main: 6 shares, lot 1 with 5 left and lot 2 with 1, both bought 2026-10-31, $150.00 per share, cost $900.00; SPY at 0 shares). Each check names its start (CLAUDE.md pitfall from 11).
1. Install over 11's build (versionCode 11): the data, the sells, the lots and both portfolios are still there (criterion 35).
2. Portfolios → Main → AAPL: the header shows Buy, Sell and Split; Split opens the Split screen with Main and AAPL chosen and today's date; "<" and Back return to AAPL.
3. Refusals on the Split screen, text kept: 1 for 1, 0 for 1, an empty number, 1.5, a date before the opening balance.
4. A 2 for 1 split of AAPL dated 2026-10-31 (it comes after the sells of that date in entry order, so it applies to the lots' remainders): the review shows "6 → 12" and no cash effect; Confirm returns to AAPL with 12 shares and cost $900.00; Transactions lists the split first; Lots shows lot 1 with 10 left at $75.00 per share and lot 2 with 2 left; the Cash tab has no new line (criterion 53).
5. A forgotten buy (Example B): from AAPL's header, Buy 1 AAPL dated 2026-10-30 for $150.00 (the date is checked against the opening balance's date before the check is given): AAPL shows 14 shares; the new lot is listed first with 2 left at $75.00 per share.
6. A 1 for 3 split of AAPL dated today: the lots show 0.66666667, 3.33333333 and 0.66666667, oldest first; cost unchanged; Sell all from AAPL's header picks all three, the quantity reads 4.66666667, and after Confirm AAPL has exactly 0 shares and Lots says "No lots with shares" (59(c), 59(d)).
7. "1 share" and "2 shares" on the lot lines, the picked line and the refusals (open item 41).
8. Largest font and landscape, light and dark, TalkBack through a whole split (criteria 39 to 41).
9. Close and reopen the app: the splits, the lots and the holdings are kept.
10. Logcat during a split (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-11-01)
1. The Split screen opens from Holding detail only (navigation outline), with that holding's portfolio and ticker chosen, as portfolio chips and chips of the tickers with a buy in that portfolio, like Sell's (11 decision 1); the spec's "picked from the tickers with a buy" holds (option A).
2. A split when the holding has 0 shares at that point (all sold, or dated before the first buy) is allowed, as 4.5 sets no such rule; the review shows "0 → 0" (option A).
3. Each ratio number is a whole number from 1 to 1,000,000; a longer number is refused with a message (option A).
4. "share" only when the quantity as shown is exactly "1", otherwise "shares" ("0.5 shares", "1.5 shares"), in every quantity line and TalkBack sentence of Buy, Sell, Holding detail, the portfolio screen and Split; Android's plural strings take whole numbers only, so one function decides from the shown text (open item 41; option A).
Also settled: Split stays a header button as view 6 and criterion 48 say; moving it into a ⋮ menu is open item 42 (your decision A).
Settled by the spec, no question asked: a split has no cash line and never triggers the ticker check (4.6); the ratio is "to for from" as wireframe 10b and the file columns; the date is digits only like Buy's and Sell's (09 decision 10); splits show in the holding's Transactions tab, newest first, and the split history on Classification waits for 23.

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Split"*, "Date"*, "Enter date in US market time (New York)"*, "for"*, "Whole numbers. 2 for 1 doubles the shares; 1 for 10 is a reverse split."*, "Review"*.
- "Review split"*, "Portfolio"*, "Ticker"*, "Split"* "2 for 1"*, "Shares at this point"* "10 → 20"*, "Total cost does not change. No cash effect."*, "Nothing is saved until you confirm."*, "Confirm and save"*, "Back to edit"*.
- "Split: whole numbers above 0, and the two cannot be equal."*
- New, asked in step 5: the split row in Transactions; the refusal for a split that would leave a later sell short; the limit message of decision 3; the TalkBack sentences for the ratio fields, the review's before and after, and the split row; the "share" lines of decision 4.

## 12. Status and notes
Done. Built 2026-11-01 (steps 1 to 6 and one fix); phone checks 2026-11-01, all yes after the fix (section 14, item 1); pull request #106 merged as 08198bd (2026-11-01), CI green on the pull request (3 checks) and on main (2m 51s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: 7929ba9 fix, 4136330 step 6, c8481c0 step 5, 807d4bf step 4, 144e6e4 step 3, 0e8183d step 2, f18df62 step 1 with this file. main was dde7568.

### 13.2 Local run
On 7929ba9's code (2026-11-01, all with --rerun-tasks): spotlessCheck green; debug build green, no compiler warnings printed; 495 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 10, Sells 19, Splits 23, TickerCheck 16, Transactions 18, RowMapping 8, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, SellFlow 21, SplitFlow 17); lint: the 4 listed findings, none silenced (AndroidGradlePluginVersion, open item 31; NewerVersionAvailable for kotlinx-coroutines-test and for sqlcipher-android, open item 43; TypographyDashes on date_not_a_date, strings.xml:86), report written 16:39:45. Release build on the same code green (R8 ran; unsigned; "Unable to strip" names libandroidx.graphics.path.so and libsqlcipher.so, as known). Debug APK versionCode 12 (aapt2). Room's migration from 4 to 5 is two ALTER TABLE ... ADD COLUMN statements (generated code read in step 3).

### 13.3 Phone results (Android 17, debug build versionCode 12, your reports)
Checks 1 to 10 all yes; 6 after the fix. Notes:
- 1: installed over the 11 build (lastUpdateTime 15:07:33, firstInstallTime 2026-10-29 10:44:04 unchanged); the cash lines, the sells, both portfolios, AAPL at 6 shares and $900.00 with its two lots as before, so the migration to version 5 ran (criterion 35); Buy, Sell and Split in AAPL's header. The opening balance is dated 2026-01-01.
- 2: Split from AAPL's header chose Main and AAPL, dated today, with the two fields, "for" and the hint; 投資信託 showed its line or chips; "<" and Back returned to AAPL.
- 3: refusals with the text kept and nothing saved: empty, 1 for 1, 0 for 1, 1.5 for 1 (the wireframe's line), 2000000 for 1 (the limit line), a date before the opening balance, and a bad number beside that date (both lines together).
- 4: 2 for 1 dated 2026-10-31: the review showed 6 and 12 with no cash effect; AAPL 12 shares, cost $900.00 unchanged, the split row first, lots of 10 and 2 at $75.00 per share; no cash line; net worth $4,041.87 unchanged.
- 5: a forgotten buy of 1 AAPL dated 2026-10-30 for $150.00 counted as 2 shares at $75.00 (Example B on the phone): AAPL 14 shares, cost $1,050.00, the new lot first, the row reading "1 share"; net worth unchanged.
- 6: 1 for 3 dated today: 14 to 4.66666667; lots 0.66666667, 3.33333333 and 0.66666667 at $225.00 per share. Sell all for $1,200.00 failed on the first build (section 14, item 1). After the fix (installed 16:20:55): proceeds $171.43, $857.14 and $171.43, gains +$21.43, +$107.14 and +$21.43 in green, realized +$150.00; AAPL at exactly 0 with "No lots with shares"; net worth $4,191.87.
- 7: after a buy of 1 AAPL: "1 share" on the row and the transaction, "Picked 0 shares", "Picked 1 share", "Picked 1 of 1 share", "Picked 1 of 2 shares", and the refusals "holds 1 share", "has 1 share left", "add up to 1 share" (open item 41).
- 8: largest font, portrait and landscape, light and dark: the three header buttons in full, the Split screen and its review scroll as one, the messages readable; TalkBack read "Split AAPL in Main", the two ratio fields, the review's two lines and the split row as section 11; a 2 for 1 split was saved with it (AAPL 2 shares).
- 9: closed from Recents and reopened: the splits, the buys, the sell, the lot and net worth kept.
- 10: Logcat cleared at 16:37:28, then a split of 1 for 2; the app's uid 10454 had 46 lines, all from system tags (WindowOnBackDispatcher, ImeTracker, InsetsController, VRI[MainActivity] and single lines). No ticker, portfolio name, amount, quantity or split word, ratio, key or token. Counts only.
- The phone now holds AAPL in Main at 1 share (one lot at $150.00), four splits, and net worth $4,191.87.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); a failed save (open item 14: not provoked); contrast measurement (by eye on the phone); network traffic (open item 39); a split refused for a later sell left short, on the phone (unit tested only); a leftover too small to show (open item 44); the "share" rule in a JVM test (it sits in a composable; phone check 7 is the proof); Compose UI tests (none in the project).

## 14. Departures from the plan
1. Defect found in phone check 6: Sell all after the 1 for 3 split was refused as more than held. Sell all's picks are the remainders rounded to 8 places, two of them rounded up (0.66666667), so their sum (4.66666667) was above the exact holding (14/3), while each pick still takes its lot's exact remainder (4.4, 4.10). In 11 no remainder had more than 8 places, and 59(d)'s lots of 10/3 round down. Options weighed with you on cases P1 to P5 (the phone's lots; 5/3; three lots of 2/3; four lots of 3/7; two lots of 1/7) and a leftover of 1/150000000: the exact comparison, a check of the allocations after the picks, and a comparison with the holding shown at 8 places each fail one of them; rounding the picker's figures down would change 4.4 and 4.10. Fixed on your decision (A): "more than held" compares with the sum of the lots' remainders at 8 places, the figures the picker shows and a pick may reach (7929ba9). My reading of 4.4: "more than held" is judged on the same per-lot figures the lot picker, the pick limit and the restore replay use. Failing tests first: P1 to P5 and the leftover in core (6), and P1 to P5 through the ViewModel (5), each seen failing with the old line before the fix was restored.
2. The split review shows "Shares at this point" and "Shares after split" on two lines, not the wireframe's "6 → 12" (your wording 2, option A).
3. The "more than held" message names what the picks can take; after a split with remainders of more than 8 places it can differ from the holding shown in the header by a few hundred-millionths (for example "holds 2.00000001 shares" beside a header of 2). The approved wording is kept (your decision A in step 8).
4. A lot left with less than 0.000000005 share after a reverse split shows "0 left" and cannot be picked, so its holding cannot reach exactly 0 (needed for Hide, 15). A spec gap in 4.4; logged as open item 44 with options S0, S3, S4 and S5, to be settled before 15 (your decision A in step 8). Not reachable in 12's checks.
5. Section 2 gained 4.4's lot-picker line (with my reading of "so far": the splits up to the sell's place in the 4.1 order), 4.8, C13 and view 3, and section 3 gained view 8, criterion 50 and criterion 48's split-history part, before step 4 (your decision A).
6. core/ gained an internal place in the 4.1 order and Lot gained its split factor (step 2); the lot functions work out the split factors on each call, which grows with the square of the number of transactions: a known limit for one person's records, not optimised.
7. Sell's Message composable and the "share" function are shared with Split (step 5); sharesText gained a form for a quantity as typed.
8. SplitFlowTest's store gives a split no cash line, as K1 does (fix step).
