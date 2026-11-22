# plan/iterations/37-screen-test-shards.md

Stage: Build. Status: approved 2026-11-20 (your A); decisions 1 and 2 settled the same day (your A each; section 11). Steps 1 to 4 built (275ef42, 8093c8f and the documents); draft pull request #214 green on 8093c8f, 178 of 178 over 3 shards in 666 s, and a re-run of one shard counted 178 from two attempts (13.1); the Deploy review done, no high, medium or low finding (13.2). Done: pull request #214 merged as 919e7c4, main green on 3 shards (13.3). Branch iter-37-screen-test-shards from main (8a605c9); not created yet.
Source: plan/spikes/two-emulators.md, decision T11 (adopt 3 runners) and section 10 (its open item's text); series E's trial workflow, commit 407503c (closed pull request #206, read 2026-11-20 through refs/pull/206/head); CLAUDE.md section 4 (the test-support rule); your choice of 2026-11-20 (build T11 now, your A).

## 1. Outcome
The screen tests run on 3 GitHub runners at once instead of one: each runner boots its own emulator and runs a third of the tests, and a final job named `screen-tests` checks all their results together. Measured in series E: median 556 s against 841 s, so each pull request and each main run is green or red about 5 minutes sooner. The app, the tests themselves and the rule "a red run is not merged" do not change.

## 2. Spec refs
- None changed. AC-61 (screen tests on every pull request and on main) and plan.md section 3 ("a red run is not merged, and the run is red when fewer tests ran than there are @Test lines") hold as they are; the count is checked over the 3 shards together.
- A test-support change (CLAUDE.md section 4): no app behaviour, no spec, so no change request.

## 3. Out of scope
- The landscape-keyboard flakes (PriceKeyKeyboardLandscapeTest, PortfolioDeleteKeyboardLandscapeTest; three red runs on 2026-11-20): unchanged; sharding neither causes nor fixes them (plan/spikes/two-emulators.md section 9, D2).
- The later ideas in plan/spikes/two-emulators.md section 10: a fourth shard, skipping the screen tests on documents-only changes, cancelling superseded runs, the deprecated `-gpu swiftshader_indirect`.
- CI (.github/workflows/ci.yml) and CodeQL: unchanged.

## 4. Analysis (2026-11-20, by reading the workflows; nothing run)
- Today: one job `screen-tests` (178 lines): build, emulator, settle, PIN, `connectedDebugAndroidTest`, the count check against the `@Test` lines, screenshots, one artifact `screen-tests-attempt-N`.
- 407503c: a matrix job `shard` (`fail-fast: false`, shards 0 to 2, each with today's steps, `numShards`/`shardIndex` passed to the test runner, a check that the shard ran some tests, an artifact `screen-tests-shard-K-attempt-N`), then a job `screen-tests` (`needs: shard`, `if: always()`) that downloads every shard's artifact and checks each file, the total against the `@Test` count, that no test ran in two shards, and that the shard jobs succeeded. It also carries the spike's memory sampler and summary, which are left out here (T11's text).
- Re-runs (a defect in 407503c's shape, found reading it): the count job downloads the pattern `…-attempt-${{ github.run_attempt }}`. A re-run of only the failed shard (what you approved three times on 2026-11-20) makes attempt 2, where only that shard uploads `…-attempt-2`; the green shards' results stay as `…-attempt-1`, so the count job would find one shard and go red after a green re-run. Fix: download every shard artifact of the run and use, per shard, the newest attempt's results (decision 1). Whether attempt 2's `download-artifact` sees attempt 1's artifacts at all is my reading, not checked (section 10); step 3 proves it.
- The required check: the repository's ruleset requires a check named `screen-tests` (plan/spikes/two-emulators.md T10). The count job keeps that name; the shard jobs are named `screen-tests shard K`, which the ruleset does not ask for. Read with gh (read only) in step 2, not changed.
- My local reading of results: the artifact names change, and a shard's files sit in their own folder; counting `<testcase>` over every `TEST-*.xml` under the download still works.

## 5. Files
- .github/workflows/screen-tests.yml: the matrix of 3 shard jobs and the count job `screen-tests` (from 407503c, without the memory sampler and summary; the count job takes each shard's newest attempt).
- plan/open-items.md: T11's open item (its text in plan/spikes/two-emulators.md section 10), added in step 1 and closed in step 2.
- plan/plan.md: row 37, section 11, the change log; section 2's file list names the spike notes (avd-memory, dependabot-findings, two-emulators; open since plan/spikes/avd-memory.md section 10, held until #190 and #193 to #196 merged, now all merged or, for #196, its own branch).
- CLAUDE.md: the pitfall on `gh run download` and the artifact's name, for shard artifacts (step 4; shown to you first).
- No file under app/ or core/.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-37-screen-test-shards, one pull request.
1. This file, plan.md's lines (row 37, section 11, the change log, section 2's spike notes), T11's open item in plan/open-items.md.
2. The workflow: 3 shard jobs and the count job (decision 1); the ruleset read (gh, read only); open item closed. The local run proves nothing about a workflow beyond the app still building; the proof is step 3.
3. The pull request (draft, on your word): the first run, all 3 shards and `screen-tests` green, 178 of 178 counted once each, each shard's time and the whole run's time against today's (series A's median 841 s); then decision 2's re-run of one shard job, with your yes, to prove the count job takes the newest attempt.
4. Documents: the results in this file, CLAUDE.md's pitfall (shown first), the Deploy review.

## 7. Stubs
None.

## 8. Risks
- 1. Re-runs (section 4): if attempt 2 cannot download attempt 1's artifacts, decision 1's fix does not work, and a failed shard can only be re-run with "Re-run all jobs" (about 10 minutes instead of 15 to 17, so still shorter than today). Step 3 shows which.
- 2. The required check: if the ruleset names a check other than `screen-tests`, the pull request cannot merge until it is changed, which is your setting; step 2 reads it first.
- 3. Runner minutes: about 1.3 to 1.9 times today's (3 setups of about 3.5 minutes each). The repository is public, so they cost nothing (plan/spikes/two-emulators.md section 3).
- 4. The flakes: a red shard's re-run now repeats only that shard's third of the tests.
- 5. The local build's intermittent failures (open item 60): as before.

## 9. Proof
- Step 3's runs on the pull request: 3 shard jobs green, `screen-tests` green with 178 of 178 and none twice; the times; one re-run of a single shard ending green with the count still 178.
- On main after the merge: the same.

## 10. Research
- L1 (answered by step 3's re-run, 2026-11-20: yes; no look-up made): in a re-run attempt, `actions/download-artifact` with a pattern finds the artifacts uploaded by earlier attempts of the same run. A look-up would be a web fetch of the action's documentation (asks first).

## 11. Decisions (Build, settled by you 2026-11-20)
1. How the count job treats a re-run (settled 2026-11-20: your A):
   - (A, chosen) Download every `screen-tests-shard-*` artifact of the run and, per shard, use the highest attempt's results. A re-run of only the failed shard then counts all 178. Maintainability: about 10 more lines of shell in one step. Test run time: no real difference. APK size: none (no app change).
   - (B) Keep 407503c's attempt-scoped pattern; a failed shard is re-run with "Re-run all jobs" only. Maintainability: no extra lines. Test run time: each re-run repeats all 3 shards (about 10 minutes against about 5). APK size: none.
2. Proving the re-run path in step 3 (settled 2026-11-20: your A):
   - (A, chosen) After the first green run, re-run one shard job once (with your yes), whatever its result, to see the count job take the newest attempt.
   - (B) No deliberate re-run; the first real flake proves it.

## 12. Wording to approve
None (no app text).

## 13. Status and notes
- Draft 2026-11-20; approved the same day (your A).

### 13.1 Steps
- Steps 1 and 2 built without a stop between them (your instruction, 2026-11-20), each step's state held for its own commit.
- Step 1 (2026-11-20): this file; plan.md (section 2's spike notes and file range, row 37, section 11's row, the change log); open item 70 (T11's text from plan/spikes/two-emulators.md section 10). Only documents changed. The full run green: 1,511 tests (1 skipped), 178 screen tests compiled, lint's 10 (report 18:21:22); "11 history" passed on try 3.
- Step 2 (2026-11-20): .github/workflows/screen-tests.yml, 233 lines: the job `shard` (named `screen-tests shard K`, `fail-fast: false`, shards 0 to 2, `NUM_SHARDS: "3"` in the workflow's env), each with today's steps, the test runner's `numShards` and `shardIndex`, a check that the shard ran tests, and its artifact `screen-tests-shard-K-attempt-N`; the job `screen-tests` (`needs: shard`, `if: always()`) downloads every `screen-tests-shard-*` artifact of the run and, per shard, uses the highest attempt (decision 1), then checks the total against the `@Test` lines, no test in two shards, and that the shard jobs succeeded. Against 407503c: 25 lines in, 51 out (the memory sampler, its summary and their files). The ruleset (gh, read only): "default branch protection", active, requires build, CodeQL and screen-tests, so it is unchanged. Open item 70 closed. The workflow is not run locally; the local full run shows only that the app and the tests still build; the proof is step 3. Not checked before the push: the YAML's validity (no local linter; GitHub rejects an invalid workflow at the push). Committed with step 1 after a first try timed out at the GPG pinentry: 275ef42 (step 1), 8093c8f (step 2).
- Step 3 (2026-11-20): pushed; draft pull request #214 (reviewer msfsgh). Its first run (Screen tests run 44724078974, attempt 1) green: shard 0 53 tests (9m 56s, tests 369 s), shard 1 59 tests (8m 6s, tests 289 s), shard 2 66 tests (10m 51s, tests 401 s); the count job `screen-tests` (9 s): 3 artifacts, each shard at attempt 1, "Tests run: 178 of 178; in more than one shard: 0". The whole run 666 s (11:01:12 to 11:12:18), against the one-runner runs of the same day, 941 to 1,266 s (#195 15m 41s, #212 21m 6s, main 16m 15s to 17m 53s). CI (4m 28s; "Scale test retries": none), CodeQL and Analyze (actions) green. Decision 2, with your yes: `gh run rerun --job` on shard 1 (attempt 2): shard 1 green again (9m 49s), shards 0 and 2 not re-run; the count job (5 s) "Found 4 artifact(s)", used shard 0 attempt 1, shard 1 attempt 2, shard 2 attempt 1, "178 of 178", none twice, green. L1 answered: a re-run attempt's `download-artifact` finds the earlier attempts' artifacts (section 10).

### 13.2 Deploy review (2026-11-20)
- The Deploy review (Ruleset A rule 6), by reading the workflow against this file, AC-61, plan.md section 3 and the ruleset. No high, medium or low finding. What held: the run is red when fewer tests ran than there are `@Test` lines (the total over every shard's newest attempt) and a shard refuses zero tests; the required check `screen-tests` is red unless every shard job ended `success`, which after a re-run reflects the re-run (attempt 2); a test in two shards or a shard with no results makes it red; a shard whose re-run uploaded nothing cannot pass on old results, because its job result is not `success`; permissions stay `contents: read` and only the constant `matrix.shard` and `github.run_attempt` reach the scripts; the ruleset (build, CodeQL, screen-tests) unchanged.
- Notes (info, no change; your A): 1. `NUM_SHARDS` and the matrix list change together (a comment says so). 2. A cancelled run ends with `screen-tests` red, not cancelled (`if: always()`); a cancelled run is not merged either. 3. Runner minutes about 1.5 to 2 times today's; free for a public repository.
- CLAUDE.md (your A): section 4's convention on red runs names the shard artifacts and the re-run of one shard; section 6's `gh run download` pitfall corrected: it fetches every attempt's artifacts in their own folders (this session's downloads after a re-run held both attempts).

### 13.3 Merge (2026-11-20)
- Step 4's record (39d6f0e) pushed; #214 green on it at the first attempt: shards 0 to 2 (10m 13s, 8m 23s, 11m 9s), the count job 178 of 178, the whole Screen tests run 11m 22s; CI (4m 46s; "Scale test retries": none), CodeQL and Analyze (actions) (gh, read only).
- Pull request #214 made ready and merged by you as 919e7c4, 3 commits (275ef42, 8093c8f, 39d6f0e). On main after the merge, the first sharded run: the Screen tests (run 44736865587) 10m 22s, shards of 53, 59 and 66 tests, the count job 178 of 178, none twice; CI (5m 0s; "Scale test retries": none) and CodeQL green, all at the first attempt (gh, read only). Merge record on docs/iter-37-merged.

## 14. Departures from the plan
None yet.
