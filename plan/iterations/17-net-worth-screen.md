# plan/iterations/17-net-worth-screen.md

Stage: Build. Status: approved 2026-11-09 (your "Yes"); decisions 1 to 4 settled the same day (section 11). Built 2026-11-09 (steps 1 to 5); phone check 2026-11-09 all yes (section 13.3); the screen tests 25 of 25 on pull request #148 (section 13.4); the app start timing compared with 16c and reviewed against the spec refs (section 13.5).
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
Approved on the phone 2026-11-09 (your A, check 10), as in strings.xml's "Iteration 17" block: the title "Net Worth (Total)" (also More's entry), the heading "Composition", the row "Cash", a composition row's TalkBack "<name>, <amount> dollars" (also for Cash and the "Net worth" row), and the $0.00 line "No net worth to show yet", which the phone did not show (its data is not at $0.00; the screen tests show it).
Proposed in step 1, to be shown on the phone in step 5:
- The screen's title "Net Worth (Total)", with the back arrow read as "Back".
- The heading "Composition", the rows "Cash" and each portfolio's name with its amount, and the last row "Net worth" with the total; TalkBack "<name>, <amount> dollars".
- The $0.00 line: "No net worth to show yet" (wireframe 02 frame C's placeholder).
- More's entry "Net Worth (Total)" (decision 1); the net worth card on Overall read as a button.
- Reused: "Net worth", "Prices as of … ET" and "…, some stale", "Updating prices", "Price refresh failed. Showing last prices."

## 13. Status and notes
- Draft 2026-11-09; approved the same day; decisions 1 to 4 settled.

### 13.1 Steps 1 to 4 (2026-11-09)
- Step 1 (e598927): this file and the plan.md lines (with pull request #147's merge line). Full run green (15:13 to 15:15), 762 tests.
- Step 2 (b982ee5): ui/NetWorthModel.kt (`NetWorthTotal.of`: net worth as cash as of today plus each portfolio's exact value, the age line over every holding, the refresh state, the composition by `Money.partsInCents`, null while net worth shows as $0.00) and ui/MoreModel.kt (`More.of`: never backed up, the Net Worth (Total) entry); `AppState.netWorthTotal` and `more`, worked out on each read. NetWorthModelTest 8 (Example A at $160: $9,600.00 and $1,280.00 adding to $10,880.00; a leftover cent to the larger dropped remainder, 0.6 over 0.4; a tie to the earlier portfolio, $0.01 and $0.00, while the second alone shows $0.01 (decision 3); a portfolio at cost with no age line; some stale with the oldest time and the refresh state; no portfolio, cash alone; $0.00; above 0 but shown as $0.00 (decision 4)); MoreModelTest 1; PriceRefreshFlowTest +1 (the screen's net worth and age line equal Overall's; the composition at the refreshed price). Shown red against stubs first: 21 tests, the 10 new ones failed. Green at the first full run (15:19 to 15:21). Tests 762 to 772.
- Step 3 (c506289): ui/NetWorthScreen.kt; `Screen.NET_WORTH` with the bottom bar, `openNetWorth` (from Overall or More only, not under the Buy/Sell layer or a dialog) and `closeNetWorth` (back to the same tab); Overall's net worth card shared by both screens, a button on Overall, its TalkBack sentence unchanged, the failed line under Overall's rows and under the age line on the new screen (section 4); MoreScreen.kt drawn from its model with the entry (decision 1); pull-to-refresh (decision 2); strings.xml's "Iteration 17" block. NetWorthFlowTest 6 (from Overall and back; from More and back; ignored on Cash and a portfolio's screen, Back elsewhere does nothing; not under the Buy/Sell menu; an unlock on the screen keeps it and Back returns to More (risk 3); a tab tap leaves it). Green at the first full run (15:27 to 15:29). Tests 778.
- Step 4 (6a0f962): NetWorthTalkBackTest, 5 tests (AC-61 (d), AC-44): fresh $160 (the net worth sentence with its age; "Cash, 9,600.00 dollars", "Portfolio P, 1,280.00 dollars", "Net worth, 10,880.00 dollars"; no updating or failed line); stale $140 while a failed refresh is retried ("some stale", 1,120.00 and 10,720.00, the updating and failed lines); $0.00 (the plain line, no composition); Overall's net worth and More's entry as buttons that open it. `@Test` lines 25 (were 20).
- Steps 2 to 4 were committed together after step 4 (your "keep going", then "commit now"), each from its own green run; step 2's files were staged on their own before step 3 changed AppViewModel.kt again.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings.

### 13.2 Step 4's runs and the JVM crashes (2026-11-09)
- Step 4's first full run (15:31) stopped: Gradle's daemon died in a JVM crash, and the androidTest compile then failed in KSP with `ClassFormatError: Unknown constant tag 0`. The second (15:32 to 15:35), after `--stop`, crashed again in spotlessCheck and the build; the tests (778), lint and the androidTest compile passed. Seven crashes in all, 15:31:14 to 15:33:33, each an `EXCEPTION_ACCESS_VIOLATION` in jvm.dll in a C2 compiler thread of Android Studio's Java 25.0.3, at a different address each time. The code had not changed since the green runs of 15:19 and 15:27. My reading: a fault of this PC at that time, not the project; not proven. The crash logs were moved from the repo root to the session's scratchpad. Your B: retried without a restart; the third run (15:36 to 15:38) was green with no crash. Not chosen: (A) restart the PC first; (C) pause.
- Step 5: versionCode 26; full run 15:40 to 15:43 green, 778 tests, 0 failures, 1 skipped; lint the 5 listed findings. APK 42,597,795 bytes, built with the price-source key set (checked True/False only).

### 13.3 Step 5: the phone check (2026-11-09, all yes)
Check 1, before the install on 16c's build (versionCode 25), online: your screenshots of Overall, the Cash tab's balance, each portfolio's value and More; a force stop. versionCode 26 installed over it (`adb install -r`, Success; lastUpdateTime 15:59:27, first install 2026-10-29 kept).
- 2 (AC-35, AC-44, AC-34): the first unlock; net worth's tap opens the screen with the bottom bar; net worth and its age line as on Overall, no failed line; the composition's cash as the Cash tab, each portfolio as its own screen, adding up to net worth; Back to Overall: yes.
- 3 (decision 1): More's entry opens it; "<" and Android Back return to More: yes.
- 4 (decision 2): pull-to-refresh; a tab tap leaves the screen: yes.
- 5 (AC-42, risk 3): airplane mode, lock and unlock: ", some stale" and the failed line under it, the composition kept, no pop-up; a lock and unlock on the screen keeps it and Back goes to Overall; back online and a pull: refreshed, the marks gone: yes.
- 6 (AC-39, risk 4): largest font and landscape, online and offline, on the screen and More: yes.
- 7 (AC-39): TalkBack: Overall's net worth read as a button and opening the screen; Back, the title, the net worth sentence, "Composition", each row with its amount, the failed line and "some stale"; More's entry as a button: yes.
- 8 (AC-40, AC-09): light and dark; the time with "ET": yes.
- 9 (spec 9): `adb logcat -d -v time --uid=10454`. The first read (47 lines, 16:07:35 to 16:10:55) no longer held checks 2 to 5: the older lines had rolled out. Re-check: a force stop, an unlock, the screen from Overall with a pull, and from More; then a read (68 lines, 16:08:49 to 16:13:38, the new process from 16:13:03): the key not in it (a True/False test); no "finnhub", "token", AAPL, "quote" or "symbol="; the two amount-like matches are the system's window insets (`VRI[MainActivity]`, read with the digits masked: `statusBars:[9,999,9,9]`), not amounts; the 10 "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`: yes.
- 10 (D9): the wording approved (section 12).
- Decision 3's cent did not show with your data.

### 13.4 AC-62 and the screen tests
From 17, the TalkBack labels of Net Worth (Total) and the buttons on Overall and More are checked by NetWorthTalkBackTest; the layout at the largest font stayed a hand check (check 6). - Pull request #148 at 452c64c: the screen tests (run 42585761040) 25 of 25 at the first attempt (tests 162 s, run 08:24:51Z to 08:30:57Z), NetWorthTalkBackTest's 5 among them; CI (run 42585760994, 2m 46s) and CodeQL green (gh, read only).

### 13.5 App start timing and the review against the spec refs (2026-11-09, by reading main..HEAD; plan.md section 5, item 7)
- AppStartTimingTest, 300 transactions, from the ScreenTestTiming lines in the run's artifact (one run each, emulator): "load and Overall" 4,380, 3,194 and 3,320 ms, median 3,320 ms, against 3,188 ms on 16c's pull request #146 (run 42558487448; the 16c file, 13.5), +132 ms (+4%); main-thread CPU in that phase 1,090, 790 and 720 ms, median 790 against 720. My reading: within the emulator's spread (16c's own runs spread from 2,598 to 5,016 ms); Overall's card only gained a click. Not proven.
- No finding that needs a change. In order: net worth is cash as of today plus each portfolio's value, a holding at its price or at cost (4.8), and equals Overall's (PriceRefreshFlowTest); the age line is the oldest price used over every holding, a holding at cost not counted, with ", some stale" (4.8, AC-44); the composition is shown by largest remainder and adds up to the net worth shown, ties to the earlier row (4.10); the $0.00 case on the shown figure (decision 4); "Updating prices" and the failed line under the age line, no pop-up (section 6, states (2) and (3); wireframe 02 marker 1); pull-to-refresh (decision 2); Back to Overall or More (marker 1); Overall's net worth sentence unchanged, now a button (risk 2); exact arithmetic (4.10); no logging (spec 9), no floating point, no store write in the added code (the screens only read; section 14). Noted, not a finding: a hidden holding is left out of a portfolio's composition row but in net worth; it has 0 shares and no value, so the two still agree.

## 14. Departures from the plan
1. Step 3: Overall's net worth card became `NetWorthCard` (internal, in OverallScreen.kt), drawn by both screens, with a parameter for where the failed line goes. Not in section 5's file list.
2. Step 3: the wiring tests for opening and Back are their own class, NetWorthFlowTest.
3. Steps 2 to 4 were committed together after step 4 (13.1), not one commit after each step.
4. Step 5: the logcat read was done twice (13.3, check 9), the second after a short re-check, because the first no longer held checks 2 to 5.
