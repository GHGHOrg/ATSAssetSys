# plan/iterations/09-buy-stock.md

Stage: Build. Status: built on branch iter-09-buy-stock (steps 1 to 10 and two fixes, commits 6e00841 to 54b1bdb) and checked on the phone 2026-10-31, all checks yes (section 13). Pull request #100 merged as e31c558 on 2026-10-31; CI green on the pull request and on main (your reports). Decisions 1 to 11 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
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
- decodeToSequence is marked @ExperimentalSerializationApi in kotlinx-serialization-json 1.12.0 and may change in a later version; a change shows as a failed build or failed test, not on the phone (decision 5).
- kotlinx-serialization-json 1.12.0 is built on Kotlin 2.4.10 and the project uses 2.4.21; that they work together is shown only by the build in step 6.

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
5. HTTPS through Android's HttpURLConnection, with no HTTP library; JSON read row by row with kotlinx-serialization-json, numbers kept as exact text. Follow-up after step 2 (your decision 2026-10-31, option A): version 1.12.0, and the row-by-row reader decodeToSequence with the opt-in @OptIn(ExperimentalSerializationApi::class), although it is still marked experimental; tests on recorded responses guard it.
6. Open item 37: (a) type "Open-End Fund" is refused as a mutual fund; (b) a ticker not in the list gets "Not saved. AAPPL was not found. Mutual funds are not accepted."; (c) every other type is accepted as 4.3 says: "ETP" is an ETF and every other type, an empty one included, a stock.
7. Lots are worked out from the transactions by replay; there is no lot table (section 8 rebuilds them the same way).
8. A check that cannot be made blocks a ticker not yet verified, with three messages: offline, the source did not answer, no price source configured. Offline is told apart with Android's network-state permission (ACCESS_NETWORK_STATE). A verified ticker needs no check. Amended in step 6 (your decisions 2026-10-31): an answer with status 200 whose body cannot be read is a content problem with its own message (option B); a refused key (401, 403) and too many calls (429) have their own messages (option B); a message caused by a status other than 200 ends with that status in parentheses, such as (429), and the others carry none (option A). So six messages in all.
9. Open item 33's phone check (an app with no data) is deferred to 34; the phone keeps its data.
10. Added in step 8 (your decision 2026-10-31): the buy date works like the Cash filters' From and To: digits only (YYYYMMDD) on the number keyboard, the dashes shown, a ninth digit ignored, another character refused at once, today filled in. Quantity and Total amount use the decimal keyboard. The cash form's date and the reversal date keep their dashes and the text keyboard: open item 32 stays open, taken up as a new intent.md when you choose (option B).
11. Added in step 8 (your decision 2026-10-31, option A): the portfolio is chosen with one chip per portfolio, not the wireframe's dropdown, because a dropdown opens a separate window (CLAUDE.md pitfall from 07).

