# plan/iterations/21-screenshot-window.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 and 2 settled the same day (section 11). Done: built 2026-11-10 (steps 1 to 8; section 10.1, section 14); phone check 2026-11-10 all yes (section 13.3); the screen tests 52 of 52 on pull request #157 (section 13.4); reviewed against the spec refs (section 13.5); merged as 93ea83d, main green (section 13.6).
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
- Draft 2026-11-10; approved the same day; decisions 1 and 2 settled.

### 13.1 Steps 1 to 7 (2026-11-10)
- Step 1 (b3440a9): this file and the plan.md lines (with pull request #156's merge line).
- Step 2 (2cb434b): open item 53: `DeviceSettingsRule.after()` waits until the font scale is back (1.0 when none was stored, my reading of Android's default). No failing test: the race cannot be forced; the evidence is run 42749660919, attempt 1.
- Step 3 (b043102): R1 by javap on androidx.biometric 1.1.0 (10.1); decisions 1 and 2 (A and A).
- Step 4 (85176cd): lock/ScreenshotWindow.kt: the check asked for only while off, a passed check the window's only if it asked and starting it only while unlocked, the end on Turn off, leaving or a lock, the time left ending at exactly 3:00. ScreenshotWindowTest 13. Red against a stub first: 13 tests, 11 failed (off at start and never on without a check passed on the stub). Tests 860 to 873.
- Step 5 (eb58984): the wiring: one prompt with the window's own title, a result the window asked for never reaching the lock; `FLAG_SECURE` cleared while on and set again at once otherwise, the recents preview off all along; the tick on each whole second of the time left; the end in `onStop` (not on a turn of the phone that keeps the app unlocked) and at a lock in view. `ScreenshotWindow` kept in `LockHolder`. ScreenshotWindowViewModelTest 4. Red against a stub first: 4 tests, 2 failed. Tests 877.
- Step 6 (579535d): Settings' Screenshot window row ("Off" or "On, m:ss left"; TalkBack in words, with plurals) and its layer drawn in the window (off: the blocked line, Allow, the check line; on: the time left with "Ends when you leave the app.", the warning, Turn off); `LayerCard`, the frame shared with the choice layer; the layer opened on Settings only, closed by Back, a tap outside, leaving Settings, a tab or an unlock; the "Iteration 21" strings. SettingsModelTest 2 more, ScreenshotWindowViewModelTest 4 more. Red against stubs first: 6 tests, 3 failed. One lint finding (ModifierParameter) changed away, not silenced. Tests 883.
- Step 7 (6673888): ScreenshotWindowScreenTest.kt (androidTest): ScreenshotLayerFocusTest (AC-61 (b)), ScreenshotWindowFlagTest (the flag after the test PIN, Turn off and Home), ScreenshotLayerLargestFontLandscapeTest (AC-61 (a); upright again at the end), ScreenshotTalkBackTest 5 (AC-61 (d)); `passCheck()` in ScreenTestSupport.kt. `@Test` lines 52 (were 44).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step. Steps 1 to 7 were committed together after step 7 (your "go", then "commit"), each from its own versions through a temporary index; a first attempt put each step's files under the next step's message and was reset with your yes (section 14).

### 13.2 Step 8 (2026-11-10)
- versionCode 30; full run 13:26 to 13:30 green, 883 tests, 0 failures, 1 skipped; lint the 5 listed findings. APK 42,892,795 bytes, built with the price-source key set (checked True/False only). No schema change (database version 8).

### 13.3 Step 8: the phone check (2026-11-10, all yes)
versionCode 30 installed over 20's build (versionCode 29; `adb install -r`, Success; lastUpdateTime 2026-11-10 13:33:00, first install 2026-10-29 kept).
- 1 (AC-35): the data opened after the install: yes.
- 2 (spec 9, AC-20): by default a screenshot refused, a recording black, the recents card without content: yes.
- 3 (AC-58): the row "Screenshot window / Off"; the layer's three lines and Allow; closed by a tap outside: yes.
- 4: the check cancelled leaves it off, a screenshot still refused: yes.
- 5 (AC-20, risk 3): after the check, the countdown from 3:00, the warning and Turn off; a screenshot with power and volume down and a screen recording show the app; the recents card still without content; back from recents the window off (leaving the app): yes. Note: recents' own Screenshot button stays unavailable during the window, since it captures the recents preview, which spec 9 keeps hidden all along; screenshots during the window are taken with the phone's keys.
- 6: at 0:00 Off, and a screenshot refused again: yes.
- 7: Turn off ends it at once: yes.
- 8: Home ends it: yes.
- 9: a turn of the phone keeps it and its count (auto-lock "only when the phone locks"): yes.
- 10 (decision 1): an idle lock in view ends it: yes.
- 11: after a force stop and restart it is off: yes.
- 12 (AC-39, risk 5): largest font and landscape, off and on: yes.
- 13 (AC-39): TalkBack: the row with its state, the layer's lines and buttons: yes.
- 14 (AC-40): light and dark: yes.
- 15 (spec 9): `adb logcat -d -v threadtime,uid`, the app's lines by uid 10454. The first read (102 lines, 14:03:47 to 14:05:53) held no start of the app. Re-check: a force stop, an unlock, the window turned on and off, Home; then a read (85 lines, the process start at 14:07:23.582, `Start proc 31238`, to 14:07:46): the key not in them nor in the buffer (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=" or amount; the "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; `E/BiometricPromptCompat` "Unable to cancel authentication. BiometricFragment not found." from `onStop` with no prompt up (as in 19): yes.
- 16 (D9): the wording of section 12 approved as shown.

### 13.4 AC-62 and the screen tests
From 21, the Screenshot window's layer focus, the layer at the largest font in landscape, the row's and the layer's text, and `FLAG_SECURE` after the test PIN, Turn off and Home are checked by ScreenshotWindowScreenTest.kt (androidTest); whether a screenshot, a recording or the recents card really shows the app stays a hand check (13.3; C19).
- Pull request #157 at f546b50: the screen tests (run 42766967483) 52 of 52 at the first attempt (tests 255 s, run 06:12:37Z to 06:21:09Z), the 8 new ones all passing at their first run (ScreenshotWindowFlagTest 10.9 s, both PIN checks taken); CI (run 42766967587, 2m 40s) and CodeQL (run 42766964305) green (gh, read only). AppStartTimingTest's "load and Overall" median 3,156 ms (20's #155: 2,604 ms); 21 adds only setting the flag and one state update in `onStart`, so this is the emulator's spread. Open item 53's race did not show; one run proves nothing about it.

### 13.5 Review against the spec refs (2026-11-10, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: blocked by default, `FLAG_SECURE` set in `onCreate`, the recents preview off through `setRecentsScreenshotEnabled(false)` all along (section 9); the window a fixed 3 minutes on a monotonic clock, ending at exactly 0, not adjustable; a fingerprint or phone PIN check each time, through the one prompt, a check the window asked for never reaching the lock and starting the window only while unlocked (decision 2, R1); `FLAG_SECURE` cleared in one place only, while time is left, set again at once on Turn off, at 0, in `onStop` and at any lock (decision 1); never saved: no store write, so not in the backup file (sections 3 and 8); a restart a new `ScreenshotWindow`, off (`LockHolder` lives only as long as the process); the row with its value and the layer with the lines of marker 8, drawn in the window (`LayerCard`; ScreenshotLayerFocusTest); no logging (spec 9) and no floating point in the added code. Recents' own Screenshot button stays unavailable during the window because the preview stays hidden, which spec 9 requires (13.3, check 5).

### 13.6 The merge (2026-11-10)
Pull request #157 merged as 93ea83d (08:45:43Z), 9 commits; the branch is gone from origin and locally. On the pull request's last commit (21bc118) CI (run 42783177590, 3m 5s), the screen tests (run 42783177630) and CodeQL green. On main after the merge, all at the first attempt: CI (run 42785161481, 3m 29s), CodeQL (run 42785161387) and the screen tests (run 42785161991, 52 of 52 by the per-class counts, 08:45:43Z to 08:54:06Z; ScreenshotWindowFlagTest 9.7 s) green (gh, read only). Open item 53's race did not show on main either.

## 14. Departures from the plan
- Step 5: a turn of the phone keeps the window only while the app stays unlocked; under "every time you leave" a turn locks the app (since 01), so it also ends the window (decision 1). `onStop` ends it unless the activity is only re-created and the app is still unlocked.
- Step 6: the frame of 18 to 20's choice layer (the clear layer, the titled card, the scroll) became `LayerCard` in AllocationParts.kt, shared by the choice layer and the screenshot layer; the choice layer's radio group is now a column inside the card. A first version passed the group as a second modifier parameter, which lint flagged (ModifierParameter, a sixth finding); it was changed, not silenced. The time left is shown in whole seconds rounded up, so the window starts at 3:00 and its last second reads 0:01. TalkBack reads the time with plurals ("1 minute", "2 minutes").
- Committing: the first attempt to build steps 1 to 6's commits from their saved versions took each step's files from the next step (a PowerShell ordered table indexed by position); five wrong commits, not pushed, were dropped with `git reset --soft c6c1881` (your yes), and the steps committed again, each diff stat checked against its report first (CLAUDE.md pitfall added).
- Step 8: the logcat read was done twice (13.3, check 15), the second after a short re-check, because the first held no start of the app.
