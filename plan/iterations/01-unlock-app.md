# plan/iterations/01-unlock-app.md

Stage: Build. Status: approved by you on 2026-10-19 (draft plus the four decisions in section 9). Built and checked on the phone (2026-10-19); awaiting your review of pull request #70. See section 10.
Source: plan/plan.md section 4 (row 01), intent/intent.md v52 (final), spec/spec.md v4 (final).
This iteration is built; what was run and checked, and what was not, is stated in section 10. Sections 1 to 9 are the plan as approved. "My reading" means an interpretation of spec.md that you approved, not a test.

## 1. Outcome
I open the app, confirm my fingerprint or phone PIN, and only then see anything. Leaving the app locks it again. Screenshots, screen recording and the recents preview show nothing.

## 2. Spec refs
- spec section 9: Lock, First launch, Privacy by default, minimum Android version, and the Android file screens bullet (read only; its behaviour is iteration 31 and 34).
- spec section 6: First launch and empty state (only the lock-first step; the guided steps are iteration 02 and later).
- spec section 14: K8 app lock, K9 screens.
- C17 (install route, unchanged here) and C18 (key handling, not touched here).
- Criteria: 6 (lock per the chosen setting, here only "every time I leave the app"), 20 (default part only: blocked screenshots, recording and recents preview; the 3-minute window is iteration 21), 35 (a new build installs over the previous one).
- Standing checks: criteria 9, 39 to 42 on the lock screen, and 35.

## 3. Out of scope
- The other three auto-lock choices and the Settings screen (iteration 20).
- The 3-minute screenshot window (iteration 21).
- The first Figma token handoff (deferred out of this iteration; the template theme is used).
- Anything with data: no storage, no records, no network, no encryption.
- Android's file screens and the lock (criterion 57; iterations 31 and 34).

## 4. Files
New:
- app/src/main/java/app/ghghorg/atsassets/lock/AppLock.kt (lock state and rules, no Android UI classes in the logic)
- app/src/test/java/app/ghghorg/atsassets/lock/AppLockTest.kt
Changed:
- app/src/main/java/app/ghghorg/atsassets/MainActivity.kt (FragmentActivity, secure flag, prompt, locked screen)
- app/build.gradle.kts and gradle/libs.versions.toml (androidx.biometric, version looked up and pinned on the day)
- app/src/main/res/values/strings.xml (placeholder wording, D9)
- CLAUDE.md only if a command, convention or pitfall is added.
- plan/plan.md status table (iteration 01 row) and this file's status.

## 5. Order of work
1. Branch iter-01-unlock-app from the updated main.
2. Look up the current androidx.biometric version and API on the day, and pin it.
3. Write the AppLock tests first: locked at start; unlocks only on success; locks when the app is left; a cancelled or failed prompt stays locked; "no PIN or biometric set up" is its own state and never unlocks. See them fail, then implement.
4. Wire MainActivity in two parts.
   4a. AppLock logic (done: 12 tests pass, commit 9397262).
   4b. Switch from ComponentActivity to FragmentActivity (needed by the biometric prompt). In onCreate, before setContent: set FLAG_SECURE, call setRecentsScreenshotEnabled(false), and create the prompt (so its callback is attached again after a recreation). In onStart: check availability (only BIOMETRIC_SUCCESS counts as available), then show the prompt if AppLock asks for it. In onStop: cancel any showing prompt, then lock. No isChangingConfigurations exception, no prompt from onResume. The Unlock button goes through AppLock.shouldShowPrompt(). The error callback (not the single-failure callback) maps to a cancelled prompt. No content while locked.
   4c. Experiment added after phone check 7 (2026-10-19): while unlocked, show a cover screen (the app name only) whenever the window is not focused, so the recents live card shows no content. Kept: the phone showed it helps (checks 14a to 14c, section 10).
5. Placeholder wording for the locked screen and the "set up a screen lock" message (D9).
6. Run build, tests and lint with real output, then CI.
7. Phone checks (section 8).

