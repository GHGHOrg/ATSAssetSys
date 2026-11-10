# plan/iterations/23a-sector-geography-source.md

Stage: Build. Status: approved 2026-11-11 (your "Approved", as one iteration 23); decisions 1 to 4 settled the same day (section 11): 23 split into 23a (this file) and 23b (the breakdowns).
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
- Step 2: the look-up R1 (section 10.1): profile2 for AAPL and SPY answered as on 2026-10-30.

## 14. Departures from the plan
None yet.
