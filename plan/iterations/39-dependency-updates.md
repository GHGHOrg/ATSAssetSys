# plan/iterations/39-dependency-updates.md

Stage: Build. Status: approved 2026-11-22 (your A). Branch iter-39-dependency-updates from main (eb5c5a9).
Source: plan/spikes/dependabot-findings.md (look-ups L1 to L10, decisions D1 to D5, verdicts in section 9.4; run on d182dfb); the open Dependabot pull requests #165, #164, #125 and #124 and the closed #123 (I read with gh, 2026-11-22: #123 closed by Dependabot 2026-11-21, "Looks like net.zetetic:sqlcipher-android is no longer updatable"); plan/open-items.md items 20, 43, 60 and 73; the hand-off of 2026-11-22 (open item 73's two later occurrences); your answers of 2026-11-22: no intent file (A), SQLCipher left out (A), open item 73 recorded and fixed here (A), no release (A).

## 1. Outcome
The build uses Gradle 9.9.1, Spotless 8.11.0, KSP 2.3.14 and kotlinx-coroutines-test 1.11.0. Dependabot pull requests #165, #164 and #124 are merged with the iteration; #125 is closed as superseded by 2.3.14. Lint drops three findings (Spotless, coroutines-test, KSP) and gains none unlisted. Open item 73's wait is fixed, or what was ruled out is recorded. Your app stays at 0.38.0: no release (your A, 2026-11-22).

## 2. Spec refs
- None changed. AC-35 (install over the previous build) on the development app; AC-61 (screen tests) and AC-63 (the release build checked by CI) hold as they are.
- No app behaviour and no spec change, so no change request (your A, 2026-11-22; as 22a, 24a and 37).

