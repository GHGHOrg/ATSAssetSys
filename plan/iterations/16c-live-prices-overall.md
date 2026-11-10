# plan/iterations/16c-live-prices-overall.md

Stage: Build. Status: approved 2026-11-09 (your "yes", after a check of the code that changed the draft); decisions 1 to 4 settled the same day (section 11). Done: built 2026-11-09 (steps 1 to 6); phone check 2026-11-09 all yes (section 13.3); the screen tests 20 of 20 on pull request #146 (section 13.4); the app start timing compared with 16b (section 13.5); reviewed against the spec refs (section 13.6); merged as 2af640a, main green (section 13.7).
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
Approved on the phone 2026-11-09 (your A, check 11), as in strings.xml's "Iteration 16c" block: the heading "Portfolios" on Overall; net worth's TalkBack sentence "Net worth, … dollars, Prices as of … Eastern Time[, some stale]"; a portfolio row's "<name>, … dollars, gain or loss … dollars" (at cost the approved "<name>, all holdings at cost"); a lot's "Lot of … Eastern Time, … left, … dollars per share, unrealized gain/loss … dollars[, stale price as of … Eastern Time], picked/not picked". The shown lines reuse 16a's and 16b's approved wording.
Proposed in step 1, to be shown on the phone in step 6:
- Net worth's age line: "Prices as of 2026-11-06 16:00 ET" and "…, some stale" (16b's approved form); its TalkBack sentence keeps "Net worth, … dollars" at its start.
- A lot's stale flag: "stale, as of 2026-11-06 16:00 ET" (16b's approved form), beside its gain.
- A portfolio row on Overall and its TalkBack sentence; a lot row's TalkBack sentence with its gain.
- Reused: "Updating prices", "Price refresh failed. Showing last prices.", "no price", "(all holdings at cost)".

