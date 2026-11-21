# plan/iterations/38c-first-release.md

Stage: Build. Status: approved 2026-11-22; decisions 0 to 4 settled the same day (your A, A, A, B, A), decision 5 in step 2 (your A; section 11). Branch iter-38c-first-release, from main (26b93cd).
Source: intent/intent-cr-04-release-process.md (CR4, final); spec/spec.md v7. CR4's third part, after 38a (the development app) and 38b (the release path); plan.md section 4.

## 1. Outcome
Your real data lives in the signed release build `0.38.0` (versionCode 49), and nothing debuggable holds it any more:
- The release key exists, outside the repo, with at least two backups (C21).
- The tag `v0.38.0` is on main's merge commit, release.yml built and checked it, and you signed the APK it built.
- Your data moved once from the debug build (45) to the release: backup, uninstall debug, install release, restore (AC-35), and the smoke check passed (AC-64).
- The GitHub Release `v0.38.0` is the record (AC-63).

## 2. Spec refs
- C17 (the ADB route), C21 (the release key is forever), C22 (rollback), C23 (release-only defects).
- AC-35 (the release installs over ADB, is not debuggable; the move from debug), AC-36 (stored data unreadable, after the move), AC-63 (the release record), AC-64 (smoke check).
- Section 8, "Releases and the format version": the backup taken before the install and kept until the next release.

## 3. Out of scope
- Any change to what the app does or shows. The app's code is unchanged; only versionCode rises.
- A second release, and with it the first check of `fetch-depth: 0` and an earlier tag (38b Deploy finding 3).
- Play Store, a limited-distribution account (C17: ADB only).

## 4. Analysis (2026-11-22, by reading; nothing run)
- Your app on the phone is the debug build 45 under `app.ghghorg.atsassets` (uid 10454), signed with this PC's debug key. The release has the same id and another signature, so Android refuses it over 45: the move is backup, uninstall, install, restore (CR4; AC-35).
- No APK of 45 was kept on the PC. If the release fails on the phone after the uninstall, the way back needs 45 (decision 3). 45 is commit 748439d (35 step 7): the phone's lastUpdateTime, 2026-11-17 13:31:40, is the 35 file's install time of 748439d.
- The release key and its passwords must never pass through Claude Code: `keytool` and `apksigner sign` ask for passwords at a prompt, which Claude Code's shell cannot answer (stdin is empty), and CLAUDE.md section 8 forbids reading *.jks and *.keystore. You run those two commands in your own terminal; I run only commands without a password (`apksigner verify`, `aapt2`, `Get-FileHash`).
- release.yml (38b) builds the unsigned APK on the tag and keeps it with mapping.txt. Whether the unsigned APK needs `zipalign` before `apksigner sign` is not known; it is researched first (section 10).
- AC-36 on a release build: `run-as` does not work for an app that is not debuggable (from memory, to check on the phone), so the stored file can't be copied the way 02a did it. The app's code and its encryption are the same as the debug build's that 02a inspected (decision 4).
- spec.md: C22's downgrade refusal ("[UNVERIFIED, from memory]") was seen in 38b step 5, and C23's "[UNVERIFIED] The release build's behaviour under R8" was checked once in 38b step 2 (38b Deploy finding 4). The spec changes only with your acceptance (step 3).
- versionCode: 48 was installed in 38b's rehearsal, so this release takes 49 (plan/release.md step 2; 38b Deploy finding 5). versionName stays 0.38.0 (38a decision 2d).

## 5. Files
- app/build.gradle.kts: versionCode 49.
- spec/spec.md: C22 and C23, the checked parts (your acceptance; step 3).
- plan/release.md: the key, zipalign and apksigner commands as researched and run.
- Documents: this file; plan.md (section 2's list, rows 38c in sections 4 and 11, the change log); plan/open-items.md if anything opens; CLAUDE.md (the release key's rules, the commands that worked).
- Outside the repo, never committed: the release key and its backups; the signed APK and mapping.txt; the copy of your backup and of the 45 APK.

