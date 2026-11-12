# plan/iterations/26-realized-gains.md

Stage: Build. Status: approved 2026-11-12; decisions 1 to 3 settled the same day (your A each; section 11).
Source: plan.md section 4 (row 26), spec section 6 (view 5; "More"), 4.4 (sells and lots), 4.6 (reversal), 4.10 (rounding, largest remainder), section 14 (K3, K9), C10, AC-11, AC-23, AC-42 (its Realized gains sentence), AC-47, AC-61; wireframe 05 (frames A to C, markers 1 to 7); spec/example-backup-expected.md (its Realized gains rows); iterations 11, 13a, 13b, 14a, 15 and 15d. "My reading" is an interpretation, not a test.

## 1. Outcome
More has a Realized gains entry. It opens one scrolling screen: a From and To date filter with the date note and Clear dates, the total realized gain with the number of sales, and "By holding", one row per holding with a sale in the range (hidden holdings included), in ticker order, with its portfolio name, number of sales and total. Tapping a row shows its sales, newest first, each with its date and "ET", gain/loss, quantity, proceeds and the cost of the lots used; a sale opens the sell. A sell cancelled by a reversal is left out whatever the range. With no sale in the range the total is $0.00 and one plain line replaces the list.

## 2. Spec refs
- Section 6, view 5: per sale, per holding and total, filtered by the sale date; no "informational" or tax label (C10); a sale cancelled by a reversal left out; From and To each optional, both ends included, not saved, empty meaning all dates, with Clear dates; the total with the number of sales; By holding in ticker order, hidden holdings included; a row expands its sales, newest first; each sale with date, gain/loss, quantity, proceeds and cost of the lots used, opening the sell in that portfolio's Transaction history; gains green with a plus sign, losses red with a minus sign, zero uncolored; no sale: $0.00 and one plain line. "More": Realized gains between Allocation and Backup/restore. Wireframe 05.
- 4.10: each figure is a sum of exact values, rounded once; a holding's sales beside its total are allocated by largest remainder; a value shown alone is rounded half away from zero; a quantity shows up to 8 places (Quantity.shown).
- Section 14: K3 works out realized gains, also for a date range, reads records and never writes them; K9 shows the date note on every date input and "ET" on every shown date.
- AC-11 (Example A: +$800 for the sale, per holding and total), AC-23 (a range containing Mar 1 shows $800; Apr 1 to Apr 30 shows no sale), AC-47 (the sample file's figures, OLDCO hidden but listed, Mar 1 to Mar 1, the empty range, the expanded sale, a reversed sale left out, TalkBack), AC-42 (the empty list as one plain line), AC-61. Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- Transaction history (27). Decision 1 settles where a sale opens until then.
- Realized gains in the backup file: none; it is worked out from the transactions (8).
- Dividends and interest: not realized gains (4.8, intent).
- A sell's lots and reversal link: already on the sell's own screen (13a, 13b).

## 4. Analysis (2026-11-12, by reading the code, the spec and the 11, 13a, 13b, 14a, 15 and 15d files)
- core already works out a sell's exact gain: `Sells.pieces(transactions, sell)` gives each lot it took with its cost, and `Sells.realizedGain` sums their gains (11; used by the reversal review, 13b). 26 adds K3's report on top: every sell not cancelled by a reversal, by holding, within a range, with its proceeds (the sell's total), the cost of its lots and its gain, each exact.
- A sell "cancelled by a reversal" is one that a reversal names (4.6). A Reverse and re-enter (14a) leaves the original reversed and the new sell counted on its own date. A reversal's own date does not matter: the sale is left out whatever the range.
- The quantity shown is the sell's quantity as entered (Quantity.shown), not split-adjusted, as the sell's own screen shows it. My reading: view 8 shows "quantity as originally traded", and view 5 names no other.
- Rounding (4.10): the total is the sum of the exact gains, rounded once; each holding's total likewise, alone; the sales under an expanded holding are allocated by largest remainder so they add up to that holding's shown total. My reading: 4.10 names only the sales beside a holding's total, so the holding rows are each rounded alone and may differ by a cent from the shown total in sum.
- The date filter: the Cash list's (06, 15d) rules and helpers: digits typed as YYYY-MM-DD, the date note beside the inputs, a field that is not a date shows its refusal line (decision 3), From after To lists nothing.
- The screen is a full screen opened from More, as Net Worth (Total) and Allocation are, with "<" back to More. No layer and no menu, so no focus or keyboard-layer test applies (AC-61); its date fields sit in the scrolling page, as the Cash list's do.
- Hidden holdings: their sales count and their rows are listed (15; wireframe 05 marker 4). A deleted portfolio's sales are gone with it (4.7), as its transactions are.
- No database change and no write: K3 reads the transactions already in the state.

## 5. Files
- core/: a K3 report of realized gains (new file, RealizedGains.kt): the sales in a range, by holding, with the totals and the counts; the largest-remainder split of a holding's sales; tests.
- app/: ui: AppViewModel.kt (Screen.REALIZED_GAINS; the date fields, the expanded holdings, opening and leaving, opening a sale), a model and its builder (RealizedGainsModel.kt), the screen (RealizedGainsScreen.kt), MoreScreen.kt (the entry), AppContent.kt, strings.xml (section 12); build.gradle.kts (versionCode 36).
- Tests: core (Example A; the sample file's figures; a range of one day; the empty range; a reversed sell left out; a re-entered sell on its own date; a hidden holding's sale; a split after the sale; several holdings in ticker order; a loss; a zero gain; largest remainder with thirds of a cent, rounding up and down); the ViewModel flows (open, the dates, Clear dates, expand and collapse, a sale opens the sell and Back returns, a reversal made there leaves the sale out on return); the model (figures, colors by sign, the count lines, the empty line, TalkBack text); screen tests (section 9).
- Documents: this file, plan.md (section 11 and the change log, with #175's merge line).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-26-realized-gains, one pull request.
1. This file and the plan.md lines (with #175's merge line).
2. K3 in core: the realized gains report and the largest-remainder split; tests first, shown red.
3. The state and the ViewModel: the screen, the dates, the expanded holdings, opening a sale and coming back; tests first.
4. The model and the screen: the builder, the screen, More's entry, the strings; models first.
5. The screen tests.
6. Documents, versionCode 36, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A reversed sell must be left out everywhere (total, count, rows, sales) whatever the range, and a re-entered one counted on its own date: core tests with both.
- 2. The sums must use exact values and round once; the sales under a holding must add up to its shown total: tests with thirds of a cent that round up and down, and several sales.
- 3. A sale opened and reversed must be gone when you come back: a ViewModel test and the phone.
- 4. Many holdings and sales at the largest font in landscape: rows and Clear dates reachable (AC-61); screen tests and the phone.
- 5. The emulator is not the phone (C19).

## 9. Proof
- Commands: as in 25, all with --rerun-tasks (CLAUDE.md's pitfall on the intermittent local failure applies). The screen tests on the pull request: 81 plus this iteration's new ones.
- Unit tests: steps 2 to 4, AC-11's, AC-23's and AC-47's figures among them.
- Screen tests (AC-61): the screen at the largest font in landscape, with many holdings and an expanded one: Clear dates, the rows and a sale reached and tapped; the TalkBack text of the total, a holding row (name, portfolio, count and total with its sign) and a sale row; the empty range's line.
- The phone check, over 25's build (versionCode 35): More's entry; the figures against the holdings' own sells (the sell screens' gains); a range of one day; an empty range; Clear dates; a field that is not a date; a hidden holding's sales listed; a sale opens the sell and Back returns as it was; a sale reversed there is gone on return (with your yes, on a sale you then re-enter or keep reversed); colors and signs; largest font and landscape; TalkBack; light and dark; offline; logcat as in 25.

## 10. Research
None.

## 11. Decisions (settled by you 2026-11-12)
1. (Build) Until Transaction history exists (27), a sale opens the sell's own screen (13a), where its lots and any reversal link show, as view 8 says a history row opens it; Back returns to Realized gains as it was. 27 then only adds the history (your A). Not chosen: (B) the sales not tappable until 27.
2. (Build) The dates and the expanded rows last while you are on Realized gains and the screens it opens; they reset when you go back to More, change tab, or open the app again (view 5: not saved) (your A). Not chosen: (B) reset each time the screen is shown, also on coming back from a sell.
3. (Build) A date field that is not a date: as the Cash list (06, 15d), its refusal line under the field, and that end counts as empty, so the list still shows (your A). Not chosen: (B) nothing listed until both fields are dates or empty.

## 12. Wording to approve (D9)
To be shown on the phone in step 6 (wireframe 05's placeholders as the first proposal):
- More: the entry "Realized gains".
- The screen: the title "Realized gains"; "From" and "To" with the date note; "Clear dates"; "Total realized gain"; "1 sale, all dates." (or "N sales", and with a range "N sales, YYYY-MM-DD to YYYY-MM-DD ET"); "By holding"; "Realized gain/loss".
- A holding row: "Portfolio P, 1 sale" (or "N sales"); its expand and collapse read by TalkBack.
- A sale: "YYYY-MM-DD ET", "Quantity", "Proceeds", "Cost of lots used".
- The empty line: "No sales in this date range" (also with no sale at all).
- TalkBack: a holding row "AAPL, Portfolio P, 1 sale, total plus 800 dollars"; a sale row with its date, gain/loss, quantity, proceeds and cost.

## 13. Status and notes

### 13.1 Steps (2026-11-12)
- Step 1: this file and the plan.md lines, commit a6dc17b.
- Step 2: K3 in core (RealizedGains.kt): `RealizedSale` (the sell, its proceeds as its total, the cost of the lots it took, its gain), `RealizedHolding` (newest first; its total) and `RealizedReport` (ticker order, then the portfolios' creation order; its total and count), each exact; a sell named by a reversal left out whatever the range. `Sells.pieces` gains an internal form taking one ledger, so the report builds the ledger once, not once per sell. The largest-remainder split stays `Money.partsInCents` (used in step 4). RealizedGainsTest's 13 tests first, red (10 failed; the three expecting an empty report passed against the stub), then green; 1,071 tests. My reading: a portfolio not in the list (none in the app) sorts last. The first full run failed in `:app:kspDebugKotlin` with `NoSuchFileException` on its own output folder, a new form of CLAUDE.md's intermittent failure; the same command re-run unchanged passed (open item 60).
- Step 3: the state and the ViewModel. `Screen.REALIZED_GAINS` (with the bottom bar); in `AppState` From and To as typed and the expanded holdings; K3's report as a kept field (`realizedGains`, KeptFields.kt, 15c's pattern: worked out again only when the transactions, the portfolios or a date change, so expanding a row runs nothing). Open from More, "<" back to More, the two fields (a ninth digit not accepted, as the Cash list's), Clear dates, expand and collapse (a holding not listed ignored); a sale opens the sell's own screen and Back returns as it was (decision 1); "<" or a tab ends the dates and the rows (decision 2), also a tab from the sell opened there. My reading of decision 2's "open the app again": an unlock keeps the screen as it was, as the Cash list's filter is kept; a new start of the app starts empty. AppContent draws nothing new until step 4 (nothing opens the screen before then). RealizedGainsFlowTest's 10 tests and KeptFieldsTest's new test and checks first, red (9 failed; opening only from More, the two resets and the report kept on expand passed against the stubs, as they check that nothing changes), then green; 1,082 tests. One style fix (an import out of order). The first full run met CLAUDE.md's intermittent failure (ScaleHistories.kt and KSP together); the same command re-run unchanged passed.
- Step 4: the model and the screen. RealizedGainsModel.kt: `GainFigure` (the signed text, its sign for the color and TalkBack), `GainsCountLine` (all dates, between, from, up to), `RealizedSaleModel` (date, gain by largest remainder with the holding's other sales through `Money.partsInCents`, quantity as entered, proceeds, cost of lots used shown alone), `RealizedHoldingModel` and `RealizedGainsModel` with the fields' date lines (the Cash list's `DateDigits.refusal`); the builder `RealizedGainsScreens.of`. RealizedGainsScreen.kt (wireframe 05: "<" and the title, From and To with the date note and Clear dates, the total and its count line, "By holding" with "Realized gain/loss", rows with "v" or "^", each sale with ">"; one plain line with no sale; Back to More). The Cash list's `FilterDateField` became internal to share it. More gains its Realized gains entry after Allocation (MoreModelTest now expects it). The strings follow section 12, wireframe 05's placeholders first; added where the wireframe has none: the count line with a range ("N sales, YYYY-MM-DD to YYYY-MM-DD ET.", "from", "up to") and the TalkBack texts. RealizedGainsModelTest's 6 tests and MoreModelTest's new entry first, red (6 failed; 1 failed), then green; the wiring test in RealizedGainsFlowTest written with them, after the state's getter (it passed at once); 1,089 tests. The first full run met CLAUDE.md's intermittent failure (ScaleHistories.kt and KSP together); the same command re-run unchanged passed.
- Step 5: the screen tests (AC-61), RealizedGainsScreenTest.kt, 4 tests on ScreenTestSupport.kt's helpers. `RealizedGainsTalkBackTest` (3): the total and each holding row read their figures with their signs, the rows as buttons; an expanded sale reads its date, gain, quantity, proceeds and cost; an empty range reads its plain line and shows no "By holding". `RealizedGainsLargestFontLandscapeTest` (1, AC-39): at the largest font in landscape above the bottom bar, thirteen holdings with AAA expanded; From, To, Clear dates, every holding row and each of AAA's sales are scrolled to, shown and take a tap, the sales newest first. The screen has no fixed part and no layer, so no focus or keyboard test applies. The workflow's count goes to 85. Compiled locally (`:app:compileDebugAndroidTestKotlin`); not run here, as the screen tests run on GitHub only. The full run passed (1,089 tests, lint's 5 findings); it printed "Compilation in Kotlin daemon has failed" and went on, the build still successful (open item 60).

## 14. Departures from the plan
None yet.
