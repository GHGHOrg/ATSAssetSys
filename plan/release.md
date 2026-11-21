# plan/release.md

Stage: Deploy (used at each release). Written 2026-11-21 in 38b step 5 (38b decision 3).
Source: intent/intent-cr-04-release-process.md (CR4), spec/spec.md v7 (C17, C21, C22, C23; section 8, "Releases and the format version"; AC-63, AC-64, AC-66).
Version rules: CLAUDE.md section 4 (38a decisions 2, 2a to 2d). Commands that install, tag, release or build the release ask every time (the hook, 38b step 4).

## Contents
- [1. Who does what](#1-who-does-what)
- [2. A release's steps](#2-a-releases-steps)
- [3. Backup format and schema rule](#3-backup-format-and-schema-rule)
- [4. Rollback](#4-rollback)
- [5. Release notes template](#5-release-notes-template)
- [6. Rehearsal record (AC-66)](#6-rehearsal-record-ac-66)

## 1. Who does what
- A release happens when you decide, from any merge commit on main; it can bundle several iterations.
- Claude prepares: the Deploy review since the last release, the version raise, the tag message, the release notes, the commands.
- You authorize and do the release steps yourself: create and push the tag, sign the APK, install it, publish the record.
- CI never receives the release key (setup.md section 6; C21). The repo is public, so the signed APK is not attached to the GitHub Release; the record holds its SHA-256.

## 2. A release's steps
Each command is run one at a time, in PowerShell at the repo root unless named otherwise.

1. **Deploy review** of everything since the last release (the iteration files' Deploy findings and the commits since the previous `v*` tag), findings by severity, each with how it was handled.
2. **Version raise**, in app/build.gradle.kts, on a branch and pull request like any change:
   - versionName by CLAUDE.md section 4 (until 1.0.0: a milestone or breaking change raises MINOR, a feature or fix raises PATCH);
   - versionCode higher than every build installed so far, the development app's included.
   - If the release raises the schema version or the backup format version: section 3 below first.
3. **Tag**, by you, on main's merge commit, annotated and GPG-signed, equal to `v<versionName>`:
   - `git checkout main`, `git pull`, `git log -1` (the merge commit);
   - `git tag -s v0.38.0 -m "<the tag message Claude drafted>"`;
   - `git tag -v v0.38.0` (the signature checks);
   - `git push origin v0.38.0`.
4. **CI** (.github/workflows/release.yml) runs on the tag: the version check (tag equals `v<versionName>`, versionCode higher than the newest earlier `v*` tag's), the unsigned release APK built and kept, with R8's mapping.txt, as artifact `release-v0.38.0` for 90 days. Then mapping.txt's SHA-256.
   - `gh run list --workflow release.yml --limit 3`, then `gh run download <run id> -n release-v0.38.0 -D <a folder outside the repo>`.
   - A red run stops the release; the tag stays and the fix is a new version.
5. **Sign and verify**, by you, on this PC, with the release key (never in the repo). The exact apksigner commands, and whether zipalign is needed first, are written in 38c from apksigner's own help on the day, not from memory. Then the signed APK's SHA-256 (`Get-FileHash -Algorithm SHA256`).
6. **Backup** on the phone with the build now installed: More, Back up now, Save. Note the Cash balance and each portfolio's cost on Overall. With a schema or format raise (section 3), keep this backup until the next release.
7. **Install** over the previous release: `& "$env:ANDROID_HOME\platform-tools\adb.exe" install -r <signed APK>` (C17, AC-35). A refusal stops here; nothing is uninstalled to make it fit.
8. **Smoke check** (AC-64), on the phone, no data entered: unlock; the Cash balance and each portfolio's cost equal the figures of step 6; one holding opens; a price update completes; a backup export completes.
9. **Record**, by you: a GitHub Release on the tag with the notes of section 5 (`gh release create v0.38.0 --title "0.38.0" --notes-file <notes file>`, or in the browser). The signed APK and mapping.txt are kept outside the repo, next to the key's backups (mapping.txt turns a release's renamed classes in a crash back into their names).

The first release (38c) adds the move of your data from the debug build once: backup, uninstall debug, install the release, restore (AC-36); its steps are in the 38c file.

## 3. Backup format and schema rule
From spec section 8, "Releases and the format version", and C22:
- A release that raises the backup format version (today 1) or the database schema version (today 14) says so in its record.
- Before it is installed, a backup is exported with the release already on the phone (step 6), and that backup is kept until the next release, so a rollback can restore it.
- Room has no destructive fallback, so a release must open every earlier released schema version: the first release that raises the schema above the first release's (14) gets a migration test from 14, and each later one a test from every released version.
- An older release refuses a newer backup format: after a format raise, a rollback can restore only a backup taken before the raise.

## 4. Rollback
When a release is broken on the phone. Entries made after the kept backup are lost and must be entered again (C22). Run one command at a time; each `adb` command asks.

1. Have the previous release's signed APK (kept next to the key's backups) and the backup taken before the broken release was installed (step 6).
2. If the broken release still opens: More, Back up now, Save, as a record of what will be lost (it may not restore into the older release if the format rose).
3. `& "$env:ANDROID_HOME\platform-tools\adb.exe" devices`: the phone is listed.
4. `& "$env:ANDROID_HOME\platform-tools\adb.exe" uninstall app.ghghorg.atsassets`. This deletes the app's data on the phone.
5. `& "$env:ANDROID_HOME\platform-tools\adb.exe" install <previous release's signed APK>`.
6. On the phone: set up the lock, More, Restore, the backup of item 1; unlock; the Cash balance and each portfolio's cost equal the figures noted before the broken release; enter the Finnhub key again in Settings, Prices.
7. Record the rollback in the broken release's GitHub Release notes, and open a change request for the fix.

Not a rollback: `adb install -r` of the older APK over the newer one. Android refuses it (`INSTALL_FAILED_VERSION_DOWNGRADE`, seen in section 6), and nothing should be done to force it.

## 5. Release notes template
```
## <versionName> (versionCode <n>)

- Commit: <full SHA of the tagged merge commit>
- Since <previous tag, or "the first release">: iterations <list>, change requests <list>
- Deploy review: <findings by severity, each with how it was handled; or "no finding">
- Schema version: <n> (<changed from m / unchanged>)
- Backup format version: <n> (<changed from m / unchanged>)
- Signed APK SHA-256: <hash>
- mapping.txt SHA-256: <hash>
- Smoke check (AC-64): <unlock / figures / holding / price update / backup export: each passed or not>
- Rollback target: <previous tag and versionCode, or "none: the first release; the debug build's backup of <date>">
```

## 6. Rehearsal record (AC-66)
2026-11-21, 38b step 5, on the development app "ATS Dev" (`app.ghghorg.atsassets.debug`) with test data; your app (uid 10454) not touched:
1. The 47 APK kept outside build/ (`0.38.0-dev.47`, 44,619,614 bytes); your figures noted on ATS Dev 47 and a backup saved.
2. versionCode raised to 48 (`0.38.0-dev.48`, the same dex sizes), installed over 47 with `adb install -r`: Success.
3. On 48 a cash deposit of 12.34 ("rehearsal 48"); the Cash balance rose by 12.34.
4. `adb install -r` of 47 over 48: `INSTALL_FAILED_VERSION_DOWNGRADE: Downgrade detected: Update version code 47 is older than current 48`; 48 stayed installed (C22's downgrade refusal seen).
5. `adb uninstall app.ghghorg.atsassets.debug`: Success. `adb install` of 47: Success; the app got a new uid (10448, was 10446).
6. On 47: the lock set up, the backup of item 1 restored; the Cash balance and each portfolio's cost matched the noted figures; the rehearsal entry was gone.
