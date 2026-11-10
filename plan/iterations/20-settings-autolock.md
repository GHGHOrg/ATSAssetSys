# plan/iterations/20-settings-autolock.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 to 3 settled the same day (section 11). Done: built 2026-11-10 (steps 1 to 8; section 10.1, decisions 4 and 5, section 14); phone check 2026-11-10 all yes (section 13.3); the screen tests 44 of 44 on pull request #155 (section 13.4); reviewed against the spec refs (section 13.5); merged as e83e6f4, main green after one re-run of its screen tests (section 13.6).
Source: plan.md section 4 (row 20), spec section 3 (Settings), section 6 (More, Settings entry, navigation outline, accessibility), section 8 (settings are in the backup file; 31), section 9 (the lock, the auto-lock choices), section 14 (K8), AC-06, AC-58 (the auto-lock part), AC-61, AC-62; wireframe 10f (frames A to C, markers 2, 3 and 7); iteration 01 (the lock), 17 and 19 (More), 18 and 19 (the choice layer); CLAUDE.md (security code not from memory; Room schema files; layers in the window). "My reading" is an interpretation, not a test.

## 1. Outcome
More has a gear icon at the top right, read by TalkBack as "Settings", that opens Settings. Settings shows its Security section with Auto-lock and its current choice; a tap opens the four choices (every time you leave the app, the default; after 1 minute idle; after 5 minutes idle; only when the phone locks or the app restarts). A choice takes effect at once and is saved, so it holds after a restart. The app then locks by that choice.

## 2. Spec refs
- Section 9: lock by biometric or PIN; the four auto-lock choices, "every time you leave the app" the default. Section 3: auto-lock is a setting. Section 14, K8: the auto-lock policy is the app lock's.
- Section 6, More and Settings entry, wireframe 10f: marker 2, the gear at the top right of More, labelled "Settings" for TalkBack, at least 48 dp; marker 3, Settings' Security section, each row with its current value; marker 7, the auto-lock choice of four with the default marked, "Takes effect at once. Saved."
- Navigation outline: Android's Back returns to the previous screen; auto-lock can cover any screen.
- AC-06 (the app locks per the chosen auto-lock setting); AC-58's auto-lock part (the gear read as "Settings"; Security; the choice applies at once); AC-61, AC-62.
- Standing checks: AC-09, AC-35 (now with a schema change), AC-39 to AC-42.

## 3. Out of scope
- The screenshot window (21), the price source (later), the tab order (22) and the lists (23 to 25): Settings shows only what is built, Security with Auto-lock.
- Settings in the backup file and a restore setting a weaker auto-lock (31, section 8).
- AC-57, Android's file screens not locking the app (31, with backup and restore).

## 4. Analysis (2026-11-10, by reading the code and the spec)
- `AppLock` (01) is pure Kotlin with one rule: `onAppLeft` (from `onStop`) locks an unlocked app. A process start is always locked. The policy is fixed in its KDoc.
- No setting is stored yet. The records are in Room on SQLCipher, version 7, each version an `AutoMigration`; a setting needs a table, so version 8 (CLAUDE.md: compare 8.json with 7.json before installing; stop at the unreadable-data screen if it shows).
- The store opens only after the first unlock (`AppViewModel.start`), so the choice is known from then on. Until it is loaded, the lock keeps the strictest rule, "every time you leave" (fails closed). A restart always locks, whatever the choice.
- "After 1 minute idle" and "after 5 minutes idle": section 9 does not say what idle means (decision 1).
- "Only when the phone locks": the app must learn that the phone locked while it was unlocked, in view or not. My reading: Android's screen-off broadcast while the process lives, registered in code (not in the manifest); a lost process is a restart and locks anyway. Not checked (section 10).
- Time for idle is measured on a clock that does not jump with the wall clock (`SystemClock.elapsedRealtime`), so changing the phone's time cannot stretch it. My reading.
- More's top row gains the gear; More is drawn from its model (17). The four choices use 19's shared choice layer, drawn in the window, with the line "Takes effect at once. Saved." under them.
- Settings is a new screen (`Screen.SETTINGS`) from More; Back returns to More.

