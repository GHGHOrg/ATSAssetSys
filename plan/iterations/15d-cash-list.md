# plan/iterations/15d-cash-list.md

Stage: Build. Status: approved 2026-11-07 (your "let's continue"); decisions 1 and 2 settled the same day (section 11). In build: step 1.
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

## 12. Wording to approve (D9)
None, since no text changes.

## 13. Status and notes
- Draft 2026-11-07; decisions 1 and 2 settled; approved the same day.

## 14. Departures from the plan
None yet.
