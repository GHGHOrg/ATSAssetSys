# plan/iterations/22-tab-order.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 to 3 settled the same day (section 11). Built 2026-11-10 (steps 1 to 7; section 14); phone check 2026-11-10 all yes (section 13.3); not yet merged.
Source: plan.md section 4 (row 22), spec section 6 (navigation outline: the default order More, Cash, Portfolios, Overall, reordered in settings; More and Settings entry: Navigation has the tab order, changed with Up and Down buttons; accessibility), section 3 (the tab order is a setting), section 8 (the `tab_order` settings key and its text form; in the backup file, 31), section 14 (K2, K9), AC-13, AC-58 (the tab order part), AC-61, AC-62; wireframe 10f (frame B marker 5, frame E marker 11); iteration 02b (the bottom bar, `DEFAULT_TAB_ORDER`), 20 (Settings, the settings row, database version 8), 21 (`LayerCard`); CLAUDE.md (Room schema files; screen models; screen tests). "My reading" is an interpretation, not a test.

## 1. Outcome
Settings gains a Navigation section with the row "Tab order", showing the current order ("More, Cash, Portfolios, Overall"). It opens the Tab order screen: one row per tab, numbered left to right, each with Up and Down. A tap moves that tab one place; the order is saved and the bottom bar follows at once. The order stays after leaving the app and after a restart.

## 2. Spec refs
- Section 6, navigation outline: four tabs, default order More, Cash, Portfolios, Overall, reordered in settings; the app still opens on Overall after unlock, wherever Overall sits.
- Section 6, More and Settings entry; wireframe 10f frame B marker 5 ("Navigation: tab order, with the current order as text"), frame E marker 11 (one row per tab with Up and Down, not dragging, so it works with TalkBack; "Left to right in the bottom bar. Takes effect at once."; the bottom bar follows at once; a setting in the backup file).
- Section 3: the tab order is a setting. Section 8: the key `tab_order`, the four tabs separated by semicolons, each once, case ignored, default `more;cash;portfolios;overall`; a tab order that is not each tab once is a state-row error (used by 31 and 33).
- Section 14: K2 (the store), K9 (the screens).
- AC-13 (the tab order can be changed in settings), AC-58's tab order part ("changes with Up and Down and the bottom bar follows at once, and the order is in the backup file": the backup part is 31's), AC-61, AC-62.
- Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The backup file and restore (31, 33 to 35): this iteration stores the order in the text form of spec 8 so that 31 can write it as it is, but writes no file.
- The price source (30) and the lists (23 to 25): Settings shows only what is built.
- Dragging (the wireframe rules it out).

## 4. Analysis (2026-11-10, by reading the code and the spec)
- The bar draws `BottomBar(DEFAULT_TAB_ORDER, ...)` in AppContent.kt; `BottomBar` already takes the order as a list (02b), so the bar itself does not change.
- `Tab` and `DEFAULT_TAB_ORDER` are in app's AppViewModel.kt. The store's interface (`RecordStore`) is in core and the "each tab once" rule is also 31's and 33's, so core needs the four tabs (decision 3).
- The settings table has one row with a column per setting (20 decision 3): a nullable `tabOrder` column, null meaning the default, is added in database version 9 by `AutoMigration(8, 9)`, as 16a added nullable columns. Stored as spec 8's text (`more;cash;portfolios;overall`), so 31 writes it unchanged. A stored text that is not each tab once is unreadable data, as an unknown auto-lock choice is (20).
- Each Up or Down is one save; the order shown and the bar follow only once it is saved, as the auto-lock choice does (20), and taps while a save runs are ignored.
- Frame E is a screen of its own ("< Tab order"), reached from Settings; Back returns to Settings. The bar stays visible on it, so the change shows at once.
- TalkBack: after a move, the moved tab's buttons are elsewhere; where the focus lands is decision 2.

