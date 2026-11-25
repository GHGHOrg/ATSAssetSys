# plan/iterations/42-release-1.1.0.md

Stage: Build. Status: Approved 2026-11-25 (your A); decisions 0 to 5 settled the same day (your A each; section 11). Branch iter-42-release-1.1.0, from main (adfd275).
Source: plan.md section 4 (row 42, added in step 1); intent/intent-cr-05-day-change-and-screens.md (CR5, final) and iteration 41 (CR5 built, merged as e032c53); spec/spec.md v8; plan/release.md (a release's steps; section 3, the schema rule); CLAUDE.md section 4 (version rules after 1.0.0: a feature raises MINOR). No intent file: this iteration changes no app behaviour, only versionName and versionCode (as 40).

## 1. Outcome
Your app is the signed release `1.1.0` (versionCode 52), installed over `1.0.0` with your data kept and moved from database schema 14 to 15, and the GitHub Release `v1.1.0` records it:
- The tag `v1.1.0` is on main's merge commit of the version raise; release.yml built and checked it; you signed the APK it built with the release key (C21: the same certificate as 0.38.0 and 1.0.0).
- A backup taken on 1.0.0 before the install is kept on the phone until the next release, the rollback's backup (spec section 8, "Releases and the format version"; plan/release.md section 3).
- `adb install -r` over 1.0.0 kept your data through the migration 14 to 15 (AC-35), and the smoke check passed (AC-64).
- The record says the schema version rose from 14 to 15 and the backup format version (1) is unchanged (AC-63).

## 2. Spec refs
- C17 (the ADB route), C21 (the release key is forever), C22 (rollback).
- Section 8, "Releases and the format version": the schema raise marked in the record; the backup taken before the install and kept until the next release.
- AC-35 (a release installs over the previous one with `adb install -r`, not debuggable), AC-63 (the release record), AC-64 (smoke check).
- What 1.1.0 adds (for the record's summary only; built and checked in 41): C24, C25, 4.8, section 6 (views 1 to 3 and 6, About), AC-67 to AC-71.

## 3. Out of scope
- Any change to what the app does or shows. Only versionName and versionCode change in the app.
- Dependabot #236 (AGP 9.5.2), #237 (compose-bom 2026.11.01) and #238 (kotlinx-serialization-json 1.13.0): after this release. Merging one first would change the release build's libraries, and LicenceListTest would ask for About's list to change with it.
- Draft #196 (spike-figma-workflow) and the open items (48, 55, 60, 61, 69, 74 and others), the spec.md C23 wording (38c Deploy review, Info 4), and 41's two [UNVERIFIED] notes (LibTomCrypt and SQLite public domain; Finnhub's `pc` split-adjusted).
- The four pitfalls the hand-off of 2026-11-25 lists (Gradle daemon crash after idle, `Select-Object -First N` on gradle output, reading a running Logcat capture, AndroidX POMs not in the cache): proposed as CLAUDE.md edits in step 9 only if you want them.
- Play Store, a limited-distribution account (C17: ADB only).

## 4. Analysis (2026-11-25, by reading; nothing built, nothing read from the phone)
- From the records (40 step 7, 41 step 9), not read from the phone today: your app `app.ghghorg.atsassets`, uid 10455, versionCode 50, `1.0.0`, schema 14; ATS Dev `app.ghghorg.atsassets.debug`, uid 10448, versionCode 51, `1.0.0-dev.51`, schema 15. So this release takes versionCode 52 (plan/release.md step 2: higher than every build installed, ATS Dev's included). Step 7 reads both with `dumpsys` before the install.
- Version: 1.1.0. CLAUDE.md section 4: CR5 is new features and a database schema change the app migrates, both MINOR; nothing in (a) to (e) of MAJOR: the backup format stays 1 (core BackupFile.kt:45), Android's minSdk and the signing key are unchanged, existing figures keep their precision, no feature is removed.
- Since v1.0.0 (362f33f), `git log --first-parent v1.0.0..main`: #231, #232 (40's records), #233 (CR5 intent), #234 (spec.md v8, wireframes), #235 (iteration 41, the app code), #239 (41's records). Step 2 reads them as the Deploy review.
- Schema: AtsDatabase.kt:607 `version = 15` (1.0.0: 14), AutoMigration(14, 15), one nullable column `instruments.pricePreviousClose`. plan/release.md section 3 asks for a migration test from 14: 41 has two (MigrationFrom14Test by hand from 14.json, MigrationFrom14HelperTest with Room's MigrationTestHelper), both green on #235 and on main. The migration also ran on the phone, on ATS Dev 50 to 51 (debug code, test data; 41 step 9).
- R8 and keep rules: `git diff --stat v1.0.0 main` shows no change under app/src/main/keepRules and none to the build type settings. app/build.gradle.kts changed in three places: versionCode 51, the schemas as androidTest assets, and the `RuntimeGroups` task for LicenceListTest. gradle/libs.versions.toml added room3-testing, used only by `androidTestImplementation`. So CLAUDE.md's probe-build rule (after changing R8 settings or keep rules) does not apply by its wording; decision 4 asks whether to rehearse anyway.
- The rollback target is v1.0.0 (versionCode 50): ats-1.0.0.apk, kept by you next to the key's backups (40 step 5), and the backup of step 6. After the raise, 1.0.0 cannot open the schema 15 database (Room refuses a downgrade, and Android refuses `install -r` of a lower versionCode anyway), so a rollback is plan/release.md section 4's uninstall, install and restore. Entries made on 1.1.0 after step 6's backup would be lost (C22).
- mapping.txt will differ from 1.0.0's (D796D74D…9436): the app code changed. Unlike 40 (13.2, finding 3), a different hash is expected here.
- Your data: 40 decision 0 says use with real data starts after 1.0.0. If your real records are in the app now, step 6's backup is the first backup of them that a release depends on.

## 5. Files
- app/build.gradle.kts: versionName `1.1.0`, versionCode 52, and the comments' example of the development app's name if one names it.
- plan/release.md section 3: "today 14" becomes 15, and the migration-test rule's next form (a later raise gets tests from 14 and from 15, the released versions).
- plan/open-items.md: open item 73, one more occurrence (#239, screen-tests run 45525704258, attempt 1: `LockAndLayerTest.layersAndMenu_keepTheFocusAndTakeInput`, "The Rename layer was still open 15 s after Save; window focused: true"; green on the re-run of shard 1, your A).
- CLAUDE.md: only what step 9 finds, and the hand-off's pitfalls if you want them (section 3).
- Documents: this file; plan.md (section 2's list `01-... to 42-...`, row 42 in sections 4 and 11, the change log).
- Outside the repo, never committed: the downloaded artifact, the signed APK, mapping.txt, the backup.

## 6. Order of work
Each step that changes files ends with spotlessCheck, build, tests and lint, with --rerun-tasks, and the report. Then I stop. Steps 4 to 8 are on your PC and phone, and each command that installs, tags, releases or builds the release asks (the release hook).
1. This file; plan.md's lines; open item 73's new occurrence.
2. The Deploy review since v1.0.0 (plan/release.md step 1): 41's Deploy review (its 13.1) and the records of #231 to #239, the commits `v1.0.0..main`, the schema and backup format versions in the code, what a 1.1.0 backup holds against 1.0.0's reader (the previous close is not in a backup, my reading of 41 to be checked in BackupFile.kt), the release build's settings and keep rules, release.yml unchanged since 1.0.0. Findings by severity, each with how it is handled. Written into section 13.
3. Version raise: versionName `1.1.0`, versionCode 52; plan/release.md section 3; the version check as a dry run in Git Bash (`bash .github/scripts/release-version-check.sh`, expecting "versionCode 52 is higher than v1.0.0's 50"). Pull request 1 and its merge (you).
4. The tag, by you, on main's merge commit of step 3 (plan/release.md step 3), with decision 2's message: `git tag -s v1.1.0 <merge commit> -m "…"`, `git tag -v v1.1.0`, `git push origin v1.1.0`. release.yml runs; I watch it, download `release-v1.1.0` to the scratchpad and you copy it to your own folder; I record the APK's and mapping.txt's SHA-256.
5. Signing, by you, in your terminal (plan/release.md step 5); then I run `apksigner verify --print-certs -Werr`, `zipalign -c`, aapt2 and `Get-FileHash` (no password). The certificate SHA-256 must equal 8115d618…cb01; otherwise the step stops. Every entry outside META-INF compared with release.yml's unsigned APK, as 40.
6. Backup on the phone with 1.0.0 (plan/release.md step 6): you note the Cash balance and each portfolio's cost on Overall; More, Back up now, Save. With the schema raise this backup is kept until the next release (plan/release.md section 3).
7. Install and smoke check: `adb devices`; `dumpsys` before (versionCode 50, uid 10455); `adb install -r` of the signed APK (AC-35); `dumpsys` after (versionCode 52, `1.1.0`, uid 10455 and the first install time kept, no DEBUGGABLE); on the phone: the app opens your data after the migration, with no "Your data cannot be read" screen, then the smoke check (AC-64: unlock; the figures of step 6; a holding opens; a price update; a backup export); a Logcat capture by uid during it, read for crashes and for any amount, ticker, key or URL (spec section 9). If the unreadable-data screen appears: stop, enter nothing, and decide on plan/release.md section 4's rollback with you.
8. The record (AC-63): the notes from plan/release.md section 5 with "Schema version: 15 (changed from 14)" and "Backup format version: 1 (unchanged)", plus decision 0's lines, shown to you first (section 12); `gh release create v1.1.0 --verify-tag --title "<decision 1>" --notes-file <file>` with decision 3's flags, by you or once at your word.
9. ATS Dev (decision 5); the Deploy review of 42, findings by severity; CLAUDE.md as section 5 says; pull request 2 with the records of steps 4 to 9 (CLAUDE.md: a release needs two pull requests).

## 7. Stubs
None.

## 8. Risks
1. The migration 14 to 15 meets your data in the release for the first time. Mitigation: two migration tests from 14 (41), the migration seen on the phone in ATS Dev 50 to 51, the certificate check in step 5 before any install, the backup and the noted figures of step 6, and the rollback of plan/release.md section 4 (rehearsed in 38b, AC-66). A refused install stops the step; nothing is uninstalled to make it fit. Decision 4 adds a rehearsal with the release code if you want one.
2. A rollback after the install is not `install -r` of 1.0.0: it uninstalls (deleting the app's data on the phone), installs 1.0.0 and restores step 6's backup; entries made on 1.1.0 after it are lost (C22). The backup format is unchanged, so step 2 checks whether a backup taken on 1.1.0 would also restore into 1.0.0.
3. The release build's libraries: About's licence list was checked against them in 41 (LicenceListTest). Merging a Dependabot pull request before step 4 would change them; section 3 keeps them out.
4. The record is public: the summary names features only, never an amount, ticker, quantity, note or portfolio name (spec section 9).
5. The local build's intermittent failures (open item 60, the Gradle daemon crash after idle among them) and the screen tests' open item 73 may need a re-run, unchanged, each with your yes. The APK you install comes from CI, not this PC.

## 9. Proof
- Step 3: full run green; the dry run prints "versionCode 52 is higher than v1.0.0's 50"; the pull request's checks green (CI with its release build and class check, the screen tests, CodeQL).
- Step 4: release.yml green on the tag; its log shows "Tag v1.1.0 equals v<versionName>; versionCode 52." and "versionCode 52 is higher than v1.0.0's 50."; the class check finds 0 classes that cannot load; the artifact holds the APK and mapping.txt.
- Step 5: `apksigner verify` passes; the certificate SHA-256 is 8115d618…cb01; aapt2 shows `app.ghghorg.atsassets`, versionCode 52, `1.1.0`, not debuggable; the signed APK's entries equal CI's.
- Step 7: the install says Success; `dumpsys` shows versionCode 52, uid 10455, the first install time kept; no unreadable-data screen; AC-64's five points; Logcat clean of data.
- Step 8: the GitHub Release `v1.1.0` holds every item of AC-63, the schema line among them.
- Standing checks: no screen changes in 42; AC-35 and AC-64 on your app are this iteration's subject.

## 10. Research (before the step that needs it; each fetch or search asks first)
- None planned. Everything is read in the repo, with gh, or on this PC.

## 11. Decisions needed from you (Build and Deploy)
Asked one at a time after you approved this file; all settled 2026-11-25.
0. What the record adds above plan/release.md section 5's lines (settled 2026-11-25: your A): (A, chosen) a summary of four to six bullets of what 1.1.0 adds (the 1-day gain/loss, Investments in the net worth card, Holding detail on Lots with each lot's value, About with the version and licences), written from spec.md and shown to you before use, plus one line: "The database moves from schema 14 to 15 on install; keep the backup taken on 1.0.0 until the next release." (B) the template's lines only. (C) A plus known limits (41's [UNVERIFIED] notes).
1. The GitHub Release's title (settled 2026-11-25: your A): (A, chosen) `1.1.0`, as `0.38.0` and `1.0.0`. (B) `1.1.0: 1-day gain/loss`.
2. The tag message (settled 2026-11-25: your A): (A, chosen) "ATS Asset System 1.1.0 (versionCode 52): the 1-day gain/loss and screen changes (CR5)." (B) "ATS Asset System 1.1.0 (versionCode 52)."
3. Publishing (settled 2026-11-25: your A): (A, chosen) a draft first (`--draft --latest`), published by you in the browser after reading it; it becomes Latest, as 1.0.0. (B) published as Latest directly.
4. A rehearsal of the upgrade with the release code before your app (settled 2026-11-25: your A): (A, chosen) none: R8 settings and keep rules are unchanged since v1.0.0 (section 4), the migration was tested twice and ran on ATS Dev on the phone, CI's class check runs on the release build, and step 6's backup and the rollback cover the rest. (B) a probe rehearsal as 38b's: 1.0.0's release code and then 1.1.0's under a `.probe` id signed with the debug key, test data restored into the first and `adb install -r` of the second, checked on the phone before step 6. For you: B checks the exact upgrade path in release code before your data meets it, for about an hour more and a temporary build setting that is removed again.
5. ATS Dev after the release (settled 2026-11-25: your A): (A, chosen) updated to `1.1.0-dev.52` (debug build of step 3's merge commit, `adb install -r` over dev.51, checked on the phone), so both apps run the same code, as after 40. (B) left at `1.0.0-dev.51` until the next iteration that changes app code.

## 12. Wording to approve (D9)
- The release notes with decision 0's lines (step 8) and the tag message (decision 2), shown to you before use.

## 13. Status and notes
- Draft 2026-11-25; approved the same day (your A); decisions 0 to 5 settled the same day (your A each).

### 13.1 Steps
- Step 1 (2026-11-25): this file; plan.md (section 2's list, rows 42 in sections 4 and 11, the change log); plan/open-items.md, open item 73's occurrence on #239 (screen-tests run 45525704258, attempt 1, green on the re-run of shard 1, your A). Only documents changed. Full run with --rerun-tasks green in 2m 22s at the first try (spotlessCheck, build, tests, lint): 1,556 unit tests, 0 failures, 1 skipped; lint the same 7 findings (report 10:08:52).

### 13.2 Deploy review since v1.0.0 (step 2)

### 13.3 Deploy review of 42 (step 9)

### 13.4 Pull requests and merges

## 14. Departures from the plan
None yet.
