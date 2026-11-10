# plan/iterations/22a-unreadable-save-tests.md

Stage: Build. Status: approved 2026-11-10 (your "Approved, with my input"); decision 1 settled the same day (section 11). Built 2026-11-10 (steps 1 to 4; section 14); no phone check (nothing under app/src/main changed); open item 54 closed, items 55 and 56 recorded; not yet merged.
Source: open-items.md item 54 (your decision 2026-11-10: its own iteration after 22, not a step of 23; a one-off, CLAUDE.md section 4's rule stays); the 22 file, 13.1 (step 4); spec section 9 and C18 (data that cannot be read shows the unreadable-data screen, nothing guessed); iterations 02a, 07, 08 and 20 (the four tests), 16a (the price refresh at start); CLAUDE.md (a defect in test support is not a change request and needs no intent file; tests first, never weakened). "My reading" is an interpretation, not a test.

## 1. Outcome
Four unit tests that should prove the app shows the unreadable-data screen when a save finds the stored data unreadable really prove it again, and the price save at start, which they covered only by accident, gets a test of its own. No screen, no figure and no behaviour of the app changes.

## 2. Spec refs
- Section 9 and C18: data that cannot be read is not guessed; the app shows the unreadable-data screen.
- The tests' own iterations: 02a (a cash entry's save), 07 (New portfolio), 08 (Rename), 20 (the auto-lock choice).
- Standing checks: none apply, since nothing under app/src/main changes (as 15b); AC-61's screen tests run unchanged on the pull request.

## 3. Out of scope
- Any change to app/src/main.
- Other test weaknesses not of this kind; anything found is listed as an open item.

## 4. Analysis (2026-11-10, by reading the code)
- Since 16a, `AppViewModel.start()` ends with a price refresh, which calls `store.savePrices` even with no tickers (`PriceRefresh.refresh`). In the tests' `FakeStore`, `savePrices` throws `saveError` like every other save.
- So with `saveError = UnreadableDataException(...)`, `started(store)` already shows the unreadable screen, every later action is ignored on it, and the test's last line, "the screen is unreadable", holds whatever the action does. My reading; step 2 shows it.
- Four tests do this; no other test passes an unreadable error to a save:
  1. `AppViewModelTest.anUnreadableStoreDuringSaveShowsTheUnreadableScreen` (02a, a cash entry);
  2. `AppViewModelTest.unreadableDataOnSaveShowsTheUnreadableScreen` (07, New portfolio);
  3. `AppViewModelTest.unreadableDataOnRenameShowsTheUnreadableScreen` (08, Rename);
  4. `AutoLockViewModelTest.anUnreadableStoreOnSave_showsTheUnreadableScreen` (20; open item 54).
- The tests with a disk error (`IllegalStateException`) are not affected in the same way: the refresh then only counts as failed, and each checks its own save's refusal. 22's tab order tests use `tabOrderSaveError`, which fails that save alone.
- The code under test was right when each test was written (before 16a for three of them); whether it still is, is what step 2 shows.
- No test checks on purpose that an unreadable price save at start shows the unreadable-data screen (PriceRefreshFlowTest has none); today only these four tests reach it, by accident. Once they no longer trip it, it needs its own test (decision 1).

