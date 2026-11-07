# plan/iterations/15d-cash-list.md

Stage: Build. Status: approved 2026-11-07 (your "let's continue"); decisions 1 to 3 settled the same day (section 11). Built 2026-11-07, steps 1 to 7 on iter-15d-cash-list (pull request #140); phone check all yes (section 13.3). Done: pull request #140 merged as e4c9179 on 2026-11-07; main green (section 13.4).
Source: plan.md section 4 (row 15d), open items 45 (the Cash tab part) and 48 (the Cash screen), the 15c file section 13.4, spec view 7, AC-49, AC-61, CR2. "My reading" is an interpretation, not a test.

## 1. Outcome
The Cash tab opens and filters quickly with many entries, and shows exactly what it shows now.

## 2. Spec refs
- Spec: view 7 (the Cash account); 4.1 (newest first, same-date entries in entry order); 4.8 (the balance); 4.10. No new rule, criterion or wording.
- AC-49: the parts built so far (the balance, Cash entry, the date range, the type chips, the rows with the real balance after each, trade lines). The ticker picker and the Buy and Sell chips stay in 28.
- AC-61: (c) for the filter's date fields, and (d) for the rows, whose TalkBack text carries the amount and the balance. No fixed part, layer or menu, so (a) and (b) do not apply (decision 2).
- CR2: the changed screen draws only from its model (plan.md section 3).
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- 28's ticker picker and Buy and Sell chips.
- Every other screen (open item 48), the cash entry detail and form included.
- Moving work off the main thread (15c decision 5).
- Any change to what is shown or how it reads.

## 4. Analysis (2026-11-07, by reading CashScreen.kt and CashAccount.kt)
- Three sorts per redraw: `cashRows` sorts every entry, `cashFilteredRows` sorts them again, and a third time for the count line while a filter is on.
- Two searches of every transaction per row: one finds the row's trade (CashScreen.kt line 91), and `transactionTitle` searches again for a reversal's original. With 300 transactions and about 400 rows, about 240,000 steps per redraw.
- Every row is drawn at once, in a scrolling Column; only the rows on screen need drawing.
- Amounts and balances are formatted inside composables (signedAmount, spokenDollars, Money.format), which CR2 leaves to a model.
- The 15c measurement (15c file, section 13.4, run B): the Cash phase used 2.2 to 4.5 s of main-thread CPU on the emulator.

The fix, in layers:
- The screen model (CR2): a plain Kotlin builder makes in one pass what the screen does per redraw: one sort through core's `CashAccount.rows` (the rule stays in core), each row's trade found through an id map (rows + transactions instead of rows × transactions), and each row's texts and kind prepared, so the screen only picks the approved string.
- The screen state (decision 1): the model is a kept field of AppState, worked out again only when the entries, the transactions, the portfolios or the filter form change (15c's KeptFields). Opening the tab, switching tabs and scrolling only read it.
- The screen (decision 2): one lazy list, so only the rows on screen are laid out.

## 5. Files
- New, app/: CashListModel.kt, the screen's model and its builder (no Compose, no resources): the balance, the filter's date refusals, whether a filter is active and the count, and the rows, each with its kind (a direct entry with its type, or a trade line with its title kind, ticker and portfolio name), the date, the signed amount text, the balance after it, the note, and what a tap opens.
- Changed, app/: AppViewModel.kt and KeptFields.kt (the model as a kept field; decision 1); CashScreen.kt (draws only from the model, in one lazy list; decision 2); build.gradle.kts (versionCode 22).
- Tests: CashListModelTest (new); one ViewModel wiring test; KeptFieldsTest gains the new inputs (added lines only); CashScreenTest in app/src/androidTest (new; AC-61 (c) and (d)).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (items 45 and 48).
- No new library, no schema change, no new wording.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-15d-cash-list, one pull request.
1. This file and the plan.md lines.
2. The model: tests first, shown red, then the builder. Example A's four rows and balances (AC-49); a date filter and a type filter keep the real balances; a reversal's title kind from its original; a trade line's portfolio name; same-date entries, the later first; the count line; no entries, and no match; the filter's date refusals.
3. The kept field and its wiring: the model is worked out only when the entries, the transactions, the portfolios or the filter change; a test for each input, and "same object" when the tab changes.
4. The screen: CashScreen draws only from the model, in one lazy list.
5. The screen tests (AC-61), on ScreenTestSupport.kt: (c) at the largest font, with the keyboard open on a date field, the field lies fully above the keyboard; (d) a trade row's and a direct entry's TalkBack text reads its amount and balance in full.
6. The "after" figures: you push once, and AppStartTimingTest on the pull request gives the Cash phase's times and CPU, compared run by run with 15c's run B.
7. Documents, the phone check, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- The keyboard: the Cash screen has no `imePadding()` today, so a date field may sit under the keyboard at the largest font. If step 5's check (c) finds it, it is fixed in this iteration.
- A lazy list and text fields: a focused date field scrolled off the screen could lose its focus (my reading). Step 5 and the phone check cover it.
- "No change to what is shown": moving formatting from the screen into the model can change a sign, a dollar sign or a word by accident. The model's tests check the texts, and the phone check compares with your screenshots.
- TalkBack: a lazy list only creates the rows on screen; TalkBack scrolls such lists itself (my reading). The phone check covers it.
- A kept field can go stale if an input is missed. The guards: 15c's single refresh point and a kept-field test for each input.
- The emulator is not the phone (C19): the figures compare before and after on one kind of machine; the phone check by feel stays the judge.

## 9. Proof
- Commands: as in 15c, all with --rerun-tasks. The screen tests 11 of 11 on the pull request (9 now, plus 2).
- Unit tests: the model and the kept field (steps 2 and 3).
- AppStartTimingTest: the Cash phase's wall time and main-thread CPU, against 15c's run B (6,751, 4,077 and 2,384 ms; main thread 4,500, 2,770 and 2,190 ms). Recorded, not asserted.
- The phone check, after a force stop, before and after installing over 15c's build: screenshots of the Cash tab with no filter, with a date range and with a type chip; after the install the same rows, balances and count line, and a trade row and a direct entry each open as before. By feel: the Cash tab after unlock, and typing a date in the filter. The standing checks AC-09, AC-39 (largest font), AC-40 (dark) and AC-42.

## 10. Research
None, since no library is added.

## 11. Decisions (Build, settled by you 2026-11-07)
1. The Cash screen's model is a kept field in AppState, worked out in KeptFields.kt only when the entries, the transactions, the portfolios or the filter change (your A). Not chosen: (B) an AppState getter that builds it on every read.
2. One lazy list for the whole screen: the title, the balance card and the filter card are its first items, then the rows; everything scrolls together, as now (your A). Not chosen: (B) the balance and filters fixed above a lazy list of rows, which would change how the screen behaves and add an AC-61(a) check.
3. Step 4: the card around the rows is drawn in slices, one per row, from Material's own outlined-card shape, border and color (`CardDefaults`): the first row draws the top edge and corners, the last the bottom, the rest only the sides, so the list still looks like one card (your A). Not chosen: (B) no card around the rows; (C) each row in its own small card. Both would change how the screen looks.

## 12. Wording to approve (D9)
None, since no text changes.

## 13. Status and notes
- Draft 2026-11-07; decisions 1 and 2 settled; approved the same day.

### 13.1 Steps 2 to 5 (2026-11-07)
- Step 2 (5f6d331): CashListModel.kt, the model and its builder (`CashList.of`): one `CashAccount.rows` sort, the trades and portfolios found through id maps, and each row's texts, signed amounts and TalkBack figures prepared. CashListModelTest, 12 tests, shown red against a stub first. Tests 683.
- Step 3 (13406fb): the model is a kept field of AppState (`cashList`), worked out in KeptFields.kt only when the entries, the transactions, the portfolios or the filter form change. KeptFieldsTest checks each new input and "same object"; CashListFlowTest, 3 tests (the model in the state, kept on a tab change, refreshed by a filter and by a saved deposit). Tests 686. A mutation check (the portfolios input left out) failed the tests, and was undone.
- Step 4 (be81fa5): CashScreen.kt draws only from the model, in one LazyColumn (decision 2), the rows keyed by the model's key, the card in slices (decision 3). No text changed.
- Step 5 (f58e894): CashScreenTest.kt, 2 screen tests: `CashFilterKeyboardTest` (AC-61 (c), font 2.0: the From field ends at or above the keyboard's top) and `CashTalkBackTest` (AC-61 (d): a sell, a buy and the opening balance each read their amount with plus or minus and the balance after them in full).
- Every step: spotlessCheck, build, tests (0 failures, 1 skipped, as before) and lint (the 5 listed findings), all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`.
- Step 7's first full run (21:06) failed once in 15c's HoldingsScaleTest: `firstNegative` on spread holdings took 9.9 ms at 3,000 transactions (limit 7.0 ms, growth 10.4, bound 8), with only documents changed since the last green run. Three runs of that test alone passed (the last 0.644 ms, growth 2.6), and the next full run (21:10) was green. My reading: a slow moment on the PC, not the code; not proven.

### 13.2 Step 6: the "after" figures (pull request #140, run 42360547829)
The screen tests 11 of 11 (9 plus 2); CI green (run 42360547804). `CashFilterKeyboardTest` passed, so the Cash screen needs no `imePadding()` (section 8, first risk). AppStartTimingTest on GitHub's emulator, against 15c's run B (15c file, section 13.4):

| Run | Cash, 15c run B | Cash, 15d | Main-thread CPU in Cash, 15c run B | 15d | Overall, 15d |
|---|---|---|---|---|---|
| 1 | 6,751 ms | 623 ms | 4,500 ms | 300 ms | 1,584 ms |
| 2 | 4,077 ms | 453 ms | 2,770 ms | 200 ms | 1,672 ms |
| 3 | 2,384 ms | 349 ms | 2,190 ms | 190 ms | 1,769 ms |

- The Cash phase is 85 to 91% quicker run by run; its median 453 ms, against 1,385 ms before 15c and 4,077 ms in 15c's run B. Garbage collection in the Cash phase: 0 ms.
- This runner was quicker than 15c's: the six other screen tests took 29.5 s, against 41.7 s in 15c's run B and 32.4 s before 15c. Part of the gain may be the machine; the main-thread CPU, which depends less on it, fell by 89 to 93%.
- Recorded, not asserted (section 9). The emulator is not the phone (C19).

### 13.3 Step 7: the phone check (2026-11-07, all yes)
Part A, before the install, after a force stop: your screenshots of the Cash tab (the top and bottom of the list, a From date with its count line, a type chip). Part B: versionCode 22 installed over 15c's build (`adb install -r`, Success).
- 7 (AC-35): the data kept, with the same Net worth, holdings and Cash balance: yes.
- 8: the Cash tab after a force stop and unlock, by feel: noticeably faster.
- 9: the same rows as the screenshots, top to bottom, still one card with dividers: yes.
- 10: the same From date gives the same count line; the same type chip, the same rows: yes.
- 11: a trade line and a direct entry each open as before: yes.
- 12 to 16: AC-39 (largest font, landscape: the From field above the keyboard), AC-40 (dark), AC-09 (the date note), AC-42 (airplane mode) and TalkBack (a row reads its kind, date, signed amount and balance): all yes.

### 13.4 The merge (2026-11-07)
Pull request #140 merged as e4c9179, 6 commits; the branch is gone from origin and locally. On the pull request's last commit (c8355dc) CI (3m 32s) and the screen tests (11 of 11, 4m 22s) green; on main after the merge CI (3m 1s), the screen tests (11 of 11, 6m 9s) and CodeQL green (gh, read only).

## 14. Departures from the plan
1. Step 4: the card around the rows is drawn in slices (decision 3), since one Card cannot wrap the items of a lazy list.
2. Step 6 changed no file and has no commit; its figures are recorded here in step 7.
3. versionCode 22 (section 5) was set in step 7, before the phone check, not in step 4.
