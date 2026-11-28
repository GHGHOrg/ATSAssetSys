# plan/iterations/44b-release-1.3.0.md

Stage: Build. Status: approved 2026-11-28 (your A), decisions 0 to 4 (your A); step 3 built: versionName 1.3.0, versionCode 56, the version check's dry run passes ("versionCode 56 is higher than v1.2.0's 53"). Branch docs/iter-44a-merged, from main (b021aad), shared with 44a's merge record (your A, 2026-11-28: one pull request for the record, this file and the version raise).
Source: plan.md section 4 (row 44b); intent/intent-cr-07-design-system.md (CR7, final) and iteration 44a (CR7 built, merged as b021aad); spec/spec.md v10; plan/release.md (a release's steps); CLAUDE.md section 4 (version rules after 1.0.0). No intent file: this iteration changes no app behaviour, only versionName and versionCode (as 40 and 42).

## 1. Outcome
Your app is the signed release `1.3.0` (versionCode 56), installed over `1.2.0` with your data kept, and the GitHub Release `v1.3.0` records it:
- The tag `v1.3.0` is on main's merge commit of the version raise; release.yml built and checked it; you signed the APK it built with the release key (C21: the same certificate as 0.38.0 to 1.2.0).
- `adb install -r` over 1.2.0 kept your data (AC-35), and the smoke check passed (AC-64).
- The record says the schema version (15) and the backup format version (1) are unchanged (AC-63).

## 2. Spec refs
- C17 (the ADB route), C21 (the release key is forever), C22 (rollback).
- Section 8, "Releases and the format version": no raise this time.
- AC-35, AC-63, AC-64.
- What 1.3.0 adds (for the record's summary only; built and checked in 44a): C27 to C29, section 6 (Visual design, Date fields, every view), K9, AC-40, AC-61, AC-62, AC-70, AC-73.

## 3. Out of scope
- Any change to what the app does or shows. Only versionName and versionCode change in the app.
- The open Dependabot pull requests (AGP, compose-bom, kotlinx-serialization-json): after this release; a library change would change the release build and About's list.
- Open items 48 and 76 to 79 (44a's Deploy review).
- Play Store (C17: ADB only).

## 4. Analysis (2026-11-28, by reading; nothing read from your app today)
- Builds installed: your app `app.ghghorg.atsassets` versionCode 53 (`1.2.0`, uid 10455; 43's record); ATS Dev `app.ghghorg.atsassets.debug` versionCode 55 (`1.2.0-dev.55`, uid 10448; read 2026-11-28 in 44a step 9). So this release takes versionCode 56 (plan/release.md step 2: higher than every build installed, ATS Dev's included). Step 7 reads both with `dumpsys` before the install.
- Version: 1.3.0. CLAUDE.md section 4: CR7 is a change of style and a new feature (the date picker, About's two rows), both MINOR; nothing in (a) to (e) of MAJOR: the backup format stays 1 (core BackupFile.kt `FORMAT_VERSION = 1`), the schema stays 15 (AtsDatabase.kt `version = 15`), minSdk and the signing key are unchanged, every figure keeps its precision, no feature is removed.
- Since v1.2.0 (1ec1e79), `git log --first-parent v1.2.0..main`: #246 and #247 (43's records), #248 (CR7 intent), #249 and #250 (spec v10 and its date-picker amendment), #251 (iteration 44a, the app code).
- `git diff v1.2.0 main`: nothing in core/, app/schemas/, gradle/libs.versions.toml, app/src/main/keepRules/, release.yml or its scripts; app/build.gradle.kts adds the token generator task and versionCode 55. The release build's settings and keep rules are unchanged, so CLAUDE.md's probe-build rule (after changing R8 settings or keep rules) does not apply by its wording. The release APK is built and class-checked on every CI run of main (38b decisions 2 and 6), b021aad's included.
- The rollback target is v1.2.0 (versionCode 53): ats-1.2.0.apk, kept by you next to the key's backups, and step 6's backup. No schema or format raise, so 1.2.0 can still read a 1.3.0 backup.
- mapping.txt will differ from 1.2.0's: the app code changed.

## 5. Files
- app/build.gradle.kts: versionName `1.3.0`, versionCode 56.
- Documents: this file; plan.md (section 2's list of iteration files, row 44b in sections 4 and 11, the change log); the 44a file's and plan.md's merge record of 44a, in the same pull request.
- Outside the repo, never committed: the downloaded artifact, the signed APK, mapping.txt, the backup.

## 6. Order of work
Each step that changes files ends with spotlessCheck, build, tests and lint, with --rerun-tasks, and the report. Then I stop. Steps 4 to 8 are on your PC and phone, and each command that installs, tags, releases or builds the release asks (the release hook).
1. This file and plan.md's lines (with 44a's merge record).
2. The Deploy review since v1.2.0 (plan/release.md step 1): 44a's (its section 14.2) and the records of #246 to #251; no new review of the code. Findings by severity, written into section 13.
3. Version raise: versionName `1.3.0`, versionCode 56; the version check as a dry run in Git Bash (`bash .github/scripts/release-version-check.sh`, expecting versionCode 56 higher than v1.2.0's 53). Pull request 1 (with 44a's merge record) and its merge (you).
4. The tag, by you, on main's merge commit of step 3 (plan/release.md step 3), with decision 2's message: `git tag -s v1.3.0 <merge commit> -m "…"`, `git tag -v v1.3.0`, `git push origin v1.3.0`. release.yml runs; I watch it, download `release-v1.3.0` to the scratchpad and you copy it to your own folder; I record the APK's and mapping.txt's SHA-256.
5. Signing, by you, in your terminal (plan/release.md step 5); then I run `apksigner verify --print-certs -Werr`, `zipalign -c`, aapt2 and `Get-FileHash` (no password). The certificate SHA-256 must equal 8115d618…cb01; otherwise the step stops.
6. Backup on the phone with 1.2.0 (plan/release.md step 6): you note the Cash balance and each portfolio's cost on Overall; More, Back up now, Save.
7. Install and smoke check: `adb devices`; `dumpsys` before (versionCode 53, uid 10455); `adb install -r` of the signed APK (AC-35); `dumpsys` after (versionCode 56, `1.3.0`, uid 10455 kept, no DEBUGGABLE); on the phone the smoke check (AC-64: unlock; the figures of step 6; a holding opens; a price update; a backup export); a Logcat capture by uid, read for crashes and for any amount, ticker, key or URL (spec section 9). A refused install or a crash stops the step; then plan/release.md section 4's rollback with you.
8. The record (AC-63): the notes of plan/release.md section 5 with "Schema version: 15 (unchanged)" and "Backup format version: 1 (unchanged)", shown to you first; `gh release create v1.3.0 --verify-tag --title "1.3.0" --notes-file <file>`, by you or once at your word.
9. ATS Dev `1.3.0-dev.56` installed over dev.55 (decision 3); the Deploy review of 44b; pull request 2 with the records of steps 4 to 9 (CLAUDE.md: a release needs two pull requests).

## 7. Stubs
None.

## 8. Risks
1. The look changes on every screen of your app at once. Mitigation: 44a's screen tests (260 of 260 on main's last run) and the phone check of ATS Dev in light and dark (44a section 14), the certificate check in step 5 before any install, the backup and figures of step 6, and the rollback of plan/release.md section 4.
2. The release code (R8 on the libraries) runs the new design code for the first time on the phone. Mitigation: the release class check on every CI run; the smoke check opens several screens and a Logcat capture is read for crashes. Decision 4 asks whether to rehearse with the release code first.

## 9. Proof
- Commands: spotlessCheck, `.\gradlew.bat :core:assemble :app:assembleDebug`, the test command of CLAUDE.md section 3, `:app:lintDebug`, with --rerun-tasks; the version check's dry run.
- The release run's artifact, the signed APK's certificate and hashes, `dumpsys` before and after, the smoke check, the Logcat capture.

## 10. Research
None.

## 11. Decisions (Build), each with my leaning for you to accept or change
- 0. Version 1.3.0, versionCode 56 (section 4).
- 1. The GitHub Release's title "1.3.0", as 1.2.0's; its notes in 1.2.0's form: what 1.3.0 adds in plain words (the new look in light and dark, the date picker on every date field, the font and icon licences in About), then the release record.
- 2. The tag message: "ATS Asset System 1.3.0 (versionCode 56): the Corporate Design System (CR7)."
- 3. ATS Dev updated to `1.3.0-dev.56` after the release (step 9), so it carries the same versionName.
- 4. No rehearsal with the release code before the install (no probe build): the keep rules and R8 settings are unchanged, the class check is green on main, and step 6's backup and section 4's rollback cover a failure.

## 12. Wording to approve (D9)
The release notes' "What 1.3.0 adds" lines, shown in step 8 before publishing. No app strings change.

## 13. Deploy review
Step 2, since v1.2.0 (2026-11-28), by reading the records, not the code again: #246 and #247 (43's records: documents only), #248 to #250 (CR7's intent and spec v10: documents only), #251 (44a: the app code; reviewed in the 44a file, section 14.2, and checked on the phone in its section 14). Schema 15 and backup format 1 unchanged; no dependency, keep-rule or release-workflow change; main's CI on b021aad green, the release build and class check included.
- High: none. The one crash found in 44a (a long Transaction history) was fixed test first before the merge (TallCardShadowTest, 5d89ca6).
- Medium: open items 76 (the loader under reduced motion, against D-35), 77 (the drawing departures recorded only in the 44a file) and 48 (the entry screens' refusal messages): left open for an iteration after this release (your B, 44a).
- Low: open items 78 (dp defaults and font weights in code, K9) and 79 (two unused imports): left open (your B, 44a).
None blocks the release: none changes a figure, a stored value or a backup, and each was seen on the phone or in the code.

Step 9: written after the release.
