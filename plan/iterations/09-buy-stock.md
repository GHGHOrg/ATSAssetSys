# plan/iterations/09-buy-stock.md

Stage: Build. Status: approved 2026-10-31 with decisions 1 to 9 (section 10); not built. Branch iter-09-buy-stock.
Source: plan/plan.md section 4 (row 09), plan/spikes/price-source.md (decisions 1 to 7, sections 10.2 and 10.3), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
I tap Buy/Sell and then Buy, pick the portfolio, type ticker, date, quantity and total, and see the price per share. Review checks the ticker with Finnhub and the cash rules and shows the review screen; Confirm saves the buy. Its cash line shows on the Cash tab and the holding on the portfolio's screen, valued at cost. A ticker that fails the check, or a buy that would make cash negative on any date, is refused with the reason, and what I typed stays.

## 2. Spec refs
- spec 4.1 (order, replay, blocking), 4.2 (cash out on the trade date), 4.3 (the buy rules and the ticker check), 4.8 (a holding with no price is valued at cost and flagged "no price"), 4.10 (exact quantities, integer cents).
- spec 3: Transaction (buy), Lot, Instrument, the cash entry's transaction_id and the trade type.
- spec 7 and 9: the source behind K4; only tickers and the source's request details leave the phone.
- Example C, as an automated test with a real buy (03 used a withdrawal as a stand-in).
- Wireframe 10a: frames A, B, F and G, markers 1 to 7 and 16 to 18. View 3: the portfolio screen's summary and holdings rows, the Portfolios tab row. View 7: the trade line. Navigation outline: the Buy/Sell button on Overall, Portfolios and Cash.
- Criteria: 15, 19 (the buy part), 24 (the buy and sell parts), 30, 31, 42 (Example C), 52 (the buy part), and 9, 35, 39 to 42.
- Spike price-source: decisions 3 to 6; open items 14 (the first multi-row save), 33 (deferred, decision 9), 37 (settled, decision 6).
- spec 14: K1 (rules), K2 (store), K3 (valuation at cost), K4 (verify only), K5 (the flow), K9.

## 3. Out of scope
- Sell (11): the menu shows Sell with the lines of decision 2 until 11.
- Holding detail (10), prices and refresh (16), Net Worth (Total) (17), sector and geography (23).
- Opening the transaction from a trade line (10 or 27). Hidden holdings and the unhide on a new buy (15). Reversal of a buy (13). The source setting (30).
- The phone check of open item 33 (34, decision 9).

