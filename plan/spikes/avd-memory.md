# plan/spikes/avd-memory.md

Stage: Build. Status: written 2026-11-18 on branch spike-avd-memory (from main at 9d952cb); approved by you 2026-11-18 as written, before the look-ups (section 5). Nothing in it has run.
Source: your request of 2026-11-18 (more emulator memory on GitHub's runner, to shorten the screen tests), your choice A (memory only, other levers listed for later), .github/workflows/screen-tests.yml (read 2026-11-18), plan/spikes/screen-tests.md sections 3 and 11.6, the last 10 screen-tests runs on main (read with gh, 2026-11-18).

How sources are labelled in this note:
- "I read": read by me in this session with gh or from the repo, with the date.
- "my reading": my interpretation. Not a fact until a look-up or the run shows it.
- "not checked": not looked at.

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

## 1. Outcome
A measured answer to one question: does giving the emulator more memory than today's 4,096 MB shorten the "Screen tests" step on GitHub's runner, with every run still green? It passes when:
1. Each memory setting in section 4 has a counted series on the same commit and the same day, alongside a 4,096 MB control series.
2. Each run records its step times, its test count (136 of 136 or the count of the day), the emulator's own RAM as it reports it, and the runner's free memory while the tests run.
3. Section 9 says, by section 6, whether one setting is better, and by how much, or that none is.
4. You choose to adopt one setting, or none (decision M5).
Other levers (time spent per test in waits, turns and unlocks; sharding the tests over two runners; snapshot handling) are listed in section 10 for later and are not measured here (your A).

## 2. Gate
This is a decision note, not an iteration. It gives no app feature, changes no app code and no spec, and blocks no iteration.
Branch: spike-avd-memory (from main at 9d952cb), one pull request with this note, merged by you.
Trial changes to the workflow go only on a throw-away branch (CLAUDE.md section 4), throw-away-avd-memory, with a draft pull request that is closed unmerged. The name has no NN: a spike has no iteration number (decision M4).
Order: the note's approval, the look-ups (section 5), decisions M1 to M4, the run (section 8), results (section 9), then M5.
An adopted setting reaches main as one line in .github/workflows/screen-tests.yml. It changes no app behaviour and no spec, so it is not a change request (CLAUDE.md section 4, the test-support rule); M5 says how it lands.

## 3. Where things stand
- The workflow (I read, 2026-11-18): .github/workflows/screen-tests.yml starts the emulator with `-memory 4096 -cores 4` (lines 100 to 102), on ubuntu-26.04, with a 45-minute job limit. 4,096 MB came from plan/spikes/screen-tests.md 11.6: with the device profile's 2 GB, System UI stopped responding (run 1 on 039611f).
- The runner (I read, run 43793616953): 4 CPUs; `free -m` before the emulator starts: 15,700 MB total, 14,229 MB available. The emulator already has all 4 CPUs.
- The last 10 screen-tests runs on main (I read, 2026-11-18; runs 43376432771 to 43793616953, 2026-11-13 to 2026-11-16; all green, all with the emulator and image from the cache; 136 of 136 tests in the latest):

| Part | Shortest | Longest |
|---|---|---|
| Whole job | 710 s (11m 50s) | 902 s (15m 2s) |
| Build the app and the tests | 70 s | 111 s |
| Wait for the boot | 38 s | 53 s |
| Let the system settle | 10 s | 12 s |
| Screen tests | 518 s | 676 s |

- Per run (same source):

| Run | Commit | Whole job | Build | Boot | Settle | Tests |
|---|---|---|---|---|---|---|
| 43793616953 | 9d952cb | 785 s | 70 s | 50 s | 10 s | 590 s |
| 43750869998 | 0a74f3b | 902 s | 103 s | 48 s | 10 s | 676 s |
| 43698170720 | fde0080 | 832 s | 80 s | 42 s | 12 s | 624 s |
| 43695749891 | be02246 | 805 s | 74 s | 44 s | 11 s | 604 s |
| 43617954249 | 924304d | 866 s | 95 s | 48 s | 12 s | 649 s |
| 43611682560 | cd5158d | 710 s | 71 s | 38 s | 10 s | 518 s |
| 43608781527 | 93bfec4 | 775 s | 71 s | 44 s | 10 s | 559 s |
| 43563386257 | cfa66b9 | 874 s | 111 s | 53 s | 12 s | 635 s |
| 43557807830 | ad9c2b6 | 839 s | 106 s | 50 s | 10 s | 616 s |
| 43376432771 | 2459099 | 769 s | 103 s | 50 s | 10 s | 537 s |

- The tests are about three quarters of the job: 136 tests at about 4.3 s each. In plan/spikes/screen-tests.md 11.6 there were 6 tests and the step took 79 to 117 s; build, boot and settle have hardly changed since.
- The longest run (902 s) is just over the 15 minutes plan/spikes/screen-tests.md section 3 (S3) set for "can run on every change". That limit was set for 6 tests; it is a yardstick here, not a rule.
- Run times on the runner vary by about 160 s between runs (518 to 676 s for the tests), so one run per setting cannot show a difference.
- Not checked: whether memory is the limit today. The workflow log of run 43793616953 has no "Not Responding" line from the settle step; the emulator log and the logcat in the runs' artifacts were not read (section 5, L1).

## 4. Approaches
All my reading, to be corrected by section 5. Each is one value of `-memory` on the throw-away branch; nothing else in the workflow changes except the measuring added for the run (section 8).
- A, control: 4,096 MB (today). Measured again the same day, so the comparison does not lean on last week's runs.
- B: 6,144 MB. Leaves the runner about 8 GB for Gradle, the test driver and the system, by the figures above (my reading).
- C: 8,192 MB. Leaves about 6 GB. The Gradle daemon from the build step stays alive during the tests (my reading), so C is the one most likely to squeeze the runner.
Why more memory could help (my reading): fewer low-memory kills of the app or System UI between tests, a larger page cache in the guest, fewer pauses. Why it may not: the 4 CPUs are shared by the runner, the emulator and the test driver; if the CPUs are the limit, the time stays the same.

## 5. Questions to look up
None needs a web search or fetch. Each gh call is a shell command and asks first (CLAUDE.md section 8).
- L1 Is there memory pressure today? Download the artifact of run 43793616953 (and of the slowest, 43750869998) into the scratchpad and read emulator.log and the test results' logcat for low-memory kills ("lowmemorykiller", "Killing", "lmkd"), long garbage-collection pauses and "Not Responding". If there is none, say so in section 9 before the run: the run may then show no gain.
- L2 Does `-memory` set the guest's RAM, whatever the AVD's config.ini says (hw.ramSize)? Read emulator.log of a run for the RAM it reports; the run then checks each setting the same way.
- L3 Which tests take the time? From the latest run's result XML, the 10 slowest tests and the total of the 136. This does not steer this spike; it feeds section 10.
- L4 Does a larger guest RAM change the app's own heap limit (dalvik.vm.heapsize)? My reading: no, it comes from the image's properties, not from `-memory`. Read with `adb shell getprop dalvik.vm.heapsize` in each run.

Answers (I read, 2026-11-18; the artifacts of runs 43793616953, the latest, and 43750869998, the slowest, in the scratchpad; 136 logcat files of about 108,000 lines each run):
- L1 No sign of memory pressure in either run: no kill by lmkd, no "Low on memory", no "Not Responding" or "ANR in", no process "has died". The lmkd lines (21 and 7) are its settings being reloaded ("lmkd updated properties successfully"), my reading: when the tests change system settings such as the font scale. The onTrimMemory lines (3 and 4) are Google's TimedProcessReaper ending a background process "to refresh configuration", not a shortage. The "Waiting for a blocking GC" lines (9 and 13) are nearly all ProfileSaver in system apps, none in the app. The garbage-collection lines show heaps of 12 to 16 MB, about half free.
- L1, the runner: the emulator warns in both runs "Suggested minimum system RAM to run avd 'ats-screen-tests-37' is 16384 MiB (available: 15700 MiB)" (15983 MiB in the other run). This is a check the emulator makes at its start, not a measured shortage: the runner has less RAM than the emulator's suggested minimum; whether it runs short during the tests is not measured (the sampler in section 8 measures it). Not checked: how the emulator works out 16384 MiB, and whether it rises with `-memory`.
- L2 The guest has 4 GiB: emulator.log's "pc_memory_init: above 4g size: 40000000" is 1 GiB above the 4 GiB line, plus 3 GiB below it (my reading of the x86 layout). The emulator log has no other RAM line; config.ini was not read.
- L3 The time is spread over the tests: the 136 tests' own times add up to 556.6 s (of a 590 s step) and 636.7 s (of 676 s). The 10 slowest are 10.9 to 24.7 s each, mostly largest-font landscape and keyboard tests; the slowest is TransactionHistoryLargestFontLandscapeTest.theFieldsChipsRowsAndLinks_canBeReached_andTapped (21.5 and 24.7 s). The slow run is slower in each of the top 10 classes, by about 10 to 40%, not in a few tests. My reading: the runner's speed differs between runs, not the memory.
- L4 Not read: it needs a running emulator (section 8, step 1).

## 6. Criteria for the choice
Hard rules: a setting that breaks one is out, whatever its time.
- H1 Every run of the series green, every test run (the count check's "Tests run: N of N").
- H2 No "Not Responding" window in the settle step, no unlock retry, no low-memory kill of the app.
- H3 The runner's available memory, sampled while the tests run, never below 1,024 MB (my figure; M1).
Then, ranked:
1. The median of the "Screen tests" step against the control's median, same day, same commit.
2. The longest "Screen tests" step against the control's longest.
3. The whole job's median.
A setting counts as shorter only if its median is at least M1's margin below the control's and its longest run is not longer than the control's longest.

## 7. Decisions
To settle after the note's approval and the look-ups, one question at a time.
Settled 2026-11-18, after the look-ups (section 5) found no memory pressure: your B, a small check run. M2 is 3 runs per setting, M3 is A (4,096 MB) and C (8,192 MB) only; B (6,144 MB) is not run. With runs already about 160 s apart (section 3), 3 runs per setting can show only a large gain.
M1 settled 2026-11-18, your B: as written in section 6 (the median of the test step at least 60 s below the control's, and the longest run not longer than the control's longest); H3's floor 1,024 MB. The stricter rule (every 8,192 MB run shorter than every 4,096 MB run) was not chosen.
M4 settled 2026-11-18, your A: throw-away-avd-memory, without NN (section 2). M5 waits for the results.
- M1 The margin for "shorter" and H3's floor. My leaning: 60 s off the median of the test step (about 10%), floor 1,024 MB.
- M2 Runs per setting. My leaning: 5 (15 runs, about 3.5 hours of runner time at today's 14 minutes a run). 10 per setting as in plan/spikes/screen-tests.md 11.6 would be 30 runs and about 7 hours. Not checked: whether this repository's Actions minutes are free or counted.
- M3 Which settings. My leaning: A, B and C as in section 4.
- M4 The throw-away branch's name (section 2).
- M5 After the results: adopt B, adopt C, or keep 4,096 MB. If one is adopted, my leaning: record it as an open item and change the line as a step of the next iteration, as the test-support rule does (CLAUDE.md section 4); the other way is a commit on spike-avd-memory, in this note's pull request.

## 8. The run
Each push by you; each run and re-run with your yes; I read each with gh.
1. Branch throw-away-avd-memory from main. Commit 1 adds measuring only, with `-memory 4096`: a background sampler that writes the runner's `free -m` and the guest's /proc/meminfo every 30 s from the boot to the end of the tests; `adb shell getprop dalvik.vm.heapsize` after the boot; the sampler's file and the guest's `logcat -b events` (am_low_memory, am_kill lines) kept in the artifact. The test count, the tests and the app are unchanged.
2. You push and open a draft pull request. Series A: M2 runs, as attempts of the same run (each attempt's artifact downloaded before the next, CLAUDE.md section 6).
3. Commit 2 changes only `-memory 4096` to `-memory 8192`. Series C, the same way. (Series B, 6,144 MB, is not run: section 7.)
4. Not used.
5. A red run in a series is read for its cause before anything else (CLAUDE.md section 4); a red run caused by memory ends that setting's series and is H1's answer for it.
All three series on the same day if the runner allows, so the runner's own speed changes as little as possible.

## 9. Results
Not run.

## 10. Cleanup and follow-up
- Close the draft pull request unmerged; delete throw-away-avd-memory with your yes.
- M5's change, if any.
- Later, not in this spike (your A): time per test (L3's slowest tests; waits, turns, unlocks), sharding over two runners, the emulator's snapshot handling. Each would be its own note.
- plan/plan.md section 2 does not list plan/spikes/screen-tests.md or this note; whether to list them is for the merge of this note.