## 11. Wording to approve (D9; * = given by the wireframes or the spec)
- "Buy/Sell"*, "Buy"*, "Sell"*, "Portfolio"*, "Ticker"*, "Date"*, "Enter date in US market time (New York)"* (existing), "Quantity"*, "Total amount, fees included"*, "Price per share: $150.00"*, "Review"*.
- "Review buy"*, "Total amount"*, "Price per share"*, "Cash effect"*, "Nothing is saved until you confirm."*, "Confirm and save"*, "Back to edit"*.
- "Set the opening balance first"* (existing), "Create a portfolio first"*, "Create a portfolio"*, "Nothing to sell yet"*, "no price, at cost"*.
- New (decision 2): "Selling arrives in a later build".
- Refusals, frame F*: "Not saved. Cash would be negative on 2026-02-01 by $500.00."; "Not saved. ZZZZ is not on a US national securities exchange."; "Total amount: must be more than 0 on a buy."; "Quantity: must be more than 0."; "Quantity: up to 8 decimals. Total amount: up to 2 decimals."
- New refusals (decisions 6 and 8): "Not saved. AAPPL was not found. Mutual funds are not accepted."; "Not saved. AAPPL has not been verified, and the device is offline."; "Not saved. AAPPL could not be checked: the price source did not answer. Try again later."; "Not saved. AAPPL could not be checked: no price source configured."
- Added in step 6 (decision 8 amended): "Not saved. AAPPL could not be checked: the price source sent an answer the app cannot read."; "Not saved. AAPPL could not be checked: the price source refused the key. (401)" (or (403)); "Not saved. AAPPL could not be checked: too many checks in a short time. Try again in a minute. (429)"; and the no-answer message with its status when there is one, such as "Not saved. AAPPL could not be checked: the price source did not answer. Try again later. (503)". Without a status (no connection, a timeout) it carries no suffix.
- Approved in step 8 (2026-10-31):
  - Wording 1 (option B): "Not saved. VFIAX is a mutual fund. Mutual funds are not accepted."; "Not saved. XYZ is quoted in EUR, not US dollars."; frame F's exchange refusal becomes "Not saved. ZZZZ is not on a US national securities exchange (OOTC)."
  - Wording 2 (option A): "Checking AAPL with the price source…" under Review, with a spinner, read once by TalkBack.
  - Wording 3 (option D, your text): a buy's cash line reads "Buy AAPL", then the date with "ET" and the balance after it, then "For portfolio Main" where a direct entry shows its note; TalkBack "Buy AAPL, for portfolio Main, 2026-02-01 Eastern Time, minus 1,500.00 dollars, balance 8,812.34 dollars".
  - Wording 4 (option C): a portfolio whose holdings are all at cost shows its gain/loss as "$0.00 (all holdings at cost)" on the Portfolios row and "$0.00" with "(all holdings at cost)" under it in the summary; a portfolio with no holdings shows $0.00 with no text.
  - Wording 5 (option A): "Buy/Sell" (TalkBack "Buy or sell, opens a menu"), the menu "Buy" and "Sell" (TalkBack "Buy or sell menu"), "Set opening balance" and "Create a portfolio" buttons on their notices, "Nothing to sell yet" and "Selling arrives in a later build" with no button (Back or a tap outside closes them), "< Buy" and "< Review buy", "1 holding"/"2 holdings", "1 share"/"10 shares", and the TalkBack sentences "Price per share, 150.00 dollars", "AAPL, 10 shares, 1,500.00 dollars, no price, at cost", "Main, 2 holdings, 2,200.00 dollars, gain or loss 0.00 dollars, all holdings at cost", "Portfolio value, 2,200.00 dollars" and "Cash effect, minus 1,500.00 dollars".
  - Wording 6 (option A): "Ticker: enter a ticker."; "Quantity: enter a quantity."; "Quantity: digits and one dot only, like 10 or 2.5."; "Quantity: up to 8 decimals."; "Total amount: enter an amount."; "Total amount: digits and one dot only, like 1500.00. No $, commas or minus."; "Total amount: up to 2 decimals."; "Total amount: too large."; "Not saved. A buy cannot be dated before the opening balance (2026-01-01)."; "Not saved. Choose a portfolio."
- Added by me in step 8, not asked: an empty portfolio's row reads "Retirement, no holdings, 0.00 dollars" for TalkBack (wireframe 3 shows no gain/loss line there). To confirm on the phone.
Approved on paper with this file; confirmed on the phone (D9): checks 2 to 7.

## 12. Status and notes
Done. Built 2026-10-31; phone checks 2026-10-31, all yes after two fixes (section 14, items 1 and 2); pull request #100 merged as e31c558 (2026-10-31), CI green on the pull request (3 checks) and on main (3m 25s) (your reports).

