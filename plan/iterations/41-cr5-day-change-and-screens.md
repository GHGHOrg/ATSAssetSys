# plan/iterations/41-cr5-day-change-and-screens.md

Stage: Build. Status: approved 2026-11-24 (your A); one iteration for all of CR5 (your decision). Step 1 in progress; not built.
Source: plan.md section 4 (row 41); intent/intent-cr-05-day-change-and-screens.md (CR5, final); spec/spec.md v8: C24, C25, section 3 (Instrument), 4.8, 4.10, section 6 (views 1, 2, 3 and 6, More and Settings entry, navigation outline, number format and color, accessibility), section 7, section 14 (K3, K4), AC-13, AC-35, AC-61, AC-62, AC-67 to AC-71; wireframes 01, 02, 03, 06 and 11; plan/spikes/price-source.md 10.2 (Finnhub's /quote fields). "My reading" is an interpretation, not a test.

## 1. Outcome
On Overall, Net Worth (Total), the Portfolios list, a portfolio's screen and its holding rows, and Holding detail, a 1-day gain/loss shows in dollars and percent under the gain/loss it belongs to, indented and smaller in a card or header. The net worth card shows Investments (net worth without cash) between net worth and the 1-day line. A holding opens on its Lots tab (tabs Lots, Transactions, Classification), and each lot shows its current value. More has a fifth entry, About: the app's name, its version and the open-source licences, each row opening in place. Installed over 1.0.0, the app keeps all data.

## 2. Spec refs
- C24: no day figure without a previous close or on a split dated on the day. C25: the day is the latest price's market date (its timestamp in New York).
- Section 3: Instrument carries the previous close. Section 7: the previous close comes with each quote and is replaced with the price; a failed refresh keeps both, marked stale.
- 4.8: the 1-day rule per lot (before the day: from the previous close; on or after it: from the lot's cost per share), sums per holding, portfolio and net worth, the percentage over the day's starting value (value − day figure) over the same holdings, no percentage at a $0.00 start, stale and "no price"; Investments = the sum of portfolio values = net worth − cash as of today.
- 4.10: exact, rounded once.
- Section 6: views 1, 2, 3 and 6 as amended by CR5; More with About (rows open in place, not a pop-up menu); number format (the day figure colored, the percentage with 2 decimals, the 1-day line, TalkBack); Investments not colored.
- Section 14: K3 works out the 1-day figures and Investments and only reads; K4 fetches the previous close and writes instrument data only.
- AC-67 to AC-71; AC-13 (About reachable); AC-35 (install over the previous build); AC-61 (screen tests: focus, TalkBack text, fixed parts at the largest font); AC-62.
- Wireframes: 01 markers 2 and 4; 02 markers 1 and 6; 03 markers 1, 4 and 5; 06 markers 1, 3 and 5 (frames A and B); 11 markers 1 to 5.
- Standing checks: AC-09, AC-35, AC-39 to AC-42 on the development app (CLAUDE.md section 7).

## 3. Out of scope
- The sell lot picker (CR5 open question 4: it stays as it is).
- A price change shown on its own (spec section 6).
- Backup and restore in Settings (asked 2026-11-24, dropped).
- Open items 48 and 69, except a screen this iteration changes anyway.
- The release 1.1.0: after this iteration, through plan/release.md, with a backup first (the schema changes; spec 8, Releases and the format version).

## 4. Analysis (2026-11-24, by reading the code)
- Lots: the tab order is `HoldingDetailScreen.kt:188` (`listOf(TRANSACTIONS, LOTS, CLASSIFICATION)`); the opening tab is `TRANSACTIONS` in `AppViewModel.kt` (the state's default at :611, the openings at :3121 and :3187). `LotRowModel` (`HoldingDetailModel.kt:67`) has `cost` and `gain`; it gains the current value.
- Tests that name the tabs: HoldingDetailFlowTest (:95 and :139 expect Transactions on opening), ClassOverrideFlowTest, KeptFieldsFlowTest, HoldingTalkBackTest, ClassificationTalkBackTest. Each keeps what it checks; the ones that check the opening tab now expect Lots (Ruleset A rule 5: not weakened).
- Investments: `NetWorthCard` (`OverallScreen.kt:140`) is drawn by Overall and Net Worth (Total) from two models (`state.overall`, `state.netWorthTotal`); one new row in the card covers both.
- About: `MoreEntry` (MoreModel.kt) gains ABOUT; the version comes from the build (versionName with the development app's `-dev.n` suffix, 38a decision 2a). The licence list's source is decision 1.
- Previous close: `FinnhubSource` reads `c` and `t` (`FinnhubSource.kt:68`); Finnhub's /quote also gives `pc` (plan/spikes/price-source.md 10.2: `"pc":343.83` for AAPL on 2026-10-30). `Quote.Priced` (core PriceSource.kt:39) and `Price` (core TickerCheck.kt:39) gain the previous close; the prices table gains a nullable column: database version 15, migrated from 14. Version 14 is the schema of v0.38.0 and v1.0.0 (added in 31, 3d456da; `git tag --contains`).
- 1-day: a pure function in core (K3) over the lots as of today, their buy dates, the price, its time and previous close, and the splits; every screen model with a gain/loss gets a day line (Overall, Net Worth (Total), Portfolios, a portfolio, holding rows, Holding detail).

## 5. Files
- core/: PriceSource.kt and TickerCheck.kt (the previous close); a new DayChange.kt (or in Valuation) with the 1-day rules; their tests.
- app/: prices/finnhub/FinnhubSource.kt (`pc`); data/ (the prices entity's column, AtsDatabase.kt version 15 and its migration; app/schemas/…/15.json, written by Room); ui/: HoldingDetailModel.kt, HoldingDetailScreen.kt, OverallModel.kt, OverallScreen.kt (NetWorthCard), NetWorthModel.kt, the portfolio models and screens, MoreModel.kt, MoreScreen.kt, a new AboutModel.kt and AboutScreen.kt, AppViewModel.kt, AppContent.kt; strings.xml; build.gradle.kts (versionCode 51; what decision 1 needs).
- Tests: unit tests for each model and rule; a migration test 14 to 15; ViewModel wiring tests; screen tests (AC-61).
- Documents: this file, plan.md (sections 4, 11 and 12), spec.md C24 (step 1, with your acceptance), open-items.md if an item is found.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-41-cr5-day-change-and-screens, one pull request.
1. This file and the plan.md lines (row 41, section 11, the change log); spec.md C24 marked partly checked (shown first, your acceptance).
2. Lots: the lot's current value in the model, tests first and shown red; then the tab order, opening on Lots, and the lot row on screen. The existing tests follow the new order.
3. Investments: the model field and the card's row, tests first.
4. About: the entry, the screen and the licence rows that open in place (after decision 1).
5. Previous close: `pc` in the source, core and the store; database 15 with the migration from 14 and its test. Nothing shown yet, so the app reads the same. This step's green run is the migration's own proof.
6. The 1-day rules in core (K3), tests first and shown red: AC-67's figures, stale, no price, no previous close, a split on the day, a $0.00 start.
7. The 1-day line in every screen model and on screen (dollars and percent, indented and smaller in a card or header, TalkBack).
8. The screen tests (AC-61): the About rows keep the window focus; the TalkBack text of the lot row, the day line and Investments; the fixed headers at the largest font in landscape with the extra line.
9. versionCode 51, the wording shown (D9), the phone check on ATS Dev, the documents and the pull request draft.

## 7. Stubs
None planned.

## 8. Risks
1. The migration (AC-71) runs on your real data at the 1.1.0 release. Room has no destructive fallback (CLAUDE.md section 6), so a wrong migration shows "Your data cannot be read" and needs a restore. Before any install: 15.json against 14.json (each table's SQL), the APK and each classesN.dex against the previous build, and a name the step added found in the dex (pitfalls from 15 and 29).
2. Whether `pc` is adjusted for a split on its day is not checked (C24); the split-day rule keeps a wrong figure from showing.
3. A test that assumed Transactions first: every read of `HoldingTab` is searched, not one pattern.
4. The extra lines at the largest font in landscape: Holding detail's header stays fixed only while it takes at most half the screen (pitfall from 10); checked by a screen test and on the phone.
5. Size: nine steps, one long pull request, about 15 to 20 new screen tests, so a longer screen-test run.
6. R8: the release build runs the new code only at release time; CI's class check covers one kind of fault (pitfall from 38b).
7. The day's market date comes from the price's timestamp `t`; AAPL and SPY carried the same `t` on 2026-10-30 (price-source.md 10.2, "may be a shared snapshot"). My reading: the date in New York is still right; not checked on a holiday.

## 9. Proof
- Commands: spotlessCheck, `.\gradlew.bat :core:assemble :app:assembleDebug`, the test command of CLAUDE.md section 3, `:app:lintDebug`, all with --rerun-tasks, and `:app:compileDebugAndroidTestKotlin`. Test totals from the result files. The screen tests on the pull request: 178 plus the new ones.
- The phone check on ATS Dev with test data, installed over 1.0.0-dev.50 (AC-35, AC-71): data kept after the migration; no day line until the first refresh; then each day line against figures worked by hand; Investments = net worth − cash; a holding opens on Lots with each lot's value; About with its version, licence rows opening in place with no cover loop; offline and stale; largest font and landscape; TalkBack; light and dark; Logcat by uid clean (spec 9).
- Your daily app gets this only at the 1.1.0 release (plan/release.md).

## 10. Research
- Decision 1 may need a web search for a licence plugin's current version and how it works with AGP 9.4.1; shown to you before it runs (CLAUDE.md section 8).

## 11. Decisions (Build)
1. The licence list's source (step 4): a Gradle plugin that gathers the licences at build time, or a list kept by hand in the repo. Weighed by maintainability, test run time and APK size. Asked in step 4.
2. Where the version text comes from (step 4): `BuildConfig` or the package manager; asked only if they would differ for you.

## 12. Wording to approve (D9)
Shown in the step that adds it, approved on the phone in step 9: the lot row's TalkBack sentence and its "Cost" label; "Investments"; the 1-day line's label and TalkBack ("1-day gain, 16 dollars, 1.27 percent"); "About", "Version …", "Open-source licences" and a row's TalkBack (expandable, collapsible).

## 13. Status and notes
- Draft 2026-11-24; approved the same day (your A).
- Step 1 (2026-11-24): this file, plan.md (sections 2, 4, 11 and 12, and K3 and K4 in "Where components first appear"), and spec.md C24 and section 7 marked partly checked (your A; spec.md stays v8). Full run green (BUILD SUCCESSFUL in 2m 17s, 103 tasks): 1,513 tests, 0 failures, 1 skipped; lint the 7 listed findings; spotlessCheck and the androidTest compile passed.
- Step 2 (2026-11-24): `LotRowModel.value` and `spokenValue` (remaining quantity × the price, exact, rounded once; the lot's cost with no price). HoldingDetailModelTest +3 and one changed (Example A $1,280.00; stale $140 gives $1,120.00; no price gives the cost $1,200.00; 1/3 × $100.005 = $33.34 and 1/3 × $100 = $33.33), shown red against a stub first (4 of 14 failed). The tabs Lots, Transactions, Classification; the state's default and both openings (from a portfolio, from Hidden holdings) on Lots; a return from a sub-screen keeps the tab. The lot row: value on the right with the gain under it, "Cost …" under the quantity on the left; the three lot sentences carry the value (D9, proposed). Tests moved, none weakened: HoldingDetailFlowTest (opens on Lots; a chosen tab is not kept), ClassificationTalkBackTest (the order left to right), HoldingTalkBackTest (the value in the lot sentences); HoldingHeaderTest and TalkBackLabelTest set Transactions, the tab they check. Full run green (1m 32s, 103 tasks): 1,516 tests, 0 failures, 1 skipped; lint the 7 listed findings; `@Test` lines in androidTest 178.

## 14. Departures from the plan
None yet.
