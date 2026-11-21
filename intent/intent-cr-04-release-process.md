# intent-cr-04-release-process.md (CR4, final)

Change request CR4, 2026-11-21: a release process, so that a signed release build can become the app on my phone and later builds can replace it safely.
Base: intent.md v52, CR1, CR2 and CR3. Discussion: the session of 2026-11-20 and 2026-11-21 (not written to a file). Reference: the AI-Native SDLC playbook, read 2026-11-21 (its Deploy section; it gives no step-by-step release procedure).

## Problem
- Every build on my phone so far is the debug build, and my real data lives in it. A debug build is debuggable: `run-as` reaches the app's files and a debugger can read the app's memory. The files are encrypted; memory is not.
- The release build (app/build.gradle.kts) has R8 on and no signing config. It has never been installed. Open item 3 (mapping.txt) and open item 4 (whether Room finds AtsDatabase_Impl under R8) are unresolved.
- There is no process for a release: no version raise (versionName has been "1.0" since the start; versionCode rises with every build), no record of what a release holds, no rollback path, and no rule for a release that changes the backup format or the database schema.
- Moving from debug to release can't be an install over it: the id is the same but the signature differs, so Android refuses. The move is backup, uninstall debug, install release, restore (the AC-36 path, with my real data).
- plan.md section 13 lists Deploy (review, install route, release signing) as outside the current plan.

## Outcome
- **My daily app is the release build** (decided 2026-11-21, open question 1, A). My real data moves to it once: backup, uninstall debug, install release, restore. After that, nothing debuggable holds my real data.
- **Phone checks run on one development app** (decided 2026-11-21, open question 2, A): the debug build gets its own id, `app.ghghorg.atsassets.debug`, and its own name on the phone, holds test data only, and installs next to the release app. ATS Check merges into it: the development app's backup reminder is due after 10 minutes, and the `.check` build and the ATS Check app on the phone go. The development app may hold a price key for price checks.
- **When:** a release happens when I decide, not on a schedule, from any merge commit on main. Main stays releasable after every merge. A release can bundle several iterations.
- **The loop for changes is unchanged:** change request, intent, spec, iteration, CI, phone check, merge. A release is a separate step after it.
- **Who does what:** Claude prepares (the Deploy review since the last release, the version raise, the tag message, the release notes, the commands); I authorize and do the release steps myself: I create and push the tag, sign the APK, install it and publish the record.
- **A release's steps:**
  1. A Deploy review of everything since the last release, findings by severity.
  2. A version raise (versionName and versionCode).
  3. An annotated, GPG-signed `v*` tag on main's merge commit, created by me only.
  4. CI builds the unsigned release APK for the tag and checks that the tag equals versionName and that versionCode rose.
  5. I sign the APK locally with apksigner and verify it (`apksigner verify`). CI never receives the release key (setup.md section 6).
  6. A backup on the phone with the installed build.
  7. The install over the previous release (`adb install -r`, the route of C17 and AC-35).
  8. A smoke check on the phone (below).
  9. The release record (below).
- **The release record:** one GitHub Release per `v*` tag. Its notes hold the version (versionName and versionCode), the commit, the iterations and change requests since the last release, the Deploy findings by severity and how each was handled, the schema version and the backup format version and whether each changed, the SHA-256 of the signed APK and of mapping.txt, the smoke-check result, and the rollback target (the previous release). The signed APK and mapping.txt are kept outside the repo, next to the key's backups.
- **Backup format and schema rule (rollback):** a release that raises the backup format version (spec section 8) or the schema version is marked so in its record. Before installing it, I take a backup with the old release, and that backup is kept until the next release. A rollback is uninstall, install the previous release's APK, restore that backup; entries made after it are lost. No downgrade install over a newer build.
- **The smoke check:** on the phone, with the release build: unlock; my data is all there (the Overall total matches the figure from before the install); open one holding; a price update; a backup export. No data entry. The first release also runs the debug-to-release move once (backup, uninstall debug, install release, restore).
- **An enforced gate:** a Claude Code hook in .claude/settings.json makes `adb install`, `git tag`, `gh release` and `assembleRelease` ask every time, so the written rule in CLAUDE.md section 8 is also enforced.
- **Rollback is rehearsed** once, before it is needed, and written as a script or a short list of commands.
- **A hotfix** is a change request like any other, with the failing test first, shipped as its own small release.

## Users
- Me.

## Constraints
- Sideloaded on my own phone, for me only; no Play Store (intent.md v52).
- The release key is created by me, kept outside the repo with at least two backups, and never given to CI (setup.md section 6). A lost key blocks every later install over the release (C17).
- No GitHub Environment and no signing secret on GitHub.
- The repo is public (stated in CR3, 2026-11-05; not checked again). So the signed APK is not attached to the GitHub Release; the record holds its SHA-256.
- No telemetry or crash reporting (spec section 9). The signals after a release are CI runs and the phone.
- Room has no destructive fallback, so a release must open every earlier released schema version.
- No change to what the app does or shows, except what the release build needs to work at all (open item 4).
- Changing .claude/settings.json needs my yes (CLAUDE.md section 8).

## Open questions
None remaining in Plan. Both were answered on 2026-11-21 (see Outcome: the daily app, and the development app).

## Deferred to Design
- The spec edits: C17 (the chosen install route), AC-35 and AC-36 (the release and the move from debug), section 8 (one sentence for the rollback rule), and whether the release record and the smoke check get criteria of their own.

## Deferred to Build
- Open item 3: where mapping.txt is kept and how it is matched to a release.
- Open item 4: whether Room finds AtsDatabase_Impl under R8, and the keep rules if not.
- Migration tests from every released schema version.
- The CI job for the tag (its workflow, the version checks, the unsigned APK as a run artifact) and whether a release APK is also built on every pull request.
- The hook's exact form, the rollback script, and the release notes template.
- The key's creation and its backups (my step, guided).
- The development app's name on the phone (D9 wording), and how the build file merges the debug and check build types.