## 13. Build results
### 13.1 Research (step 2)
Finnhub call, I called 2026-10-31 from this PC, GET https://finnhub.io/api/v1/stock/symbol?exchange=US with the key in the X-Finnhub-Token header and Accept-Encoding gzip; only counts, sizes and times printed, nothing saved:
- HTTP 200, Content-Type application/json, Content-Encoding gzip.
- 1,209,998 bytes on the wire; 7,356,709 bytes after decompression; first byte after 2,047 ms, complete after 3,751 ms; 31,111 rows by a text count of "symbol": (31,116 on 2026-10-30).
- My reading: about 1.2 MB and 4 s per check of a ticker not yet verified, so decision 4 holds (your decision, option A). The time on the phone, over Wi-Fi or mobile data, is seen in step 10. That Android's HttpURLConnection asks for gzip by default is my knowledge, not checked.
- Found in step 10 (phone check 3): this call was answered by a redirect, which .NET's HttpWebRequest followed by default without my noticing, so the figures above are those of the redirected download. Rechecked 2026-10-31 with redirects off: HTTP 302 to https://static2.finnhub.io/file/privatedatany2/exchange/USf.json with one query parameter, "Authorization", 111 characters, not the key; fetched without the key: HTTP 200, application/json, 7,356,709 bytes, 2,947 ms, 31,111 rows. Only the parameter's name and length were printed.
kotlinx-serialization-json, I fetched 2026-10-31:
- https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-serialization-json/maven-metadata.xml: latest and release "1.12.0", lastUpdated "20261019182949"; before it 1.12.0-RC, 1.11.0, 1.10.0.
- https://github.com/Kotlin/kotlinx.serialization/releases: v1.12.0 "released this 19 Oct 18:36" (no year on the page); "This release is based on Kotlin 2.4.10 and contains all of the changes from 1.12.0-RC."; nothing on decodeToSequence or streaming.
- https://kotlinlang.org/api/kotlinx.serialization/kotlinx-serialization-json/kotlinx.serialization.json/decode-to-sequence.html (version 1.12.0, platform jvm): "@ExperimentalSerializationApi fun <T> Json.decodeToSequence(stream: InputStream, deserializer: DeserializationStrategy<T>, format: DecodeSequenceMode = DecodeSequenceMode.AUTO_DETECT): Sequence<T>"; "Transforms the given stream into lazily deserialized sequence of elements of type T using UTF-8 encoding and deserializer."
- Outcome: decision 5's follow-up (section 10).

### 13.2 Commits
Newest first: 54b1bdb fix 2, fea01af fix 1, 4735441 step 9, be20a0a step 8, 15f1070 step 7, e74382a step 6, 69e8bb5 step 5, aada001 step 4, a16399b step 3, 60d3851 step 2, 6e00841 step 1 with this file. main was 5978482.

### 13.3 Local run
On 54b1bdb's code (2026-10-31, all with --rerun-tasks): spotlessCheck green; debug build green; 388 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 9, Holdings 9, MarketDate 10, Money 19, OpeningBalance 9, Portfolios 23, Quantity 10, TickerCheck 16, Transactions 18, RowMapping 3, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7); lint: 3 findings, all listed, none silenced (AndroidGradlePluginVersion at libs.versions.toml:2, open item 31; NewerVersionAvailable at :17, kotlinx-coroutines-test; TypographyDashes on date_not_a_date, strings.xml:86). kotlinx-serialization-json 1.12.0 raised no lint notice. Release build on 4735441's code green (R8 ran; unsigned; only the known "Unable to strip" message); the two fixes were not rebuilt as release. Debug APK versionCode 9 (aapt2).
Room generated the 2 to 3 migration as two CREATE TABLE IF NOT EXISTS statements (transactions, instruments) and touched no existing table; insertBuy runs both inserts in performInTransactionSuspending, each INSERT OR ABORT with bound parameters (read in app/build/generated/ksp).
The key reached BuildConfig with length 40 (only the length printed); BuildConfig.java and .gradle/ are ignored by git (git check-ignore).