## 4. Files
New, core/: Quantity.kt (exact decimals up to 8 places); Transactions.kt (the buy rules on K1, the replay with buys, lots derived by replay); TickerCheck.kt (the 4.3 decision from type, MIC and currency; the exchange table of the 29 SEC names with XNAS, XNYS, ARCX, BATS, XASE and IEXG as aliases); their tests.
Changed, core/: RecordStore.kt; CashAccount.kt; Valuation.kt (holdings at cost in net worth and portfolio values); their tests.
New, app/: PriceService.kt (K4, verify only: HttpURLConnection over HTTPS, the key in the X-Finnhub-Token header, the US list read row by row, no logging); BuyScreen.kt; BuyReviewScreen.kt; tests on recorded responses.
Changed, app/: AtsDatabase.kt (version 3, an auto migration adding the transactions and instruments tables and transaction_id on cash entries); RoomRecordStore.kt; AppViewModel.kt; AppContent.kt; OverallScreen.kt; PortfoliosScreen.kt; PortfolioScreen.kt; CashScreen.kt; strings.xml; AndroidManifest.xml (INTERNET and ACCESS_NETWORK_STATE, the app's first network permissions); app/build.gradle.kts (the BuildConfig field from the user environment variable FINNHUB_API_KEY, empty when it is missing; versionCode 9); gradle/libs.versions.toml (kotlinx-serialization-json); AppViewModelTest.kt and the fake.
Documents: plan.md, open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-09-buy-stock from main (5978482). This file, the plan.md line, open items 33, 37 and 38, the CLAUDE.md line for letter tags. Baseline run.
2. Research (section 9), each look-up or call approved first.
3. core/ quantities and the buy rules: tests first (up to 8 decimals, a ninth refused, 0 or less refused, a total above 0 with at most 2 decimals, the cash replay with future-dated entries, Example C with a real buy, lots derived by replay), then the code.
4. core/ ticker check: tests first (the 6 MICs pass, OOTC and unknown codes refused, "Open-End Fund" refused as a mutual fund, not USD refused, not found refused, a type, exchange or currency the source does not state accepted, "ETP" as ETF and every other type as stock), then the code.
5. Data layer: the migration to version 3, the DAO, RoomRecordStore and the fake; the buy and its cash line written as one action.
6. K4: PriceService behind an interface; tests on recorded responses (found, not found, OOTC, ETP, no answer, no key).
7. ViewModel: tests first (the menu and its lines; "Set the opening balance first"; "Create a portfolio first" and the return to Buy after create; the form; Review runs every check; Confirm checks again and saves; a refusal keeps the text; offline, no answer and no key; a verified ticker needs no check), then the code.
8. Screens and strings: the Buy/Sell button and its in-window menu, Buy, Review, the holdings rows and summary, the Portfolios tab row, the trade line.
9. versionCode 9. Full run, release build, install over 08's build with data.
10. Phone checks (section 8).
11. Documents and the pull request draft.

## 6. Stubs
- A holding shows "no price, at cost" and no gain/loss until 16; its value is its cost (4.8).
- Sell in the Buy/Sell menu shows "Selling arrives in a later build" while something is held, until 11 (decision 2).
- A trade line does not open its transaction until 10 or 27.

## 7. Risks
- The app's first network access. The key is built in (spike decision 5): no HTTP logging, the key only in the header, no ticker, amount or key in the log. Checked by Logcat (proof 9).
- The first multi-row save (the transaction and its cash line): both or neither (open item 14).
- The first schema change that adds a column to an existing table (cash entries).
- [UNVERIFIED] The size and time of Finnhub's US list (31,116 rows on 2026-10-30) on the phone; measured from this PC in step 2 (decision 4).
- [UNVERIFIED] How Android reports offline against a source that does not answer.
- The Buy/Sell menu, like every menu and dialog, stays inside the activity window with imePadding where it has a field (CLAUDE.md pitfall from 07 and 08).
- Exact arithmetic: quantity and price per share never pass through floating point (4.10).

## 8. Proof
Commands: as in 08, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 08's data (the 03 to 06 cash entries, Main and 投資信託):
1. Install over 08's build (versionCode 8): the data and both portfolios are still there (criterion 35).
2. The Buy/Sell button on Overall, Portfolios and Cash opens the menu inside the window; Sell says "Nothing to sell yet".
3. Buy AAPL in Main: the price per share shows as the fields change; Review shows the review screen; Back to edit keeps the values; Confirm saves. The Cash tab shows the trade line and the balance after it; Main shows the holding at cost with "no price, at cost"; the Portfolios row and Overall's net worth count it at cost.
4. Buy SPY (NYSE Arca, ETP): accepted (criterion 15). Sell now says "Selling arrives in a later build".
5. Refusals, each with the text kept: AABB (OTC), VFIAX and a typo (not found), a quantity with a ninth decimal, a quantity of 0, a total of 0, a total above the cash, a backdated buy that makes a later date negative (Example C style, criterion 42).
6. With the device offline: a new ticker is refused with the offline message; AAPL (verified) is bought (criterion 15).
7. Largest font, light and dark, TalkBack: the menu, the form, the review and the refusals (criteria 39 to 41).
8. Close and reopen the app: the buys, the holdings and the cash lines are kept.
9. Logcat (log cleared first): no ticker, amount or key in the app's lines (counts only).

## 9. Research
- One Finnhub call from this PC: the size and time of /stock/symbol?exchange=US (decision 4). Shown and approved first.
- kotlinx-serialization-json: the current version and whether its row-by-row reader (decodeToSequence) is stable, looked up on the day (decision 5). Shown and approved first.

## 10. Decisions (all Build, settled by you 2026-10-31)
1. 09 is one iteration of 11 steps.
2. Sell is in the Buy/Sell menu. With nothing held it says "Nothing to sell yet" (spec); with something held it says "Selling arrives in a later build" until 11.
3. A buy shows on the portfolio screen (summary at cost, gain/loss $0.00 without color, a holdings row with ticker, shares, value at cost and "no price, at cost"), on the Portfolios tab row (holding count, value, gain/loss), in Overall's net worth (at cost, 4.8) and as a trade line on the Cash tab.
4. The check fetches Finnhub's full US list (/stock/symbol?exchange=US) at each check of a ticker not yet verified, looks the ticker up and keeps only the result. If step 2 shows the list too large or too slow, I come back to you before building it.
5. HTTPS through Android's HttpURLConnection, with no HTTP library; JSON read row by row with kotlinx-serialization-json, numbers kept as exact text.
6. Open item 37: (a) type "Open-End Fund" is refused as a mutual fund; (b) a ticker not in the list gets "Not saved. AAPPL was not found. Mutual funds are not accepted."; (c) every other type is accepted as 4.3 says: "ETP" is an ETF and every other type, an empty one included, a stock.
7. Lots are worked out from the transactions by replay; there is no lot table (section 8 rebuilds them the same way).
8. A check that cannot be made blocks a ticker not yet verified, with three messages: offline, the source did not answer, no price source configured. Offline is told apart with Android's network-state permission (ACCESS_NETWORK_STATE). A verified ticker needs no check.
9. Open item 33's phone check (an app with no data) is deferred to 34; the phone keeps its data.

## 11. Wording to approve (D9; * = given by the wireframes or the spec)
- "Buy/Sell"*, "Buy"*, "Sell"*, "Portfolio"*, "Ticker"*, "Date"*, "Enter date in US market time (New York)"* (existing), "Quantity"*, "Total amount, fees included"*, "Price per share: $150.00"*, "Review"*.
- "Review buy"*, "Total amount"*, "Price per share"*, "Cash effect"*, "Nothing is saved until you confirm."*, "Confirm and save"*, "Back to edit"*.
- "Set the opening balance first"* (existing), "Create a portfolio first"*, "Create a portfolio"*, "Nothing to sell yet"*, "no price, at cost"*.
- New (decision 2): "Selling arrives in a later build".
- Refusals, frame F*: "Not saved. Cash would be negative on 2026-02-01 by $500.00."; "Not saved. ZZZZ is not on a US national securities exchange."; "Total amount: must be more than 0 on a buy."; "Quantity: must be more than 0."; "Quantity: up to 8 decimals. Total amount: up to 2 decimals."
- New refusals (decisions 6 and 8): "Not saved. AAPPL was not found. Mutual funds are not accepted."; "Not saved. AAPPL has not been verified, and the device is offline."; "Not saved. AAPPL could not be checked: the price source did not answer. Try again later."; "Not saved. AAPPL could not be checked: no price source configured."
- To draft in step 8 and show you before use: the mutual fund refusal, the not-in-USD refusal, the trade line (buy, ticker and portfolio name, with "ET"), and the TalkBack labels of the menu, the holdings row and the trade line.
Approved on paper with this file; confirmed on the phone (D9): checks 2 to 7.

## 12. Status and notes
Approved 2026-10-31. Not built.