## 5. Files
- app/: lock/AppLock.kt (the four policies, the idle clock, the phone lock; pure Kotlin); data/ (a settings table, database version 8, `AutoMigration(7, 8)`, the schema file 8.json; `RecordStore` load and save of the auto-lock choice in core's interface); core/: the auto-lock choice as a type (`AutoLock`) and `RecordStore`'s two functions; MainActivity.kt (the choice from the screen state, touch input as activity, the idle check while in view, the screen-off receiver while the process lives); ui/SettingsModel.kt and ui/SettingsScreen.kt (new; CR2); MoreScreen.kt (the gear); AllocationParts.kt (the choice layer's note line); AppViewModel.kt (the choice loaded with the records, saved on change; the screen; the choice layer); AppContent.kt; strings.xml (section 12); build.gradle.kts (versionCode 29).
- Tests: AppLock (each policy: leaving, idle in view and away, the phone locking, a restart, the choice not yet loaded, a late prompt result); the store (the default when none is saved, a saved choice read back; the migration from 7 to 8 if Room's test helper is available, otherwise on the phone, section 9); the model and ViewModel wiring (load, change, saved, applied at once); screen tests (AC-61 (d): the gear's TalkBack label, Settings' row with its value, the choices with the default marked; (b): the choice layer keeps the window's focus; (a): the choices at the largest font in landscape).
- Documents: this file, plan.md (section 11 and the change log), open-items.md (48 for More if needed).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-20-settings-autolock, one pull request.
1. This file and the plan.md lines (with pull request #154's merge line).
2. The look-ups of section 10, with your yes for each.
3. The lock rules in `AppLock` for the four choices, tests first, shown red.
4. The stored choice: the settings table, version 8, the store's load and save, tests first, shown red.
5. The wiring: the choice in the screen state, MainActivity's idle clock, touch input and screen-off receiver; tests first where they can run (ViewModel), shown red.
6. Settings and the gear on More, the choice layer, the strings.
7. The screen tests (AC-61 (a), (b) and (d)).
8. Documents, the phone check (each of the four choices on the phone; the install over 19 with the schema change), the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A lock rule that fails open would leave the data on screen. The rules are pure Kotlin with a test per choice; until the choice is loaded the strictest rule holds; every doubt locks.
- 2. The platform's behaviour (when `onStop` comes, the screen-off broadcast, a process killed in the background) is not checked (section 10); the phone check proves each choice (AC-06).
- 3. The schema change: an install over 19 must keep the data (AC-35); compare 8.json with 7.json first.
- 4. The idle check while in view needs a timer on the main thread; it must stop when the app leaves view.
- 5. The choice layer must keep MainActivity's window focus and fit at the largest font (18, 19).
- 6. The emulator is not the phone (C19); screen tests cannot press the power button reliably, so the phone-lock choice is a hand check.
- 7. "Only when the phone locks" with the app in the background rests on a broadcast Android may defer for a cached app (10.1); the phone check tests that case, and a failure there blocks the merge.

## 9. Proof
- Commands: as in 19, all with --rerun-tasks. The screen tests on the pull request: 37 plus this iteration's new ones.
- Unit tests: steps 3 to 6.
- The phone check, over 19's build (AC-35 with the schema change): the data opens after the install; the gear and Settings; each of the four choices, chosen and then: leave and return at once; stay away just under and just over 1 minute (and 5); stay in the app untouched over the limit; lock the phone and return; force stop and restart; the choice kept after a restart; largest font and landscape; TalkBack; light and dark; logcat as in 19.

## 10. Research (step 2; each look-up shown to you first, CLAUDE.md section 8)
- R1: Android's screen-off broadcast for an app registering it in code on API 33 to 37: whether it is delivered while the app is in the background, and which receiver flag API 34 and later need. The developer.android.com reference pages could not be fetched before (CLAUDE.md); I would ask you to paste the parts of the `Intent.ACTION_SCREEN_OFF` and `Context.registerReceiver` pages, or check the flags with javap on the cached jars. The phone check stays the proof.
- R2: Room's migration test helper (`MigrationTestHelper`) for Room 3.0.3 (androidx.room3): whether it exists and works on the JVM; javap on the cached jar. Without it, the migration is checked by the install on the phone (AC-35), as in 16a.
- No version change is planned: no new library.

### 10.1 Results (2026-11-10, step 2)
- R1, by your C: the web-fetch agent on both pages (your yes). The Intent reference came back only in part: its `ACTION_SCREEN_OFF` entry was in a summary, not in the verbatim text ("Sent when the device goes to sleep and becomes non-interactive", not quoted from the page), so nothing about who may receive it is checked. The broadcasts guide (developer.android.com/develop/background-work/background-tasks/broadcasts), read in full, says: "If this receiver is listening for broadcasts sent from the system or from other apps … use the `RECEIVER_EXPORTED` flag"; with `RECEIVER_NOT_EXPORTED` "the receiver is able to receive some system broadcasts"; for Android 14, "While apps are in a cached state … the system defers less important system broadcasts such as `ACTION_SCREEN_ON` … Once the app goes from the cached state into an active process lifecycle, the system delivers any deferred broadcasts"; "delivery times of broadcasts are not guaranteed"; and "If you register with the Application context, you receive broadcasts as long as the app runs." The page does not say a flag is required on API 34, nor name `ACTION_SCREEN_OFF` among the deferred ones.
- What follows for the design (my reading): the receiver is registered with the application context and `RECEIVER_EXPORTED`; a fake screen-off from another app could only lock the app, the safe side. When the phone locks while the app is in view, `onStop` comes with the screen off, so the lock also checks there whether the screen is off, without the broadcast. When the app is already in the background, the broadcast may be deferred until the app is active again, and whether it then arrives before the screen shows is not known: risk 7. The phone check tests this case directly; if it does not lock, the design changes before the merge.
- R2: javap was not needed. Room's testing artifact (`androidx.room3:room3-testing`, which holds `MigrationTestHelper`) is neither in the project nor in Gradle's cache; using it would be a new library. The migration 7 to 8 is checked as in 16a: 8.json against 7.json, then the install over 19 on the phone (AC-35).

## 11. Decisions (Build, settled by you 2026-11-10; 4 in step 3, 5 in step 5)
1. "Idle" for the 1- and 5-minute choices is no touch in the app for that long, the time away counting as idle: the app also locks while it is in view and untouched (your A). Not chosen: (B) only the time away, checked on return.
2. When the idle time runs out while the app is in view, the lock screen shows with the biometric prompt at once, as when you return (your A). Not chosen: (B) the lock screen with its Unlock button only.
3. Settings are stored as one row with a column per setting; each later setting adds a column and a schema version (your A). Not chosen: (B) name and value rows.
4. Under the two idle choices, the phone locking does not lock the app by itself; only the idle time counts, so a phone unlocked again within the limit shows the app (your A, step 3). Not chosen: (B) the phone lock also locks the app under the idle choices.
5. Only touches on the app's own window count as activity for the idle choices; typing on the on-screen keyboard (Android's own window) does not, so typing alone for longer than the limit locks the app, and the typed text is kept (02b decision 9) (your A, step 5). Not chosen: (B) every change to a typed field also counts. The phone check types a note for over a minute.

## 12. Wording to approve (D9)
To be shown on the phone in step 8:
- The gear's TalkBack label "Settings"; the title "Settings"; the section "Security"; the row "Auto-lock" with its current choice.
- The choices: "Every time I leave the app" with "(default)", "After 1 minute idle", "After 5 minutes idle", "Only when the phone locks or the app restarts"; the line "Takes effect at once. Saved."

## 13. Status and notes
- Draft 2026-11-10; approved the same day; decisions 1 to 3 settled.

### 13.1 Steps 1 to 7 (2026-11-10)
- Step 1 (2e26a8c): this file and the plan.md lines (with pull request #154's merge line).
- Step 2 (05bff7e): the look-ups R1 and R2, risk 7 (section 10.1).
- Step 3 (3988ebb): core's `AutoLock` (four choices); `AppLock` with the four rules, the idle time on a monotonic clock passed in, from the last touch or the unlock, the strictest rule until the choice is loaded, a restart locked whatever the choice (decision 4: the phone lock does not lock under the idle choices). AppLockTest 36 (12 kept with the new signatures, 24 new). Red against a stub that kept 01's rule first: 36 tests, 10 failed; 14 new ones passed on the stub, each paired with one that failed. Tests 810 to 834.
- Step 4 (6b4ff46): `RecordStore.loadAutoLock` and `saveAutoLock`; the settings table, one row (`id` 1) with the column `autoLock` (decision 3), whose save changes only that column; database version 8 with `AutoMigration(7, 8)`; 8.json against 7.json: the six tables the same, one new (`settings`). RowMappingTest 3 new: no row is the default, each choice read back, an unknown text unreadable. Red against a stub first: 3 tests, 2 failed (no row passed on the stub's default). Tests 837.
- Step 5 (2285b64): the choice in the screen state, loaded with the records, saved first and applied once saved; `lock/LockHolder.kt` (the lock kept across a turn of the phone, the screen-off receiver on the application context, exported); MainActivity: the choice applied to the lock, a touch as activity (idle checked first), the idle check every second while in view, the time away on return, the screen-off check in `onStop` (decision 5: typing on the keyboard is not activity). AutoLockViewModelTest 11. Red against stubs first: 11 tests, 8 failed. Tests 848.
- Step 6 (8bf37d0): ui/SettingsModel.kt and ui/SettingsScreen.kt (CR2): the Security section with Auto-lock and its value, the choice layer with "(default)" and the note; the gear on More; `ChoiceLayer`'s optional note; `Screen.SETTINGS` with the bottom bar; a choice only from the open layer; the "Iteration 20" strings. SettingsModelTest 4, AutoLockViewModelTest 8 more (19; step 5's choice tests now open the layer first). Red against stubs first: 23 tests, 10 failed. Tests 860.
- Step 7 (fbbc006): SettingsScreenTest.kt (androidTest): AutoLockChoiceFocusTest (AC-61 (b)), AutoLockChoiceLargestFontLandscapeTest (AC-61 (a); font scale 2.0, landscape, upright again at the end), SettingsTalkBackTest 5 (AC-61 (d)). `@Test` lines 44 (were 37).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings. Steps 3 to 5 and 6 and 7 were committed after a later step (your "go", then "commit"), each from its own green run; step 3 from its staged files through a temporary index, as step 5 changed two of them again.

### 13.2 Step 8 (2026-11-10)
- versionCode 29; full run 09:50 to 09:54 green, 860 tests, 0 failures, 1 skipped; lint the 5 listed findings. APK 42,854,731 bytes, built with the price-source key set (checked True/False only).

### 13.3 Step 8: the phone check (2026-11-10, all yes)
versionCode 29 installed over 19's build (versionCode 28; `adb install -r`, Success; lastUpdateTime 2026-11-10 10:02:14, first install 2026-10-29 kept). The screen timeout set to 10 minutes or more for the in-view checks.
- 1 (AC-35, schema 7 to 8): the data opened after the install; no unreadable-data screen: yes.
- 2 (AC-58): the gear on More; Settings with Security and Auto-lock "Every time I leave the app"; the layer with four choices, the first "(default)" and selected, the note; closed by a tap outside; Back to More: yes.
- 3 (AC-06): every time you leave: Home and back at once locks; a turn of the phone locks, as since 01: yes.
- 4 (AC-06; decisions 1, 2, 4 and 5): after 1 minute idle: away about 50 s open, about 70 s locked; untouched in view over 1 minute, the lock screen and the prompt by themselves; a tap every 40 s keeps it open; a turn keeps it open; the phone locked and unlocked within 30 s, still open; typing a note for over 1 minute locks, the text kept: yes.
- 5 (AC-06): after 5 minutes idle: away about 4 min 50 s open, about 5 min 10 s locked; untouched in view over 5 minutes locks: yes.
- 6 (AC-06; risk 7): only when the phone locks or the app restarts: away 6 minutes open, a turn open; the phone locked with the app in view locks; the phone locked with the app in the background, unlocked after about 1 minute and after 10 minutes or more, locked both times: yes.
- 7 (AC-06): a force stop locks; the choice kept after the restart: yes.
- 8 (AC-39, risk 5): largest font and landscape: Settings and the layer readable, every choice and the note reached, the gear shown: yes.
- 9 (AC-39): TalkBack: the gear "Settings, button"; the row "Auto-lock, <choice>, button"; the choices as radio buttons, the default marked; the note read: yes.
- 10 (AC-40): light and dark: yes.
- 11 (spec 9): `adb logcat -d -v threadtime,uid`, the app's lines by uid 10454: 20 lines, 10:58:59 to 11:00:46, from the process start at 10:58:59.602 (`Start proc 29237`): the key not in them nor anywhere in the buffer (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=", amount or "$digit"; tags only the system's window, keyboard and back handling: yes.
- 12 (D9): the wording of section 12 approved as shown.

### 13.4 AC-62 and the screen tests
From 20, the Auto-lock choice layer's window focus, the choices at the largest font in landscape and Settings' TalkBack text (the gear's label and size, the row with its value, the choices with the default marked, the note, a failed save) are checked by SettingsScreenTest.kt (androidTest); the lock's timing on the phone stays a hand check (13.3).
- Pull request #155 at 137a812: the screen tests (run 42747380912) 44 of 44 at the first attempt (tests 192 s, run 03:07:44Z to 03:15:05Z), AutoLockChoiceFocusTest 1, AutoLockChoiceLargestFontLandscapeTest 1 and SettingsTalkBackTest 5 among them, all passing at their first run; CI (run 42747381239, 2m 32s) and CodeQL (run 42747378479) green (gh, read only). AppStartTimingTest's "load and Overall" median 2,604 ms (19's #153: 3,043 ms); 20 adds one read of a one-row table to the start, so this is the emulator's spread.

### 13.5 Review against the spec refs (2026-11-10, by reading main..HEAD; plan.md section 5, item 7)
No finding that needs a change. In order: the four choices of section 9, "every time you leave" the default (`AutoLock`, Settings' model); each choice's rule in `AppLock`, idle as no touch with the time away counting on a clock that does not follow the wall clock (decisions 1, 4 and 5); an in-view lock shows the lock screen and the prompt (decision 2); fails closed: the strictest rule until the choice is loaded, a stored choice that cannot be read is unreadable data and not the default, a restart always locks, a touch after the limit does not reach the screen (risk 1); the choice saved, then applied, in one settings row (decision 3; K2), no write to transactions or cash (K1 not involved); the receiver exported, a screen-off from another app can only lock (10.1); the gear labelled "Settings" and at least 48 dp, Settings' row with its value, the choice layer drawn in the window with the default marked and "Takes effect at once. Saved." (wireframe 10f markers 2, 3 and 7; AutoLockChoiceFocusTest); Back to More (navigation outline); no logging (spec 9) and no floating point (4.10) in the added code. Spec section 8's "a missing row is never an error" is about the backup file's settings (31), not the database.

### 13.6 The merge (2026-11-10)
Pull request #155 merged as e83e6f4 (03:28:20Z), 9 commits; the branch is gone from origin and locally. On the pull request's last commit (ca9feb6) CI (run 42748716581, 3m 50s) and the screen tests (run 42748716771, 44 of 44) green. On main after the merge: CI (run 42749661002, 2m 43s) and CodeQL (run 42749659722) green; the screen tests (run 42749660919) red at attempt 1, 43 of 44: AllocationTalkBackTest.netWorth_showsTheThreeBreakdowns_withEachRowReadInFull found no screen ("No compose hierarchies found"), because the font scale that AllocationChoiceLargestFontLandscapeTest (19) put back reached the emulator about 2 s into it and Android re-created its activity (its log: font scale 1.0 at 03:32:34.552, the activity destroyed at 35.023). Not 20's code: the same order passed twice on #155. Read before the re-run; re-run with your yes (your A): attempt 2 green, 44 of 44 (tests 215 s, 03:39:09Z to 03:47:09Z) (gh, read only; each attempt's results downloaded before the next). The cause is open item 53, to be fixed as a step of 21 without an intent file (your decision; CLAUDE.md section 4).

## 14. Departures from the plan
- Step 3 changed MainActivity's two calls to AppLock (the time passed in) so the app still built; the wiring stayed in step 5. `onAuthSucceeded` takes the time too: an unlock restarts the idle time.
- Step 5: MainActivity is re-created when the phone is turned (no `configChanges` in the manifest), and a new `AppLock` starts locked, so a turn would have locked the app under every choice. The lock is kept in `lock/LockHolder.kt`, a view model that lives as long as the activity's screen state and not past the process (a restart still locks); it also holds the screen-off receiver. Under "every time you leave" a turn still locks, as since 01 (`onStop` runs).
- Step 5: the store opens and Overall shows on the prompt's success, no longer on every composition that finds the app unlocked; a turn that keeps the app unlocked is no unlock (02b decision 9, 16a decision 2).
- Step 5: a choice is saved first and applied once saved, so a choice that could not be saved never applies (my reading of "Takes effect at once. Saved.").
- Step 6: Settings' Auto-lock row shows its value under the name, not beside it as in wireframe 10f frame B, so the longest choice fits at the largest font. A failed save shows the approved line "Not saved. Nothing was changed. Try again." (save_failed) on Settings; no new wording. A choice counts only from the open layer on Settings, so step 5's choice tests now open it first (their checks unchanged). The gear is the text sign U+2699 in its text form inside a 48 dp button, not an icon from a library.