## 5. Files
- app/src/test: AppViewModelTest.kt (`FakeStore`'s own error for the price save, decision 1; tests 1 to 3), AutoLockViewModelTest.kt (test 4), PriceRefreshFlowTest.kt (the new test for the price save at start).
- No file under app/src/main or core changes.
- Documents: this file, plan.md (section 4's row 22a, section 11 and the change log, with #161's merge line), open-items.md (item 54 closed, with the evidence).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-22a-unreadable-save-tests, one pull request.
1. This file and the plan.md lines (with pull request #161's merge line).
2. The evidence: each of the four tests run against a stub of its own action (the save it is about made to do nothing), on a scratch change that is shown and then put back, not committed. A test that still passes on its stub confirms the weakness.
3. The fix (decision 1): `FakeStore.savePrices` throws only its own `pricesSaveError`, not `saveError`; each of the four tests first checks it is on its own screen before the action, so it can no longer pass on the start's refresh, and fails only on its own save; a new test makes only the price save at start unreadable and checks the unreadable-data screen. The four shown red against the same stubs, the new one red against a stub that ignores the refresh's save failure; then all green against the code.
4. Documents, open item 54 closed, the pull request draft, and the open item 33 reminder.

## 7. Stubs
Step 2 and 3's stubs are scratch changes to app/src/main, put back before each step's run and never committed; each is shown with its diff.

## 8. Risks
- 1. Changing what `FakeStore.saveError` covers could change other tests' paths (the refresh then succeeds where it used to fail). Every test is run; any test that changes result is read, not adjusted to pass.
- 2. A stub left in app/src/main would change the app: each step's report shows `git diff --stat` of app/src/main empty before the run.
- 3. If a test is red against the real code, the app has a defect, not the test: that is a bug, and it stops this iteration (Ruleset A rule 7, a new intent).

## 9. Proof
- Commands: as in 22, all with --rerun-tasks. Unit tests 919 (one new, four strengthened, none removed). The screen tests on the pull request: 57.
- Step 2's and step 3's red runs against the stubs, shown with their output.
- No phone check: nothing under app/src/main changes (as 15b).

## 10. Research
None.

## 11. Decisions (Build, settled by you 2026-11-10)
1. The price save at start gets a test of its own, and the four tests no longer touch it (your proposal): `FakeStore.savePrices` stops throwing `saveError` and gets its own `pricesSaveError`, used only by the new test; each of the four tests also checks its screen before the action. Not chosen: (B) `FakeStore` as it is and a save error of each test's own (as 22's `tabOrderSaveError`), which would leave the price save at start untested. Your first idea, clearing the error screen before each action, was not taken: the screen is a dead end on purpose (section 9, C18), and clearing it would hide the false alarm, not stop it.

## 12. Wording to approve (D9)
None: no text the user sees changes.

## 13. Status and notes
- Draft 2026-11-10; approved the same day with your input (the price save at start tested on its own); decision 1 settled.

### 13.1 Steps (2026-11-10)
- Step 1 (3270310): this file and the plan.md lines (with #161's merge line).
- Step 2, the evidence, on scratch changes never committed: each action's first line made `if (store != null) return` (`confirm`, `createPortfolio`, `saveRename`, `chooseAutoLock`; a bare `return` did not compile, the rest of `confirm` then being unreachable). Against these stubs three of the four tests still passed: 07's `unreadableDataOnSaveShowsTheUnreadableScreen`, 08's `unreadableDataOnRenameShowsTheUnreadableScreen` and 20's `anUnreadableStoreOnSave_showsTheUnreadableScreen` (the weakness confirmed). 02a's `anUnreadableStoreDuringSaveShowsTheUnreadableScreen` failed ("expected UNREADABLE but was REVIEW"). A scratch test (deleted with your yes) printed the screen right after `started()` for both stores the four tests use: UNREADABLE for each, so the price save at start trips all four. 02a's test still reaches its own save because `openForm()` does not check for the unreadable screen and opens the form from it; on the phone that screen is a message with no button and no bar (AppContent.kt), so the user cannot do this. Recorded as open item 55 in step 4 (section 3). The stubs were taken out again; app/src/main unchanged.
- Step 3, the fix (decision 1): `FakeStore.savePrices` throws only its own `pricesSaveError`; each of the four tests checks the screen it is on before its action (02a the review, 07 Overall with the dialog and its name, 08 the portfolio with the Rename dialog and its name, 20 Settings with the layer open); PriceRefreshFlowTest's new `anUnreadableStoreOnThePriceSaveAtStart_showsTheUnreadableScreen` makes only the price save unreadable and checks the unreadable-data screen. Red against scratch stubs first (the four actions doing nothing; the refresh treating an unreadable save as a failed one): 5 tests, 5 failed, each at its last line with the screen before the action (REVIEW, PORTFOLIO, TABS, SETTINGS, TABS), so each check before the action held. The stubs were taken out; app/src/main unchanged. Against the code: 919 tests, 0 failures; no other test changed result (risk 1).
- Step 4: open item 54 closed with the evidence; items 55 (`openForm()` and the unreadable screen) and 56 (no test of an ordinary price save failure) recorded; plan.md's row and change log; the pull request draft.
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step; app/src/main unchanged after each (risk 2). Each step committed after its run (your "commit").

## 14. Departures from the plan
- Step 2: section 4 read all four tests as passing on the start's price refresh alone. 02a's did not: after the start had shown the unreadable screen, `openForm()` opened the cash form from it, so the test still reached its own save. Recorded as open item 55; the fix of step 3 was the same for all four.
- Step 2: the stubs were `if (store != null) return`, not a bare `return`, which did not compile; a scratch test printed the screen after start for both stores (deleted with your yes).
- Step 3: the four actions' stubs were used again, and the new test's stub made the refresh treat an unreadable save as an ordinary failure.
