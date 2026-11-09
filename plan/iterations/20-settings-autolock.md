# plan/iterations/20-settings-autolock.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 to 3 settled the same day (section 11). In build: step 1.
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

## 9. Proof
- Commands: as in 19, all with --rerun-tasks. The screen tests on the pull request: 37 plus this iteration's new ones.
- Unit tests: steps 3 to 6.
- The phone check, over 19's build (AC-35 with the schema change): the data opens after the install; the gear and Settings; each of the four choices, chosen and then: leave and return at once; stay away just under and just over 1 minute (and 5); stay in the app untouched over the limit; lock the phone and return; force stop and restart; the choice kept after a restart; largest font and landscape; TalkBack; light and dark; logcat as in 19.

## 10. Research (step 2; each look-up shown to you first, CLAUDE.md section 8)
- R1: Android's screen-off broadcast for an app registering it in code on API 33 to 37: whether it is delivered while the app is in the background, and which receiver flag API 34 and later need. The developer.android.com reference pages could not be fetched before (CLAUDE.md); I would ask you to paste the parts of the `Intent.ACTION_SCREEN_OFF` and `Context.registerReceiver` pages, or check the flags with javap on the cached jars. The phone check stays the proof.
- R2: Room's migration test helper (`MigrationTestHelper`) for Room 3.0.3 (androidx.room3): whether it exists and works on the JVM; javap on the cached jar. Without it, the migration is checked by the install on the phone (AC-35), as in 16a.
- No version change is planned: no new library.

## 11. Decisions (Build, settled by you 2026-11-10)
1. "Idle" for the 1- and 5-minute choices is no touch in the app for that long, the time away counting as idle: the app also locks while it is in view and untouched (your A). Not chosen: (B) only the time away, checked on return.
2. When the idle time runs out while the app is in view, the lock screen shows with the biometric prompt at once, as when you return (your A). Not chosen: (B) the lock screen with its Unlock button only.
3. Settings are stored as one row with a column per setting; each later setting adds a column and a schema version (your A). Not chosen: (B) name and value rows.

## 12. Wording to approve (D9)
To be shown on the phone in step 8:
- The gear's TalkBack label "Settings"; the title "Settings"; the section "Security"; the row "Auto-lock" with its current choice.
- The choices: "Every time I leave the app" with "(default)", "After 1 minute idle", "After 5 minutes idle", "Only when the phone locks or the app restarts"; the line "Takes effect at once. Saved."

## 13. Status and notes
- Draft 2026-11-10; approved the same day; decisions 1 to 3 settled.

## 14. Departures from the plan
None yet.
