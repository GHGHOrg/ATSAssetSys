# plan/iterations/16a-live-prices-holding.md

Stage: Build. Status: approved 2026-11-08 (your "yes"); decisions 1 to 4 settled the same day (section 11). Built 2026-11-08 (steps 1 to 8); phone check 2026-11-08 all yes; the screen tests 13 of 13 on pull request #142 after one test fix (section 13.4).
Source: plan.md section 4 (row 16a), spec 4.8, section 6 (view 6, error and loading states, number format and color), section 7, section 9, AC-05, AC-41, AC-42, AC-48, AC-61, AC-62; plan/spikes/price-source.md (decisions 2, 5 and 6, section 10.2); open items 40 (closed 2026-11-08) and 48. "My reading" is an interpretation, not a test.

## 1. Outcome
On Holding detail you see the holding valued at its latest price: the value, the unrealized gain/loss in the header and on each lot, the price's age, and a stale or "no price" flag. Prices refresh by themselves when the app opens online, and by pulling down on Holding detail. Offline, the last prices stay, marked stale, with one quiet line saying the refresh failed.

## 2. Spec refs
- 4.8: market value = shares × latest price; a holding never priced is valued at cost and flagged "no price"; a price without a fresh quote keeps the last price, marked stale; the age is the age of the oldest price used.
- Section 7: automatic refresh when online, plus pull-to-refresh; each price shows its timestamp; failure keeps the last price, marked stale.
- Section 6: view 6 (the header: value, shares, cost, "Unrealized gain/loss", the price age line and the flag; the Lots tab's unrealized gain/loss per lot); error and loading states (2) and (3): no pop-up, one quiet refresh-failed line, an "updating" indicator on the price part only, and stored figures show at once; number format and color (green and red with sign, "no price" uncolored).
- Section 9: K4 is the only part that sends data out (tickers, to the source); the key in the X-Finnhub-Token header only; nothing logged.
- Section 14: K4 only writes prices and instrument data; K3 and K9 only read.
- AC-05, AC-41 (the Holding detail figures for Example A at $160), AC-42 (offline, the refresh-failed line, the indicator), AC-48 (the Holding detail parts that need a price).
- AC-61: (a) Holding detail's header with its new lines; (d) the header and each lot row, whose TalkBack text carries figures. AC-62: 16a is the side-by-side iteration (plan.md row 16a): the screen tests and the hand check both run, and the results are compared.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- 16b: a portfolio's screen, its holding rows and the Portfolios list (their age line, flags and refresh-failed line).
- 16c: Overall and the sell lot picker.
- 23 (sector and geography from the source), 30 (changing the source and the sweep), 31 (backup; the file holds no prices, spec 8).
- A refresh on a timer while the app stays open (decision 2).
- Moving the valuation off the main thread (15c decision 5).

## 4. Analysis (2026-11-08, by reading the code and the spec)
- Prices do not exist yet: `Valuation` values every holding at cost (Valuation.kt line 5), `InstrumentRow` has no price columns (AtsDatabase.kt line 76, schema version 6), and `PriceSource` has only `lookUp` (PriceSource.kt).
- Finnhub's /quote (spike 10.2; open item 40): GET /api/v1/quote?symbol=X with the key in the X-Finnhub-Token header; 200 with c (price), pc, t (Unix seconds) and more; no redirect (checked 2026-11-08). During the session t is about half a minute behind; outside it, t is the last close. Prices have up to 4 decimals and are read as exact decimals, never as floating point (4.10).
- Checked in step 2 (one call, your yes, 2026-11-08 01:53:51 +08:00, AAPPL): /quote answers a ticker it does not price with 200 and {"c":0,"d":null,"dp":null,"h":0,"l":0,"o":0,"pc":0,"t":0}. Such an answer (c or t 0) is treated as "no quote", so the price stays as it was and is marked stale.
- The kept fields (15c): `holdings`, `netWorth` and `holdingLots` are worked out only when their inputs change. Prices become a new input; a missed input would leave stale figures on the screen (risk 1).
- Holding detail's header and Lots tab have no screen model (open item 48); the Transactions rows do (15a). CR2 asks for a model when a screen changes.

## 5. Files
- core/: PriceSource.kt (a `quote(ticker)` call and its result kinds, as `Lookup` has); a `Price` value (exact decimal, its time, the stale flag); Valuation.kt (market value, unrealized gain/loss and the age from prices; "no price" at cost as now).
- app/: FinnhubSource.kt (the /quote call, same key and logging rules as the look-up); AtsDatabase.kt (schema version 7: price, price time and stale flag on the instruments table, with a migration from 6); RoomRecordStore.kt (read and write prices; K4 only); a price refresh in AppViewModel.kt (on open when online, and on pull-to-refresh; decision 2); KeptFields.kt (prices as an input); HoldingDetailModel.kt (new: the header and the lots, CR2); HoldingDetailScreen.kt (draws from the model; pull-to-refresh; the age line, the flag, the refresh-failed line and the "updating" indicator); strings.xml (section 12); build.gradle.kts (versionCode 23).
- Tests: core (Valuation with prices: Example A at $160; stale; no price; the oldest age); FinnhubSource against a fake server (the quote's fields, exact decimals, the header, no redirect followed, the failure kinds); the migration 6 to 7; the refresh in the ViewModel (online, offline, partial failure, the indicator); the model's builder; KeptFields (prices as input); screen tests (AC-61 (a) and (d)).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (item 48).
- No new library: pull-to-refresh comes from Material 3, which the app already uses (checked with javap in step 6, not from memory).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-16a-live-prices-holding, one pull request.
1. This file, the plan.md lines, and open item 40's result.
2. K4 in core and app: the quote call, tests first against a fake server, shown red. One live call to /quote for a ticker Finnhub does not price, with your yes (section 4).
3. Storage: schema version 7 and its migration, tests first; the store reads and writes prices.
4. K3: Valuation with prices, tests first (Example A at $160: value $1,280.00, cost $1,200.00, +$80.00; a stale price keeps its figures; "no price" at cost; the age of the oldest price used).
5. The refresh: on unlock when online, when the phone comes back online, and on pull-to-refresh (decision 2), every ticker held except hidden ones (decision 3), the stale flag on failure, the "updating" state, and prices as a kept-field input. Tests first.
6. Holding detail: its model (header and lots, CR2), then the screen: the age line, the flag, the refresh-failed line, the indicator, pull-to-refresh. The wording of section 12, once you approve it.
7. The screen tests (AC-61 (a) and (d)) and the AC-62 comparison.
8. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A kept field that misses prices as an input shows old figures after a refresh. Guard: prices in KeptInputs and a kept-field test for them (step 5).
- 2. Figures elsewhere change in 16a (decision 1): once K3 uses prices, Overall's net worth and a portfolio's value use market values, but their age line and flags come only in 16b and 16c.
- 3. Floating point: JSON numbers must be read as exact decimals. A test with a 4-decimal price and one under $1 (spec section 6, number format).
- 4. The key: the same rules as 09 (the header only, no HTTP logging, nothing logged). Checked at Deploy and with adb logcat on the phone (spec 9).
- 5. Calls: Finnhub's free key has a per-minute limit (not checked on the day); a refresh asks once per ticker. A 429 answer leaves the remaining prices as they were, marked stale.
- 6. The schema migration on your phone: compare 7.json with 6.json before installing, and stop at the unreadable-data screen if it appears (CLAUDE.md pitfall; no backup until 31).
- 7. Market hours: a refresh outside the session gives the last close with its time; the age line then shows hours, which is correct but may look like a fault (decision 4).
- 9. A refresh asks for sold-out tickers too (decision 3), so it makes more calls than the figures need; with the per-minute limit (risk 5) a large list could reach it.
- 8. The emulator is not the phone (C19); the screen tests use a fake price source, never the network.

## 9. Proof
- Commands: as in 15d, all with --rerun-tasks. The screen tests on the pull request: 11 plus this iteration's new ones.
- Unit tests: steps 2 to 6.
- The phone check, before and after installing over 15d's build (AC-35, schema 7): Holding detail for a holding you own: the value, the gain/loss with its color and sign, the age line and each lot's gain/loss, against the price shown in another app at the same moment; pull-to-refresh and the indicator; with airplane mode on: the app opens, the last prices stay, marked stale with their age, one refresh-failed line, no pop-up (AC-42); a holding never priced shows "no price, at cost". Logcat shows no key, ticker or amount (spec 9). The standing checks AC-09, AC-39, AC-40, AC-42.
- AC-62: each screen-test check next to the matching hand check, and whether the tests caught everything the hand check found.

## 10. Research
- Material 3's pull-to-refresh in the version the app uses: checked with javap on the cached jar (CLAUDE.md pitfall), in step 6.
- Finnhub's answer for an unpriced ticker: one live call, with your yes, in step 2.
- Finnhub's free per-minute limit: from its pages only if you paste them; otherwise risk 5 stands.

## 11. Decisions (Build, settled by you 2026-11-08)
1. Where prices count in 16a: K3 uses prices for every figure from 16a: Holding detail, and also Overall's net worth and a portfolio's value, which then follow 4.8; their age line and flags come in 16b and 16c (your A, 2026-11-08). Not chosen: (B) only Holding detail uses prices until 16b and 16c, so a holding's value there would differ from what its portfolio shows, against 4.8.
2. When the automatic refresh runs: each time the app is unlocked while online, and when the phone comes back online while the app is open; plus pull-to-refresh on Holding detail (your A, 2026-11-08). Not chosen: (B) as A, plus every few minutes while the app stays open during the session.
3. Which tickers a refresh asks for: every ticker the app holds, sold-out ones included, excluding hidden ones (your answer, 2026-11-08). My reading: a ticker is asked for when at least one of its holdings (a ticker within a portfolio) is not hidden, so a ticker hidden in one portfolio and held in another is still asked for. Not chosen: (A) only tickers with shares; (B) every ticker, hidden ones included, as the sweep of spec 7 does.
4. The age shown: the price's own time from the source (t); after the close it says the price is from the last close, hours ago (your A, 2026-11-08). Not chosen: (B) the time of the last successful refresh.

## 12. Wording to approve (D9)
Approved on the phone 2026-11-08 (your A, check 10), as in strings.xml's "Iteration 16a" block: "Price $342.57, as of 2026-11-06 16:00 ET" and its ", stale" form; "Updating prices"; "Price refresh failed. Showing last prices."; "stale" beside a lot's gain; the TalkBack sentences of the header, the lots, the holding rows and the portfolio summary ("plus" or "minus" on a gain, "Eastern Time", ", stale" and ", stale price"). Not chosen for now: (B) an age such as "Price 9 h old", recorded as an idea (open item 49).
Proposed in step 1:
- The age line, from the price's own time (decision 4): "Price as of 2026-11-07 16:00 ET", or an age such as "Price 9 h old".
- The stale flag: "stale" next to the age line, with its age.
- The no-price flag: "no price, at cost" (already in the app) and "no price" on a lot (already in the app).
- The refresh-failed line: "Price refresh failed. Showing last prices." (wireframe 06).
- The indicator: "Updating prices", read by TalkBack.

## 13. Status and notes
- Draft 2026-11-08; decisions 1 to 4 settled; approved the same day.

### 13.1 Steps 2 to 6 (2026-11-08)
- Step 2 (c3d1a1d): core's `Quote` and `PriceSource.quote`; `FinnhubQuote.read` reads c and t as exact decimals, c or t 0 as `NoQuote`, a malformed body as `Unreadable`; the /quote call sends the key in the X-Finnhub-Token header only and follows no redirect. FinnhubSourceTest 8 new, shown red first. The live call for an unpriced ticker: section 4. Tests 694.
- Departure 1 (dc51485): the Finnhub adapter moved to prices/finnhub/.
- Step 3 (72ac60e): core's `Price` on `Instrument`; `RecordStore.savePrices`; `saveInstrument` keeps a stored price (DAO `upsertKeepingPrice`); `InstrumentRow` gains `price` (TEXT), `priceTime` (INTEGER, Unix seconds) and `priceStale` (INTEGER NOT NULL DEFAULT 0); AutoMigration 6 to 7. Schema compare: only the instruments table changed, 6.json unchanged, 7.json LF (8,145 bytes), as expected. RowMappingTest 4 new (3 red first). Tests 698.
- Step 4 (15892af): `Valuation.valued`, `lotGain`, `netWorthOf(prices)`, `portfolioValue(prices)`, `oldestPriceTime`; `HoldingValue`. ValuationPricesTest 10 new (9 red first). Example A at $160: value $1,280.00, +$80.00, net worth $10,880.00. Tests 708.
- Step 5 (653beb6): prices/PriceRefresh.kt (the tickers, the run, the prices applied); `AppViewModel.refreshPrices` after the load, on unlock, on pull-to-refresh, and when the phone comes back online only after a failed refresh; `AppState.refreshing` and `refreshFailed`; the kept fields take the instruments as an input, and net worth uses prices; `NetworkStatus.watch` from MainActivity's onStart and onStop. PriceRefreshTest 7 and PriceRefreshFlowTest 7 (12 red first). A mutation check (the instruments input left out) failed 3 tests, and was undone. Tests 722 and 734 with step 6.
- Step 6 (653beb6): ui/HoldingDetailModel.kt (header, lots, `GainModel`, `PriceLineModel`, the time in New York); `AppState.shownPrice`, `holdingHeader`, `holdingLotRows`; HoldingDetailScreen draws from them (the gain colored with its sign, the price line, "Updating prices", the refresh-failed line, PullToRefreshBox, whose own spinner shows only for a pull). Departure 2: ui/PortfolioFigures.kt, so the Portfolios list and a portfolio's screen draw their figures at prices. HoldingDetailModelTest 11, written after the model, so not shown red; a mutation check (the plus sign) failed 3 and was undone. PortfolioFiguresTest 5 (red first); PriceRefreshFlowTest 9 (two wiring tests). Tests 740.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, green; lint the 5 listed findings. Only the final tree of steps 5 and 6 was built, not each commit alone.

### 13.2 Steps 7 and 8 (2026-11-08)
- Step 7 (3a4c754): HoldingHeaderTest (both cases) now measures the header at its tallest: a stale price line, "Updating prices" and the refresh-failed line together (AC-61 (a)). HoldingTalkBackTest, new, 2 tests (AC-61 (d)): Example A on the Lots tab at a fresh $160 and a stale $140, the header's and the lot's TalkBack text exact. No network: the screen is drawn from a built state. `@Test` lines in app/src/androidTest: 13 (11 before). Unit tests 740 (no change). The optional check of a portfolio holding row's TalkBack text was not written (its age line and flags come in 16b).
- Step 8: versionCode 23. Its first full run (10:07:48) failed once in 15c's HoldingsScaleTest: `lotsOf` on spread holdings took 6.554 ms at 3,000 transactions after 0.659 ms at 1,000 (growth 9.9, bound 8), with only versionCode changed since the last green run (02:56). The next full run (10:09:48 to 10:10:48), unchanged, was green: 740 tests, 0 failures, 1 skipped; `lotsOf` spread grew 1.4, 2.4 and 3.2 times; lint the 5 listed findings (report 10:10:47). My reading: a slow moment on the PC, as in 15d (its file, section 13.1); not proven.

### 13.3 Step 8: the phone check (2026-11-08, all yes)
Check 1, before the install on 15d's build (versionCode 22): your screenshots of Overall, a portfolio's screen, and Holding detail with both tabs; then airplane mode on and a force stop. versionCode 23 installed over it (`adb install -r`, Success, 10:45:43), still offline.
- 2 (AC-35, AC-42, 4.8), offline, no price yet: no unreadable-data screen (schema 7's migration ran); the figures as in the screenshots, all at cost; Holding detail "no price, at cost", no price line, one refresh-failed line, no pop-up; each lot "no price": yes.
- 3, airplane mode off on Holding detail: "Updating prices" briefly, then the failed line gone; AAPL $342.57, as of 2026-11-06 16:00 ET (Friday's close), the same price and time as another app; the gain/loss figures and colors right: yes.
- 4, pull to refresh: the spinner and "Updating prices", then both gone: yes.
- 5 (AC-42), airplane mode on, lock and unlock: the last price kept, ", stale" on the price line and "stale" beside each lot's gain, one refresh-failed line, no pop-up; airplane mode off with the screen open: refreshed by itself, the marks and the line gone: yes.
- 6 (AC-39): at the largest font in landscape the header scrolls with the list, nothing cut off, the Lots tab too; upright at the normal font it stays in place: yes.
- 7 (AC-39): TalkBack reads the header, a lot, the portfolio's summary and a holding row in full, with "plus" or "minus", the price "as of … Eastern Time", and "stale" offline; the refresh-failed line is read: yes.
- 8 (AC-40, AC-09): Holding detail and the portfolio's screen in light and in dark, readable, the stale marks and the failed line too; each shown date with "ET": yes.
- 9 (spec 9): `adb logcat -d --uid=10454`, 54 lines from 10:43:23 to 10:58:02 (the install and checks 2 to 5): the key not in it (a True/False test), and no "finnhub", "token", AAPL, "quote", "symbol=", dollar amount or 342.57; the 4 "APPL" matches are the system's PHASE_CLIENT_APPLY_ANIMATION. Every line is the system's (ImeTracker 29, InsetsController 22, DisplayManager 1, ActivityThread 1): yes.
- 10 (D9): the wording approved (section 12).

### 13.4 AC-62: the screen tests next to the hand check
| Screen-test check | Matching hand check | Hand check | Screen test |
|---|---|---|---|
| HoldingHeaderLargestFontLandscapeTest: the tallest header scrolls with the list | 6, largest font, landscape | yes | passed after the test fix (run 42445930002); red in run 42442053835 by a fault in the test |
| HoldingHeaderNormalFontPortraitTest: the tallest header stays in place | 6, normal font, upright | yes | passed in both runs |
| HoldingTalkBackTest, fresh price: header and lot text with "plus" and the price's time | 7, TalkBack | yes | passed in both runs |
| HoldingTalkBackTest, stale price: header and lot text with "minus" and "stale" | 7, TalkBack offline | yes | passed in both runs |

- Run 42442053835 (pull request #142 at 2f55c8f): CI and CodeQL green; the screen tests 12 of 13. HoldingHeaderLargestFontLandscapeTest stopped at its setup check: "the fixed part ends at 0.0 px of 1344 px". My reading: at the largest font in landscape 16a's header puts the tab row below the screen, and `boundsInRoot` is clipped to the screen, so it was empty; the phone (check 6) showed the header scrolling as it should. Fixed in the test (3f13bd5, departure 5), not re-run; the run's results are kept in the session's scratchpad.
- Run 42445930002 (at 3f13bd5): the screen tests 13 of 13 (tests 177 s); CI (run 42445930031) and CodeQL green.
- Result (AC-62): the hand check found no defect, and every matching screen test passed, so the tests caught everything the hand check found. From 16b on, for a screen AC-61 covers, the layout at the largest font and in landscape, layers, menus and the keyboard, and TalkBack labels are no longer checked by hand. Not covered by a screen test, and stays a hand check: TalkBack speech itself, the themes (check 8), the network (checks 2 to 5), installing over data (checks 1 and 2), and the portfolio's TalkBack text (16b). Open item 47 closed.

## 14. Departures from the plan
1. After step 2 (2026-11-08, your A): the Finnhub adapter (FinnhubSource, FinnhubList, FinnhubQuote) and its test moved into their own package, prices/finnhub/, so adapter code can be found by folder; CLAUDE.md's "Where things are" names the adapter folders. Its own commit, not in the plan. Not chosen: (B) an "Adapter" suffix on every adapter class; (C) only the CLAUDE.md note.
2. Step 6 (2026-11-08, your A): to keep decision 1 true, a portfolio's value also uses the latest prices (AppState.portfolioValue), and the Portfolios list and a portfolio's screen draw their value, cost, gain/loss and holding rows from a new model (PortfolioFigures.kt, CR2); "at cost" shows only for holdings with no price. Their age line, stale flags and refresh-failed line stay in 16b. Not chosen: (B) the portfolio screens at cost until 16b, so Overall's net worth would not equal cash plus the portfolios.
3. Step 6: Material 3's PullToRefreshBox was checked by compiling against it, not with javap (section 10), so no file outside the repo was read.
4. Step 5: a ticker the source does not price (NoQuote) is marked stale on its own and does not make the refresh "failed"; only offline or a failed answer shows the refresh-failed line, so a delisted sold-out ticker (decision 3) cannot keep it on screen.
5. After step 8 (2026-11-08, your approval): HoldingHeaderTest measures the tab row's bottom unclipped (`positionInRoot.y + size.height`) instead of `boundsInRoot.bottom`, which is empty for a row below the screen (run 42442053835; section 13.4). The assertions, the swipe and the setup are unchanged. Its own commit, 3f13bd5.
