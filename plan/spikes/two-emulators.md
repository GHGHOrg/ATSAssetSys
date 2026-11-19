# plan/spikes/two-emulators.md

Stage: Build. Status: written 2026-11-19 on branch spike-two-emulators (from main at 6d26199); approved by you 2026-11-19 as written, before the look-ups (section 5). Look-ups L1 and L5 done 2026-11-19, L3 worked out before the run; L2, L4, L6 and L7 answered by the run. The research's open questions settled 2026-11-19 (section 11.5). Decisions T1 to T5 settled 2026-11-19 (section 7). The run done 2026-11-19 (section 9): series A 3 of 3 green, B1 and C1 red; T6: one emulator kept, nothing adopted. Cleanup done 2026-11-19 (section 10). Extended 2026-11-19 at your request, while pull request #205 was open: series D, the tests split over separate runners (sections 1, 2, 4, 6, 7 and 8); the extension approved and T7 to T10 settled 2026-11-19 (section 7). Series D (2 runners) and, at your request after it, series E (3 runners) run 2026-11-19 (section 9); T11: 3 runners adopted, to land as an open item fixed in a step of the next iteration (section 10). Cleanup of throw-away-runner-shards done 2026-11-19 (section 10). Merged: pull request #205 as b236484 (2026-11-19); main green on b236484 (CI, screen tests, CodeQL).
Source: your request of 2026-11-19 (two emulators on one GitHub runner, to shorten the screen tests; tried on a throw-away branch), the research report "Screen test runtime on GitHub" (2026-11-19; its findings and sources are in section 11, as the report itself stays in my scratchpad, which can be cleared), .github/workflows/screen-tests.yml (read 2026-11-19), plan/spikes/avd-memory.md sections 3, 5 and 9, the step times of runs 44314279343, 44309994483, 44299031081 and 44273292455 (read with gh, 2026-11-19), and your statement of 2026-11-19 that the repository is public on GitHub's Free plan.

How sources are labelled in this note:
- "I read": read by me in this session with gh or from the repo, with the date.
- "my reading": my interpretation. Not a fact until a look-up or the run shows it.
- "not checked": not looked at.
- "research": found on the web on 2026-11-19 by the research agents of `/deep-research`, with your approval of the searches, and not checked again by me (section 11).

