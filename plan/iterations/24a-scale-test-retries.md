# plan/iterations/24a-scale-test-retries.md

Stage: Build. Status: approved 2026-11-12 (your "approved"); decision 1 settled the same day (section 11).
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
- No file under app/ or core/src/main changes.
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

## 14. Departures from the plan
None yet.