### 13.4 Phone results (Android 17, debug build versionCode 9, your reports)
Checks 1 to 9 all yes; check 3 after fix 1, check 7's finding after fix 2. Notes:
- 1: installed over the 08 build (adb install -r, lastUpdateTime 13:25:29, firstInstallTime 2026-10-29 10:44:04 unchanged); the cash data, Main and 投資信託 were there, Buy/Sell showed (criterion 35, database version 3).
- 2: Buy/Sell on Overall, Portfolios and Cash, not on More; the menu inside the window with no cover loop; a tap outside and Back close it; Sell said "Nothing to sell yet"; Buy opened on Main.
- 3: failed on the first build: every new ticker got "did not answer (302)" (section 14, item 1). After the fix (installed 14:03:35) APPL, a typo, gave "Not saved. APPL was not found. Mutual funds are not accepted.", other tickers passed, and AAPL 10 for $1,500.00 in Main went through check, review, a second review with no look-up, and save; Cash, Portfolios, Main and Overall showed it as described. "Checking…" showed for about 3 to 4 seconds.
- 4: SPY (ETP, NYSE Arca) accepted and shown as "1 share"; Sell then said "Selling arrives in a later build".
- 5: AABB (OOTC), VFIAX, a ninth quantity decimal, quantity 0, total 0, a total over the cash, a date before the opening balance and an empty ticker were refused with their messages and the text kept.
- 6: offline, a new ticker got the offline message with no look-up; AAPL went to review with no look-up.
- 7: largest font, light and dark with the typed values kept, and TalkBack as section 11; your finding: quantity 0 hid the cash rule (section 14, item 2). After the fix (installed 14:41:46) quantity 0 with a total over the cash, and a ninth decimal with a date before the opening balance, each showed both messages.
- 8: after a force stop the buys, holdings, cash lines and net worth were kept, and AAPL needed no look-up.
- 9: log cleared at 14:48:35; you force-stopped the app, reviewed KO (2 shares, 750.25, a look-up) and AAPL, and opened Cash and Main. Read at 14:51:31: app uid 10454, 118 app lines from 14:48:44 to 14:49:54; in the app's lines 0 for KO, AAPL, SPY, 750.25, 1500, the key, finnhub, static2 and Authorization; in the whole log (7,479 lines) 0 for all of them except 1500 (329, from uid 1000 and radio) and Authorization (1, from uid 1017). The app's lines carry only Android's own tags. Only counts, uids and tags were printed. A first read that day failed to find the uid (dumpsys has no userId line here) and was repeated with pm list packages -U.
- The phone now holds the AAPL buy in Main and the SPY buy from check 4, besides the 03 to 06 cash data.

### 13.5 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); a release build of the two fixes; that a refused buy write saves nothing in Room (shown by the generated code only, open item 14); the app's network traffic (criterion 38, traffic part); contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project); whether Finnhub counts the file download against the 60 calls a minute.

## 14. Departures from the plan
1. Defect found in phone check 3: Finnhub answers /stock/symbol?exchange=US with a 302 to a signed link on static2.finnhub.io, and the source followed no redirect, so every new ticker got "did not answer (302)". Step 2's PC measurement had followed the redirect silently (13.1). Fixed with failing tests first (5 of 7 new tests failed before): one redirect (301, 302, 303, 307, 308) is followed by hand, only over https to finnhub.io or a subdomain, without the key; anything else stays "did not answer" with the first status (fea01af, your decision A). Rechecked on the phone.
2. Defect found in phone check 7: a field error hid K1's cash and date rules, because Review ran K1 only when every field passed; this departed from the CLAUDE.md pitfall from 04 and 05. Fixed with failing tests first (3 failed before): K1 runs with stand-ins (quantity 1, a placeholder ticker) whenever the date and total are valid; the Finnhub check still waits for everything else (54b1bdb, your decision A). Rechecked on the phone.
3. Files: Fraction.kt and Holdings.kt are their own files (the plan put lots in Transactions.kt); OpeningBalance.kt (CashRules.replay moved there) and Money.kt changed; PriceSource.kt is in core/ and the implementation is FinnhubSource.kt (the plan said PriceService.kt); NetworkStatus.kt, TickerVerifier.kt (K4 writes instruments, not the ViewModel), BuySellMenu.kt and RowMappingTest.kt are new; Instrument sits in TickerCheck.kt.
4. cash_entries already had transaction_id from 02a, so version 3 adds two tables and no column (section 7's risk did not arise).
5. Valuation.netWorthCents stays as the cash part, because existing core tests use it; the screens use netWorth.
6. Review checks the fields and K1 first (now together, item 2) and asks Finnhub only when both pass; a ticker accepted at Review stays verified even if the buy is not confirmed; after a saved buy the app returns to the tab Buy was opened from.
7. Redirects were not followed at first, for key safety (step 6); item 1 replaced that with the one checked redirect.
8. The buy date follows the Cash filters (decision 10), and the portfolio is chosen with chips (decision 11); both were settled in step 8, after the file was approved.
9. An empty portfolio row reads "<name>, no holdings, 0.00 dollars" for TalkBack, a sentence added in step 8 without asking (section 11).
10. A trade line in the Cash list is not tappable, as this file's section 6 (stubs) planned; the ViewModel's openDetail would still open one if called.