## 3. Out of scope
- SQLCipher 4.20.0 (#123, closed; open item 43 stays open for a change of its own; your A, 2026-11-22).
- AGP 9.5.1 (open item 31), kotlinx-serialization-json 1.13.0, lifecycle 2.11.1, compose-bom 2026.11.01: lint findings with no Dependabot pull request.
- A release (your A). The updates reach your app with the next release.
- Open items 63, 69 and 71.

## 4. Analysis (2026-11-22, by reading; nothing run)
- The spike's verdicts hold for d182dfb. Main has moved since (35 to 38c): the release build with R8 and the class check (38b) run in CI, and the code KSP generates now passes through R8 in the release build. Every check runs again here.
- Dependabot heads now (gh, 2026-11-22): #165 7b85938 (wrapper 9.9.1), #164 b0fae1d (Spotless 8.11.0), #125 6aa75d6 (KSP 2.3.14; the title moved from 2.3.13), #124 571f0ee (coroutines-test 1.11.0). Not yet read: whether each diff is still the one L1 read, and whether #165 still stores gradlew.bat as CRLF.
- #125 now asks for 2.3.14 itself, so spike decision D5's hand edit may no longer be needed (decision 1).
- Open item 73: three red runs in a row on shard 1 since #220, green before (#218, #219). Each time a layer "was still open 15 s after" its confirm button, with the window focused: #220 (run 44937015046, attempt 1), #222 (run 45001099474, attempt 1: PortfolioDeleteOverTheBarTest.theBottomBar_takesNoTap_whileTheDialogIsOpen, in createPortfolio), main eb5c5a9 (run 45002825295, attempt 1: LockAndLayerTest.layersAndMenu_keepTheFocusAndTakeInput, Rename's Save). Cause not looked at.

## 5. Files
- gradle/wrapper/gradle-wrapper.properties, gradle-wrapper.jar, gradlew, gradlew.bat (#165; gradlew.bat stored as LF, gradlew LF and 100755).
- gradle/libs.versions.toml: spotless, ksp, coroutines lines.
- app/src/androidTest/…: the wait open item 73 points to (ScreenTestSupport.kt or the two tests' helpers); known only after step 3.
- plan/open-items.md: 73's two occurrences (step 1) and its result (step 3); 20 closed; 43 noted (#123 closed by Dependabot).
- plan/plan.md: row 39, section 11, the change log.
- CLAUDE.md: section 6's lint list, and the Gradle version if named (shown first).
- No file under app/src/main or core/.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus :app:compileDebugAndroidTestKotlin, and the report. Then I stop.
1. This file; plan.md (row 39, section 11, the change log); open item 73's two occurrences. Baseline: test total, lint findings, the debug APK's and each classesN.dex's size; the release build and its class check (the hook asks), with its APK and dex sizes.
2. The four updates (decision 1), with versions checked on the day (each look-up asks first). gradlew.bat stored as LF. Gradle daemon stopped first. The full run, then the release build and the class check; sizes against step 1; AtsDatabase_Impl and Room's generated DAOs in the dex; the deprecations with --warning-mode all.
3. Open item 73: the artifacts of the green run 44924198765 and the three red runs (downloads ask first), compared: runner image, emulator and system image versions, the emulator log, the timing of the layer close. A fix to the wait in test support if the cause is found; otherwise what was ruled out.
4. The pull request (draft, on your word): CI with the release build and the class check, screen tests (all shards, the count), CodeQL. The phone check (decision 2).
5. Documents: the results here, open items 20 and 73, CLAUDE.md's lint list (shown first), the Deploy review.

## 7. Stubs
None.

## 8. Risks
1. KSP silently leaving out Room's code (google/ksp#3286 was in 2.3.13): step 2 compares dex sizes with step 1 and checks names from Room's generated code in the dex; a missing class is red even if every test passes.
2. gradlew.bat's line endings (spike 9.2, 10): stored CRLF it shows as modified and blocks branch switches; stored as LF in step 2.
3. Dependabot may force-push a head again during the iteration; the merge then takes the head of the day, read again before merging.
4. Open item 60 (local intermittent KSP or testFixtures failure): read the cause, re-run unchanged, and compare the APK and dex before any install (CLAUDE.md).
5. Open item 73's cause may not be found from the artifacts; the item then stays open with what was ruled out, and the pull request may still need a re-run (your yes each time).
6. A new Gradle deprecation (Configuration.setVisible, from a plugin, removed in Gradle 11; spike 9.2): recorded, not fixed.

## 9. Proof
- Step 2: the full run green with the same test total as step 1; lint's 10 findings less the three cleared, none new unlisted; release build and class check green; dex sizes and Room's names as above.
- Step 4: CI, screen tests and CodeQL green on the pull request; the phone check (decision 2).
- On main after the merge: the same; #165, #164 and #124 shown merged, #125 closed.

## 10. Research
- R1 Newer versions on the day than Gradle 9.9.1, Spotless 8.11.0, KSP 2.3.14, coroutines-test 1.11.0, and any known defect in those (web fetch, each shown first and asking first).
- R2 The four pull requests' diffs today (gh, read only).

## 11. Decisions (Build, to settle one at a time after approval)
1. How the updates reach the branch: (A) merge the Dependabot heads of #165, #164, #124 and #125 (now 2.3.14), signed, so GitHub marks them merged with the iteration; (B) set the versions by hand and close the four pull requests with a comment. My leaning: A, if R2 shows #125 as only the 2.3.14 line.
2. The phone check: (A) on ATS Dev, after setting up the lock, entering the key and restoring a test-data backup on the build of main, install this branch's build over it (adb install -r; the hook asks): unlock, every tab opens, one cash entry and its reversal; (B) none, as the change does not touch app code. My leaning: A, as KSP writes Room's code and the spike's phone check (V3) was not run.

## 12. Wording to approve
None (no app text).

## 13. Status and notes
- Draft 2026-11-22; approved the same day (your A). Your instruction (2026-11-22): work steps 1 to 3 without a stop between them, each step's state held for its own commit; commit only when the pull request is needed.

### 13.1 Steps
- Step 1 (2026-11-22): this file; plan.md (section 2's iteration range, row 39, section 11's row, the change log); open item 73's two later occurrences. Only documents changed. Baseline, every command with --rerun-tasks: spotlessCheck green (16 s); build green (43 s); tests green (1m 4s): 40 + 68 + 1 result files, 1,512 tests, 0 failures, 0 errors, 1 skipped; lint first red in `:core:compileKotlin` ("Failed to create MD5 hash for file … core\build\kotlin\compileKotlin\cacheable\dirty-sources.txt", open item 60's pattern; no crash log), green after `--stop` unchanged (1m 56s; report 17:30:32): 10 findings, as CLAUDE.md section 6 lists, except AGP now offered at 9.5.2 (was 9.5.1); :app:compileDebugAndroidTestKotlin green (37 s). The debug APK rebuilt on the fresh daemon (36 s): 44,619,618 bytes; classes.dex 17,676,928, classes2 621,696, classes3 210,632, classes4 13,536, classes5 37,912, classes6 436,560, classes7 2,819,972, classes8 61,968, classes9 43,560, classes10 13,519,180. Release build green (1m; "Unable to strip" as known): 14,475,110 bytes, classes.dex 5,881,376; the class check: 6,320 classes, 3,037 in the root package, 0 that cannot load. Room's generated classes in both (classes6.dex in debug, classes.dex in release): AtsDatabase_Impl, 6 DAOs (CashEntry, Instrument, Portfolio, Restore, Settings, Transaction) and 13 auto-migrations (1_2 to 13_14), 20 in all. Both APKs kept in the scratchpad (baseline39), not in the repo.

## 14. Departures from the plan
None yet.
