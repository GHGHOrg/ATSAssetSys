# plan/iterations/16a-live-prices-holding.md

Stage: Build. Status: approved 2026-11-08 (your "yes"); decisions 1 to 4 settled the same day (section 11). In build: step 1.
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
- [To check in step 2, not from memory] What /quote answers for a ticker it does not price (my reading: 200 with c = 0 and t = 0). Such an answer is treated as "no quote", so the price stays as it was and is marked stale.
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
Proposed, to be shown to you on the phone in step 8 and approved before the commit:
- The age line, from the price's own time (decision 4): "Price as of 2026-11-07 16:00 ET", or an age such as "Price 9 h old".
- The stale flag: "stale" next to the age line, with its age.
- The no-price flag: "no price, at cost" (already in the app) and "no price" on a lot (already in the app).
- The refresh-failed line: "Price refresh failed. Showing last prices." (wireframe 06).
- The indicator: "Updating prices", read by TalkBack.

## 13. Status and notes
- Draft 2026-11-08; decisions 1 to 4 settled; approved the same day.

## 14. Departures from the plan
1. After step 2 (2026-11-08, your A): the Finnhub adapter (FinnhubSource, FinnhubList, FinnhubQuote) and its test moved into their own package, prices/finnhub/, so adapter code can be found by folder; CLAUDE.md's "Where things are" names the adapter folders. Its own commit, not in the plan. Not chosen: (B) an "Adapter" suffix on every adapter class; (C) only the CLAUDE.md note.
2. Step 6 (2026-11-08, your A): to keep decision 1 true, a portfolio's value also uses the latest prices (AppState.portfolioValue), and the Portfolios list and a portfolio's screen draw their value, cost, gain/loss and holding rows from a new model (PortfolioFigures.kt, CR2); "at cost" shows only for holdings with no price. Their age line, stale flags and refresh-failed line stay in 16b. Not chosen: (B) the portfolio screens at cost until 16b, so Overall's net worth would not equal cash plus the portfolios.
3. Step 6: Material 3's PullToRefreshBox was checked by compiling against it, not with javap (section 10), so no file outside the repo was read.
4. Step 5: a ticker the source does not price (NoQuote) is marked stale on its own and does not make the refresh "failed"; only offline or a failed answer shows the refresh-failed line, so a delisted sold-out ticker (decision 3) cannot keep it on screen.
