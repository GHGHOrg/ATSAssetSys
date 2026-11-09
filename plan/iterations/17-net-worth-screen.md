# plan/iterations/17-net-worth-screen.md

Stage: Build. Status: approved 2026-11-09 (your "Yes"); decisions 1 to 4 settled the same day (section 11). In build: step 1.
Source: plan.md section 4 (row 17), spec 4.8, 4.10, section 6 (view 2, the navigation outline, More, error and loading states, number format and color, accessibility), AC-34, AC-44, AC-61, AC-62; wireframe 02 (frames A and C, markers 1, 2 and 6) and wireframe 10f (More); 16a to 16c (their decisions and wording); open items 48 and 51. "My reading" is an interpretation, not a test.

## 1. Outcome
Tapping net worth on Overall, or Net Worth (Total) on More, opens a screen with net worth, the age of its prices, "Updating prices" while a refresh runs and one quiet line when the last refresh failed, then the composition: cash and each portfolio's value, a plain list whose amounts add up to the net worth shown. With net worth of $0.00 the screen shows $0.00 and one plain line. Back returns to where you came from.

## 2. Spec refs
- View 2 and wireframe 02: marker 1, net worth with the age line, the "updating" indicator on the price part only, the quiet failed line, and a back arrow to Overall or More; marker 2, the composition, a plain list of amounts (cash plus each portfolio's value) adding up to net worth, no donut; marker 6 (frame C), net worth of $0.00: $0.00 and one plain line, no composition.
- 4.8: net worth = cash as of today + every portfolio's value; a portfolio's value at the latest price, at cost with no price; the age line is the oldest price used, a holding at cost not counted.
- 4.10: the composition's parts are shown by largest remainder, so they add up to the net worth shown (`Money.partsInCents`, built in 11).
- Navigation outline: tapping the net worth figure on Overall opens Net Worth (Total); More lists it (10f).
- Section 6, error and loading states (2) and (3); accessibility (2) and (5); number format.
- AC-44 (the net worth, the composition, the age line, offline, TalkBack, $0.00); AC-34's last sentence (portfolio values plus cash equal the net worth on Net Worth (Total)); AC-61 (d), AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The five breakdowns with donuts (wireframe 02 markers 3 to 5): asset type, portfolio and holding in 18, sector and geography in 23 (plan.md section 4). Until 18 the screen ends after the composition.
- The allocation summary on Overall and tapping it (18).
- More's other entries (Allocation 19, Realized gains 26, Backup and restore) and the gear (20).
- Open item 51 (the models as kept fields): measure first.
- An age such as "9 h old" (open item 49).

## 4. Analysis (2026-11-09, by reading the code and the spec)
- `AppState.netWorth` (a kept field since 15c, at prices since 16a) and `Overall.of` (16c) give net worth, its age line and the refresh state; `PortfolioFigures.pricesAsOf` gives the age over every holding.
- Each portfolio's exact value is the sum of `Valuation.valued` over its holdings, as `PortfolioFigures.of` adds them; that model keeps only the shown strings, so the composition needs the exact values for `Money.partsInCents`.
- Cash in the composition is the balance as of today (4.8), in whole cents, so it never takes a leftover cent; the portfolios share them.
- A screen opened from a tab: `Screen` gains NET_WORTH, opened from TABS on Overall or More; Back returns to TABS on the same tab, as `closePortfolio` does.
- MoreScreen.kt is a stub with the "never backed up" line (02b); open item 48 lists More. Changing it means it gets a model (CLAUDE.md section 4).
- Overall's net worth card is read by TalkBack as one sentence starting "Net worth"; AppStartTimingTest finds Overall by it. Making the card tappable keeps that sentence and adds the button role.
- My reading: a portfolio row on Overall rounds its value alone (4.10, a single value), while the composition rounds by largest remainder, so the two can differ by a cent for the same portfolio (decision 3).
- My reading: "net worth is $0.00" is the net worth as shown; a net worth above 0 that rounds to $0.00 would give a composition of $0.00 parts (decision 4).
- My reading: while a refresh runs, the screen shows both the age line and "Updating prices", as Overall does (16c decision 1); the failed line sits under the age line, inside the net worth part (wireframe 02 marker 1), not under the composition.

## 5. Files
- app/: ui/NetWorthModel.kt (new: net worth, its age and stale state, the refresh state, the composition rows by largest remainder, the $0.00 case; CR2); ui/NetWorthScreen.kt (new); ui/MoreModel.kt (new: the last backup line and the entries); MoreScreen.kt (the Net Worth (Total) entry, decision 1); OverallScreen.kt (the net worth card opens the screen); AppViewModel.kt (Screen.NET_WORTH, open and close, `AppState.netWorthScreen`); AppContent.kt (wiring); strings.xml (section 12); build.gradle.kts (versionCode 26).
- Tests: the Net Worth model's builder (Example A at $160: cash $9,600.00 and Portfolio P $1,280.00 adding to $10,880.00; parts that need a leftover cent, with ties to the earlier row; a portfolio at cost; no portfolio; $0.00; the age line, some stale, no price used); the More model; ViewModel wiring tests (open from Overall and from More, Back to each, ignored while not on the tabs); screen tests (AC-61 (d): the net worth sentence with its age, the composition rows, the $0.00 line, the failed line).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48).
- No change in core, no new library, no schema change.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-17-net-worth-screen, one pull request.
1. This file and the plan.md lines (with pull request #147's merge line).
2. The models: Net Worth and More, tests first, shown red.
3. The screen, its opening from Overall and More and Back, and its strings.
4. The screen tests (AC-61 (d)).
5. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The composition and Overall's rows can show a portfolio a cent apart (section 4; decision 3).
- 2. Tapping the net worth card must not take the pull-to-refresh gesture or change the card's TalkBack start "Net worth" (AppStartTimingTest).
- 3. A new screen opened from a tab: Back must return to that tab (Overall or More), also after a lock and unlock while it is open; a refresh while it shows redraws it from the model, which is worked out on each read (open item 51).
- 4. No screen test covers the screen's layout at the largest font: checked on the phone.
- 5. The emulator is not the phone (C19); the screen tests use no network.

## 9. Proof
- Commands: as in 16c, all with --rerun-tasks. The screen tests on the pull request: 20 plus this iteration's new ones.
- Unit tests: steps 2 and 3.
- The phone check, over 16c's build (AC-35; no schema change): Overall's net worth tap opens the screen, Back returns to Overall; More's entry opens it, Back returns to More; net worth and its age line as on Overall; the composition's amounts against each portfolio's screen and the Cash tab, and their sum against net worth; offline: ", some stale" and the failed line, no pop-up; "Updating prices" and pull-to-refresh (decision 2); largest font and landscape; TalkBack speech; light and dark; logcat as in 16c. The $0.00 case only in the screen tests and unit tests: the phone's data is not changed.

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-09)
1. More gets the Net Worth (Total) entry now, under the last backup line; a tap opens the screen and Back returns to More (your A). Not chosen: (B) leave More until its other entries come.
2. Pulling down on Net Worth (Total) refreshes the prices, as on the other price screens (your A). Not chosen: (B) no pull-to-refresh.
3. The cent between the composition and Overall's rows is accepted: the composition by largest remainder, a row alone, as 4.10 says (your A). Not chosen: (B) Overall's rows by largest remainder too, a change request.
4. The $0.00 case applies when net worth shows as $0.00, also when it is above 0 but rounds to it (your A). Not chosen: (B) only when it is exactly 0.

## 12. Wording to approve (D9)
To be shown on the phone in step 5:
- The screen's title "Net Worth (Total)", with the back arrow read as "Back".
- The heading "Composition", the rows "Cash" and each portfolio's name with its amount, and the last row "Net worth" with the total; TalkBack "<name>, <amount> dollars".
- The $0.00 line: "No net worth to show yet" (wireframe 02 frame C's placeholder).
- More's entry "Net Worth (Total)" (decision 1); the net worth card on Overall read as a button.
- Reused: "Net worth", "Prices as of … ET" and "…, some stale", "Updating prices", "Price refresh failed. Showing last prices."

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 4 settled.

## 14. Departures from the plan
None yet.
