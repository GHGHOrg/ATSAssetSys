# plan/iterations/40-release-1.0.0.md

Stage: Build. Status: Approved 2026-11-24 (your yes); step 1 built. Decisions 0 to 4 settled the same day (your A, B, A, A, A; section 11). Branch iter-40-release-1.0.0, from main (e5253a2).
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7; plan/release.md (a release's steps); CLAUDE.md section 4 (version rules; 38a decision 2d: "1.0.0 comes when you call the app productive"). No intent file: the app's code does not change (your decision 2026-11-24, after the About screen was dropped).

## 1. Outcome
Your app is the signed release `1.0.0` (versionCode 50), installed over `0.38.0` with your data kept, and the GitHub Release `v1.0.0` records it as the first productive release:
- The tag `v1.0.0` is on main's merge commit, release.yml built and checked it, and you signed the APK it built with the release key (C21: the same certificate as 0.38.0).
- `adb install -r` over 0.38.0 kept your data (AC-35's second half, the first time), and the smoke check passed (AC-64).
- From 1.0.0 on, CLAUDE.md section 4's version rules apply as written: a milestone or a breaking change raises MAJOR.
- After 1.0.0, use of the app with real data starts (decision 0). The data in your app today is not your real record.

## 2. Spec refs
- C17 (the ADB route), C21 (the release key is forever), C22 (rollback).
- AC-35 (a release installs over the previous one with `adb install -r`, not debuggable), AC-63 (the release record), AC-64 (smoke check).
- Section 8, "Releases and the format version": the backup taken before the install and kept until the next release.

## 3. Out of scope
- Any change to what the app does or shows. Only versionName and versionCode change. The About screen was asked about and dropped (your decision 2026-11-24): it would need an intent file.
- Open items, among them the spec.md C23 wording (38c Deploy review, Info 4), which stays for your acceptance.
- ATS Dev: it stays `0.38.0-dev.49` until a later iteration installs a development build.
- How your real records start after 1.0.0 (clearing the app's current data, a restore, entry by hand): your own step after this iteration, asked about then.
- Play Store, a limited-distribution account (C17: ADB only).

## 4. Analysis (2026-11-24, by reading and `dumpsys`; nothing built)
- On the phone (`dumpsys package`, read only): your app `app.ghghorg.atsassets`, uid 10455, versionCode 49, `0.38.0`, not debuggable; ATS Dev `app.ghghorg.atsassets.debug`, uid 10448, versionCode 49, `0.38.0-dev.49`, debuggable. So this release takes versionCode 50 (plan/release.md step 2).
- Since v0.38.0 (7ac20e2): iteration 39 (the build on Gradle 9.9.2, Spotless 8.12.0, KSP 2.3.14, kotlinx-coroutines-test 1.11.0; 39 found the debug and release dex the same byte for byte) and 39a (test code and documents). No app code changed. The schema version (14) and the backup format version (1) are expected unchanged; step 2 checks them in the code.
- This is the first release after an earlier tag: release-version-check.sh compares versionCode with `git show v0.38.0:app/build.gradle.kts` (49). That path has never run on a tag (38b Deploy finding 3; 38c section 3). Step 2 reads release.yml's checkout for the tags it fetches; step 3 runs the check as a dry run in Git Bash.
- The signed APK must carry the release key's certificate, SHA-256 8115d618f042c3dd9f34ee7e19c91ef8c2eec8507ac0b9a915d86c3b1182cb01 (C21). A different key would make Android refuse the install over 0.38.0.
- 38c's file (section 1) calls the data moved into 0.38.0 "your real data". By your answer of 2026-11-24 (decision 0) it is not your real record; use with real data starts after 1.0.0. 38c's file stays as merged; this file records the correction.
- The rollback target is v0.38.0 (versionCode 49): ats-0.38.0.apk, kept by you next to the key's backups (38c step 6), and the backup taken in step 6 (plan/release.md section 4).
- Whether `gh release create` takes `--draft` and `--latest` together is not checked; step 2 reads `gh release create --help` on this PC (no fetch).

## 5. Files
- app/build.gradle.kts: versionName `1.0.0`, versionCode 50 (and the comment's example of the development app's name, if it names one).
- plan/release.md: the version rule after 1.0.0 (step 2's "until 1.0.0" text), and the commands as run if they differ.
- CLAUDE.md: section 4's version rules worded for after 1.0.0; the pitfalls found on the way.
- Documents: this file; plan.md (section 2's list, rows 40 in sections 4 and 11, the change log); plan/open-items.md if anything opens.
- Outside the repo, never committed: the downloaded artifact, the signed APK, mapping.txt, the backup.

## 6. Order of work
Each step that changes files ends with spotlessCheck, build, tests and lint, with --rerun-tasks, and the report. Then I stop. Steps 4 to 8 are on your PC and phone, and each command asks.
1. This file, plan.md's lines.
2. The Deploy review since v0.38.0 (plan/release.md step 1): 39's and 39a's Deploy findings, the commits `v0.38.0..main`, the schema and backup format versions in the code, release.yml's checkout, `gh release create --help`. Findings by severity, each with how it is handled. Written into section 13.
3. Version raise: versionName `1.0.0`, versionCode 50; plan/release.md's version rule; the version check as a dry run in Git Bash (`bash .github/scripts/release-version-check.sh`, expecting "versionCode 50 is higher than v0.38.0's 49"). Pull request 1 and its merge (you, or once at your word).
4. The tag, by you, on main's merge commit of step 3 (plan/release.md step 3), with decision 3's message: `git tag -s v1.0.0 <merge commit> -m "…"`, `git tag -v v1.0.0`, `git push origin v1.0.0`. release.yml runs; I watch it, download `release-v1.0.0` to the scratchpad and you copy it to your own folder (as 38c); I record both SHA-256.
5. Signing, by you, in your terminal (plan/release.md step 5); then I run `apksigner verify --print-certs -Werr`, `zipalign -c`, aapt2 and `Get-FileHash` (no password). The certificate SHA-256 must equal 8115d618…cb01; otherwise the step stops.
6. Backup on the phone with 0.38.0 (plan/release.md step 6): you note the Cash balance and each portfolio's cost on Overall; More, Back up now, Save. This backup is the rollback's and is kept until the next release.
7. Install and smoke check: `adb devices`; `adb install -r` of the signed APK (AC-35); `dumpsys` (versionCode 50, `1.0.0`, uid 10455 kept, no DEBUGGABLE); on the phone the smoke check (AC-64: unlock; the figures of step 6; a holding opens; a price update; a backup export); a Logcat capture by uid during it, read for crashes and for any amount, ticker, key or URL (spec section 9).
8. The record (AC-63): the notes from plan/release.md section 5 with decision 1's milestone line and summary, shown to you first (section 12); `gh release create v1.0.0 --title "1.0.0" --notes-file <file> --draft --latest`, by you or once at your word; you read the draft on GitHub and publish it (decision 4).
9. The Deploy review of 40, findings by severity; CLAUDE.md section 4; pull request 2 with the records of steps 4 to 9 (CLAUDE.md: a release needs two pull requests).

## 7. Stubs
None.

## 8. Risks
1. The data in your app during the install (not yet your real record, decision 0, but the smoke check compares its figures): `adb install -r` keeps it when the key matches; this has not run over your release before. Mitigation: the certificate check in step 5 before any install, the backup of step 6, the figures noted, the rollback of plan/release.md section 4 (rehearsed in 38b, AC-66). A refused install stops the step; nothing is uninstalled to make it fit.
2. The version check against an earlier tag runs on a tag for the first time (38b Deploy finding 3). If release.yml does not see v0.38.0, the check passes with "the first release" instead of comparing. Step 2 reads the checkout, step 3 runs the dry run locally, and step 4 reads the run's log for the comparison line. A red run stops the release; the tag stays and the fix is a new version (plan/release.md step 4).
3. 39's build tools: the release APK's bytes may differ from 0.38.0's although no app code changed. The class check in CI and the smoke check on the phone are the checks.
4. The record is public: the summary names features only, never an amount, ticker, quantity, note or portfolio name (spec section 9).
5. The local build's intermittent failures (open item 60) and the screen tests' open item 73 may need a re-run, unchanged, each with your yes. The APK you install comes from CI, not this PC.

## 9. Proof
- Step 3: full run green; the dry run prints the comparison with v0.38.0's 49; the pull request's checks green (CI, its release build and class check, the screen tests, CodeQL).
- Step 4: release.yml green on the tag; its log shows "Tag v1.0.0 equals v<versionName>; versionCode 50." and "versionCode 50 is higher than v0.38.0's 49."; the class check 0 classes that cannot load; the artifact holds the APK and mapping.txt.
- Step 5: `apksigner verify` passes; the certificate SHA-256 is 8115d618…cb01; aapt2 shows `app.ghghorg.atsassets`, versionCode 50, `1.0.0`, not debuggable.
- Step 7: the install says Success; `dumpsys` shows versionCode 50 and uid 10455; AC-64's five points; Logcat clean of data.
- Step 8: the GitHub Release `v1.0.0` is Latest and holds every item of AC-63, the milestone line and the summary.
- Standing checks: no screen changes; AC-35 and AC-64 on your app are this iteration's subject.

## 10. Research (before the step that needs it; each fetch or search asks first)
- None planned. `gh release create --help` and the build-tools commands are read on this PC.

## 11. Decisions needed from you (Build and Deploy)
0. What 1.0.0 requires (settled 2026-11-24: your A): (A, chosen) your word: main as it is plus the version raise; the app works as 0.38.0 did; from 1.0.0 on, CLAUDE.md section 4's rules apply as written. Added the same day (your B): use of the app with real data starts after 1.0.0; the data in your app today is not your real record, and how the real records start is your own step after this iteration. Not chosen there: (A) the data in your app stays and becomes the real record from 1.0.0 on. Not chosen: (B) chosen open items closed first; (C) a written bar for "productive", main checked against it.
1. What the record adds above plan/release.md section 5's lines (settled 2026-11-24: your B): (B, chosen) a milestone line ("1.0.0: the app is productive by the owner's decision (2026-11-24). It works as 0.38.0 did; from this release on, a breaking change raises MAJOR. Use with real data starts after this release.") and a summary of five to eight bullets of what 1.0.0 does, written from spec.md and shown to you before use. Not chosen: (A) the milestone line only; (C) B plus known limits from the open items.
2. The GitHub Release's title (settled 2026-11-24: your A): (A, chosen) `1.0.0`, as `0.38.0`. Not chosen: (B) `1.0.0: first productive release`; (C) `ATS Asset System 1.0.0`.
3. The tag message (settled 2026-11-24: your A): (A, chosen) "ATS Asset System 1.0.0 (versionCode 50): the first productive release." Not chosen: (B) "ATS Asset System 1.0.0 (versionCode 50)."; (C) the milestone line in full.
4. Publishing (settled 2026-11-24: your A): (A, chosen) a draft first (`--draft --latest`), published by you in the browser after reading it; it becomes Latest. Not chosen: (B) published as Latest directly, as 0.38.0; (C) a pre-release.

## 12. Wording to approve (D9)
- The release notes: decision 1's milestone line and summary, with plan/release.md section 5's lines (step 8), shown to you before use. The tag message is decision 3.

## 13. Status and notes
- Draft 2026-11-24; your B on "use with real data starts" added to decision 0 the same day; approved the same day (your yes).

### 13.1 Steps
- Step 1 (2026-11-24): this file; plan.md (section 2's list, rows 40 in sections 4 and 11, the change log). Only documents changed. Both apps' versions read from the phone (`dumpsys`, read only; section 4).

## 14. Departures from the plan
None yet.