## 6. Stubs
- The unlocked screen stays the template's "Hello Android!". It is replaced from iteration 02.
- The lock policy is fixed at "every time I leave the app" (the spec default). The setting arrives in iteration 20.

## 7. Risks
- Whether "leaving the app" is detected the same way while the system prompt is open. Not checked.
- Whether FLAG_SECURE alone blanks the recents preview, or setRecentsScreenshotEnabled(false) is also needed (spec section 9 marks this partly checked; the order of setting and clearing is tested here and in iteration 21).
- Whether androidx.biometric builds cleanly with Kotlin 2.2.10 and AGP 9.4.1. Not checked.
- Phone check 7 (2026-10-19): the stored recents preview was blank, but opening recents straight from the unlocked app showed the live content. My reading (not confirmed): the launcher shows the live window at that moment. Step 4c tries a cover shown while the window is not focused. It may flicker when the notification shade is pulled down, and it may not help if the window keeps focus when recents opens. Result (2026-10-19): it helped, see section 10.
- Lint after step 4b shows the same 8 library-version notices as before; androidx.biometric 1.1.0 adds none (seen 2026-10-19). They are left for the dependency-upgrade spike, not silenced.
- The dependency-upgrade spike runs right after this iteration and may break this code. Its proof re-runs the phone checks in section 8.
- A recreation (rotation, theme or font-size change) builds a new AppLock, so the app re-locks and prompts again. Accepted for now (fail closed); keeping the unlock across a recreation needs a ViewModel and belongs to a later iteration, when forms must keep typed values (criterion 40).
- AppLock ignores a success that arrives after the app was left. If the system prompt ever stops the Activity while it is open, a correct PIN could be ignored and the prompt would reappear (a loop). Phone checks 1 and 4 would show it.
- Opening the recents screen and returning to the same app may not call onStop, so the app might stay unlocked. Decided by phone check 13.
- cancelAuthentication() may deliver its error callback after the next prompt was shown, which could clear that prompt's "showing" state and allow a duplicate. Not checked.
- androidx.biometric 1.1.0 may not expose androidx.fragment on the compile classpath. If the compile fails on FragmentActivity, add it explicitly.
- Checked so far, from Android's biometric page on translated copies: setAllowedAuthenticators with BIOMETRIC_STRONG or DEVICE_CREDENTIAL gives one prompt for biometric or PIN; that combination is not supported on API 29 and lower (my minimum is 33); androidx.biometric is required; a sample used FragmentActivity. Not re-read today: FLAG_SECURE and the recents preview.

## 8. Proof
Commands (Windows PowerShell, repo root), real output shown:
- .\gradlew.bat :core:assemble :app:assembleDebug
- .\gradlew.bat :core:test :app:testDebugUnitTest
- .\gradlew.bat :app:lintDebug
- CI green on the pull request.
The test report must show the new AppLock tests, so a vacuous pass is visible.

By hand on the phone (Android 17), each result written back into this file as "checked on the phone" or "not checked":
1. A cold start shows the prompt before any content.
2. Cancelling leaves the locked screen with an Unlock button.
3. Success shows "Hello Android!".
4. Home, then return, prompts again.
5. A screenshot is blocked.
6. Screen recording shows no app content.
7. The recents preview is hidden.
8. With no PIN or biometric set up on the phone, the plain message shows and nothing unlocks. (Whether this can be tested on your phone without changing its settings is not known; if not, it is stated as not run.)
9. A new build installs over the old one (criterion 35).
10. The locked screen is readable at the largest font size and in light and dark themes (criteria 39 to 42).
11. Rotate the phone while the prompt shows: no second prompt opens on top.
12. Switch dark mode on or off while unlocked: record what happens (expected: the app locks and prompts again, see Risks).
13. Open the recents screen and return to the app: record whether it is locked (the spec says every time the app is left).
14. After step 4c: repeat check 7 (recents opened from inside the app, and again after switching to another app) and pull down the notification shade while unlocked. Record whether the cover shows, whether the content ever shows, and whether it flickers.

