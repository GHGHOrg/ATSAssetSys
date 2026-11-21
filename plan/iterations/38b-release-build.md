# plan/iterations/38b-release-build.md

Stage: Build. Status: approved 2026-11-21; decisions 0 to 4 settled the same day, decision 5 in step 2 (section 11). You asked for steps 1 to 6 in one go, one commit per step. Branch iter-38b-release-build, from main (30c354b).
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7. CR4 is split into 38a (done), 38b (this file) and 38c (the first release); plan.md section 4 and 38a's decision 0.

## 1. Outcome
Everything a release needs, proven before the first one, with your app (45) not touched:
- The release build's code runs on the phone: open items 3 and 4 settled. Under R8 it crashed at start, so R8 is off (decision 5, section 14; open item 72).
- CI builds the unsigned release APK on a `v*` tag, checks that the tag equals versionName and that versionCode rose, and keeps the APK and mapping.txt as the run's artifacts.
- A Claude Code hook makes `adb install`, `git tag`, `gh release` and `assembleRelease` ask every time.
- A rollback rehearsed once on the development app (AC-66), and the release steps, the rollback commands and the release notes template written down for 38c.

## 2. Spec refs
- C23 (release-only defects): open item 4 settled; CI builds the release APK on every tag.
- C22 (no downgrade; a rollback loses recent entries): the rehearsal (AC-66); its [UNVERIFIED] downgrade refusal checked on the phone.
- Section 8, "Releases and the format version": the backup kept before a release, written into the release steps and the notes template.
- AC-63, the CI part: the tag equals versionName, versionCode higher than the previous release's, the unsigned release APK built. The GitHub Release itself is 38c.
- AC-66 (rollback rehearsed).
- C21 (the release key is forever): only the steps written for 38c; no key is created in 38b.

