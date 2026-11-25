# plan/iterations/43-cr6-gain-loss-percent.md

Stage: Build. Status: In progress. Approved 2026-11-26 (your A). Built and released in one iteration (your decision, 2026-11-26): the release 1.2.0 follows the build in the same iteration, through plan/release.md.
Source: plan.md section 4 (row 43); intent/intent-cr-06-gain-loss-percent.md (CR6, final); spec/spec.md v9: C26, 4.8, 4.10, section 6 (views 1, 3 and 6, buy and sell entry, number format and color, accessibility), section 14 (K3), AC-14, AC-34, AC-35, AC-61, AC-63, AC-64, AC-69, AC-72; wireframes 01, 03, 06 and 10a; plan/release.md. "My reading" is an interpretation, not a test.

## 1. Outcome
Wherever the unrealized gain/loss shows (Overall's portfolio rows, the Portfolios list, a portfolio's summary and its holding rows, Holding detail's header and its lots, and the sell lot picker), its percentage over cost follows it in brackets with 2 decimals, in the same sign and color: "+$80.00 (+6.67%)". Then your app is the signed release 1.2.0 with it, installed over 1.1.0 with your data kept.

## 2. Spec refs
- 4.8: the percentage is the unrealized gain/loss ÷ cost; a lot's over its remaining cost; a holding's and a portfolio's the total gain/loss ÷ the total cost of what has a gain/loss; "no price" left out of both, the total keeping its sign; stale counts; none at a cost of $0.00; a total is its own ratio.
- 4.10: exact, rounded once.
- Section 6: views 1, 3 and 6 and the sell lot list; number format (2 decimals, brackets, the dollar figure's color, each figure's own sign, none at a figure that rounds to zero, the percentage on the next line where the two do not fit, C26); TalkBack reads it in full ("Unrealized gain, 80 dollars, 6.67 percent", D9).
- Section 14: K3 works out the percentage and only reads.
- AC-14, AC-34, AC-69 and AC-72 (the figures); AC-61 (screen tests: TalkBack text, fixed parts at the largest font, the row that does not fit); AC-35 (install over the previous build).
- The release: AC-63, AC-64, C17, C21, C22; plan/release.md sections 2 and 3.
- Wireframes: 01 marker 4; 03 markers 1, 4 and 5; 06 markers 1 and 5; 10a markers 10 and 12.
- Standing checks: AC-09, AC-35, AC-39 to AC-42 on the development app (CLAUDE.md section 7).

## 3. Out of scope
- Realized gains (view 5) and the sell review's realized gain: no percentage (CR6 outcome 6).
- The net worth card: it shows no unrealized gain/loss, so no percentage.
- A percentage on Net Worth (Total)'s composition or on Allocation.
- Open items 48 and 69, except a screen this iteration changes anyway; Dependabot #236 to #238.

## 4. Analysis (2026-11-26, by reading the code)
- One shown type carries every unrealized gain: `GainModel.Figure` (`HoldingDetailModel.kt:31`), built by `HoldingDetail.gainOf` (:200). It is used by the holding header (:163), the lot rows (:194), `PortfolioFigures.of` (the summary, and Overall's portfolio rows through `OverallModel.kt:73`) and `.rows` (`PortfolioFigures.kt:92`, :120), and the sell lot rows (`SellLotsModel.kt:46`). It gains the percentage beside the dollars, so each of those models carries it.
- The costs: a holding's `Holding.cost`; a lot's `Lot.cost` (its remaining cost); a sell lot choice's `available × costPerShare` (`Sells.choiceGain`, core `Sells.kt:182`); a portfolio's sum over its holdings that have a price. Today `PortfolioFigures.of` sums every holding's cost (:84) and adds $0 for a "no price" gain (:85); the percentage needs the cost of the priced holdings only (4.8; CR6 outcome 4).
- The 2-decimal percentage and its signs are already written for the 1-day line (`DayLineModel.kt:32`, `DayLineModelTest`): rounded half away from zero, grouped by thousands, no sign at zero. My reading: the same formatter serves both, moved to one place and tested once more.
- Screens that draw the gain: HoldingDetailScreen.kt (header :441, lot row :636 and :674), PortfolioScreen.kt (holding row :350, summary), the Portfolios list and Overall's portfolio rows, SellScreen.kt (lot row :340). Their TalkBack sentences (`holding_row_gain_spoken`, `sell_lot_gain_spoken` and the others) gain the percentage.
- No stored data changes: database version stays 15, the backup format 1 (CR6 constraints). So no migration and no migration test (plan/release.md section 3 needs one only at a schema raise).

## 5. Files
- core/: Valuation.kt (the percentage of a gain over a cost, none at $0.00; a portfolio's priced cost), Sells.kt (a lot choice's cost, if not already there); their tests.
- app/ ui/: HoldingDetailModel.kt (`GainModel.Figure` with the percentage; `gainOf` with the cost), DayLineModel.kt (the shared percentage formatter), PortfolioFigures.kt, SellLotsModel.kt; HoldingDetailScreen.kt, PortfolioScreen.kt, PortfoliosScreen.kt or OverallScreen.kt where the portfolio rows are drawn, SellScreen.kt; a shared composable for "+$80.00 (+6.67%)" that moves the percentage to the next line where it does not fit (C26); strings.xml.
- app/build.gradle.kts: versionCode 53, versionName 1.2.0 (step 6; the version raise of plan/release.md step 2).
- Tests: unit tests for core and each model; ViewModel wiring tests; screen tests (AC-61).
- Documents: this file, plan.md (sections 2, 4, 11 and 12), plan/release.md section 3 if its schema line needs a word, open-items.md if an item is found.

## 6. Order of work
Each build step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-43-cr6-gain-loss-percent; two pull requests (CLAUDE.md section 6: the version raise merged first, so the tag sits on main's merge commit; then the release records).
Pull request 1 (the build and the version raise):
1. This file and the plan.md lines (section 2, row 43, section 11, the change log).
2. Core (K3): the percentage of a gain over a cost and a portfolio's priced cost, tests first and shown red: AC-72's +6.67%, the lots' +60.00% and +6.67% (AC-14), the "no price" holding left out (+6.67%, not +4.00%), a loss −6.67%, none at $0.00, exact thirds (for example a gain of $1 on $3: 33.33%), rounding up and down.
3. The models: `GainModel.Figure` with the percentage and its spoken form; the shared formatter; the holding header, lot rows, portfolio figures and rows, Overall's rows and the sell lot rows; tests first and shown red. The existing model tests follow the new type, none weakened.
4. The screens: "+$80.00 (+6.67%)" in each place, the percentage on the next line where it does not fit (C26), the TalkBack sentences with the percentage (D9, proposed).
5. The screen tests (AC-61): each changed row's and header's TalkBack text with the percentage; the lot row and the holding row at the largest font in portrait and landscape show the whole percentage, not cut off (C26); Holding detail's header still fixed only while it takes at most half the screen. The existing screen tests that read these sentences follow them.
6. versionCode 53 and versionName 1.2.0 (MINOR: a feature, CLAUDE.md section 4); the wording shown (D9); the phone check on ATS Dev (`1.2.0-dev.53`, installed over `1.1.0-dev.52`); the Deploy review of 43 against the spec refs (Ruleset A rule 6), findings by severity; the pull request draft.
Release (plan/release.md section 2, after pull request 1 is merged):
7. The Deploy review since v1.1.0 (43 only), the tag message and the release notes drafted for your approval.
8. You: the tag `v1.2.0` on main's merge commit, signed and pushed. release.yml on the tag, read by me; the artifact downloaded outside the repo.
9. You: the APK signed; I check it (the certificate's SHA-256 equal to 1.1.0's, C21; zipalign; badging; its SHA-256).
10. You: a backup on 1.1.0 and the figures noted; `adb install -r` over 1.1.0 (C17, AC-35); the smoke check (AC-64); Logcat by uid clean (spec 9).
11. You: the GitHub Release `v1.2.0` with the approved notes (`--verify-tag`).
Pull request 2 (the records):
12. This file's records, plan.md section 11 and the change log, CLAUDE.md's uid line if it changes; the pull request draft. Then the merge record.

## 7. Stubs
None planned.

## 8. Risks
1. The portfolio's base: today's cost sums every holding, "no price" included. A total that used it would show +4.00% where 4.8 says +6.67%; step 2 tests that case first.
2. Room on narrow rows (C26): a lot row in the picker and on the Lots tab already carries several figures; at the largest font the percentage must wrap, not be cut off. Checked by a screen test and on the phone. Unit tests cannot see it (CLAUDE.md section 6).
3. Holding detail's header gets a longer gain line; it stays fixed only while it takes at most half the screen (pitfall from 10); its screen test measures it.
4. The TalkBack sentences change in several screens: every screen test that reads them is searched by the string names, not one pattern (pitfall from 14c).
5. The release: an R8 fault shows only when the release runs (C23); CI's class check covers one kind. The smoke check is on your data, after a backup on 1.1.0.
6. The two pull requests: the release's tag must sit on pull request 1's merge commit (AC-63); nothing else is merged to main between that merge and the tag.

## 9. Proof
- Commands: spotlessCheck, `.\gradlew.bat :core:assemble :app:assembleDebug`, the test command of CLAUDE.md section 3, `:app:lintDebug`, all with --rerun-tasks, and `:app:compileDebugAndroidTestKotlin`. Test totals from the result files (1,556 before this iteration). The screen tests on the pull request: 188 plus the new ones, counted from the artifacts.
- The phone check on ATS Dev with test data, installed over `1.1.0-dev.52` (AC-35): each place of section 1 against figures worked by hand (AC-72: +6.67%, a loss, "no price" left out, sold out with none); the lots in the picker (AC-14: +60.00% and +6.67%); largest font and landscape (C26); TalkBack; light and dark; offline and stale; Logcat by uid clean (spec 9).
- The release: release.yml green on the tag; the signed APK's certificate equal to 1.1.0's; your app at 1.2.0 (versionCode 53) with uid 10455 kept; the smoke check (AC-64); the GitHub Release `v1.2.0` with the schema line "15 (unchanged)" and the backup format "1 (unchanged)".

## 10. Research
None planned.

## 11. Decisions (Build)
None open. Asked in a step only if two ways of writing the code differ for you (CLAUDE.md section 8: maintainability, test run time, APK size).

## 12. Wording to approve (D9)
Shown in step 4, approved on the phone in step 6: the TalkBack sentences of the holding header, a lot row, a holding row, a portfolio's summary, Overall's and the Portfolios list's portfolio rows, and a sell lot row, each with the percentage (for example "Unrealized gain, 80 dollars, 6.67 percent"); the bracket form "%1$s (%2$s)" is already approved (41, the 1-day line).

## 13. Status and notes
- Draft 2026-11-26; approved the same day (your A). On iter-43-cr6-gain-loss-percent from main (fa4641f). Each step's tree is held and its commit message written; the commits come together, in order, when one is needed (your instruction, 2026-11-26).
- Step 1 (2026-11-26): this file and plan.md (sections 2, 4, 11 and 12). Full run green (BUILD SUCCESSFUL in 2m 59s, 104 tasks): 1,556 tests, 0 failures, 1 skipped; lint the 7 listed findings; spotlessCheck and the androidTest compile passed; `@Test` lines in androidTest 188.
- Step 2 (2026-11-26): core/UnrealizedGain.kt (K3): `UnrealizedGain` (the gain and the cost it is over, exact; the percentage gain ÷ cost, none at $0.00; a sum) and `UnrealizedGains` (`ofHolding` over the holding's cost, `ofLot` over the lot's remaining cost, `ofChoice` over the sell choice's available quantity × its cost per share, each null with no price; `total` leaving out the ones with none, $0.00 over $0.00 when none has one). The existing `Valuation.valued`, `lotGain` and `Sells.choiceGain` stay as they are and are reused. UnrealizedGainTest 9, shown red against a stub first (9 of 9 failed): AC-72's 1/15 (+6.67%) for the holding and its lot, AC-14's 3/5 and 1/15 for the picker's lots, a choice across a split, the "no price" holding left out of both (1/15, not 80/2,000), a total as its own ratio (680/2,200), a loss −1/15 and a stale price, $0.00 over $0.00 with none, 1/3 exact. Full run green (1m 57s, 104 tasks): 1,565 tests, 0 failures, 1 skipped; lint the 7 listed findings; `@Test` lines 188. Main's runs on fa4641f (#244's merge) green at the first attempt (gh, read only).
- Step 3 (2026-11-26): the models. ui/PercentFigure.kt: `PercentFigure` and `Percent.of` (2 decimals, half away from zero, grouped by thousands, each figure's own sign, none at zero), now also the 1-day line's formatter (DayLineModel.kt uses it; DayLineModelTest unchanged and green). `GainModel.Figure` gains `percent`; `HoldingDetail.gainOf` takes core's `UnrealizedGain`, so the holding header, the lot rows, `PortfolioFigures.of` (the summary and Overall's portfolio rows) and `.rows`, and the sell lot rows carry it. `PortfolioFigures.of` sums gain and cost over the priced holdings for the percentage (risk 1); its shown cost stays every holding's. Shown red against a stub first (20 of 45 failed in the five classes). New: PercentFigureTest 5 (+6.67% and −6.67%, +60.00%, 33.33% down, 66.67% up, 0.13% half up, 0.00% with no sign, +1,250.00%, none); HoldingDetailModelTest +1 (+$0.01 (0.00%)); PortfolioFiguresTest +1 (AC-72: the "no price" holding left out, +6.67%, the shown cost $2,000.00) and the wiring in `eachPortfolio_isWorthItsOwnHoldingsOnly` (+60.00%, −77.14%, none). Tests that follow the new type, each with the percentage worked by hand, none weakened: HoldingDetailModelTest (Example A's header and lot +6.67%, the loss −6.67%, the stale price +6.67%; the gain that rounds to $0.00 now shows its own "+0.40%", $0.004 over $1.00, uncolored by the dollar figure, section 6), PortfolioFiguresTest (Example A +6.67%; AAPL at $140 with MSFT unpriced −6.67% over $1,200.00, not −5.00% over $1,600.00), SellLotsModelTest (+60.00%, −40.00%, +20.00% twice, −40.00%). Full run green (2m 0s, 104 tasks): 1,572 tests, 0 failures, 1 skipped; lint the 7 listed findings; `@Test` lines 188.
- Step 4 (2026-11-26): the screens. ui/GainText.kt: `gainFigures` ("+$80.00 (+6.67%)", or the dollars alone with no percentage; one text with a space, so the percentage wraps under the dollars where the two do not fit, C26) and `gainSpoken` ("plus 80.00 dollars, plus 6.67 percent"). Drawn in the 7 places: Holding detail's header line and each lot (its figures and "stale" now in a FlowRow, so they wrap rather than squeeze), a portfolio's summary (`SummaryLine` reads the percentage) and its holding rows, Overall's and the Portfolios list's portfolio rows, the sell lot rows. Strings (D9, proposed): the ten gain sentences lose their own "dollars" after the gain (holding_header_gain_spoken, holding_header_sold_out_spoken, holding_lot_gain_spoken, holding_lot_gain_stale_spoken, holding_row_gain_spoken, holding_row_gain_stale_spoken, overall_portfolio_row_spoken, portfolio_row_spoken, sell_lot_gain_spoken, sell_lot_gain_stale_spoken), and four are new: gain_figures "%1$s (%2$s)", gain_spoken "%1$s dollars, %2$s percent", gain_spoken_no_percent "%1$s dollars", spoken_label_dollars_percent "%1$s, %2$s dollars, %3$s percent". So a holding row reads "AAPL, 8 shares, 1,280.00 dollars, unrealized gain/loss plus 80.00 dollars, plus 6.67 percent". No unit test draws a screen; step 5's screen tests check the text and the sentences. Runs: red on spotlessCheck (a blank line before a comment in a `when`), fixed by hand; then green (2m 20s, 104 tasks): 1,572 tests, 0 failures, 1 skipped; lint the 7 listed findings; `@Test` lines 188.

## 14. Departures from the plan
None yet.
