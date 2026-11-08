# plan/iterations/16b-live-prices-portfolio.md

Stage: Build. Status: approved 2026-11-08 (your "yes approve"); decisions 1 to 3 settled the same day (section 11). In build: step 1.
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
To be shown on the phone in step 6:
- The summary's age line, from the oldest price used: "Prices as of 2026-11-06 16:00 ET" (the form approved for Holding detail in 16a), and "Prices as of 2026-11-06 16:00 ET, some stale" (decision 1).
- A holding row's stale flag with its age: "stale, as of 2026-11-06 16:00 ET".
- Reused, already approved in 16a: "Updating prices", "Price refresh failed. Showing last prices.", "no price, at cost".
- The TalkBack sentences of the summary and the rows with these parts.

## 13. Status and notes
- Draft 2026-11-08; approved the same day; decisions 1 to 3 settled.

## 14. Departures from the plan
None yet.