## 6. Order of work
Each step that changes files ends with spotlessCheck, build, tests and lint, with --rerun-tasks, and the report. Then I stop. Steps 4 to 8 are on your PC and phone, and each command asks.
1. This file, plan.md's lines.
2. Research (each fetch shown to you and asks first): `keytool -genkeypair` for an Android release key (algorithm, key size, validity); `apksigner sign` and `apksigner verify`; whether an AGP unsigned release APK needs `zipalign` first. Written into plan/release.md step 5.
3. versionCode 49; spec.md C22 and C23 (the text shown to you, your acceptance); pull request and merge (you, or once at your word).
4. The key, by you, in your terminal, with the command from step 2: a keystore file outside the repo (decision 0), its passwords in your password manager, two backups. You tell me "key created, two backups"; I record only that, never a path, password or alias beyond what you choose to give.
5. The tag, by you, on main's merge commit of step 3 (plan/release.md step 3): `git tag -s v0.38.0`, `git tag -v v0.38.0`, `git push origin v0.38.0`. release.yml runs; I watch it, download `release-v0.38.0` outside the repo (decision 1) and record both SHA-256.
6. Signing, by you, in your terminal (plan/release.md step 5, decision 1); then I run `apksigner verify --print-certs` (no password) and record the certificate's SHA-256 and the signed APK's SHA-256.
7. The move, on the phone (AC-35, AC-36, AC-64), each command asking:
   a. In your app (45): note the Cash balance and each portfolio's cost on Overall, and the number of transactions; More, Back up now, Save.
   b. The backup copied to the PC outside the repo (`adb pull`); its size and SHA-256 recorded. The way back is a rebuild of 748439d if needed (decision 3).
   c. `adb uninstall app.ghghorg.atsassets`; `adb install` of the signed release.
   d. On the release: lock set up, the price key entered, the backup of (a) restored; the figures and the count match (AC-35); the smoke check (AC-64).
   e. Not debuggable: aapt2 on the APK, `run-as app.ghghorg.atsassets` refused (decision 4).
8. The record, by you: the GitHub Release `v0.38.0` with the notes I draft from plan/release.md section 5 (AC-63), shown to you first.
9. The Deploy review of 38c, findings by severity; CLAUDE.md.

## 7. Stubs
None.

## 8. Risks
1. Your real data during the move: after the uninstall, the data on the phone is gone until the restore works. Mitigation: two copies of the backup (phone and PC), a rebuild of 45's commit 748439d to go back (decision 3; that it builds today is not checked), the figures noted before. A restore failure stops the step; nothing else is tried without your yes.
2. The key (C21): lost, every later release needs the move again (backup, uninstall, install, restore); leaked, someone could sign an update for your phone. Two backups, passwords in your password manager, never in the repo, CI or this session.
3. The release has not run with your real data's size and history; the probe ran test data. The smoke check (AC-64) is the check; a rebuild of 748439d and the backup are the way back (decision 3).
4. Android or Play Protect may warn when installing an app signed with a new key over ADB (C17, [UNVERIFIED]). A warning stops the step until you decide.
5. The local build's intermittent failures (open item 60) apply to step 3's full run; the APK you install comes from CI, not this PC.

## 9. Proof
- Step 3: full run green; the pull request's checks green, including release.yml's dry run if a release file changes (none planned).
- Step 5: release.yml green on the tag: the version check names v0.38.0 and versionCode 49, the class check 0, the artifact holds the APK and mapping.txt.
- Step 6: `apksigner verify` passes; the certificate is yours (its SHA-256 recorded), not the debug key's (e28a2c41…fc92f).
- Step 7: the figures and count equal those of (a); AC-64's five points; `run-as` refused; aapt2 shows no debuggable flag.
- Step 8: the GitHub Release holds every item of AC-63.
- Standing checks: no screen changes; AC-35 and AC-64 on your app are this iteration's subject.

## 10. Research (before the step that needs it; each fetch or search asks first)
- keytool's `-genkeypair` for an Android app key: the developer.android.com app-signing page (developer.android.com pages may not load for my tools: CLAUDE.md section 6; then I ask you to paste it), else Oracle's keytool reference.
- apksigner's `sign` and `verify`, and zipalign's place before signing: developer.android.com's apksigner and zipalign pages, or `apksigner help sign` from build-tools 36.0.0 on this PC (no fetch).

