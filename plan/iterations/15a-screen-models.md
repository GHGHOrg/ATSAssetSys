# plan/iterations/15a-screen-models.md

Stage: Build. Status: approved, built and checked on the phone 2026-11-03 (section 12). Decision 1 settled (section 10).
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

## 10. Decisions (Build, settled by you 2026-11-03)
1. The models are built in separate builders in ScreenModels.kt, tested on their own, with one ViewModel wiring test per screen (your B, after comparing maintainability, test run time and APK size). hiddenRows from 15 stays where it is (open item 48). The three criteria now apply to every Build choice about code (CLAUDE.md section 8).

## 11. Wording to approve (D9)
- None: no text changes.

## 12. Status and notes
Built 2026-11-03 (steps 1 to 6); phone checks 2026-11-03, all yes. Deploy review 2026-11-03: three Low findings; 1 and 3 fixed on the branch and 1 rechecked on the phone (versionCode 21), 2 recorded under open item 48 (section 14, items 9 to 11). Pull request #120 merged as 2411066 (2026-11-03), CI green on the pull request (3 checks) and on main (2m 9s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: the Deploy commits (versionCode 21; these documents), 9e67930 Deploy fix, 3ba48fb step 8, 5119a9d step 6, 3040626 step 5, 1f52b06 step 4, f6a52ad step 3, 40f56d9 step 2, 15003bc step 1 with this file, 772533c Plan (CR2 and open item 48). main was 65250c7.

### 13.2 Local run
On 5119a9d's code (2026-11-03, all with --rerun-tasks): spotlessCheck green; debug and release builds green ("Unable to strip" as always); 647 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Hiding 7, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 12, ReentriesSplitCash 16, Reentries 27, Reversals 14, Sells 23, Splits 23, TickerCheck 16, Transactions 18, RowMapping 17, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, CashFormMessages 5, DateDigits 7, HideFlow 10, HoldingDetailFlow 12, HoldingRowModel 5, ReenterFlow 32, ReplaceReviewModel 5, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint the 4 listed findings only (report 2026-11-03 19:47:19). APKs: debug 42,258,107 bytes; release (unsigned) 12,457,558 bytes, 32,784 bytes (about 0.26%) more than 15's 12,424,774. No schema change. On the Deploy fix (9e67930, versionCode 21): the same run, 649 tests (ReplaceReviewModel 7), 0 failures, lint the 4 listed findings only (report 2026-11-03 21:36:37); release 12,473,942 bytes, 16,384 more than versionCode 20 (my reading: the new ShareList class and the package's alignment padding).

### 13.3 Phone results (Android 17, your reports)
No photos: you read the three screens on versionCode 19 against the expected text worked out from your data and the strings, then again on versionCode 20 after the install. Both readings matched.
- Before, on versionCode 19: screens 1 to 3 all as expected (MA's 5 Transactions rows; the combined review of MA's newest buy; the cash entry form's two messages).
- 1: installed over the 15 build (versionCode 20, lastUpdateTime 2026-11-03 20:21:12; firstInstallTime 2026-10-29 10:44:04 unchanged); net worth $4,155.32, cash $3,005.32, Main AAPL 15, V 0.00000001, MA 1, SPY still hidden in 投資信託 (criterion 35).
- 2: MA's Transactions tab, newest first, all 2026-11-03 ET: Split 3 for 1; Buy 0.33333333 shares $100.00; Reversal of the buy of 2026-11-03 ET; Split 1 for 3; Buy 1 share $100.00; row 2 opened its detail.
- 3: Reverse and re-enter on MA's newest buy, unchanged: the banner "Replacing the buy of 2026-11-03 ET: 0.33333333 MA, $100.00. Nothing changes until you confirm.", quantity 1 with "Original: 0.33333333"; Review both: part 1 Reverses Buy, 2026-11-03 ET, reversal date 2026-11-03 ET with Change, shares removed 1, cash effect +$100.00; part 2 Buy MA, Main, 2026-11-03 ET, quantity 1, total amount $100.00, price per share $100.00, cash effect −$100.00; combined cash effect $0.00; nothing saved.
- 4: the cash entry form, an Adjustment Out of $5,000.00 dated 2026-11-03 with no note, at Review: "Note: required for an adjustment." and "Not saved. Cash would be negative on 2026-11-03 by $1,994.68." together; nothing saved.
- 5: largest font, light and dark on those three screens (criteria 9, 39 to 42).
- Recheck after the Deploy fix (versionCode 21, lastUpdateTime 2026-11-03 21:36:49): Reverse and re-enter on V's 2026-11-03 sell of 0.99999997, unchanged, Review: part 1 "Lots put back" 0.14285714 shares, part 2 "Lots used" 0.14285714 shares; nothing saved. My first expected value, 0.99999997, was wrong: the reversal goes after every entry of the original's date, so the 1 for 7 entered later that day applies (0.99999997 / 7 shown 0.14285714), and the new sell's pick takes the lot's exact 1/7 (CLAUDE.md pitfall from 14a). The app was right.

### 13.4 Not run or not checked
CI and the pull request; the combined review of a sell, split or cash pair on the phone (unit tested); a replacing cash entry's messages on the phone (unit tested); TalkBack on the three screens (the strings are unchanged; not read by TalkBack this time); the release APK on a device (unsigned, open item 4); open item 33 (34); drawing the screens in a test (open item 47's remainder).

## 14. Departures from the plan
1. Step 4's model is in its own file, CashFormMessages.kt, with its builder and an AppState extension property, not in ScreenModels.kt and AppViewModel.kt (section 4): steps 2 and 3 already shared those two files, and step 4 in them would have made steps 3 and 4 impossible to commit apart.
2. Tests that passed against their stubs: the "nothing replaced" test (step 3) and the "no refusal" test (step 4); real guards once the code was in.
3. The combined review's wiring test (ReenterFlowTest, step 3) was written after the wiring property; it was never red.
4. The combined review's part 1 for a split now draws its four rows itself from the model, instead of calling the reversal screen's shared EffectRows; the same rows and strings.
5. Section 8 asked for photos before the install; you could not take them, so the before and after readings of the expected text stand in for them (section 13.3).
6. Steps 2 to 4 were built before their commits on your request ("continue"); each was committed on its own, in order, step 2 from the index.
7. Step 2's first full run failed spotlessCheck on two blank lines between the branches of a when in ScreenModels.kt; added by hand, spotlessApply not run.
8. Not covered, as section 3 says: the cash entry review screen's own save message (CashEntryScreen.kt) stays outside the model (open item 48).
9. Deploy finding 1 (Low), fixed in 9e67930 (your decision A): the combined review's joinedShares still decided between "share" and "shares" inside the composable, against the new convention, and the one-lot-of-1 case was untested. The decision moved into the model as ShareList (with "0 shares" for no lot, as before); failing test first. The sell test's two expected values changed type (listOf to ShareList) with the same quantities, not weakened. Rechecked on the phone (section 13.3).
10. Deploy finding 2 (Low), recorded under open item 48 (your decision A): the cash rule's date and shortfall inside the cash entry form's message are still formatted by cashMessage, a helper shared with the reversal screen.
11. Deploy finding 3 (Low), fixed in 9e67930: no test covered a trade's part 1 with no effect yet (the reversal date not a whole date). The test was written after the code and passed on its first run.
