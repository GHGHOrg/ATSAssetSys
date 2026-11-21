# plan/iterations/38b-release-build.md

Stage: Build. Status: approved 2026-11-21; decisions 0 to 4 settled the same day (section 11). You asked for steps 1 to 6 in one go, one commit per step. Branch iter-38b-release-build, from main (30c354b).
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7. CR4 is split into 38a (done), 38b (this file) and 38c (the first release); plan.md section 4 and 38a's decision 0.

## 1. Outcome
Everything a release needs, proven before the first one, with your app (45) not touched:
- The release build's code runs on the phone under R8: open items 3 and 4 settled.
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
- No proguard-rules.pro exists; the libraries' own consumer rules apply.
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
1. Decision 1's probe is not the release you install in 38c: another id, another signature. It proves R8 and the code once; the signed release first runs at 38c's smoke check (AC-64), and later releases have no probe (decision 1's B), so an R8 defect a later release brings first shows on your real-data app, with the rollback (C22) as the way back.
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

## 12. Wording to approve (D9)
- The probe's name on the phone, shown only during step 2 (decision 1): proposed before step 2.

## 13. Status and notes
- Draft 2026-11-21; approved the same day.

### 13.1 Steps
- Step 1 (2026-11-21): this file; plan.md (section 2's list, rows 38b in sections 4 and 11, the change log). Only documents changed. The first full run with --rerun-tasks failed after 2m 21s; I kept only its last lines, so its cause is not known (likely open item 60, not shown). A plain re-run passed, and the same forced command re-run unchanged, with all output kept, was green in 1m 46s: 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 17:07:43). From here every run's whole output is kept in the scratchpad.

## 14. Departures from the plan
None yet.
