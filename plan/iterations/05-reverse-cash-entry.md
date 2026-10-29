# plan/iterations/05-reverse-cash-entry.md

Stage: Build. Status: built on branch iter-05-reverse-cash-entry (steps 1 to 5 and one fix, commits b73d5fe to ce1c6fd) and checked on the phone 2026-10-29, all checks yes (section 13). Pull request not created; CI not run. Decisions 1 to 4 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
Source: plan/plan.md section 4 (row 05), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On the Cash tab I tap an entry to see its details. On a deposit, withdrawal, interest/dividend or adjustment I can tap Reverse: the app saves an opposite Adjustment linked to it, dated the original's date or a later one, unless that would make cash negative. Both entries stay, and each shows the link to the other.

## 2. Spec refs
- spec 4.6: no edit or delete; a reversal is all-or-nothing and linked; follows 4.1; cannot be reversed itself; at most once per entry; the amount is fixed from the stored value; the date defaults to the original's, later allowed, earlier refused; a cash reversal is an Adjustment with the opposite amount and a pre-filled note; never the opening balance or a trade line.
- spec 4.1: the replay after every entry (03 decision 5).
- spec 6, view 7: a direct entry opens its detail, where a reversal starts. Wireframe 10b: frame H (the detail), frame I marker 19 (the cash reversal), markers 15 to 20, and frame J's "Reversal date: not earlier than the original (<date>)."
- Criteria: 53 (part: "Reverse is not offered on the opening balance, a reversal, an already reversed entry or a trade line"; a reversal dated before the original is refused), 9, 35, 39 to 42.
- spec 14: K1, K5, K9.

## 3. Out of scope
- Reverse and re-enter (14). Reversing buys, sells and splits (13). Trade lines and their detail (09).
- Filters (06). Criterion 37: still deferred.

## 4. Files
Changed, core/: CashEntries.kt (a new reverse(existing, id, originalId, date, note): the original exists; it is not the opening balance, a trade line or a reversal; it is not already reversed; the date is not before the original's; the note is not blank and has no line break; then the replay. The new entry is an Adjustment with reversesId set to the original); CashEntriesTest.kt.
Changed, app/: AppViewModel.kt (new screens DETAIL and REVERSAL, the chosen entry, the reversal form: date and note); new CashEntryDetailScreen.kt (the detail and the reversal screen); CashScreen.kt (rows can be tapped; a row shows its note, decision 3); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 5); AppViewModelTest.kt.
Documents: plan.md, open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-05-reverse-cash-entry from main (dc2549e). Baseline run.
2. core/: tests first (reversing a deposit, withdrawal, interest or adjustment gives the opposite Adjustment with reversesId and the next seq; the opening balance, a trade line, a reversal or an already reversed entry is refused; a date before the original is refused naming it, a later date is allowed; a blank note is refused; a reversal that makes later cash negative is refused with the date and shortfall; the amount comes from the stored value), then the code.
3. ViewModel: tests first (opening the detail; which actions are offered; opening the reversal with the defaults; confirm saves one row and returns to the detail, now showing the link; refusals keep the typed date and note; Back moves reversal to detail to Cash), then the code.
4. Screens: tappable rows with the note line, the detail, the reversal screen, the messages.
5. versionCode 5. Full run, release build, install over 04's build with data.
6. Phone checks (section 8).
7. Documents and the pull request draft.

## 6. Stubs
- "Reverse and re-enter" is not shown until 14 (decision 2).

