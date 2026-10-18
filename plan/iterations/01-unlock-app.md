# plan/iterations/01-unlock-app.md

Stage: Build. Status: approved by you on 2026-10-19 (draft plus the four decisions in section 9). Not started: no branch, no code.
Source: plan/plan.md section 4 (row 01), intent/intent.md v52 (final), spec/spec.md v4 (final).
Nothing here has been built, run or verified. "My reading" means an interpretation of spec.md that you approved, not a test.

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
4. Wire MainActivity: switch from ComponentActivity to FragmentActivity (needed by the biometric prompt), set the secure window flag before any content is set, show the prompt on start and on return, and show no content while locked.
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
- It will probably add a ninth "newer version" lint notice. It is left for the dependency-upgrade spike, not silenced.
- The dependency-upgrade spike runs right after this iteration and may break this code. Its proof re-runs the phone checks in section 8.
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

## 9. Decisions (all resolved, approved 2026-10-19)
1. No PIN or biometric set up on the phone: block with a plain message.
2. Figma tokens: first handoff deferred out of this iteration; the template theme is used.
3. Cancelling the prompt: stay on a locked screen with an Unlock button.
4. The locked screen shows only the app name and the Unlock button.

## 10. Status and notes
Approved, not started. Definition of done: plan/plan.md section 9.
