# plan/iterations/39a-open-items-tidy.md

Stage: Build. Status: Approved 2026-11-23 (your yes); step 1 built (documents and baseline, 13.1); step 2 built (item 65); step 3 built (item 56, 1,513 tests); step 4: draft pull request #227 green at the first attempt (CI, the screen tests 178 of 178, CodeQL); the Deploy review done, no High, Medium or Low finding (13.2). Branch iter-39a-open-items-tidy from main (ba28308), created on your word.
Source: plan/open-items.md items 21, 56 and 65 (the three with the least work, chosen 2026-11-23); the hand-off of 2026-11-23, items 1 to 3; your answer of 2026-11-23 ("Yes draft").

## 1. Outcome
Open item 21 is closed (the README was rewritten and merged in #226). The two unneeded `!!` of open item 65 are gone, and the compiler warns about neither. A unit test shows what happens when the price save fails with an ordinary error: the prices stay as they were, and the refresh counts as failed (open item 56). The hand-off's evidence for open item 60 and the BackupReportTest timing failures are recorded as open items. Nothing in the app changes. No release.

## 2. Spec refs
- None changed. Item 56's test checks the behaviour that 16a built for spec 7 (a refresh that fails leaves the prices stale), and AC-41.
- Test code and documents only. No app behaviour and no spec change, so no change request (as 22a and 24a).

## 3. Out of scope
- Every other open item. Open item 55 (`openForm` on the unreadable screen) changes app behaviour and needs its own intent file (Ruleset A rule 7). Open item 61 needs the phone.
- The CLAUDE.md addition for the Kotlin extension (the hand-off's item 2; you chose "later" on 2026-11-22), and any settings for the extension (the hand-off's item 4).
- A release.

## 4. Analysis (2026-11-23, by reading; nothing run)
- Item 65: [HoldingDetailModelTest.kt:140](../../app/src/test/java/app/ghghorg/atsassets/ui/HoldingDetailModelTest.kt) and [NetWorthModelTest.kt:79](../../app/src/test/java/app/ghghorg/atsassets/ui/NetWorthModelTest.kt) still hold the `!!` that the item names. In each case the `!!` on the line above already makes the value non-null, so the compiler calls the second one unneeded. Line 139's `model.price!!` and line 78's `model.composition!!` stay.
- Item 56: `AppViewModel.refreshPrices` (AppViewModel.kt:1486 to 1488) catches an ordinary exception from the save, sets `refreshing = false` and `refreshFailed = true`, and leaves `instruments` unchanged. `PriceRefreshFlowTest.kt:177` tests only the unreadable case (`pricesSaveError = UnreadableDataException`). Since 22a, `FakeStore.pricesSaveError` (AppViewModelTest.kt:1925) fails the price save alone, so the new test needs no change to the fake. If the test fails, that is an app defect: stop, and it becomes an intent file (Ruleset A rule 7), not a change to this iteration.
- Item 21: its status cell still says "Set aside by your decision (2026-11-22)". #226 merged the rewritten README as ba28308.
- The hand-off's item 3 (a new open item): core's `BackupReportTest.aFileOf3000Transactions_isCheckedInTime` (limit 5,000 ms) failed in five full local runs in a row (6,423 / 6,492 / 7,159 / 5,827 / 5,802 ms) while the Kotlin server's Gradle daemon ran beside them. Alone it passed in 1,278 ms, the sixth full run (no daemon from the server) passed in 2.8 s, and CI is green. Same pattern as open item 59. The limit stays as it is.

## 5. Files
- app/src/test/java/app/ghghorg/atsassets/ui/HoldingDetailModelTest.kt (item 65; one line).
- app/src/test/java/app/ghghorg/atsassets/ui/NetWorthModelTest.kt (item 65; one line).
- app/src/test/java/app/ghghorg/atsassets/ui/PriceRefreshFlowTest.kt (item 56; one test).
- plan/open-items.md: 21 closed; 56 and 65 closed with their commits; 60 gains the hand-off's evidence; a new item 74 for the BackupReportTest timing.
- plan/plan.md: row 39a, section 11, the change log.
- No file under app/src/main or core/.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus :app:compileDebugAndroidTestKotlin, and the report. Then I stop. Expect open item 60's failures while VS Code's Kotlin server runs: read the cause, `gradlew --stop`, and re-run unchanged.
1. Documents: this file; plan.md (row 39a, section 11, the change log); open item 21 closed; open item 60's new evidence; new open item 74. Baseline: the test total and the compiler warnings in the unit tests.
2. Item 65: the two `!!` removed. Proof: the unit tests compile without those two warnings; the test total equals step 1's.
3. Item 56: the new test in PriceRefreshFlowTest, a store with stored prices and `pricesSaveError = IllegalStateException("disk")`. It checks that the instruments' prices are as stored before, `refreshFailed` is true, `refreshing` is false, and the screen is not the unreadable one. It is shown red first against a stub, then green: in a scratch copy only, never committed, the `catch (e: Exception)` branch is changed to leave `refreshFailed` false. Proof: the test total is step 2's plus 1.
4. The pull request (draft, on your word): CI, screen tests (all shards, the count) and CodeQL. Open items 56 and 65 closed. The Deploy review.

## 7. Stubs
Step 3's red check only (a scratch change to `refreshPrices`, undone before the full run; `git diff` shows no file under app/src/main).

## 8. Risks
1. Open item 60 (local failures while VS Code's Kotlin server runs a Gradle daemon in the same build/ folders): read the cause, `--stop`, and re-run unchanged.
2. BackupReportTest (new item 74) can fail a full local run for the same reason. Re-run unchanged, and record each failure in 13.1. The limit is not changed (Ruleset A rule 5).
3. Open item 73 can turn shard 1 red on the pull request. Re-run only that shard's job, with your yes.
4. The new test passes for the wrong reason (open item 54's pattern). Step 3's stub check covers this.

## 9. Proof
- Steps 2 and 3: the full run green; the test total step 1's, then plus 1; the two warnings gone, and no new warning.
- Step 4: CI, the screen tests and CodeQL green on the pull request and on main after the merge.
- No phone check: no app code changes. Each step's `git diff --stat` shows no file under app/src/main or core/.

## 10. Research
None.

## 11. Decisions (Build, to settle one at a time after approval)
None. The red stub check in step 3 is the convention from 22a, not a choice.

## 12. Wording to approve
None (no app text). The text of open item 74 and of item 60's addition is shown in step 1's report.

## 13. Status and notes
- Draft 2026-11-23; approved the same day (your yes).

### 13.1 Steps
- Step 1 (2026-11-23): this file; plan.md (section 2's file list, row 39a, section 11's row, the change log); open items 21 (closed), 60 (the hand-off's evidence) and 74 (new). Only documents changed. Baseline, daemons stopped first, every command with --rerun-tasks: spotlessCheck green (24 s); build green (1m 1s; "Unable to strip" as known); tests green (58 s): 109 result files (core 40, debug 68, release 1), 1,512 tests, 0 failures, 0 errors, 1 skipped; `:app:compileDebugUnitTestKotlin` alone (29 s), to see the warnings: exactly two, `HoldingDetailModelTest.kt:140:43` and `NetWorthModelTest.kt:79:65`, "Unnecessary non-null assertion (!!)" (item 65); `:core:compileTestKotlin` (15 s): no warning; lint green (1m 19s; report 10:15:37): the 7 findings CLAUDE.md section 6 lists; `:app:compileDebugAndroidTestKotlin` green (29 s). No failure of open item 60's kind in this run, and BackupReportTest passed.
- Step 1 committed with your yes: 66b01d4 (signed). Your instruction (2026-11-23): work on to the step before the pull request, each step's state held for its own commit; commit only when the pull request is needed.
- Step 2 (2026-11-23): item 65, the second `!!` removed at HoldingDetailModelTest.kt:140 (`model.price.spokenPerShare`) and NetWorthModelTest.kt:79 (`model.composition.portfolios`); line 139's and line 78's `!!` kept. The full run, daemons stopped first, every command with --rerun-tasks: `:app:compileDebugUnitTestKotlin` green (1m) with no warning; spotlessCheck green (19 s); build green (39 s); tests green (52 s): 109 result files, 1,512 tests, 0 failures, 0 errors, 1 skipped, as step 1; lint green (1m 17s; report 10:23:56): 7 findings, as step 1; `:app:compileDebugAndroidTestKotlin` green (28 s). No failure of open item 60's kind.
- Step 3 (2026-11-23): item 56, `PriceRefreshFlowTest.anOrdinaryFailureOfThePriceSave_keepsThePricesAsTheyWere_andSaysTheRefreshFailed`: the store's `pricesSaveError = IllegalStateException("disk")`, the source quoting AAPL at 160. It checks that no price was saved, AAPL keeps its stored price (150, not marked stale), net worth stays $10,800, `refreshFailed` is true, `refreshing` false, and the screen is not the unreadable one. Shown red against a stub first: `refreshPrices`'s `catch (e: Exception)` set to `refreshFailed = false` in the working copy, `:app:testDebugUnitTest --tests "*PriceRefreshFlowTest"`: 17 tests, 1 failed, the new one at line 192 (`assertTrue(state.refreshFailed)`); the stub undone, and `git diff` then showed no file under app/src/main or core/. The full run, daemons stopped first, every command with --rerun-tasks: spotlessCheck green (23 s); build green (59 s); tests green (58 s): 109 result files, 1,513 tests (step 2's plus 1), 0 failures, 0 errors, 1 skipped; PriceRefreshFlowTest 17 of 17; lint green (1m 19s; report 10:29:31): 7 findings, as step 1; `:app:compileDebugAndroidTestKotlin` green (28 s). No failure of open item 60's kind. The app's code already behaved as the item asked, so no defect and no intent file.
- Steps 2 and 3 committed with your yes, each from its held tree and signed: 819992a (step 2, 4 files) and 8f0995d (step 3, 3 files); each step's staged files matched its report; HEAD's tree equals step 3's (93fc732); `git status` clean.
- Step 4 (2026-11-23). Pushed once by me and draft pull request #227 created by me at your request ("push once. Create this PR once, add a reviewer"), reviewer msfsgh. On 8f0995d, all at the first attempt (gh, read only; watched in the background): CI run 45100917330 green, `build` 6m 41s, the release class check "classes read: 6320, in the root package: 3037, that cannot load: 0"; Screen tests run 45100917279 green, shards 0, 1 and 2 with 53, 59 and 66 tests (9m 28s, 8m 55s, 7m 39s), the count job "Tests run: 178 of 178; in more than one shard: 0" (no screen test added, so 178 as before); CodeQL run 45100914830, Analyze (actions) and CodeQL pass. Open item 73 did not show. Open items 56 and 65 fixed by steps 3 and 2; they close when #227 merges.

### 13.2 Deploy review (2026-11-23)
The Deploy review (Ruleset A rule 6), by reading the branch's whole change against main (`git diff ba28308 8f0995d`: 3 unit-test files, 18 lines; 3 documents) against this file, plan.md and spec 7 and AC-41. No High, Medium or Low finding.
What held:
- No file under app/src/main, core/, the build files or the workflows changed, so K1 to K10's boundaries, logging (spec section 9), exact arithmetic (4.10) and the release path cannot have changed; CI's release build and class check gave the same counts as 39 (6,320 classes, 0 that cannot load).
- Item 65: only the second, unneeded `!!` of each pair removed; the first stays and still fails the test with a clear error if the model is null. No assertion weakened (Ruleset A rule 5).
- Item 56: the test asserts the behaviour 16a built (AppViewModel.kt:1486 to 1488), was shown red against a stub before it went green, and adds no change to `FakeStore`.
- Checks: CI, the screen tests (178 of 178) and CodeQL green at the first attempt.
Notes (info, no change):
1. `assertTrue(state.screen != Screen.UNREADABLE)` names no values when it fails; `assertNotEquals` would. Readability only.
2. The stub check showed the test red through `refreshFailed`. That the prices stay as they were follows from the save failing before `applied` runs; no second stub was tried for it.
3. Open items 60 and 74 did not show in this iteration's four local full runs.

### 13.3 Merge

## 14. Departures from the plan
