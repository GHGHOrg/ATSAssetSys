# plan/iterations/04-interest-adjustment.md

Stage: Build. Status: approved 2026-10-29; decisions 1 to 4 settled the same day (section 10). Not started.
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

## 12. Status and notes
Approved 2026-10-29. Not started.
