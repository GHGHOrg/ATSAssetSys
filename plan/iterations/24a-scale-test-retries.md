# plan/iterations/24a-scale-test-retries.md

Stage: Build. Status: approved 2026-11-12 (your "approved"); decision 1 settled the same day (section 11). Built 2026-11-12 (steps 1 to 4 and one Deploy fix; section 14); no phone check (tests only); open item 59 closed; the screen tests 72 of 72 on pull request #172 (section 13.2); reviewed, two findings fixed (section 13.3).
Source: open-items.md item 59 (your decision 2026-11-12: its own iteration after 24, not a step of 25; a one-off, CLAUDE.md section 4's rule stays); the 15c file (section 9, part 2: the timing method; decision 2: the fixed times at 3,000; section 8: "a failing ratio is re-run once before it is treated as real"); the 16b file, 13.2 (the scale test red in full runs; your B, re-run and record); CLAUDE.md (tests first; never weaken or delete a test to make it pass). "My reading" is an interpretation, not a test.

## 1. Outcome
HoldingsScaleTest stops failing the full local run when the PC is busy, and still fails when a call really got slower. No app code, no screen and no limit or bound changes.

## 2. Spec refs
- The 15c file's spec refs for the speed of loads, saves and redraws, and its section 9, part 2 and decision 2 (the ratio bounds and the fixed times at 3,000 transactions), unchanged.
- Standing checks: none apply, since nothing under app/src/main or core/src/main changes (as 15b and 22a); AC-61's screen tests run unchanged on the pull request.

## 3. Out of scope
- The JVM crashes and the fixture failure (open item 60).
- Changing a limit, a bound, the sizes or the calls timed.
- Running `:core:test` after the app's tasks (not chosen, section 11).

## 4. Analysis (2026-11-12, from runs on this PC and by reading the test)
- The test times 9 calls on two shapes at 100, 300, 1,000 and 3,000 transactions; each figure is the median of 5 blocks of at least 50 ms, about a quarter of a second per size (`ScaleTiming.median`). Then it fails if a call's time at 3,000 is over its fixed limit or grew 8 times or more from 1,000 to 3,000 (20 for calls 6 and 8 on one holding).
- Gradle runs `:core:test` beside the app's tasks in the usual command, though `org.gradle.parallel` is off: in a run of 2026-11-12 (00:27:37, 96 tasks, profile and stamped output), `:core:test` ran from about 20 s to 55 s and HoldingsScaleTest from 20 s to 40 s, while the app's code, unit tests and screen tests were compiled, its dex built and packed, and lint's three analyses ran (lint until about 84 s); up to 7 tasks ran at once, and the tasks' durations added up to more than twice the build's 1m 27s. In the red run of 00:19 the app's 44 unit-test classes and the packaging fell inside HoldingsScaleTest's 19 s.
- In that red run one figure was 5 times its usual value ("2 netWorth", SPREAD, 29.175 ms at 3,000 against about 6 ms alone; growth 10.1) while its neighbours measured at about the same time were normal (holdings 7.3 ms, lots 9.4 ms). Alone, the test was green every time (16b decision B). So a busy moment spoils one measurement; slow code would spoil every one. My reading of the 24 runs: about 1 in 3 full runs that reached the tests was red on this test.
- Waiting for lint (`mustRunAfter`) would add about 35 s to every full run: `:core:test` could start only at about 84 s.
- A retry right after a failed try often falls in the same busy stretch (lint runs for about a minute), so tries are not independent; a pause before a retry lowers that.

## 5. Files
- core/src/test: HoldingsScaleTest.kt (the check of each call and shape: a failed check is measured again under decision 1; every try's figures go into the printed table and core/build/scale/holdings-scale-after.txt); a test of that rule with made-up measurements (decision 1's cases).
- No file under app/ or core/src/main changes. From the Deploy review: .github/workflows/ci.yml (section 14, item 1) and CLAUDE.md section 3.
- Documents: this file, plan.md (section 4's row 24a, section 11 and the change log), open-items.md (items 59 and 60).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-24a-scale-test-retries, one pull request.
1. This file, the plan.md lines and open items 59 and 60.
2. The rule (decision 1), test first: its cases with made-up measurements (a slow first try, then normal: passes on the second; slow, slow, then normal: passes on the third; slow on every try: fails after 3; a first try that passes: measured once), shown red against a stub that judges the first try alone, as today; then the rule in HoldingsScaleTest, the table showing every try.
3. The evidence, on scratch changes never committed: a slowed `Valuation.netWorth` (a repeat of its work, growing with the square of the transactions) shown red on every try; then taken out. Then 5 usual runs in a row, each test count and each retry read from the table.
4. Documents, open item 59 closed, the pull request draft, and the open item 33 reminder.

## 7. Stubs
Step 2's stub and step 3's slowdown are scratch changes, taken out before each step's run and never committed; each is shown with its diff, and core/src/main is unchanged after each step (risk 2).

## 8. Risks
- 1. The rule could hide a real slowdown that shows only now and then (for example only when garbage collection runs). Step 3 shows a steady slowdown still caught; an intermittent one is not tested.
- 2. A scratch change left in core/src/main would change the app: each step's report shows `git diff --stat` of core/src/main and app/src/main empty.
- 3. The fixture failure and the JVM crashes (open item 60) can stop a run before the tests; such a run is re-run unchanged after reading its cause (CLAUDE.md) and does not count in step 3.
- 4. CI runs the same test on GitHub's runner; the rule applies there too. CI has not shown this failure.

## 9. Proof
- Commands: as in 24, all with --rerun-tasks. Unit tests 993 plus step 2's, none removed or weakened in its limits. The screen tests on the pull request: 72.
- Step 2's red run against the stub; step 3's red run against the slowdown; step 3's 5 runs with each retry read.
- No phone check: nothing under app/src/main changes (as 22a).

## 10. Research
None.

## 11. Decisions (settled by you 2026-11-12)
1. (Build) The rule when a check fails: any 1 of 3 (your B). A failed check (a call and shape over its fixed time at 3,000, or over its growth bound) is measured again at 1,000 and 3,000 at once, the same way, up to 2 more times; the check passes if any try passes, and fails if all 3 fail. Every try's figures go into the table. Limits and bounds unchanged. It costs nothing on a quiet run and about 1 to 4 s after a failure; it is more lenient than one try (section 8, risk 1), and a retry at once may fall in the same busy stretch (section 4), which step 3's runs show. Not chosen (rough red rates, assuming independent tries): (A) 2 retries 5 s apart, both must pass, about 1 in 5; (C) any 1 of 3 with 5 s between tries, about 1 in 25; (D) a majority of 2 of 3 on every run, about 1 in 4, each call measured at least twice; (E) no change, re-run alone by hand (16b decision B). Not offered: `:core:test` after the app's heavy tasks (about 35 s more on every full run, section 4).

## 12. Wording to approve (D9)
None: no text the user sees changes.

## 13. Status and notes
- Draft 2026-11-12; decision 1 settled the same day (your B); approved the same day.

### 13.1 Steps (2026-11-12)
- Step 1: this file, the plan.md lines (section 4's row 24a, section 11, the change log with #171's merge line) and open items 59 and 60. Only documents changed. The usual command green at the first try: 993 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 01:08:22).
- Step 2: the rule (decision 1) in core/src/test/…/ScaleJudge.kt: `ScaleCheck` (a call on a shape, its growth bound or none, its fixed time at 3,000), `ScaleTry` (the times at 1,000 and 3,000), `ScaleJudge.judge` (a failed, timed check measured again at once, up to 3 tries; passes when any try passes) and `ScaleJudge.problem` (15c's two checks, the messages as before). HoldingsScaleTest judges each call and shape through it, measuring again with `ScaleTiming.measure` on histories built for that size and shape; a check that still fails says "(3 of 3 tries)"; the table in core/build/scale/holdings-scale-after.txt ends with every try of each check measured again ("none" when none was). The "before 15c" table is unchanged. ScaleJudgeTest 7, with made-up measurements (the red run's 2.882 and 29.175 ms as a busy try). Red against a stub that judged the first try alone: 7 tests, 4 failed (the second try, the third try, slow on every try "expected 3 but was 1", over the fixed time), 3 passed (a first try that passes, not timed, no growth bound: the same under the stub). The usual command green at the first try: 1,000 tests, 0 failures, 1 skipped; no check measured again; lint the 5 listed findings (report 01:17:36).
- Step 3, the evidence. A scratch slowdown in `Valuation.netWorth` (ten passes over every pair of transactions; shown with its diff, taken out after, core/src/main unchanged): HoldingsScaleTest alone red, "2 netWorth" failing on every try on both shapes (SPREAD 109.4, 120.3 and 121.6 ms at 3,000, growth 11.2, 12.6 and 13.6; ONE_HOLDING 135.7, 111.9 and 116.8 ms, growth 14.3, 11.4 and 12.3; limits 60 and 75 ms, bound 8), "(3 of 3 tries)". Then 5 usual runs in a row (01:21 to 01:27): all green, 1,000 tests, 0 failures, 1 skipped each; runs 2 and 3 measured "1 holdings" SPREAD again (54.5 ms at 3,000, growth 16.3, then 7.9 ms, growth 3.0; 41.0 ms, growth 8.4, then 11.4 ms, growth 2.7), each passing on its second try, so 2 of 5 runs would have been red before 24a; no new crash log. These runs used the Kotlin daemon started at 00:17 with C2 off (open item 60), which does not touch the test JVM where HoldingsScaleTest times its calls.
- Step 4: open item 59 closed with the evidence of steps 2 and 3; plan.md's row and change log; the pull request draft. The usual command green at the first try: 1,000 tests, 0 failures, 1 skipped; no check measured again; lint the 5 listed findings (report 01:31:47).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings; core/src/main and app/src/main unchanged after each (risk 2). Steps 1 to 4 committed after step 4 (your "commit later"), each from its own versions.

### 13.2 The pull request's runs (2026-11-12)
- Pull request #172 (created by me on your word, this once; reviewer msfsgh) at 3e25a0a: CI (3m 7s), Analyze (actions) and CodeQL green; the screen tests (run 43072794197, 10m 41s) 72 of 72 test cases, 0 failed, exit-code file 0. CI's log does not show a passing test's output, so whether HoldingsScaleTest measured a check again there could not be seen (finding 1).

### 13.3 Deploy review (2026-11-12)
The branch against this file, 15c's limits and bounds and CLAUDE.md's rule on weakening tests; it changes only core/src/test and plan.
- Finding 1 (Low): on GitHub a check rescued by a retry passed unseen: every try is in core/build/scale/holdings-scale-after.txt and the test's printed output, but CI's log leaves out a passing test's output and the workflow does not upload the file. Fixed on your B (section 14, item 1).
- Finding 2 (Low): a check not timed failed at once, as meant, but its message ended "(1 of 3 tries)", as if retries had been possible. Fixed on your B (section 14, item 1).
- Recorded, nothing to change: passing on any 1 of 3 tries is more lenient than one try, your decision 1 knowingly, with the limits and bounds unchanged and a steady slowdown failing every try (step 3); 15c's section 8 ("a failing ratio is re-run once … that re-run needs your yes") is about re-running a run on CI, which still needs your yes, not about the test's own re-measure; the retries time the same data (ScaleHistories has nothing random); `timesTheCodeBefore15c` and its table unchanged; no file under app/ or core/src/main.

## 14. Departures from the plan
- 1. Deploy review, findings 1 and 2 (your B): .github/workflows/ci.yml gains a step after the unit tests, "Scale test retries", run also when the tests fail, printing the table's "Checks measured again" part (every try of each check measured again, or "none"; "No scale table" when HoldingsScaleTest did not run); the command was tried in Git Bash on today's table, on step 3's run 2 table (its retry printed) and on a missing file. `ScaleJudge.failure` gives the test's message: the tries are named only once a check was measured again, so a check not timed reads "not timed" alone. Tests first: ScaleJudgeTest 2 (the "(3 of 3 tries)" message, the same before and after; not timed with no tries and a passing check with no message), red against a stub that always named the tries (1 of 2 failed, "not timed (1 of 3 tries)"); then green. The usual command green at the first try: 1,002 tests, 0 failures, 1 skipped; no check measured again; lint the 5 listed findings (report 02:14:51). CLAUDE.md section 3 names the new CI step.
- 2. Item 1's CI step broke the workflow: on 6b21564 GitHub could not read ci.yml ("This run likely failed because of a workflow file issue"; run 43076525866, listed by its path, no job started), so #172 had no CI check, while CodeQL and the screen tests (run 43076533968, 8m 17s) passed. My reading: the step's `run:` was a plain YAML value holding ": " ("No scale table: HoldingsScaleTest …"), which YAML reads as a new key; not checked locally (no YAML parser on this PC). Fixed by writing the command as a block (`run: |`), where a colon is text; the next CI run is the proof.
