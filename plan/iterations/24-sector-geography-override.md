# plan/iterations/24-sector-geography-override.md

Stage: Build. Status: approved 2026-11-11 (your A); decisions 1 to 3 settled the same day (section 11). Built 2026-11-11 (steps 1 to 7 and one phone-check fix; section 14); phone check 2026-11-11 all yes (section 13.3); the screen tests 72 of 72 on pull request #170 (section 13.4); reviewed against the spec refs, one finding fixed and one recorded (section 13.5).
Source: plan.md section 4 (row 24), spec 3 (Holding classification), 4.9 (override, effective value, lists), section 6 (view 6, the Classification tab; "More and Settings entry", the Edit picker), C14, AC-22 (the override part), AC-12, AC-61, AC-62; wireframes 06 (frame C, marker 6) and 10f (frame H, markers 16 and 17); iterations 23a and 23b. "My reading" is an interpretation, not a test.

## 1. Outcome
On Holding detail's Classification tab you can override a holding's sector and geography by picking a value from its list, and clear the override again. The tab labels each value "Override", "From the source" or "Unclassified"; Overall, Net Worth (Total) and Allocation use the override at once.

## 2. Spec refs
- 4.9: the effective value is the override if set, else the source's value, else "Unclassified"; the override is edited on Holding detail, picked from the matching list, not typed; a manual value wins until you clear it; clearing returns to the source's value or "Unclassified".
- Section 6, view 6: Classification shows each value labelled "From the source", "Override" or "Unclassified", with Edit (a pick from the list) and Clear override. "More and Settings entry": the Edit picker is one pick from the matching list, no typing, with a line pointing to Settings, Lists (decision 1). Wireframe 10f frame H: a radio list, the pointer line, Save and Cancel; with an empty list one plain line and no Save.
- AC-22 (override part: an override is picked from the list, and clearing it returns the source value or "Unclassified"); AC-12 (the allocation by sector and geography uses these values); C14 (ETFs often unclassified); AC-61, AC-62. Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- Adding, renaming and deleting list values, remembered source texts, Settings, Lists and the picker's pointer line (25; decision 1).
- Overrides in the backup file (31).
- Removing a deleted portfolio's overrides (29; risk 4).

## 4. Analysis (2026-11-11, by reading the code, the spec and the 23a and 23b files)
- A holding is a ticker within a portfolio, so an override belongs to (portfolio, ticker, list), not to the instrument. 23b's `byClass` already takes each holding's value (23b file, 4), so 24 only changes what `HoldingClasses` passes in.
- `ClassOrigin` has SOURCE and UNCLASSIFIED; 24 adds OVERRIDE. `Classification.effective` gains the override, first.
- The lists are in `list_values` (seq, list, value). An override needs a new table naming the value's row (decision 3): database version 11, `AutoMigration(10, 11)`. The write goes through K2 under K10's rule that the value is on its list. K1 is for transactions and cash only (section 14, rule 1).
- View 9 opens Holding detail for a hidden holding; my reading: its override can be edited there too (4.9 counts hidden holdings as users of a value).
- Frame H is a full screen ("< Sector, AAPL"), not a layer: it opens as its own screen, drawn in the activity window, nothing in a separate window (CLAUDE.md pitfall).

