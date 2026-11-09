# plan/iterations/19-allocation-portfolio.md

Stage: Build. Status: approved 2026-11-09 (your "Approved"); decisions 1 to 3 settled the same day (section 11). In build: step 1.
Source: plan.md section 4 (row 19), spec 4.2, 4.7, 4.8, 4.10, C12, section 6 (view 3, view 4, the navigation outline, More, number format and color, accessibility, error and loading states, portfolio create (10d)), AC-33, AC-46, AC-61, AC-62; wireframe 04 (frames A to D, markers 1 to 5), wireframe 03 (the links on a portfolio's screen), wireframe 10d and 10f; 18 (its core allocation, model, donut and rows, choice layer, defect 18-6); open items 48 and 51; CLAUDE.md's pitfall on layers and menus. "My reading" is an interpretation, not a test.

## 1. Outcome
From a portfolio's screen ("Allocation >") or from More ("Allocation"), a screen shows one portfolio's allocation: a portfolio chooser at the top, the portfolio value with the age of its prices, "Updating prices" and the quiet failed line, then the breakdowns by asset type (stocks and ETFs, always both) and by holding, each with its donut and rows, as shares of the portfolio value. It opens on the portfolio you came from, or the first one from More; the chooser redraws it and is not saved. With no portfolio it says "No portfolios yet" with Create a portfolio; a portfolio worth $0.00 shows $0.00 and one plain line. Back returns to where you came from.

## 2. Spec refs
- View 4 and wireframe 04: marker 1, the chooser (portfolios as created; opens on the one you came from or the first from More; not saved; a back arrow to where you came from); marker 2, the portfolio value with the age of the oldest price used in it, "updating" on the price part, the quiet failed line; marker 3, the breakdowns in the order asset type, holding (sector and geography in 23), each with a donut and rows under view 1's rules, no cash and no breakdown by portfolio, asset type always both stocks and ETFs, holding only what has value, a 100.0% slice with no gap; marker 4, no portfolio: "No portfolios yet" and Create a portfolio, which says "Set the opening balance first" before it exists (4.2); marker 5, a value of $0.00: $0.00 and one plain line.
- 4.8: the portfolio level of allocation, shares of the portfolio value; a holding with 0 shares has no slice; a base of $0.00 shows one plain line. C12: by holding, one slice per holding in that portfolio.
- 4.10: percentages to exactly 100.0% by largest remainder; row amounts as in 18 (decision 3 there).
- Section 6, view 3 and wireframe 03: a portfolio's screen links to Allocation, Transaction history and Hidden holdings. More (10f) lists Allocation. Portfolio create (10d, 4.7): the dialog over the screen it opens from, and back to it.
- Number format, accessibility (2), (5) and (6), error and loading states (2) and (3).
- AC-33 (one portfolio at a time, chosen at the top; stocks and ETFs, holding; no cash; opens on the portfolio you came from or the first from More; no portfolio; Example A at $160: $1,280 and AAPL 100%); AC-46 (one scrolling screen; the value with the age; Stocks $1,280.00 100.0% and ETFs $0.00 0.0%; holding one slice at 100.0%; no cash; the chooser redraws and is not saved; $0.00 and one plain line; no portfolio; offline the failed line; TalkBack reads each row in full; a portfolio with only a fully sold holding: $0.00 and the line); AC-61 (a), (b) and (d), AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- By sector and by geography (23), on this screen as on Overall and Net Worth (Total).
- Transaction history (27), the link's other neighbour on a portfolio's screen.
- Open item 51 (the models as kept fields): measure first.

## 4. Analysis (2026-11-09, by reading the code and the spec)
- Core's `Allocation` (18) is the net worth level: every breakdown carries cash. The portfolio level needs the same slices without cash: asset type with both stocks and ETFs always, holding only what has value; the shares by the same `tenths`.
- `PortfolioFigures.of` (16a, 16b) gives a portfolio's value, its age line and "some stale"; `AppState.priceRefresh` the refresh state. The model 18 built (`Allocations.of`, `AllocationBreakdown`) takes the rows; only the slicing differs.
- A portfolio's holdings are `holdingsOf(id)` (a hidden holding left out; it has 0 shares and would have no slice anyway).
- A new screen: `Screen.ALLOCATION`, with the bottom bar. From a portfolio's screen it keeps `portfolioId`, so Back returns to that portfolio; from More `portfolioId` is null and Back returns to More. The chooser's portfolio is its own field, so changing it does not change where Back goes.
- The chooser is a layer drawn inside the activity window, like 18's "Allocation by" card, scrolling when taller than the space (18's defect 18-6); it lists every portfolio, so it can be long.
- Create a portfolio is allowed only from the tabs (`openPortfolioDialog`), and "Set the opening balance first" is drawn only on the Portfolios tab. My reading of 10d ("back to the screen it was opened from"): on this screen the dialog opens over it, and after Create the screen shows the new portfolio (its value $0.00, so one plain line).
- More is drawn from its model since 17: the entry list gains Allocation after Net Worth (Total) (wireframe 10f's order).
- My reading: the screen's rows are not buttons (as on Net Worth (Total)); nothing opens from them.

## 5. Files
- core/: Allocation.kt (the portfolio level: asset type and holding without cash), exact.
- app/: AllocationModel.kt (the portfolio's breakdowns); ui/PortfolioAllocationModel.kt (new: the chooser's portfolios and the shown one, the value and its age, the refresh state, the breakdowns, the no-portfolio and $0.00 cases; CR2); ui/PortfolioAllocationScreen.kt (new); AllocationParts.kt (the chooser layer, sharing the "Allocation by" card's form); PortfolioScreen.kt (the "Allocation >" link); MoreModel.kt and MoreScreen.kt (the entry, decision 1); AppViewModel.kt (the screen, its portfolio, open and close from a portfolio and from More, the chooser layer, the portfolio dialog from this screen); AppContent.kt (wiring); strings.xml (section 12); build.gradle.kts (versionCode 28).
- Tests: core (Example A: stocks 100.0% and ETFs 0.0%, AAPL 100.0%; no cash; an ETF; a holding at cost; a sold-out holding without a slice; a base of $0.00); the model (the shown portfolio, the value and age line, the $0.00 case, no portfolio); ViewModel wiring tests (from a portfolio and back to it; from More on the first portfolio and back to More; the chooser redraws and is not saved, back on the portfolio you came from when opened again; Create a portfolio from the screen and the screen showing it; "Set the opening balance first" here); screen tests (AC-61 (b): the chooser keeps the window's focus; (a): the chooser at the largest font in landscape, every portfolio reachable; (d): the rows' TalkBack text; the two plain lines).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48 if a screen's coverage changes).
- No new library, no schema change.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-19-allocation-portfolio, one pull request.
1. This file and the plan.md lines (with pull request #152's merge line).
2. Core: the portfolio level of the allocation, tests first, shown red.
3. The model and the screen state (the screen, its portfolio, the chooser, the portfolio dialog from it), tests first, shown red.
4. The screen with its chooser layer, the link on a portfolio's screen and More's entry, and the strings.
5. The screen tests (AC-61 (a), (b) and (d)).
6. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The chooser can hold many portfolios: it must scroll at the largest font in landscape (18's defect 18-6); a screen test covers it from the start.
- 2. The chooser layer must keep MainActivity's window focus (CLAUDE.md pitfall); a screen test checks it.
- 3. Create a portfolio from a screen other than a tab is new: Back, a lock while the dialog is open, and the screen after Create need care; wiring tests and the phone check.
- 4. Each breakdown is worked out on each draw (open item 51); this screen is not on the start path, so AppStartTimingTest does not see it.
- 5. The emulator is not the phone (C19); the screen tests use no network.

## 9. Proof
- Commands: as in 18, all with --rerun-tasks. The screen tests on the pull request: 31 plus this iteration's new ones.
- Unit tests: steps 2 and 3.
- The phone check, over 18's build (AC-35; no schema change): "Allocation >" on a portfolio's screen opens it on that portfolio, Back returns there; More's Allocation opens it on the first portfolio, Back returns to More; the value and age line against the portfolio's screen; the breakdowns' figures against the portfolio's holdings, adding up to 100.0%; the chooser redraws, is not saved, and keeps Back's target; offline (stale, the failed line); a portfolio worth $0.00 if you have one (otherwise only the screen tests); largest font and landscape (the chooser and the donuts); TalkBack; light and dark; logcat as in 18. Create a portfolio from this screen only in the tests: the phone's data is not changed.

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-09)
1. More lists Allocation now, after Net Worth (Total); it opens on the first portfolio and Back returns to More (your A). Not chosen: (B) later.
2. The chooser is "Portfolio" and a button with the shown portfolio's name, which opens a card of the portfolios (radio buttons) drawn in the window and scrolling when long, as 18's "Allocation by" (your A). Not chosen: (B) a row of chips.
3. Pulling down on this screen refreshes the prices, as on the other price screens (your A). Not chosen: (B) none.

## 12. Wording to approve (D9)
To be shown on the phone in step 6:
- The title "Allocation"; the chooser's label "Portfolio" and its TalkBack "Portfolio, <name>"; the card's title.
- "Portfolio value" with the age line (16b's approved form); "By asset type" and "By holding" (18's).
- The $0.00 line: "No value to show in this portfolio" (wireframe 04 frame D's placeholder).
- "No portfolios yet" and "Create a portfolio" (07's, reused); "Set the opening balance first" (reused).
- The link on a portfolio's screen "Allocation >" and More's entry "Allocation".

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 3 settled.

## 14. Departures from the plan
None yet.
