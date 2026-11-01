# plan/iterations/12-split.md

Stage: Build. Status: approved 2026-11-01, not built. Decisions 1 to 4 settled (section 10).
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
Approved 2026-11-01. Not built.

## 13. Build results
Filled in during the build.

## 14. Departures from the plan
None yet.
