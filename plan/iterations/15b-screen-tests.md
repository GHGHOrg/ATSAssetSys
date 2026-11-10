# plan/iterations/15b-screen-tests.md

Stage: Build. Status: approved 2026-11-06 (your A); done: built and checked 2026-11-06 (section 13); pull request #132 merged as 57891e7 (2026-11-06). Decisions 1 to 4 settled 2026-11-06 (section 10).
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
- Local runs on your PC (intent: not required; plan/spikes/screen-tests.md 11.8).
- The "database is locked" observation (plan/spikes/screen-tests.md 11.6): not part of CR3; asked about separately.
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
- Cost (C20): the runs are free only while the repo is public and the emulator cache fits GitHub's 10 GB. Each branch fills its own cache unless main's is found (plan/spikes/screen-tests.md 11.3, look-up 8); runs on pull requests can read main's cache. If usage nears the limit, the runs stop and you decide. Seen in step 4 (section 13.2): a pull request cannot read another pull request's cache, so until main has one, each new pull request's first run installs the emulator and image (about 1.5 to 2 minutes more); its later runs hit its own cache.
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
4. The release APK: :app:assembleRelease and :app:dependencies --configuration releaseRuntimeClasspath on main before and on the branch after; the same entries with the same CRC-32 and the same dependency list (as plan/spikes/screen-tests.md 11.8).
No phone check: nothing under app/src/main changes, so nothing the phone shows can change (proof 4).

## 9. Research
- None needed: no new library. The action versions are those of plan/spikes/screen-tests.md 11.5 (2026-11-05); looked up again on the day of step 3 with gh release list (read only, gh signed in by you; section 13.2).

## 10. Decisions (Build, settled by you 2026-11-06)
1. Settled 2026-11-06 (your A): the screen tests run in their own workflow file, screen-tests.yml, triggered by pull requests (any base, as ci.yml) and push to main; ci.yml unchanged. The two jobs run side by side, so a pull request waits for the longer one (about 5 to 6 minutes), and a red screen test shows by name. Not chosen: (B) a second job in ci.yml.
2. Settled 2026-11-06 (your A): the workflow counts the @Test lines in app/src/androidTest during the run and is red when the results hold fewer <testcase> entries; nothing to update when tests are added. A stray @Test in a comment or an @Ignore'd test errs toward red. Not chosen: (B) a number written in the workflow (the spike's TESTS_EXPECTED).
3. Settled 2026-11-06 (your A): the spike's test code is kept as written at 2fed892, with the renames only ("spike" leaves names and comments); the diagnostics after a failed unlock stay (screenshot, window focus and node tree in the log, the workflow's screenshot pull into the artifact), test data only (section 9). Not chosen: (B) the diagnostics removed.
4. Settled 2026-11-06 (your A): step 4's defects run through a draft pull request from the throwaway branch into iter-15b-screen-tests, which you create and close unmerged; I push each defect commit with your "push once". Two runs: 10, 08, 07b and TB's card together, then 07a in place of 07b (07a and 07b break the same test). Not chosen: (B) a push trigger for that branch, added for step 4 and removed after.

## 11. Wording to approve (D9)
- None: no text the app shows. The workflow's failure messages and CLAUDE.md's convention are shown to you in steps 3 and 5.

## 12. Status and notes
Written 2026-11-06 on branch docs/cr-03-screen-tests; approved by you 2026-11-06 (as written, option A). Step 1 merged with the CR3 documents in pull request #131 (98e5fa6, 2026-11-06). Steps 2 to 6 built 2026-11-06 on iter-15b-screen-tests (pull request #132); merged as 57891e7 (2026-11-06), proofs 1 to 4 hold. AC-62's side-by-side check is iteration 16's (open item 47).

