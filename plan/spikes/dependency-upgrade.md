# plan/spikes/dependency-upgrade.md

Stage: Build. Status: approved 2026-10-21 (note and leanings); decisions D1 to D7 settled 2026-10-21 (section 5). Run on branch spike-dependency-upgrade 2026-10-21 to 2026-10-22; results in section 9. Merged as 7df2049 (merge commit of pull request 73).
Source: plan/plan.md section 7 (the spike's question list), plan/setup.md section 13 (departures 4, 5, 6 and 8),
plan/iterations/01-unlock-app.md sections 7, 8 and 10.5, spec/spec.md v4 (final).
The look-ups were done on 2026-10-21. Versions here are facts of that day. They are checked again on the day the spike runs.

How sources are labelled in this note:
- "you read": text you copied from an official page and pasted into the chat on 2026-10-21. I did not see the page.
- "I fetched": page text returned by my own search or fetch in this chat.
- "your file": a repo file you uploaded. Its SHA-256 matched the hash you pasted.
- "site copy": an official Android page seen only in other-language copies. The current English copy was not seen.
- "not checked": my own knowledge or reading. It is not a fact until the branch run shows it.
Third-party pages (pull-request pages, blogs) were not used as facts.

## 1. Outcome
The 8 library-version lint notices are cleared by moving to current stable versions. Build, tests and lint are green on them,
a style checker is chosen or its deferral is recorded, and iteration 01's phone checks still pass. Iteration 02 can then start.

## 2. Gate
This is a decision note, not an iteration. Iteration 02 cannot start until: the decisions in section 5 are approved, the branch
run is done, and its results are written into section 9. Dependabot's branch for Compose BOM 2026.09.00 is not merged before then.

## 3. Findings

### 3.1 Starting point (your files, unless marked)
| Item | Now |
|---|---|
| AGP | 9.4.1 |
| Gradle | 9.6.0 (from the hand-off; the wrapper file was not seen) |
| Kotlin (both Kotlin plugins read one catalog line) | 2.2.10 |
| Compose BOM | 2026.02.01 |
| core-ktx / activity-compose / lifecycle-runtime-ktx | 1.10.1 / 1.8.0 / 2.6.1 |
| androidx.test ext junit / espresso-core | 1.1.5 / 3.5.1 |
| biometric / junit | 1.1.0 / 4.13.2 |
| compileSdk / targetSdk / minSdk | 37 / 37 / 33 |
| Java | 25 toolchain request (CLAUDE.md; gradle-daemon-jvm.properties not seen). core/ and app/ compile at Java 11 |
| CI | ubuntu-24.04, Temurin 25 by setup-java, ./gradlew, no Android SDK step |
| gradle.properties | configuration cache on, daemon -Xmx2048m, no android.* settings, no builtInKotlin setting |
| Lint | 8 notices, which are 7 version lines (the two Kotlin plugins share one) |

### 3.2 Answers to the questions in plan.md section 7
| # | Question | Finding | Source |
|---|---|---|---|
| 1 | Newest stable Kotlin | The 2.4 line was released June 3, 2026 and is supported to December 3, 2027. Latest 2.4.21 (Oct 8, 2026), then 2.4.20 (Sep 7) and 2.4.10 (Jul 14). | you read |
| 2 | Kotlin against Gradle | 2.4.20 to 2.4.21 are fully compatible with Gradle 7.6.3 to 9.7.0. 2.4.0 to 2.4.10 only to 9.5.0, so they are out with Gradle 9.6.0. Kotlin says newer Gradle may be used, with possible deprecation warnings. | you read |
| 3 | Kotlin against AGP | The documented AGP range for 2.4.20 to 2.4.21 is 8.5.2 to 9.3.1. Our AGP 9.4.1 is above it. Our Kotlin 2.2.10 documents Gradle to 8.14 and AGP to 8.10.0, and we already run 9.6.0 and 9.4.1 on it with a passing build and phone checks. My reading: the maximum is the newest version tested, not a limit. | you read; reading not checked |
| 4 | Kotlin 2.3 as a lower option | Its rows stop at Gradle 9.3.0 and AGP 9.0.0, farther from ours than 2.4.21. Not a candidate. | you read |
| 5 | AGP and Gradle | AGP 9.4.0 page: minimum and default Gradle 9.6.0, JDK 17, Build Tools 36.0.0, maximum API 37. No notes for AGP 9.4.1 were found. Gradle 9.7.0 page: Java 25 supported for running Gradle since 9.1.0, and "tested with AGP 9.0 through 9.4.0-alpha03", so AGP 9.4.x is not named. Gradle 9.7.0 was released Aug 6 (year not shown on the page). A 9.7.1 patch exists (seen only in third-party pages). The runner image carries Gradle 9.8.1. Conclusion: Gradle stays on 9.6.0. | I fetched (AGP page, Gradle release page); you read (Gradle compatibility page, image notes) |
| 6 | Built-in Kotlin | AGP 9.0 and later turn it on by default. The migration page does not state which Kotlin version it uses. In our files, app has only the kotlin.compose plugin, and the root file declares kotlin.compose and kotlin.jvm. My reading: one catalog line sets Kotlin for every module. | I fetched; your files; reading not checked |
| 7 | R8 | Kotlin 2.4 needs AGP 8.5.2 or newer and R8 9.1.29. The R8 version bundled in AGP 9.4.1 was not found. The release build has never been run, and R8 optimization is on for it. | site copy (fr, it, id, pt-br); your file |
| 8 | Compose BOM | 2026.09.00 is the latest in the page's dropdown. It maps Compose UI and Foundation 1.12.1, Material 3 1.4.0 and Material 3 Adaptive 1.3.0. The minimum compileSdk and AGP for these were not looked up. | you read |
| 9 | AndroidX libraries | Stable: core 1.19.1 (Oct 7), activity 1.13.0 (Sep 23), lifecycle 2.11.0 (Sep 23), ext.junit 1.3.0 and espresso-core 3.7.0 (no dates shown). All five match lint's suggestions. biometric stays on 1.1.0, the current stable. The "(*)" mark next to core and lifecycle on that page is unexplained. | you read; your lint output |
| 10 | Java 11 target of core/ and app/ | Kotlin 2.4.0 can generate Java 26 bytecode, so the original reason for core/'s pin (Kotlin 2.2.10 stops at 24) goes away. A second reason appears: app is also at Java 11 and will use core/ from iteration 02. Both stay at 11 for now. | you read; second reason not checked |
| 11 | Style checker | ktlint: stable 1.8.0 (Nov 15, 2025), no Kotlin 2.4 statement, 2.4 appears only in 2.0.0-alpha-4. Spotless Gradle plugin 8.10.4 (2026-10-08): default ktlint version, minimum Gradle and Kotlin 2.4 support not shown. detekt: stable 1.23.8 (Feb 21, 2025), built against Kotlin 2.0.21, and 2.0 is alpha. No stable checker states Kotlin 2.4 support. | you read |
| 12 | CI runner | The ubuntu-26.04 image lists Java 25.0.4 (default, JAVA_HOME_25_X64), platform android-37.0 (rev 2), build-tools 36.0.0 and 37.0.0, and Gradle 9.8.1. The image snapshot date was not shown. ci.yml installs Temurin 25 itself, runs the wrapper, and relies on the image's Android SDK. ubuntu-latest moves to 26.04 between Oct 19 and Nov 19, 2026 (earlier look-up, not re-checked). | you read; your file |
| 13 | Dependabot grouping | The current file has the gradle ecosystem, "/", weekly, and no groups. GitHub's docs support groups with patterns, update-types, dependency-type and exclude-patterns (earlier look-up). How catalog plugins and libraries are named for patterns was not checked. | your file; earlier look-up |

### 3.3 Not looked up, or not seen
- AGP 9.4.1 release notes, and the R8 version it bundles.
- Minimum compileSdk and AGP for Compose 1.12.1 and the new AndroidX versions.
- Gradle 9.7.1 and 9.8.x compatibility pages.
- Which default ktlint version Spotless uses, and whether ktfmt 0.65 supports Kotlin 2.4.
- Whether Dependabot has a minimum release age option (I think so; not checked).
- The end-of-support date of the Kotlin 2.2 line.
- gradle/wrapper/gradle-wrapper.properties and gradle/gradle-daemon-jvm.properties were not seen.
- Whether app/src/androidTest still holds any file (setup removed the template test).

## 4. Proposed change set and order of work (decisions of section 5 settled)
Files:
| File | Change |
|---|---|
| gradle/libs.versions.toml | 7 [versions] entries: kotlin 2.2.10 to D1; composeBom 2026.02.01 to 2026.09.00; coreKtx 1.10.1 to 1.19.1; activityCompose 1.8.0 to 1.13.0; lifecycleRuntimeKtx 2.6.1 to 2.11.0; junitVersion 1.1.5 to 1.3.0; espressoCore 3.5.1 to 3.7.0 (the last two per D3) |
| .github/workflows/ci.yml | runs-on ubuntu-24.04 to ubuntu-26.04 (D7), in its own commit |
| .github/dependabot.yml | groups per D5, in the last commit |
| style checker files | per D4, only if one is adopted |
| CLAUDE.md, plan/plan.md, this note | updated in the same pull request (stale lines on the 8 notices, the style checker, the Java 11 reason, the runner and the spike) |
| Not changed | build.gradle.kts (root), app/build.gradle.kts, core/build.gradle.kts, gradle.properties, the Gradle wrapper, all source code |

Order of work, on branch spike-dependency-upgrade from the updated main:
1. Create the branch. Re-check the latest stable Kotlin, BOM and the five AndroidX versions. If a newer stable exists, stop and ask.
2. Baseline on main as it is: build, tests, lint. Record the counts (expected 13 tests, 8 notices).
3. Commit A: the Kotlin line only. Build, tests, lint, the dependency checks and the release build (section 8).
4. Commit B: the BOM and the five AndroidX lines. The same checks.
5. Commit C: the runner. Push, and read the CI result.
6. Commit D: the style checker trial (D4).
7. Commit E: Dependabot grouping (D5).
8. Phone: install the debug build and re-run checks 1 to 14 of iteration 01.
9. Write section 9, update CLAUDE.md and plan/plan.md, and ask you to review.
If a step fails, I revert that commit and record it in section 9. A needed edit to a build file is a departure: I show it to you first.
Nothing is pushed unless you say so.

## 5. Decisions (all [Build]; settled by you on 2026-10-21)
Settled: D1 2.4.21, with 2.4.20 as the fall-back if the branch build fails on 2.4.21. D2 no rule now, ages recorded in this note, revisited at Deploy.
D3 upgrade both test libraries. D4 option (a): try ktlint 1.8.0, defer if it does not run cleanly. D5 option (a): one group for the Kotlin plugins only.
D6 keep Java 11 on both modules, revisit the level for both together later. D7 try ubuntu-26.04 in its own commit, kept only if the run is green.
The options below are kept as they were put to you.
- D1 Kotlin version. Options: 2.4.21 (latest, 13 days old) or 2.4.20 (44 days old). Both sit in the same documented row. Leaning: 2.4.21, and fall back to 2.4.20 if the branch build fails on it.
- D2 Minimum release age for new versions. Options: no rule, or at least 14 days. Under a 14-day rule, Kotlin 2.4.21 (13 days) fails and core-ktx 1.19.1 (14 days) just passes. Leaning: no rule now, record ages in this note, revisit at Deploy.
- D3 The two test-only libraries (androidx.test ext junit, espresso-core). Options: upgrade to 1.3.0 and 3.7.0, or remove them from the catalog and app/build.gradle.kts. Leaning: upgrade, because removing is a design choice about future instrumented tests.
- D4 Style checker. Options: (a) try ktlint 1.8.0 on the branch and defer if it does not run cleanly; (b) defer now; (c) a pre-release (ktlint 2.0 alpha or detekt 2.0 alpha), which breaks the stable-only rule used so far. Leaning: (a). How it is wired (Spotless plugin or another route) is decided on the branch and recorded in section 9. The zero-warnings rule of setup.md step 7 applies.
- D5 Dependabot grouping. Options: (a) one group for the Kotlin plugins only; (b) groups by family (Kotlin, AndroidX, AGP); (c) none. Leaning: (a). It changes future pull requests only, so it is the last commit.
- D6 Java 11 on core/ and app/. Options: keep both at 11, or try removing the pin. Leaning: keep both, and revisit the level for both together later.
- D7 CI runner. Options: move to ubuntu-26.04 and keep it only if the run is green, or stay on ubuntu-24.04 for now. Leaning: try it, in its own commit. (This is a seventh item. I had folded it into the proposals before.)

## 6. Out of scope
- Any change of AGP or Gradle (AGP 9.5 preview, Gradle 9.7.x and 9.8.x).
- Room, SQLCipher and everything else in iteration 02. Note for iteration 02: Kotlin 2.4.0 turned annotations in metadata on by default.
- Removing the Java 11 pin (D6), release signing, the install route, the price-source spike, and Figma.

## 7. Risks
- AGP 9.4.1 is above Kotlin's documented AGP range (9.3.1). The evidence that this is not a hard limit is our own project on Kotlin 2.2.10, on one machine.
- The bundled R8 version is unknown, and the release build has never been run with R8 optimization on.
- The configuration cache is on. Its behaviour with the new versions is not known.
- The daemon memory is 2 GB. Kotlin 2.4 or R8 may need more. Watch for out-of-memory errors.
- The JvmTarget setting in core/build.gradle.kts may be changed or deprecated in Kotlin 2.4 (not checked).
- The style checker is untested on Kotlin 2.4.21 and on AGP 9's built-in Kotlin. ktlint reads source files, so the Gradle runner is the larger unknown.
- ubuntu-26.04 is untested for our workflow, and the image snapshot date is unknown.
- Dependabot pattern names for catalog entries are unverified until it runs once.
- Versions can change before the run day. Step 1 of section 4 re-checks them.
- The phone checks are on one phone (Android 17). Compose and activity upgrades may change the lock screen or the cover screen.
- The VS Code Kotlin extension may again start the Gradle daemon on its own Java. Fix: .\gradlew.bat --stop, then rebuild.

## 8. Proof
Commands (Windows PowerShell, repo root), real output shown after each commit A and B:
- .\gradlew.bat :core:assemble :app:assembleDebug
- .\gradlew.bat :core:test :app:testDebugUnitTest, then the test count command from CLAUDE.md. Expected: 13 tests (1 in core, 12 in app).
- .\gradlew.bat :app:lintDebug, then the lint report read with ConvertFrom-Json. Expected: no library-version notice left. Any new notice is listed and not silenced.
- .\gradlew.bat :app:buildEnvironment and :core:buildEnvironment, to show which Kotlin version each module uses (task use to be confirmed when run).
- The same build command run twice, to see whether the configuration cache is reused.
- .\gradlew.bat :app:assembleRelease, with no signing key. Expected: an unsigned APK, so the R8 step runs. If it asks for a signing configuration, stop and record it.
- CI green for each pushed commit (your report; I do not see GitHub).
- git status shows no key, properties or build output tracked.

By hand on the phone (Android 17), each result written into section 9 as "checked on the phone" or "not checked":
- Checks 1 to 14 of plan/iterations/01-unlock-app.md section 8, on the debug build after commit B (and again after the last commit that changes a dependency). Check 8 needs the phone's screen lock removed for a moment, as before.
- Check 9 is the install over the previous build.
Anything that could not be run is stated as not run.

## 9. Results (run 2026-10-21 to 2026-10-22 on branch spike-dependency-upgrade)
Every Gradle, Git, CI and phone result here is your report of pasted output or of a check you did. I do not see your machine, GitHub or the phone. Where something was not run or not verified, it says so.

### 9.1 Commits
| Commit | What | Pushed, CI |
|---|---|---|
| 7a9ef90 | this note, plan.md status row | yes |
| b3b5488 | decisions D1 to D7 recorded | yes |
| 00f448a | A: Kotlin 2.2.10 to 2.4.21 (D1) | yes |
| 6654f2e | B: Compose BOM 2026.09.00 and the five AndroidX lines (D3) | yes |
| 7d08e78 | C: CI runner ubuntu-24.04 to ubuntu-26.04 (D7) | yes, CI green (9.3) |
| 9ab2e24 | D1: Spotless 8.10.4 with ktlint 1.8.0 wired in (D4). spotlessCheck fails until D2 | yes, with d66cf27 |
| ad522bd | D2: formatting only, 9 files, 42 insertions, 37 deletions, plus the Composable naming config | yes, with d66cf27 |
| d66cf27 | spotlessCheck step added to ci.yml before Build | yes, CI green (9.3) |
| 1474fe2 | E: Dependabot group "kotlin" (D5) | yes, in pull request 73 |
| 6038a10 | this note, sections 9 and 10 | yes, in pull request 73 |
| a382a6c | CLAUDE.md and plan.md updates | yes, in pull request 73 |
Pull request 73 was merged by you as merge commit 7df2049 (a real merge commit, not a squash). Your local main went from 6d3d7ea to 7df2049 by fast-forward. Both are your reports.

### 9.2 Build, tests, lint (local)
| | Baseline (Kotlin 2.2.10) | Commit A | Commit B |
|---|---|---|---|
| Debug build | OK, 17s | OK, 1m 2s | OK, 57s, 37 of 37 tasks |
| Tests | 13 | 13 | 13 (1 core, 12 app), none failing |
| Lint | 8 notices | 6 notices | 0 notices, confirmed by a forced --rerun-tasks run (27 tasks executed) |
| Configuration cache | reused | reused | reused on repeat runs |
| Release build (no signing key) | not run | OK, 33s | OK, 39s, R8 9.4.24, min_api 33; unsigned APK 2,283,170 bytes; mapping.txt 20,837,956 bytes |
I do not know which notices commit A still had, so the note says only "6 notices after the Kotlin bump" and "0 after the six bumps".
After D2 (formatting): forced spotlessCheck green (4 tasks executed); build green, 31s, 37 tasks; tests forced, 27 tasks executed, 13 tests with fresh timestamps, none failing; lint 0 findings.
The strip message "Unable to strip ... libandroidx.graphics.path.so" appears in every build (baseline, A, B, D2, CI). It is not silenced.

### 9.3 CI
- Commit C (at 7d08e78): runner ubuntu-26.04 (Ubuntu 26.04.1 LTS, image 20261012.163.1), Temurin 25.0.4. Build green 1m 52s (37 tasks), Unit tests green 5s, Lint green 20s, CI / build successful in 2m. Two CodeQL checks passed ("No new alerts"). Kept (D7).
- d66cf27 (with the Style check step): CI / build successful in 2m, both CodeQL checks passed. The Style check log was seen: ./gradlew spotlessCheck on Temurin 25.0.4, fresh daemon, no cached configuration, the four Spotless tasks and spotlessCheck executed, BUILD SUCCESSFUL in 31s. No line-ending finding appeared.
- The CodeQL checks are not configured in ci.yml. Where they are configured was not found.
- One run each. The Style check ran once on CI.
- Merge commit 7df2049 on main (your report): CI / build (push) 2m, CodeQL / Analyze (actions) 44s, Dependabot config validation 1s. The pull request also had a "Code scanning results / CodeQL" check that is not in the list for main. My reading is that it appears only on pull requests. Not checked. I did not see any log of these runs.

### 9.4 Style checker (D4)
- Adopted: Spotless Gradle plugin 8.10.4 with ktlint 1.8.0 (the version is pinned in the catalog and in the root build file). The plugin and ktlint resolved with the existing repository settings: settings.gradle.kts was not changed. No .editorconfig exists in the repo.
- Targets are set by hand, because Android sources are not detected automatically: core/src and app/src (*.kt) for the kotlin step, and the three .gradle.kts locations for the kotlinGradle step.
- Spotless, ktlint 1.8.0 and Kotlin 2.4.21 parsed all sources together locally and on CI. Neither tool's release notes state Kotlin 2.4 support. What is known is that they ran cleanly.
- ktlint reported rule standard:function-naming for the PascalCase @Composable functions (MainActivity.kt and Theme.kt). Settled with ktlint_function_naming_ignore_when_annotated_with set to Composable in the kotlin step of the root build file. The property name came from memory. It is supported only by behaviour: the two errors disappeared. I have not read ktlint's documentation for it.
- D2 changed formatting only: trailing commas, final newlines, small re-wraps; no logic change (I read every diff). Theme.kt also got one blank line before an else branch and a one-space shift of a comment closer.
- The style check is part of CI from d66cf27. CLAUDE.md gets the command in the same pull request.

### 9.5 Dependabot (D5)
- Commit E adds one group, kotlin, with the pattern org.jetbrains.kotlin.* under the gradle update. GitHub's groups page (you read) says patterns match dependency names, * is a wildcard, a dependency outside every group keeps its own pull request, and dependency-type is not listed for Gradle.
- Not verified: how Dependabot names the two Kotlin plugins, so whether the pattern matches them. Whether the group changes anything, since both plugins read one version variable. Both are decided by the first Dependabot run after the push. The Compose BOM is outside the group.
- After the merge (your report): the Dependabot config validation check passed on 7df2049, so GitHub accepts the file's syntax. No Dependabot activity had been seen since the push; the schedule is weekly. The pattern match and the effect of the group are still unverified. What happened to Dependabot's old Compose BOM 2026.09.00 branch (closed, deleted or still open) was not reported.
- The pattern has a dot, so it would not match org.jetbrains.kotlinx libraries or a library written org.jetbrains.kotlin:kotlin-stdlib. The catalog has none today.

### 9.6 Phone
- Debug build installed over the previous one with adb install -r: Success. The app started. Phone 52041FDAS000Z0 (Android 17). The model and build were not recorded.
- The build command that preceded the install found all 37 tasks up to date, so the installed APK is the one built after D2, as far as I can tell. I did not see its timestamp.
- Checks 1 to 14 of plan/iterations/01-unlock-app.md section 8, all YES (your report, 2026-10-22): prompt before content, cancel leaves the Unlock screen, unlock shows the greeting, Home and return prompts again (and one prompt only when Home is pressed during the prompt), screenshot blocked, recording black, recents hidden both ways, no-screen-lock message with no unlock (check 8, lock set up again afterwards), install over the old build, largest font in light and dark, rotation gives one prompt, dark-mode switch re-locks and prompts, recents then return is locked, cover screen with no flicker.
- Check 9 shows that the install over works. Data survival is still untestable, because the app stores nothing yet (iteration 02).

### 9.7 Answers settled by the run
- Kotlin 2.4.21 builds with AGP 9.4.1 and Gradle 9.6.0 on one machine and on CI. No fall-back to 2.4.20 was needed. AGP above Kotlin's documented range did not break the build.
- Gradle stays on 9.6.0.
- core/ and app/ still compile at Java 11 on Kotlin 2.4.21 (D6 holds).
- R8 9.4.24 is the version the release build used.
- Not answered: the minimum compileSdk and AGP for the new Compose and AndroidX versions were never looked up. The build passes with compileSdk 37.
- D2: no minimum-release-age rule. Kotlin 2.4.21 was 13 days old when decided. Revisited at Deploy.

### 9.8 Departures from section 4
- The style checker was committed in two steps (D1 wiring, D2 formatting) and its CI step in a third, d66cf27, which section 4 did not list. You approved the CI step on 2026-10-22.
- Section 4 said the root build.gradle.kts, app/build.gradle.kts and core/build.gradle.kts would not change. The root build file changed (Spotless plugin, targets, naming config). The other two got a final newline from the formatter.
- ci.yml was changed twice (the runner, then the Style check step).

### 9.9 Not run or not verified
Not run:
- the release APK on the phone, and R8's effect on the lock code;
- other phones and Android versions, and the cover screen in split-screen, with incoming-call banners, at the largest font, in dark theme or with TalkBack (as in iteration 01 section 10.4);
- a tests-from-scratch run of build and lint after D2 (only spotlessCheck and the tests were forced);
- Dependabot after commit E (merged in 7df2049; no run seen yet).
Not verified:
- the Gradle version (the wrapper file was never seen; 9.6.0 comes from the hand-off);
- Spotless 8.10.4 and ktlint 1.8.0 release ages, and ktlint's Kotlin 2.4 support;
- the naming property name beyond its effect;
- Dependabot pattern names;
- where CodeQL is configured;
- the phone model.

## 10. Status and notes
Approved 2026-10-21 (note and leanings). Decisions D1 to D7 settled 2026-10-21. Run on branch spike-dependency-upgrade (commits in 9.1), pull request 73, merged as 7df2049. Sections 4 to 8 are the plan as approved; section 9 records what was done and what differed.
The spike is finished: the CLAUDE.md and plan.md updates were in the pull request, and you merged it. Iteration 02 no longer waits on it.
Left open: the first Dependabot run (to see whether the kotlin group forms and whether its pattern matches the two Kotlin plugins), and the fate of Dependabot's old Compose BOM 2026.09.00 branch (not reported). That branch needs no merge: the same version is already on main (commit B).
