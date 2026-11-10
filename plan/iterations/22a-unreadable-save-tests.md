# plan/iterations/22a-unreadable-save-tests.md

Stage: Build. Status: approved 2026-11-10 (your "Approved, with my input"); decision 1 settled the same day (section 11).
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

## 14. Departures from the plan
None yet.
