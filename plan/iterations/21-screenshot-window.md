# plan/iterations/21-screenshot-window.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 and 2 settled the same day (section 11). In build: step 3 done (section 10.1).
Source: plan.md section 4 (row 21), spec section 9 (privacy by default, the screenshot window, the [UNVERIFIED] note on `FLAG_SECURE` and the recents preview), section 6 (More and Settings entry), section 3 (not a setting: never saved), section 8 (not in the backup file), section 14 (K8), AC-20, AC-58 (the screenshot window part), AC-61, AC-62; wireframe 10f (frame B's Security row, frame C marker 8); iteration 01 (the lock and `FLAG_SECURE`), 20 (Settings, the lock rules, `LockHolder`); open-items.md item 53; CLAUDE.md (security code not from memory; layers in the window; a defect in test support needs no intent file). "My reading" is an interpretation, not a test.

## 1. Outcome
Settings' Security section gains a second row, "Screenshot window", showing "Off". A tap opens a layer with "Allow screenshots for 3 minutes". After your fingerprint or phone PIN, screenshots and screen recording work for 3 minutes, and the row and the layer show the time left and Turn off. The window ends after 3 minutes, on Turn off, or when you leave the app; a restart always starts with it off. The recent-apps preview stays hidden throughout.

## 2. Spec refs
- Section 9: screenshots and recording blocked by default, the recents preview hidden; the window a fixed 3 minutes, not adjustable, after a biometric or phone PIN check each time it is turned on; the recents preview stays hidden; it ends after 3 minutes or when you leave the app; it never survives a restart and is not in the backup file; it is not selective.
- Section 6, wireframe 10f: frame B, the row "Screenshot window / Off >"; frame C marker 8: "Off: screenshots and recording are blocked.", the button "Allow screenshots for 3 minutes", "Asks for your fingerprint or phone PIN each time.", and while on "On, 2:41 left. Ends when you leave the app.", "Anything can capture the screen while it is on." and Turn off.
- Sections 3 and 8: not a setting, never saved, not in the backup file. Section 14, K8: the 3-minute window is the app lock's.
- AC-20 (blocked by default; the check; 3 minutes; ends early on leaving; gone after a restart; a hand check on the phone), AC-58's screenshot window part, AC-61, AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.
- Open item 53 (the font-scale reset in test support).

## 3. Out of scope
- The price source, the tab order (22) and the lists (23 to 25): Settings shows only what is built.
- The backup file (31): there is nothing to leave out of it yet, since the window is never stored.
- Protection against overlay attacks, which spec section 9 says `FLAG_SECURE` does not reliably give.

## 4. Analysis (2026-11-10, by reading the code and the spec)
- MainActivity sets `FLAG_SECURE` in `onCreate` and calls `setRecentsScreenshotEnabled(false)` (01); CoverScreen replaces the content whenever the window loses the focus.
- The window, in my reading: `FLAG_SECURE` cleared for 3 minutes and set again after, with `setRecentsScreenshotEnabled(false)` kept all along. Whether that alone hides the recents preview, and the order of setting the flag again on leaving, are [UNVERIFIED] in the spec; only the phone shows them.
- The window ends on leaving, so the flag is set again in `onStop`; `onPause` may be safer for the preview (spec 388: set late it can fail to hide it). With the preview off through `setRecentsScreenshotEnabled(false)`, my reading is that `onStop` is enough; the phone check tests recents during the window.
- The window's state is K8's, so it sits next to the lock: a pure-Kotlin `ScreenshotWindow` (start, time left, end) on a monotonic clock passed in, as in 20, kept in `LockHolder`, so a turn of the phone keeps it and a process restart loses it (always off after a restart, as the spec requires).
- MainActivity owns the flag. It passes the time left, or "off", to the ViewModel each second while the window is on, and the Settings model shows it (CR2).
- The check: MainActivity has one `BiometricPrompt`, wired to the lock. A second use for the window must never unlock the app, and the lock's prompt must never start the window. How AndroidX routes a result with two prompt objects is a look-up (R1); the alternative is one prompt that remembers what it is for (decision 2).
- Frame C is a layer, not a choice of options; it is drawn in the window like 20's choice layer, never as a `Dialog` (CLAUDE.md, 07). The fingerprint or PIN prompt is a system window and takes the focus while it is up; CoverScreen shows under it, as at unlock.
- A lock while the window is on: the spec says only that the window ends on leaving. An idle lock in view (20) does not leave the app (decision 1).
- Open item 53: `DeviceSettingsRule.after()` waits until the font scale is back, as `before()` does.

## 5. Files
- app/: lock/ScreenshotWindow.kt (new, pure Kotlin); lock/LockHolder.kt (holds the window); MainActivity.kt (the flag, the check, the end on leaving, the one-second tick while on); ui/SettingsModel.kt and ui/SettingsScreen.kt (the row and the layer); AppViewModel.kt (the layer's open state, the time left as shown); AppContent.kt; strings.xml (section 12); build.gradle.kts (versionCode 30).
- androidTest: ScreenTestSupport.kt (open item 53); a new test file for the row, the layer and the flag.
- Tests: ScreenshotWindow (off at start; on after start; time left; ends at exactly 3:00; turned off; ended; never on without the check); the model and the ViewModel wiring (Off, "On, m:ss left", the layer, Turn off); screen tests (AC-61 (d): the row and the layer's text; (b): the layer keeps the window's focus; (a): the layer at the largest font in landscape; and, if it runs steadily, on MainActivity after the test PIN `FLAG_SECURE` cleared, and after Home set again).
- Documents: this file, plan.md (section 11 and the change log, with #156's merge line), open-items.md (item 53 closed).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-21-screenshot-window, one pull request.
1. This file and the plan.md lines (with pull request #156's merge line).
2. Open item 53: `DeviceSettingsRule.after()` waits for the font scale to come back. No failing test, since the race cannot be forced; the evidence is run 42749660919, attempt 1.
3. The look-up R1, with your yes.
4. `ScreenshotWindow` in lock/, tests first, shown red against a stub.
5. The wiring: the check, the flag cleared and set again, the end on leaving, the tick to the ViewModel; ViewModel tests first, shown red.
6. Settings' row, the layer and the strings; model tests first, shown red.
7. The screen tests.
8. Documents, versionCode 30, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A window that stays open would leave the screen capturable. It cannot outlast its 3 minutes, leaving the app or a restart; every doubt ends it.
- 2. A check result routed to the wrong use could unlock the app, or start the window without a check (R1, decision 2).
- 3. The platform is unverified (spec 388): clearing the flag at run time lets screenshots and recording through at once; the recents preview stays hidden with `setRecentsScreenshotEnabled(false)` alone; setting the flag again in `onStop` is in time. Each is a phone check, and a failure blocks the merge.
- 4. The tick needs a timer on the main thread that stops on leaving, as in 20.
- 5. The layer must keep MainActivity's focus and fit at the largest font; the prompt is a system window and takes the focus on purpose.
- 6. The emulator is not the phone (C19): a screen test can read the flag, not whether a screenshot was really taken.

## 9. Proof
- Commands: as in 20, all with --rerun-tasks. The screen tests on the pull request: 44 plus this iteration's new ones.
- Unit tests: steps 4 to 6.
- The phone check, over 20's build: the data opens after the install (AC-35); a screenshot and a recording blocked, the recents preview hidden; "Allow screenshots for 3 minutes": the prompt cancelled leaves it off; after the check a screenshot and a recording work; recents during the window still hidden; the countdown and its end at 3:00; Turn off; Home during the window ends it; a force stop and restart: off; an idle lock with the window on (decision 1); a turn of the phone with the window on keeps it; largest font and landscape; TalkBack; light and dark; logcat as in 20.

## 10. Research (step 3; each look-up shown to you first, CLAUDE.md section 8)
- R1: AndroidX Biometric (the version in the project): what a second `BiometricPrompt` object in the same activity does to the first one's callback, and what happens to a result after the activity is re-created. By javap on the cached jar, or the library's source if you paste it (developer.android.com could not be fetched before, CLAUDE.md). It decides decision 2.
- No version change is planned: no new library.

### 10.1 Results (2026-11-10, step 3)
- R1, by javap (your yes): Gradle's cache holds androidx.biometric 1.1.0 as an aar without sources; its classes.jar disassembled with `javap -p -c` in the scratchpad. Every `BiometricPrompt` constructor gets the `BiometricViewModel` through `new ViewModelProvider(activity).get(BiometricViewModel.class)` (`getViewModel`) and then calls `init`, which calls `setClientExecutor` and `setClientCallback` on it; `authenticate` sets no callback (it calls `findOrAddBiometricFragment(...).authenticate`); `ResetCallbackObserver` only calls `resetClientCallback`.
- What follows (my reading): one ViewModel per activity, shared by every prompt object, so the last object constructed receives every result, the lock's included: a second prompt object for the window is not safe (decision 2). After a re-creation, the new activity's `onCreate` builds the prompt again and sets its callback on the same ViewModel, which the re-creation keeps, so a late result reaches the new activity's callback, as for the lock since 01.

## 11. Decisions (Build, settled by you 2026-11-10)
1. A lock while the window is on (the idle lock in view, 20; the phone locking with the app in view) also ends the window: any lock ends it (your A). Not chosen: (B) only leaving, the 3 minutes and Turn off end it, as the spec's words say.
2. One `BiometricPrompt` that remembers what it was opened for, the lock or the window, the purpose kept in `LockHolder` so a turn of the phone keeps it; a result with no purpose is ignored (your A). Not chosen: (B) a second prompt object, which R1 rules out (10.1).

## 12. Wording to approve (D9)
To be shown on the phone in step 8:
- The row "Screenshot window" with "Off", or "On, m:ss left".
- The layer's title "Screenshot window"; "Off: screenshots and recording are blocked."; "Allow screenshots for 3 minutes"; "Asks for your fingerprint or phone PIN each time."; "On, m:ss left. Ends when you leave the app."; "Anything can capture the screen while it is on."; "Turn off".
- The prompt's title (new), for example "Allow screenshots".
- TalkBack for the row: "Screenshot window, Off" or "Screenshot window, on, 2 minutes 41 seconds left".

## 13. Status and notes
- Draft 2026-11-10; approved the same day.

## 14. Departures from the plan
- Step 5: a turn of the phone keeps the window only while the app stays unlocked; under "every time you leave" a turn locks the app (since 01), so it also ends the window (decision 1). `onStop` ends it unless the activity is only re-created and the app is still unlocked.
- Step 6: the frame of 18 to 20's choice layer (the clear layer, the titled card, the scroll) became `LayerCard` in AllocationParts.kt, shared by the choice layer and the screenshot layer; the choice layer's radio group is now a column inside the card. A first version passed the group as a second modifier parameter, which lint flagged (ModifierParameter, a sixth finding); it was changed, not silenced. The time left is shown in whole seconds rounded up, so the window starts at 3:00 and its last second reads 0:01. TalkBack reads the time with plurals ("1 minute", "2 minutes").