## 5. Files
- core/: Classification.kt (`ClassOrigin.OVERRIDE`; `effective` with the override first; setting an override needs a value on its list); RecordStore.kt (load, save and clear an override).
- app/: data/AtsDatabase.kt (the override table, version 11), data/RoomRecordStore.kt, schemas/…/11.json (compared with 10.json); ui: `HoldingClasses` takes the overrides; the Classification tab shows "Override", and an Edit and a Clear override per row (decision 2); the picker screen (frame H) and its model; the state's overrides, loaded at start and changed on save; strings.xml (section 12); build.gradle.kts (versionCode 34).
- Tests: core (the override wins; clearing returns to the source or "Unclassified"; a value not on the list refused); RowMappingTest (an override naming no list row unreadable, decision 3); ViewModel flow (Edit, Save, Cancel, Clear; the breakdowns follow at once; a failed save keeps everything and says so); the tab's and the picker's models; screen tests (AC-61: the tab's TalkBack text with "Override"; the picker's rows and its empty line; the picker at the largest font in landscape).
- Documents: this file, plan.md (section 11 and the change log, with #169's merge line).

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-24-sector-geography-override, one pull request.
1. This file and the plan.md lines (with #169's merge line).
2. K10 in core: the override in `effective` and the rule for setting one; tests first, shown red.
3. The store: the override table, version 11, 11.json compared with 10.json; RowMappingTest first.
4. The state and the ViewModel: load, save and clear, the breakdowns following; tests first.
5. The Classification tab's model and screen (labels, the Edit and Clear override per row) and the picker screen (frame H); models first; the strings.
6. The screen tests.
7. Documents, versionCode 34, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The migration 10 to 11 on the phone: 11.json against 10.json; stop at the unreadable-data screen (CLAUDE.md).
- 2. A failed override save must keep the picker open with the choice and say so; unit tested (the phone cannot force it).
- 3. The picker at the largest font in landscape (AC-39): a screen test and the phone.
- 4. Iteration 29 (delete portfolio) must also remove that portfolio's overrides; noted for 29's file.
- 5. The emulator is not the phone (C19).

## 9. Proof
- Commands: as in 23b, all with --rerun-tasks (CLAUDE.md's pitfall on the intermittent local failure applies). The screen tests on the pull request: 65 plus this iteration's new ones.
- Unit tests: steps 2 to 5.
- The phone check, over 23b's build (versionCode 33; migration to version 11): the data opens; an ETF's sector set to a listed value shows "Override" and Overall by Sector moves its slice at once; Clear override returns it to "Unclassified"; a stock's override over its source value, then cleared, back to "From the source"; Cancel changes nothing; the empty-list line if a list is empty; a hidden holding's override; offline; largest font and landscape; TalkBack; light and dark; logcat as in 23b.

## 10. Research
None.

## 11. Decisions (settled by you 2026-11-11)
1. (Plan) The order stays 24 then 25. Until 25 the picker shows "Choose from the list." without the pointer to Settings, Lists, and an empty list one plain line such as "No sectors yet."; 25 adds the pointer when the screen exists. Wording only (D9), no spec change (your A). Not chosen: (B) 25 built before 24; (C) the wireframe's pointer line now, to a screen not there yet.
2. (Build) An Edit and a Clear override per row: under Sector its own, under Geography its own; Clear override only while that row has an override. One picker screen takes its list (your A). Not chosen: (B) one Edit and one Clear override for both rows, as frame C draws; (C) one picker with both lists and one Save.
3. (Build) An override names its value's row in `list_values` (its seq), so a rename in 25 changes one row and the overrides follow; an override naming no row is unreadable data (your A). Not chosen: (B) the value's text, rewritten by every rename.

## 12. Wording to approve (D9)
To be shown on the phone in step 7:
- The tab: the label "Override"; the buttons "Edit" and "Clear override", read by TalkBack as "Edit sector" and "Clear sector override" (and for geography).
- The picker: its title ("Sector, AAPL"); "Save" and "Cancel"; the line "Choose from the list." (decision 1); the empty-list line ("No sectors yet." and "No geographies yet.").
- TalkBack: a row "Sector, Technology, override"; a picker row "Technology, selected".
- A failed save: the shared "Not saved" line.

## 13. Status and notes
- Draft 2026-11-11; approved the same day; decisions 1 to 3 settled.

### 13.1 Steps (2026-11-11)
- Step 1 (e764cf2): this file and the plan.md lines (with #169's merge line; plan.md section 11 gains a row for 24). Only documents changed. The usual command red twice before green: HoldingsScaleTest ("4 lotsOf | SPREAD: 11.092 ms at 3,000, limit 7.0 ms"; 16b decision B), then the test fixtures (CLAUDE.md's pitfall); the third run, unchanged, green: 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 15:20:03).
- Step 2 (34c3f7d): core/Classification.kt (K10): `ClassOrigin.OVERRIDE`; `effective` takes the override first (a value of the list), else the source's value, else "Unclassified"; `overrideValue`, the rule for setting one: the value on the list that the choice matches (D7), in the list's spelling, or none (refused). Core and the screens work with value texts; the store turns a text into its list row on save and back on load (decision 3; step 3). ClassificationTest 3. Red against stubs first: 3 tests, 2 failed (clearing passed on the stub, which ignores the override and returns the source or "Unclassified"). The usual command green at the first try: 970 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 15:29:19).
- Step 3 (f3c6835): core's `ClassOverride` (portfolio, ticker, list, the value as its list spells it) and `RecordStore.loadOverrides`, `saveOverride` (throws when no value of the list matches; nothing saved) and `clearOverride`. The `class_overrides` table (portfolioId, ticker, list, valueSeq; primary key the first three), its queries in `InstrumentDao` beside the list values, so a save reads the list in its own transaction without repeating the list query open item 57 removed; `toOverrides` (each row read as the value its list row spells; an unknown list or a row naming no value of its own list unreadable, the messages naming no holding or value) and `toRow` (the row of its own list whose value matches, D7, or none). Database version 11 with `AutoMigration(10, 11)`. 11.json against 10.json: the eight tables the same, `class_overrides` new (no index, no foreign key). `FakeStore` keeps overrides as RoomRecordStore does, with `overrideSaveError` failing a save or a clear alone; TickerVerifierTest's store "not used". RowMappingTest 3. Red against stubs first: 3 tests, 3 failed. The usual command red once on the test fixtures (CLAUDE.md's pitfall), then green unchanged: 973 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 16:25:39).
- Step 4 (c1d274b): the screen state's `overrides` (loaded with the records), `classPicker`, `classPickerChoice` and `overrideRefusal`; `Screen.CLASS_PICKER` with the bottom bar (frame H). `HoldingClasses` takes the overrides: a holding's effective value through K10's `effective`, the override first, for the breakdowns and the picker (`valueFor`, `effective`). The ViewModel: `openClassPicker` (on Holding detail; the picker on the value shown when it is on the list, decision 2), `chooseClassValue` (only a value of the list, 4.9), `cancelClassPicker` (nothing changed), `saveClassOverride` and `clearClassOverride` (stored first, then applied, as 22's tab order; a failure keeps the picker and the pick, or the override, and says so; unreadable data shows its screen; a clear with no override ignored). AppContent draws `CLASS_PICKER` with its `else` branch until step 5, and nothing opens it before then. ClassOverrideFlowTest 8 (CR2's wiring included: the stored overrides reach the breakdowns). Red against stubs first: 8 tests, 7 failed (Cancel passed on the stub, which opens nothing, so nothing changed). The usual command red three times: the test fixtures twice (CLAUDE.md's pitfall), HoldingsScaleTest once ("4 lotsOf | SPREAD: 11.280 ms at 3,000, limit 7.0 ms"); then with `--continue`, unchanged, green: 981 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 16:47:47).
- Step 5 (f1e3fa1): the models (CR2). `HoldingDetail.classification` takes the holding's values from `HoldingClasses` (the same as the breakdowns and the picker, overrides included) and `notSaved` after a failed clear; ui/ClassPickerModel.kt: the picker's rows in the list's order with the pick checked, Save while a value of the list is picked and no save runs, none with an empty list (marker 17), `notSaved` after a failed save; the state's `classPickerModel`. The screens: the Classification tab's rows say "Override" (read "Sector, Global, override"), each with its own Edit and, while overridden, Clear override (decision 2; read "Edit sector", "Clear sector override"), the shared "Not saved" line after a failed clear; ui/ClassPickerScreen.kt (frame H): "<" and "Sector, AAPL", radio rows (TalkBack's radio role reads "selected"), "Choose from the list." (decision 1), Save and Cancel, the empty-list line and no Save, "Not saved"; one scrolling screen with the bar, drawn in the activity window, Back cancels. `HoldingActions` gains Edit and Clear with empty defaults, so the screen tests that build it are unchanged. The "Iteration 24" strings (12). Tests: ClassificationModelTest 2 (its helper takes `HoldingClasses`, the expected values unchanged), ClassPickerModelTest 4, ClassOverrideFlowTest 1 (CR2's wiring). Red against stubs first: of the 7 new, 6 failed (the empty list passed on the stub, which has no rows); 23a's two source-value tests also failed on the stub, which dropped the source. The usual command with `--continue`: red once on the test fixtures (CLAUDE.md's pitfall), then every task ran, red only on HoldingsScaleTest ("4 lotsOf | SPREAD: grew 8.6 times, bound 8"), which alone was green (16b decision B): 988 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 17:24:03).
- Step 6 (3d3eed1): the screen tests (AC-61), compiled here and run on GitHub's emulator only, with the pull request. ClassificationTalkBackTest 2 (an override reads "Geography, Global, override" and only its row has Clear override, each Edit and Clear naming its list; Edit sector, Edit geography and Clear geography override each call their own list). New ClassPickerScreenTest.kt: ClassPickerTalkBackTest 3 (the title a heading; the radio rows in the list's order, the pick selected, a tap naming its value; the note; Save on; an empty list's line with no rows and no Save, Cancel on; a failed save's line with the pick kept) and ClassPickerLargestFontLandscapeTest 1 (risk 3: twelve sectors above the bottom bar at font scale 2.0 in landscape, each row scrolled to, shown and tapped, then the note, Save and Cancel). The picker draws no layer or menu and has no fixed part or text field, so no focus, header or keyboard test applies. The screen-tests run expects 71 (65 and these 6; 72 after section 14, item 1). The usual command with `--continue` green at the first try: 988 tests, 0 failures, 1 skipped (no unit test changed); the androidTest code compiled; lint the 5 listed findings (report 19:02:13).
- Each step's run: spotlessCheck, build, tests and lint with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`; lint the 5 listed findings at the end of each step. Each step was committed on your "commit" after its report.

### 13.2 Step 7 (2026-11-11)
- versionCode 34. The usual command with `--continue` was red twice: the test fixtures (CLAUDE.md's pitfall), then, unchanged, HoldingsScaleTest alone ("4 lotsOf | SPREAD: grew 12.1 times, bound 8"; every other task ran: 988 tests, 1 failure, 1 skipped; lint the 5 listed findings, report 19:34:49), which alone was green (16b decision B). 11.json against 10.json before the install: the eight tables' SQL the same, `class_overrides` new with no index and no foreign key (a first count of 1 foreign key was Windows PowerShell's `@($null).Count`; the field is absent).
- The phone-check fix (section 14, item 1): the usual command with `--continue` red once on the test fixtures, then, unchanged, green: 991 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 21:55:06). APK 43,137,807 bytes (21:54:25), built with the price-source key set (checked True/False only). Database version 11.
- The documents (this section, 13.3, line 3 and the plan.md lines): the usual command with `--continue` red once on the test fixtures, then, unchanged, every task ran and only HoldingsScaleTest failed ("4 lotsOf | SPREAD: grew 9.7 times, bound 8"; 991 tests, 1 failure, 1 skipped; lint the 5 listed findings, report 22:10:07), which alone was green (16b decision B).

### 13.3 Step 7: the phone check (2026-11-11, all yes)
versionCode 34 installed over 23b's build (versionCode 33; `adb install -r`, Success), then the fixed build over it (Success; lastUpdateTime 2026-11-11 21:58:14). Checks 1 to 10 on the first build found the Save defect (section 14, item 1); checks 1 to 13 and the new check were done again on the fixed build.
- 1 (AC-35, risk 1): the migration to version 11; the data opened, no unreadable-data screen: yes.
- 2 (view 6, decision 2): a stock "From the source" on its Classification tab; one Edit under Sector and one under Geography, no Clear override: yes.
- 3 (4.9, frame H): Edit under Sector: "<" and "Sector, TICKER", the list's values in order with the current one checked, "Choose from the list.", Save and Cancel; another sector saved: the row shows it with "Override" and Clear override: yes.
- 4 (AC-12): Overall by Sector moved the stock's value to the new slice at once: yes.
- 5 (AC-22): Clear override: back to the source's value, "From the source", no Clear override; Overall by Sector back: yes.
- 6 (frame H): Edit under Geography, a pick, then Cancel, and then Back: nothing changed: yes.
- 7 (C14, AC-22): an ETF "Unclassified": an override saved shows "Override" and Overall by Sector moves it out of "Unclassified"; Clear override: back to "Unclassified": yes.
- 8 (marker 17, decision 1): the empty-list line and no Save, where a list was empty: yes.
- 9: a hidden holding's override shown and kept: yes.
- 10 (AC-42): offline, an override set and cleared: yes.
- New (section 14, item 1), on the fixed build: Edit opens with Save off and the current value checked; another pick turns Save on, the first value again turns it off; Cancel; the same under Geography: yes.
- 11 (AC-39, risk 3): largest font and landscape: the tab and the picker, every row, Save and Cancel reached, nothing cut, the bar kept: yes.
- 12 (AC-61): TalkBack: "Sector, X, override"; "Edit sector", "Clear sector override"; a picker row's value with "selected": yes.
- 13 (AC-40): light and dark, the tab and the picker: yes.
- 14 (spec 9): the uid's buffer held only 13 lines from 21:59:42 (the main buffer reached back about 90 s), so on your A a capture (`adb logcat -v time --uid=10454`, streamed to a file) ran while you did a short round (unlock; Edit, Save and Clear override; Edit and Cancel; Overall by Sector; a refresh): 54 lines, 21:59:42 to 22:05:40, one process (13087), all platform tags (WindowOnBackDispatcher, InsetsController, ImeTracker, VRI, Surface); no AAPL, SPY, "Technology", "Health Care", "Global", "US", "Unclassified", "Override", "finnhub", "token", "country", "sector", "geography", "industry", "http", portfolio name or amount; the key not in the capture (a True/False test); the "$digit" matches the class name `OnBackInvokedInput$createOnBackAnimationCallback$1`; no warning or error line: yes.
- 15 (D9): the 12 "Iteration 24" strings (section 12) approved as shown.

### 13.4 AC-62 and the screen tests (2026-11-11)
- Pull request #170 (created by me on your word, this once; reviewer msfsgh) at e1abf27: the screen tests (run 43036335049, attempt 1, 8m 44s) 72 of 72 test cases in the result XML, 0 failed, 0 skipped, exit-code file 0; the 7 new among them (ClassificationTalkBackTest 2, ClassPickerTalkBackTest 4, ClassPickerLargestFontLandscapeTest 1, 12.0 s). CI (3m 8s), Analyze (actions) and CodeQL green.
- App start: AppStartTimingTest's median of 3 at 300 transactions, load and Overall 1417 ms and Cash 306 ms, against 23b's (#168) 2364 ms and 364 ms: no slowdown from the overrides (the emulator's spread is wide; not a speed-up claim).

### 13.5 Deploy review (2026-11-11)
The branch's code against spec 4.9, view 6, wireframe 10f frame H, sections 9 and 14 and this file.
- Finding 1 (Medium): a failed clear's "Not saved" (`overrideRefusal`) was reset only by the picker's own functions, not by `closeHolding`, `openHolding` or `selectTab`, so after a failed clear on one holding, the next holding opened said "Not saved" on its Classification tab; leaving the picker by a tab in the bar also kept `classPicker` and `classPickerChoice`, so `classPickerModel` was not null off the picker (nothing drew it). Found by reading; a failed write cannot be forced on the phone. Fixed in this pull request on your A (section 14, item 2).
- Finding 2 (Low, recorded): overrides outlive their holding (every transaction reversed, or the portfolio deleted in 29), and spec 8's restore refuses an override for a holding not found; open item 58, for the 29 and 31 files.
- Checked with nothing found: no logging added and no message naming a holding, ticker or value (spec 9); only K10's rule sets an override's value, the store saves it in one transaction against the list, K3 only reads it through `HoldingClasses`, K4's price save unchanged (section 14); the effective value override first, then the source, then "Unclassified", the pick matched under D7 and kept in the list's spelling, per portfolio (4.9, AC-22); one new table (11.json); the lock held in MainActivity, outside the screen state, so a save ending cannot uncover a locked app.

## 14. Departures from the plan
- 1. Phone check, step 7 (2026-11-11): on the Classification tab, Edit then Save at once made the row an override with the same value, for sector and geography alike. The picker opened on the value shown and Save was on with any value of the list picked. Fixed by your A (Build; spec 4.9 allows either, so no spec change): Save is off while the value the row shows is still picked, so it saves only a change; another value can be saved, the source's own included over an override. `ClassPicker.of` takes the value shown (`AppState.classValueShown`, also used to open the picker), and `saveClassOverride` ignores a Save the model does not allow. Tests first: ClassPickerModelTest 1, ClassOverrideFlowTest 2; red against the stub (the value taken, Save unchanged): 3 of 3 failed. ClassPickerTalkBackTest gains 1 (Save off with the shown value picked), so the screen-tests run expects 72. Not chosen: (B) saving the source's own value stores no override and clears one; (C) as built, the unchanged value pinned as an override.
- 2. Deploy review, finding 1 (13.5; your A): `closeHolding` and `openHolding` drop `overrideRefusal`, and `selectTab` drops it with `classPicker` and `classPickerChoice`, so "Not saved" belongs to the visit where the save or clear failed and the picker ends with the holding. Tests first: ClassOverrideFlowTest 2, red against the pull request's code at e1abf27 (2 of 2 failed, on "Not saved" still set after reopening the holding and on the picker's list still set after a tab); then green. A first targeted run stopped in Room's KSP on `java.io.EOFException` before any test ran; the usual command with `--rerun-tasks --continue` then green at its first run: 993 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 22:37:19). No screen changed, so no new screen test and no new phone check; the screen-tests run again on #170.
