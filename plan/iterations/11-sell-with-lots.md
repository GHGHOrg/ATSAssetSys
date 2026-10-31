# plan/iterations/11-sell-with-lots.md

Stage: Build. Status: approved 2026-10-31, not built. Decisions 1 to 3 settled (section 10).
Source: plan/plan.md section 4 (row 11), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
I tap Buy/Sell and then Sell, pick the portfolio and a ticker it holds, type date, quantity and total (zero allowed), and see the lots oldest first. I tap a lot to pick it with its full remainder, edit the number for a partial lot, or use Sell all; "Picked X of Y shares" keeps count. Review checks everything and shows the lots used, their cost, the realized gain/loss and the cash effect; Confirm saves the sell. Cash shows a "Sell AAPL" line, the holding's shares and cost go down, and Holding detail lists the sell. A sold-out holding stays in its portfolio at 0 shares. Sell is also in Holding detail's header. A sell that breaks a rule is refused with the reason, and what I typed stays.

## 2. Spec refs
- spec 4.1 (replay; shares checked per lot and per holding), 4.2 (cash in on the trade date), 4.4 (input, lot rules, a lot's remainder for a pick, the pick limit at 8 places and the exact remainder, Sell all, selling more than held, a total of zero, realized gain per piece, a holding at 0 shares stays visible; Hide excluded), 4.6 (a sell never triggers the ticker check), 4.8 (cost is the remaining lot cost; only holdings with shares are valued), 4.10 (exact proportional cost and proceeds; allocations store the exact quantity taken; largest remainder for the lots used on the review).
- spec 3: Transaction (sell), Sell allocation, the `trade` cash line (one per sell) and C4.
- Example A, as an automated test.
- Section 6: view 3 (a sold-out holding stays and counts); view 6 (Sell in the header, sells in Transactions, "No lots with shares"); view 7 (the sell's trade line); Buy and sell entry; the date-field note; the Targets line; Number format and color; Accessibility rule 5; Error, empty and loading states 1 and 4; the navigation outline (the Buy/Sell button, Holding detail).
- Wireframe 10a: frames C to G, markers 8 to 17.
- Criteria: 3, 13, 14, 24 (the Sell part), 29 (the trade line's text), 30, 31, 41 (the realized part), 45 (the 0-share part), 48 (the Sell parts), 52, 59(b), 59(d) without the split, 60(d) (the K1 part), 38 (the log during a sell), and 9, 35, 39 (TalkBack through a sell included), 40, 42.
- spec 14: K1 (rules), K2 (store), K3 (lots, holdings, realized gain of a sell), K5 (the flow), K9.

## 3. Out of scope
- Prices and unrealized gain/loss (16): every lot shows "no price"; the stale format is not reachable.
- Splits (12): criteria 59(a) and (c), and the split part of 59(d). The exact-remainder rule is built and tested in core/ with a hand-made fraction; the phone first sees it in 12.
- Reversal of a sell (13), Reverse and re-enter (14), Hide and criterion 60(a) (15).
- The Realized gains screen (26) and criteria 11 and 23; in 11 the realized gain shows on the sell review only.
- Opening a transaction from a Transactions row or a trade line (13).
- The `sell` row of the backup file (section 8), criterion 25 and the restore half of 60(d) (31, 33, 34).
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
New, core/: Sells.kt (the sell rules on K1, a lot's remainder for a pick, the pick limit, Sell all, the realized gain and the largest-remainder split); its tests.
Changed, core/: Fraction.kt (the stored text form and its strict parser, decision 3); Transactions.kt (SELL); Holdings.kt (lots reduced by allocations); Valuation.kt; RecordStore.kt (loading allocations, saving a sell); Money.kt if needed; their tests.
New, app/: SellScreen.kt (the entry and the review); SellFlowTest.kt.
Changed, app/: AtsDatabase.kt (version 4, an auto migration adding the sell_allocations table; schema file 4.json); RoomRecordStore.kt (the sell, its cash line and its allocations written as one action); AppViewModel.kt; BuySellMenu.kt (the stub line goes); AppContent.kt; HoldingDetailScreen.kt; CashScreen.kt; PortfolioScreen.kt; PortfoliosScreen.kt if needed; the theme file (green and red for gains, light and dark); strings.xml; app/build.gradle.kts (versionCode 11); RowMappingTest.kt; the fake store.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No new library and no new network call.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-11-sell-with-lots from main (385f3fb). This file and the plan.md lines (with the #103 merge). Baseline run.
2. core/: tests first, then the code. Example A (cost $1,300, realized +$800, cash $9,600, lot 2 at 8 shares and $1,200); the lot rules (another portfolio, bought after the sell date, same date entered later); a pick over the lot's remainder at 8 places refused, a pick equal to it taking the exact remainder (a hand-made 10/3 lot); picks that do not add up refused, naming both numbers; selling more than held refused; a total of 0 allowed, a quantity of 0 or a ninth decimal refused; the cash replay with a future-dated sell; 59(b) (proceeds $33.34, $33.33, $33.33; gains +$23.34, +$23.33, +$23.33; total +$70.00); 59(d) without the split (Sell all's quantity is the sum of its picks; the lots end at exactly 0); 60(d) (a backdated sell does not offer L1 and offers L2 with 10); 4.8 (a holding at 0 shares adds nothing to net worth or the portfolio value); the fraction text form and its parser (round trip, large numbers, each refused form).
3. Data layer: version 4, the DAO, RoomRecordStore and the fake; the sell, its cash line and its allocations written as one action; row-mapping tests.
4. ViewModel: tests first (Sell from the menu; "Nothing to sell yet" when no holding has shares; the portfolio and ticker chips; the lot list; a tap picks and unpicks a lot; a partial quantity; Sell all; a date change updates the list and drops picks that no longer pass, with a line; Review shows every refusal together (K1 with stand-ins); Confirm checks again and saves; a refusal keeps the text; Sell from the holding fills in the portfolio and ticker and returns to the holding; "Nothing to sell" at 0 shares; a sold-out holding stays and counts), then the code.
5. Wording questions, then the screens, the gain colors and the strings.
6. versionCode 11. Full run, release build, install over 10's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- Lots show "no price" in the picker and on the Lots tab until 16.
- The realized gain shows on the sell review only, until 26.
- Transactions rows and trade lines open nothing until 13.

## 7. Risks
- The first write of three tables at once (the sell, its cash line, its allocations): all or nothing. A failed write cannot be provoked (open item 14 stays open).
- A lot's remainder for a pick is the least it holds on the sell date or any later date (4.4, 60(d)); a backdated sell is the hard case.
- The pick limit compares at 8 places, while the lot gives up its exact remainder (4.10). Before 12 every remainder has at most 8 places, so that path is tested in core/ only.
- Largest remainder for the lots used on the review (4.10): the first place where shown parts must add up to a shown total.
- The first gain colors (green with a plus, red with a minus): each needs a light and a dark value that meets contrast (criteria 40, 41); color is never the only signal.
- The Sell screen is long (fields, lots, Review): at the largest font and in landscape it must scroll as one (criterion 39; CLAUDE.md pitfall from 10).
- Schema version 4 adds one table and touches no existing one; install over 10 (criterion 35).

## 8. Proof
Commands: as in 10, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 10's data (AAPL in Main: lots of 10 and 1 share, both bought 2026-10-31; SPY):
1. Install over 10's build (versionCode 10): the data, the buys and both portfolios are still there (criterion 35).
2. The Buy/Sell menu's Sell opens the Sell screen; the stub line is gone (criterion 13: two taps from Overall).
3. Sell 3 AAPL in Main for $480.00, today: the lot list shows two lots, oldest first, same date in entry order, each with "no price"; tap lot 1 and edit it to 3, "Picked 3 of 3 shares"; Review shows lots used 2026-10-31 ET, 3 shares, cost $450.00, realized +$30.00 in green, cash +$480.00; Confirm. Cash shows "Sell AAPL" with the balance after it; the holding shows 8 shares and cost $1,200.00; Transactions lists the sell first; Lots shows lot 1 with 7 left. Roughly timed: under 30 seconds (criterion 3).
4. Date 2026-10-30: both lots greyed out with "bought after the sell date"; picks dropped with a line.
5. Mismatch: picks of 2 for a sell of 3 give a message naming both numbers; nothing is saved.
6. Refusals, text kept: more than held, a pick over a lot's remainder, a ninth decimal, quantity 0, a date before the opening balance.
7. Sell all of SPY with total $0.00: the realized loss shows in red with its sign; SPY stays on its portfolio's screen at 0 shares; its Lots tab says "No lots with shares"; the Portfolios row still counts it, at $0.00 if it is the only holding; Sell in its header says "Nothing to sell".
8. Sell from AAPL's header: Main and AAPL are chosen; Back and a saved sell return to the holding.
9. Largest font and landscape, light and dark, TalkBack through a whole sell with the lot picker (criteria 39 to 41).
10. Close and reopen the app: the sells, the lots and the cash lines are kept.
11. Logcat during a sell (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-10-31)
1. The ticker on Sell is one chip per ticker held in the chosen portfolio, in ticker order; the first is chosen when Sell opens from the menu, and a single ticker shows already chosen. Not the wireframe's dropdown, which opens a separate window (CLAUDE.md pitfall from 07; as 09 decision 11) (option A).
2. Sell sits beside Buy in Holding detail's header. It opens the Sell screen with the holding's portfolio and ticker chosen; "<", Back and a saved sell return to the holding; at 0 shares it stays and says "Nothing to sell" when tapped (option A).
3. A sell allocation's exact quantity is stored as a reduced fraction in one text column ("numerator/denominator", the denominator above 0, the numerator above 0, such as 10/1, 5/2 or 10/3), written by one function in core/ and read by one strict parser that refuses any other form; table sell_allocations, key (sell_id, lot_id), schema version 4. Splits (12) then need no change to stored data (option A).
Settled by the spec, no question asked: the portfolio opens on the one you came from, otherwise the first; a sold-out holding stays in the list and counts (view 3, criterion 45). Following 09: the sell date is digits only like Buy's (09 decision 10); the pick fields use the decimal keyboard.

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Sell"*, "Ticker (held)"*, "Date"*, "Enter date in US market time (New York)"*, "Quantity"*, "Total amount, zero allowed"*, "Price per share: $175.00"*, "Sell all"*, "Review"*.
- "Lots, oldest first"*, "2026-01-02 ET"*, "10 left, $100.00 per share"*, "Sell: 10"*, "no price"*, "Picked 12 of 12 shares"*, "Not available: bought after the sell date"*.
- "Review sell"*, "Lots used"*, "2026-01-02 ET, 10 shares"*, "cost $1,000.00"*, "Cost of lots used"*, "Realized gain/loss"*, "Cash effect"*, "Nothing is saved until you confirm."*, "Confirm and save"*, "Back to edit"*.
- "Not saved. Portfolio P holds 8 shares of AAPL on 2026-04-01; the sell is for 12."*; frame D's placeholder "Not saved yet. The picked lots add up to 10 shares, but the sell is for 12."; "Nothing to sell yet"* (existing).
- New, asked in step 5: the dropped-pick line; the pick-over-remainder refusal; "Nothing to sell" from the header; the line for a portfolio with nothing held; the trade line "Sell AAPL"; the TalkBack sentences for the chips, a lot, a pick, the count line, the review and a realized gain or loss.

## 12. Status and notes
Approved 2026-10-31. Not built.

## 13. Build results
Filled in during the build.

## 14. Departures from the plan
None yet.
