# plan/iterations/05-reverse-cash-entry.md

Stage: Build. Status: approved 2026-10-29; decisions 1 to 4 settled the same day (section 10). Not started.
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

## 12. Status and notes
Approved 2026-10-29. Not started.
