# plan/iterations/24-sector-geography-override.md

Stage: Build. Status: approved 2026-11-11 (your A); decisions 1 to 3 settled the same day (section 11).
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
- Step 1: this file and the plan.md lines (with #169's merge line; plan.md section 11 gains a row for 24). Only documents changed. The usual command red twice before green: HoldingsScaleTest ("4 lotsOf | SPREAD: 11.092 ms at 3,000, limit 7.0 ms"; 16b decision B), then the test fixtures (CLAUDE.md's pitfall); the third run, unchanged, green: 967 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 15:20:03).
- Step 2: core/Classification.kt (K10): `ClassOrigin.OVERRIDE`; `effective` takes the override first (a value of the list), else the source's value, else "Unclassified"; `overrideValue`, the rule for setting one: the value on the list that the choice matches (D7), in the list's spelling, or none (refused). Core and the screens work with value texts; the store turns a text into its list row on save and back on load (decision 3; step 3). ClassificationTest 3. Red against stubs first: 3 tests, 2 failed (clearing passed on the stub, which ignores the override and returns the source or "Unclassified"). The usual command green at the first try: 970 tests, 0 failures, 1 skipped; lint the 5 listed findings (report 15:29:19).

## 14. Departures from the plan
None yet.