## 9. Decisions (all resolved, approved 2026-10-19)
1. No PIN or biometric set up on the phone: block with a plain message.
2. Figma tokens: first handoff deferred out of this iteration; the template theme is used.
3. Cancelling the prompt: stay on a locked screen with an Unlock button.
4. The locked screen shows only the app name and the Unlock button.
5. Design readings (approved 2026-10-19): only BIOMETRIC_SUCCESS counts as "screen lock available"; the lifecycle rules in section 5, step 4b; FLAG_SECURE and setRecentsScreenshotEnabled(false) are permanent in this iteration.
6. Recents live card (approved 2026-10-19, option 2): try the cover screen of step 4c as an experiment; fall back to recording the limit if it does not help.

## 10. Status and notes
Built and checked on the phone (2026-10-19). Done: pin of androidx.biometric 1.1.0; AppLock logic with 12 passing tests (commits 81c7bb8 and 9397262); MainActivity wiring (4b, commit 6611587, pull request #70 as a draft). Local run: build green, 13 unit tests pass (1 in core, 12 in app), lint green with 8 library-version notices and nothing new. CI was reported green by you; I did not see its log.
Step 4c (cover screen while the window is not focused) is built and passed the same local build, tests and lint.
Phone results (2026-10-19, debug build of commit 6611587, then again with step 4c; these are your reports, I did not see the phone): 1 cold start shows the prompt first, yes. 2 cancel leaves the app name and an Unlock button, yes. 3 unlock shows "Hello Android!", yes. 4 Home then return prompts again, yes; Home while the prompt is open, then return, gives exactly one prompt. 5 screenshot blocked. 6 screen recording is black. 7 recents: before step 4c, opening recents straight from the unlocked app showed the live content (the stored card was blank); with step 4c the card shows the app name only (14a) and stays blank after switching to another app (14b). 9 install over the old build succeeded (data survival not testable yet, the app stores nothing). 11 rotating while the prompt shows gives one prompt. 12 switching dark mode while unlocked brings the prompt back (the app re-locks on recreation, as accepted in Risks). 13 recents then return: the prompt appears. 14 cover screen: app name only, no flicker when the notification shade was pulled down; unlocking still works after Home and return.
10 largest font size, light theme and dark theme on the locked screen: all readable and tappable. TalkBack on the locked screen: reads it fine (your report; I do not know the exact words it spoke).
8 no screen lock set up: checked on the phone on 2026-10-20 (your report), on the debug build of commit 658a387, installed at 2026-10-20 15:24:09 phone time (Gradle reused the earlier APK, all 37 tasks up to date). After you removed the screen lock and fingerprints, the plain "set up a screen lock" message showed, with no Unlock button and no content. Home and return kept the message and nothing unlocked. After you set the lock again, the app prompted normally. An earlier run on an older build (which one is not known) is not counted.
Review against the spec refs: done are the lock of section 9, the lock-first step of first launch, and the default privacy part of criterion 20, with criterion 6 for the fixed "every time I leave the app" policy. Not done, with reasons: the other three auto-lock choices (iteration 20), the 3-minute screenshot window (iteration 21), and criterion 35's data survival (the app stores nothing yet; checked in iteration 02). Several Android behaviours that spec section 9 marks as unverified were seen on one phone (Android 17) with both window protections set: one prompt for biometric or PIN, blocked screenshots and recording, and a blank stored recents card. The live recents card needed the cover screen of step 4c, which is not written in spec.md. Not seen: other phones or Android versions.
For later iterations: iteration 21 must keep the cover-screen behaviour and check the order of the window flags; iteration 02 must add the data-survival check for updates; the dependency-upgrade spike must re-run checks 1 to 14.
Left before done: your review of pull request #70. The CLAUDE.md pitfall and the plan.md status row are committed. CI was reported green by you on commit 658a387; I did not see its log.
Definition of done: plan/plan.md section 9.
