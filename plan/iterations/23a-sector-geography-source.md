# plan/iterations/23a-sector-geography-source.md

Stage: Build. Status: approved 2026-11-11 (your "Approved", as one iteration 23); decisions 1 to 4 settled the same day (section 11): 23 split into 23a (this file) and 23b (the breakdowns). Built 2026-11-11 (steps 1 to 8; section 14); phone check 2026-11-11 all yes (section 13.3); the screen tests 61 of 61 on pull request #166 (section 13.4); reviewed against the spec refs (section 13.5).
Source: plan.md section 4 (row 23, split into 23a and 23b), spec section 3 (Instrument: source sector and geography; Holding classification; Lists), 4.9 (effective value, source values mapped to the lists, a new text added), section 6 (view 6, the Classification tab with the split history), section 7 (the source supplies sector/geography texts when it has them), section 14 (K4, K10), C9, C14, AC-12 (the holding part), AC-61, AC-62; wireframe 06 (frame C, marker 7 and the notes on the tabs and the Classification tab); plan/spikes/price-source.md (profile2, the calls of 2026-10-30, question 5); iterations 10 (Holding detail), 16a (the refresh). "My reading" is an interpretation, not a test.

## 1. Outcome
Each holding shows its sector and geography on Holding detail's new third tab, Classification: taken from the price source where it supplies them ("From the source"), "Unclassified" where it does not, with the holding's split history under them. Nothing else on the screens changes; the sector and geography breakdowns come in 23b.

## 2. Spec refs
- Section 3: an instrument carries the source sector and geography as the source supplies them (may be missing); a holding's effective value is the override, else the source value via the list, else "Unclassified"; two lists, sectors and geographies.
- 4.9: a source text is mapped to a list value through the remembered source texts, then by name (D7: case and spaces ignored); if neither matches, the text is added to the list as a new value. No remembered text exists before a rename (25), so in 23a a text maps by name or is added (decision 4).
- Section 6, view 6: three tabs, Transactions, Lots, Classification, opening on Transactions; Classification shows the effective sector and geography each labelled "From the source", "Override" or "Unclassified", and the split history (C9); with no split, one plain line.
- Section 7 and K4: the source supplies sector/geography texts when it has them and passes them to K10; K4 writes only instrument data. K10: the lists and the mapping, pure logic.
- C14: ETFs often have none; they show "Unclassified" (the override is 24).
- AC-12's holding part ("Each holding shows its sector and geography … A holding with neither shows Unclassified"; the allocation part is 23b's), AC-61, AC-62. Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The sector and geography breakdowns on Overall, Net Worth (Total) and Allocation (23b).
- The override, Edit and Clear override (24); "Override" as a label appears only from 24.
- Managing the lists in Settings, renaming and remembered source texts (25), deleting (25).
- The backup file's lists, overrides and source texts (31). A second price source (30).

## 4. Analysis (2026-11-11, by reading the code, the spec and the price-source note)
- Finnhub's `/stock/profile2?symbol=` gives, for a stock, `finnhubIndustry` (for example "Technology") and `country` (the headquarters, for example "US"); for an ETF `{}` (SPY); for an OTC ticker "N/A" as the industry (AABB). Calls of 2026-10-30 (the price-source note, 10 and question 5). My reading: the industry is the source sector and the country the source geography. R1 checks it today.
- `PriceSource` (core) has `lookup` and `quote`; FinnhubSource (prices/finnhub/) is the only adapter. A third call, for the profile, goes there, with the key in the `X-Finnhub-Token` header as the others, never in the URL.
- `Instrument` and `InstrumentRow` have no source sector or geography: two nullable columns and a mark that the profile was asked (decision 3); the two lists a table of values: database version 10, `AutoMigration(9, 10)`.
- Holding detail has two tabs; the third is new. The split history: every split of the holding, newest first, with its date and ratio, as the Transactions tab already lists them among the others.

