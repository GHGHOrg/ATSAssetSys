# plan/iterations/16c-live-prices-overall.md

Stage: Build. Status: approved 2026-11-09 (your "yes", after a check of the code that changed the draft); decisions 1 to 4 settled the same day (section 11). In build: step 1.
Source: plan.md section 4 (row 16c), spec 4.4, 4.8, section 6 (view 1, the buy and sell entry, error and loading states, number format and color, accessibility), section 7, AC-41, AC-42, AC-61, AC-62; wireframe 01 (state A, markers 2, 4 and 5), wireframe 10a (the lot list, markers 12 and 14) and wireframe 10c (Reverse and re-enter of a sell); 16a and 16b (their decisions and wording); open items 48 and 51. "My reading" is an interpretation, not a test.

## 1. Outcome
On Overall you see net worth with the age of its prices (the oldest price used), each portfolio with its value and gain/loss, an "updating" indicator while a refresh runs, and one quiet line when the last refresh failed; pulling down refreshes. On the sell screen each lot shows its unrealized gain/loss at the latest price, with the stale or "no price" flag, and the refresh lines beside the lot list.

## 2. Spec refs
- 4.8: net worth at market; the age line beside net worth is the age of the oldest price used, a holding at cost not counted; stale and "no price".
- 4.4 and wireframe 10a marker 12: each lot in the lot list shows its unrealized gain/loss (green with plus, red with minus); marker 14: "no price" instead of a gain, and a stale price's gain with the stale marker and its age; a lot can still be picked either way.
- Wireframe 10c: the lot list of Reverse and re-enter of a sell is the same list, shown as it will be after the reversal.
- Section 6, view 1 and wireframe 01: marker 2, net worth with the age of the prices and an "updating" indicator while a refresh runs (decision 1); marker 4, each portfolio with its value and gain/loss, a tap opens it; marker 5, one quiet line when a refresh failed, no pop-up.
- Section 6, error and loading states (2) and (3), also on the sell screen, which shows prices from 16c; accessibility (2) and (5); number format and color (unrealized gain/loss in the lot picker and on Overall).
- AC-41 (net worth $10,880.00 at $160, already right since 16a; a gain green with plus), AC-42, AC-61 (d), AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The allocation summary on Overall (iteration 18) and Net Worth (Total) (17): tapping net worth opens nothing new yet.
- The rest of the sell entry screen and the sell review (decision 3; open item 48 keeps them).
- Open item 51 (the models as kept fields): measure first.
- An age such as "9 h old" (open item 49).

## 4. Analysis (2026-11-09, by reading the code and the spec)
- OverallScreen.kt shows net worth (`AppState.netWorthCents`, at prices since 16a decision 1) or the guided steps, and "No portfolios yet"; no portfolio rows, no age line, no refresh lines, no screen model (open item 48 lists Overall).
- `Valuation.oldestPriceTime`, `PricesAsOfModel`, `PriceRefreshModel`, PriceRefreshParts.kt and `PortfolioFigures.of` (with "(all holdings at cost)", 09 wording 4) can be used for Overall as they are.
- `openPortfolio` keeps the tab, so Back from a portfolio opened on Overall returns to Overall.
- SellScreen.kt's lot row shows "no price" in place of every gain ("until 16"), also in its TalkBack sentence (`sell_lot_spoken`). The same lot list serves Reverse and re-enter of a sell (`replacing`), built from the transactions as they will be after the reversal.
- `Sells.lotChoices` gives each lot's available quantity and cost per share in the units of the sell date; the latest price is in today's units. A split between the sell date and today (either way: a sell dated after today is allowed, 4.8) puts them on different bases, so the gain needs the split factor between the two dates, worked out in core (decision 4).
- AppStartTimingTest finds Overall by a TalkBack text containing "Net worth"; net worth's TalkBack sentence keeps that start.
- My reading: Overall's portfolio rows carry no stale mark (view 1, wireframe 01 marker 4); net worth's age line carries ", some stale" as a portfolio's does (16b decision 1).

