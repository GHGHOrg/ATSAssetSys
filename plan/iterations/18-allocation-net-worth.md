# plan/iterations/18-allocation-net-worth.md

Stage: Build. Status: approved 2026-11-09 (your "approve"); decisions 1 to 4 settled the same day (section 11). In build: step 1.
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
To be shown on the phone in step 7:
- The choice's label "Allocation by" and its options "Asset type", "Portfolio", "Holding"; its TalkBack text, for example "Allocation by, Asset type, button".
- The breakdown headings on Net Worth (Total): "By asset type", "By portfolio", "By holding".
- The slice names "Cash", "Stocks", "ETFs"; a portfolio's name; a ticker.
- A row's TalkBack text "<name>, <amount> dollars, <percent> percent", for example "Cash, 9,600.00 dollars, 88.2 percent".
- The $0.00 line on Overall in place of the allocation (17's "No net worth to show yet", reused unless you choose another).
- The slice colors in light and dark.

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 4 settled.

## 14. Departures from the plan
None yet.
