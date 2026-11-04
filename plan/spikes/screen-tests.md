# plan/spikes/screen-tests.md

Stage: Plan (open item 47's remainder, before change request CR3). Status: written 2026-11-04, not approved. Nothing in section 7 runs until you approve this note and settle the decisions in section 6.
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
A measured answer for CR3: which past phone-found defects instrumented Compose tests on an emulator (level C) catch, how long a run takes, how flaky it is over repeated runs, and whether it can run on every change from a self-hosted GitHub Actions runner on your PC (fallback: an emulator on GitHub's runner). Level B (Robolectric, screenshots) is considered only if level C cannot run on every change.

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
- The repo's visibility, collaborators, fork setting, and the Actions setting for fork pull request workflows.
- Whether this PC can run the Android Emulator with hardware acceleration (Windows Hypervisor Platform), and its free disk space.
- Which emulator system images exist for API 37, and the current versions of the androidx.test libraries and of the GitHub Actions runner.
- Whether the build tools uninstall the app after connectedDebugAndroidTest [UNVERIFIED].
- Whether the emulator runs headless (-no-window) under a Windows service account.
- Whether workflow_dispatch can start a workflow whose file is not on main (my reading: it cannot; not checked).

## 4. What is measured, and what counts as a pass
Defects replayed (plan/discussions/screen-tests.md section 4):
| # | Defect (where found) | What the test checks | How it is brought back on the throwaway branch |
|---|---|---|---|
| 10 | The header does not give way at the largest font and in landscape (10, phone check 6a) | At the largest font and in landscape, Holding detail's header is measured: it stays fixed only while it takes at most half the screen's height, otherwise it scrolls with the list. Heights are measured, not pictures compared | The header made fixed at every size |
| 07 | A layer and the cover screen replace each other in a loop (07, phone check 2) | With MainActivity unlocked, opening New portfolio's name layer and a portfolio's ⋮ menu: the cover screen does not appear, and typing and tapping reach the layer | The layer drawn as a Compose Dialog, the menu as a DropdownMenu |
| 08 | The keyboard covers a layer (08, phone check 9a) | With the keyboard open, the layer's text field and its buttons lie fully above the keyboard | imePadding() removed from the layer |
| TB | TalkBack labels (14c, Deploy finding 2) | The merged semantics text of Holding detail's Transactions row and of the reversal card names the exact shares | The label built from the stored quantity instead of the model's text |

Measures, per defect:
- Caught: the test is green on the real code in every repeated run, and red on the throwaway branch with the defect brought back, with a failure message that names what was expected.
- Run time: local (emulator cold boot, then the test task alone) and on the runner (push to result).
- Flakiness: every red run on unchanged code counts, with its cause if found.

Pass thresholds (my proposal; decision S3):
- A defect counts as "caught at level C" only if both the green and the red runs hold in every repetition.
- Level C counts as "can run on every change" if a runner run takes at most 15 minutes from push to result and has no flaky run in the repetitions.

## 5. What is not changed
- No app behaviour, no spec, no intent, and not the release APK. Proof: the release build's runtime dependency list and APK size before and after (section 10).
- No debug-only switch that skips the lock or changes a screen for tests.
- ci.yml keeps its triggers and steps (decision S5 puts the emulator job in a separate file).
- The phone is never used: the emulator only.
- Dropping hand checks that criteria require (for example 20 and 51) is a spec change, made in CR3, not here.
- What the spike may add, on its run branch only (decision S4): test files under app/src/androidTest, androidTest-only catalog entries if a library is missing, and the workflow file. None of these reach the debug or release APK; the debug APK already carries ui-test-manifest (3.1).

## 6. Decisions (to be settled one at a time before section 7 starts; leanings are mine)
- S1 [Build] How tests get past the lock for 07. Defects 10, 08 and TB are tested on the screens alone (createComposeRule, no MainActivity, no lock); 07 needs MainActivity's real cover. Options:
  - (a) Launch MainActivity and unlock on the emulator: a PIN and a fingerprint set up once on the AVD by hand, and the test touches the simulated fingerprint. Maintainability: tests the real cover code, one extra setup step per AVD. Test run time: a few seconds per unlock (expected). APK size: no difference.
  - (b) A test-only activity that copies MainActivity's cover rule. Maintainability: the copy can drift from MainActivity and then tests nothing real. Test run time and APK size: no real difference from (a).
  - Leaning: (a). For you: 07 is tested on the code that runs on your phone.
- S2 [Build] Emulator image: API 37 (the phone's Android 17 and the targetSdk), Google APIs, x86_64, without the Play Store; or API 36 if 37 has no such image on the day. Leaning: 37. Download expected 1 to 2 GB.
- S3 [Build] Repetitions and thresholds: 20 local runs and 10 runner runs on unchanged code; 3 runs per brought-back defect; the 15-minute limit of section 4. Leaning: as written.
- S4 [Build] Where the test code lives: on a run branch spike-screen-tests-run, not merged; CR3 decides what is kept. This docs branch carries only this note now and the results later. Leaning: as written. For you: main does not change until CR3 is approved.
- S5 [Build] Where the emulator job lives: a new workflow file (screen-tests.yml) triggered only by push to the spike's run branches, not by pull_request; or a job in ci.yml. ci.yml runs on pull_request, and a self-hosted job there would run fork pull requests on your PC. Leaning: the new file. This departs from the hand-off's "the ci.yml change", which I read as "the workflow change".
- S6 [Build] When to fall back to GitHub's runner: only if the self-hosted runner cannot run the emulator headless, breaks the 15-minute limit, or is flaky. Leaning: as written; the fallback is a step of its own with its own approval.

## 7. Order of work (each "Ask" is an approval point; I stop at each)
Before step 1, this note is approved and S1 to S6 are settled. Steps 1 to 5 need no runner; steps 6 to 9 set it up.
1. Repo settings. Ask: an anonymous Invoke-WebRequest to https://api.github.com/repos/GHGHOrg/ATSAssetSys -MaximumRedirection 0 (200 = public; 404 = private or hidden). You check in GitHub: collaborators, Settings > General > Allow forking (private repos only), and Settings > Actions > the setting for fork pull request workflows. If the repo is public, I stop: GitHub advises against self-hosted runners on public repos (not checked, looked up in step 2), and you choose between making it private and going straight to the fallback.
2. Look-ups. Ask, one at a time, each with the tool, query and reason: androidx.test runner and rules versions; the Android Emulator and the API 37 system images; the GitHub Actions runner's current release and its checksum; GitHub's guidance on self-hosted runners and public repos; whether connectedDebugAndroidTest uninstalls the app.
3. This PC. Ask: one read-only command for the Windows Hypervisor Platform state and the free disk space. If acceleration is off, I stop: turning it on is your change, with a restart.
4. Emulator. Ask: the system image download (expected 1 to 2 GB) with the SDK's sdkmanager, then one AVD created with avdmanager. You set the PIN and enroll the fingerprint on it by hand (S1 a). Boot time recorded.
5. Tests, locally. Ask before creating spike-screen-tests-run from this branch. Write the four tests; build, unit tests, lint and spotlessCheck green; then connectedDebugAndroidTest on the emulator, 20 times (S3). Then, Ask before creating spike-screen-tests-defects from the run branch: one commit per brought-back defect (each edit shown to you first), 3 runs each. Results into section 11.
6. The separate Windows user. You create it as administrator (a standard user, steps written for you). Ask: icacls on the keystore and signing files' folder and on ~/.claude, to show the new user has no read right. I never read those files.
7. Runner. Ask: download the runner release from step 2 into an empty scratchpad folder and check its SHA-256 against GitHub's. You run its config step yourself under the new user (the registration token is a secret and stays out of the chat) and install it as a service under that user. Its labels: self-hosted, Windows, X64, emulator.
8. Workflow file (S5). Ask: show the new .github/workflows/screen-tests.yml. Rules in it: on push to spike-screen-tests-run and spike-screen-tests-defects only; no pull_request; permissions contents: read; ANDROID_SERIAL set to the emulator; a first step that lists adb devices and fails if any device other than the emulator is attached (so the phone is never touched, even if plugged in).
9. Runner runs. You push the run branch (I never push); 10 runs (S3), each started by your push or by re-running the job in GitHub. Then the defects branch, 3 runs each. Results into section 11.
10. Fallback, only under S6. Ask: a workflow on GitHub's ubuntu-26.04 runner with an emulator, looked up on the day; the same measures.
11. Results and cleanup (section 8), then section 11 and the status line; plan.md and open-items.md point to the results; CR3 starts from them.

## 8. Throwaway branch and cleanup
- spike-screen-tests-defects is never merged. At the end, Ask: delete it locally and on origin (you delete it on origin).
- spike-screen-tests-run stays until CR3 decides what is kept, then Ask: delete it.
- The AVD and the system image: Ask at the end whether to keep them (CR3 may need them) or delete them.
- The runner and the separate user: kept if CR3 adopts them; otherwise you remove the runner in GitHub's settings and uninstall the service, then remove the user.
- The emulator's app data is test data only; nothing from the phone is copied to it.

## 9. Risks
- A self-hosted runner runs workflow code on your PC. Guarded by: no pull_request trigger, pushes to two named branches only, a separate standard user, contents: read. Not guarded: a commit that you push yourself runs as written.
- The phone attached by USB could be seen by the runner user's adb (my reading, not checked). Guarded by the device check of step 8 and ANDROID_SERIAL.
- The emulator may not behave like the phone for 07 (window focus) [UNVERIFIED]. Then 07 stays a phone check.
- Emulator runs can be flaky (boot, animations, timing). The repetitions measure it; animations are turned off on the AVD.
- The emulator's fingerprint simulation and a PIN on the AVD are untested here (S1 a).
- Disk: the system image, the AVD and a second Gradle cache for the runner user (expected several GB, not measured).
- A Windows service session may have no display; the emulator then needs -no-window (not checked).
- Versions change; step 2 looks them up on the day.

## 10. Proof
Commands (Windows PowerShell, repo root), real output shown after each step:
- .\gradlew.bat spotlessCheck, then build, tests and lint as in CLAUDE.md section 3; test totals from the result files.
- .\gradlew.bat :app:connectedDebugAndroidTest with the emulator running; the per-run time and result from the instrumented test result files.
- Before step 5 and after step 9: .\gradlew.bat :app:assembleRelease and :app:dependencies --configuration releaseRuntimeClasspath; the APK size and the dependency list must not change.
- The runner's runs: your report of each run's result and time (I do not see GitHub).
Anything not run is stated as not run in section 11.

## 11. Results
(Empty until the spike runs.)

## 12. Status and notes
Written 2026-11-04 on branch docs/spike-screen-tests. Not approved. Decisions S1 to S6 not settled.