## Table of contents
- [1. Outcome](#1-outcome)
- [2. Gate](#2-gate)
- [3. Where things stand](#3-where-things-stand)
- [4. Approaches](#4-approaches)
- [5. Questions to look up](#5-questions-to-look-up)
- [6. Criteria for the choice](#6-criteria-for-the-choice)
- [7. Decisions](#7-decisions)
- [8. The run](#8-the-run)
- [9. Results](#9-results)
- [10. Cleanup and follow-up](#10-cleanup-and-follow-up)
- [11. Research findings](#11-research-findings)

## 1. Outcome
A measured answer to one question: do two emulators on one GitHub runner, each running half of the screen tests, finish sooner than one emulator running all of them, with every run still green? It passes when:
1. The two-emulator setting (section 4, B) has a counted series on the same commit base and the same day as a one-emulator control series (A), both started the same way (decision T2).
2. Each run records its step times, each emulator's test count and time, the total test count against the @Test count of the day, and the runner's free memory while the tests run.
3. Section 9 says, by section 6, whether B is shorter, and by how much, or that it is not.
4. You choose to adopt B, or not (decision T6).
A second question, added 2026-11-19 at your request: does splitting the tests over separate runners (a job matrix, one emulator per runner, the research's approach 3, section 11.2) finish the screen tests sooner than one runner, with every run still green? It passes when:
5. Series D (section 4) has a counted series, judged against series A (run the same day) by section 6's rules for D.
6. Each run records each shard job's step times, test count and time, the total test count against the @Test count of the day, the wall time from the first shard job's start to the end of the last job, and the runner minutes of all jobs together.
7. Section 9 says, by section 6, whether D is shorter, and by how much, or that it is not; you choose to adopt D, or not (decision T11).
Skipping docs-only changes and cancelling superseded runs (the research's approaches 1 and 2) are not measured here (section 10).

## 2. Gate
This is a decision note, not an iteration. It gives no app feature, changes no app code and no spec, and blocks no iteration.
Branch: spike-two-emulators (from main at 6d26199), one pull request with this note, merged by you.
Trial changes to the workflow go only on a throw-away branch (CLAUDE.md section 4), throw-away-two-emulators, with a draft pull request that is closed unmerged. No NN: a spike has no iteration number (as plan/spikes/avd-memory.md, decision M4).
Order: the note's approval, the look-ups (section 5), decisions T1 to T5, the run (section 8), results (section 9), then T6. For series D: the extension's approval, decisions T7 to T10, the run (section 8, steps 6 to 9), results (section 9), then T11. D's trial goes on its own throw-away branch (T7).
An adopted setting changes only .github/workflows/screen-tests.yml. It changes no app behaviour and no spec, so it is not a change request (CLAUDE.md section 4, the test-support rule); T6 says how it lands.

## 3. Where things stand
- The workflow (I read, 2026-11-19): one emulator, `-memory 4096 -cores 4 -gpu swiftshader_indirect -no-snapshot`, API 37 google_apis x86_64, pixel_9_pro_xl; the APKs are built before the emulator starts, on purpose (plan/spikes/screen-tests.md 11.6, run 8: a build beside the booting emulator slowed both and Android's system process stopped responding). The tests run through `./gradlew :app:connectedDebugAndroidTest`. Every adb call in the workflow names no device (boot wait, settle, animations and PIN, screenshot pull).
- The runner (your statement and plan/spikes/avd-memory.md section 3, run 43793616953): public repository, standard ubuntu-26.04 runner, 4 CPUs, 15,700 MB total; runner minutes are free.
- Memory with one emulator at 4,096 MB (plan/spikes/avd-memory.md section 9): the runner's lowest available memory during the tests was 5,623 to 5,877 MB; the guest used about 2.1 to 2.5 GB; the emulator took its full guest size from the runner whether the guest used it or not (at 8,192 MB the runner's lowest fell by about 3.1 GB).
- Step times of 4 green runs on 2026-11-18 (I read, 2026-11-19):

| Run | Build | Boot | Settle | Screen tests |
|---|---|---|---|---|
| 44314279343 | 109 s | 51 s | 10 s | 698 s |
| 44309994483 | 103 s | 55 s | 10 s | 776 s |
| 44299031081 | 93 s | 54 s | 11 s | 793 s |
| 44273292455 | 68 s | 37 s | 10 s | 535 s |

- The time is spread over the tests, not held by a few (plan/spikes/avd-memory.md section 5, L3: 136 tests' own times add up to 556.6 s and 636.7 s; the slowest are largest-font landscape and keyboard tests, 10.9 to 24.7 s each). A slow run is slower in every class, by about 10 to 40% (same source): my reading is that the runner's CPU speed sets the pace.
- Nothing in the androidTest code is tied to one device (I read, 2026-11-19): ScreenTestSupport.kt runs its shell commands on the device through uiAutomation (line 49) and writes its unlock screenshots to the device's /data/local/tmp (line 218). No serial and no adb call appear in app/src/androidTest.
- Run times on the runner vary by about 160 to 200 s on the same commit (plan/spikes/avd-memory.md sections 3 and 9), so 3 runs per setting can show only a large gain.

## 4. Approaches
All my reading, to be corrected by section 5.
- A, control: one emulator, `-cores 4 -memory 4096` (today's flags), all tests, started the way T2 settles.
- B: two emulators on the same runner, `-cores 2 -memory 4096` each, on ports 5554 and 5556, started together after the build; each runs one half of the tests (`numShards 2`, `shardIndex 0` and `1`), both halves at once.
Why B could help (my reading): a test spends part of its time waiting (idling, the lock prompt, settle loops), and a second emulator can use the CPU while the first waits. Why it may not: the runner's 4 CPUs are shared by both guests and by swiftshader, which draws on the runner's CPUs outside `-cores`; if the CPUs are the limit, each half runs about twice as slowly and the total stays the same or grows. Two boots at once also share the CPUs, and a busy system was where "Not Responding" windows came from (plan/spikes/screen-tests.md 11.6).
Memory for B (my reading, from section 3): two guests at 4,096 MB leave the runner about 1.5 to 2 GB during the tests, near H3's floor. If H3 fails, a follow-up series at `-memory 3072` each is possible (T3), as the guest used at most about 2.5 GB.
My expectation, before the run: B is not shorter by T5's margin. The research reached the same view (section 11.3); this spike measures it.
- D (added 2026-11-19): the screen-tests job becomes a matrix of N jobs (T8), each on its own runner with one emulator as today (`-cores 4 -memory 4096`), each building the APKs, booting, settling, setting the PIN and running its share of the tests through Gradle with `numShards N` and its `shardIndex`. A last job, named `screen-tests` like today's required check, waits for the shard jobs, downloads their result files and checks the count (T10).
Why D could help (my reading): each shard has a whole runner's 4 CPUs, so a test runs at series A's speed, which B and C lacked (section 9: each half 2 to 3 times slower). Why it may not: each job repeats about 3.5 minutes of setup (section 3: build, boot, settle), the halves are not equal (B1 and C1: 76 and 60 tests; series A's times for them about 288 to 396 s and 274 to 369 s), and a job may wait for a free runner.
My estimate, before the run: with two shards, about 210 s of setup plus the larger half's 290 to 400 s of tests, so about 9 to 11 minutes per shard job against series A's whole job of 778 to 1,035 s (median 841 s); runner minutes about 1.3 times series A's. Not measured; the run decides.

## 5. Questions to look up
None needs a web search or fetch. Each gh call is a shell command and asks first (CLAUDE.md section 8).
- L1 The app and test package names and the runner class, for `am instrument`: read app/build.gradle.kts (applicationId, testInstrumentationRunner); in the run, check with `adb shell pm list instrumentation`.
- L2 What `am instrument -w -r` prints per test, so the count check can work from it: my reading is one `INSTRUMENTATION_STATUS_CODE: 1` per test started and a closing `OK (N tests)` or `FAILURES!!!`. Checked in control run A1 against the @Test count of the day.
- L3 How `numShards` splits the tests: my reading is by a hash of each test, so the halves differ in count and time. The run records each half's count and time.
- L4 Does `-port 5556` give the second emulator the serial emulator-5556 and its own adb connection? My reading: yes. Checked with `adb devices` in run B1.
- L5 Is the measuring of plan/spikes/avd-memory.md section 8, step 1 (the `free -m` sampler and the memory summary step, commit 161a322 on the deleted branch throw-away-avd-memory) still reachable with `git show 161a322`? If not, it is written again.

- L6 (added with T2) Does the Android Gradle plugin's connected test run only on the device `ANDROID_SERIAL` names? My reading: yes. Checked in run B1 by each half's result XML naming one device; before the run, optionally by searching the cached plugin jar for ANDROID_SERIAL (reading outside the repo, asks first).
- L7 (added with T2) Can two Gradle runs of `:app:connectedDebugAndroidTest` run at once in one checkout? My reading, not checked: the second may wait for locks the first holds, or both may write the same results and report folders (app/build/outputs/androidTest-results/connected/debug/, app/build/reports/androidTests/connected/debug/). The result XML's file name carries the device name (my reading), so the count check, which counts `<testcase>` in every XML there, would then add up both halves. Checked in run B1: both halves' start and end times overlap, and both halves' XML files are present.

Answers (I read, 2026-11-19):
- L1 app/build.gradle.kts: applicationId app.ghghorg.atsassets (line 17), testInstrumentationRunner androidx.test.runner.AndroidJUnitRunner (line 23), no testApplicationId. My reading: the test package is then app.ghghorg.atsassets.test, so the target is `app.ghghorg.atsassets.test/androidx.test.runner.AndroidJUnitRunner`; run A1 checks it with `adb shell pm list instrumentation`.
- L1 and L2 were needed only for starting the tests with `am instrument` (T2's A); with T2 settled as Gradle they are not used. Gradle uninstalls the APKs after the tests, so `pm list instrumentation` could not have been read after them.
- L4 (run B1): yes. `-port 5556` gave the serial emulator-5556; both emulators booted and took their tests (section 9).
- L3, before the run (section 11.5, the sharding filter and the time per class): runner 1.7.0 splits test by test by `Math.abs(description.hashCode()) % numShards`. Worked out with that rule on run 44314279343's 136 tests (my reading of the hash, not run): shard 0 would get 76 tests and 337.5 s, shard 1 60 tests and 318.7 s, about 51 to 49 by time. Run B1 records the real split. Answered by B1 and C1: 76 and 60 tests, as worked out.
- L6 (runs B1 and C1): yes. Each Gradle run printed "Running tests on devices:" with its own emulator only, and each half's result file names one device.
- L7 (runs B1 and C1): yes. Both Gradle runs started 156 s (B1) and 124 s (C1) after the emulators' start and ran at the same time, with no lock wait seen in the logs; each wrote its own result file (`TEST-ats-screen-tests-37-5554(AVD) - 17.xml` and `...-5556...`), and the count check added up both: 136 of 136, no test in both halves.
- L5 Reachable: 161a322 (2026-11-18, "Spike avd-memory, run commit 1: memory sampler, guest RAM and heap limits, memory summary ...") adds 49 lines to .github/workflows/screen-tests.yml and nothing else. Commit 1 (section 8) takes its sampler and summary from there.

## 6. Criteria for the choice
Hard rules: a setting that breaks one is out, whatever its time.
- H1 Every run of the series green, every test run: the two halves' test counts add up to the @Test count of the day, with no test in both halves.
- H2 No "Not Responding" window in either emulator's settle step, no unlock retry, no low-memory kill of the app.
- H3 The runner's available memory, sampled while the tests run, never below 1,024 MB (as plan/spikes/avd-memory.md, M1).
Then, ranked:
1. The median of "tests ready to tests done": from the first emulator's start to the end of the last half's tests (this takes in the boot and settle, which B does twice at once), against the control's median, same day.
2. The longest of the same against the control's longest.
3. The whole job's median.
B counts as shorter only if its median is at least T5's margin below the control's and its longest run is not longer than the control's longest.
For D (added 2026-11-19), the same hard rules for every shard job (H1: every job green and the shards' counts adding up to the @Test count with no test in two shards; H2; H3 on each runner), then, ranked:
1. The median of D's wall time, from the first shard job's start to the end of the last job (the count job), against series A's whole job (median 841 s, longest 1,035 s; section 9). Series A's whole job is the fair match: both take in checkout, build, boot, settle and tests.
2. The longest of the same against series A's longest.
3. The runner minutes per run (all jobs' times added), reported, not ranked: free for this public repository.
D counts as shorter only if its median is at least T5's 60 s below 841 s (so 781 s or less) and its longest run is not longer than 1,035 s.

## 7. Decisions
To settle after the note's approval and the look-ups, one question at a time.
T1 settled 2026-11-19, your A: throw-away-two-emulators, without NN (section 2).
T2 settled 2026-11-19, your B (my leaning was A): the tests start through Gradle's `:app:connectedDebugAndroidTest`, for the control as today, and in B as two Gradle runs at once, one per emulator, each with `ANDROID_SERIAL` and the shard arguments (`-Pandroid.testInstrumentationRunnerArguments.numShards=2` and `...shardIndex=0` or `1`). The control is then started exactly as today's runs. This depends on L6 and L7 (section 5); if two Gradle runs cannot run at once in one checkout, the trial stops there and T2 comes back to you.
T3 settled 2026-11-19, your A: 4,096 MB for each emulator in B; a series at 3,072 MB only if B breaks H3.
T4 settled 2026-11-19, your A: 3 runs per setting (6 runs); only a large difference can show.
T5 settled 2026-11-19, your A: as written in section 6, with a margin of 60 s off criterion 1's median, as plan/spikes/avd-memory.md, M1.
T3's follow-up settled 2026-11-19, your B (my leaning was to end the trial): after run B1 broke H3, a series C at 3,072 MB per emulator, everything else as B. Run C1 was red as well (section 9), which ended series C.
T6 settled 2026-11-19, your A, after the run (section 9): keep one emulator; nothing is adopted and main's workflow does not change.
For series D (added 2026-11-19), to settle after the extension's approval, one question at a time.
The extension approved by you 2026-11-19 as written. T7 to T10 settled 2026-11-19, each your A: T7 throw-away-runner-shards; T8 2 shards; T9 the runner's `numShards` and `shardIndex`; T10 series A as the control, with a count job named `screen-tests`.
After series D (2026-11-19), asked T11, you chose to run 3 shards first: series E, the same as D with `NUM_SHARDS: "3"` and the matrix `[0, 1, 2]` (commit 407503c), judged by the same rules. After D2 was red from the known keyboard-landscape flake (section 9), your A: D2 recorded but not counted, D3 and D4 run, so D is judged on 3 green runs.
T11 settled 2026-11-19, your A, after series E (section 9): adopt 3 runners. It lands as an open item fixed in a step of the next iteration (CLAUDE.md section 4, the test-support rule); section 10 holds the item.
- T7 D's throw-away branch. My leaning: throw-away-runner-shards, from main, with its own draft pull request closed unmerged.
- T8 How many shard jobs. My leaning: 2. Section 4's estimate gives 3 shards about 1.5 minutes more off the wall time, for about 3.5 minutes more runner time per run and a third emulator boot that can fail.
- T9 How the tests are split. A: the runner's `numShards` and `shardIndex` (76 and 60 tests, about 51 to 49 by time, section 11.5), no list to keep up to date. B: class lists balanced by series A's times, passed with the runner's `class` argument; better balanced, but a new test class must be added to a list, and one that is left out does not run (the count check catches it). My leaning: A.
- T10 The control and the count. My leaning: series A of 2026-11-19 is the control (same day, same main); the count job named `screen-tests` downloads every shard's result files, checks each shard's file, the total against the @Test count and that no test ran in two shards, and is the job the ruleset's required check would then name. Each shard job's artifact carries its shard number.
- T11 After the results: adopt D, or keep one runner. If D is adopted, my leaning: record it as an open item and change the workflow as a step of the next iteration (CLAUDE.md section 4, the test-support rule).
- T1 The throw-away branch's name. My leaning: throw-away-two-emulators (section 2).
- T2 How the tests start. A: `adb -s <serial> shell am instrument -w -r` on each emulator, both halves at once in the background, for the control too, so A and B differ only in the emulators; the APKs are installed with `adb -s <serial> install`; the result is the raw output, kept in the artifact. B: Gradle's connectedDebugAndroidTest with `ANDROID_SERIAL` and the shard arguments, one Gradle run per emulator at once. My reading: two Gradle runs in one project at once clash on the build folder and write the same results folder, and Gradle without ANDROID_SERIAL runs every test on every device rather than splitting them. My leaning: A. The control then differs from today's runs in how it starts, so section 3's times are for reference only.
- T3 Memory per emulator in B. My leaning: 4,096 MB (the size every green run so far had); 3,072 MB only if B breaks H3.
- T4 Runs per setting. My leaning: 3 each, as plan/spikes/avd-memory.md, M2 (6 runs, about 1.5 hours of runner time; free for a public repository).
- T5 The margin for "shorter". My leaning: 60 s off criterion 1's median, as plan/spikes/avd-memory.md, M1.
- T6 After the results: adopt B, or keep one emulator. If B is adopted, my leaning: record it as an open item and change the workflow as a step of the next iteration (CLAUDE.md section 4, the test-support rule), with the count check adding up the halves.

## 8. The run
Each push by you; each run and re-run with your yes; I read each with gh.
1. Branch throw-away-two-emulators from main. Commit 1, the control (A): the memory sampler and summary from 161a322 (L5) are added; every adb call names its device with `-s`. The test step (Gradle, T2), flags, count check, tests and app unchanged.
2. You push and open a draft pull request. Series A: T4 runs, as attempts of the same run (each attempt's artifact downloaded before the next, CLAUDE.md section 6).
3. Commit 2 (B): a second emulator on port 5556, `-cores 2` on both, both started together after the build; the boot wait, settle, animations and PIN steps run for each; each emulator runs one half through its own Gradle run, both at once (T2), each printing its own start and end time; the count check checks that each half's XML is there and that the halves add up; both emulators' logs and screenshots kept in the artifact. Series B, the same way.
4. A red run in a series is read for its cause before anything else (CLAUDE.md section 4); a red run caused by the second emulator ends B's series and is H1's or H2's answer for it.
Both series on the same day if the runner allows.
Series D (added 2026-11-19), after T7 to T10:
6. Branch from main (T7). Commit D1: the screen-tests job becomes a matrix of T8's shard jobs, each with today's steps and one emulator, running its share (T9) through `:app:connectedDebugAndroidTest` with the shard arguments; each job prints its own times and keeps its artifact with its shard number; the count job (T10) follows; the memory sampler and summary from step 1 stay in each shard job.
7. You push and open a draft pull request. Series D: T4's 3 runs, as attempts of the same run (each attempt's artifacts downloaded before the next).
8. A red run is read for its cause before anything else (CLAUDE.md section 4); a red run caused by the split ends D's series.
9. After the run: results in section 9, T11, then the throw-away branch's cleanup with your yes (section 10).

## 9. Results
Run 2026-11-19 on throw-away-two-emulators, draft pull request #204. Series A: run 44457933505, attempts 1 to 3, on 4eafe4b (section 8, step 1: one emulator, 4 cores, 4,096 MB, measuring added). Series B: run 44463954382 on 16f387e (step 3: two emulators, 2 cores and 4,096 MB each, one Gradle run and one half per emulator). Series C: run 44467392105 on eb98fb7 (T3's follow-up: as B with 3,072 MB each). Each push and re-run with your yes; I read each with gh and downloaded each attempt's artifact before the next (kept in my scratchpad, not in the repo). Step times are the job's from the GitHub API; boot and settle are the times the workflow printed; criterion 1 is "Emulators' start to tests done".

| Run | Result | Whole job | Build | Boot | Settle | Animations and PIN | Screen tests | Criterion 1 | Tests | Runner lowest available | Guest lowest available | Unlock screenshots |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| A1 | green | 1,035 s | 91 s | 51 s | 11 s | 5 s | 812 s | 879 s | 136 of 136 | 5,716 MB | 1,855 MB | 0 |
| A2 | green | 841 s | 85 s | 41 s | 11 s | 5 s | 631 s | 688 s | 136 of 136 | 5,912 MB | 1,796 MB | 0 |
| A3 | green | 778 s | 73 s | 42 s | 11 s | 2 s | 596 s | 652 s | 136 of 136 | 5,910 MB | 1,877 MB | 0 |
| B1 | red | 1,436 s | 108 s | 91 and 97 s | 12 s | 47 s | 1,109 s | 1,265 s | 76 + 60, 15 failed | 946 MB | 1,973 and 1,954 MB | 13 |
| C1 | red | 1,067 s | 64 s | 74 and 74 s | 11 s | 39 s | 821 s | 945 s | 76 + 60, 11 failed | 1,245 MB | 1,952 and 1,948 MB | 10 |

- Series A: criterion 1 median 688 s, longest 879 s; test step median 631 s; whole job median 841 s. No "Not Responding" window, no low-memory event; the kill events were the end of the tests ("finished inst", the test package removed), and in A1 com.android.externalstorage "Sync transaction while in frozen state", as in plan/spikes/avd-memory.md section 9.
- B1: half 0 (emulator-5554, 76 tests) failed 15, half 1 (emulator-5556, 60 tests) none. Half 1 ran from 156 s to 855 s after the emulators' start, half 0 from 156 s to 1,265 s. Failures: 13 tests "Timed out after 15000 ms waiting for the lock prompt's PIN pad (com.android.systemui:id/key_enter)", 2 AppStartTimingTest runs "Overall's net worth did not show within 120000 ms". The runner's available memory fell to 946 MB.
- C1: half 0 failed 1 test, half 1 failed 10: 10 PIN-pad timeouts as in B1 and 1 AppStartTimingTest timeout. Half 0 ended 723 s, half 1 945 s after the emulators' start. The runner kept 1,245 MB.
- Time per test against series A (I read, the result files, the same test names summed): B1's half 0 took 994.0 s for tests that took 288.0 to 396.2 s in A (about 2.5 to 3.5 times), half 1 579.2 s against 273.6 to 369.0 s (about 1.6 to 2.1 times); C1's half 0 520.1 s against 288.0 to 396.2 s (about 1.3 to 1.8 times), half 1 759.6 s against 273.6 to 369.0 s (about 2.1 to 2.8 times). Two halves each running about twice as slowly cannot finish sooner than one emulator running both.
- The guests never ran short: each had about 1.9 GB available at its lowest, at 4,096 or 3,072 MB. The cause of the failures, my reading: the four CPUs shared by two guests, two swiftshader renderers and two Gradle runs leave SystemUI too slow to draw the PIN pad within the tests' 15 s, at random on either emulator (B1 mostly 5554, C1 mostly 5556).
- Section 6: B and C break H1 (both red) and H2 (13 and 10 unlock retries); B breaks H3 (946 MB, floor 1,024 MB), C holds it (1,245 MB). Criterion 1: 1,265 s and 945 s against the control's median 688 s, where T5 asked for 628 s or less. Neither is shorter; by section 8, step 4, each red run ended its series (B2, B3, C2 and C3 not run).
- The answer to section 1: two emulators on one GitHub runner (4 CPUs, 16 GB) do not finish the screen tests sooner than one; they make them longer and red. More memory per emulator does not change that.

Series D and E (2026-11-19, throw-away-runner-shards, draft pull request #206): series D, run 44477659527, attempts 1 to 4, on a0585be (section 8, step 6: 2 shard jobs and a count job); series E, run 44484486343, attempts 1 to 3, on 407503c (3 shard jobs). Each push and re-run with your yes; each attempt's artifacts downloaded before the next (kept in my scratchpad). Wall time is from the first shard job's start to the count job's end (section 6, D criterion 1); runner minutes are all jobs' times added up.

| Run | Result | Wall time | Shard jobs | Shard test steps | Tests per shard | Runner minutes | Runner lowest available |
|---|---|---|---|---|---|---|---|
| D1 | green | 612 s | 604 / 519 s | 394 / 323 s | 76 / 60 | 18.8 | 5,928 / 5,777 MB |
| D2 | red (known flake, not counted) | 760 s | 512 / 748 s | 312 / 501 s | 76 (1 failed) / 60 | 21.1 | 5,970 / 6,043 MB |
| D3 | green | 708 s | 696 / 605 s | 459 / 385 s | 76 / 60 | 21.8 | 5,836 / 6,097 MB |
| D4 | green | 590 s | 582 / 555 s | 359 / 341 s | 76 / 60 | 19.1 | 5,958 / 6,013 MB |
| E1 | green | 556 s | 548 / 499 / 461 s | 311 / 272 / 263 s | 44 / 40 / 52 | 25.2 | 5,859 / 6,170 / 6,143 MB |
| E2 | green | 614 s | 605 / 500 / 478 s | 321 / 277 / 273 s | 44 / 40 / 52 | 26.5 | 6,082 / 6,221 / 5,952 MB |
| E3 | green | 554 s | 545 / 514 / 380 s | 317 / 276 / 192 s | 44 / 40 / 52 | 24.1 | 6,152 / 5,957 / 6,128 MB |

| | A (one runner) | D (2 runners: D1, D3, D4) | E (3 runners) |
|---|---|---|---|
| Median | 841 s (whole job) | 612 s | 556 s |
| Longest | 1,035 s | 708 s | 614 s |
| Spread | 257 s | 118 s | 60 s |
| Runner minutes, median | 14.0 | 19.1 | 25.2 |

- Every counted run: green, 136 of 136 tests, none in two shards, no unlock screenshot, no "Not Responding" window, no low-memory event, the runner's available memory never below 5,777 MB. Kill events were the end of the tests and system apps stopped by "change" or "frozen state" shortly after boot (up to 10 on one shard), none a memory event.
- The split: as worked out in section 5, L3 (76 and 60 tests for 2 shards; 44, 40 and 52 for 3, section 11.5's rule applied to run 44314279343). Each shard's tests ran at series A's speed (for example D4's shard 0: 325.0 s for tests that took 288.0 to 396.2 s in A); the shard with the most test time (shard 0) ended last in every run but D2.
- Setup per shard job: build 71 to 135 s, boot 35 to 58 s, settle 10 to 13 s, so about 3 minutes before the tests in every job; with 3 shards it is about half of each job. A fourth shard would save less again (my reading, not run).
- D2: shard 0's PriceKeyKeyboardLandscapeTest.theEntry_reachesDownToTheKeyboard_andShowsItsField_inLandscape failed, "The key entry ends at 0.0 px, 216.0 px above the keyboard's top at 216.0 px; expected its margin, 72.0 px" (PriceKeyScreenTest.kt:329), with shard 0's tests at normal speed (281.9 s). The same signature as the flake plan/spikes/dependabot-findings.md recorded on the one-runner workflow (run 44299031081, attempt 1, PortfolioDeleteKeyboardLandscapeTest): not caused by the split.
- Section 6 for D and E: H1 to H3 hold for every counted run. D: median 612 s (T5 asks 781 s or less) and longest 708 s (not over 1,035 s): shorter, by 229 s on the median. E: median 556 s and longest 614 s: shorter, by 285 s, about a third, and the steadiest of the three.
- The answer to section 1's second question: splitting the tests over separate runners finishes the screen tests sooner, about 27% with 2 runners and 34% with 3, all green, for about 1.4 and 1.8 times the runner minutes (free for this public repository).

## 10. Cleanup and follow-up
- Done 2026-11-19 with your yes: #204 closed unmerged; throw-away-two-emulators deleted locally and on GitHub (`gh pr close 204 --delete-branch` removed both). Its commits 4eafe4b, 16f387e and eb98fb7 are named in section 9; the runs' logs stay on GitHub.
- T6's change: none (section 7).
- Series D and E: done 2026-11-19 with your yes: #206 closed unmerged; throw-away-runner-shards deleted locally and on GitHub (`gh pr close 206 --delete-branch` removed both). Its commits a0585be (D) and 407503c (E) are named in section 9; the runs' logs stay on GitHub. A copy of 407503c's workflow is kept in my scratchpad for the next iteration's step, not in the repo.
- T11's change, an open item to add to plan/open-items.md (not added now: pull requests #193 to #196 edit that file too; it moves there with the plan.md lines below): "Screen tests on 3 runners (plan/spikes/two-emulators.md, T11): split .github/workflows/screen-tests.yml into a matrix of 3 shard jobs (`fail-fast: false`, the runner's `numShards` and `shardIndex`, each with today's steps and its own artifact named by shard and attempt) and a count job named `screen-tests` that downloads every shard's results and checks each shard's file, the total against the @Test count, that no test ran in two shards and that every shard job succeeded. Take the shape from commit 407503c without its memory sampler and summary. The ruleset's required check `screen-tests` then names the count job. Measured: median 556 s against 841 s." A step of the next iteration (CLAUDE.md section 4, the test-support rule).
- The keyboard-landscape flake ("ends at 0.0 px") came back in D2 (section 9), on PriceKeyKeyboardLandscapeTest this time: a second record for the point plan/spikes/dependabot-findings.md keeps for plan/open-items.md.
- What the run adds for the later notes: series A's three runs on one commit spread over 227 s (criterion 1, 652 to 879 s), larger than the 160 to 200 s seen before. Sharding over separate runners (section 11.2, approach 3) avoids the CPU sharing that failed here, but each runner repeats about 3.5 minutes of setup.
- Later, not in this spike: a fourth shard (not run; setup is already about half of each job with 3), skipping the screen tests on docs-only changes (a detection job and a gate job, not a workflow-level paths filter), cancelling superseded pull-request runs, and the deprecated `-gpu swiftshader_indirect` (deprecated since emulator 36.4.9, research, section 11.4; not checked in the emulator's own notes). Each would be its own note; section 11 holds what the research found for each.
- Open, from plan/spikes/avd-memory.md section 10: plan/plan.md section 2 does not list the spike notes. This note joins that point.

## 11. Research findings
From the research report "Screen test runtime on GitHub" (2026-11-19). Three research agents searched the web with your approval (topics: splitting the test run, emulator speed, workflow-level savings), and a fourth wrote the report from their notes and the step times in section 3. Everything here is "research" (see the labels at the top) unless marked otherwise; figures marked "estimate" are worked out from section 3's times, not measured anywhere.

### 11.1 The finding
The test step is about three quarters of a 12 to 17 minute job; everything else adds up to about 3.5 minutes (section 3). So no emulator-level change saves much, and that 3.5 minutes is also what each extra parallel job pays again. Most recent runs were for docs-only branches or the push to main after merging them (I read, 2026-11-19: most of the last 12 runs; the exact share not counted), and each took the full time.

### 11.2 The three approaches, ranked
1. Skip the emulator job when only documents change. A small first job (`dorny/paths-filter@v4`, or `git diff --name-only` against the base) decides whether anything outside the docs folders changed; the emulator job has `needs:` on it and an `if:` on its output; a final gate job with `if: always()` is the required check and fails unless detection succeeded and the tests passed or were skipped. Not a workflow-level `paths-ignore`: a workflow skipped by a path filter leaves its required check "Pending" and blocks merging, while a job skipped by `if:` reports "Success" ([GitHub Docs, troubleshooting required status checks](https://docs.github.com/en/enterprise-cloud@latest/pull-requests/how-tos/merge-and-close-pull-requests/troubleshooting-required-status-checks)). The filter must fail open: a short list of folders that may skip (intent/, spec/, plan/, `**/*.md`), so workflows, gradle/, `*.gradle.kts`, gradle.properties, app/, core/ and the wrapper always run the tests ([Mergify on path filters](https://mergify.com/blog/path-filters-are-not-a-ci-gate)). Pass the filter's outputs through `env:`, never straight into `run:` ([dorny/paths-filter](https://github.com/dorny/paths-filter)). Saving: the whole 13 to 17 min of each docs-only run.
2. Cancel superseded runs, and decide whether main needs its own run. A `concurrency` group on the pull request with `cancel-in-progress` only for `pull_request` events ([GitHub Docs, concurrency](https://docs.github.com/en/actions/how-tos/write-workflows/choose-when-workflows-run/control-workflow-concurrency)). The push run on main tests the same tree again when the branch was up to date. If it goes, something must still run on main: setup-gradle writes its cache only from the default branch ([gradle/actions setup-gradle](https://github.com/gradle/actions/blob/main/docs/setup-gradle.md)); whether the emulator cache behaves the same is not checked. A cancelled run's artifact is incomplete.
3. Split the suite across two (or three) runner jobs. A matrix with `fail-fast: false`; each job boots its own emulator and runs part of the tests, through the runner's `numShards`/`shardIndex` ([Android Developers, AndroidJUnitRunner](https://developer.android.com/training/testing/junit-runner)) or, better balanced, through class lists built from the `<testcase time=…>` values of the result XML. The runner's own split hashes tests and gives "no guarantee that test cases will be distributed uniformly" ([Google DeviceRun API](https://googleapis.dev/dotnet/Google.Apis.DeviceRun.v1alpha/latest/api/Google.Apis.DeviceRun.v1alpha.Data.AndroidInstrumentationTestUniformSharding.html); an older report saw a 2-shard split of about 1:2, from about 2017). Estimate: two jobs about 8 to 10 min instead of 13 to 17, three about 6.5 to 8 min; runner minutes +23 to 30% for two, +45 to 60% for three, free for a public repository ([GitHub Docs, Actions billing](https://docs.github.com/en/billing/concepts/product-billing/github-actions)); the Free plan allows 20 jobs at once ([GitHub Docs, limits](https://docs.github.com/en/actions/reference/limits)). The count check must add up the jobs, and artifact names need the job's number.

### 11.3 What bears on this spike
- The standard Linux runner has 4 vCPU and 16 GB for public repositories and 2 vCPU and 8 GB for private ones ([GitHub-hosted runners](https://docs.github.com/en/actions/reference/runners/github-hosted-runners)). This repository is public (your statement, 2026-11-19), and run 43793616953 had 4 CPUs (plan/spikes/avd-memory.md section 3).
- Two emulators on one runner, or Gradle Managed Devices sharding on one runner, share the same CPUs ([QA Wolf](https://www.qawolf.com/blog/5-strategies-to-address-android-emulator-instability-during-automated-testing)); the research expected well under twice the speed. No published measurement of Android instrumented tests on GitHub's runners, for either one or two emulators, was found.
- Gradle Managed Devices' `numManagedDeviceShards` is still marked experimental, and Google warns it uses many resources and can time out ([Android Developers, managed devices](https://developer.android.com/studio/test/gradle-managed-devices)). Not used here: this spike keeps today's emulator start.
- KVM: GitHub's Linux x64 runners support hardware acceleration for the emulator, including the 2-vCPU ones since April 2024; no primary source shows it on GitHub's arm64 runners.

### 11.4 Considered and not recommended
- Overlapping the build with the boot: up to about 50 s on paper, but it conflicts with plan/spikes/screen-tests.md 11.6, run 8 (section 3); a retry needs its own measured run.
- ATD images (aosp_atd, google_atd): they remove SystemUI, the Settings app, LatinIME and the launcher, and turn off hardware rendering ([Android Developers, managed devices](https://developer.android.com/studio/test/gradle-managed-devices)). The BiometricPrompt PIN pad is drawn by SystemUI and the keyboard tests need LatinIME (my reading: both break; not tested). An API 37 `aosp_atd` x86_64 image exists ([SDK manifest](https://dl.google.com/android/repository/sys-img/aosp_atd/sys-img2-4.xml)); no `google_atd` for API 37 was found. The only measured gain is about 20%, from 2022 on API 30 ([emulator.wtf](https://blog.emulator.wtf/posts/2022-04-15-atd-images)).
- Android Test Orchestrator: documented as a "possible increase in test execution time" ([Android Developers](https://developer.android.com/training/testing/junit-runner)).
- Larger runners: paid only, and only for organisations on the Team or Enterprise Cloud plans ([GitHub runner pricing](https://docs.github.com/en/billing/reference/actions-runner-pricing)); per minute $0.012 (4-core), $0.022 (8-core), $0.042 (16-core), against $0.006 for the standard 2-core.
- Test impact analysis: the tool found works per module ([Dropbox AffectedModuleDetector](https://github.com/dropbox/AffectedModuleDetector/)) and every screen test is in app/; a hand-made file-to-test map would miss shared code (the theme, MainActivity's cover and lock, ScreenTestSupport.kt), and a miss shows green.
- AVD snapshot caching: about 20 to 40 s at best (estimate). About 50 s to boot and save a snapshot plus about 30 s to save the cache ([android-test pull request #2115, January 2024](https://www.github.com/android/android-test/pull/2115)); about 15 s to boot from one ([2021 summary](https://lightrun.com/answers/reactivecircus-android-emulator-runner-using-an-emulator-snapshot)). It reverses the deliberate `-no-snapshot`, a PIN set before the snapshot boots every run locked, and any emulator, image or AVD change resets it. On API 36, `init.svc.bootanim` is never set when the boot animation is skipped; the workflow waits on `sys.boot_completed`, which is not affected.
- GPU mode, a smaller device profile, or the `aosp` image: unmeasured. `swiftshader_indirect`, `guest` and `swangle_indirect` are deprecated since emulator 36.4.9; the current modes are `auto`, `host`, `software`, `lavapipe`, `swiftshader` and `swangle`, and `host` needs a GPU the runner does not have ([Android Developers, acceleration](https://developer.android.com/studio/run/emulator-acceleration)). A smaller screen in dp changes what the largest-font landscape and keyboard tests check; a lower density alone may be safer (my reading).
- Hosted device clouds (Firebase Test Lab, emulator.wtf): no shell access for `locksettings`, API 37 not confirmed, paid beyond small free quotas ([Firebase pricing](https://firebase.google.com/docs/test-lab/usage-quotas-pricing)).

### 11.5 Open questions from the research, settled
All settled 2026-11-19, with your approval of each call. The helper scripts (run-history.ps1, class-times.ps1) and the downloaded artifact are in my scratchpad, not in the repo.
- Visibility (your statement, 2026-11-19): public, so the runner has 4 vCPU and 16 GB and the emulator's `-cores 4` is not oversubscribed.
- Required check and "up to date" (I read, `gh api repos/GHGHOrg/ATSAssetSys/rules/branches/main` and `.../rulesets`): ruleset 24172648 "default branch protection", active, requires the checks `build`, `CodeQL` and `screen-tests`, with `strict_required_status_checks_policy: true` (the branch must be up to date before merging). There is no classic branch protection (`.../branches/main/protection` answered 404). So approach 1's gate job must report as `screen-tests`, or the ruleset must name the gate instead (a repository setting, yours to change). With "up to date" on, the push run on main after a merge tests the tree the pull request's run already tested (my reading).
- Docs-only share (I read, the last 100 screen-tests runs, 2026-11-12 to 2026-11-18, with run-history.ps1; docs-only means every changed file under intent/, spec/, plan/ or ending in .md; a pull request's files are its final files, a push's files are the merge commit's against its first parent): 33 of 100 runs were docs-only (19 pull-request runs, 14 pushes to main); 67 changed code (61 pull-request runs, 6 pushes). Pushes to main were 20 of 100. 92 runs green, 8 red. The minutes, from a run's start to its last update (re-runs stretch them), split about the same: 478 of 1,501 docs-only.
- New pushes while a run was still going (same source): 2 of the 48 pull-request runs that followed an earlier run on the same branch. Approach 2's cancelling would have saved little in this period.
- Caches across pull requests (I read, `gh cache list`, 2026-11-19; research: [Dependency caching reference](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching), "Restrictions for accessing a cache"): every cache of the repository is on refs/heads/main; the emulator cache (`emulator-37.2.13-android-37.0-google_apis-x86_64-8`, 2,265 MB) was saved on main on 2026-11-06 and restored by pull-request runs since. GitHub's rule: a pull-request run can restore caches of its own branch, the default branch and its base branch; "Workflow runs cannot restore caches created for child branches or sibling branches"; a cache saved by a pull-request run is created for `refs/pull/.../merge` and "can only be restored by re-runs of the pull request". So if no run on main saves caches any more, each pull request would install and save a new emulator version for itself.
- The sharding filter (I read, 2026-11-19: the app's test runtime resolves androidx.test:runner 1.7.0 through espresso-core 3.7.0, by `.\gradlew.bat :app:dependencies`; `javap -c` on `TestRequestBuilder$ShardingFilter` in the cached runner-1.7.0.aar): a test runs in shard i when `Math.abs(description.hashCode()) % numShards == shardIndex`; anything that is not a single test passes. So the split is test by test, by a hash of the test's JUnit description, not by class and not by time. My reading: the hash is the Java String hash of "method(fully.qualified.Class)".
- Time per class (I read, the result XML of run 44314279343 on main, 2026-11-18, with class-times.ps1): 136 tests in 57 test classes (the 27 files in app/src/androidTest hold several classes each); their own times add up to 656.2 s of a 698 s step. The slowest classes: TransactionHistoryLargestFontLandscapeTest 41.9 s (2 tests), CashTickerLargestFontLandscapeTest 38.1 s (3), AppStartTimingTest 30.8 s (3), AllocationChoiceFocusTest 23.7 s (1), ClassListLargestFontLandscapeTest 23.4 s (2), ClassListLayerTest 23.3 s (2). The slowest tests are 10.6 to 24.7 s each, all largest-font landscape, keyboard, focus or lock tests; the 52 TalkBack-type tests are about 1 to 5 s each. The result file is named after the device (`TEST-ats-screen-tests-37(AVD) - 17.xml`), which supports L7's reading.
- The research's savings for approach 3 remain a model until measured.