## 5. Files
- core/: Tab.kt (the four tabs, moved from app, decision 3); TabOrder.kt (each tab once; move up and down; the default; spec 8's text form, case ignored, read and written); RecordStore.kt (`loadTabOrder`, `saveTabOrder`); testFixtures' store.
- app/: data/AtsDatabase.kt (the `tabOrder` column, version 9, `AutoMigration(8, 9)`, `SettingsDao.saveTabOrder`); data/RoomRecordStore.kt; schemas/…/9.json (new, Room's); ui/AppViewModel.kt (the order in the state, `Screen.TAB_ORDER`, open, close, move); ui/AppContent.kt (the bar from the state); ui/SettingsModel.kt and ui/SettingsScreen.kt (the Navigation section); ui/TabOrderModel.kt and ui/TabOrderScreen.kt (new, frame E); strings.xml (section 12); build.gradle.kts (versionCode 31); the files that use `Tab` (an import, decision 3).
- Tests: TabOrderTest (core: default; each tab once; Up on the first and Down on the last change nothing; the text form both ways, case ignored, refused when a tab is missing, twice or unknown); RowMappingTest (no row or no value is the default; each order read back; a bad text unreadable; saving the order keeps the auto-lock choice and the other way round); the ViewModel wiring (loaded at start; a move saved, then applied; a failed save keeps the order and says so; ignored while saving; the bar follows); TabOrderModelTest and SettingsModelTest; screen tests (section 9).
- Documents: this file, plan.md (section 11 and the change log, with #158's and #159's merge lines).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-22-tab-order, one pull request.
1. This file and the plan.md lines (with #158's and #159's merge lines).
2. core: `Tab` moved to core (decision 3), `TabOrder`; tests first, shown red against a stub.
3. The store: `loadTabOrder`, `saveTabOrder`, the column, version 9, 9.json compared with 8.json; RowMappingTest first, shown red.
4. The ViewModel: the order loaded at start and given to the bar, the Tab order screen's open and close, a move saved and then applied; tests first, shown red.
5. Settings' Navigation section, the Tab order screen and the strings; model tests first, shown red.
6. The screen tests.
7. Documents, versionCode 31, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. A stored order that is not each tab once would draw a wrong bar: core refuses it, and the store reports unreadable data (as 20); no stored text means the default.
- 2. The migration 8 to 9 on the phone: 9.json compared with 8.json (each table's SQL) before the install; stop at the unreadable-data screen if it shows (CLAUDE.md; open item 34).
- 3. A save between taps: the order shown is always the saved one; taps during a save are ignored, so two fast taps cannot leave the bar and the store apart.
- 4. TalkBack after a move (decision 2): the focus could land on another tab's button, and a second tap would then move the wrong tab. A phone check.
- 5. The app must still open on Overall after unlock wherever Overall sits (spec 6); the selected tab must not change when the order does.
- 6. Saving the order must not touch the auto-lock choice in the same row, and the other way round (RowMappingTest).

## 9. Proof
- Commands: as in 21, all with --rerun-tasks. The screen tests on the pull request: 52 plus this iteration's new ones.
- Unit tests: steps 2 to 5.
- Screen tests (AC-61): (d) the TalkBack text of the Tab order rows (each carries its position) and of Settings' Tab order row; the bar's order after a Down on the Tab order screen. The screen has no layer, no text field and no fixed part, so (a) to (c) do not apply.
- The phone check, over 21's build: the data opens after the install (AC-35, the migration to version 9); Settings' Navigation row with the default order; the Tab order screen as frame E; Up and Down move a tab and the bar follows at once; Up on the first and Down on the last do nothing (decision 1); the order kept after Home and after a force stop and restart; the app opens on Overall wherever it sits; TalkBack: each row and button, and where the focus lands after a move (decision 2); largest font and landscape; light and dark; logcat as in 21.

## 10. Research
- None planned. Adding a nullable column by `AutoMigration` is what 16a did for version 7; 9.json shows the result. No new library and no version change.

## 11. Decisions (Build, settled by you 2026-11-10)
1. Up on the first row and Down on the last are shown but inactive, read by TalkBack as unavailable, so every row has the same two buttons as in frame E (your A). Not chosen: (B) left out.
2. TalkBack after a move: the rows are keyed by tab so the focus moves with the tab, and the row reads its new place ("More, position 2 of 4") (your A). Whether the focus really follows is a phone check. Not chosen: (B) nothing special, the focus staying where it was, on another tab's button.
3. `Tab` moves to core: one type for the bar, the store and later the backup file (31), and an import line in the app files that use it (your A). Not chosen: (B) a tab type of core's own, mapped to app's. User-defined tabs (your question) would replace the enum under either option and need a change request (spec 6 and 8 fix four tabs).

## 12. Wording to approve (D9)
To be shown on the phone in step 7:
- Settings: the section "Navigation"; the row "Tab order" with the order, for example "More, Cash, Portfolios, Overall".
- The screen's title "Tab order"; the rows "1. More" to "4. Overall" with "Up" and "Down"; the line "Left to right in the bottom bar. Takes effect at once."
- TalkBack: the Settings row "Tab order, More, Cash, Portfolios, Overall"; a row "More, position 1 of 4"; the buttons "Move More up" and "Move More down".
- A failed save: the shared "Not saved" line (save_failed), as for the auto-lock choice.

## 13. Status and notes
- Draft 2026-11-10; approved the same day; decisions 1 to 3 settled. Open item 54 recorded (section 14).

### 13.1 Steps (2026-11-10)
- Step 1 (8b55191): this file and the plan.md lines (with #158's and #159's merge lines).
- Step 2 (34cf12f): core/Tab.kt, the four tabs moved from app's AppViewModel.kt (decision 3), each with its key in spec 8's text; core/TabOrder.kt: each tab once, made only by `of` or `parse`; Up and Down one place, the first's Up and the last's Down changing nothing (decision 1); the default; spec 8's text both ways, case ignored, refused when a tab is missing, twice or unknown. The `Tab` import in 28 app files (11 androidTest, 3 main, 14 unit tests), nothing else in them changed. TabOrderTest 10. Red against a stub first: 10 tests, 8 failed (the two refusal tests passed on the stub, which refuses everything). One expected value, in `moves_keepEachTabOnce`, was worked out wrong by hand; the code's Portfolios, Cash, Overall, More is right (the steps are in the test), so the value was corrected and no assertion removed. Tests 893. HoldingsScaleTest red in one full run (1 holdings, SPREAD: grew 20.4 times, bound 8; 70.8 ms at 3,000, limit 65.0 ms), re-run unchanged and green (16b's decision, your B).
- Step 3 (2b808f9): `RecordStore.loadTabOrder` and `saveTabOrder`; the settings row's nullable `tabOrder` column holding spec 8's text, null the default order, a stored text that is not each tab once unreadable data; `withAutoLock` and `withTabOrder` change one column of the row, or make the row with the other setting at its default, and the DAO's two saves use them (`saveAutoLock` as before); database version 9 with `AutoMigration(8, 9)`. 9.json against 8.json: the six other tables the same, `settings` with the one new column `tabOrder TEXT` (nullable). RowMappingTest 5 new. Red against stubs first: 5 tests, 4 failed (no row passed on the stub's default). Tests 898.
- Step 4 (ceffbb0): the order in the screen state (`tabOrder`, the default until loaded, and `tabOrderRefusal`), loaded with the records; `Screen.TAB_ORDER` with the bar, opened from Settings only and not over a layer, Back to Settings; Up and Down saved, then applied, ignored off that screen, during a save and when nothing moved; a failed save keeps the order and says so, unreadable data shows its screen; a tab from the screen shows that tab, the selected tab stays on a move, and an unlock opens Overall wherever it sits. The app's bar draws `state.tabOrder`; `DEFAULT_TAB_ORDER` stays for screen tests drawn without the state. Until step 5 the screen has no drawing of its own and nothing opens it. TabOrderViewModelTest 14. Red against stubs first: 14 tests, 7 failed; one of the passes, the unreadable save, came from the start's price refresh, which `FakeStore`'s shared `saveError` also fails, so the screen was unreadable before any move. `FakeStore` gained `tabOrderSaveError` for this save alone, and that test and the tab test now first check that the Tab order screen is open: 14 tests, 8 failed. Tests 912.
- Step 5 (651b5b3): ui/TabOrderModel.kt (CR2): one row per tab with its place, the count and whether Up and Down move it; the "Not saved" line. Settings' model gains the order in force. ui/TabOrderScreen.kt (frame E): "<" and the title, the rows "1. More" with Up and Down (inactive on the first's Up and the last's Down, decision 1), each row keyed by its tab (decision 2), read as "More, position 1 of 4" with "Move More up" and "Move More down"; the note line; the "Not saved" line. Settings' Navigation section with the Tab order row and the order as its value, filled into one string, so the screen joins nothing itself. The tab names come from the bar's own (`labelRes`, now internal). The "Iteration 22" strings. TabOrderModelTest 4, SettingsModelTest 1 more, TabOrderViewModelTest 1 more (the models follow a move). Red against stubs first: 6 tests, 6 failed. Tests 918.
- Step 6 (e1c58fa): TabOrderScreenTest.kt (androidTest): TabOrderTalkBackTest 4 (AC-61 (d): Settings' row reads the order in force; each row reads its place and its Up and Down name its tab; the first's Up and the last's Down inactive; a failed save says so) and TabOrderBarTest 1 (on MainActivity, unlocked: More, the gear, Tab order, Down on More puts Cash left of More in the bar, Up puts the default back for later tests). Compiled here; they run on GitHub's emulator on the pull request. `@Test` lines 57 (were 52).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step. Steps 1 to 3 were committed after step 3 (your "commit"), each from its own versions through a temporary index, each diff stat checked against its step's report first; steps 4 to 6 each after its step.

### 13.2 Step 7 (2026-11-10)
- versionCode 31; full run 19:52:51 to 19:54:19 green, 918 tests, 0 failures, 1 skipped; lint the 5 listed findings. APK 42,946,475 bytes, built with the price-source key set (checked True/False only). Database version 9 (9.json compared with 8.json in step 3).

### 13.3 Step 7: the phone check (2026-11-10, all yes)
versionCode 31 installed over 21's build (versionCode 30; `adb install -r`, Success; lastUpdateTime 2026-11-10 20:11:00, first install 2026-10-29 kept).
- 1 (AC-35, risk 2): the data opened after the install and the migration to version 9; the app on Overall, the bar More, Cash, Portfolios, Overall: yes.
- 2 (AC-58, marker 5): More, the gear: Settings' Navigation section with "Tab order" and the default order: yes.
- 3 (frame E, decision 1): the Tab order screen's four numbered rows with Up and Down, the note line, the bar under it; More's Up and Overall's Down inactive and doing nothing: yes.
- 4 (AC-58): Down on More: the rows and the bar Cash, More, Portfolios, Overall at once; More still the selected tab: yes.
- 5: Up on Overall three times: Overall, Cash, More, Portfolios; Overall's Up and Portfolios' Down inactive: yes.
- 6: "<" to Settings with the new order in the row; Back to More: yes.
- 7: Home and back: the order kept: yes.
- 8 (risk 5, spec 6): Overall moved to the third place; after a force stop and restart the order kept and the app opened on Overall: yes.
- 9 (decision 2): TalkBack: each row read with its place, each button with its tab, the first's Up unavailable; after "Move More down" the focus stayed on that button, the row read its new place, and a second double-tap moved More again: yes.
- 10: TalkBack: Settings' row read with the order; the bar's tabs read in the new order: yes.
- 11 (AC-39): largest font and landscape: the Tab order screen's rows, names, Up and Down and Settings' Navigation row reached in full: yes.
- 12 (AC-40): light and dark: yes.
- 13: the order set as you keep it: yes.
- 14 (spec 9): `adb logcat -d -v threadtime,uid`, the app's lines by uid 10454: 88 lines, 20:45:41.737 to 20:49:25.519, with the process start at 20:49:03.656 (`Start proc 10690`) and 61 lines after it, through a force stop, an unlock and a move. The key not in the buffer (a True/False test); no "finnhub", "token", AAPL, "quote", "symbol=", "http", "portfolio", "ticker", the tab order's text or any amount; the "$digit" matches are the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; one `hiddenapi` note from Compose; `E/BiometricPromptCompat` "Unable to cancel authentication. BiometricFragment not found." at the force stop with no prompt up (as in 19 and 21): yes.
- 15 (D9): the wording of section 12 approved as shown.
- Not on the phone: a failed save, which cannot be forced there; unit tested (step 4).

## 14. Departures from the plan
- Step 3: the DAO's `saveAutoLock` now goes through `withAutoLock`, with the same effect, so both saves are pure functions RowMappingTest can check; section 5's "testFixtures' store" was not touched (the stores the tests use are `FakeStore` in AppViewModelTest.kt and TickerVerifierTest's own).
- Step 4: `FakeStore` gained `tabOrderSaveError`, which fails the tab order's save alone (section 13.1, step 4). 20's matching auto-lock test may pass for the wrong reason; recorded as open item 54, to be fixed in its own iteration after 22 (your decision, a one-off; CLAUDE.md section 4's rule stays).
- Step 5: the bar's tab names (`labelRes` in BottomBar.kt) became internal, so the bar, Settings' row and the Tab order screen use the same strings; Settings' row value is one string with the four names filled in.
