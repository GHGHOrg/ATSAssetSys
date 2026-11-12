# plan/iterations/25-manage-lists.md

Stage: Build. Status: approved 2026-11-12; decisions 1 to 4 settled the same day (your A each; section 11).
Source: plan.md section 4 (row 25), spec 3 (Lists, Settings), 4.9 (lists, remembered source texts, rename, delete, matching), section 6 ("More and Settings entry": Settings, Lists, the list screen, the Edit picker's pointer line), section 14 (K10, rule 2), AC-22, AC-58, AC-61, AC-62; wireframe 10f (frame B marker 6, frames F, G and H, markers 12 to 17); iterations 23a, 23b and 24. "My reading" is an interpretation, not a test.

## 1. Outcome
Settings has a Lists section with Sectors and Geographies. Each opens a list screen that shows every value with the number of holdings that use it and the source texts that still map to it after a rename. There you can add a value, rename one and delete one. A rename changes every holding that uses the value at once and remembers the source's text, so a later refresh maps that text to the renamed value instead of adding it again. A delete is refused while any holding uses the value, hidden holdings included. The Edit picker on Holding detail gets its line pointing to Settings, Lists.

## 2. Spec refs
- 4.9: add, rename and delete values; no two values that match (D7) in a list, and no value that matches any remembered text in its list, its own included; a source text maps through the remembered texts first, then by name, else it is added; a remembered text belongs to one value, and no two match in a list; a rename updates every holding using the value and remembers the source's original text; a delete is blocked while any holding uses the value through an override or the source, hidden holdings included, and removes the value's remembered texts; a value keeps the casing it was first given.
- Section 6, "More and Settings entry": Settings' Lists section with Sectors and Geographies; each list screen shows every value with its holdings count (hidden holdings count) and its remembered texts (read only), with Rename, Delete and Add value; a name that matches a value or a remembered text is refused and the typed text stays; the Edit picker's line pointing to Settings, Lists. Wireframe 10f: frame B marker 6 (each list with its number of values), frame F (the list screen), frame G (the Name field and the four messages), frame H (the pointer line, and the empty-list line "No sectors yet. Add one in Settings, Lists.").
- Section 3: Lists (each value with its remembered texts), Settings (the two lists).
- Section 14: K10 is the only writer of the lists, the remembered texts and the overrides (rule 2); K4 passes source texts to K10.
- AC-22 (the rest after 24: a source text not on the list is added; a rename updates the holdings and a refresh does not add the old text again; deleting a value in use is blocked; case and spaces ignored); AC-58 (the Lists part, with its sample-file figures); AC-61, AC-62. Standing checks: AC-09, AC-35, AC-39 to AC-42.

## 3. Out of scope
- The lists and remembered texts in the backup file, and spec 8's restore rules for them (31).
- Removing a deleted portfolio's overrides (29; open item 58). Decision 3 settles only the overrides that a delete of a value meets.
- A typed sector or geography on Holding detail: the override stays a pick (4.9).
- Reordering a list: values stay in the order they were added (23a).

## 4. Analysis (2026-11-12, by reading the code, the spec and the 23a, 23b and 24 files)
- The lists are in `list_values` (seq, list, value); an override names its value's row by seq (24 decision 3), so a rename updates one row and every override follows. No remembered text is stored yet (23a decision 4). They need a table of their own: each text names its value's row, so a rename or a delete reaches them by seq. Database version 12, `AutoMigration(11, 12)`.
- A holding's source text sits on its instrument (`sourceSector`, `sourceGeography`; profile asked once, 23a decision 3), so a holding uses a value through the source when its instrument's text maps to it. One instrument held in two portfolios is two holdings.
- `Classification.valueFor` maps by name only. 25 maps through the remembered texts first, then by name (4.9). `ClassLists` carries each value with its texts, and `HoldingClasses` passes them on, so the tab, the breakdowns and the list screen's counts read one rule.
- My reading of "uses": a holding uses a value when its override names it, or when its instrument's text maps to it, also while an override wins over that text. Deleting such a value would leave the text unmapped once the override is cleared. The holdings counted are those K3 lists (each portfolio and ticker with a lot), whatever their shares, hidden ones included.
- My reading of a name: trimmed, not empty, with no other rule. 4.7's portfolio rules (3 characters, a letter) would refuse the source's own "US". A rename that changes only case or spaces keeps the value and remembers nothing (the old name would match the new one).
- Delete: the wireframe draws no confirmation. My reading: an allowed delete happens at once, and a line confirms it. A value deleted by mistake can be added again.
- K10 stays pure in core (the rules, the refusals, the counts). The store does each add, rename and delete in one database transaction under those rules. The screens call the ViewModel, which calls K10 and the store; nothing else writes the lists (rule 2). K4's price save already adds new texts through K10's rule (23a, your A); 25 makes that rule look at the remembered texts first.
- Frame F is a full screen ("< Sectors"), like the Tab order screen (22). The Name field for Add and Rename (frame G, marker 14) is a layer drawn in the activity window with imePadding(), as `PortfolioNameDialog` is (CLAUDE.md pitfalls: no separate window; the keyboard).
- On the phone, the profile is asked once per instrument, so "a refresh does not add the old text again" shows only with an instrument whose profile is asked after the rename: a buy of a ticker not held before (section 9).

## 5. Files
- core/: Classification.kt (remembered texts on each value; mapping through texts, then name; add, rename and delete with their refusals; the holdings count of a value); RecordStore.kt (add, rename and delete a value; load the texts).
- app/: data/AtsDatabase.kt (the remembered-texts table, version 12; the three writes in one transaction each; the price save mapping through the texts), data/RoomRecordStore.kt, schemas/…/12.json (compared with 11.json); ui: SettingsModel.kt and SettingsScreen.kt (the Lists section), a list screen and its model (frame F), the Name layer and the messages (frame G), ClassPickerModel.kt and ClassPickerScreen.kt (the pointer line and the empty-list line), AppViewModel.kt (the lists with texts and counts; add, rename, delete), AllocationModel.kt (`HoldingClasses` with the texts), AppContent.kt (the new screen), strings.xml (section 12); build.gradle.kts (versionCode 35).
- Tests: core (AC-58's sample figures for both lists; a text mapped through a remembered text before a name; every refusal; a case-only rename; delete blocked by an override, by a source text, by a hidden holding; delete removes the texts); RowMappingTest (a text naming no row, two texts that match, a text that matches a value: each unreadable); ViewModel flows (add, rename, delete; the tab and the breakdowns follow at once; a failed save keeps the screen and the typed text and says so); the list screen's and the picker's models; screen tests (section 9).
- Documents: this file, plan.md (section 11 and the change log, with #173's merge line), open-items.md if decision 3 or 4 changes item 58.

## 6. Order of work
Each step ends with spotlessCheck, build, tests and lint, all with --rerun-tasks, plus `:app:compileDebugAndroidTestKotlin`, and the report. Then I stop. Branch iter-25-manage-lists, one pull request.
1. This file and the plan.md lines (with #173's merge line).
2. K10 in core: remembered texts, the mapping, add, rename and delete with their refusals, and the count; tests first, shown red.
3. The store: the remembered-texts table, version 12, 12.json compared with 11.json; the three writes; the price save through the texts; RowMappingTest first.
4. The state and the ViewModel: the lists with texts and counts, the three actions, the holdings following; tests first.
5. The models and screens: Settings' Lists section, the list screen, the Name layer and its messages, the picker's lines; models first; the strings.
6. The screen tests.
7. Documents, versionCode 35, the phone check, the wording approval, the pull request draft, and the open item 33 reminder.

## 7. Stubs
None.

## 8. Risks
- 1. The migration 11 to 12 on the phone: 12.json against 11.json; stop at the unreadable-data screen (CLAUDE.md).
- 2. The count, the delete block and the breakdowns must read the same rule; one K10 function for all three, unit tested with an override over a source value and with a hidden holding.
- 3. A rename must change the tab, the picker and the breakdowns at once, and the overrides through their seq; a ViewModel test and the phone.
- 4. The Name layer at the largest font in landscape and above the keyboard, keeping MainActivity's focus (AC-61; CLAUDE.md pitfalls): screen tests and the phone.
- 5. Decision 4 changes what K4's price save adds; a held instrument must never add a value that matches a remembered text.
- 6. The emulator is not the phone (C19).

## 9. Proof
- Commands: as in 24, all with --rerun-tasks (CLAUDE.md's pitfall on the intermittent local failure applies). The screen tests on the pull request: 72 plus this iteration's new ones.
- Unit tests: steps 2 to 5, AC-58's sample figures among them.
- Screen tests (AC-61): the list screen and the Name layer at the largest font in landscape; the Name layer keeping the window focus and above the keyboard; the TalkBack text of a list row with its count and texts, and of Settings' list rows.
- The phone check, over 24's build (versionCode 34; migration to version 12): the data opens; Settings, Lists shows each list's number of values; each value's count agrees with Overall by Sector and Geography; adding a value that matches one ignoring case and spaces is refused with the text kept; deleting a value in use is refused, a hidden holding's value too; deleting an unused value works; renaming the source's "US" changes the tab and the breakdowns at once and shows "Also matches: US"; an override follows a rename; a value named "US" is then refused; a buy of a ticker not held before, with the source's "US", maps to the renamed value and adds nothing; the picker's pointer line and, with an empty list, its empty line (if one can be emptied); offline; largest font and landscape; TalkBack; light and dark; logcat as in 24.

## 10. Research
None.

## 11. Decisions (settled by you 2026-11-12)
1. (Build) A rename remembers the old name only when an instrument's stored source text matches it; a value you added yourself remembers nothing, so it can be renamed back (your A). Not chosen: (B) the old name always, except a case-only rename.
2. (Build) Renaming a value back to one of its own remembered texts is refused, as 4.9 says ("whether its own or another value's"); the value takes a third name. No spec change (your A). Not chosen: (B) allowed, the text then no longer remembered, as its own change request (Ruleset A rule 7).
3. (Build) An override of a holding that no longer exists (open item 58) that names the value being deleted is removed by the delete in the same transaction and does not count as a use. Open item 58 stays open for 29 and 31 (your A). Not chosen: (B) it counts as a use and blocks the delete, with no holding to change.
4. (Build) Each price save adds, through K10's rule, the texts of held instruments (hidden ones included) that map to no value, so a deleted value comes back only when a holding needs it (your A). Not chosen: (B) as before: the holding shows the source's text, not on the list, until its value is added by hand.

## 12. Wording to approve (D9)
To be shown on the phone in step 7 (wireframe 10f's placeholders as the first proposal):
- Settings: the section "Lists"; the rows "Sectors" and "Geographies" with "2 values" (one: "1 value"), read by TalkBack as "Sectors, 2 values".
- The list screen: its titles "Sectors" and "Geographies"; per value "Used by 1 holding." (or "holdings"), "Also matches: Information Technology"; the buttons "Rename", "Delete" and "Add value", read by TalkBack as "Rename Technology" and "Delete Technology"; an empty list's line.
- The Name layer: its label "Name", "Save" and "Cancel".
- The messages (frame G): "Not saved. A value named "Technology" already exists. Case and spaces are ignored."; "Not saved. "Tech" is already a remembered source text for Technology. Case and spaces are ignored."; "Not deleted. Technology is used by 1 holding (hidden holdings count). Change that holding first."; "Renamed. Source text "Tech" still maps to Technology."; an empty name's refusal; "Deleted." and "Added."; a failed save: the shared "Not saved" line.
- The picker: "Choose from the list. To add a value, use Settings, Lists."; "No sectors yet. Add one in Settings, Lists." (and for geographies).

## 13. Status and notes

### 13.1 Steps (2026-11-12)
- Step 1: this file and the plan.md lines, commit 1def82a.
- Step 2: K10 in core (Classification.kt): `RememberedText` and `ClassLists.remembered`; the mapping through the remembered texts, then by name (`valueFor`, `withSource`, `effective`, `additions`); `holdingsUsing`, `add`, `rename` and `delete` returning a `ListResult` or a `ListRefusal`. ClassListEditTest's 22 tests first, red (22 failed, the other 383 core tests passed), then green; 1,024 tests. My reading of AC-58's rename sentence: after Technology is renamed Tech, a new "Tech" is refused because Tech is now a value's name (a duplicate); with AC-58's sample nothing else is remembered, as no source text is "Technology" (decision 1). 4.9's own example (the source's "Tech" renamed Technology, then a new "Tech" refused as a remembered text) is tested as written.
- Step 3: the store. The `source_texts` table (seq, list, valueSeq, text), database version 12 with `AutoMigration(11, 12)`; 12.json against 11.json: the nine tables the same, one new table. `toClassLists` reads the remembered texts and refuses a text naming no value of its list, matching a value or matching another text; the DAO's add, rename and delete each in one transaction through pure functions (`addedRow`, `renamedRows`, `deletedSeq`, K10 run against the stored lists); a delete refuses when an override not listed by K10 names the value (decision 3); `savePrices` takes the held tickers and adds their stored texts that map to no value (`profilesToAdd`, decision 4); `RecordStore` gains the three writes and `held` (section 14, item 1). The ViewModel passes no held tickers until step 4. RowMappingTest's 7 new tests first, red (7 failed, the other 39 passed), then green; 1,031 tests. The local runs met CLAUDE.md's intermittent failure three times: twice `:app:kspDebugKotlin` alone ("references a type that is not present", passing when run alone), once with ScaleHistories.kt's unresolved core classes (exit code 255, no new crash log); each time the same command passed when re-run unchanged.

## 14. Departures from the plan
1. Step 2 (2026-11-12): RecordStore's add, rename and delete and the remembered texts' load move from step 2 to step 3. A new method in core's interface would leave RoomRecordStore and the test stores unbuilt until step 3.