## 13. Status and notes
- Draft 2026-11-09; changed after a check of the code (six findings: the split between the sell date and today, the sell screen's refresh lines, Reverse and re-enter's lot list, AppStartTimingTest's "Net worth" text, "(all holdings at cost)" on Overall's rows, the lot's TalkBack sentence); approved the same day; decisions 1 to 4 settled.

### 13.1 Steps 1 to 5 (2026-11-09)
- Step 1 (57fc0e5): this file and the plan.md lines. The first full run red in 15c's HoldingsScaleTest (`lotsOf` SPREAD 9.549 ms at 3,000, limit 7.0; 02:32:22); the next, unchanged, green (02:34:05 to 02:35:02).
- Step 2 (602fa2f): core `Sells.unitsToToday` (the splits from the sell's place to the end of today; for a sell dated after today, the inverse of the splits from today to the sell) and `Sells.choiceGain` (available × toToday × price − available × cost per share; null with no price), exact. LotChoiceGainTest 5 tests: today's sell up (+600 at 160), down (−400 at 60) and under $1 (−39,799/40 at 0.5025); no price; a split after the sell date (+200, not −400); a sell after today with the split between (factor 1/2, +200); a split before both (factor 1). App: ui/OverallModel.kt, ui/SellLotsModel.kt, `KeptFields.sellUnitsToToday`, `AppState.overall` and `sellLotRows`, `PortfolioFigures.pricesAsOf` made public. OverallModelTest 4, SellLotsModelTest 6 (with two AppState wiring tests: a split since the sell date; Reverse and re-enter shows the sold lot back at 10 left), PriceRefreshFlowTest +1 (Overall's wiring). Shown red against stubs first: core 5 tests, 4 failed (the no-price case passed on the stub's null); app 21 tests, 11 failed. Before that red run one wrong expected value of mine was corrected (the under-$1 case). One red run fixed: spotlessCheck (an orphaned KDoc: the new functions sat between `lotChoices`' KDoc and `lotChoices`; moved below it). Tests 746 to 762.
- Steps 3 and 4 (0f017fc, one commit, since both change strings.xml; section 14): OverallScreen draws from `state.overall`: the net worth card (net worth, "Prices as of … ET[, some stale]", "Updating prices"), the "Portfolios" heading and one row per portfolio (value, gain with sign and color, or "(all holdings at cost)"; a tap opens it and Back returns to Overall), the failed line under the rows (wireframe 01 marker 5), pull-to-refresh (decision 2). Net worth's TalkBack sentence starts "Net worth" (risk 4). SellScreen's lot rows draw from `state.sellLotRows` (the gain or "no price", "stale, as of … ET", the new TalkBack sentences) and the refresh lines under the "Lots" heading when there are lots. Green at the first run. Tests 762.
- Step 5 (aeb5d7f): OverallAndSellTalkBackTest, 4 tests (AC-61 (d)): Overall at a fresh $160 (net worth 10,880.00 with its age, the row "plus 80.00", no failed line); Overall at a stale $140 while refreshing after a failed refresh (10,720.00, "some stale", the row "minus 80.00", the Updating and failed lines); a sell lot fresh ("8 left, 150.00 … plus 80.00, not picked"); a sell lot stale after a failed refresh ("minus 80.00", "stale price as of 2026-10-29 16:00 Eastern Time", the failed line). `@Test` lines 20 (were 16).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; green at the end of each step; lint the 5 listed findings.

### 13.2 Step 6 (2026-11-09)
- versionCode 25. Full run 03:14:40 to 03:16:15, green: 762 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 03:16:14). APK 42,546,603 bytes (03:15:24), built with the price-source key set (checked True/False only).
- HoldingsScaleTest: one red recorded in this iteration (step 1, 13.1), re-run unchanged as decided in 16b (its file, 13.2, your B).

### 13.3 Step 6: the phone check (2026-11-09, all yes)
Check 1, before the install on 16b's build (versionCode 24), online: your screenshots of Overall, the Portfolios tab, a portfolio's screen and a holding's Lots tab with each lot's gain; a force stop. versionCode 25 installed over it (`adb install -r`, Success; lastUpdateTime 10:47:04, first install 2026-10-29 kept).
- 2 (AC-35), online, the first unlock on Overall: net worth with "Prices as of 2026-11-06 16:00 ET", "Updating prices" briefly, no failed line; one row per portfolio with the figures of its own screen; net worth = cash + the rows; a row opens its portfolio, Back returns to Overall: yes.
- 3: pull-to-refresh on Overall: yes.
- 4: the sell screen (Buy/Sell > Sell, same portfolio and holding, dated today): each lot's gain as on the Lots tab, a pick works, left with Back: yes.
- 5 (AC-42): airplane mode, lock and unlock: Overall "…, some stale" and the failed line under the rows, no pop-up; the sell screen's failed line under "Lots", each lot "stale, as of … ET", still pickable; back online on Overall: refreshed, the marks gone: yes.
- 6 (wireframe 10c): Reverse and re-enter of a sell: the lots as after the reversal, each with its gain; left with Back, not confirmed: yes.
- 7 (AC-39, risk 5): largest font and landscape on Overall and the sell lot list, online and offline: yes.
- 8 (AC-39): TalkBack: net worth, a portfolio row, a lot fresh and stale, the failed line: yes.
- 9 (AC-40, AC-09): light and dark; each shown time with "ET": yes.
- 10 (spec 9): `adb logcat -d -v time --uid=10454` right after check 6: 110 lines, 10:45:58 to 11:02:49, covering the install and checks 2 to 6. The key not in it (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=" or amount outside the timestamps; the 46 "$digit" matches are all the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`: yes.
- 11 (D9): the wording approved (section 12).

### 13.4 AC-62 and the screen tests
From 16c, the TalkBack labels of Overall's net worth and rows and of the sell lot list are checked by OverallAndSellTalkBackTest; their layout at the largest font stayed a hand check (check 7).
- Pull request #146 at 6677ccd: the screen tests (run 42558487448) 20 of 20 at the first attempt (tests 164 s, run 03:49:41Z to 03:55:50Z), OverallAndSellTalkBackTest's 4 among them; CI (run 42558487514, 2m 26s) and CodeQL green (gh, read only).

### 13.5 App start timing (risk 3)
AppStartTimingTest, 300 transactions, from the ScreenTestTiming lines in each run's artifact (one run each, emulator):
- 16b, pull request #144 at 22c8b59 (run 42495826227): load and Overall 3,476, 2,793 and 2,791 ms, median 2,793 ms; main-thread CPU in that phase 650, 670 and 530 ms.
- 16c, pull request #146 at 6677ccd (run 42558487448): load and Overall 5,016, 2,598 and 3,188 ms, median 3,188 ms (+395 ms, +14%); main-thread CPU 1,170, 700 and 720 ms (median 720 against 650, +70 ms).
- My reading: Overall now works out every portfolio's figures on each draw, which may cost a little; but 16c's own runs spread from 2,598 to 5,016 ms and its quickest is quicker than any of 16b's, so one run each cannot tell a change from the emulator's spread. Not proven. Recorded on open item 51 (measure before changing); no change in 16c.

### 13.6 Review against the spec refs (2026-11-09, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: net worth's age line is the oldest price used over every holding, by the same `PortfolioFigures.pricesAsOf` as a portfolio's, with ", some stale" (4.8; 16b decision 1); "Updating prices" in the net worth card and the failed line once, under the rows, no pop-up (section 6, states (2) and (3); decision 1); each row its value and gain with sign and color, never color alone, or "(all holdings at cost)", and a tap opens the portfolio (view 1, marker 4); pull-to-refresh on Overall (decision 2); net worth's TalkBack sentence starts "Net worth" (risk 4; AppStartTimingTest passed); a lot's gain only for a lot that can be picked, at the latest price in today's shares through `Sells.unitsToToday`, both ways round, and in Reverse and re-enter from the transactions as after the reversal (4.4, 4.8, decision 4); the stale flag only with a stale price, "no price" without one (wireframe 10a marker 14); exact arithmetic in core (4.10); no logging (spec 9); the screens only read, K1 and K4 unchanged (section 14). Noted, not findings: (a) the row's "(all holdings at cost)" is joined to the gain inside the composable, as on the Portfolios list since 16b (open item 48's form); (b) `AppState.overall` and `sellLotRows` are worked out on each read, the split factor included (open item 51).

### 13.7 The merge (2026-11-09)
Pull request #146 merged as 2af640a (05:35:01Z), 6 commits; the branch is gone from origin and locally. On the pull request's last commit (0fc39ab) CI (run 42565246860, 2m 58s) and the screen tests (run 42565246814, 20 of 20, tests 113 s) green. On main after the merge: CI (run 42568143450, 2m 36s), CodeQL (run 42568143327) and the screen tests (run 42568143214, 20 of 20 at the first attempt, tests 138 s, 6m 9s) green (gh, read only).

## 14. Departures from the plan
1. Steps 3 and 4 are one commit (0f017fc): both change strings.xml's "Iteration 16c" block, and a file is not staged in parts.
