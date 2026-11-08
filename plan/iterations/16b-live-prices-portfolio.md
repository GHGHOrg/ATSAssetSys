# plan/iterations/16b-live-prices-portfolio.md

Stage: Build. Status: approved 2026-11-08 (your "yes approve"); decisions 1 to 3 settled the same day (section 11). Built 2026-11-08 (steps 1 to 6); phone check 2026-11-08 all yes; the screen tests 16 of 16 on pull request #144 (section 13.4); reviewed against the spec refs (section 13.5).
Source: plan.md section 4 (row 16b), spec 4.8, section 6 (view 3, error and loading states, number format and color, accessibility), section 7, AC-41, AC-42, AC-61, AC-62; wireframe 03 (frames A and B, markers 1, 4, 5 and 6); 16a (its decisions 1 to 4, wording, departures 2 and 4); open items 48 and 50 to 52. "My reading" is an interpretation, not a test.

## 1. Outcome
On a portfolio's screen you see how old its prices are (the oldest price used), each holding's stale or "no price" flag, an "updating" indicator while a refresh runs, and one quiet line when the last refresh failed; the Portfolios list shows the same quiet line and indicator. Pulling down refreshes prices on these screens too (decision 2).

## 2. Spec refs
- 4.8: a portfolio's price age line is the age of the oldest price used in it; a holding valued at cost is not counted; stale and "no price" flags.
- Section 6, view 3: the summary's price age line; each holding row's stale or "no price" flag. Wireframe 03 marker 1: one quiet line under the Portfolios list when a refresh failed; marker 4: the age line, and the quiet line under it; marker 6: "stale" with its age, "no price, at cost".
- Section 6, error and loading states (2) and (3): no pop-up, one quiet line per screen that shows prices, an "updating" indicator on the price part only. Accessibility (5): stale and "no price" are text, never color alone.
- Section 7: pull-to-refresh; each price shows its timestamp.
- AC-41 (the portfolio figures at $160, already right since 16a), AC-42 (offline, the line, the indicator), AC-61 (d) (rows and the summary whose TalkBack text carries figures), AC-62 (from 16b the layout and TalkBack-label parts of covered screens are not checked by hand).
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- 16c: Overall (net worth's age line, its failed line) and the sell lot picker.
- Allocation (view 4) and Net Worth (Total) (view 2): not built yet.
- An age such as "9 h old" instead of the time (open item 49, idea).
- Open item 51 (the models as kept fields): measure first, not in 16b.

## 4. Analysis (2026-11-08, by reading the code and the spec)
- 16a draws the portfolio figures at prices from ui/PortfolioFigures.kt (departure 2), but the model has no price time, no stale flag and no refresh state: PortfolioScreen.kt and PortfoliosScreen.kt say "follows in 16b".
- `Valuation.oldestPriceTime(holdings, prices)` exists (16a step 4) and counts only holdings with shares and a price; nothing uses it yet.
- `AppState.refreshing` and `refreshFailed`, the refresh itself (K4's `PriceRefresh.refresh`) and the strings "Updating prices", "Price refresh failed. Showing last prices." and "stale" exist and are approved (16a section 12).
- Pull-to-refresh is on Holding detail only (16a decision 2, PullToRefreshBox).
- My reading: a stale price (16a departure 4: also a ticker the source does not price) shows "stale" on its row even when the refresh did not fail, so the failed line alone does not tell you.

## 5. Files
- app/: ui/PortfolioFigures.kt (the summary's age and stale state, each row's stale flag and its price time, the refresh state); PortfolioScreen.kt and PortfoliosScreen.kt (draw them; pull-to-refresh, decision 2); AppViewModel.kt or AppContent.kt (wiring); prices/PriceRefresh.kt (open item 50); core's Valuation.kt and AppState (open item 52); strings.xml (section 12); build.gradle.kts (versionCode 24).
- Tests: PortfolioFiguresTest (age, stale, no price, sold out, refresh state); PriceRefreshTest (a 5xx answer stops the refresh); the tests of the removed functions moved to PortfolioFigures; a ViewModel wiring test; screen tests (AC-61 (d)).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48, 50 and 52).
- No new library, no schema change.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-16b-live-prices-portfolio, one pull request.
1. This file and the plan.md lines.
2. The model: PortfolioFigures with the age, the stale flags and the refresh state, tests first, shown red.
3. The screens: the portfolio's summary (age line, indicator, failed line), its holding rows (flags), the Portfolios list (indicator, failed line), pull-to-refresh on both (decision 2). The wording of section 12, once approved.
4. Open items 50 and 52 (decision 3), tests first.
5. The screen tests (AC-61 (d)).
6. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A portfolio with only holdings at cost or sold out has no price used: no age line (my reading of 4.8). A test for it.
- 2. A long row at the largest font: "stale" with its time beside a value and a gain can wrap; the rows already wrap (FlowRow). Checked by the screen tests and, for speech, by hand.
- 3. AC-62 is in force from 16b: for these screens the layout and the TalkBack labels are checked by the screen tests only; if a test is missing, nothing checks it. The hand check keeps TalkBack speech, the themes, the network and the install.
- 4. Pull-to-refresh inside a screen that already scrolls (both screens use verticalScroll): it worked on Holding detail; checked again on the phone.
- 5. The emulator is not the phone (C19); the screen tests use no network.
- 6. Open item 50: stopping at a 5xx answer leaves the remaining tickers stale until the next refresh; the same as for a 429 since 16a.

## 9. Proof
- Commands: as in 16a, all with --rerun-tasks. The screen tests on the pull request: 13 plus this iteration's new ones.
- Unit tests: steps 2 and 4.
- The phone check, over 16a's build (AC-35; no schema change): a portfolio's age line against Holding detail's time; a holding's stale flag and the summary's ", some stale" (airplane mode, lock and unlock); "no price, at cost" (if a holding has none); the failed line on both screens and no pop-up; "Updating prices" and pull-to-refresh on both screens; TalkBack speech of the summary and a row; light and dark; logcat as in 16a.
- AC-62: no side-by-side table; the matching hand checks are dropped for these screens.

## 10. Research
None: Material 3's PullToRefreshBox is already in use (16a).

## 11. Decisions (Build, settled by you 2026-11-08)
1. The summary's age line when some price used is stale: it adds ", some stale" ("Prices as of 2026-11-06 16:00 ET, some stale"), and the rows say which (your A). Not chosen: (B) the rows only.
2. Where pull-to-refresh works: on the Portfolios list and on a portfolio's screen too, the same refresh as Holding detail (your A). Not chosen: (B) a portfolio's screen only; (C) neither.
3. Open items in 16b: both 50 (a refresh stops at the first 5xx answer, the rest stay stale, as for a 429) and 52 (`AppState.portfolioValue` and `Valuation.portfolioValue` removed, their tests moved to PortfolioFigures) (your A). Not chosen: (B) only 52; (C) neither.

## 12. Wording to approve (D9)
Approved on the phone 2026-11-08 (your A, check 9), as in strings.xml's "Iteration 16b" block: "Prices as of 2026-11-06 16:00 ET" and "…, some stale"; "stale, as of 2026-11-06 16:00 ET"; the TalkBack forms "Prices as of … Eastern Time[, some stale]" and a row ending "…, stale price as of … Eastern Time".
Proposed in step 1:
- The summary's age line, from the oldest price used: "Prices as of 2026-11-06 16:00 ET" (the form approved for Holding detail in 16a), and "Prices as of 2026-11-06 16:00 ET, some stale" (decision 1).
- A holding row's stale flag with its age: "stale, as of 2026-11-06 16:00 ET".
- Reused, already approved in 16a: "Updating prices", "Price refresh failed. Showing last prices.", "no price, at cost".
- The TalkBack sentences of the summary and the rows with these parts.

## 13. Status and notes
- Draft 2026-11-08; approved the same day; decisions 1 to 3 settled.

### 13.1 Steps 1 to 5 (2026-11-08)
- Step 1 (7f2861e): this file and the plan.md lines.
- Step 2 (b9e1f78): ui/PortfolioFigures.kt gains `PricesAsOfModel` (the oldest price used, in New York, from `Valuation.oldestPriceTime`, and `someStale` for any price used that is stale; null when no price is used), a row's `staleAsOf` (its price's own time, only with shares and a stale price) and `PriceRefreshModel`; `AppState.priceRefresh`. PortfolioFiguresTest 4 new, and its two whole-model checks extended with the new fields; PriceRefreshFlowTest 1 wiring test. Shown red against a stub first (19 tests, 6 failed). Tests 742 to 747.
- Step 3 (659c660): ui/PriceRefreshParts.kt (pull-to-refresh and the "Updating prices" and refresh-failed lines, moved unchanged from HoldingDetailScreen, which now uses them); a portfolio's summary gains the age line and the two refresh lines, a stale row "stale, as of … ET" and its TalkBack text; the Portfolios list the two refresh lines under the list; pull-to-refresh on both (decision 2); strings.xml's "Iteration 16b" block. Two red runs fixed in the code: a ktlint blank line between `when` branches, and a new lint finding, ModifierParameter, from a modifier parameter named `under` (renamed `modifier`, not silenced). Tests 747.
- Step 4 (c70bd56): open item 50, a refresh stops at a 5xx answer (PriceRefreshTest's stop case gains 500 and 503, shown red first, 9 tests, 1 failed; its go-on case uses 404); open item 52, `Valuation.portfolioValue` and `AppState.portfolioValue` removed, their checks moved: a sold-out holding through `Valuation.valued`, the per-portfolio sums to PortfolioFiguresTest `eachPortfolio_isWorthItsOwnHoldingsOnly`, the price cases already in PortfolioFiguresTest, BuyFlowTest to the shown figure, PriceRefreshFlowTest to cash plus `Valuation.valued`, exactly. One red run: a compile error in the 5xx line (no smart cast across modules), fixed. Tests 746 (−2 +1).
- Step 5 (c9872e5): PortfolioTalkBackTest, 3 tests (AC-61 (d)): a portfolio at a fresh $160 (the value, "plus 80.00", "Prices as of … Eastern Time", the row; no failed line), at a stale $140 after a failed refresh ("minus 80.00", "some stale", the row's stale price, the failed line), and the Portfolios list (the row, the failed line). `@Test` lines 16.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; green at the end of each step; lint the 5 listed findings.

### 13.2 Step 6 and the scale test (2026-11-08)
- versionCode 24. Its first full run (14:17:34) was red in 15c's HoldingsScaleTest; the next (14:19:02 to 14:20:01), unchanged, green: 746 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 14:20:00).
- HoldingsScaleTest was red in full runs of this iteration with core unchanged: 13:39 (`netWorth` SPREAD grew 9.8, bound 8), 13:51 (`Hide` SPREAD 17.3 ms at 3,000, limit 15.0), 13:53 (`lots` ONE_HOLDING grew 8.8), 14:17 (`lotsOf` SPREAD grew 18.1); the run at 14:05 also counted one core failure, not read before the next run. Run alone twice after the first, it was green (`netWorth` SPREAD 5.9 and 7.0 ms at 3,000). With 16a's two, 6 reds (7 with the unread one) on 2026-11-08, each a different row, never alone. My reading: the test runs beside the app's tests and lint in a full run (the configuration cache runs tasks in parallel); not proven. Your B (2026-11-08): keep it as it is, re-run and record. Not chosen: (A) its own Gradle task after the rest; (C) an open item for later.

### 13.3 Step 6: the phone check (2026-11-08, all yes)
Check 1, before the install on 16a's build (versionCode 23), online: your screenshots of the Portfolios tab, a portfolio's screen and a Holding detail's price line; a force stop. versionCode 24 installed over it (`adb install -r`, Success, 14:25:21).
- 2 (AC-35), online, the first unlock: "Updating prices" under the list, then gone, no failed line; the portfolio's figures as in the screenshots, "Prices as of 2026-11-06 16:00 ET", no stale row: yes.
- 3: pull-to-refresh on a portfolio's screen (the spinner and "Updating prices" in the summary) and on the Portfolios list (under the list): yes.
- 4 (AC-42): airplane mode, lock and unlock: one failed line under the list, no pop-up; the summary "…, some stale" with the failed line; each row "stale, as of … ET"; back online on the screen: refreshed by itself, the marks and the line gone: yes.
- 5 (AC-39; no screen test covers these screens' layout): largest font, landscape, offline: both screens scroll, nothing cut off, the stale lines included: yes.
- 6 (AC-39): TalkBack reads the age line, a fresh and a stale row, the Portfolios row and the failed line: yes.
- 7 (AC-40, AC-09): both screens in light and dark, the stale and failed lines readable; each shown time with "ET": yes.
- 8 (spec 9): `adb logcat -d --uid=10454`. The first read (72 lines, 23:08:54 to 23:12:55) no longer held checks 2 to 4: the older lines had rolled out of the buffer. Re-check (your choice): a force stop, an unlock and a pull on the Portfolios list, then a read at 23:15:04 (69 lines, 23:09:45 to 23:14:29, the new process from 23:14:19): the key not in it (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=" or amount outside the timestamps; the "$1" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; one `BiometricPromptCompat` line ("Unable to cancel authentication. BiometricFragment not found.") from the old process at the force stop. Every line is the system's: yes.
- 9 (D9): the wording approved (section 12).

### 13.4 AC-62 and the screen tests
From 16b, for these screens the TalkBack labels are checked by PortfolioTalkBackTest, not by hand. No screen test covers their layout at the largest font, so check 5 stayed a hand check. The screen tests (16) run on the pull request; their result is added there.
- Pull request #144 at f2b646c: the screen tests (run 42494633276) 16 of 16 (tests 127 s, run 5m 16s), CI (run 42494633306, 3m 11s) and CodeQL green (gh, read only). PortfolioTalkBackTest's 3 tests passed on the first run.

### 13.5 Review against the spec refs (2026-11-08, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: the age line is the oldest price used and leaves out holdings at cost and sold out (4.8); ", some stale" counts only prices used (decision 1); a row's stale flag only with shares and a stale price, as text, never color alone (section 6, accessibility (5)); the failed line on both screens and no pop-up, "Updating prices" only in the price part (section 6, states (2) and (3)); pull-to-refresh on both (decision 2), and not under the ⋮ menu's layer, which takes the touches; a 5xx answer stops the refresh (decision 3); no logging and no floating point in the changed files (spec 9, 4.10); K4 alone writes prices, the screens only read (section 14). Noted, not a finding: a portfolio row on the Portfolios list carries no stale mark (view 3 gives it none), so a price that is stale only because the source no longer prices it (16a departure 4) shows on a portfolio's screen, not on the list.

## 14. Departures from the plan
1. Step 3: the pull-to-refresh wrapper and the two refresh lines moved out of HoldingDetailScreen into ui/PriceRefreshParts.kt, shared by the three screens; Holding detail draws the same. Not in section 5's file list.
2. Step 6: the phone check's logcat read was done twice (13.3, check 8), the second after a short re-check, because the first no longer held checks 2 to 4.
