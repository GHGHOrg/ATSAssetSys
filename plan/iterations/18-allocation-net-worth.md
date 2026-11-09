# plan/iterations/18-allocation-net-worth.md

Stage: Build. Status: approved 2026-11-09 (your "approve"); decisions 1 to 4 settled the same day (section 11). Built 2026-11-09 (steps 1 to 7), with defect 18-6 found and fixed in step 7; phone check 2026-11-09 all yes after the fix (section 13.3); the screen tests 31 of 31 on pull request #151 (section 13.4); the app start timing compared with 17 and reviewed against the spec refs (section 13.5); merged as 57cc8cd, main green (section 13.6).
Source: plan.md section 4 (row 18), spec 4.8, 4.10, C12, section 6 (view 1, view 2, the navigation outline, number format and color, accessibility (2), (5) and (6), error and loading states), AC-32, AC-43, AC-61, AC-62; wireframe 01 (state A, marker 3) and wireframe 02 (markers 3 and 4); 17 (its model, screen and decisions); open items 48 and 51; CLAUDE.md's pitfall on layers and menus. "My reading" is an interpretation, not a test.

## 1. Outcome
Overall shows the allocation of net worth under net worth: an "Allocation by" choice (Asset type, Portfolio, Holding), a donut with a gap between slices and the percentage on each slice of 5% or more, and one row per slice with a swatch, a name and an amount, and the percentage in the row for a slice under 5%. It starts on Asset type each time the app is opened or unlocked; tapping the donut or a row opens Net Worth (Total), the choice does not. Net Worth (Total) shows the same three breakdowns stacked under the composition. Cash is its own slice everywhere; Asset type always lists cash, stocks and ETFs; with net worth of $0.00 one plain line stands in.

## 2. Spec refs
- 4.8: allocation at the net worth level, shares of net worth by asset type (cash, stocks, ETFs), portfolio and holding; cash its own "Cash" slice; a holding with 0 shares has no slice; a base of $0.00 shows one plain line.
- C12: by holding on the net worth level merges the same ticker held in several portfolios into one slice.
- Section 6, view 1 and wireframe 01 marker 3: the "Allocation by" dropdown, Asset type first and not saved; the donut with a gap; the percentage on a slice of 5% or more, in the row for one under 5%, and a 0.0% slice without an arc; a swatch, a name and an amount per row; Asset type lists cash, stocks and ETFs even at 0.0%, the others only what has value; a tap on the donut or the list opens Net Worth (Total), the dropdown does not.
- Section 6, view 2 and wireframe 02 markers 3 and 4: the breakdowns stacked under the composition in the order asset type, portfolio, holding, each with its own donut and rows under the rules of view 1.
- Section 6, number format: percentages with 1 decimal, adjusted so that each breakdown shows exactly 100.0%; allocation shares are not colored. Accessibility (2) and (6): every row's TalkBack label reads its name, amount and percentage in full ("Cash, 9,600 dollars, 88.2 percent"); the chart's figures are also text.
- AC-32 (the breakdowns on Net Worth (Total), cash its own slice, each adding up to 100%, one slice per ticker; Example A at $160: about 88.2% cash and 11.8% stocks); AC-43 (the dropdown, starting on Asset type, not saved; 88.2% and 11.8% on the donut, the rows $9,600.00, $1,280.00 and $0.00, ETFs 0.0% with no arc; 100.0%; a tap opens Net Worth (Total); TalkBack); AC-61 (b) and (d), AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- By sector and by geography (iteration 23, with the classification): not in the dropdown and not on Net Worth (Total) until then (decision 1).
- The per-portfolio Allocation screen (19).
- Open item 51 (the models as kept fields): measure first.

