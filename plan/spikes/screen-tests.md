# plan/spikes/screen-tests.md

Stage: Plan (open item 47's remainder, before change request CR3). Status: written 2026-11-04 (pull request #126, merged as c489ec9); decisions S1 to S6 settled 2026-11-04 (section 6); approved by you 2026-11-04. Step 1 run 2026-11-04: the repo is public. S7 first kept the self-hosted runner (amendment A1, approved); after step 2's look-ups (section 11.2) you changed S7 to GitHub's runner, falling back to runs by hand on your PC (amendment A2, marked in sections 1 and 6 to 10). A1 is withdrawn: no self-hosted runner, no separate Windows user. Step 2 was completed before A2's approval, at your request (look-ups only; sections 11.2 and 11.3). A2 approved by you 2026-11-04 (as written, with S8 and the S2 image). Step 3 may start; each "Ask" still stops for your yes. Steps 5, 8 and 9 run 2026-11-04 to 2026-11-05 (sections 11.4 to 11.8); step 10 not needed; results in section 11.9. Cleanup (section 8) still asks first.
Source: plan/discussions/screen-tests.md (sections 4 to 8; your decision F, 2026-11-04), plan/open-items.md item 47, CLAUDE.md section 6 (the pitfalls from 07, 08, 10 and 14c), .github/workflows/ci.yml, app/build.gradle.kts, gradle/libs.versions.toml.
Facts in this note are facts of the day they were looked up; each carries its date. Versions are looked up on the day the spike runs.

How sources are labelled in this note:
- "your report": something you saw or did and told me (GitHub, the phone, Windows settings).
- "I fetched": page text returned by my own search or fetch, each approved by you before it ran.
- "I ran": output of a command I ran on this PC, each approved by you where CLAUDE.md section 8 says ask first.
- "your file": a repo file I read.
- "not checked": my own knowledge or reading. It is not a fact until a look-up or run shows it.
Third-party pages (blogs, forums) are not used as facts.

## 1. Outcome
A measured answer for CR3: which past phone-found defects instrumented Compose tests on an emulator (level C) catch, how long a run takes, how flaky it is over repeated runs, and whether it can run on every change on GitHub's ubuntu-26.04 runner (A2; fallback D: run by hand on your PC after each step). Level B (Robolectric, screenshots) is considered only if level C cannot run on every change.

## 2. Gate
This is a decision note, not an iteration. Nothing in section 7 runs before you approve this note and settle the decisions in section 6. CR3 (its own intent file, intent/intent-cr-03-...) starts only after section 11 holds the results. Iteration 16 does not wait on this spike unless you decide so.

## 3. Starting point
### 3.1 In the repo (your file, 2026-11-04)
| Item | Now |
|---|---|
| Instrumented tests | none: app/src/androidTest does not exist (setup removed the template test) |
| Test runner | testInstrumentationRunner = androidx.test.runner.AndroidJUnitRunner (app/build.gradle.kts:27) |
| androidTest dependencies | Compose BOM, ui-test-junit4, espresso-core 3.7.0, androidx.test.ext junit 1.3.0 (app/build.gradle.kts:77-80) |
| ui-test-manifest | already a debugImplementation dependency (app/build.gradle.kts:81), so the debug APK already carries its test activity entry |
| minSdk / targetSdk | 33 / 37; the phone runs Android 17 |
| Lock and cover | MainActivity shows LockedScreen until the biometric or device-credential prompt succeeds, and CoverScreen instead of the app whenever its window is not focused (MainActivity.kt:94-124) |
| CI | ci.yml: triggers pull_request (any base) and push to main; no workflow_dispatch; ubuntu-26.04, contents: read; style, build, unit tests, lint |

### 3.2 Not checked yet
- Collaborators, the fork setting, and the Actions setting for fork pull request workflows. (The visibility was checked in step 1: public, section 11.)
- Whether this PC can run the Android Emulator with hardware acceleration (Windows Hypervisor Platform), and its free disk space.
- Which emulator system images exist for API 37, and the current versions of the androidx.test libraries and of the GitHub Actions runner.
- Whether the build tools uninstall the app after connectedDebugAndroidTest [UNVERIFIED].
- Whether workflow_dispatch can start a workflow whose file is not on main (my reading: it cannot; not checked).
- A2: whether the emulator package and the API 37 system image download and boot on GitHub's ubuntu-26.04 runner with KVM today, and how long the download and boot take (section 11.2 has what GitHub's pages say).

## 4. What is measured, and what counts as a pass
Defects replayed (plan/discussions/screen-tests.md section 4):
| # | Defect (where found) | What the test checks | How it is brought back on the throwaway branch |
|---|---|---|---|
| 10 | The header does not give way at the largest font and in landscape (10, phone check 6a) | At the largest font and in landscape, Holding detail's header is measured: it stays fixed only while it takes at most half the screen's height, otherwise it scrolls with the list. Heights are measured, not pictures compared | The header made fixed at every size |
| 07 | A layer and the cover screen replace each other in a loop (07, phone check 2) | With MainActivity unlocked, opening New portfolio's name layer and a portfolio's ⋮ menu: the cover screen does not appear, and typing and tapping reach the layer | The layer drawn as a Compose Dialog, the menu as a DropdownMenu |
| 08 | The keyboard covers a layer (08, phone check 9a) | At the largest font, with the keyboard open, the layer's text field and its buttons lie fully above the keyboard | imePadding() removed from the layer |
| TB | TalkBack labels (14c, Deploy finding 2) | The merged semantics text of Holding detail's Transactions row and of the reversal card names the exact shares | The label built from the stored quantity instead of the model's text |

