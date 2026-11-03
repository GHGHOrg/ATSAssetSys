# plan/iterations/15a-screen-models.md

Stage: Build. Status: approved 2026-11-03.
Source: plan/plan.md section 4 (row 15a), intent/intent-cr-02-screen-models.md (CR2), spec/spec.md v5 (final; no change, CR2 Design), plan/discussions/screen-tests.md. "My reading" is an interpretation, not a test.

## 1. Outcome
What three screens show is built as a model and checked by tests on every build: Holding detail's Transactions rows, the Reverse and re-enter combined review, and the messages the cash entry form shows at Review. Nothing I see changes. From 16 on, every new or changed screen is built the same way.

## 2. Spec refs
- Spec: view 6 (Holding detail's Transactions tab), 4.6 (Reverse and re-enter's combined review), 4.2 and section 6 (the cash entry form's messages; Error states). Section 14, K9 (screens only read through K3), unchanged.
- Criteria: 48 (Holding detail), 54 (Reverse and re-enter), 42 (keeps its phone check; CR2 Design, your A), 53 (cash entry), and the standing criteria 9, 35, 39 to 42.
- The three past defects, now covered by tests: 14c section 14 item 9, 14b section 14 item 6, and the 05 phone check 5 pitfall in CLAUDE.md.

## 3. Out of scope
- The other screens (open item 48).
- Screen tests on the computer and on an emulator (open item 47's remainder); layout, windows and the keyboard; TalkBack speech.
- Open items 45 and 32; the phone check of open item 33 (34).

## 4. Files
New, app/: ScreenModels.kt (the three models and how they are built: HoldingRowModel, a trade, a split or a reversal row with kind, date, shares text, the "one share" flag, the amount text, the ratio, and the original's kind and date; ReplaceReviewModel, part 1 with the reversal's date, shares removed or restored and cash, part 2 with the new entry's kind, quantity, total, price, note or ratio, and the combined cash; CashFormMessages, the list of messages shown at Review, each a kind with its values) and their tests (HoldingRowModelTest, ReplaceReviewModelTest, CashFormMessagesTest).
Changed, app/: AppViewModel.kt (the models as AppState properties, as hiddenRows; decision 1); HoldingDetailScreen.kt, ReplaceReviewScreen.kt and CashEntryScreen.kt draw only from the models; app/build.gradle.kts (versionCode 20).
Documents: plan.md (section 3, a technical decision: a screen draws only figures, texts and messages its model prepares; each model is unit tested), CLAUDE.md (the same as a convention), open-items.md (item 47's status: first part done by CR2).
No new library, no schema change, no new wording, no change to what is shown.

## 5. Order of work
Each step ends with build, tests and lint (all with --rerun-tasks) and the report; then I stop.
1. This file and the plan.md lines (the 15a row linking CR2 and item 48; the #119 merge record; the change log with CR2's Design result: no spec change, criterion 42 keeps its phone check). Baseline run (630 tests).
2. Holding detail's Transactions rows: tests first, shown red (a buy with a quantity ratio shows 0.33333333 shares, the 14c case; a sell; a split's ratio; a reversal's original kind and date; "1 share" vs "2 shares"; the amount as $100.00; newest first, a shared date's later entry first, as now), then the code; the screen draws only from the model.
3. The combined review: tests first (part 1 and part 2 for a buy pair, a sell pair, a split pair and a cash pair; the typed note shown, an empty one included, never the stand-in "check", the 14b case; the combined cash; the new buy's exact shares and price, CR1), then the code.
4. The cash entry form's messages at Review: tests first (an adjustment with no note and a date that makes cash negative shows both messages, the 05 case; each field message alone; a save failure), then the code.
5. Conventions: plan.md section 3 and CLAUDE.md, wording shown to you first.
6. versionCode 20. Full run, release build, install over 15's build.
7. Phone checks (section 8): short, since nothing shown should change.
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- None.

## 7. Risks
- "No change to what is shown": moving a value from the screen into a model can change its format by accident (a sign, a dollar sign, a plural). The tests check the model, not the drawing, so the phone checks compare each screen with what it showed before, using your photos.
- A model and the screen drifting apart: if a screen later bypasses its model, the tests no longer guard it. The plan.md and CLAUDE.md convention, and the review, keep that in check.
- The screen code becomes simpler, so a changed line could break the layout at the largest font; check 5 covers it.

## 8. Proof
Commands: as in 15, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 15's data (Main: AAPL 15, V 0.00000001, MA 1; 投資信託: SPY hidden; cash $3,005.32). Before the install you photograph the three screens; after it each must look the same.
1. Install over 15's build: the data is still there (criterion 35).
2. MA's Transactions tab: every row (buys, the reversal, the splits) matches the photo; the newest buy reads 0.33333333 shares, $100.00.
3. The combined review: Reverse and re-enter on MA's newest buy, unchanged, Review: parts 1 and 2 and the combined cash match the photo; back out without saving.
4. The cash entry form: an Adjustment of −$5,000.00 with no note, dated today, Review: both messages show together and match the photo; back out without saving.
5. Largest font, light and dark, on those three screens (criteria 9, 39 to 42).

## 9. Research
- None needed: no new library.

## 10. Decisions needed from you (Build; asked one at a time)
1. Where the models are built: as AppState properties, as hiddenRows (tested through the existing ViewModel tests); or in separate builder objects tested on their own.

## 11. Wording to approve (D9)
- None: no text changes.

## 12. Status and notes
- 2026-11-03: drafted and approved (your A).