## 7. Risks
- A sign error would double the mistake instead of cancelling it. Tests use the stored amount for all four types, in both signs.
- "At most once" depends on finding an existing reversal by reversesId. A test covers it, and the button is hidden as well.
- Reversing an old deposit can block because of later withdrawals. The message must name the right date (tested with the phone's own data, check 7).
- New screens: Back paths and the bar on the detail and reversal screens. My reading: the bar shows there too, as on the form (02b decision 1).
- A reversal row reads "Adjustment"; without its note it would be hard to tell apart (decision 3).

## 8. Proof
Commands: as in 04. CI green on the pull request.
By hand on the phone, with 04's data:
1. Tap each row type: the detail shows the type, the date with ET, the signed amount, the note and "Not reversed.", with a Reverse button.
2. The opening balance's detail shows "The opening balance cannot be reversed." and no button.
3. Reverse the Interest/dividend +$12.34 with the default date: the reversal screen shows "Adjustment (fixed)", −$12.34, the pre-filled note "Reversal of the interest/dividend of <date> ET" and the date. Confirm: back on the detail, which now shows "Reversed on <date> ET" with a link and no Reverse button.
4. Open the new reversal row: it shows "Reverses: Interest/dividend, <date> ET" with a link, and no Reverse button.
5. A reversal date before the original's is refused with "Reversal date: not earlier than the original (<date>)."
6. A reversal dated later than the original is saved; the Cash rows and balance are right.
7. Reverse the Jan 5 deposit of $500.00 dated 2026-01-05: refused with "Not saved. Cash would be negative on 2026-01-10 by $300.00." (my arithmetic: 1,000 to 500 on Jan 6, to −300 after the Jan 10 withdrawal).
8. Clearing the reversal note gives "Note: required for an adjustment."
9. Install over the 04 build with data: the data is still there (criterion 35).
10. Largest font, light and dark, TalkBack on the tappable rows, the detail, the links and the reversal screen.
11. Logcat while saving a reversal: no amount or note text.

## 9. Research
None planned; no new library.

## 10. Decisions (all Build, settled by you 2026-10-29)
1. The reversal note is pre-filled and editable, but not blank (the adjustment rule of 04 decision 1).
2. "Reverse and re-enter" is hidden until 14.
3. The Cash rows show the note as a third line (wireframe 07 marker 4); TalkBack reads it.
4. Confirm returns to the original's detail, which then shows "Reversed on <date> ET".

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Detail: the title is the type (for example "Deposit")*; rows Type*, Date*, Amount*, Note*; "Not reversed."*; "Reverse"*; "The opening balance cannot be reversed."*
- Links (mine): "Reversed on <date> ET" on the original; "Reverses: <Type>, <date> ET" on the reversal.
- The reversal screen: the title "Reverse <type>" (for example "Reverse deposit")*; "Reverses"*; "Type: Adjustment (fixed)"*; "Amount" (opposite, fixed)*; "Reversal date"* with "Enter date in US market time (New York). Not earlier than <date>."*; "Cash effect"*; "Confirm and save"*; "Cancel"*.
- The pre-filled note: "Reversal of the <type> of <date> ET"*, the type in lower case (deposit, withdrawal, interest/dividend, adjustment).
- Message: "Reversal date: not earlier than the original (<date>)."*
Approved on paper with this file; confirmed on the phone (D9).
Confirmed on the phone 2026-10-29 (checks 1 to 8 and 10): the detail, the links, the reversal screen and the messages as written above. TalkBack reads a row's note at the end of its label, with no lead-in word (section 14, item 2).

## 12. Status and notes
Built 2026-10-29; phone checks 2026-10-29. Pull request and CI: not yet.

## 13. Build results
### 13.1 Commits
Newest first: ce1c6fd fix, c40a8db step 5, 1c78aae step 4, c977ccc step 3, 43312bf step 2, b73d5fe iteration file. main was dc2549e.

### 13.2 Local run
On ce1c6fd's code (2026-10-29, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 184 tests, 0 failures (Balance 11, CashAccount 10, CashEntriesReverse 12, CashEntries 27, MarketDate 10, Money 15, OpeningBalance 9, AppLock 12, AppViewModel 77); lint: 2 findings, both known, neither silenced (NewerVersionAvailable at libs.versions.toml:17; TypographyDashes on date_not_a_date, now strings.xml:75). Release build on c40a8db's code green (52 tasks, R8 ran; unsigned); the fix changed only the ViewModel and was not rebuilt as release. Debug APK versionCode 5 (aapt2).

### 13.3 Phone results (Android 17, debug build versionCode 5, your reports)
Checks 1 to 11 all yes, and 5b and 7b after the fix. Notes:
- 9: installed over the 04 build (versionCode 4, adb install -r, firstInstallTime unchanged); the data was still there (criterion 35). The fixed build was installed over it again at 17:27:42.
- 3 and 4: Interest/dividend +$12.34 reversed (−$12.34); the original shows "Reversed on <date> ET", the reversal shows "Reverses: Interest/dividend, <date> ET"; the links work both ways.
- 5 and 6: the Fee adjustment: dated yesterday refused; dated tomorrow saved (+$5.00).
- 7: the Jan 5 deposit dated 2026-01-05 refused with "Not saved. Cash would be negative on 2026-01-10 by $300.00."; Cancel saved nothing.
- 5b and 7b (after the fix): a blank note together with a date before the original, or with a short balance, shows both messages.
- 11: log cleared at 17:31:34; the Adjustment +$7,654.21 reversed with the note "zqxrevcheck". The app's process (155 lines) had 0 matches for the amount in four forms, both notes and the labels. The whole log had 0 for everything except "Adjustment" (11 lines from other processes, content not read).
- No uninstall in 05; the phone holds the 03 and 04 data plus the 05 reversals.

### 13.4 Not run or not checked
CI and the pull request; the release APK on a device (unsigned); a release build of the fix commit; contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project).

## 14. Departures from the plan
1. Defect found in phone check 5: with a date before the original (or a short balance) and a blank note, only the note error showed, because K1 stops at the first failing rule and checks the note first. Two failing tests were written first (both failed: 89 tests, 2 failed), then confirmReversal was changed to two K1 passes as in the form's review (the note with the original's date as a stand-in; the date and balance rules with a stand-in note), plus a third pass with the real id and note as the guard before saving. Commit ce1c6fd, your decision to fix it in 05. Rechecked on the phone (5b, 7b).
2. A row's TalkBack label ends with ", <note>" when the entry has a note; no lead-in word was added (no new wording).
3. The message helpers (date, note, save, cash) and ReviewRow in CashEntryScreen.kt became internal, shared with the reversal screen; cashMessage gained the BeforeOriginal message.
4. The reversal tests are in a new file, CashEntriesReverseTest.kt, not in CashEntriesTest.kt.
5. The pre-filled note text is passed from the screen to the ViewModel (openReversal(defaultNote)), because its wording lives in the strings (D9).