## 3. Out of scope
- The release key, signing with it, the first tag, the GitHub Release and the move of your data: 38c (C17, C21, AC-35, AC-36, AC-63's record, AC-64).
- Migration tests from released schema versions: no release exists yet, so there is no released schema to migrate from. The first release (38c) is schema 14, and the first release after it that raises the schema needs a test from 14 (written into the release steps).
- Any change to what the app does or shows (CR4's constraint), except keep rules if the release build needs them to work at all (open item 4).

## 4. Analysis (2026-11-21, by reading; nothing run)
- app/build.gradle.kts:31-36: the release build has `optimization { enable = true; packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**") }` and no signing config. My reading, not checked: `packageScope` limits R8 to those packages, so the app's own code (app.ghghorg.atsassets.**) is neither shrunk nor renamed. That would explain open item 3 (mapping.txt lists no app.ghghorg.atsassets.data class, and the dex keeps their names). To be checked against AGP 9.4.1's jar in step 2, not from memory.
- Room: SqlCipherOpener.kt:28 calls `Room.databaseBuilder<AtsDatabase>(…)` with no factory, so Room finds `AtsDatabase_Impl` by its name at run time (my reading of Room 3; checked in step 2). If the app's package is outside R8's scope, the name stays and open item 4 should pass; only running it shows it.
- The release build can't be installed as it is: it is unsigned, and its id `app.ghghorg.atsassets` is your app's. Your app (45) is a debug build signed with this PC's debug key, so a release APK signed with the same debug key would install over your app. Running the release code in 38b needs another id (decision 1).
- No proguard-rules.pro exists; the libraries' own consumer rules apply. Corrected in step 2: app/src/main/keepRules/rules.keep exists, AGP's template with no rule in force (from the setup, bf0c018); a keep rule would go there.
- .github/workflows has ci.yml and screen-tests.yml; neither runs on a tag. Actions in use: checkout@v6, setup-java@v5, setup-gradle@v6, upload-artifact@v7.
- .claude/settings.json has allow, ask and deny lists, no hooks. CLAUDE.md section 6: project rules stay inactive until Claude Code's trust dialog is accepted for the exact path; whether hooks behave the same is not known (section 10).
- The development app keeps the APK it was installed from only in app\build, which every build overwrites. The rehearsal needs the older APK kept (step 5).

## 5. Files
- app/build.gradle.kts: the `probe` build type (decision 1), added and removed again in step 2; keep rules only if step 2 needs them (then app/proguard-rules.pro, new, kept).
- app/src/probe/res/values/strings.xml (new in step 2, removed at its end): the probe's name (section 12).
- .github/workflows/release.yml (new): on a `v*` tag and a manual dry run; the version checks; the unsigned APK and mapping.txt as artifacts (decision 4).
- .github/scripts/release-version-check.sh (new): the tag and versionCode checks, run by release.yml and locally (decision 4).
- .github/workflows/ci.yml: a step "Release build", `assembleRelease`, on every pull request and push to main (decision 2).
- .claude/settings.json: the hook (asks first; section 10's research before).
- plan/release.md (new): the release steps of CR4, the rollback commands, the release notes template (decision 3).
- Documents: this file; plan.md (section 2's list, section 11's row, the change log); plan/open-items.md (items 3 and 4); CLAUDE.md (section 3's release commands, section 8's hook, pitfalls found).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop.
1. This file, plan.md's lines.
2. Open items 3 and 4: `packageScope` read in AGP 9.4.1's jar; `assembleRelease` (asks, the hook not yet in place); mapping.txt and the dex read; the `probe` build (decision 1) built with `assembleProbe` (asks) and its APK read (id, R8 applied); installed next to your app and ATS Dev; with test data (a restore of an ATS Dev backup): unlock, Overall, one holding, a price update, a backup export, with a Logcat capture; keep rules only if it fails, test first where a unit test can show it. Then the probe uninstalled, and its build type and name file removed (deleting asks). Open items 3 and 4 closed or recorded.
3. ci.yml's "Release build" step (decision 2), its time measured on the pull request; release.yml and its version checks, proven per decision 4.
4. The hook in .claude/settings.json (your yes), proven by trying each command form and answering No.
5. The rollback rehearsal on the development app (AC-66), with C22's downgrade check; plan/release.md with the commands as run (decision 3).
6. CLAUDE.md: the release commands, the hook, the pitfalls found.
7. The Deploy review of 38b, findings by severity.

## 7. Stubs
None.

## 8. Risks
1. Decision 1's probe is not the release you install in 38c: another id, another signature. It proves the release code once (since decision 5 without R8, the same code as the development app); the signed release first runs at 38c's smoke check (AC-64), and later releases have no probe (decision 1's B), so an R8 defect a later release brings first shows on your real-data app, with the rollback (C22) as the way back.
2. The hook may stay inactive for the same reason as the allow rules (CLAUDE.md section 6, trust): step 4 tries it, and a hook that doesn't fire is a failed step.
3. The tag job can't run on a real tag before 38c (only you create tags; AC-63 wants the tag to equal versionName). Decision 4's dry run proves all but the tag trigger, which first fires on `v0.38.0`.
4. Your app stays at 45 until 38c (38a risk 1). Keep 38b and 38c close.
5. A run artifact is deleted by GitHub after a time (90 days by default, from memory, not checked); mapping.txt must also be kept by you, next to the key's backups (open item 3).
6. The local build's intermittent failures (open item 60), now also for assembleRelease: read the cause, re-run unchanged, compare the APK's dex sizes.

## 9. Proof
- Unit tests: any keep rule or version check that can be tested off the phone.
- Screen tests: none new; the 178 run on the pull request.
- Step 2 on the phone: the release code (decision 1) restores the test backup, unlocks, shows Overall, opens a holding, updates prices and exports a backup; Logcat holds no data and no crash.
- Step 3: the check script run locally with a matching tag (passes), a tag that does not match and a versionCode that did not rise (each fails); release.yml's dry run green on the branch (decision 4), its artifacts read (aapt2 on the APK: versionName, versionCode; mapping.txt present).
- Step 4: each of the four command forms asks; answered No, nothing runs.
- Step 5 (AC-66), on the development app: a backup; a newer build (48) installed over 47; an entry made; `adb install -r` of 47 over 48 refused (C22, expected INSTALL_FAILED_VERSION_DOWNGRADE); uninstall, install 47, restore; the counts and the cash balance match the figures from before 48; the entry made on 48 is gone.
- Standing checks: no screen changes; AC-35 for the development app at step 5.

## 10. Research (before the step that needs it; each fetch or search asks first)
- Claude Code hooks: how a PreToolUse hook makes a command ask, its settings form, and whether it needs the trust dialog like the allow rules. Not written from memory (CLAUDE.md section 8). Source: the Claude Code hooks page (code.claude.com/docs), fetched with your yes, before step 4.
- GitHub Actions: the `v*` tag trigger, reading the previous release's versionCode, artifact retention. Checked on GitHub's docs pages with your yes, before step 3.
- AGP's `packageScope`: in the cached gradle-9.4.1.jar (reading outside the repo, asks), step 2.
- Room 3's lookup of `AtsDatabase_Impl`: javap on the cached room3-runtime jar (asks), step 2.

## 11. Decisions needed from you (Build)
0. 38b as one iteration or two (plan.md section 5: an iteration with more than one capability is split before it starts; settled 2026-11-21: your A): (A, chosen) one iteration, its capability "the release path proven before the first release"; each part is small and none is of use before 38c. Not chosen: (B) two, the release build and release.yml, then the hook, the rehearsal and the docs, with the first release moving to 38d; (C) three.
1. How the release code runs on the phone without the release key and without touching your app (open item 4; settled 2026-11-21: your B): (B, chosen) a `probe` build type, `initWith(release)` (R8 and the same `packageScope`), id `app.ghghorg.atsassets.probe`, its own name (section 12), signed with the debug key, built only when asked (`assembleProbe`; CI does not build it); installed next to your app and ATS Dev for step 2's checks with test data, uninstalled after, and the build type and its name file removed in the same step once open item 4 is settled. Later releases are checked by the smoke check (AC-64) only. Not chosen: (A) the probe kept for every later release; (C) no probe, open item 4 settled at 38c's smoke check on your real-data app.
2. Whether CI also builds the release APK on every pull request, or only on a tag (settled 2026-11-21: your A): (A, chosen) a step "Release build" in ci.yml runs `assembleRelease` (unsigned) on every pull request and push to main, so a break of the release build (R8, a library's rules) turns the pull request red; expected 1 to 3 minutes more per CI run, measured in step 3, beside the screen tests' 10 minutes. Not chosen: (B) only on a `v*` tag; (C) only on pull requests that change a build file.
3. Where the release steps, the rollback commands and the notes template live, and whether the rollback is a script or a list (settled 2026-11-21: your A): (A, chosen) one file, plan/release.md, next to plan.md and setup.md: CR4's nine steps, section 8's backup rule, the rollback as a short list of commands run one at a time (each `adb install` asks through the hook), and the release notes template; the rehearsal (AC-66) runs the list as written. Not chosen: (B) the same file plus scripts/rollback.ps1 (the hook sees only the script's name; a wrong path could uninstall before it fails); (C) spread over CLAUDE.md, a GitHub release template and the 38c file.
4. How release.yml is proven before 38c's real tag (settled 2026-11-21: your A): (A, chosen) release.yml also has a manual run (`workflow_dispatch`) with a dry-run input; in step 3 it runs on the 38b branch (your yes): the unsigned release APK built, the version checked as for a tag with `v<versionName>` taken from the build file, versionCode checked against the newest `v*` tag (none yet), the APK and mapping.txt kept as artifacts. The check is a small shell script (.github/scripts/, new), also run locally with a tag that does not match and a versionCode that did not rise, each expected to fail. The tag trigger itself first fires on `v0.38.0` in 38c; the GitHub Release waits for that run. Not chosen: (B) a throw-away tag pushed and deleted; (C) no proof before 38c.
4a. Where decision 4's dry run runs, after GitHub's workflow syntax page said a manual run "only receives events when the workflow file is on the default branch", so release.yml can't be started by hand before 38b is merged (step 3; settled 2026-11-21: your A): (A, chosen) release.yml also runs on a pull request that changes it or its check script (`pull_request` with `paths`), as a dry run: the check made as for `v<versionName>`, the unsigned APK built and kept. 38b's own pull request is the proof. The manual run stays for later use from main; on a branch it is a dry run, on a tag it checks that tag, so no dry-run input is needed. Not chosen: (B) the manual run only, once on main after the merge; (C) both.
5. R8 in the release build, after the probe crashed at start under R8 (step 2; settled 2026-11-21: you asked me to decide, I chose A): (A, chosen) R8 off, `optimization { enable = false }`: the release runs the same unshrunk code as the development app, so this kind of fault can't occur; the APK grows from 14,475,110 to 36,305,404 bytes (measured), which matters only for the install over USB; tests take the same time; bringing R8 back is open item 72, and with it decision 4's mapping.txt artifact lapses until then. Not chosen: (B) R8 kept with a rule that stops it moving classes out of their packages (to be looked up; any library update could bring a new R8 fault that only the phone shows); (C) only `kotlin.**` taken out of `packageScope` (3,491 kotlinx and androidx classes stay moved).

## 12. Wording to approve (D9)
- The probe's name on the phone, shown only during step 2 (decision 1): "ATS Probe" (approved 2026-11-21, your A; not chosen: "ATS R8", "ATS Release Test").

## 13. Status and notes
- Draft 2026-11-21; approved the same day.

### 13.1 Steps
- Step 1 (2026-11-21): this file; plan.md (section 2's list, rows 38b in sections 4 and 11, the change log). Only documents changed. The first full run with --rerun-tasks failed after 2m 21s; I kept only its last lines, so its cause is not known (likely open item 60, not shown). A plain re-run passed, and the same forced command re-run unchanged, with all output kept, was green in 1m 46s: 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 17:07:43). From here every run's whole output is kept in the scratchpad.
- Step 2 (2026-11-21):
  - Open item 3: AGP's DSL documents `packageScope` as the packages included for optimization (default "**"); with androidx, kotlin and kotlinx only, R8 left app.ghghorg.atsassets alone. `assembleRelease`: app-release-unsigned.apk 14,475,110 bytes, mapping.txt 0 app classes, classes.dex 5,876,840 bytes holding `AtsDatabase_Impl` under its name. Room finds it by name; Room's consumer rule keeps `RoomDatabase` subclasses' constructors. Item 3 closed.
  - The probe (decision 1; name "ATS Probe", section 12): app-probe.apk 14,483,306 bytes, `app.ghghorg.atsassets.probe`, versionCode 47, versionName 0.38.0-probe, signed with the debug key, not debuggable. Installed next to your app (uid 10454) and ATS Dev (10446), uid 10447, with a Logcat capture by uid.
  - Check 1, first try, failed: the probe crashed at every start, three times, before the app's code ran: `IllegalAccessError: Class kotlin.collections.j extended by class zm is inaccessible`, in `ProcessLifecycleInitializer` under `androidx.startup`. The mapping: R8 moved `kotlin.collections.CollectionsKt__MutableCollectionsJVMKt` to the root package as `zm`, away from its package-private parent `CollectionsKt__IterablesKt` (`kotlin.collections.j`). The release build's mapping held the same two lines, so your app would have crashed at its first release. R8 had moved 160 of 259 kotlin, 331 of 366 kotlinx and 3,160 of 3,568 androidx classes to the root package; configuration.txt has no rule asking for it (only `-allowaccessmodification` from AGP's default file). A web search (your yes) found no report of this case. Decision 5: R8 off; open item 72.
  - Without R8: `assembleRelease` and `assembleProbe` green in 1m 6s; app-release-unsigned.apk 36,305,404 bytes, app-probe.apk 36,313,604 bytes, three dex files, class names kept. The probe installed over the first (`install -r`).
  - Check 1, second try, passed: restore of an ATS Dev backup, unlock, Overall's cash balance and portfolio costs equal to ATS Dev's, a holding opened, the key entered and a price update, a backup exported. Logcat 21:14:05 to 21:17:58, 358 lines: no crash, no app tag, no token, "finnhub" or URL; warnings from the system only, 32 of them open item 71's "A resource failed to call release". Item 4 closed.
  - The probe uninstalled (your A); its build type and app/src/probe removed.
  - Full run with --rerun-tasks: the first failed after 2m 34s in `:core:test`, `BackupReportTest.aFileOf3000Transactions_isCheckedInTime`: 5,300 ms above its bound of 5,000 ms (2,230 ms in the full run of 2026-11-16). This step changed no core code; the core tests ran while app compiled. The same command re-run unchanged, test not touched: green in 1m 51s, the check took 1,753 ms; 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 21:24:55). Whole output of both runs kept.
- Step 3 (2026-11-21):
  - Research (your yes; the web-fetch agent): GitHub's workflow syntax page (docs.github.com/en/actions/reference/workflows-and-actions/workflow-syntax): `on.push.tags` takes glob patterns, and with only `tags` "the workflow won't run for events affecting the undefined Git ref"; a manual run "only receives events when the workflow file is on the default branch" (decision 4a). upload-artifact's README (github.com/actions/upload-artifact): `@v7`; artifacts "retained for 90 days by default", between 1 and 90, changeable in the repository's settings; `if-no-files-found` defaults to `warn`.
  - .github/workflows/release.yml (new): on a `v*` tag, a pull request changing it or its script, and a manual run; checkout with `fetch-depth: 0` (for the earlier tags; from memory, not in the pages read: the first release with an earlier tag shows it), the version check, `assembleRelease`, the unsigned APK as artifact `release-<tag>` or `release-dry-run`, `if-no-files-found: error`, kept 90 days. No mapping.txt while R8 is off (decision 5).
  - .github/scripts/release-version-check.sh (new, LF; .gitattributes now keeps `*.sh` LF): the tag equals `v<versionName>`, versionName is MAJOR.MINOR.PATCH, versionCode is higher than the newest other `v*` tag's (read with `git show <tag>:app/build.gradle.kts`); no earlier tag passes with a note. Run locally in Git Bash, 9 cases, each as expected: in this repo a dry run, tag v0.38.0 (pass) and v0.38.1 (fail); in a scratch repo with a lightweight tag v0.37.0 on a tree with versionCode 47 (no commit, so no signing), versionCode 47 and 46 (fail), 48 (pass, also as dry run), versionName 0.38 (fail), and the pushed tag v0.38.0 present beside v0.37.0 (pass).
  - ci.yml: the step "Release build" (`assembleRelease`) after Lint (decision 2). Its time and release.yml's dry run are measured on 38b's pull request (decision 4a); nothing on GitHub has run yet.
  - Full run with --rerun-tasks plus `:app:assembleRelease`: green in 2m 52s at the first try; 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 21:40:35), app-release-unsigned.apk 36,305,404 bytes.
- Step 4 (2026-11-21):
  - Research (your yes; the web-fetch agent): the Claude Code hooks page (code.claude.com/docs/en/hooks) and the permissions page. A PreToolUse hook gets the call as JSON on stdin (`tool_input.command`); printing `hookSpecificOutput.permissionDecision` "ask" with exit 0 makes the call ask, the reason shown in the prompt; the matcher `Bash|PowerShell` covers both shell tools; on Windows the hook runs in Git Bash; the `if` filter is "best-effort", so the hook matches in a script. Not stated on the pages: whether a hook's "ask" beats an allow rule (tried below), and whether hooks need the trust dialog (a summary of the permissions page lists settings-file hooks as used even in a folder never trusted; unconfirmed).
  - .claude/hooks/release-ask.sh (new, LF): asks for `adb … install` (uninstall included), `git … tag`, `gh release` and `assembleRelease`, `bundleRelease`, `packageRelease`, matched in the whole call as text; any other call gets no answer. Fed 17 sample calls in Git Bash: the 11 release forms asked (a wrapped `& $adb install`, `git -C … tag -s`, `git status; git tag --list` among them), the 6 others did not (`adb devices`, `git log --tags`, `gh pr checks`, `testReleaseUnitTest` among them).
  - .claude/settings.json (your yes): `hooks.PreToolUse`, matcher `Bash|PowerShell`, command `bash "${CLAUDE_PROJECT_DIR}/.claude/hooks/release-ask.sh"`.
  - Proof, each answered No by you: `adb install does-not-exist.apk`, `git tag --list`, `gh release list`, `.\gradlew.bat :app:assembleRelease --dry-run`, and `.\gradlew.bat :core:assemble :app:assembleDebug :app:assembleRelease --dry-run`, which an allow rule matches; each prompt showed "Release command (38b hook)" (your A). The control, the same allowed line without the release task (`--dry-run`), ran without a prompt, so the allow rules are active and the hook's "ask" beat the allow rule. Risk 2 did not occur.
  - From this step on, a full run that includes `assembleRelease` asks.
- Step 5 (2026-11-21), the rollback rehearsal (AC-66) on ATS Dev, your app (uid 10454) not touched; each adb command asked through the hook:
  - The 47 APK kept in the scratchpad (`0.38.0-dev.47`, 44,619,614 bytes, aapt2); your figures noted and a backup saved on ATS Dev 47.
  - versionCode 48 (app/build.gradle.kts; it stays 48, a number once installed is not reused): ATS Dev 48 built (`0.38.0-dev.48`, 44,619,618 bytes, every dex the same size as 47's), installed over 47 with `adb install -r`: Success.
  - On 48 a cash deposit of 12.34, "rehearsal 48"; the balance rose by 12.34.
  - `adb install -r` of 47 over 48: `INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected: Update version code 47 is older than current 48`; 48 stayed. C22's [UNVERIFIED] downgrade refusal seen on the phone.
  - `adb uninstall app.ghghorg.atsassets.debug`, `adb install` of 47: both Success; ATS Dev's uid is now 10448 (was 10446).
  - On 47 the backup restored: the Cash balance and each portfolio's cost matched, the rehearsal entry gone. AC-66 passed.
  - plan/release.md (new): CR4's nine steps with the commands, section 8's rule, the rollback as a list run one command at a time (as rehearsed), the release notes template, the rehearsal record. The apksigner commands are left to 38c, from apksigner's help on the day.
  - Found while writing it: decision 5 conflicts with spec C23 ("R8 shrinks and renames code in the release build") and AC-63 ("the SHA-256 … of mapping.txt"); with R8 off there is no mapping.txt. release.md marks the line as pending; the spec change is your decision (Design).

## 14. Departures from the plan
- Step 2: R8 is off in the release build (decision 5), not kept as planned; open item 72. Decision 4's mapping.txt artifact has nothing to keep while R8 is off; step 3 keeps the APK only. Section 4's "No proguard-rules.pro" corrected (rules.keep exists). AGP's `packageScope` was read in its DSL documentation, not in the jar (section 10).
- Step 3: release.yml's dry run is proven on 38b's pull request, not by a manual run on the branch (decision 4a); release.yml has no dry-run input. .gitattributes gains `*.sh text eol=lf` (not in section 5's list).
- Step 4: the hook's matching lives in a script, .claude/hooks/release-ask.sh (new, not in section 5's list), because the docs call the `if` filter best-effort.