## 13. Build results
### 13.1 Commits
On iter-15b-screen-tests, newest first: e6211ab step 6, 32696a3 step 5, fd2ae5c merge of main (#131) before step 5, a747782 step 3, a9810dd step 2. Step 1: 79ab267 on docs/cr-03-screen-tests, merged in #131. main was 810fe19 when the branch was made, 98e5fa6 when it was merged in. #132 (5 commits) was merged into main as 57891e7; the branch is gone from origin and locally. The merge record: one commit on docs/iter-15b-merge-record. On throw-away-15b-defects (never merged; draft pull request #133, closed): d49aa8f (10), d8f6bb7 (07b), ab2c5e6 (08), 7083526 (TB card), then 861949a and d91c3c3 (07b and 08 reverted) and 99c1941 (07a), cherry-picked from spike-screen-tests-defects.

### 13.2 Runs
Local, on each step's code (2026-11-06, all with --rerun-tasks, plus :app:compileDebugAndroidTestKotlin from step 2): spotlessCheck green; build green; 649 unit tests, 0 failures (unchanged from 15a: no unit test added or changed); lint 5 findings, all listed in CLAUDE.md (the new one, NewerVersionAvailable for Spotless 8.11.0 at gradle/libs.versions.toml:12, listed in step 1; TypographyDashes now at strings.xml:86). The screen tests themselves were not run locally (section 3).
Action versions, gh release list (step 3): checkout v7.0.1, setup-java v6.2.0, gradle/actions v6.4.1, cache v6.1.0, upload-artifact v7.0.2; the same majors as plan/spikes/screen-tests.md 11.5.
On GitHub's runner (gh, read only):
- Proof 1, #132 (run 42166855837, on a747782): green, 6m 6s, "Tests run: 6 of 6". Emulator 37.2.13, image revision 8, 4 CPUs. The emulator cache missed (install 51 s, save 26 s); build 87 s; boot 48 s; settle 11 s, no Not Responding window; tests 107 s. The other checks: build 2m 49s, Analyze (actions) 54 s, CodeQL 2 s.
- Proof 2, #133 run 1 (run 42169370774, on 7083526; 10, 07b, 08, TB card): red as expected, 6m 48s (cache missed again: install 104 s), "Tests run: 6 of 6". HoldingHeaderLargestFontLandscapeTest red: "The fixed part stayed in place though it takes more than half the height (10): 72.0 px, then 72.0 px". KeyboardLayerTest red: "Cancel ends at 2118.0 px, below the keyboard's top at 1984.0 px (08)". LockAndLayerTest red: "MainActivity's window lost the focus 3 time(s) while the ⋮ menu was open, so the cover screen replaces it (07)". TalkBackLabelTest.reversalCard red: "Failed: assertExists.". The portrait control and the Transactions row green. The build check green (no unit test sees these defects). The figures equal plan/spikes/screen-tests.md 11.7's.
- Proof 2, #133 run 2 (run 42170419656, on 99c1941; 10, 07a, TB card): red as expected, 3m 49s (cache hit, boot 31 s), "Tests run: 6 of 6". LockAndLayerTest red: "MainActivity's window lost the focus 6 time(s) while New portfolio's layer was open, so the cover screen replaces it (07)": 07a caught on GitHub, which the spike showed only locally. 10 and the TB card red as in run 1; KeyboardLayerTest red by a 5000 ms timeout (the layer in its own window is not found; as in plan/spikes/screen-tests.md 11.4); the two controls green.
- Proof 4, the release APK (local, 2026-11-06): :app:assembleRelease on main (98e5fa6) and on 32696a3: both unsigned APKs 12,473,942 bytes, 458 entries each, none differing in size or CRC-32 (the files' hashes differ: zip metadata, my reading); :app:dependencies --configuration releaseRuntimeClasspath equal (764 lines compared). Nothing under app/src/main changed.
- #132 on the merged branch (run 42172540878, on e6211ab): green, 4m 58s, "Tests run: 6 of 6". The emulator cache hit (from #132's earlier run); boot 42 s; settle 12 s; tests 93 s. The other checks: build 2m 50s, Analyze (actions) 30 s, CodeQL 3 s.
- Proof 3, main after the merge (run 42184692649, on 57891e7): green, 6m 12s, "Tests run: 6 of 6". main had no emulator cache yet (a pull request's cache is not main's): install 45 s, then saved as emulator-37.2.13-android-37.0-google_apis-x86_64-8 (23 s), which later pull requests can read. Boot 49 s; settle 10 s; tests 1m 50s. CI on main green (2m 57s), CodeQL green.

### 13.3 Not run or not checked
The screen tests on your PC (not required, section 3); the phone (nothing it shows changed, proof 4); the cache use in GitHub (Settings > Actions > Caches, your check; C20).

## 14. Departures from the plan
1. Step 1 was not committed before step 2 (your request): its files were stashed, the code branch was made from main 810fe19 before the documents were merged (your B), and step 1 was committed later on docs/cr-03-screen-tests (79ab267) and merged in #131 before step 4. Section 5 had said steps 2 to 5 start from main after that merge. main was merged into the branch before step 5 (fd2ae5c, your A), so step 5's edits sit on #131's documents.
2. Step 2: besides the renames of decision 3, the comments cite plan/spikes/screen-tests.md by section instead of "spike F" (your B in step 3), and one reference to a choice the note does not record ("your B for test 10") became its reason. A first renaming script corrupted HoldingHeaderTest.kt (a one-pair list flattened by PowerShell); it was rewritten from the spike's copy before any commit, and differs from it in one name only.
3. Step 4: the throwaway branch is named throw-away-15b-defects (your rule, now in CLAUDE.md section 4). Run 2 ran on the same branch and pull request: 07b and 08 were reverted before 07a was picked, since 07a and 08 change the same layer.
4. Step 4's runs showed the cache scope (section 7, cost): each new pull request's first run is cold until main has a cache.
5. Lint's fifth finding (Spotless 8.11.0) appeared in step 1 and was listed in CLAUDE.md, not acted on.
6. Section 9 said gh release view; gh release list was used, since gh release view found no release without a tag.
