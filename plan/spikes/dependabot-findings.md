# plan/spikes/dependabot-findings.md

Stage: Build. Status: written 2026-11-18 from main at d182dfb (the merge of #200), after your interview of 2026-11-18, on branch spike-dependabot-findings (from main at d182dfb); approved by you 2026-11-18 as written, before the look-ups (section 5). Look-ups L1 to L10 and decisions D1 to D5 done 2026-11-18 (D4: no phone; D5: KSP 2.3.14 in place of #125's 2.3.13). Baseline run 2026-11-18 (section 9.1); all bumps green locally 2026-11-18 (section 9.2); CI, screen tests and CodeQL green on draft pull request #201 (section 9.3); verdicts in section 9.4. Cleanup done 2026-11-18 (section 10): #201 closed unmerged, the throw-away branch deleted. This note's pull request not yet opened.
Source: your interview of 2026-11-18 (starting commit main d182dfb; scope every open Dependabot pull request; outcome a verdict only, the Dependabot pull requests untouched; order of work your B, all bumps at once and split on failure), the open Dependabot pull requests and alerts (I read with gh, 2026-11-18), gradle/libs.versions.toml and gradle/wrapper/gradle-wrapper.properties (I read, 2026-11-18), plan/open-items.md items 31, 43 and 60, CLAUDE.md section 6 (lint findings, the incomplete-APK pitfall), plan/spikes/dependency-upgrade.md and plan/spikes/avd-memory.md (layout and branch names).

How sources are labelled in this note:
- "I read": read by me in this session with gh or from the repo, with the date.
- "my reading": my interpretation. Not a fact until a look-up or the run shows it.
- "not checked": not looked at.

## Table of contents
- [1. Outcome](#1-outcome)
- [2. Gate](#2-gate)
- [3. Where things stand](#3-where-things-stand)
- [4. Change set](#4-change-set)
- [5. Questions to look up](#5-questions-to-look-up)
- [6. Verdict rules](#6-verdict-rules)
- [7. Decisions](#7-decisions)
- [8. The run](#8-the-run)
- [9. Results](#9-results)
- [10. Cleanup and follow-up](#10-cleanup-and-follow-up)

## 1. Outcome
A recorded verdict, pass or fail with the reason, for each of the 5 open Dependabot pull requests (section 3), tried together on a throw-away branch from main at d182dfb. It is done when:
1. Section 9 gives each pull request a verdict by section 6, with the real output it rests on.
2. A failure, if any, is traced to the bump or bumps that cause it (section 8, step 5).
3. Whatever could not be run or verified is listed as such.
Not part of the outcome (your answer of 2026-11-18): no Dependabot pull request is merged, closed, rebased, commented on or otherwise changed. They stay open until a proper iteration takes them up, and that iteration uses this note's verdicts.

## 2. Gate
This is a decision note, not an iteration. It changes no app code on main and no spec, and blocks no iteration.
Branches (D1): this note on spike-dependabot-findings (from main at d182dfb), one pull request, merged by you. The trial bumps only on throw-away-dependabot-findings, with a draft pull request that is closed unmerged (CLAUDE.md section 4).
Order: the note's approval, the look-ups (section 5), decisions D1 to D5, the run (section 8), results (section 9), cleanup (section 10). Look-ups L1 to L10 and decisions D1 to D5 done 2026-11-18 (sections 5 and 7).

## 3. Where things stand
- Open Dependabot pull requests (I read, 2026-11-18, `gh pr list --author app/dependabot`):

| PR | Bump | Opened | Head branch | Ships in the APK | Note |
|---|---|---|---|---|---|
| #165 | Gradle wrapper 9.6.0 to 9.9.1 | 2026-11-10 | dependabot/gradle/gradle-wrapper-9.9.0 | no (build tool) | Title says 9.9.1, branch name 9.9.0 (L1) |
| #164 | com.diffplug.spotless 8.10.4 to 8.11.0 | 2026-11-10 | dependabot/gradle/com.diffplug.spotless-8.11.0 | no (style check) | A listed lint finding (CLAUDE.md section 6) |
| #125 | com.google.devtools.ksp 2.3.12 to 2.3.13 | 2026-11-03 | dependabot/gradle/com.google.devtools.ksp-2.3.13 | no, but it writes Room's generated code, which ships | Open item 60 (intermittent KSP and testFixtures failure) |
| #124 | kotlinx-coroutines-test 1.9.0 to 1.11.0 | 2026-11-03 | dependabot/gradle/org.jetbrains.kotlinx-kotlinx-coroutines-test-1.11.0 | no (tests only) | A listed lint finding |
| #123 | net.zetetic:sqlcipher-android 4.19.1 to 4.20.0 | 2026-11-03 | dependabot/gradle/net.zetetic-sqlcipher-android-4.20.0 | yes (database encryption) | Open item 43: "upgrade in its own change, with its release notes looked up on the day" |

- Open Dependabot security alerts: none (I read, 2026-11-18, the repository's dependabot/alerts with state open).
- Today on main (I read, 2026-11-18): Gradle 9.6.0 (wrapper, with distributionSha256Sum), AGP 9.4.1, Kotlin 2.4.21, KSP 2.3.12, Room 3.0.3, Spotless 8.10.4, SQLCipher 4.19.1, coroutines 1.9.0. Lint: 6 findings (CLAUDE.md section 6).
- Lint findings with no Dependabot pull request, out of scope (section 10): AGP 9.5.1 (open item 31) and kotlinx-serialization-json 1.13.0.
- Other open pull requests, not in d182dfb (I read, 2026-11-18): #190 (iteration 32), #193, #194, #195 (iterations 33b, 34, 35, drafts) and #196 (spike figma-workflow, draft). The verdicts hold for main at d182dfb; when those merge, the iteration that takes up the bumps runs its own checks again.
- Not checked: the Dependabot pull requests' diffs (L1), which build is on the phone (L5), the five versions' release notes (L2 to L4).

## 4. Change set
All my reading until L1 shows the diffs.
| File | Change | From |
|---|---|---|
| gradle/wrapper/gradle-wrapper.properties | distributionUrl and distributionSha256Sum to 9.9.x | #165 |
| gradlew, gradlew.bat, gradle/wrapper/gradle-wrapper.jar | all three change (L1); gradlew.bat arrives as CRLF in git | #165 |
| gradle/libs.versions.toml | spotless, coroutines, sqlcipher lines | #164, #124, #123 |
| gradle/libs.versions.toml | ksp 2.3.12 to 2.3.14, by hand (D5) | departure, in place of #125 |
| Not changed | app and core source, tests, build files, workflows, CLAUDE.md, plan.md | |
A needed edit beyond the pull requests' own (for example a code change a new version asks for) is a departure: I show it to you first and record it in section 9.

## 5. Questions to look up
Each gh call is a shell command and asks first; each web search or fetch is shown to you first (tool, query or URL, reason) and asks first (CLAUDE.md section 8).
- L1 Each Dependabot pull request's diff and body (gh). What exactly changes, and for #165 whether gradlew, gradlew.bat or the jar change, and which Gradle version it really sets (title 9.9.1, branch 9.9.0). If gradlew changes, check that it stays LF and mode 100755 (CLAUDE.md section 6).
- L2 Gradle 9.9.x against AGP 9.4.1 and Kotlin 2.4.21: the Gradle release notes and AGP's compatibility table (web fetch).
- L3 KSP 2.3.13 against Kotlin 2.4.21 and Room 3.0.3: its release notes (web fetch).
- L4 SQLCipher 4.20.0 release notes (web fetch, open item 43): any change to the file format, the default settings (KDF iterations, page size, cipher) or the API the app calls, and whether a database written by 4.19.1 opens without a migration. Spotless 8.11.0 and coroutines-test 1.11.0 notes too, for breaking changes and the ktlint version Spotless uses.
- L5 Which build is on your phone now (your answer). If it is a build from #190 or #193 to #195 with a newer Room schema version than main's at d182dfb, the spike's build would be a downgrade, and Room would refuse the data (my reading). Then decision D4.
- L6 (added 2026-11-18 after L1) #165 replaces gradle/wrapper/gradle-wrapper.jar, and no workflow validates the wrapper jar (no "wrapper" step in .github/workflows, I read). Compare the jar's SHA-256 in #165 with the checksum Gradle publishes for 9.9.1's wrapper jar (web fetch). A mismatch fails #165 outright.

Answers (I read, 2026-11-18, with gh; head commits as named):
- L1, #165 (head c940f56): sets Gradle 9.9.1 (distributionUrl gradle-9.9.1-bin.zip, distributionSha256Sum d8a18a7a841dd5df825d35a685fad6f2d7f12ac0c280e57b67c27b4a520ba228). The branch name's "9.9.0" is from Dependabot's first version of it (my reading). It also changes the jar (binary), gradlew (+3 -6: the header comments, and the empty CLASSPATH and the -classpath option removed) and gradlew.bat (whole file: +112 -94; a new "safety net" jump, endlocal before java, no -classpath). gradlew keeps LF (0 CR, 248 LF) and mode 100755. gradlew.bat is committed with CRLF (112 CR, 112 LF), while .gitattributes (*.bat text eol=crlf) has git store it as LF (main: i/lf w/crlf): the whole-file diff is partly line endings. Merged as it is, the repo would hold a CRLF gradlew.bat against its own rule; the iteration should store it as LF (my reading: git add --renormalize, or the wrapper task run locally).
- L1, #164 (head b0fae1d), #125 (d9c1740), #124 (2083814), #123 (396ec05): one line each in gradle/libs.versions.toml, as their titles say (spotless 8.11.0, ksp 2.3.13, coroutines 1.11.0, sqlcipher 4.20.0). Nothing else changes.
- L1, bases: #164 on b6b2d2a, the others on be02246; both are ancestors of d182dfb, so merging a head branch brings only its own commit. GitHub showed mergeable "UNKNOWN" for all five (not yet worked out).
- L1, conflicts (my reading, not tried): ksp (line 14, #125) and sqlcipher (line 15, #123) are adjacent lines, and git reports changes to adjacent lines from two branches as a conflict. D2 (a) then needs one hand resolution, shown to you first. The other lines are at least one line apart.

Answers L2 to L4 and L6 (fetched 2026-11-18 with your yes, through the web-fetch agent; "fetched" below means its report of the page, quotes as it gave them; not read by me directly):
- L2 Gradle 9.9.1 (fetched: docs.gradle.org/9.9.1 release notes, compatibility page, upgrade guide in part). 9.9.1 is the first patch of 9.9.0 (released 2026-11-10) and fixes three security advisories: "Version catalog values run as code in generated accessors" (GHSA-3cfg-jpcr-gfp3, 9.3 Critical), "Gradle client trusts the content of the daemon registry" (GHSA-gqrr-4j89-5267, 8.6 High), "Daemon client trusts stale registry entries (port squatting)" (GHSA-pcp8-98xm-c6pr, 7.0 High). Compatibility: "tested with Android Gradle Plugin 9.0 through 9.5.0-alpha10", so AGP 9.4.1 is inside; JVM 17 to 27 runs Gradle (we use 25); embedded Kotlin 2.4.21, tested with Kotlin 2.0.0 to 2.5.0-Beta1. Expected on the run: a deprecation notice that KGP 2.0 to 2.4.x registers an attribute type (fixed only in KGP 2.5.0), and the wrapper task now keeps existing gradle-wrapper.properties values. Nothing Android-specific; the rest of the upgrade guide (about 40,000 characters) was not read.
- L3 KSP 2.3.13 (fetched: github.com/google/ksp/releases/tag/2.3.13; dated "30 Oct", year not shown). Ten changes: new context-parameter API, diagnostics, opt-in classloader caching, cache and incremental-compilation speed-ups, a backported CoreJarHandler fix "to prevent non-deterministic missing jar entries", KMP and project-isolation fixes. The page carries "Defect in release": "a regression described in issue 3286 which may cause issues for concurrent executions of KSP. We are working on a fix and will release a new version soon." No Kotlin, Room or AGP statement. My reading: this bears on open item 60 (KSP failing intermittently in our parallel full run), in either direction; the run must watch for it, and a red KSP step is not re-run away without reading it.
- L4 SQLCipher 4.20.0 (fetched: github.com/sqlcipher/sqlcipher-android/releases/tag/v4.20.0; 27 Oct 2026 by its announcement's URL). "This is a maintenance release": SQLite baseline 3.54.1; "Avoids a deferred error when PRAGMA cipher_migrate fails before migrating"; a WebAssembly finalizer change. Nothing on the file format, default settings, API, minSdk or packaging. The fuller post, https://www.zetetic.net/blog/2026/10/27/sqlcipher-4.20.0-release/, was not fetched (L7).
- L4 Spotless 8.11.0 (fetched: raw CHANGES.md of plugin-gradle; 2026-11-02). Only google-java-format changes (default 1.36.1 to 2.0.0, two new options). Nothing for ktlint, Kotlin steps, line endings or minimum Gradle and Java; the default ktlint is still 1.8.0, which we pin. My reading: no effect on our check beyond clearing its lint notice.
- L4 kotlinx-coroutines 1.11.0 (fetched: its release page; dated "08 May", year not shown). Built with Kotlin 2.2.20. For the test library only: "Advanced the deprecation levels on kotlinx-coroutines-test APIs (#4604)", with no API named; a test that uses one of them may now warn or fail to compile. Also deprecated: CoroutineDispatcher as a context key, and passing a Job to builders (lint). 1.10.x notes not read.
- L6 Wrapper jar (fetched: gradle.org/release-checksums; I computed the jar's SHA-256 from #165's blob 51cbd2a with gh). #165's jar: 49,154 bytes, 621008afc4f5d59357fcf06ffa644dd5aade5e091bdc363742710445f73cdaf8, equal to Gradle's published 9.9.1 Wrapper JAR checksum. #165's distributionSha256Sum equals the published 9.9.1 bin.zip checksum. Both match.
- L6, found on the way: main's jar (working copy at d182dfb) has SHA-256 76805e32c009c0cf0dd5d206bddc9fb22ea42e84db904b764f3047de095493f3, not the published 9.6.0 Wrapper JAR checksum (497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7). My reading, not checked: the jar was not regenerated when the wrapper went to 9.6.0 (only the properties changed), so it is some older Gradle's jar; whether it equals any published wrapper jar is not known (L8). #165 replaces it with a verified one.

- L5 (your answer, 2026-11-18): the phone has a debug build from #190, #193, #194 or #195, not main. I read with git, 2026-11-18: the Room schema is version 14 on d182dfb and on all four branches (iter-32 39657e3, iter-33b 1f610cf, iter-34 245350d, iter-35 672176f), and 14.json is the same blob (11a013a) on all five, so the database is not a downgrade. The app's versionCode is: d182dfb 41, iter-32 42, iter-33b 43, iter-34 44, iter-35 45. The spike's build (41) is therefore lower than the phone's, and adb install -r refuses a lower versionCode (my reading, not tried: INSTALL_FAILED_VERSION_DOWNGRADE). iter-34 and iter-35 also add a "check" build type with applicationId suffix .check: a separate app, not the one checked here. Then decision D4.

Answers L7 to L10 (fetched 2026-11-18 with your yes, through the web-fetch agent; L10 also by my search of the repo):
- L7 SQLCipher 4.20.0 announcement (fetched: zetetic.net blog post of 2026-10-27). Version 4.20.0 on SQLite 3.54.1; Community Edition on Android uses a provider "based on LibTomCrypt 1.18.2". Nothing on the file format, default settings (KDF, page size, cipher, HMAC), 4.19.x compatibility, Android API, minSdk or packaging; no security fix or CVE named. The core's own changelog (github.com/sqlcipher/sqlcipher/tree/v4.20.0) was not fetched. My reading: nothing points to a format change, but nothing rules one out; the phone check (deferred, D4) stays the proof.
- L8 main's wrapper jar (fetched: gradle.org/release-checksums, and wrapperChecksumUrl for 9.3.0 to 9.6.0). Its SHA-256 76805e32… is listed on Gradle's page as the Wrapper JAR checksum of 9.0.0 and of 9.1.0; not confirmed through those two versions' own wrapperChecksumUrl. Published wrapper jar checksums fetched: 9.6.0, 9.5.1, 9.5.0 497c8c2a…; 9.4.1, 9.4.0 55243ef5…; 9.3.1, 9.3.0 b3a875dd…. So main's jar is a genuine Gradle 9.0.0 or 9.1.0 wrapper jar, older than the 9.6.0 the properties name (my reading: not regenerated when the wrapper moved to 9.6.0). Not a defect in behaviour; recorded for the iteration that takes up #165.
- L9 KSP issue 3286 (fetched: the issue and google/ksp releases). "KSP 2.3.13 regression: library symbols disappear during concurrent in-process executions", P1 bug, opened 2026-10-30, closed. Symptoms: library annotations resolve as error types, generated code silently omitted, unresolved references, with KSP itself reporting nothing; reported with org.gradle.parallel=true, several kspKotlin tasks in one daemon, Kotlin 2.4.20/2.4.21, JDK 21 or 25. Workarounds: stay on 2.3.12, or run KSP one task at a time. Fixed in KSP 2.3.14 (released 13 Nov, "Latest"): "Fix regression where library symbols disappear during concurrent KSP executions ... (#3286)". #125 still asks for 2.3.13 (I read, 2026-11-18). I read (2026-11-18): only app applies KSP (Room's compiler, app/build.gradle.kts lines 4 and 60; core does not), and gradle.properties leaves org.gradle.parallel commented out. My reading: KSP tasks in this build rarely if ever run at the same time, so the exposure is smaller than in the reports, but a silently omitted Room class would be the worst kind of failure; decision D5.
- L10 kotlinx.coroutines #4604 (fetched: the pull request and its files). 1.11.0 moves runTest(context, dispatchTimeoutMs: Long, …) and TestScope.runTest(dispatchTimeoutMs: Long, …) from WARNING to ERROR, and removes runBlockingTest, runBlockingTestOnTestScope, runTestWithLegacyScope, TestCoroutineScope, TestCoroutineDispatcher and TestCoroutineExceptionHandler. I searched every .kt file in the repo (2026-11-18): none of these names and no dispatchTimeoutMs appear. My reading: #124 needs no test change; the build is the proof.

Further look-ups proposed (each asks first):
- L7 The SQLCipher 4.20.0 announcement (URL above), for the file format and default settings.
- L8 Whether main's jar equals any published wrapper jar (Gradle's full checksum list: https://services.gradle.org/versions/all and each version's wrapperChecksumUrl, or the rest of the release-checksums page).
- L9 KSP issue 3286 (https://github.com/google/ksp/issues/3286), and whether a later KSP release fixes it.
- L10 kotlinx.coroutines pull request #4604 (https://github.com/Kotlin/kotlinx.coroutines/pull/4604), for the test APIs whose deprecation level moved. Or skip it, and let the build show any use in our tests.

## 6. Verdict rules
A bump passes only when all of these hold with it in place:
- V1 Locally: spotlessCheck, build, tests and lint green with --rerun-tasks; the test total (added up from the result files) equal to the baseline's; lint findings the baseline's less the ones the bumps clear, with any new one listed, not silenced.
- V2 On the draft pull request: CI and the screen tests green, with every screen test run (the workflow's count check), and CodeQL as `gh pr checks` shows it.
- V3 For #123 and #125 (they change what ships): the phone check in section 8, step 4.
- V4 No departure (section 4) needed, or one you approved and that is recorded.
All five are tried together (your B). If everything passes, each passes. If something fails, section 8, step 5 finds the bump; a bump that fails on its own fails, and the others are tried again without it.

## 7. Decisions
To settle after the note's approval and the look-ups, one question at a time.
- D5 (added 2026-11-18 after L9) KSP: #125 asks for 2.3.13, which has a known, fixed regression (L9). Options: (a) try 2.3.13 as #125 brings it, and record the verdict with L9 whatever the run shows; (b) try 2.3.14 instead, a departure from "exactly what the pull requests bring", and record #125's verdict as "fail: superseded by 2.3.14 (L9)" with 2.3.14's result beside it; (c) leave KSP out of the trial, #125's verdict "fail by look-up (L9)".
D1 settled 2026-11-18, your A: spike-dependabot-findings for this note, throw-away-dependabot-findings for the trial, without NN (as plan/spikes/avd-memory.md section 7, M4).
D2 settled 2026-11-18, your A: the head branches of #165, #164, #124 and #123 are fetched and merged into throw-away-dependabot-findings only (each fetch and merge asks first); #125 is replaced by D5. You asked whether the Dependabot pull requests stay on main: they stay open against main and unchanged, because GitHub marks a pull request merged only when its head commit reaches its base branch (main), and the throw-away branch is never merged (my reading of GitHub's behaviour, not tried in this repository; section 9 records their state after the run).
D3 settled 2026-11-18, your A: a baseline on d182dfb before the bumps, as section 8 step 2 says.
D5 settled 2026-11-18, your B: the trial uses KSP 2.3.14, not 2.3.13. #125's verdict is "fail: superseded by 2.3.14 (L9)", with 2.3.14's run recorded beside it. The ksp line is set by hand on the throw-away branch (section 4), so #125's head branch is not merged; this also removes the adjacent-line conflict L1 foresaw with #123.
D4 settled 2026-11-18, your C: no phone in the spike. V3 is recorded as not run for #123 and #125, and the phone check (install over the previous build, data opens, a write works) is done by the iteration that takes up the bumps. Section 8 step 4 keeps the APK and dex size comparison and drops the install.
- D1 Branch names. My leaning: spike-dependabot-findings for this note and throw-away-dependabot-findings for the trial, without NN, as plan/spikes/avd-memory.md section 7 (M4) did for a spike.
- D2 How the bumps reach the throw-away branch. Options: (a) merge the 5 Dependabot head branches as they are, so the trial holds exactly what the pull requests would bring; (b) re-type the version lines by hand. My leaning: (a). Each merge asks first. A conflict between them (my reading: none, they touch different lines) is shown to you before it is resolved.
- D3 The baseline. My leaning: build, tests and lint on d182dfb before the bumps, with the APK's and each classesN.dex's size kept for the comparison in step 4 (CLAUDE.md section 6, the incomplete-APK pitfall). It measures only; it does not change your order of work.
- D4 The phone (rewritten 2026-11-18 after L5: same schema, lower versionCode). Options: (a) a backup in the app, then adb install -r -d (downgrade allowed for a debug build, data kept; my reading, not tried), the checks of section 8 step 4, then your iteration build installed back; (b) on the throw-away branch only, raise versionCode above 45 so a plain install -r works (a departure, one line in app/build.gradle.kts), then your iteration build needs -d to go back; (c) no phone in the spike: V3 recorded as not run, and the phone check done by the iteration that takes up the bumps.

## 8. The run
Each push by you; each run and re-run of a workflow with your yes; I read each with gh.
1. Create spike-dependabot-findings (this note) and, after its approval and the look-ups, throw-away-dependabot-findings, both from d182dfb.
2. Baseline on d182dfb (D3): spotlessCheck, build, tests and lint, forced fresh; test total, lint findings, the APK's and each classesN.dex's size.
3. All 5 bumps on the throw-away branch at once (your B; D2), then the same commands. A red local step is read for its cause first; an intermittent KSP or testFixtures failure (open item 60) is re-run unchanged once, and recorded either way.
4. You push and open a draft pull request; CI and the screen tests run (V2). The phone part below is not run (D4, your C); only the size and dex comparison is. As written before D4: compare the APK and dex sizes with the baseline and check a name from Room's generated code is in the dex; a backup in the app first; install over the phone's build with adb install -r; the app unlocks and every tab opens with its data, with no "Your data cannot be read" screen; one cash entry added and then reversed, to show writing works.
5. Only on a failure: undo one bump at a time (a revert of its merge, asking first), cheapest to rule out first (#124, #164, #165, #125, #123), and run the failing check again each time, until the failure goes or one bump alone shows it. A bump that fails is recorded with its output; the others are run again without it, through steps 3 and 4.
6. Results into section 9.

## 9. Results
Run on this PC by me; the output was shown in the chat as each command ran.

### 9.1 Baseline (section 8 step 2, D3), 2026-11-18
On spike-dependabot-findings at 549deb0, whose code is d182dfb's (the commit adds only this note). git status clean before the run. Every command with --rerun-tasks.
| Check | Result |
|---|---|
| spotlessCheck | BUILD SUCCESSFUL in 24s, 4 tasks executed |
| :core:assemble :app:assembleDebug | BUILD SUCCESSFUL in 1m 10s, 39 tasks executed |
| :core:test :app:testDebugUnitTest | BUILD SUCCESSFUL in 1m 17s, 32 tasks executed. Added up from the result files (39 in core, 59 in app): 1,395 tests, 0 failures, 0 errors, 1 skipped |
| :app:lintDebug | BUILD SUCCESSFUL in 1m 48s, 32 tasks executed; report written 18:14:24. 6 findings, as CLAUDE.md section 6 lists: AndroidGradlePluginVersion (libs.versions.toml:2, 9.5.1), NewerVersionAvailable for spotless (:12, 8.11.0), sqlcipher-android (:15, 4.20.0), kotlinx-coroutines-test (:17, 1.11.0), kotlinx-serialization-json (:18, 1.13.0), TypographyDashes (strings.xml:86) |
| Debug APK | 44,205,294 bytes (18:09:47); copy kept in the scratchpad (baseline\app-debug-baseline.apk), not in the repo |
| classesN.dex sizes | classes.dex 17,676,928; classes2 612,808; classes3 203,720; classes4 7,068; classes5 35,828; classes6 386,148; classes7 2,551,948; classes8 56,964; classes9 43,560; classes10 13,519,180 |
The test run printed "Problems report is available at build/reports/problems/problems-report.html"; its contents were not read.

### 9.2 All bumps at once (section 8 step 3; D2, D5), 2026-11-18
Branch throw-away-dependabot-findings from 549deb0. The four Dependabot heads were fetched (each head equal to the commit L1 read) and merged with --no-ff, all signed (G):
| Commit | What |
|---|---|
| 104ac5e | merge #165 (c940f56), Gradle wrapper 9.9.1: jar, properties, gradlew, gradlew.bat. The first try timed out at GPG signing and was finished with git commit after you were ready |
| cb182f5 | merge #164 (b0fae1d), Spotless 8.11.0 |
| 7dd8de3 | merge #124 (2083814), kotlinx-coroutines-test 1.11.0 |
| 177722b | merge #123 (396ec05), sqlcipher-android 4.20.0 |
| 962f345 | ksp 2.3.12 to 2.3.14 by hand in gradle/libs.versions.toml (D5); the file keeps CRLF (52 CR, 52 LF) |
No conflict. After the merges, `git ls-files --eol` shows gradlew i/lf w/lf (mode kept) and gradlew.bat i/crlf w/crlf against its attribute eol=crlf (stored LF on main): git then lists gradlew.bat as modified in the working tree, a diff of 112 lines in and out that is empty when CR is ignored. Left as #165 brought it, not staged.

The Gradle daemon was stopped first (`--stop`); `--version` then showed Gradle 9.9.1, Kotlin 2.4.21, launcher JVM 25.0.3 (JetBrains). Every command with --rerun-tasks:
| Check | Result | Against the baseline (9.1) |
|---|---|---|
| spotlessCheck | BUILD SUCCESSFUL in 39s, 4 tasks executed | same; printed a deprecation notice |
| build (with --warning-mode all) | BUILD SUCCESSFUL in 1m 14s, 39 tasks executed | same task count |
| tests (with --warning-mode all) | BUILD SUCCESSFUL in 57s, 32 tasks executed; 39 + 59 result files: 1,395 tests, 0 failures, 0 errors, 1 skipped | equal |
| lint | BUILD SUCCESSFUL in 1m 49s, 32 tasks executed; report written 19:02:56. 3 findings: AndroidGradlePluginVersion (:2, 9.5.1), NewerVersionAvailable kotlinx-serialization-json (:18, 1.13.0), TypographyDashes (strings.xml:86) | the 3 notices for spotless, sqlcipher-android and coroutines-test are gone; no new finding; KSP had none |
| Debug APK | 44,239,182 bytes | +33,888 bytes. My reading: SQLCipher's native libraries (SQLite 3.54.1); not checked file by file |
| classesN.dex | classes.dex 17,676,968 (+40); classes2 to classes10 equal to the byte | |
| Names in the dex | AtsDatabase_Impl (Room's generated database, from KSP) in classes6.dex; net/zetetic/database/sqlcipher/SQLiteDatabase in classes.dex and classes6.dex | classes6.dex, which holds Room's generated code, is the same size as with KSP 2.3.12 |
- Deprecation: the only warning shown with --warning-mode all, in the build and in the tests: "The Configuration.setVisible(boolean) method has been deprecated. This is scheduled to be removed in Gradle 11." Our build scripts do not call it (no isVisible or setVisible in any .gradle.kts), so a plugin does (not checked which). Whether Gradle 9.6.0 showed it too was not checked: the baseline ran without --warning-mode all. The KGP attribute deprecation L2 expected did not appear.
- Open item 60 did not show: no KSP or testFixtures failure in these runs (one run each).

### 9.3 CI and screen tests (section 8 step 4), 2026-11-18
throw-away-dependabot-findings pushed once by me at your request (962f345), and draft pull request #201 created by me at your request ("Create this draft PR once, add a reviewer"): base main, reviewer msfsgh (as for #197). Runs started: build (CI) 44291538447, screen-tests 44291538782, Analyze (actions) 44291532685.
Results (I read with gh, 2026-11-18; one run each, on 962f345):
| Check | Result |
|---|---|
| CI (build), run 44291538447 | success, 4m 3s. "Welcome to Gradle 9.9.1!". Style check 39 s, Build 1m 23s, Unit tests 44 s, Lint 1m 5s, each BUILD SUCCESSFUL. "Scale test retries": "Checks measured again (24a decision 1), every try: none". Each Gradle step printed "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10" (my reading: the setVisible deprecation seen locally in 9.2; not read with --warning-mode all on CI) |
| Screen tests, run 44291538782 | success, 15m 21s. "Tests run: 136 of 136". Emulator and image restored from the cache. Build the app and the tests 148 s, Wait for the boot 42 s, Let the system settle 11 s, Screen tests 646 s |
| Analyze (actions) 44291532685 and CodeQL | pass (38 s and 5 s) |
Against main's last 10 runs (plan/spikes/avd-memory.md section 3): the screen-tests step (646 s) is inside their 518 to 676 s; the whole job (921 s) is 19 s above their longest (902 s), from the build step (148 s against 70 to 111 s). My reading, not checked: Gradle 9.9.1 and the new versions were not in the runner's Gradle cache yet. One run cannot show a change in speed.

### 9.4 Verdicts (section 6), 2026-11-18
| PR | Verdict | V1 local | V2 CI, screen tests, CodeQL | V3 phone | Notes for the iteration that takes it up |
|---|---|---|---|---|---|
| #165 Gradle wrapper 9.9.1 | pass | green | green | not needed (build tool) | Jar and distribution checksums equal Gradle's published 9.9.1 values (L6); 9.9.1 fixes three security advisories (L2). gradlew.bat arrives stored as CRLF against .gitattributes: store it as LF (L1, 9.2). One new deprecation from a plugin, Configuration.setVisible, removed in Gradle 11 (9.2) |
| #164 Spotless 8.11.0 | pass | green | green | not needed | Clears its lint notice; only google-java-format changed (L4) |
| #124 kotlinx-coroutines-test 1.11.0 | pass | green | green | not needed (tests only) | Clears its lint notice; no use of the APIs 1.11.0 made errors or removed (L10) |
| #123 sqlcipher-android 4.20.0 | pass, phone not run | green | green | not run (D4) | Clears its lint notice (open item 43). APK +33,888 bytes. Format and defaults not stated by its release notes (L4, L7): the phone check (install over the previous build, data opens, a write works) is still owed |
| #125 KSP 2.3.13 | fail: superseded by 2.3.14 (L9) | not tried (D5) | not tried | not run | 2.3.13 has the concurrent-execution regression google/ksp#3286, fixed in 2.3.14. KSP 2.3.14 in its place: green locally and on CI and the screen tests; Room's generated code the same size (9.2); phone not run (D4) |
No departure beyond D5 was needed (V4). Lint after all bumps: 3 findings, against 6 (9.2).
The Dependabot pull requests after the run (I read with gh, 2026-11-18): #165, #164, #125, #124 and #123 all OPEN, not draft, each head the same commit L1 read (c940f56, b0fae1d, d9c1740, 2083814, 396ec05). D2's reading held: merging their commits into the throw-away branch and pushing it did not change them.

## 10. Cleanup and follow-up
- Done 2026-11-18 with your yes: #201 closed unmerged at 11:47:31 UTC, with a comment pointing to this note; throw-away-dependabot-findings deleted locally and on GitHub (`gh pr close 201 --delete-branch`; `git branch --list` and `git ls-remote --heads` then found none). Its commits are named in section 9.2; the runs' logs stay on GitHub.
- On the way back: `git checkout -- gradlew.bat` did not clear the line-ending difference (git kept listing it as modified, and refused to switch branches). This note was copied to the scratchpad, the branch switched with `git checkout -f spike-dependabot-findings`, and the note copied back; its SHA-256 was the same before and after (7588D408…). The iteration that takes up #165 meets the same thing until gradlew.bat is stored as LF.
- The 5 Dependabot pull requests stay open and unchanged (section 1). The iteration that takes them up reads section 9.
- Out of scope, for that iteration or a later note: AGP 9.5.1 (open item 31) and kotlinx-serialization-json 1.13.0, which have lint findings but no Dependabot pull request.
- plan/plan.md section 2 does not list spikes yet while #190 and #193 to #196 are open (plan/spikes/avd-memory.md section 10). This note is added with the others then.
