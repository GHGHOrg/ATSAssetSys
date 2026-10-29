# plan/iterations/04-interest-adjustment.md

Stage: Build. Status: built on branch iter-04-interest-adjustment (steps 1 to 5, commits 822c58d to cef9a8a) and checked on the phone 2026-10-29, all checks yes (section 13). Pull request not created; CI not run. Decisions 1 to 4 settled (section 10). Sections 1 to 12 were written before the build; where the build differs, section 14 says so.
Source: plan/plan.md section 4 (row 04), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
From the Cash tab I add an interest/dividend entry, or an adjustment in either direction with a required note, to fix a difference against my real cash. An adjustment that takes cash out is refused like a withdrawal if it would make cash negative.

## 2. Spec refs
- spec 4.2: "A cash difference vs reality is fixed with an adjustment deposit/withdrawal with a note"; amounts never 0, up to 2 decimals; an adjustment may be either sign.
- spec 4.1: the replay after every entry (03 decision 5) also covers an outgoing adjustment.
- spec 6, Cash entry flow: wireframe 10b frame B and markers 1, 4, 5, 6 and 7 (four type chips; the direction toggle on Adjustment only; the note required on an adjustment, labelled "required"; frame J's message).
- Interest/dividend is cash-only, not linked to a holding (wireframe 10b marker 1; intent v52).
- Criteria: 53 (part: the types Interest/dividend and Adjustment; "an adjustment without a note is refused"), 42 (a blocked outgoing adjustment), 9, 35, 39 to 42.
- spec 14: K1, K5, K9.

## 3. Out of scope
- Reversals, the cash entry detail screen, and the cash reversal as an Adjustment (05).
- Filters (06). Per-holding dividends: a later version (intent).
- Example E (needs portfolios, 29). Criterion 37: still deferred.

## 4. Files
Changed, core/: CashEntries.kt (allows Interest/dividend, in, and Adjustment with a direction, in or out; a note-required rule for adjustments); CashEntriesTest.kt.
Changed, app/: AppViewModel.kt (four types, the direction in the form, the note-required refusal); CashEntryScreen.kt (four chips; Direction chips on Adjustment; "Note (required)"; the review's sign follows the direction); strings.xml; app/build.gradle.kts (versionCode 4); AppViewModelTest.kt.
The Direction chips show only when Adjustment is selected (wireframe 10b marker 6); the chosen direction is kept across a type switch.
Documents: plan.md, open-items.md, CLAUDE.md if needed.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-04-interest-adjustment from main (00156bf). Baseline run.
2. core/: tests first (interest added; adjustment in and out; an outgoing adjustment that overdraws is refused with the date and shortfall; a missing note refused; a note of only spaces, decision 1; a direction given for other types is ignored, decision 4), then the code.
3. ViewModel: tests first (the four types; the direction kept across a type switch; the note-required refusal shown with other refusals; the saved sign), then the code.
4. Screens: the chips, the direction, the note label, the amount line, the review sign, the message.
5. versionCode 4. Full run, release build, install over 03's build with data.
6. Phone checks (section 8).
7. Documents and the pull request draft.

## 6. Stubs
- Rows on the Cash tab still cannot be tapped (05).

## 7. Risks
- A sign error in an outgoing adjustment would raise the balance instead of lowering it. Tests cover both directions and the replay.
- The type switch keeps the typed text. A direction picked for Adjustment must not leak into Deposit or Withdrawal (decision 4).
- "Withdrawal (−)" uses the true minus sign (U+2212), as Money.format does. Checked on the phone with TalkBack.

## 8. Proof
Commands: as in 03. CI green on the pull request.
By hand on the phone:
1. With the opening balance present, the form shows four chips: Deposit, Withdrawal, Interest/dividend, Adjustment.
2. Interest/dividend $12.34: review +$12.34; saved; the row reads "Interest/dividend".
3. Adjustment, Deposit (+), $20.00, note "Match broker balance": review +$20.00 and the note; saved.
4. Adjustment, Withdrawal (−), $5.00 with a note: review −$5.00; saved; the balance goes down.
5. Adjustment with an empty note: "Note: required for an adjustment."; the text stays.
6. Adjustment with a note of only spaces: refused with "Note: required for an adjustment." (decision 1).
7. An outgoing adjustment larger than the balance on a later date: refused with the date and the shortfall.
8. Switching the type keeps the date, amount and note; back on Adjustment the direction is kept.
9. Install over the 03 build with data: the data is still there (criterion 35).
10. Largest font, light and dark, TalkBack on the four chips, the direction chips and the note label.
11. Logcat while saving an adjustment: no amount or note text.

## 9. Research
None planned; no new library.

## 10. Decisions (all Build, settled by you 2026-10-29)
1. A note of only spaces on an adjustment is refused as missing; the note is stored as typed.
2. An adjustment starts with Deposit (+) selected, as in frame B.
3. No line under the amount for Adjustment.
4. K1 ignores a direction on any type other than Adjustment; the type decides the sign.

## 11. Wording to approve (D9; * = given by the spec or the wireframes)
- Chips: "Interest/dividend"*, "Adjustment"*. Direction: "Direction"*, "Deposit (+)"*, "Withdrawal (−)"*.
- Note label on an adjustment: "Note (required)"*. Message: "Note: required for an adjustment."*
- The line under the amount for Interest/dividend: "A positive number. It is added to cash." (the same as Deposit).
Approved on paper with this file; confirmed on the phone (D9).
Confirmed on the phone 2026-10-29 (checks 1 to 6 and 10): the four chips, the Direction row, "Note (required)" and the message as written above.

## 12. Status and notes
Built 2026-10-29; phone checks 2026-10-29. Pull request and CI: not yet.

## 13. Build results
### 13.1 Commits
Newest first: cef9a8a step 5, 682bf25 step 4, 3b0075b step 3, 0298c13 step 2, 822c58d iteration file. main was 00156bf.

### 13.2 Local run
On cef9a8a's code (2026-10-29, all with --rerun-tasks): spotlessCheck green; debug build green (39 tasks); 155 tests, 0 failures (Balance 11, CashAccount 10, CashEntries 27, MarketDate 10, Money 15, OpeningBalance 9, AppLock 12, AppViewModel 60); lint: 2 findings, both known, neither silenced (NewerVersionAvailable at libs.versions.toml:17; TypographyDashes at strings.xml:58, the date_not_a_date string); release build green (52 tasks, R8 ran; unsigned). Debug APK versionCode 4 (aapt2). The minus sign in "Withdrawal (−)" is stored as U+2212 (byte check).

### 13.3 Phone results (Android 17, debug build versionCode 4, your reports)
Checks 1 to 11 all yes. Notes:
- 9: installed over the 03 build (versionCode 3, adb install -r, firstInstallTime unchanged); the 03 data was still there (criterion 35).
- 7: an outgoing adjustment of $300.00 dated 2026-01-05 was refused with "Not saved. Cash would be negative on 2026-01-10 by $100.00." (the Example C arithmetic).
- 11: log cleared at 13:42:46; an adjustment of $7,654.21 with the note "zqxlogcheck" saved. The app's process (82 lines) had 0 matches for the amount in four forms, the note and the screen labels. The whole log had 0 for the amount and the note; "Adjustment" matched 4 lines from other processes (content not read).
- No uninstall in 04; the phone holds the 03 data plus the 04 entries.

### 13.4 Not run or not checked
CI and the pull request; the release APK on a device (unsigned); contrast measurement (by eye only); criterion 37 (deferred); Compose UI tests (none in the project).

## 14. Departures from the plan
1. 03's core test typesOtherThanDepositAndWithdrawal_areRefused was narrowed to theOpeningBalanceAndTradeTypes_areRefused (your decision, step 2): Interest/dividend and Adjustment are allowed from 04 and covered by the new tests.
2. 03's ViewModel test theTypeChangesToWithdrawalAndBack_otherTypesAreIgnored was narrowed to theTypeChangesToWithdrawalAndBack_openingBalanceAndTradeAreIgnored (your decision, step 3), for the same reason.
3. Review's second K1 pass uses a stand-in note (PROBE_NOTE), so an empty adjustment note cannot hide the date and balance refusals behind "note required".
4. The Direction row sits between the date and the amount, as in frame B, not inside the type card.
5. NoteRequired is checked after the line-break rule, so a note "a\nb" gets the line-break message.
