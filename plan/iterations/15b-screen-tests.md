# plan/iterations/15b-screen-tests.md

Stage: Build. Status: approved 2026-11-06 (your A). Decisions 1 to 4 settled 2026-11-06 (section 10).
Source: plan/plan.md section 4 (row 15b), intent/intent-cr-03-screen-tests.md (CR3), spec/spec.md v6 (final; C19, C20, AC-61, AC-62, section 9), plan/spikes/screen-tests.md (section 11), branch spike-screen-tests-run (2fed892). "My reading" is an interpretation, not a test.

## 1. Outcome
The four spike checks become tests of the app and run on GitHub's runner on every pull request and on main after each merge (AC-61). A red run is not merged. Nothing in the app changes: the release APK stays content-identical. From 16 on, every new or changed screen with a layer, a menu, a fixed part or a text field gets the checks that apply to it, and 16 is the first iteration in which these tests and the phone check both run (AC-62).

## 2. Spec refs
- Spec: AC-61 (the screen tests), AC-62 (which hand checks end, and when), C19, C20, section 9 (test data only, a throwaway test PIN).
- Criteria checked by the tests: AC-39 (largest font; TalkBack labels) and AC-45, AC-48, AC-55 (the screens the four checks use). The standing checks AC-09, AC-35, AC-39 to AC-42 need no phone run here, since the app does not change (section 8, proof 4).
- The four past defects, now covered on GitHub: 10 (Holding detail's header), 07 (layers, menus and the cover screen), 08 (the keyboard), and the TalkBack label of 14c (Deploy finding 2).

## 3. Out of scope
- Screen tests for the screens built before 15b beyond the four checks (open item 48 lists the screens without a model).
- TalkBack speech, the themes (AC-40), the network (AC-42), installing over data (AC-35), the lock and Keystore, blocked screenshots and Recents (AC-62: they stay phone checks).
- Local runs on your PC (intent: not required; spike 11.8).
- The "database is locked" observation (spike 11.6): not part of CR3; asked about separately.
- Open items 45 and 32; the phone check of open item 33 (34).

## 4. Files
New, app/src/androidTest/java/app/ghghorg/atsassets/ (from spike-screen-tests-run, 2fed892): ScreenTestSupport.kt (the spike's SpikeSupport.kt, renamed: launching and unlocking MainActivity with the test PIN, the focus listener, font scale, rotation, the keyboard's top, and the diagnostics after a failed unlock), HoldingHeaderTest.kt (10: largest font in landscape, and normal font in portrait as a control), LockAndLayerTest.kt (07), KeyboardLayerTest.kt (08), TalkBackLabelTest.kt (TB: the reversal card and the Transactions row). 6 tests.
New: .github/workflows/screen-tests.yml (the spike's workflow at 2fed892, with the triggers and count of decisions 1 and 2).
Documents: plan.md (section 3, a technical decision: screen tests on an emulator on every pull request; the 15b row; the change log), CLAUDE.md (the convention: a new or changed screen with a layer, a menu, a fixed part or a text field gets the AC-61 checks that apply; a red screen-tests run is not merged), open-items.md (item 47: done by CR3, once 16 has run side by side).
No change under app/src/main, no new library, no catalog entry, no schema change, no new wording, no versionCode change.

## 5. Order of work
Each step ends with spotlessCheck, build, tests and lint (all with --rerun-tasks), plus :app:compileDebugAndroidTestKotlin from step 2 on, and the report; then I stop. Steps 2 to 5 run on branch iter-15b-screen-tests from main, once this file is approved and its documents are merged.
1. This file and the plan.md lines (the 15b row linking CR3; the change log). On branch docs/cr-03-screen-tests, with the CR3 intent and spec v6, as one pull request.
2. The test code: the five files from 2fed892 (decision 3), renamed as section 4 says. Baseline run before (649 tests), the same run after (unit tests unchanged), and the androidTest sources compile.
3. The workflow: screen-tests.yml with decisions 1 and 2. You push once; the run on the pull request must be green with 6 of 6 tests.
4. Proof that it still catches (decision 4): the spike's defects on a throwaway branch from step 3, each run once on GitHub, each red at its own check, 07a included (the spike ran 07a only locally). The branch is never merged.
5. Conventions: plan.md section 3 and CLAUDE.md, wording shown to you first.
6. Documents, the release APK comparison (section 8, proof 4), and the pull request draft; the open item 33 reminder; the cleanup asks (spike-screen-tests-run, spike-screen-tests-defects, step 4's branch, the local AVD and image).

## 6. Stubs
- None.

## 7. Risks
- Cost (C20): the runs are free only while the repo is public and the emulator cache fits GitHub's 10 GB. Each branch fills its own cache unless main's is found (spike 11.3, look-up 8); runs on pull requests can read main's cache. If usage nears the limit, the runs stop and you decide.
- Flakiness: 10 of 10 on 2fed892, but every pull request now waits on it. A red run with no defect blocks a merge; its cause is recorded, and a re-run needs your yes (as in the spike).
- Run time: about 5 to 6 minutes per pull request, docs-only pull requests included (intent: every pull request).
- GitHub's runner image and the emulator package change over time: a new emulator version changes the cache key, and the next run is a cold one (about 13 minutes in the spike's only cold run, which failed for another reason).
- 07a was caught only locally in the spike; step 4 runs it on GitHub.
- The emulator is not the phone (C19): AC-62 ends a hand check only after 16 has shown the tests catch what the phone check finds.
- The test PIN is public in the repo (section 9: throwaway, emulator only, never the phone's).

## 8. Proof
Commands: as in 15a, all with --rerun-tasks, plus :app:compileDebugAndroidTestKotlin. CI green on the pull request, and the screen-tests run green with 6 of 6 tests (your report or gh, read only).
1. Step 3: the pull request's screen-tests run green; its total time and phases recorded.
2. Step 4: each defect red at its own check, with its own message (10, 08, 07a, 07b, TB card); the TB row and the portrait control stay green.
3. The run on main after the merge: green (your report).
4. The release APK: :app:assembleRelease and :app:dependencies --configuration releaseRuntimeClasspath on main before and on the branch after; the same entries with the same CRC-32 and the same dependency list (as spike 11.8).
No phone check: nothing under app/src/main changes, so nothing the phone shows can change (proof 4).

## 9. Research
- None needed: no new library. The action versions are those of spike 11.5 (2026-11-05); looked up again on the day of step 3 with gh release view (read only, gh signed in by you).

## 10. Decisions (Build, settled by you 2026-11-06)
1. Settled 2026-11-06 (your A): the screen tests run in their own workflow file, screen-tests.yml, triggered by pull requests (any base, as ci.yml) and push to main; ci.yml unchanged. The two jobs run side by side, so a pull request waits for the longer one (about 5 to 6 minutes), and a red screen test shows by name. Not chosen: (B) a second job in ci.yml.
2. Settled 2026-11-06 (your A): the workflow counts the @Test lines in app/src/androidTest during the run and is red when the results hold fewer <testcase> entries; nothing to update when tests are added. A stray @Test in a comment or an @Ignore'd test errs toward red. Not chosen: (B) a number written in the workflow (the spike's TESTS_EXPECTED).
3. Settled 2026-11-06 (your A): the spike's test code is kept as written at 2fed892, with the renames only ("spike" leaves names and comments); the diagnostics after a failed unlock stay (screenshot, window focus and node tree in the log, the workflow's screenshot pull into the artifact), test data only (section 9). Not chosen: (B) the diagnostics removed.
4. Settled 2026-11-06 (your A): step 4's defects run through a draft pull request from the throwaway branch into iter-15b-screen-tests, which you create and close unmerged; I push each defect commit with your "push once". Two runs: 10, 08, 07b and TB's card together, then 07a in place of 07b (07a and 07b break the same test). Not chosen: (B) a push trigger for that branch, added for step 4 and removed after.

## 11. Wording to approve (D9)
- None: no text the app shows. The workflow's failure messages and CLAUDE.md's convention are shown to you in steps 3 and 5.

## 12. Status and notes
Written 2026-11-06 on branch docs/cr-03-screen-tests; approved by you 2026-11-06 (as written, option A).