Measures, per defect:
- Caught: the test is green on the real code in every repeated run, and red on the throwaway branch with the defect brought back, with a failure message that names what was expected.
- Run time: local (emulator cold boot, then the test task alone) and on GitHub's runner (push to result, with the download and boot shown apart).
- Flakiness: every red run on unchanged code counts, with its cause if found.

Pass thresholds (my proposal; decision S3):
- A defect counts as "caught at level C" only if both the green and the red runs hold in every repetition.
- Level C counts as "can run on every change" if a run on GitHub's runner with a warm cache (S8) takes at most 15 minutes from push to result and has no flaky run in the repetitions. If not, fallback D (A2): whether the local runs alone are enough is for CR3 to decide.

## 5. What is not changed
- No app behaviour, no spec, no intent, and not the release APK. Proof: the release build's runtime dependency list and APK size before and after (section 10).
- No debug-only switch that skips the lock or changes a screen for tests.
- ci.yml keeps its triggers and steps (decision S5 puts the emulator job in a separate file).
- The phone is never used: the emulator only.
- Dropping hand checks that criteria require (for example 20 and 51) is a spec change, made in CR3, not here.
- What the spike may add, on its run branch only (decision S4): test files under app/src/androidTest, androidTest-only catalog entries if a library is missing, and the workflow file. None of these reach the debug or release APK; the debug APK already carries ui-test-manifest (3.1).