## 5. Files
- core/: Classification.kt (new, K10: the two lists, D7 matching, adding a source text once, the effective value with "Unclassified"); PriceSource.kt (the profile call and its answer); TickerCheck.kt's `Instrument` (source sector and geography, the asked mark); RecordStore.kt (the lists).
- app/: prices/finnhub/FinnhubSource.kt (profile2 and its parsing); prices/PriceRefresh.kt (the profile asked once per instrument, decision 3); data/AtsDatabase.kt (instrument columns, the list table, version 10), data/RoomRecordStore.kt, schemas/…/10.json; ui: `HoldingTab.CLASSIFICATION`, HoldingDetailModel and HoldingDetailScreen (the tab); strings.xml (section 12); build.gradle.kts (versionCode 32).
- Tests: K10 (D7 matching, a text added once, the effective value, "Unclassified", "N/A" and empty as no value); the profile answer's parsing (stock, ETF `{}`, "N/A", empty country, no field); the profile asked once (not again for `{}`); RowMappingTest; the ViewModel wiring; the tab's model; screen tests (AC-61 (d) for the tab's rows).
- Documents: this file, plan.md (section 4's rows 23a and 23b, section 11 and the change log, with #163's merge line).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-23a-sector-geography-source, one pull request.
1. This file and the plan.md lines (with #163's merge line).
2. The look-up R1, with your yes.
3. K10 in core: the lists, D7 matching, adding a source text, the effective value; tests first, shown red.
4. The store: the instrument columns, the list table, version 10, 10.json compared with 9.json; RowMappingTest first.
5. K4: the profile call in FinnhubSource and its parsing, asked once per instrument at a refresh (decision 3), its texts passed to K10; tests first.
6. Holding detail's Classification tab with the split history; the model first; the strings.
7. The screen tests.
8. Documents, versionCode 32, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. More calls to Finnhub (60 a minute on the free plan): the profile is asked once per instrument (decision 3), so a refresh after the first adds none; the first refresh after the install asks one profile per held instrument.
- 2. "N/A", an empty text or a missing field is no value (decision 2), so no meaningless value enters a list.
- 3. D7 matching ("Technology" and "technology " are one value) must hold for every added text, or a list gets duplicates that 25's rules forbid.
- 4. The migration 9 to 10 on the phone: 10.json against 9.json; stop at the unreadable-data screen (CLAUDE.md; open item 34).
- 5. A profile call that fails (offline, a refused key, 429, 5xx) must not mark the instrument as asked, so the next refresh asks again, and must not stop the prices.
- 6. The new tab at the largest font and in landscape, and its TalkBack text (AC-39, AC-61).
- 7. Never log a ticker, a sector or a country (spec 9); the key in the header only.

## 9. Proof
- Commands: as in 22, all with --rerun-tasks. The screen tests on the pull request: 57 plus this iteration's new ones.
- Unit tests: steps 3 to 6.
- The phone check, over 22a's build (versionCode 31): the data opens after the install (AC-35, the migration to version 10); after a refresh, a stock holding's Classification tab shows the source's sector and country "From the source", an ETF "Unclassified", and the split history or its plain line; the tab order and the opening tab (Transactions) as before; offline, the tab shows what is stored and nothing breaks; largest font and landscape; TalkBack; light and dark; logcat as in 22 (no ticker, sector or country in it, the key not in the buffer).

## 10. Research (step 2; each look-up shown to you first, CLAUDE.md section 8)
- R1: `/stock/profile2` today: one call for AAPL and one for SPY, with the key in the header, nothing saved, to check that the fields and the ETF answer are as on 2026-10-30. Or you paste the Company Profile 2 page. It decides step 5's parsing.

### 10.1 Results (2026-11-11, step 2)
- R1, your yes: two calls at 2026-11-11 01:15:41 +08:00, `Invoke-WebRequest` with the key in the `X-Finnhub-Token` header only (set: True; never printed), redirects off, nothing saved. AAPL: status 200, fields ticker, name, country, currency, estimateCurrency, exchange, ipo, marketCapitalization, logo, shareOutstanding, finnhubIndustry, phone, weburl, floatingShare; `finnhubIndustry` "Technology", `country` "US", `exchange` "NASDAQ NMS - GLOBAL MARKET", `currency` "USD". SPY: status 200, an empty object (no field). Both as on 2026-10-30 (the price-source note, call 5 and e). Step 5 reads `finnhubIndustry` as the source sector and `country` as the source geography; an empty object, a missing field, an empty text or "N/A" is no value (decision 2).

## 11. Decisions (settled by you 2026-11-11)
1. (Plan) 23 split before it starts: 23a (this file: the source values on Holding detail) and 23b (Overall's Sector and Geography, Net Worth (Total)'s and Allocation's two breakdowns), each its own file, branch and pull request (your A). Not chosen: (B) one iteration.
2. (Build) "N/A", an empty text or a missing field is no value: the holding is "Unclassified" for it and nothing is added to a list (your A). Not chosen: (B) every text a value, "N/A" included.
3. (Build) The profile is asked once per instrument, at the first refresh after it has none, and marked as asked, so an ETF's `{}` is not asked again; asked again only when the source changes (30) (your A). Not chosen: (B) at every refresh, with each price.
4. (Build) The remembered-texts table comes with rename in 25; in 23a a source text maps by name (D7) or is added (your A). Not chosen: (B) the table now, empty until 25.

## 12. Wording to approve (D9)
To be shown on the phone in step 8:
- The tab "Classification"; its rows "Sector" and "Geography" with the value and "From the source" or "Unclassified"; the heading "Split history", each split's date and ratio, and the plain line when there is none (for example "No splits recorded.").
- TalkBack for the rows, for example "Sector, Technology, from the source".

## 13. Status and notes
- Draft 2026-11-11 as iteration 23; approved; decisions 1 to 4 settled, and 23 split into 23a and 23b (decision 1).

### 13.1 Steps (2026-11-11)
- Step 1 (c7f3738): this file, 23 split into 23a and 23b in plan.md section 4, and the plan.md lines (with #163's merge line).
- Step 2 (40a5698): the look-up R1 (section 10.1): profile2 for AAPL and SPY answered as on 2026-10-30.
- Step 3 (5cc2969): core/Classification.kt (K10): `ClassList` (sector, geography); `ClassOrigin` (from the source, unclassified; the override comes in 24); `sourceText` (trimmed, or no value when missing, blank or "N/A", decision 2); `valueFor` (the list's own spelling, matched with D7's key, `Portfolios.nameKey`, as portfolio names are); `withSource` (a new text added once, at the end); `effective` (the list's value, the source's text while the list does not hold it yet, or "Unclassified"). No remembered text is looked up before 25 (decision 4). ClassificationTest 7. Red against a stub first: 7 tests, 6 failed (a no-value text adding nothing passed on the stub, which adds nothing). Tests 926.
- Step 4 (6f3de59): `Instrument.profile` (`SourceProfile`: sector and geography texts, null until asked, decision 3); `InstrumentRow`'s `sourceSector`, `sourceGeography` and `profileAsked` (a text without the mark unreadable); `withPriceOf` became `withStoredOf`, which keeps the profile as well as the price when a buy saves its instrument again (without it, a buy would have wiped the profile); the `list_values` table (`seq`, `list`, `value`) read as `ClassLists` by `RecordStore.loadClassLists`, an unknown list or two matching values (D7) in a list unreadable; database version 10 with `AutoMigration(9, 10)`. No unique index on the table: `Index` would be the database's first and its room3 package was not checked; K10 adds only unmatched values and reading refuses two that match. 10.json against 9.json: six tables the same, `instruments` with the three new columns only (`profileAsked` NOT NULL DEFAULT 0, so existing instruments are asked once at the next refresh), `list_values` new. RowMappingTest 7 new (the existing re-save test keeps its assertions, under `withStoredOf`). Red against stubs first: 7 tests, 6 failed (never asked passed on the stub, which writes no profile). Tests 933.
- Step 5 (e2c94d4): core's `ProfileAnswer` (Given, Unreadable, KeyRefused, TooManyCalls, NoAnswer, NoKey, as `Quote`) and `PriceSource.profile`; `FinnhubProfile.read` (an object's `finnhubIndustry` and `country` as value texts, decision 2; an ETF's `{}` Given with neither; anything not an object, or an object carrying "error", Unreadable, so an error answer never marks an instrument asked) and `FinnhubSource.profile` (`/stock/profile2`, the key in the `X-Finnhub-Token` header only, no redirect followed, as `/quote`); `PriceRefresh.run` asks, after every quote, the profile of each ticker not yet asked (decision 3), keeps only Given answers, stops on an answer that would be the same for the next ticker, and a profile without an answer fails neither a price nor the refresh (risk 5); `applied` sets each given profile; `savePrices` saves the profiles and the texts they add to the lists (`Classification.additions`) in the same transaction, a ticker with no stored instrument adding nothing. The ViewModel passes the instruments not yet asked. `FakeSource` answers `{}` unless told otherwise; `FakeStore` saves profiles and lists as RoomRecordStore does. Tests: ClassificationTest 1, FinnhubProfileTest 7, PriceRefreshTest 5, PriceRefreshFlowTest 3. Red against stubs first: 16 tests, 13 failed (an empty key, offline and an instrument already asked passed on the stubs, which ask nothing). Nothing logged. Tests 949.
- Step 6 (25598f4): Holding detail's third tab, Classification (frame C): Sector and Geography, each with its value and "From the source", or "Unclassified" (`HoldingDetail.classification`, from the instrument's profile through the lists); "Split history", every split of the holding, newest first, a reversed one marked "(reversed)" (your B, asked in this step: not chosen, (A) only the splits in effect), or "No splits recorded."; each row read by TalkBack as one line. The state holds the lists, loaded with the records and extended after each refresh by the same additions the store makes (`Classification.extended`, used by `FakeStore` too). The "Iteration 23a" strings. ClassificationModelTest 4, HoldingDetailFlowTest 1 (CR2's wiring test), ClassificationTest 1 (`extended`). Red against stubs first: of the 5 app tests, 3 failed ("Unclassified" and "no split" passed on the stub, which returns them); `extended`'s test was written with its code, not shown red: it only folds `additions`, which was. Tests 955.
- Step 7 (eee644c): ClassificationTalkBackTest (AC-61 (d), AC-12), 4 screen tests on HoldingDetailScreen with a stock whose source gave "technology" and no country: Sector reads "Sector, Technology, from the source" (the list's spelling) and Geography "Geography, unclassified"; with a reversed 2-for-1 and a 3-for-1, the history reads "2026-03-01 Eastern Time, split, 2 for 1, reversed" and "2026-05-01 Eastern Time, split, 3 for 1", and not "No splits recorded."; with no split it reads that line; Classification is the third tab, right of Transactions and Lots, and a tap selects it and shows its rows. Not run here (no emulator on this PC since 2026-11-06): compiled with `:app:compileDebugAndroidTestKotlin`, and not shown red, since the tab was built in step 6; they first run in the pull request's screen-tests workflow. Screen tests 61 (57 + 4). The full run's first try was red on HoldingsScaleTest alone (`theCodeAfter15c_growsWithinItsBounds`, "3 lots | SPREAD: grew 8.9 times, bound 8") with core unchanged; re-run unchanged, green (16b decision B). Tests 955 (1 skipped).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step. Each step committed after its step (your "commit").

### 13.2 Step 8 (2026-11-11)
- versionCode 32; full run (spotlessCheck, build, tests and lint with --rerun-tasks, 2m 6s, ending 09:19) green, 955 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 09:19:28). APK 43,032,951 bytes, built with the price-source key set (checked True/False only). Database version 10 (10.json compared with 9.json in step 4).

### 13.3 Step 8: the phone check (2026-11-11, all yes)
versionCode 32 installed over 22a's build (versionCode 31; `adb install -r`, Success; lastUpdateTime 2026-11-11 09:21:29).
- 1 (AC-35, risk 4): the data opened after the install and the migration to version 10, no unreadable-data screen; the bar in the order set, the app on the same tab as before, Portfolios' portfolios and values as before, the refresh at opening finished: yes.
- 2 (view 6, AC-12, decision 3): a stock's Holding detail opened on Transactions, the tabs Transactions, Lots, Classification; Classification showed Sector "Technology" and Geography "US", each with "From the source", after the refresh at opening: yes.
- 3 (C14, decision 2): an ETF's Classification: Sector and Geography "Unclassified", no "From the source": yes.
- 4 (C9; step 6's B): with no split "Split history" and "No splits recorded."; on a holding with splits every split newest first, each date with "ET" and its ratio, a reversed one marked "(reversed)", the same splits as its Transactions tab: yes.
- 5 (decision 3, risk 1): a second refresh: the values the same, prices refreshed as usual: yes.
- 6 (AC-42, risk 5): offline, a pull to refresh: Sector and Geography still shown, the one refresh-failed line, no pop-up, prices stale: yes.
- 7 (AC-39, risk 6): largest font and landscape: the Classification tab's rows and split rows in full, the header giving way: yes.
- 8 (AC-39, AC-61): TalkBack: "Sector, Technology, from the source", "Geography, US, from the source", "Split history" as a heading, an ETF's "Sector, unclassified", a reversed split with ", reversed", the tab "Classification": yes.
- 9 (AC-40): light and dark: yes.
- 10 (AC-09): the split history's dates with "ET" and no note: yes.
- 11 (spec 9, risk 7): `adb logcat -d --uid=10454 -T '2026-11-11 09:21:29.000'`: 94 lines, 09:30:18 to 09:35:04; the process start from the events buffer at 09:23:18.571 (`am_proc_start`, uid 10454, MainActivity). The key not in the buffer (a True/False test); no AAPL, SPY, "Technology", "finnhub", "token", "country", "sector", "industry", "http" or any amount; the "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; all lines platform tags (WindowOnBackDispatcher, InsetsController, ImeTracker, DecorView, Surface, VRI, ContentCaptureHelper); `E/BiometricPromptCompat` "Unable to cancel authentication. BiometricFragment not found." at 09:34:41 as the window went hidden (as in 19, 21 and 22): yes. Not covered: the uid's lines from 09:23:18 to 09:30:18, when the refresh at opening asked the profiles, were no longer in the buffer.
- 12 (D9): the 11 "Iteration 23a" strings (section 12) approved as shown.

### 13.4 AC-62 and the screen tests
From 23a, the TalkBack text of the Classification tab's rows (a source value, an unclassified one, a split, a reversed split, the plain line) and the tab's place and opening are checked by ClassificationTalkBackTest.kt (androidTest); TalkBack's speech and the heading stay hand checks (13.3, check 8; AC-62).
- Pull request #166 at e01e446 (created by me on your word, this once): the screen tests (run 42941014073) 61 of 61 at the first attempt (run 01:43:32Z to 01:51:29Z), the 4 new ones all passing at their first run (0.8 to 2.1 s each); CI (run 42941014172, 3m 17s) and CodeQL (run 42941011492) green (gh, read only; the results downloaded to the scratchpad, the testcases counted from the result XML).

### 13.5 Review against the spec refs (2026-11-11, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: a source text mapped to a list value by name with D7's key or added once, and "N/A", an empty text or a missing field no value (4.9, decision 2; ClassificationTest); "Unclassified" for a holding with no value, an ETF's `{}` included (C14; check 3); the profile asked once per instrument, only a Given answer marking it asked, a failed profile call failing neither a price nor the refresh (decision 3, risk 5; PriceRefreshTest); `/stock/profile2` with the key in the `X-Finnhub-Token` header only and no redirect followed (`prepared` sets `instanceFollowRedirects = false`; spec 9, risk 7); the profile in the prices' transaction, a ticker with no stored instrument adding nothing; a stored text without the asked mark, an unknown list or two matching values unreadable, the messages naming no value (spec 9); the migration adding three columns and one table only (10.json; check 1); the third tab, Holding detail opening on Transactions, every split newest first with a reversed one marked (view 6, C9; checks 2 and 4); the tab drawn from its model (CR2); no logging and no floating point in the added code. Findings, by severity:
1. Low (boundary, CLAUDE.md section 4: K4 only writes prices and instrument data): K4's `savePrices` also inserts the lists' new values (AtsDatabase.kt, `InstrumentDao.savePrices`). What is added is K10's rule (`Classification.additions`), written in the prices' transaction so a profile and its list values are saved together or not at all. Your A: kept and recorded (section 14). Not chosen: (B) a separate K10 save after the refresh.
2. Low (maintainability): `ListValueDao.insert` is never called, and `InstrumentDao.loadListValues` repeats `ListValueDao.loadAll`. Open item 57, to tidy in 23b.
3. Info: when the quotes stop on a refused key or too many calls (401, 403, 429), the profile loop still makes one call, gets the same answer and stops: one extra call on such a refresh, prices not affected. Recorded only.
Not checked: the logcat minutes in which the first refresh asked the profiles (13.3, check 11).

## 14. Departures from the plan
- Step 4: `withPriceOf` became `withStoredOf`, which keeps the profile as well as the price when a buy saves its instrument again; no unique index on `list_values` (`Index` would be the database's first, and its room3 package was not checked; K10 adds only unmatched values and reading refuses two that match).
- Step 5: `savePrices` gained a profiles argument, so the profiles and the list additions are saved in the prices' transaction; the profile loop runs after every quote; a Finnhub answer carrying "error" is Unreadable, so it never marks an instrument asked.
- Step 6: the split-history question was asked during the step (your B: every split, a reversed one marked); `Classification.extended` added for the state's lists, its test not shown red (it folds `additions`, which was).
- Step 7: the screen tests were not shown red (the tab was built in step 6) and not run here; they first run on the pull request.
- Review (13.5, finding 1, your A): the lists' new values are written by K4's price save, in its transaction, by K10's rule; CLAUDE.md section 4's "K4 only writes prices and instrument data" is kept in that sense only. Pull request #166 was created by me on your word, this once (CLAUDE.md section 8 leaves it to you).