## 5. Files
- core/: a lot choice's gain at a price, with the split factor from the sell date to today (Sells.kt or Valuation.kt), exact.
- app/: ui/OverallModel.kt (new: net worth, its age and stale state, the portfolio rows; CR2); OverallScreen.kt (draws it; pull-to-refresh, decision 2); the lot rows' model and SellScreen.kt's lot rows, plus "Updating prices" and the failed line beside the lot list (decision 3); AppContent.kt (wiring: a row opens its portfolio); strings.xml (section 12); build.gradle.kts (versionCode 25).
- Tests: core (a lot choice's gain: up, down, under $1, no price, a split after the sell date, a sell dated after today with a split before it); the Overall model's builder (age, some stale, rows, all at cost, no portfolio, no price used); the lot rows' builder (gain, stale with its time, no price, an unavailable lot without a gain, a replaced sell's lot); a ViewModel wiring test each; screen tests (AC-61 (d)).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48).
- No new library, no schema change.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-16c-live-prices-overall, one pull request.
1. This file and the plan.md lines.
2. The models: a lot choice's gain in core, the Overall model and the lot rows' model, tests first, shown red.
3. Overall: net worth with its age line, the portfolio rows, the refresh lines, pull-to-refresh (decisions 1 and 2).
4. The sell screen: each lot's gain or "no price", the stale flag with its time, the lot's TalkBack sentence, "Updating prices" and the failed line beside the lot list; in Sell and in Reverse and re-enter of a sell (decisions 3 and 4).
5. The screen tests (AC-61 (d)).
6. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A split between the sell date and today: the gain must be in one unit (section 4). Tests both ways.
- 2. A sell dated in the past shows each lot's gain at today's price; it may look odd next to an old date.
- 3. Overall works out every portfolio's figures on each draw (open item 51). AppStartTimingTest logs the time from unlock to Overall; step 6 compares it with 16b's run.
- 4. Net worth's TalkBack sentence must keep its "Net worth" start, or AppStartTimingTest times out.
- 5. The lot row gains a line or two; at the largest font it wraps. No screen test covers the sell screen's layout: checked on the phone.
- 6. AC-62: the TalkBack labels of Overall and the lot rows are checked by the screen tests only; the hand check keeps speech, the themes, the network and the install.
- 7. The emulator is not the phone (C19); the screen tests use no network.

## 9. Proof
- Commands: as in 16b, all with --rerun-tasks. The screen tests on the pull request: 16 plus this iteration's new ones.
- Unit tests: steps 2 to 4.
- The phone check, over 16b's build (AC-35; no schema change): Overall's net worth and age line against a portfolio's; a portfolio row's figures and its tap; offline (airplane mode, lock and unlock): ", some stale" and the failed line on Overall and beside the sell screen's lots, no pop-up; "Updating prices" and pull-to-refresh on Overall; the sell screen's lots: for a sell dated today each gain against Holding detail's Lots tab, the stale flag offline, a lot still pickable, then Back without saving; one Reverse and re-enter of a sell opened and left with Back, without saving; largest font and landscape on Overall and the sell screen; TalkBack speech; light and dark; logcat as in 16b.

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-09)
1. While a refresh runs, Overall shows both lines: "Prices as of … ET" stays and "Updating prices" shows under it, as on a portfolio's screen and Holding detail (your A). A departure from wireframe 01 marker 2's "or", recorded here. Not chosen: (B) "Updating prices" in place of the age line.
2. Pulling down on Overall refreshes the prices, as on the other price screens (your A). Not chosen: (B) no pull-to-refresh on Overall.
3. Of the sell entry screen, only the lot list gets a model (each lot row and the refresh lines beside it); the fields, Sell all and the review stay in open item 48 (your A). Not chosen: (B) the whole sell entry screen.
4. A lot's gain in the picker is for the quantity the row shows as left for the sell date, converted to today's shares through any split between the two dates, at the latest price; also in Reverse and re-enter, with the lots as after the reversal (your A). Not chosen: (B) the lot's whole remaining quantity today, as on Holding detail's Lots tab.

## 12. Wording to approve (D9)
To be shown on the phone in step 6:
- Net worth's age line: "Prices as of 2026-11-06 16:00 ET" and "…, some stale" (16b's approved form); its TalkBack sentence keeps "Net worth, … dollars" at its start.
- A lot's stale flag: "stale, as of 2026-11-06 16:00 ET" (16b's approved form), beside its gain.
- A portfolio row on Overall and its TalkBack sentence; a lot row's TalkBack sentence with its gain.
- Reused: "Updating prices", "Price refresh failed. Showing last prices.", "no price", "(all holdings at cost)".

## 13. Status and notes
- Draft 2026-11-09; changed after a check of the code (six findings: the split between the sell date and today, the sell screen's refresh lines, Reverse and re-enter's lot list, AppStartTimingTest's "Net worth" text, "(all holdings at cost)" on Overall's rows, the lot's TalkBack sentence); approved the same day; decisions 1 to 4 settled.

## 14. Departures from the plan
None yet.
