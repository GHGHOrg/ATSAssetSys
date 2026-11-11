# plan/iterations/23b-sector-geography-breakdowns.md

Stage: Build. Status: approved 2026-11-11 (your A); decisions 1 and 2 settled the same day (section 11). Built 2026-11-11 (steps 1 to 7; section 14); phone check 2026-11-11 all yes (section 13.3); pull request to come.
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
- Step 1 (010d7b1): this file and the plan.md lines (with #167's merge line). Only documents changed. The full run was red twice before it was green: first on HoldingsScaleTest alone (`theCodeAfter15c_growsWithinItsBounds`, "2 netWorth | SPREAD: 62.280 ms at 3,000, limit 60.0 ms"), then once more after 30 s with 1 failure whose test I did not read; the third run, unchanged, green (16b decision B): 955 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 11:02:02).
- Step 2 (ae7bca8): core/Allocation.kt: `SliceKey.OfClass` (a sector or geography value) and `SliceKey.Unclassified`; `byClass` (cash, then one slice per value, each holding's value from the caller, values that match (D7) merged in the list's spelling and order (decision 1), a value not on the list after them in the order of the holdings, then "Unclassified" last; only what has value; cash never "Unclassified") and `portfolioByClass` (the same with no cash). Values are matched and added through K10's `Classification.withSource` and `valueFor`, so the breakdown and the lists use one rule. ClassAllocationTest 7. Red against stubs first: 7 tests, 7 failed. The app's `AllocationModel.nameOf` must cover the two new keys to compile; until step 3 it throws for them (section 14). Tests 962 (1 skipped).
- Step 3 (be44f1c): ui/AllocationModel.kt: `AllocationBy.SECTOR` and `GEOGRAPHY` after Holding; `SliceName.Unclassified`; `HoldingClasses` (the lists and each instrument's profile; a holding's effective value through K10's `Classification.effective`, as the Classification tab shows it); `Allocations.of` and `ofPortfolio` build the two breakdowns with core's `byClass` and `portfolioByClass`; the step 2 stub in `nameOf` replaced by the names. `PortfolioAllocation.of`: four breakdowns in view 4's order. The state passes `holdingClasses` (the stored lists and profiles) to both levels. Because Overall's choice and Net Worth (Total)'s stack take every `AllocationBy`, the enum's two entries need their option and heading strings and the "Unclassified" slice name to compile: AllocationParts.kt and the "Iteration 23b" strings came with this step (section 14). Tests: AllocationModelTest 3 (by sector with "Unclassified" last and 80.8%, 10.8%, 8.4%; by geography; not asked yet), AllocationFlowTest 2 (CR2's wiring test: the stored profile reaches both levels; a refresh that gives the profile changes the breakdown); PortfolioAllocationModelTest's AC-46 test extended to the four breakdowns (Technology and "US" at 100.0%; no assertion removed). Red against stubs first: 7 tests, 7 failed (the 5 new, the extended AC-46 test, and AllocationModelTest's 100% test, which takes every `AllocationBy`). A first test run gave a Room (KSP) "type not present" error from running the app's tests alone with --rerun-tasks; with core built first it ran as expected. Tests 967 (1 skipped).
- Step 4 (359a679): the three screens checked by reading: Overall's choice layer lists every `AllocationBy` (now five; it scrolls when taller than the space, defect 18-6) and its button reads "Allocation by, <option>"; Net Worth (Total) draws every `AllocationBy` in order under its heading; Allocation draws its model's four in order under the same headings; each row's name comes from `sliceName`, "Unclassified" included. No drawing code changed; the doc comments of NetWorthScreen.kt, NetWorthModel.kt and PortfolioAllocationScreen.kt that said sector and geography "follow in 23" now say what is drawn. The first full run failed in core's test fixtures (ScaleHistories.kt, unchanged, could not see core's own classes) and in Room's KSP; after `.\gradlew.bat --stop` (CLAUDE.md's daemon pitfall) the same run, unchanged, was green. My reading at the time, a broken Gradle or Kotlin daemon, was disproved in step 5 (its line). Tests 967 (1 skipped).
- Step 5 (80cfdd2): open item 57. `ListValueDao` taken out (its `insert` was never called, its `loadAll` repeated `InstrumentDao.loadListValues`); `RoomRecordStore.loadClassLists` reads through `InstrumentDao.loadListValues`, the query `savePrices` already uses in its transaction. A DAO is not part of the schema: 10.json unchanged. No behaviour change and no new test; RowMappingTest and the 23a flow tests stay green. The full run failed three times in a row at `:core:compileTestFixturesKotlin` (ScaleHistories.kt, unchanged, without core's own classes), also after `.\gradlew.bat --stop`, while that task alone passed; with `--no-configuration-cache` the same command was green: 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 11:38:28). Looked into on your C (2026-11-11), cause not found: with `--info`, one run failed the same way and one was red only on HoldingsScaleTest ("2 netWorth | SPREAD: 92.289 ms at 3,000, limit 60.0 ms", under the slower logging); in the failing one, `:core:compileKotlin` reported success after retrying its Kotlin daemon connection, the app's dexing then found core's `Allocation.class` and others "removed" from core's output, and the test fixtures and Room's KSP could not resolve core's classes, though core's output and core.jar held all 239 classes when the run ended. Two runs with Kotlin compiling in Gradle's process (`-Pkotlin.compiler.execution.strategy=in-process`): the first failed the same way with a freshly stored configuration cache entry, the second was green. So neither the Kotlin daemon nor a reused cache entry is the cause. Untested leads: the VS Code "Gradle for Java" extension's server (running since 09:15) working in the same folder, and tasks of one project running together under the configuration cache. Your C: the usual command; a red run is re-run unchanged after reading its cause. Step 5's closing run, the usual command: green, 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 11:53:20).
- Step 6 (8a35fa1): screen tests (AC-61 (a) and (d)). AllocationTest.kt: AllocationChoiceLargestFontLandscapeTest takes all five options (Sector and Geography added; each scrolled to, shown and tapped at font scale 2.0 in landscape); AllocationTalkBackTest 2 new (Net Worth (Total) shows "By sector" and "By geography" and reads "Technology" and "US" at $1,280.00 and 11.8% in full; Overall by Sector reads the choice as "Allocation by, Sector", AAPL with no value as "Unclassified" at 11.8% and cash on its own row). PortfolioAllocationScreenTest.kt: PortfolioAllocationTalkBackTest 2 new (Portfolio P's "By sector" and "By geography" with "Technology" and "US" at 100.0%; not asked yet, both read "Unclassified"). Each test's state gains AAPL's profile and the lists, empty by default, so the existing tests are unchanged. Not run here (no emulator on this PC): compiled with `:app:compileDebugAndroidTestKotlin`, and not shown red, since the screens were built in steps 3 and 4; they first run in the pull request's screen-tests workflow. Screen tests 65 (61 + 4; the largest-font test widened, not added). The usual command green at the first try: 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 11:58:52).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step. Steps 2 to 4 were committed after step 4 and steps 5 and 6 after step 6 (your "continue, commit later", then "commit" and your A), each from its own versions through the index, each diff stat checked against its step's report first.

### 13.2 Step 7 (2026-11-11)
- versionCode 33. The usual command was red four times: the test fixtures (as in step 5) twice, HoldingsScaleTest twice ("7 Hide | SPREAD: 20.533 ms at 3,000, limit 15.0 ms"; "8 effects | ONE_HOLDING: 100.387 ms at 3,000, limit 85.0 ms"). CPU load then 19%, with Chrome, VS Code, Task Manager and the VS Code Kotlin extension's `intellij-server` (1.4 GB) running. Your A: the usual command with `--continue`, green at its first run (1m 51s): 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 12:19:02). APK 43,034,547 bytes (12:18:20), built with the price-source key set (checked True/False only). No schema change: database version 10.

### 13.3 Step 7: the phone check (2026-11-11, all yes)
versionCode 33 installed over 23a's build (versionCode 32; `adb install -r`, Success; lastUpdateTime 2026-11-11 12:26:55).
- 1 (AC-35): the data opened, no unreadable-data screen, Overall's net worth as before: yes.
- 2 (AC-43, view 1): "Allocation by" on Asset type; the five options in order: yes.
- 3 (AC-12, AC-32, decision 1): by sector, Cash, then the values in the order they came from the source, "Unclassified" last; the amounts adding up to net worth and the percentages to 100.0%; Cash never under "Unclassified": yes.
- 4 (decision 2): by geography, the values as the source spells them ("US"), "Unclassified" last: yes.
- 5 (view 1): after a lock and unlock, back on Asset type: yes.
- 6 (AC-44): Net Worth (Total)'s five breakdowns in order, sector and geography as on Overall: yes.
- 7 (AC-46): Allocation's four breakdowns, no Cash row, each 100.0%; the chooser redraws them; a portfolio worth $0.00 shows the plain line: yes.
- 8 (AC-42): offline, the breakdowns from stored data, the one refresh-failed line, no pop-up: yes.
- 9 (AC-39, risk 1): largest font and landscape: the five options reached and Geography tapped; the sector and geography rows in full: yes.
- 10 (AC-39, AC-61): TalkBack: "Allocation by, Sector"; each row's name, amount and percentage, "Unclassified" included: yes.
- 11 (AC-40, risk 2): light and dark, the slice colors and text readable, every row naming its slice: yes.
- 12 (spec 9): `adb logcat -d --uid=10454 -T '2026-11-11 12:26:55.000'`: 40 lines, 12:50:56 to 12:56:22, all platform tags (WindowOnBackDispatcher, InsetsController, ImeTracker, VRI, Surface, ContentCaptureHelper) and one garbage collection line; no AAPL, SPY, "Technology", "US", "Unclassified", "finnhub", "token", "country", "sector", "industry", "http", portfolio name or amount; the key not in the buffer (a True/False test); the "$digit" matches the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; `E/BiometricPromptCompat` "Unable to cancel authentication. BiometricFragment not found." at 12:56:22 as the window went hidden (as in 19, 21, 22 and 23a): yes. Not covered: the process start and the uid's lines before 12:50:56 (the events buffer began at 12:41:45).
- 13 (D9): the 4 "Iteration 23b" strings (section 12) approved as shown (your A).

## 14. Departures from the plan
- Step 2: a stub against section 7's "None": the app's `AllocationModel.nameOf` throws for `SliceKey.OfClass` and `SliceKey.Unclassified` until step 3, which replaces it with the slice names, tests first. Nothing reaches it in step 2: no app code builds a sector or geography breakdown yet. Writing the names in step 2 instead would have let step 3's name tests pass before they could be shown red.
- Step 3: the two new `AllocationBy` entries reach Overall's choice and Net Worth (Total)'s stack at once (both take every entry), so their option and heading strings, the "Unclassified" slice name (AllocationParts.kt) and the "Iteration 23b" strings came with step 3, not step 4. Step 4 is left with checking the three screens.
- Step 5: the local full run failed intermittently in core's test fixtures from step 4 on; looked into (your C), cause not found (13.1, step 5). From then on a red run is re-run unchanged after reading its cause; step 7's run used `--continue` (your A).
- Commits: steps 2 to 4 and steps 5 and 6 were each committed after the later step, from each step's own versions (13.1).