## 4. Analysis (2026-11-09, by reading the code and the spec)
- An instrument carries its asset type (`AssetType.STOCK` or `ETF`, set when the ticker is verified at a buy). A holding's value at its price or at cost is `Valuation.valued`; net worth and cash as of today are in `NetWorthTotal.of` (17).
- Core has no percentage rule yet. `Money.partsInCents` (11) is the largest-remainder rule for cents; the same rule in tenths of a percent gives "exactly 100.0%" (section 6). K3 owns allocation (section 14), so the slices and their shares go into core, exact (4.10).
- Section 6 orders the breakdowns but not the rows within one after Cash (decision 2), and does not say whether a row's amount is rounded alone or with the others (decision 3). Wireframe 02 marker 2 says the composition "carries the same figures as the By portfolio breakdown, by design".
- Section 6 lists five options in the dropdown; sector and geography have no data until 23 (decision 1).
- A dropdown or menu in its own window takes MainActivity's focus and loops with the cover screen (CLAUDE.md pitfall, 07): the "Allocation by" choice is drawn inside the activity window, as the ⋮ menu on a portfolio's screen is, and a screen test checks that it keeps the window's focus (AC-61 (b)).
- The choice is not saved (view 1): a field of the screen state, set to Asset type when the records load and on each unlock.
- The donut is drawn from the shown percentages (tenths), so its arcs match the figures; angles are worked out only for drawing, never for a figure (4.10).
- My reading (step 2): view 1's "the other options list only what has value" covers cash too: By portfolio and By holding list Cash only while it has value; "Cash is its own slice" means it is never merged or "Unclassified". Asset type always lists all three.
- My reading: a slice whose share shows as 0.0% has no arc but keeps its row while it has value (or always, for Asset type's three rows).
- My reading: on Overall each row is a button that opens Net Worth (Total), so TalkBack reads its figures and the action; the donut itself is left to the rows for TalkBack (accessibility (6)).
- Slice colors are new in the theme, one set for light and one for dark, each with the 3 to 1 contrast of a control outline against the background (accessibility (4)); they repeat after the last color, and every row names its slice, so meaning never rests on color (accessibility (5)).

## 5. Files
- core/: Allocation.kt (new: the slices by asset type, portfolio and holding with one slice per ticker, cash first; the shares in tenths of a percent by largest remainder, adding up to 1,000 when the base is above $0.00), exact.
- app/: ui/AllocationModel.kt (new: each breakdown's rows with name, amount, spoken amount, percentage text and whether it shows in the row, the drawn share; the $0.00 case; CR2); ui/AllocationParts.kt (new: the donut and the rows); OverallScreen.kt (the summary with the "Allocation by" choice drawn in the window, a tap to Net Worth (Total)); NetWorthScreen.kt (the three breakdowns); NetWorthModel.kt (the breakdowns); AppViewModel.kt (the choice, its reset on load and unlock, opening and closing the choice); AppContent.kt (wiring); theme/Color.kt and Theme.kt (the slice colors); strings.xml (section 12); build.gradle.kts (versionCode 27).
- Tests: core (Example A: 88.2%, 11.8% and 0.0%; percentages that need a leftover tenth, ties to the earlier row; a ticker in two portfolios as one slice; a holding at cost; a sold-out holding without a slice; ETFs; no portfolio; a base of $0.00); the app models (rows, percentage in the row only under 5%, 0.0% without an arc, the $0.00 case, the order of decision 2, the amounts of decision 3); ViewModel wiring tests (the choice starts on Asset type, is kept while the app is open, goes back to Asset type on unlock; a row opens Net Worth (Total); the choice layer closes on Back); screen tests (AC-61 (b): the choice keeps the window's focus; (d): each row's TalkBack text with its percentage, on Overall and on Net Worth (Total)).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48 if a screen's coverage changes).
- No new library, no schema change.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-18-allocation-net-worth, one pull request.
1. This file and the plan.md lines (with pull request #149's merge line).
2. Core: the slices and their shares, tests first, shown red.
3. The app models and the choice in the screen state, tests first, shown red.
4. The donut and the rows, and the three breakdowns on Net Worth (Total), with the slice colors.
5. Overall's summary with the "Allocation by" choice and the tap to Net Worth (Total).
6. The screen tests (AC-61 (b) and (d)).
7. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The percentages on the donut must stay readable at the largest font; a thin slice of 5% may be too narrow for its label. Checked on the phone; no screen test measures the donut.
- 2. The choice must keep MainActivity's window focus (CLAUDE.md pitfall); a screen test and the phone check it.
- 3. Overall works out a breakdown on each draw (open item 51); AppStartTimingTest's median is compared with 17's run.
- 4. A ticker without a stored instrument would have no asset type; my reading is that a held ticker always has one (verified at its buy, or restored with the file); the model counts such a holding as a stock, and a test covers it.
- 5. Slice colors in both themes are new; their contrast is checked on the phone (AC-40), not measured.
- 6. The emulator is not the phone (C19); the screen tests use no network.

## 9. Proof
- Commands: as in 17, all with --rerun-tasks. The screen tests on the pull request: 25 plus this iteration's new ones.
- Unit tests: steps 2, 3 and 5.
- The phone check, over 17's build (AC-35; no schema change): Overall's allocation on Asset type after the first unlock, its percentages adding up to 100.0% and its rows against Net Worth (Total)'s composition; the choice changed to Portfolio and Holding, kept while the app is open and back on Asset type after a lock and unlock; a tap on the donut and on a row opens Net Worth (Total), the choice does not; the three breakdowns on Net Worth (Total); offline (stale prices, the failed line); largest font and landscape (the donut's labels); TalkBack (each row with its percentage, the choice); light and dark (the slice colors); logcat as in 17.

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-09)
1. The choice offers Asset type, Portfolio and Holding in 18; Sector and Geography are added in 23 (your A). Not chosen: (B) all five now, with one "Unclassified" slice until 23.
2. After Cash, the rows follow the app's usual order: portfolios as created, holdings by ticker; Asset type is always cash, stocks, ETFs (your A). Not chosen: (B) largest first.
3. A row's amount is shown by largest remainder with the other rows of its breakdown, so they add up to the net worth shown and By portfolio shows the composition's figures (your A). Not chosen: (B) each rounded alone.
4. A slice's percentage of 5% or more is drawn on the ring, in the slice (your A). Not chosen: (B) beside the donut.

## 12. Wording and colors to approve (D9)
Approved on the phone 2026-11-09 (your A, check 10), as in strings.xml's "Iteration 18" block and theme/Color.kt: the label "Allocation by" and its TalkBack "Allocation by, <option>"; the options "Asset type", "Portfolio", "Holding"; the headings "By asset type", "By portfolio", "By holding"; "Cash", "Stocks", "ETFs"; a percentage "88.2%"; a row's TalkBack "<name>, <amount> dollars, <percent> percent"; 17's "No net worth to show yet" on Overall at $0.00 (seen only in the screen tests); the slice colors, cash teal, then indigo, orange, purple, blue, brown and olive, one set per theme.
Proposed in step 1, to be shown on the phone in step 7:
- The choice's label "Allocation by" and its options "Asset type", "Portfolio", "Holding"; its TalkBack text, for example "Allocation by, Asset type, button".
- The breakdown headings on Net Worth (Total): "By asset type", "By portfolio", "By holding".
- The slice names "Cash", "Stocks", "ETFs"; a portfolio's name; a ticker.
- A row's TalkBack text "<name>, <amount> dollars, <percent> percent", for example "Cash, 9,600.00 dollars, 88.2 percent".
- The $0.00 line on Overall in place of the allocation (17's "No net worth to show yet", reused unless you choose another).
- The slice colors in light and dark.

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 4 settled.

### 13.1 Steps 1 to 6 (2026-11-09)
- Step 1 (435775d): this file and the plan.md lines (with pull request #149's merge line). Full run green, 778 tests.
- Step 2 (f5c089f): core's `Allocation`: `byAssetType` (cash, stocks, ETFs, always all three; a ticker without an instrument counts as a stock), `byPortfolio` (in the order given), `byHolding` (one slice per ticker over every portfolio, C12, by ticker), a sold-out holding without a slice, a holding with no price at cost; `tenths`, the shares in tenths of a percent by largest remainder, ties to the earlier row, all 0 for a base of $0.00. AllocationTest 9 (Example A 88.2, 11.8 and 0.0; a leftover tenth to the larger remainder, 33.3 and 66.7; a tie to the earlier row, 33.4, 33.3, 33.3; a base of $0.00; a ticker in two portfolios; the given order and an empty portfolio; at cost and sold out; an ETF and a ticker without a type; no cash). Red against a stub first: 9 tests, 8 failed (the $0.00 base passed on the stub's zeros). Green 17:58 to 18:00. Tests 778 to 787.
- Step 3 (69ce803): ui/AllocationModel.kt (`Allocations.of`: rows with name, amount by largest remainder (decision 3), percentage, in the row only under 5%, the drawn share; null while net worth shows as $0.00); `AppState.allocationBy`, `allocationMenuOpen` and `allocation(by)`; `openAllocationMenu` (Overall only, not over another layer), `closeAllocationMenu`, `chooseAllocation`; Asset type and the layer closed at each unlock; a tab change closes the layer; net worth does not open while it is open. AllocationModelTest 5, AllocationFlowTest 5. Red against stubs first: 10 tests, 7 failed; the other 3 passed only because the stubs did nothing (said before the code). Green 18:05 to 18:08. Tests 797.
- Steps 4 and 5 (aff42a9, one commit; section 14): ui/AllocationParts.kt (the donut: arcs from the shown percentages, clockwise from the top, a 2° gap, a slice of 5% or more labelled on the ring (decision 4), 200 dp growing with the font up to the width; the rows: swatch, name, the percentage under 5%, amount, TalkBack "<name>, <amount> dollars, <percent> percent", a button on Overall); the slice colors (Color.kt, Theme.kt; WCAG worked out on Material's baseline tones, not measured); Net Worth (Total)'s three breakdowns; Overall's card with "Allocation by", the breakdown or the $0.00 line, a tap to Net Worth (Total); the choice layer drawn in the window, in TabContent beside the Buy/Sell layer; strings.xml's "Iteration 18" block. Step 4's first run listed 2 new lint findings, `UnusedResources` for "Allocation by" and its TalkBack text, used only from step 5: the two strings moved to step 5, not silenced. Green 18:17 to 18:19 (step 4) and 18:22 to 18:24 (step 5).
- Step 6 (497749a): AllocationTest.kt (androidTest): AllocationChoiceFocusTest (the choice keeps MainActivity's window focus and takes a tap; AC-61 (b)) and AllocationTalkBackTest 4 (AC-61 (d)). `@Test` lines 30. One style fix (a line ktlint wrapped). Gradle's Java crashed in this step's runs (4 crashes from 18:55 to 19:08, as in 17's 13.2); after a Windows restart the run was green (19:17 to 19:20).
- Steps 2 to 6 were committed together after step 6 (your "continue", then "commit"), each from its own green run.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step.

### 13.2 Step 7 and defect 18-6 (2026-11-09)
- versionCode 27; full run 19:22 to 19:24 green, 797 tests; APK built with the price-source key set (checked True/False only). Installed over 17's build (versionCode 26) at 20:05:05.
- Phone check 6, item 3: at the largest font in landscape the "Allocation by" card showed only its first option; the other two could not be read or tapped (defect 18-6). Cause: the card's options did not scroll and were taller than the space.
- Failing test first (your A): AllocationChoiceLargestFontLandscapeTest (the card above the bottom bar at font scale 2.0 in landscape; each option scrolled to, shown and tapped), committed alone (fa123cb) and pushed on throw-away-18-choice-largest-font; draft pull request #150. Its screen tests (run 42618482123), 31 of 31 run, 3 failed: the new test ("no parent layout with a Scroll SemanticsAction": the card could not scroll); AllocationTalkBackTest's Net Worth test ("No compose hierarchies found": the new test left the emulator in landscape and the rotation's return re-created the next test's activity); AllocationTalkBackTest's Asset type test (the "0.0%" in a row cannot be seen by a test, since the row replaces its texts with its TalkBack sentence: a fault of step 6's test). CI and CodeQL green. #150 closed unmerged.
- The fix (4427100): the card's options scroll and its outer padding is 16 dp; the new test turns back upright at its end; the two asserts no test could see removed (your A; section 14). Full run 20:46 to 20:48 green. The fixed build installed at 20:50:40.

### 13.3 Step 7: the phone check (2026-11-09, all yes after the fix)
Check 1, before the install on 17's build: your screenshots of Overall and of Net Worth (Total)'s composition; a force stop.
- 2 (AC-35, AC-43): the first unlock; the allocation on Asset type, the donut with gaps and its percentages, the rows with swatches, ETFs 0.0% in its row, the percentages adding up to 100.0% and the amounts to net worth: yes.
- 3 (view 1; decisions 1 and 2): the choice card, closed by a tap outside and by Back; Portfolio (as created, the composition's amounts) and Holding (by ticker, one row per ticker); kept across tabs; Asset type again after a lock and unlock: yes.
- 4 (view 1, view 2; AC-32): the donut and a row open Net Worth (Total); its three breakdowns, By portfolio as the composition, rows not buttons: yes.
- 5 (AC-42): offline, "some stale", the failed line, the allocation at the last prices, no pop-up; refreshed back online: yes.
- 6 (AC-39, risk 1): largest font and landscape: the donut, its labels and the rows yes; the choice card no (defect 18-6, 13.2); after the fix, check 6b: all three options reached, read and tapped: yes.
- 7 (AC-39, AC-43): TalkBack: the choice, the card's options as radio buttons, the rows with their percentages, the headings; after the fix, check 7b on the card: yes.
- 8 (AC-40, AC-09, risk 5): light and dark, the slice colors and the labels on the ring: yes.
- 9 (spec 9): `adb logcat -d -v time --uid=10454`, read twice. First, after checks 2 to 4 (85 lines, 20:08:15 to 20:14:39): the first start after the install had rolled out of the buffer; a second full start from 20:11:11 (`ActivityThread`) with its refresh and the checks after it is in it. Second, after checks 6b and 7b on the fixed build (27 lines, 20:50:24 to 20:52:58, its start at 20:50:40). Both: the key not in it (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=", amount or percentage; the two amount-like matches of the second read are the system's window insets (`VRI[MainActivity]`, read with the digits masked); the "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`: yes.
- 10 (D9): wording and colors approved (section 12).

### 13.4 AC-62 and the screen tests
From 18, the choice's window focus, the choice card at the largest font in landscape and the rows' TalkBack text are checked by AllocationTest.kt (androidTest); the donut's labels at the largest font stay a hand check.
- Pull request #151 at c98d713: the screen tests (run 42621928887) 31 of 31 at the first attempt (tests 190 s, run 13:02:04Z to 13:08:48Z), AllocationChoiceFocusTest 1, AllocationChoiceLargestFontLandscapeTest 1 (red on #150) and AllocationTalkBackTest 4 among them; CI (run 42621928886, 3m 2s) and CodeQL green (gh, read only).

### 13.5 App start timing and the review against the spec refs (2026-11-09, by reading main..HEAD; plan.md section 5, item 7)
- AppStartTimingTest (risk 3), 300 transactions, from the ScreenTestTiming lines in the run's artifact (one run each, emulator): "load and Overall" 2,709, 1,997 and 1,669 ms, median 1,997 ms, against 3,320 ms on 17's pull request #148 (run 42585761040; the 17 file, 13.5), −1,323 ms; main-thread CPU in that phase 660, 460 and 490 ms, median 490 against 790. Overall now draws a breakdown on each draw, so the drop is the emulator's spread, not a gain of 18. Not proven.
- No finding that needs a change. In order: the slices are cash as of today and each holding at its price or at cost, a sold-out holding without a slice (4.8); one slice per ticker on the net worth level (C12); Asset type always lists cash, stocks and ETFs, the others only what has value (view 1; reading in section 4); percentages with 1 decimal adding up to exactly 100.0%, by largest remainder, ties to the earlier row (section 6, 4.10); row amounts by largest remainder, By portfolio as the composition (decision 3); a percentage in the row only under 5%, a 0.0% slice without an arc (view 1); the choice on Overall only, drawn in the window (CLAUDE.md pitfall; AllocationChoiceFocusTest), Asset type at each unlock, not saved (view 1); a tap on the donut or a row opens Net Worth (Total), the choice does not (navigation outline); the $0.00 case (4.8); allocation shares not colored, swatches with names (section 6); exact arithmetic in core and the model (4.10): floating point only in drawing the donut, from the shown tenths; no logging (spec 9); no store write (the screens only read; section 14). Noted, not a finding: a ticker without an instrument counts as a stock (risk 4, AllocationTest); whether any path can leave a held ticker without one was not checked in this review (the restore comes in 31).

### 13.6 The merge (2026-11-09)
Pull request #151 merged as 57cc8cd (14:05:08Z), 9 commits; the branch is gone from origin and locally, as is throw-away-18-choice-largest-font (#150 closed unmerged). On the pull request's last commit (170f4ba) CI (run 42628281595, 2m 33s) and the screen tests (run 42628281507, 31 of 31, tests 150 s) green. On main after the merge: CI (run 42631222233, 3m 56s), CodeQL (run 42631221294) and the screen tests (run 42631222211, 31 of 31 at the first attempt, tests 197 s, 7m 0s) green (gh, read only).

## 14. Departures from the plan
1. Steps 4 and 5 are one commit (aff42a9): both change strings.xml and ui/AllocationParts.kt, and a file is not staged in parts.
2. Step 4: two strings ("Allocation by" and its TalkBack text) moved to step 5 after lint listed them as unused; not silenced.
3. Steps 2 to 6 were committed together after step 6 (13.1), not one commit after each step.
4. Step 7: defect 18-6 (13.2): a new screen test (AllocationChoiceLargestFontLandscapeTest), shown red first on a throw-away draft pull request (#150, closed unmerged), then the fix; not in section 5's list.
5. Step 7: two asserts of AllocationTalkBackTest removed (your A): they checked a row's percentage text, which no test can see under the row's TalkBack sentence; where a percentage shows stays covered by AllocationModelTest and the phone check.
6. Step 7: the logcat read was done twice (13.3, check 9), once per build.