## 11. Decisions needed from you (Build)
0. How the key is created (settled 2026-11-22: your A; Kleopatra asked about: it manages OpenPGP keys, which can't sign an APK, so it was offered for encrypting the backups (A+K) and not chosen): (A, chosen) `keytool -genkeypair` in your terminal, the command written from step 2's research, the keystore file in a folder you choose outside the repo, the passwords in your password manager, two plain backups. Not chosen: (A+K) as A, both backups encrypted with Kleopatra to your own OpenPGP key; (B) Android Studio's "Generate Signed App Bundle or APK" wizard, which creates the key and signs an APK Android Studio builds itself, not CI's (against CR4 step 4 and 5).
1. Which APK is signed (settled 2026-11-22: your A): (A, chosen) the one release.yml built for the tag (`release-v0.38.0`), downloaded outside the repo: the same build CI checked, and mapping.txt from the same run. Not chosen: (B) one built on this PC from the tagged commit: the build that is signed is not the one CI checked.
2. Who runs which command (settled 2026-11-22: your A): (A, chosen) you run the two commands that need a password (`keytool`, `apksigner sign`) in your own terminal; I prepare them and run everything without a password, each asking. Not chosen: (B) I run them in a way that reads the passwords from a file: they would pass through this session, against CLAUDE.md section 8.
3. The way back if the release fails after the uninstall (C22 names no rollback target for a first release; settled 2026-11-22: your B): (B, chosen) rebuild a debug build from 45's commit if needed, and install it (`adb install`), then restore the backup. 45's commit is 748439d: the phone shows versionCode 45, versionName 1.0, debuggable, lastUpdateTime 2026-11-17 13:31:40 (dumpsys, 2026-11-22), the time the 35 file records for "both apps at 748439d, installed 13:31:40". Not checked: that 748439d still builds on this PC today, and the rebuilt APK is not the same bytes as the installed one (it is signed with the same debug key on this PC). Not chosen: (A) 45's installed APK pulled from the phone before the uninstall and kept in a durable folder outside the repo; (C) none: the backup only, restored later into a fixed release.
4. AC-36 ("inspecting the app's stored data file shows no readable ticker, amount or note") on the release (settled 2026-11-22: your A): (A, chosen) the release is not debuggable, so `run-as` is refused and the file can't be copied without root; that refusal is checked, and the file content check stands from 02a (the same code and encryption, now with the release's key in the phone's Keystore). Not chosen: (B) a debuggable copy of the release to copy the file: it would not be the release.
5. The release key's values (step 2; settled 2026-11-22: your A): (A, chosen) RSA 4096-bit, validity 10,950 days (30 years; the app-signing page: "Your key should be valid for at least 25 years"), keystore type PKCS12 set explicitly, `-dname "CN=ATS Asset System"` (the certificate's name is in every APK and the record shows its fingerprint, so no personal name, email or place), alias `ats-release`, the file named `ats-release.keystore` (covered by .gitignore and CLAUDE.md's never-read rule). The page states no algorithm or key size: 4096 is my choice. Not chosen: (B) RSA 2048 (Android Studio's wizard default, from memory); (C) an EC key (P-256).

## 12. Wording to approve (D9)
- The tag message and the release notes (step 5 and 8), shown to you before use.

## 13. Status and notes
- Draft 2026-11-22; approved the same day (your A).

### 13.1 Steps
- Step 1 (2026-11-22): this file; plan.md (section 2's list, rows 38c in sections 4 and 11, the change log). Only documents changed. Your app's version read from the phone for decision 3 (dumpsys, read only). Full run with --rerun-tasks green in 2m 16s at the first try.
- Step 2 (2026-11-22), research:
  - On this PC, no fetch: apksigner 0.9 (build-tools 36.0.0), `sign --help` and `verify --help`. Signing aligns the APK itself: "--alignment-preserved … the default for this setting is false. When this value is false, the value provided to --lib-page-alignment will be used to page align native library files and all other files will be aligned to 4 bytes", the page alignment's default 16384. Without `--ks-pass` it "will prompt for password via console" (decision 2). `--out` writes a new file. `--debuggable-apk-permitted`: "By default, signing debuggable APKs is permitted"; set to false here. `verify --print-certs` shows the certificate, `-Werr` treats warnings as errors.
  - zipalign (build-tools 36.0.0) `-c -P 16 -v 4` on #218's dry-run unsigned APK: "Verification successful": CI's APK is aligned already; no zipalign step before signing, a `zipalign -c` check after it.
  - keytool (Java 25.0.3, JAVA_HOME) `-genkeypair -help`: `-alias`, `-keyalg`, `-keysize`, `-validity`, `-dname`, `-keystore`, `-storetype`; without `-storepass` and `-keypass` it prompts.
  - developer.android.com/studio/publish/app-signing (your yes; the web-fetch agent): "Your key should be valid for at least 25 years"; "If you lose or misplace your key, you will not be able to publish updates to your existing app. You cannot regenerate a previously generated key." No algorithm, key size or keystore type stated, nothing on zipalign. Decision 5.
  - Written into plan/release.md step 5: the key's creation, signing and the checks. Not yet run: whether apksigner takes `--debuggable-apk-permitted false` in that form (the help shows the option, not its value syntax); step 6 shows it.
  - Full run: the first failed after 49s in `:core:compileTestFixturesKotlin` and Room's KSP ("references a type that is not present"; open item 60, only documents changed). The same command re-run unchanged: green in 2m 32s; 1,512 unit tests (1 skipped), 178 screen tests compiled, lint the same 10 (report 02:24:47).

## 14. Departures from the plan
None yet.
