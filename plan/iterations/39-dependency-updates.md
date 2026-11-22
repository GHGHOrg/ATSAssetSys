# plan/iterations/39-dependency-updates.md

Stage: Build. Status: approved 2026-11-22 (your A). Branch iter-39-dependency-updates from main (eb5c5a9).
Source: plan/spikes/dependabot-findings.md (look-ups L1 to L10, decisions D1 to D5, verdicts in section 9.4; run on d182dfb); the open Dependabot pull requests #165, #164, #125 and #124 and the closed #123 (I read with gh, 2026-11-22: #123 closed by Dependabot 2026-11-21, "Looks like net.zetetic:sqlcipher-android is no longer updatable"); plan/open-items.md items 20, 43, 60 and 73; the hand-off of 2026-11-22 (open item 73's two later occurrences); your answers of 2026-11-22: no intent file (A), SQLCipher left out (A), open item 73 recorded and fixed here (A), no release (A).

## 1. Outcome
The build uses Gradle 9.9.2, Spotless 8.12.0 (one patch and one minor above #165 and #164; decision 1, your B), KSP 2.3.14 and kotlinx-coroutines-test 1.11.0. Dependabot pull requests #165, #164 and #124 are merged with the iteration; #125 is closed as superseded by 2.3.14. Lint drops three findings (Spotless, coroutines-test, KSP) and gains none unlisted. Open item 73's wait is fixed, or what was ruled out is recorded (step 3: its cause read and recorded, the fix left for later by your decision; section 14, item 4). Your app stays at 0.38.0: no release (your A, 2026-11-22).

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
1. How the updates reach the branch (settled 2026-11-22: your B, after R1 found Gradle 9.9.2 and Spotless 8.12.0: the four heads merged, then 9.9.2 and 8.12.0 on top; section 14, item 1). As drafted: (A) merge the Dependabot heads of #165, #164, #124 and #125 (now 2.3.14), signed, so GitHub marks them merged with the iteration; (B) set the versions by hand and close the four pull requests with a comment. My leaning: A, if R2 shows #125 as only the 2.3.14 line.
2. The phone check: (A) on ATS Dev, after setting up the lock, entering the key and restoring a test-data backup on the build of main, install this branch's build over it (adb install -r; the hook asks): unlock, every tab opens, one cash entry and its reversal; (B) none, as the change does not touch app code. My leaning: A, as KSP writes Room's code and the spike's phone check (V3) was not run.

## 12. Wording to approve
None (no app text).

## 13. Status and notes
- Draft 2026-11-22; approved the same day (your A). Your instruction (2026-11-22): work steps 1 to 3 without a stop between them, each step's state held for its own commit; commit only when the pull request is needed.

### 13.1 Steps
- Step 1 (2026-11-22): this file; plan.md (section 2's iteration range, row 39, section 11's row, the change log); open item 73's two later occurrences. Only documents changed. Baseline, every command with --rerun-tasks: spotlessCheck green (16 s); build green (43 s); tests green (1m 4s): 40 + 68 + 1 result files, 1,512 tests, 0 failures, 0 errors, 1 skipped; lint first red in `:core:compileKotlin` ("Failed to create MD5 hash for file … core\build\kotlin\compileKotlin\cacheable\dirty-sources.txt", open item 60's pattern; no crash log), green after `--stop` unchanged (1m 56s; report 17:30:32): 10 findings, as CLAUDE.md section 6 lists, except AGP now offered at 9.5.2 (was 9.5.1); :app:compileDebugAndroidTestKotlin green (37 s). The debug APK rebuilt on the fresh daemon (36 s): 44,619,618 bytes; classes.dex 17,676,928, classes2 621,696, classes3 210,632, classes4 13,536, classes5 37,912, classes6 436,560, classes7 2,819,972, classes8 61,968, classes9 43,560, classes10 13,519,180. Release build green (1m; "Unable to strip" as known): 14,475,110 bytes, classes.dex 5,881,376; the class check: 6,320 classes, 3,037 in the root package, 0 that cannot load. Room's generated classes in both (classes6.dex in debug, classes.dex in release): AtsDatabase_Impl, 6 DAOs (CashEntry, Instrument, Portfolio, Restore, Settings, Transaction) and 13 auto-migrations (1_2 to 13_14), 20 in all. Both APKs kept in the scratchpad (baseline39), not in the repo.
- Step 2 (2026-11-22). R2 (gh, read only): the heads are #165 7b85938 (Gradle 9.9.1; distributionSha256Sum d8a18a7a…, as L1; the jar's SHA-256 621008af…, as L6; gradlew mode 100755, LF; gradlew.bat in the commit as CRLF, 112 CR, 112 LF), #164 b0fae1d (spotless 8.11.0), #125 6aa75d6 (ksp 2.3.14: Dependabot moved it from 2.3.13, so spike decision D5's hand edit is not needed), #124 571f0ee (coroutines 1.11.0); one commit each, one line each in gradle/libs.versions.toml apart from #165. R1 (your yes; Invoke-WebRequest to services.gradle.org, plugins.gradle.org, then repo.maven.apache.org after the portal's 303, and repo1.maven.org; gh api for two release notes): newest are Gradle 9.9.2 (built 2026-11-19), Spotless 8.12.0 (2026-11-21), KSP 2.3.14 (2026-11-13), coroutines-test 1.11.0 (2026-05-08). Gradle 9.9.2's GitHub release lists only the 9.9 highlights (its fixes are on docs.gradle.org, not fetched); Spotless 8.12.0 changes Java, npm, SQL and Groovy steps and spotlessApply's report, nothing for ktlint or Kotlin. Decision 1 settled: your B. Gradle 9.9.2's checksums (your yes; services.gradle.org answered 301 to downloads.gradle.org): distribution 971fcd7150ca4ba04852c0b2ca0d2d0ab9468e14b03473eab19528809914c76b, wrapper jar 621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8 (the same jar as 9.9.1's).
  - Changes: gradle/libs.versions.toml (spotless 8.12.0, ksp 2.3.14, coroutines 1.11.0; CRLF kept, 52 CR, 52 LF). The wrapper by Gradle's own task, daemons stopped first: `wrapper --gradle-version 9.9.2 --gradle-distribution-sha256-sum 971fcd71…`, then `wrapper` under 9.9.2. The jar's SHA-256 621008af… equals the published one; gradlew, gradlew.bat and the jar equal #165's head with CR ignored; gradlew LF and 100755 (no mode change); gradlew.bat CRLF in the working copy and i/lf in the index, as .gitattributes asks. gradle-wrapper.properties differs from #165's: 9.9.2's URL and checksum, the timestamp comment gone, and two settings 9.9.2's task writes, `retries=0` and `retryBackOffMs=500` (section 14).
  - The full run on Gradle 9.9.2 (`--version`: Kotlin 2.4.21, launcher JVM 25.0.3), every command with --rerun-tasks: spotlessCheck green (15 s); build green (1m 33s), but `:core:compileKotlin` printed "Compilation in Kotlin daemon has failed … Using fallback strategy … Compile without Kotlin daemon" (open item 60's message; no crash log), so the daemons were stopped and the build run again unchanged: green (1m 12s), no such message. The debug APK 44,619,618 bytes and every classesN.dex equal to step 1's in size and SHA-256; Room's 20 generated classes in classes6.dex. Tests green (1m 5s): 109 result files, 1,512 tests, 0 failures, 0 errors, 1 skipped, as step 1. Lint green (2m 1s; report 17:53:11): 7 findings, step 1's 10 less Spotless, KSP and coroutines-test, none new (AGP 9.5.2, lifecycle-runtime-ktx and lifecycle-viewmodel-compose 2.11.1, compose-bom 2026.11.01, sqlcipher-android 4.20.0, kotlinx-serialization-json 1.13.0, TypographyDashes at strings.xml:86). :app:compileDebugAndroidTestKotlin first red in `:core:compileKotlin` with step 1's "Failed to create MD5 hash … dirty-sources.txt" (so on Gradle 9.6.0 and 9.9.2 alike; both times the run right after another forced run), green after `--stop` unchanged (1m 6s). Release build green (1m 12s; "Unable to strip" as known): 14,475,110 bytes, classes.dex 5,881,376 bytes with the same SHA-256 as step 1, 20 Room classes; the class check: 6,320 classes, 3,037 in the root package, 0 that cannot load. The only deprecation shown with --warning-mode all: `Configuration.setVisible(boolean)`, removed in Gradle 11 (spike 9.2; risk 6). KSP 2.3.14 wrote the same Room code byte for byte as 2.3.12.
  - The commits wait for the pull request (your instruction): step 1's commit, then the four Dependabot heads merged (signed), then step 2's commit on top, which leaves this step's tree.
- Step 3 (2026-11-22), open item 73, read only (gh, each call with your yes). Shard 1's attempt 1 of the green run 44924198765 and of the red runs 44937015046, 45001099474 and 45002825295: logs (`gh run view <run> --attempt 1 --log`; `gh run view --job <id> --log` and `gh api …/jobs/<id>/logs` give the newest attempt's log for an attempt 1 job, so they were not used) and the artifacts `screen-tests-shard-1-attempt-1`, kept in the scratchpad (i73), not in the repo.
  - The same in all four: runner image Version 20261116.202.1 (the third red run 20261116.88.1), provisioner 20261111.635, the emulator and system image from the cache key `emulator-37.3.10-android-37.0-google_apis-x86_64-8`. Since #219 (green) nothing outside the documents changed but versionCode and versionName (`git diff 26b93cd eb5c5a9`). So neither the app nor the runner's set-up explains the red runs.
  - In every red run's logcat of the failed test, the soft keyboard opened over MainActivity just before Create or Save (`VRI[MainActivity]: WindowInsets changed: ime:[0,0,0,1008]`, with missed frames in `IME_INSETS_SHOW_ANIMATION`), and then the app logged nothing until the 15 s ran out (no save, window focused). In the green run's two matching tests (LockAndLayerTest, PortfolioDeleteOverTheBarTest.theBottomBar_takesNoTap_whileTheDialogIsOpen) the keyboard started and finished its input but MainActivity got no keyboard insets: it did not open over the layer. The "database is locked" line and SQLCipher's CloseGuard warning appear in green and red alike.
  - Reading, not proven (one green run compared; no emulator on this PC to force the keyboard): when the keyboard opens, the layer slides up with it (imePadding), and the test's tap right after typing lands where Create or Save was, so the tap misses. Five places type and then tap at once: LockAndLayerTest (Create, Save), `createPortfolio` in PortfolioDeleteScreenTest, TransactionHistoryScreenTest:326, PortfolioAllocationScreenTest:97. Ways to fix, not chosen: (A) tap once the button and the keyboard have stopped moving, (B) close the keyboard before the tap, (C) tap again if the layer stays open.
  - Your decision (2026-11-22): no fix now; a red run of this kind is re-run, with your yes each time. Open item 73 stays open with this reading (section 14, item 4). No file under app/ changed.
  - The full run (documents only changed; daemons stopped first), every command with --rerun-tasks: spotlessCheck green (18 s); build green (1m 6s); tests green (1m): 109 result files, 1,512 tests, 0 failures, 0 errors, 1 skipped; lint green (1m 19s; report 18:40:08): the 7 findings of step 2; :app:compileDebugAndroidTestKotlin green (47 s), with the Kotlin daemon fallback message once more (open item 60's; the third time today, each on Gradle 9.9.2 or 9.6.0, no crash log).

## 14. Departures from the plan
1. Step 2 (decision 1, your B): Gradle 9.9.2 and Spotless 8.12.0 on top of the four Dependabot heads, which bring 9.9.1 and 8.11.0; sections 1 and 5 and row 39 name the versions the pull requests bring. KSP 2.3.14 and coroutines-test 1.11.0 as the heads bring them.
2. Step 2: gradle-wrapper.properties as Gradle 9.9.2's wrapper task writes it: `retries=0` and `retryBackOffMs=500` added, the timestamp comment gone.
3. The commits wait until the pull request is needed (your instruction, 2026-11-22); each step's tree is held in a scratchpad index meanwhile.
4. Step 3 (your decision, 2026-11-22): open item 73's cause read and recorded, but no fix to the wait (sections 1, 5 and 6 planned one); a red run of this kind is re-run, with your yes each time.
