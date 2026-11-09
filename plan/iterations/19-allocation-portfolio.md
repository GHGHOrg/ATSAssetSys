# plan/iterations/19-allocation-portfolio.md

Stage: Build. Status: approved 2026-11-09 (your "Approved"); decisions 1 to 3 settled the same day (section 11). Built 2026-11-09 and 10 (steps 1 to 6); phone check 2026-11-10 all yes (section 13.3); the screen tests 37 of 37 on pull request #153 (section 13.4); reviewed against the spec refs (section 13.5); merged as 8b93443, main green (section 13.6).
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
Approved on the phone 2026-11-10 (your A, check 10), as in strings.xml's "Iteration 19" block: the title "Allocation" (also More's entry and the link "Allocation >", read as "Allocation, opens a screen"); the chooser's label and card title "Portfolio" and its TalkBack "Portfolio, <name>"; "Portfolio value" and its TalkBack "Portfolio value, <amount> dollars" with the approved age line; the $0.00 line "No value to show in this portfolio" (on the phone only if a portfolio was worth $0.00; the screen tests show it). Reused: "No portfolios yet", "Create a portfolio", "Set the opening balance first", "By asset type", "By holding", 18's rows and colors.
Proposed in step 1, to be shown on the phone in step 6:
- The title "Allocation"; the chooser's label "Portfolio" and its TalkBack "Portfolio, <name>"; the card's title.
- "Portfolio value" with the age line (16b's approved form); "By asset type" and "By holding" (18's).
- The $0.00 line: "No value to show in this portfolio" (wireframe 04 frame D's placeholder).
- "No portfolios yet" and "Create a portfolio" (07's, reused); "Set the opening balance first" (reused).
- The link on a portfolio's screen "Allocation >" and More's entry "Allocation".

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 3 settled.

### 13.1 Steps 1 to 5 (2026-11-09)
- Step 1 (6e8539e): this file and the plan.md lines (with pull request #152's merge line). Full run green, 797 tests.
- Step 2 (f865efe): core's `Allocation.portfolioByAssetType` (stocks and ETFs, always both, no cash) and `portfolioByHolding` (one slice per holding, by ticker, only what has value, no cash), built on 18's net worth slices with cash at $0.00. PortfolioAllocationTest 3 (core): Example A, Stocks 100.0% and ETFs 0.0%, AAPL 100.0%; an ETF at its price, a stock at cost, a sold-out holding without a slice; a portfolio with only a sold-out holding. Red against a stub first: 3 tests, 3 failed. Green 23:03 to 23:05. Tests 797 to 800.
- Step 3 (94da9f8): ui/PortfolioAllocationModel.kt (`PortfolioAllocation.of`: the chooser's portfolios, the shown one or the first, its value with the age line, the refresh state, the breakdowns or none at $0.00, nothing with no portfolio); `Allocations.ofPortfolio`, sharing 18's row builder; `Screen.ALLOCATION` with the bottom bar; `openAllocation` (from a portfolio's screen on it, from More on the first), `closeAllocation` (back to that portfolio or to More), `openPortfolioChoice`, `closePortfolioChoice`, `choosePortfolio`; Create a portfolio and "Set the opening balance first" from this screen, the new portfolio shown after Create; an unlock or a tab change closes the chooser. PortfolioAllocationModelTest 5, PortfolioAllocationFlowTest 5. Red against stubs first: 10 tests, 9 failed (no portfolio passed on the stub's empty model). Two style fixes (one test helper's line). Green 23:14 to 23:16. Tests 810.
- Step 4 (04091cd): ui/PortfolioAllocationScreen.kt; the choice layer and its button made generic (`ChoiceLayer`, `ChoiceButton`) and shared with Overall's "Allocation by", whose `AllocationChoiceLayer` stays as a wrapper (18's tests unchanged); "Allocation >" above "Hidden holdings >" on a portfolio's screen, both one shared link row; More's entry (decision 1), MoreModelTest now expecting both entries; strings.xml's "Iteration 19" block. With no portfolio before the opening balance, the screen shows "Set the opening balance first" with its button, which opens the cash form as on the Portfolios tab. One style fix (a blank line between `when` branches). Green 23:23 to 23:25.
- Step 5 (0930ee5): PortfolioAllocationScreenTest.kt (androidTest): PortfolioChoiceFocusTest (AC-61 (b); creates a portfolio from the screen when there is none), PortfolioChoiceLargestFontLandscapeTest (AC-61 (a); six portfolios at font scale 2.0 in landscape, each reached and tapped, upright again at the end), PortfolioAllocationTalkBackTest 4 (AC-61 (d)). `@Test` lines 37 (were 31). Green 23:27 to 23:29.
- Steps 1 to 5 were committed together after step 5 (your "continue, commit later", then "commit"), each from its own green run.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step.

### 13.2 Step 6 (2026-11-09 and 10)
- versionCode 28; full run 23:31 to 23:33 green, 810 tests, 0 failures, 1 skipped; lint the 5 listed findings. APK 42,752,267 bytes, built with the price-source key set (checked True/False only).

### 13.3 Step 6: the phone check (2026-11-10, all yes)
Check 1, before the install on 18's build (versionCode 27), online: your screenshots of a portfolio's screen and of More; a force stop. versionCode 28 installed over it (`adb install -r`, Success; lastUpdateTime 2026-11-10 00:03:23, first install 2026-10-29 kept).
- 2 (AC-35, AC-46): "Allocation >" above "Hidden holdings >"; the screen on that portfolio, its value and age line as on its screen; By asset type with stocks and ETFs, By holding by ticker adding up to the value, no cash; Back to the portfolio: yes.
- 3 (decisions 1 and 2; AC-33): More's entry on the first portfolio; the chooser card with the portfolios as created, closed by a tap outside and by Back; another portfolio redraws the screen; "<" to More; opened again, the first portfolio again: yes.
- 4 (decision 3): pull-to-refresh; a tab tap leaves the screen: yes.
- 5 (AC-46, AC-42): offline, "some stale", the failed line in the value card, the breakdowns kept, no pop-up; refreshed back online: yes.
- 6 (AC-39, risk 1): largest font and landscape: the chooser and the value card wrap, both donuts readable, the screen scrolls; every portfolio in the chooser reached, read and tapped; More's entries and the links: yes.
- 7 (AC-39): TalkBack: the link as a button; Back, the title, the chooser, the value with its age, the headings, each row in full and not a button; the chooser's options as radio buttons; More's entry: yes.
- 8 (AC-40, AC-09): light and dark; the time with "ET": yes.
- 9 (spec 9): `adb logcat -d -v time --uid=10454`. The first read (31 lines, 00:06:19 to 00:10:22) no longer held the start after the install. Re-check: a force stop, an unlock, Allocation from a portfolio with the chooser and a pull; then a read (93 lines, 00:07:41 to 00:12:43, the new process from 00:12:16): the key not in it (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=", amount or percentage; the two amount-like matches are the system's window insets (`VRI[MainActivity]`, read with the digits masked); one `E/BiometricPromptCompat` line from the old process at the force stop (as in 16b); the "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`: yes.
- 10 (D9): the wording approved (section 12).

### 13.4 AC-62 and the screen tests
From 19, the chooser's window focus, the chooser at the largest font in landscape and Allocation's TalkBack text are checked by PortfolioAllocationScreenTest.kt (androidTest); the donuts' labels at the largest font stay a hand check.
- Pull request #153 at 76012a8: the screen tests (run 42655091904) 37 of 37 at the first attempt (tests 225 s, run 16:28:14Z to 16:35:40Z), PortfolioChoiceFocusTest 1, PortfolioChoiceLargestFontLandscapeTest 1 and PortfolioAllocationTalkBackTest 4 among them, all passing at their first run; CI (run 42655091911, 3m 39s) and CodeQL green (gh, read only). AppStartTimingTest's "load and Overall" median 3,043 ms (18's #151: 1,997 ms); 19 does not touch the start path (risk 4), so this is the emulator's spread.

### 13.5 Review against the spec refs (2026-11-10, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: one portfolio's slices are its holdings at their price or at cost, a sold-out holding without a slice, no cash (4.8 portfolio level, view 4); asset type always lists stocks and ETFs, holding only what has value, one slice per holding (C12); the shares and amounts by largest remainder, as in 18 (4.10); the value and its age line from `PortfolioFigures`, the oldest price used in this portfolio, "some stale" (4.8, marker 2); "Updating prices" and the failed line in the value card, no pop-up (section 6, states (2) and (3)); the chooser drawn in the window, on the portfolio you came from or the first from More, not saved, Back to where you came from (marker 1; PortfolioChoiceFocusTest); the $0.00 line (marker 5) and "No portfolios yet" with Create a portfolio, the dialog over this screen and back to it (marker 4, 10d), "Set the opening balance first" before it exists (4.2); no logging (spec 9), no floating point and no new store write in the added code (Create uses the existing `createPortfolio`; section 14).

### 13.6 The merge (2026-11-10)
Pull request #153 merged as 8b93443 (16:52:58Z), 7 commits; the branch is gone from origin and locally. On the pull request's last commit (cd06c38) CI (run 42657988625, 2m 46s) and the screen tests (run 42657988774, 37 of 37, tests 160 s) green. On main after the merge: CI (run 42659202461, 2m 55s), CodeQL (run 42659201414) and the screen tests (run 42659202575, 37 of 37 at the first attempt, tests 151 s, 5m 55s) green (gh, read only).

## 14. Departures from the plan
1. Step 4: the choice layer and its button became generic and shared with 18's "Allocation by" (`ChoiceLayer`, `ChoiceButton`), and "Hidden holdings >" a shared link row with "Allocation >"; section 5 named only AllocationParts.kt and PortfolioScreen.kt.
2. Steps 1 to 5 were committed together after step 5 (13.1), not one commit after each step.
3. Step 6: the logcat read was done twice (13.3, check 9), the second after a short re-check, because the first no longer held the start.
