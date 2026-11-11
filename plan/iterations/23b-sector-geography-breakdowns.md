# plan/iterations/23b-sector-geography-breakdowns.md

Stage: Build. Status: approved 2026-11-11 (your A); decisions 1 and 2 settled the same day (section 11).
Source: plan.md section 4 (row 23b; 23 split by the 23a file's decision 1), spec 4.8 (allocation at both levels), 4.9 (effective value), C12, C14, section 6 (view 1, view 2, view 4, number format, accessibility (2), (5) and (6)), section 14 (K3, K10), AC-12 (allocation part), AC-32, AC-43, AC-44, AC-46, AC-61, AC-62; wireframes 01 (marker 3), 02 (markers 3 to 5 and the "Unclassified" note), 04; iterations 18, 19 and 23a; open item 57. "My reading" is an interpretation, not a test.

## 1. Outcome
Overall's "Allocation by" offers Sector and Geography after Holding; Net Worth (Total) stacks five breakdowns (asset type, portfolio, holding, sector, geography) and Allocation four (asset type, holding, sector, geography). A sector or geography slice is each holding's effective value from 23a; holdings with no value form one "Unclassified" slice. Cash stays its own slice and is never "Unclassified".

## 2. Spec refs
- 4.8: net worth level by asset type, portfolio, holding and sector/geography, cash its own "Cash" slice in every breakdown; portfolio level by asset type, holding and sector/geography, no cash; a holding with 0 shares has no slice; a base of $0.00 shows one plain line.
- 4.9 and C14: the effective value is the override (24), else the source's value, else "Unclassified"; in 23b, the source's value, as on the Classification tab.
- Section 6: view 1 (the dropdown with Asset type, Portfolio, Holding, Sector, Geography; Asset type first, not saved; the other options list only what has value), view 2 (the five breakdowns in that order), view 4 (the four in this order: asset type, holding, sector, geography); number format (each breakdown exactly 100.0%); accessibility (2) and (6) (every row reads its name, amount and percentage in full).
- AC-12 (allocation part: allocation by sector/geography uses these values); AC-32 (cash never "Unclassified"; each breakdown adds up to 100%); AC-43, AC-44, AC-46 (Example A at $160: Technology 11.8% beside Cash 88.2% on Net Worth (Total); Portfolio P's Technology and its geography at 100.0% on Allocation; decision 2); AC-61, AC-62. Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The override (24): the breakdowns use the source's value and "Unclassified" only; the core API takes a value per holding, so 24 changes only what is passed in.
- Rename, delete and remembered texts (25): until then a source text shows as the source spells it, for example "US" (decision 2).
- The backup file (31).

## 4. Analysis (2026-11-11, by reading the code, the spec and the 23a file)
- `Allocation` (core, K3) has `byAssetType`, `byPortfolio`, `byHolding` and the portfolio level's `portfolioByAssetType` and `portfolioByHolding`; `SliceKey` has Cash, Type, OfPortfolio and OfTicker. Sector and geography need a key for a class value and one for "Unclassified".
- `AllocationBy` (ASSET_TYPE, PORTFOLIO, HOLDING) drives Overall's choice layer and Net Worth (Total)'s stack through `AllocationBy.entries`, and the portfolio level lists ASSET_TYPE and HOLDING by hand (`PortfolioAllocation.of`). Adding SECTOR and GEOGRAPHY to the enum gives the first two; the third needs its list extended.
- The effective values exist from 23a: `Classification.effective(lists, profile?.sector)` per instrument, the lists in `AppState.classLists`. K3 only reads them; K10 gives them.
- My reading: one sector and one geography per holding (4.9), and an override (24) is per holding, so a ticker in two portfolios may one day carry two values. The core functions therefore take each holding's value, not each ticker's, and sum by value under D7 (case and spaces ignored), so one value is one slice.
- Section 6 does not order the rows after Cash for sector and geography (decision 1).
- No schema change: the lists and profiles are stored since version 10.

## 5. Files
- core/: Allocation.kt (the sector and geography slices at both levels: one slice per value, merged under D7, in list order, and one "Unclassified" slice last; Cash first at the net worth level).
- app/: ui/AllocationModel.kt (SECTOR and GEOGRAPHY in `AllocationBy`; a slice name for "Unclassified"; `Allocations.of` and `ofPortfolio` take each holding's effective sector and geography); ui/PortfolioAllocationModel.kt (four breakdowns); ui/AppViewModel.kt (the effective values from the state's lists and profiles); ui/AllocationParts.kt (the two options and headings); strings.xml (section 12); data/AtsDatabase.kt (open item 57); build.gradle.kts (versionCode 33).
- Tests: core (Example A with Technology; two holdings with one value under D7 as one slice; values in list order; holdings with no value as one "Unclassified" slice, last, and Cash never in it; a sold-out holding without a slice; 100.0% by largest remainder; the portfolio level without Cash); the app models (the slice names, the order); ViewModel wiring (the effective values reach the breakdowns; a refresh that gives a profile changes them); screen tests (AC-61 (d): the sector and geography rows' TalkBack text on Net Worth (Total) and on Allocation, "Unclassified" included; the choice layer with five options at the largest font in landscape).
- Documents: this file, plan.md (section 11 and the change log, with #167's merge line), open-items.md (57).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-23b-sector-geography-breakdowns, one pull request.
1. This file and the plan.md lines (with #167's merge line).
2. Core: the sector and geography slices at both levels; tests first, shown red.
3. The app models, the portfolio level's four breakdowns and the wiring of the effective values; tests first, shown red.
4. The screens: Overall's five options, Net Worth (Total)'s five breakdowns, Allocation's four; the strings.
5. Open item 57: `ListValueDao.insert` and the repeated query taken out; no behaviour changes, the existing tests stay green.
6. The screen tests.
7. Documents, versionCode 33, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The choice layer has five options at the largest font in landscape; it scrolls since defect 18-6. A screen test and the phone check it.
- 2. Many sector values mean more slices than the seven slice colors; colors repeat, and every row names its slice (accessibility (5)), as accepted in 18.
- 3. AC-46's example names the geography "United States"; Finnhub sends "US" (23a, 10.1), so the breakdown shows "US" until a rename (25) (decision 2).
- 4. Overall works out a breakdown on each draw (open item 51); sector and geography add a pass over the holdings. AppStartTimingTest's median is compared with 23a's run.
- 5. The emulator is not the phone (C19).

## 9. Proof
- Commands: as in 23a, all with --rerun-tasks. The screen tests on the pull request: 61 plus this iteration's new ones.
- Unit tests: steps 2, 3 and 5.
- The phone check, over 23a's build (versionCode 32; no schema change): Overall's choice with five options; Sector and Geography with Cash, the source's values in list order and an ETF's holding under "Unclassified" last, each adding up to 100.0%; back on Asset type after an unlock; Net Worth (Total)'s five breakdowns in order; Allocation's four per portfolio, without Cash; the geography as the source spells it ("US", decision 2); offline; largest font and landscape; TalkBack (each sector and geography row with its percentage); light and dark; logcat as in 23a.

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-11)
1. After Cash, the sector and geography rows follow their list's order (the order the values were added, as Settings, Lists will show them in 25), with "Unclassified" last (your A). Not chosen: (B) alphabetical, "Unclassified" last; (C) largest first.
2. AC-46's example "United States" is read as illustrative: the breakdown shows the source's text ("US" from Finnhub) until it is renamed in 25; no spec change (your A). Not chosen: (B) AC-46's example changed to "US" in spec.md; (C) country codes mapped to names in the app.

## 12. Wording to approve (D9)
To be shown on the phone in step 7:
- The options "Sector" and "Geography"; the headings "By sector" and "By geography" (wireframes 02 and 04).
- The slice name "Unclassified", the same string as on the Classification tab.
- TalkBack, for example "Unclassified, 1,280 dollars, 11.8 percent".

## 13. Status and notes
- Draft 2026-11-11; approved the same day; decisions 1 and 2 settled.

### 13.1 Steps (2026-11-11)
- Step 1: this file and the plan.md lines (with #167's merge line). Only documents changed. The full run was red twice before it was green: first on HoldingsScaleTest alone (`theCodeAfter15c_growsWithinItsBounds`, "2 netWorth | SPREAD: 62.280 ms at 3,000, limit 60.0 ms"), then once more after 30 s with 1 failure whose test I did not read; the third run, unchanged, green (16b decision B): 955 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 11:02:02).

## 14. Departures from the plan
None yet.
