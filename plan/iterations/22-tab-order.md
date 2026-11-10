# plan/iterations/22-tab-order.md

Stage: Build. Status: approved 2026-11-10 (your "Approved"); decisions 1 to 3 settled the same day (section 11).
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
- Draft 2026-11-10; approved the same day; decisions 1 to 3 settled.

### 13.1 Steps (2026-11-10)
- Step 1: this file and the plan.md lines (with #158's and #159's merge lines).
- Step 2: core/Tab.kt, the four tabs moved from app's AppViewModel.kt (decision 3), each with its key in spec 8's text; core/TabOrder.kt: each tab once, made only by `of` or `parse`; Up and Down one place, the first's Up and the last's Down changing nothing (decision 1); the default; spec 8's text both ways, case ignored, refused when a tab is missing, twice or unknown. The `Tab` import in 28 app files (11 androidTest, 3 main, 14 unit tests), nothing else in them changed. TabOrderTest 10. Red against a stub first: 10 tests, 8 failed (the two refusal tests passed on the stub, which refuses everything). One expected value, in `moves_keepEachTabOnce`, was worked out wrong by hand; the code's Portfolios, Cash, Overall, More is right (the steps are in the test), so the value was corrected and no assertion removed. Tests 893. HoldingsScaleTest red in one full run (1 holdings, SPREAD: grew 20.4 times, bound 8; 70.8 ms at 3,000, limit 65.0 ms), re-run unchanged and green (16b's decision, your B).

## 14. Departures from the plan
None yet.