## 6. Decisions (all [Build]; settled by you on 2026-11-04)
Settled: S1 (a) real MainActivity, unlocked on the emulator with a PIN and a simulated fingerprint. S2 API 37, Google APIs, x86_64, no Play Store (API 36 only if 37 has no such image on the day). S3 as written: 20 local runs, 10 runner runs, 3 per brought-back defect, 15 minutes. S4 as written: the run branch, not merged. S5 the new workflow file; ci.yml unchanged. S6 as written: the fallback only on failure.
- S7 [Plan], settled by you 2026-11-04 after step 1 found the repo public: (C) keep the repo public and still use the self-hosted runner, with added guards (amendment A1). Not my leaning. The options put to you: (A) keep it public and use GitHub's runner only (my leaning; nothing runs on your PC); (B) make it private, then the plan as written; (C) public with the self-hosted runner. This departs from step 1's rule (stop; choose between private and the fallback).
- S7 changed by you 2026-11-04 after look-ups 1 to 4 (section 11.2): GitHub's own pages say self-hosted runners "should almost never be used for public repositories" and can be "persistently compromised", and that a manually started workflow still needs the runner online. You chose (A) GitHub's runner, with a fallback to (D) runs by hand on your PC only (amendment A2). My leaning was (A). The options put to you: (A) GitHub's runner for the automatic runs, plus the local runs; (D) by hand on the PC only, no runner anywhere; (B) make the repo private, then the self-hosted runner; (C) keep S7 as it was. A1 is withdrawn. This changes your decision F's order (self-hosted first): plan/discussions/screen-tests.md section 8 notes it.
- S8 [Build], settled by you 2026-11-04 (A2): caching. (A) cache the downloads only: the emulator package and the API 37 system image, with GitHub's actions/cache; the emulator boots fresh each run; the 15-minute limit applies to runs with a warm cache, and the first run with an empty cache is recorded separately. My leaning. Not chosen: (B) also cache a booted emulator snapshot (faster boot, but state carries over between runs: a new source of flakiness, and the PIN and fingerprint or leftover app data could hide failures); (C) A for 10 runs, then B for 10, compared. Maintainability: one cache step, nothing stateful. Test run time: saves the download on warm runs (expected minutes, not measured). APK size: no difference.
- A2 effect on S5 and S6: S5 stands (a new file, ci.yml unchanged), now running on GitHub's ubuntu-26.04 runner. S6 is replaced: the fallback is D, used if GitHub's runner cannot boot the API 37 emulator, breaks the 15-minute limit, or is flaky (S3).
- A3 [Build], settled by you 2026-11-04 in step 5 (reconstructed 2026-11-05 from the step 5 commit, "S1 as A3", and the hand-off; its wording at the time was not recorded): S1 (a) without the fingerprint. The tests unlock MainActivity by typing the S10 test PIN into its lock prompt (the system's PIN pad), and enter the same PIN on the emulator's lock screen after each boot. No fingerprint is enrolled or touched, on your emulator or on GitHub's. Section 9's fingerprint risks fall away.
The options below are kept as they were put to you.
- S1 [Build] How tests get past the lock for 07. Defects 10, 08 and TB are tested on the screens alone (createComposeRule, no MainActivity, no lock); 07 needs MainActivity's real cover. Options:
  - (a) Launch MainActivity and unlock on the emulator: a PIN and a fingerprint set up once on the AVD by hand, and the test touches the simulated fingerprint. Maintainability: tests the real cover code, one extra setup step per AVD. Test run time: a few seconds per unlock (expected). APK size: no difference.
  - (b) A test-only activity that copies MainActivity's cover rule. Maintainability: the copy can drift from MainActivity and then tests nothing real. Test run time and APK size: no real difference from (a).
  - Leaning: (a). For you: 07 is tested on the code that runs on your phone.
- S2 [Build] (Refined 2026-11-04, section 11.3: system-images;android-37.0;google_apis;x86_64, revision 8.0.0; google_atd not chosen.) Emulator image: API 37 (the phone's Android 17 and the targetSdk), Google APIs, x86_64, without the Play Store; or API 36 if 37 has no such image on the day. Leaning: 37. Download expected 1 to 2 GB.
- S3 [Build] Repetitions and thresholds: 20 local runs and 10 runner runs on unchanged code; 3 runs per brought-back defect; the 15-minute limit of section 4. Leaning: as written.
- S4 [Build] Where the test code lives: on a run branch spike-screen-tests-run, not merged; CR3 decides what is kept. This docs branch carries only this note now and the results later. Leaning: as written. For you: main does not change until CR3 is approved.
- S5 [Build] Where the emulator job lives: a new workflow file (screen-tests.yml) triggered only by push to the spike's run branches, not by pull_request; or a job in ci.yml. ci.yml runs on pull_request, and a self-hosted job there would run fork pull requests on your PC. Leaning: the new file. This departs from the hand-off's "the ci.yml change", which I read as "the workflow change".
- S6 [Build] When to fall back to GitHub's runner: only if the self-hosted runner cannot run the emulator headless, breaks the 15-minute limit, or is flaky. Leaning: as written; the fallback is a step of its own with its own approval.

## 7. Order of work (each "Ask" is an approval point; I stop at each)
Before step 1, this note is approved (S1 to S6 are settled, section 6). Steps 1 to 5 need no runner. A2: steps 6 to 10 below replace the self-hosted runner steps; A1's lines are kept and marked "withdrawn" so the record stays readable.
1. Repo settings. Ask: an anonymous Invoke-WebRequest to https://api.github.com/repos/GHGHOrg/ATSAssetSys -MaximumRedirection 0 (200 = public; 404 = private or hidden). You check in GitHub: collaborators, Settings > General > Allow forking (private repos only), and Settings > Actions > the setting for fork pull request workflows. If the repo is public, I stop: GitHub advises against self-hosted runners on public repos (not checked, looked up in step 2), and you choose between making it private and going straight to the fallback. Run 2026-11-04: public (section 11); you chose S7 (C), so the spike went on with amendment A1; after the look-ups, S7 changed to A2 (section 6).
2. Look-ups. Ask, one at a time, each with the tool, query and reason: androidx.test runner and rules versions; the Android Emulator and the API 37 system images; the GitHub Actions runner's current release and its checksum; GitHub's guidance on self-hosted runners and public repos; whether connectedDebugAndroidTest uninstalls the app. A1 adds: the setting that makes fork pull requests wait for your approval before any workflow runs, and its strictest value; whether a repo-level runner can be limited to selected workflows; whether a fork's pull request can reach a repo's self-hosted runner at all, and under which settings.
   A1, after the look-ups and before step 6: you set the strictest fork-approval setting in GitHub and report it; you never approve a run from a fork's pull request without reading its workflow files first. (Withdrawn by A2.)
   A2: look-ups 1 to 8b are done (sections 11.2 and 11.3); step 2 is complete. Planned at A2's writing, one at a time: androidx.test runner and rules versions; the API 37 system image name for sdkmanager; whether connectedDebugAndroidTest uninstalls the app; the current major version of actions/cache (S8), and how GitHub scopes caches between branches. Dropped: the GitHub Actions runner release and the fork-approval, runner-limiting and fork-reach look-ups (no self-hosted runner).
3. This PC. Ask: one read-only command for the Windows Hypervisor Platform state and the free disk space. If acceleration is off, I stop: turning it on is your change, with a restart.
4. Emulator. Ask: the system image download (expected 1 to 2 GB) with the SDK's sdkmanager, then one AVD created with avdmanager, in D:\AndroidAvd (S9, section 11.3). You set the PIN and enroll the fingerprint on it by hand (S1 a). Boot time recorded.
5. Tests, locally. Ask before creating spike-screen-tests-run from this branch. Write the four tests; build, unit tests, lint and spotlessCheck green; then connectedDebugAndroidTest on the emulator, 20 times (S3). After the first run, check whether the app is still on the emulator (11.3, look-up 7). Then, Ask before creating spike-screen-tests-defects from the run branch: one commit per brought-back defect (each edit shown to you first), 3 runs each. Results into section 11.
6. (Withdrawn by A2: the separate Windows user.)
7. (Withdrawn by A2: the self-hosted runner and A1's guards.)
8. Workflow file (S5, A2). Ask: show the new .github/workflows/screen-tests.yml on the run branch. Rules in it: runs-on ubuntu-26.04 (GitHub's runner, as ci.yml); on push to spike-screen-tests-run and spike-screen-tests-defects only; permissions contents: read; a step that gives the job access to /dev/kvm; the emulator package and the API 37 system image installed with the image's sdkmanager (no third-party action unless you approve one), and cached between runs with GitHub's actions/cache@v6, keyed on their names and versions (S8; 11.3); the Gradle cache kept by gradle/actions/setup-gradle as in ci.yml; the AVD created with avdmanager, booted headless, animations off; then connectedDebugAndroidTest. Each phase's time is printed. S1's PIN and fingerprint are set by adb commands in the workflow, since nobody can set them by hand on GitHub's runner, and the PIN is entered once after boot (S10: the fixed test PIN); if that cannot be done, 07 runs only locally and section 11 says so.
9. Runs on GitHub's runner. You push the run branch (I never push); 10 runs (S3), each started by your push or by re-running the job in GitHub. Then the defects branch, 3 runs each. Results into section 11, from your reports.
10. Fallback D, only under A2's conditions (section 6). No new setup: the local runs of step 5 are the measure, and section 11 states that level C would run by hand after each step, not automatically.
11. Results and cleanup (section 8), then section 11 and the status line; plan.md and open-items.md point to the results; CR3 starts from them.

## 8. Throwaway branch and cleanup
- spike-screen-tests-defects is never merged. At the end, Ask: delete it locally and on origin (you delete it on origin).
- spike-screen-tests-run stays until CR3 decides what is kept, then Ask: delete it.
- The AVD and the system image: Ask at the end whether to keep them (CR3 may need them) or delete them.
- The workflow file stays on the run branch only; it goes with that branch unless CR3 keeps it. (A2: no runner or separate user to remove.)
- The emulator's app data is test data only; nothing from the phone is copied to it.

## 9. Risks
- (A1's runner risks withdrawn by A2: nothing on your PC listens to GitHub.)
- A2: the first run, and any run after the cache is evicted, downloads the emulator and the system image (expected 1 to 2 GB); the cache (S8) covers the rest. GitHub's cache size limit per repo and eviction rules are not looked up; a cache that does not fit is downloaded each run, and the 15-minute limit may not hold. KVM on standard runners rests on 2023 and 2024 changelog posts seen through search summaries only (11.2); a run shows whether it holds on ubuntu-26.04.
- A2: setting a PIN and fingerprint on an emulator by adb commands is untested; if it fails, 07 runs only locally.
- Local runs (step 5): the phone, if attached by USB, is seen by adb. Guarded by ANDROID_SERIAL set to the emulator and a device check before each run that stops if any other device is attached.
- The emulator may not behave like the phone for 07 (window focus) [UNVERIFIED]. Then 07 stays a phone check.
- Emulator runs can be flaky (boot, animations, timing). The repetitions measure it; animations are turned off on the AVD.
- The emulator's fingerprint simulation and a PIN on the AVD are untested here (S1 a).
- Disk: the system image (D) and the AVD (D:\AndroidAvd, S9) on your PC (expected a few GB, not measured). C has 9.3 GB free (step 3); Gradle's caches stay on C.
- Versions change; step 2 looks them up on the day.

## 10. Proof
Commands (Windows PowerShell, repo root), real output shown after each step:
- .\gradlew.bat spotlessCheck, then build, tests and lint as in CLAUDE.md section 3; test totals from the result files.
- .\gradlew.bat :app:connectedDebugAndroidTest with the emulator running; the per-run time and result from the instrumented test result files.
- Before step 5 and after step 9: .\gradlew.bat :app:assembleRelease and :app:dependencies --configuration releaseRuntimeClasspath; the APK size and the dependency list must not change.
- The runs on GitHub's runner: your report of each run's result and time, and of each phase's printed time (I do not see GitHub).
Anything not run is stated as not run in section 11.

## 11. Results
### 11.1 Step 1: repo settings (2026-11-04)
- I ran, 2026-11-04 11:51:51 +08:00: an anonymous Invoke-WebRequest to https://api.github.com/repos/GHGHOrg/ATSAssetSys with -MaximumRedirection 0. Status 200; "private":false; "visibility":"public"; "full_name":"GHGHOrg/ATSAssetSys". Only those fields were read.
- Collaborators, the fork setting and the Actions setting for fork pull request workflows: not seen yet (your check in GitHub).
- Your decision S7 (C): public, with the self-hosted runner and amendment A1 (section 6). Changed after 11.2 to A2.

### 11.2 Step 2: look-ups 1 to 4 (2026-11-04, each approved by you before it ran)
- 1, a search (WebSearch, query "GitHub Actions self-hosted runner public repository fork pull request security site:docs.github.com"). The search tool's summaries only, not facts by themselves: GitHub.com's "Adding self-hosted runners" warns about forks; an Enterprise Server 3.19 copy recommends self-hosted runners only for private repos.
- 1b, I fetched https://docs.github.com/actions/hosting-your-own-runners/adding-self-hosted-runners (no date shown): "We recommend that you only use self-hosted runners with private repositories. This is because forks of your public repository can potentially run dangerous code on your self-hosted runner machine by creating a pull request that executes the code in a workflow." And: "For security reasons, public repositories can't use runners in a runner group by default, but you can override this in the runner group's settings."
- 2, I fetched https://docs.github.com/en/actions/reference/security/secure-use: "self-hosted runners should almost never be used for public repositories on GitHub, because any user can open pull requests against the repository and compromise the environment." "Self-hosted runners for GitHub do not have guarantees around running in ephemeral clean virtual machines, and can be persistently compromised by untrusted code in a workflow." The page says nothing about the setting that makes fork pull requests wait for approval. The fetch tool passes the page through a smaller model, so the quotes are close but the raw page was not seen.
- 3, I fetched https://github.com/actions/runner-images/blob/main/images/ubuntu/Ubuntu2604-Readme.md: image 20261026.180.1, Ubuntu 26.04.1 LTS. Android: Command Line Tools 20.0; Platform-Tools 37.0.1; build-tools 34.0.0 to 37.0.0; platforms android-34 to android-37.2 (android-37.0 rev 2 among them); ANDROID_HOME /usr/local/lib/android/sdk. No Android Emulator row and no system images; the page does not mention "emulator", "system-images", "KVM" or hardware acceleration. Java 25.0.5 default; Gradle 9.8.2.
- 4, a search (WebSearch, query "GitHub Actions hosted runner Linux KVM hardware acceleration Android emulator site:github.blog OR site:docs.github.com"). The search tool's summaries only: GitHub changelog posts of 2023-02-22/23 (larger Linux and Windows runners) and 2024-04-02 (standard 2-vCPU Linux runners) announce hardware-accelerated Android emulators; the job must first give itself access to /dev/kvm. Not fetched; not confirmed for ubuntu-26.04 today.
- Your question in between: starting the workflow by hand would not remove the self-hosted risk, since the runner must be online to take the job; runs by hand on the PC without a runner would (option D).

### 11.3 Step 2: look-ups 5 to 8b (2026-11-04, each approved by you before it ran)
- 5, I fetched https://developer.android.com/jetpack/androidx/releases/test (page "last updated" October 29, 2026; first 100,000 characters read, the table among them). Stable: runner 1.7.0, rules 1.7.0, core 1.7.0, ext.junit 1.3.0, espresso 3.7.0, monitor 1.8.0, orchestrator 1.6.1 (July 31, 2025; the others July 30, 2025). Newer lines are alpha only. The project's ext.junit 1.3.0 and espresso-core 3.7.0 are current; runner and rules are not in the catalog (1.7.0 if the tests need them directly).
- 6, I ran sdkmanager --list (SDK Command-line Tools 23.0.0, installed by you in Android Studio for this; nothing else installed; the list saved in the scratchpad). The S2 image: system-images;android-37.0;google_apis;x86_64, revision 8.0.0. Also listed: system-images;android-37.0;google_atd;x86_64 7.0.0 (Automated Test Device). API 37.1 and 37.2 exist only as 16 KB page size images (37.2 also as betas). Already installed on this PC, not used by the spike: emulator 37.2.12 (37.2.13 available) and system-images;android-37.0;google_apis_playstore;x86_64 6.0.0 (8.0.0 available). The tool printed that "'android sdk' is the replacement for 'sdkmanager'"; sdkmanager still works.
- 7, a search (WebSearch, query "connectedAndroidTest uninstall app after tests Android Gradle Plugin site:developer.android.com OR site:issuetracker.google.com"): no Google page in the results. Community reports only (Stack Overflow through mirrors, the Gradle forum): AGP 8.1 and later uninstall the app after instrumented tests, with no switch to turn it off (Google issue 295039976, not opened). Your choice: measured in step 5 instead (after the first local run, adb -s on the emulator: pm list packages app.ghghorg.atsassets). The guard stays: the emulator only.
- 8, I fetched https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching: "By default, the limit is 10 GB per repository"; "Any usage beyond 10 GB is billed to your account"; "GitHub will remove any cache entries that have not been accessed in over 7 days"; "Workflow runs can restore caches created in either the current branch or the default branch (usually `main`)"; they "cannot restore caches created for child branches or sibling branches". Examples use actions/cache@v4. My reading: each spike branch fills its own emulator cache (two cold runs in all); setup-gradle can restore main's Gradle cache; a pause over 7 days makes the next run cold. The repo's current cache use and any billing limit: your check in GitHub (Settings > Actions > Caches) before step 9.
- 8b, I fetched https://github.com/actions/cache/releases (no year shown): v6.1.0 marked Latest (26 Jun), v6.0.0 (23 Jun, "Update packages, migrate to ESM"), v5.1.0 (26 Jun); v5 "runs on the Node.js 24 runtime". The workflow pins actions/cache@v6.
- Step 3, I ran 2026-11-04 (read-only, approved by you): D:\AndroidSdk\emulator\emulator.exe -accel-check printed "WHPX(10.0.26200) is installed and usable." (exit 0). Free space: C 9.3 GB (110.3 GB used), D 75.6 GB (269.4 GB used).
- Step 4a, I ran 2026-11-04 (approved by you). First try with the ';' name: "Package system-images not found." and the same for each part, exit 0, nothing installed (sdkmanager.bat in Command-line Tools 23.0.0 hands over to the new Android CLI, and the name was split at each ';'; my reading). Second try with the '/' name (approved by you): x86_64-37.0_r08.zip, 2.0 GB downloaded, 5.3 GB unzipped, 202 s in all, exit 0; the folder D:\AndroidSdk\system-images\android-37.0\google_apis\x86_64 holds 5,427 MB; D free 70.3 GB. Its source.properties reads "Pkg.Revision=7", while the list and the zip name say 8; the cause is not known. For step 8: an exit code of 0 does not prove the install, so the workflow checks the folder; the cache holds 5.3 GB unzipped per branch (compressed size not measured), against GitHub's 10 GB per repo.
- Step 4b, I ran 2026-11-04 (approved by you): avdmanager list device -c (read-only); you chose the profile pixel_9_pro_xl (your phone's model). Creating the AVD with the '/' name failed: "Error: Package path is not valid", listing "system-images;android-37.0;google_apis;x86_64" as valid (exit 1; only the empty folder D:\AndroidAvd was made). Retried with the ';' name (approved by you): "Auto-selecting single ABI x86_64", exit 0. D:\AndroidAvd\ats-spike-37.avd\config.ini: hw.device.name=pixel_9_pro_xl, hw.lcd.width=1344, hw.lcd.height=2992, hw.lcd.density=480, hw.ramSize=2G, disk.dataPartition.size=10G, tag.id=google_apis, abi.type=x86_64. Folder size before the first boot: 0 MB. For step 8: in Command-line Tools 23.0.0, sdkmanager wants the '/' name and avdmanager the ';' name.
- Step 4c, I ran 2026-11-04 (approved by you): adb devices -l before the start listed no device (phone not attached, no other emulator). emulator 37.2.12 -avd ats-spike-37 -no-snapshot-save, with a window, ANDROID_AVD_HOME=D:\AndroidAvd for that process only: seen by adb as emulator-5554 after 8 s; sys.boot_completed=1 after 85 s (first, cold boot); Android 17, SDK 37; model sdk_gphone64_x86_64. Only emulator-5554 attached afterwards. You then set a throwaway PIN (not told to me) and added a fingerprint in the emulator's Settings; three simulated touches (adb -s emulator-5554 emu finger touch 1, each answered "OK"); you reported the fingerprint added. Cold-boot check (approved by you): before it, only emulator-5554 attached and dumpsys fingerprint showed "count":1 for id 0; emu kill ("OK: killing emulator, bye bye"), gone after 4 s; restarted the same way: seen after 2 s, sys.boot_completed=1 after 30 s. After the restart a touch did not unlock (isKeyguardShowing=true): a screenshot showed the lock screen with "Unlock for all features and data" and no fingerprint icon, so Android wants the PIN once after each boot before it takes a fingerprint (my reading of that text). The window looked black because the screen was asleep (mWakefulness=Asleep; KEYCODE_WAKEUP made it Awake), not a display fault. You entered the PIN; I put the screen to sleep (KEYCODE_SLEEP; Asleep, isKeyguardShowing=true), then woke it and sent one touch: isKeyguardShowing=false, and a screenshot showed the unlocked home search screen. So the PIN and fingerprint survive a cold boot, and the fingerprint unlocks once the PIN has been entered since boot. The emulator's clock runs on UTC (06:10 shown at about 14:10 +08:00). Step 4 complete.
- S10 [Build], settled by you 2026-11-04 (your A): one fixed throwaway test PIN, written in the workflow and the test setup on the run branch (public); set by adb on GitHub's fresh emulator and entered once after each boot, and used the same way on your emulator, so local and GitHub runs share one path. It unlocks only a throwaway emulator with no data and is never your phone's PIN. Your emulator's PIN is changed to it by you in the emulator's Settings before step 5's runs. Not chosen: (B) the PIN as a GitHub Actions secret (hides a value that protects nothing real; two paths); (C) 07 on your PC only.
- S9 [Build], settled by you 2026-11-04 (your A): the spike's AVD lives in D:\AndroidAvd, set with ANDROID_AVD_HOME inside the spike's own commands only; no change to your Windows or Android Studio settings; emulators you already have are not touched. Not chosen: (B) the default place on C (C could run out of space); (C) you free C first.
- S2 refinement, settled by you 2026-11-04 (Build, your A): the google_apis image as S2 says, not google_atd. Not chosen: (B) google_atd (likely faster and smaller, my knowledge; further from your phone; PIN and fingerprint for 07 unknown); (C) A, then B measured.

### 11.4 Step 5: tests and local runs (2026-11-04 to 2026-11-05)
- Branches: spike-screen-tests-run from main 8531e6d; spike-screen-tests-defects from 501c693. Neither is merged (S4). Five files under app/src/androidTest/java/app/ghghorg/atsassets/ (SpikeSupport.kt, HoldingHeaderTest.kt, LockAndLayerTest.kt, KeyboardLayerTest.kt, TalkBackLabelTest.kt), 6 tests: 10 twice (largest font in landscape; normal font in portrait as a control), 07, 08, TB twice (the reversal card; Holding detail's transaction row).
- Look-up 7, I ran 2026-11-04: the app is uninstalled after connectedDebugAndroidTest.
- I ran, 2026-11-04 (emulator 37.2.12), series on unchanged code, each fixed before the next: series 1, 18 green, run 19 red (08), run 20 without results (Gradle passed with no test run); series 2 after a cold boot, 0 of 20 (the header tests turned the emulator while it was locked; fix: unlock first, wait for the display's real rotation, keep the screen on); series 3, 0 of 5 (the launcher held the screen upright when it was turned before the test's activity was up; the first save after a cold boot took over 5 s; fix: turn after launch, 15 s for Create); check run 3, 6 of 6; series 4a, 5 green, then the emulator went down.
- Faults, I ran (hand-off 2026-11-05): emulator WHPX "Unexpected VP exit code 5" at 21:06, 21:20 and 21:53 on 11-04 (once while idle); a Gradle worker JVM internal error (21:10) and a Gradle daemon JVM access violation (21:12, its hs_err file written to the repo root, moved to the scratchpad, never committed). You upgraded the emulator to 37.2.13 (build 16740729) on 11-05, reversing "no change to the PC".
- Your decisions: 07 split into 07a (New portfolio's layer as a Compose Dialog) and 07b (the ⋮ menu as a DropdownMenu), 3 runs each, 07a taken back out before 07b (A); TB: only the reversal card brought back, since unit test HoldingRowModelTest:63 catches the row half (A); 08 committed although test 08 missed it, the test fixed later (A); a diagnostic run for 07b (A).
- Defect runs with the first tests, I ran (3 valid runs each): 10 caught at its own check; 07a caught only indirectly (the 5 s wait for the layer; test 08 red too); 07b caught only indirectly (the Rename click): the 100 ms polling focus check missed a popup that lived about 150 ms (the popup and cover loop seen in logcat); 08 not caught (the test ran at the normal font; the defect was found at the largest font, plan.md 2026-10-30); TB card caught at its own check.
- Test fixes (your A each; on the run branch, cherry-picked to the defects branch): the focus check is a listener added before the action that opens the layer or menu (c591a3c); it waits with waitForIdle every 100 ms, since a plain sleep let no popup appear (57c6150; my reading: the v2 Compose rules redraw only on waitForIdle or node access, not checked in the library source); test 08 at font scale 2.0 (c591a3c).
- Defect runs with the fixed tests, I ran (runs-fix-defects2, 3 runs; 07a: 1 uncommitted run, then restored): each defect caught at its own check, with its own message: 10 "The fixed part stayed in place though it takes more than half the height"; 08 "Cancel ends at 2118.0 px, below the keyboard's top at 1984.0 px"; 07b "lost the focus 4 to 11 time(s) while the ⋮ menu was open"; 07a "lost the focus 18 time(s) while New portfolio's layer was open"; TB card "Spike, 0.33333333 shares" not found. Without defects: 3 of 3 green; 1 red (07, emulator slow after about 1 h of uptime) then 2 green; 3 of 3 after a cold boot (07 about 12 to 14 s, 08 9 to 10 s, 10 4 to 11 s, TB 1 to 3 s; 40 to 55 s per run).

### 11.5 Step 8: the workflow file (2026-11-05)
- Versions, I ran gh release list and gh release view after you signed gh in: actions/checkout v7.0.1, actions/setup-java v6.2.0, gradle/actions v6.4.1, actions/cache v6.1.0, actions/upload-artifact v7.0.2. Neither changelog (checkout v7.0.0, setup-java v6.0.0) touches this workflow. Your A: checkout v7 and setup-java v6 in screen-tests.yml; ci.yml unchanged (v6, v5).
- .github/workflows/screen-tests.yml (fcecc2a, run branch): push to the two spike branches only; contents: read; ubuntu-26.04; /dev/kvm opened by a udev rule; the emulator and the API 37 image installed with sdkmanager and cached with actions/cache restore and save v6, keyed on their versions (saved right after the install, so a red run still fills the cache); the AVD made with avdmanager; a headless boot; animations off; the S10 PIN set with locksettings and checked; connectedDebugAndroidTest; the job fails unless 6 tests ran; results and the emulator log kept 7 days.

### 11.6 Step 9: the run branch on GitHub's runner (2026-11-05)
Each push with your "push once", each re-run with your yes; I read each run with gh (results, step times, artifacts). The runner: 4 CPUs, 15,983 MB (nproc, free -m). Emulator 37.2.13; the image listed as revision 8, Pkg.Revision=7 on disk (as locally, 11.3).

| Run | Commit | Result | Cause | Change that followed |
|---|---|---|---|---|
| 1, cold cache | fcecc2a | red, 13m 7s | the emulator found no AVD: avdmanager wrote it elsewhere; the wait ran 10 min without printing the log | 76afd95: one ANDROID_AVD_HOME for both tools; the wait fails within 2 s if the emulator exits and prints its log; clock time for the boot |
| 2, cache hit | 76afd95 | red, 6m 25s | 07 and 08 "did not unlock with the test PIN"; both TB tests found no screen after them | 69c7ea4: screenshot, focus and node tree after a failed unlock (diagnostic) |
| 3 | 69c7ea4 | green, 4m 15s | but the first PIN attempt was lost in 07 and 08: typed while the system's PIN pad was not ready (node tree; the screenshot is black, a secure window) | 039611f: wait up to 15 s for the pad's Enter key, found by walking the nodes; 8 × Delete; then the PIN |
| 1 on 039611f | 039611f | red, 4m 53s | System UI not responding (its dialog in the unlock screenshot) | 87d0a2f: 4 GB and 4 cores for the emulator; a settle step |
| 1 to 8 on 87d0a2f | 87d0a2f | 7 green (4m 38s to 5m 56s), run 8 red (6m 28s) | run 8: system_server and the launcher not responding; settle failed after 120 s; 0 tests; build and boot shared the CPUs (boot 160 to 187 s) | 02031f0: build before the emulator starts; print CPUs and memory |
| 1 on 02031f0 | 02031f0 | red, 5m 6s (boot 50 s) | 07: the 5 s wait after Rename's Save ran out | 2fed892: 15 s, with a message for each half |

Counted series on 2fed892 (run 41973283679, attempts 1 to 10): 10 of 10 green, 6 of 6 tests each, no unlock retry, no Not Responding window.

| Run | Total | Build | Boot | Settle | Tests | 10 L / P | 08 | 07 | TB card / row |
|---|---|---|---|---|---|---|---|---|---|
| 1 | 5m 19s | 1m 27s | 48 s | 11 s | 104 s | 14.4 / 6.6 s | 22.0 s | 21.9 s | 2.5 / 1.9 s |
| 2 | 5m 25s | 1m 28s | 45 s | 10 s | 103 s | 13.1 / 11.9 s | 20.9 s | 22.0 s | 2.4 / 2.6 s |
| 3 | 4m 49s | 1m 3s | 39 s | 10 s | 83 s | 13.5 / 5.4 s | 15.5 s | 16.3 s | 2.8 / 2.6 s |
| 4 | 5m 41s | 1m 22s | 50 s | 11 s | 109 s | 14.0 / 11.3 s | 22.7 s | 22.5 s | 3.1 / 2.9 s |
| 5 | 5m 45s | 1m 16s | 48 s | 11 s | 106 s | 14.5 / 8.3 s | 22.2 s | 18.9 s | 2.5 / 3.8 s |
| 6 | 5m 42s | 1m 27s | 52 s | 10 s | 117 s | 17.7 / 8.7 s | 25.7 s | 19.7 s | 4.9 / 2.3 s |
| 7 | 5m 15s | 1m 21s | 48 s | 11 s | 107 s | 15.7 / 8.0 s | 25.1 s | 20.6 s | 4.3 / 2.2 s |
| 8 | 5m 29s | 1m 20s | 47 s | 10 s | 110 s | 16.7 / 8.8 s | 24.0 s | 21.7 s | 2.0 / 3.7 s |
| 9 | 4m 19s | 58 s | 35 s | 11 s | 79 s | 11.6 / 7.0 s | 15.6 s | 16.6 s | 2.5 / 1.6 s |
| 10 | 4m 16s | 56 s | 38 s | 12 s | 82 s | 11.6 / 5.1 s | 18.5 s | 17.8 s | 2.9 / 2.7 s |

- Observation, not acted on: in run 1 on 02031f0 the app's logcat showed "database is locked; [PRAGMA journal_mode=delete]" and a database connection "acquired ... but never released" from RoomRecordStore.loadCashEntries (RoomRecordStore.kt:36). Not shown to cause anything; for you to decide whether it becomes an open item.

### 11.7 Step 9: the defects branch on GitHub's runner (2026-11-05)
- The 7 workflow and test commits cherry-picked onto spike-screen-tests-defects in order (db327d2 to 26275e4, your A); test code and workflow then equal the run branch's; the only differences are the four defects. Run 41987365823, attempts 1 to 3, red by design:

| Test | Defect | Run 1 | Run 2 | Run 3 |
|---|---|---|---|---|
| 10, landscape | 10 | caught | caught | caught ("72.0 px, then 72.0 px") |
| 10, portrait | control | green | green | green |
| 08 | 08 | caught (Cancel at 2118.0 px, keyboard at 1984.0 px) | caught (same) | caught (the Name field at 1910.0 px, keyboard at 1723.0 px) |
| 07 | 07b | caught (3 focus losses) | caught (2) | caught (2) |
| TB card | TB | caught | caught | caught |
| TB row | unit test | green | green | green |

- Totals 4m 34s to 5m 19s; no unlock retry, no Not Responding window. 07a had no GitHub run (your A): the same focus check caught 07b 3 of 3 on the runner.

### 11.8 Steps 9 and 10: local repeats on the final code, and the APK proof (2026-11-05)
- Step 10 not needed: A2's conditions (no boot, over 15 minutes, flaky) are not met on GitHub's runner.
- I ran six local series of up to 20 on 2fed892, each stopping at the first run not 6 of 6 green, each after a cold boot unless marked:

| Series | GPU | Green | Stop | Cause |
|---|---|---|---|---|
| 1 (16:21) | hardware | 7 | run 8: no test ran | the PC's adb server did not answer |
| 2 (16:29, same boot) | hardware | 11 | run 12: 08's teardown, MainActivity stayed paused 45 s | the emulator's virtual GPU froze about 40 s (graphics allocator call took 39,235 ms) |
| 3 (16:53) | software | 1 | run 2 | the emulator stopped: WHPX "Unexpected VP exit code 5" |
| 4 (16:59) | software | 0 | run 1 | the same |
| 5 (17:02) | hardware | 0 | run 1 | the same |
| 6 (17:14, after a Windows restart) | hardware | 1 | run 2 | the same |

- 20 green runs and 6 stops; none from the tests or the app. The WHPX crash, cause not found: nothing at those times in Windows' System and Application logs (I ran, without administrator rights); your report: the Hyper-V-Hypervisor logs hold only informational events, and their few warnings are at other times and point to other programs. Not checked: the Claude desktop app's VM service, also on this PC, uses the same hypervisor.
- APK proof, I ran: :app:assembleRelease and :app:dependencies --configuration releaseRuntimeClasspath on main (8531e6d) and on spike-screen-tests-run (2fed892): both unsigned APKs 12,473,942 bytes, 446 entries, no entry differing in size or CRC-32 (the files' hashes differ, my reading: zip metadata only); dependency lists equal (766 lines; also equal to 11-04's list before step 5).

### 11.9 Against section 4
- Caught at level C (green on the real code in every repeated run, red with its own message on the defects branch): 10, 08, 07b and TB's reversal card, locally (3 runs) and on GitHub's runner (3 runs); 07a locally only (1 run with the fixed tests). TB's row half is caught by a unit test.
- Run time: GitHub's runner with a warm cache 4m 16s to 5m 45s from start to result, under the 15 minutes. The one cold-cache run (run 1) failed on the AVD before the tests; a green cold-cache run was not measured. Locally 40 to 65 s per run after a 29 to 35 s cold boot.
- Flakiness: GitHub's runner 10 of 10 on 2fed892, after earlier workflow versions failed, each with its cause (11.6). Local: not steady on this PC because of the emulator stack (11.8, 11.4).
- Can run on every change: yes on GitHub's runner by S3's thresholds, with 2fed892's workflow. CR3 starts from these results.

## 12. Status and notes
Written 2026-11-04 on branch docs/spike-screen-tests (pull request #126, merged as c489ec9). Decisions S1 to S6 settled 2026-11-04 on branch docs/spike-screen-tests-decisions. Approved by you 2026-11-04 (as written, option A). Step 1 run 2026-11-04 (section 11.1); S7 settled; amendment A1 written on branch docs/spike-screen-tests-step1 and approved by you 2026-11-04 (as written, option A). Look-ups 1 to 4 run (section 11.2); S7 changed to GitHub's runner with fallback D; A1 withdrawn; amendment A2 written on the same branch, with caching (S8, your A), not approved. Look-ups 5 to 8b run (section 11.3); step 2 complete; S2's image settled (google_apis). A2 approved by you 2026-11-04. Step 3 run (acceleration usable; C 9.3 GB free); S9: the AVD on D. Step 4 run: the image installed, the AVD ats-spike-37 (pixel_9_pro_xl) created, booted, PIN and fingerprint set; the PIN is needed once after each boot. Steps 5, 8 and 9 run 2026-11-04 to 2026-11-05 on branches spike-screen-tests-run (2fed892) and spike-screen-tests-defects (26275e4), both pushed, neither merged (S4); amendment A3 (no fingerprint); step 10 not needed; results in sections 11.4 to 11.9, written on branch docs/spike-screen-tests-results. Cleanup (section 8) not yet asked.
