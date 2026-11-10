# plan/iterations/13b-reverse-transaction.md

Stage: Build. Status: approved 2026-11-01; done: built and checked 2026-11-01 to 2026-11-02, pull request #110 merged as e5edb0e (2026-11-02) (section 12). Decisions 1 to 3 settled (section 10).
Source: plan/plan.md section 4 (row 13b), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a buy, sell or split's detail (13a) I tap Reverse. One screen shows what is reversed, fixed, the reversal date (the original's by default; a later one may be picked, an earlier one is refused) and the effect: shares removed with the splits since applied and the cash returned for a buy; the lots restored, the proceeds taken back and the realized gain removed for a sell; the inverse ratio and the shares before and after for a split. Confirm and save records the reversal and, for a buy or sell, its cash line; I return to the original, which now says "Reversed on …" and has no Reverse. The reversal shows in Holding detail and on the Cash tab, links to its original and cannot itself be reversed. A reversal that breaks a rule is refused with the reason and the date that works, and the screen stays open.

## 2. Spec refs
- spec 4.6 (all of it except Reverse and re-enter: all-or-nothing, the link, no reversal of a reversal, at most once, the amount fixed, calculated from the stored original, the date rule, splits between the original and the reversal, reversing a buy, a sell and a split, the cash line), 4.1 (the replay of cash and of shares per lot from the reversal's date on), 4.2 (cash), 4.4 (a lot's remainder for a pick counts reversals; the least it holds on the sell date or later), 4.5 (a split's reversal applies the inverse ratio), 4.10 (from stored values; exact).
- spec 3: Transaction (optional reverses_id; "shown as such"), the `trade` cash line (one per buy or sell, reversals included), C5.
- Examples D (a buried mistake: the two messages) and F (a buy's reversal after a split removes 20 shares and returns $1,000.00), as automated tests.
- Section 6: view 6 (a reversal shown as such and linked to its original), view 7 (a reversal's trade line reads "Reversal of buy" or "Reversal of sell" with the ticker and portfolio), "Cash, split and reversal entry" (the detail screens, the reversal screen with one Confirm and save and no review), the date-field note, Number format and color, Accessibility rule 5, Error states.
- Wireframe 10b: frame G marker 14 (the reversed and reversal states), frame I (markers 16 to 18 and 24 to 26), frame J (marker 28: the blocked-save messages).
- Criteria: AC-53 (the reversal parts: Example F shows 20 shares removed and $1,000.00 returned, a reversal dated before the original is refused, Example D's two blocks and the save dated Jan 3, Reverse not offered on a reversal or an already reversed entry), AC-59(a)'s reversal part (reversing the 1 for 3 split gives exactly 10 shares and $100.00 per share), and AC-09, AC-35, AC-39 (TalkBack included), AC-40, AC-41, AC-42, AC-38 (the log during a reversal).
- spec 14: K1 (the reversal rules and the replay), K2 (store), K3, K5, K9.

## 3. Out of scope
- Reverse and re-enter (14), and AC-21, AC-54, AC-60(b).
- Hide, and unhiding a holding that a sell's reversal gives shares again (15, AC-60(a)); open item 44 (before 15).
- Realized gains, which leave out a reversed sale (26); Transaction history (27).
- The `reversal` row of the backup file (31, 33, 34).
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
Changed, core/: Transactions.kt (REVERSAL, decision 1); Holdings.kt (lots with reversals in the 4.1 order: a buy's reversal takes its lot's whole split-adjusted quantity out, a sell's reversal puts back what it took, a split's reversal applies the inverse ratio); Sells.kt (a lot's remainder for a pick as the least it holds on the sell date or later, now that a reversal can raise it again); Splits.kt (the replay shared with reversals); new Reversals.kt (the rules, the effect for the screen, the refusals with their dates); RecordStore.kt (saving a reversal with its cash line); their tests.
Changed, app/: RoomRecordStore.kt (a reversal row: its link, nothing else; a reversal of no original, of a reversal, a second one, or one dated before its original is unreadable); AppViewModel.kt; new ReversalScreen.kt and ReversalFlowTest.kt; TransactionDetailScreen.kt (Reverse; "Reversed on …"; a reversal's detail); HoldingDetailScreen.kt (reversal rows); CashScreen.kt and CashEntryDetailScreen.kt (a reversal's trade line); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 14); RowMappingTest.kt; the fake store.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No new library, no schema change (the transactions table has reversesId since 09) and no new network call.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-13b-reverse-transaction from main (460d3b6). This file and the plan.md lines (with the #109 merge). Baseline run.
2. core/: tests first, then the code. Example F (a buy's reversal after a 2 for 1 split removes 20 and returns $1,000.00; cash $10,000.00; the lot at 0); Example D (the reversal of the Jan 2 buy is refused while the Jan 3 sell stands, naming −10 on Jan 3 and that sell; after the sell's reversal, dated Jan 2 it is still refused naming Jan 3 as the date that works; dated Jan 3 it is saved); a sell's reversal puts its exact lots back with the splits since, takes the proceeds out and is refused when cash would go negative; a split's reversal applies the inverse ratio; 59(a)'s reversal (1 for 3 on 10 for $1,000.00, then its reversal: exactly 10 at $100.00); a reversal dated before its original refused; a reversal of a reversal, a second reversal and a reversal of a cash line refused; the reversal's quantity is the original's as traded with the splits between applied; after a sell's reversal its lots can be picked again, and a backdated sell sees the least a lot holds on its date or later; the effect figures for the screen; the cash line's amount and date.
3. Data layer: the reversal row and its checks; saving a reversal with its cash line as one action; row-mapping tests.
4. ViewModel: tests first (Reverse offered only on a buy, sell or split that is not reversed and not a reversal; the screen with the date defaulting to the original's; the effect; a refusal keeps the date and shows the message; Confirm saves and returns to the original's detail, as 05's cash reversal does; the original shows "Reversed on" and the reversal shows its original; Cancel; a bottom tab), then the code.
5. Wording questions, then the screens and strings: the reversal screen for a buy, a sell and a split; Reverse on the detail; the reversed and reversal states; reversal rows in Holding detail; the reversal's trade line on the Cash tab and its detail.
6. versionCode 14. Full run, release build, install over 13a's build with data.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- A sell's reversal does not yet unhide its holding (15; nothing can be hidden before 15).
- Realized gains are not shown anywhere but a sell's review and detail until 26; a reversed sell's detail still shows its gain, marked "Reversed on …".

## 7. Risks
- The replay with reversals (4.1): a buy's reversal must find every lot at or above 0 at every later place; a sell's reversal raises a lot again, so the "least it holds on that date or later" of 4.4 is no longer always the last figure (12 used the last one); the remainder for a pick must take the least over the places after the sell.
- Example D's two messages need the blocking sell, whether it is reversed, and the date of its reversal (decision 3).
- Units: a reversal's quantity is the original's as traded with the splits between applied (4.6); a split's reversal dated later than the original also applies to lots bought between the two dates (4.5 as written; my reading; the default date avoids it).
- Cash: a buy's reversal adds its total back, a sell's takes it out; both on the reversal's date, with the replay from that date.
- Stored data: a reversal holds only its link and date (decision 1), so a damaged link must be read as unreadable, never guessed.
- Rounding (CLAUDE.md pitfall from 12): the effect figures after splits that leave remainders of more than 8 places, tested with 1 for 3 and 1 for 7 cases.

## 8. Proof
Commands: as in 13a, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 13a's data (AAPL in Main at 1 share, from the buy of 2026-11-01; the sold-out buys of 2026-10-30 and 2026-10-31; the sell of 4.66666667 for $1,200.00 that took from them; the splits; net worth $4,191.87; opening balance 2026-01-01). Each check names its start (CLAUDE.md pitfall from 11). The exact figures are worked out before each check from the data then on the phone.
1. Install over 13a's build: the data is still there (AC-35).
2. Reverse is on a buy's, a sell's and a split's detail; the reversal screen shows the original fixed, the date and its note; "<" or Cancel leaves without saving.
3. A reversal dated before its original is refused, the text kept.
4. Example D on the phone: reversing the 2026-10-30 buy is refused while the sell of 4.66666667 stands, naming the date and that sell.
5. Reverse that sell: its three lots are back (with the splits since), cash −$1,200.00 on a "Reversal of sell AAPL" line, the sell shows "Reversed on …" with no Reverse, the reversal shows its original and has no Reverse.
6. Example D continued: reversing the 2026-10-30 buy dated 2026-10-30 is refused, naming the date that works; dated that date it is saved, its lot gone and cash +$150.00 on a "Reversal of buy AAPL" line.
7. Reverse a split: the inverse ratio, the shares before and after, no cash line; the lots change back.
8. Largest font and landscape, light and dark, TalkBack through a whole reversal (AC-39 to AC-41).
9. Close and reopen: the reversals, their links and their cash lines are kept.
10. Logcat during a reversal (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-11-01)
1. A reversal is a transaction of a new type REVERSAL holding only its link (reverses_id), its date and its entry order, with a trade line for a buy's or sell's reversal; every figure is worked out from the stored original (4.6, 4.10), as the backup file's `reversal` row also holds only these (section 8) (option A).
2. Reverse is on the original's detail only (13a), as wireframe 10b and 05 have it; after a save the app returns to the original's detail (option A).
3. A refused buy reversal names the earliest sell from its lot that is not yet reversed, in frame J's wording; once all are reversed, the date that works is the latest of their reversal dates (option A).
Settled by the spec, no question asked: one screen with Confirm and save and no separate review (section 6, frame I); the date defaults to the original's and an earlier one is refused (4.6); Reverse is not offered on a reversal or an entry already reversed, and a trade line points to its buy or sell (4.6, 13a); a buy's realized gain/loss is $0.00 (4.6); the date is digits only as on the other entry screens (09 decision 10).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "< Reverse buy AAPL"*, "Reverses"*, "Buy, 2026-01-02 ET"* "Portfolio P"*, "Quantity as traded"*, "Total amount"*, "Reversal date"*, "Enter date in US market time (New York). Not earlier than 2026-01-02."*, "Shares removed (split applied)"*, "Cash effect"*, "Realized gain/loss: $0.00"*, "Confirm and save"*, "Cancel"*.
- "Reverse"*, "Reversed on 2026-04-05 ET >"*, "Reversal of the buy of 2026-03-20 ET >"*, "Reversal of buy"*, "Reversal of sell"*.
- "Not saved. Shares would be −10 on 2026-01-03. Reverse the sell of 2026-01-03 first, then reverse this entry dated on or after that reversal."*, "Not saved. Shares would be −10 on 2026-01-03, because this reversal, dated 2026-01-02, is applied before the sell of 2026-01-03. Pick a reversal date of 2026-01-03 or later (the date the sell was reversed)."*, "Not saved. Cash would be negative on 2026-01-10 by $100.00."*, "Reversal date: not earlier than the original (2026-01-02)."* (frame J placeholders, D9).
- New, asked in step 5: a sell's and a split's reversal screen (the lots restored, the realized gain removed; the inverse ratio and the shares before and after), the refusal for a split's reversal that leaves a later sell short, the reversal rows in Holding detail, the reversal's detail and its title, and the TalkBack sentences.

## 12. Status and notes
Done. Built 2026-11-01 to 2026-11-02 (steps 1 to 6); phone checks 2026-11-02, all yes, no fix needed; pull request #110 merged as e5edb0e (2026-11-02), CI green on the pull request and on main (2m 21s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: e9da4fa step 6, f1d4b92 step 5, 3e0a581 step 4, 600f9aa step 3, edb06bb step 2, 959179c step 1 with this file. main was 460d3b6. Steps 1 to 6 were committed together after step 6, on your request (section 14, item 4).

### 13.2 Local run
On e9da4fa's code (2026-11-02, all with --rerun-tasks): spotlessCheck green; debug build green, no compiler warnings printed; 527 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 10, Reversals 14, Sells 19, Splits 23, TickerCheck 16, Transactions 18, RowMapping 11, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint: the 4 listed findings, none silenced (AndroidGradlePluginVersion, open item 31; NewerVersionAvailable for kotlinx-coroutines-test and for sqlcipher-android, open item 43; TypographyDashes on date_not_a_date, strings.xml:86), report written 01:11:53. Release build on the same code green (R8 ran; unsigned; "Unable to strip" names libandroidx.graphics.path.so and libsqlcipher.so, as known). Debug APK versionCode 14 (aapt2). No schema change (still 5.json).

### 13.3 Phone results (Android 17, debug build versionCode 14, your reports)
Checks 1 to 10 all yes. The figures of each check were worked out from the data on the phone before it was given. Notes:
- 1: installed over the 13a build (lastUpdateTime 2026-11-02 00:11:04, firstInstallTime 2026-10-29 10:44:04 unchanged); net worth $4,191.87, AAPL at 1 share; Reverse under "Not reversed." on a buy (AC-35).
- 2: "< Reverse buy AAPL" for the 2026-11-01 buy: the original fixed, the date and its note, the effect (1 share removed, the 2 for 1 and 1 for 2 since cancelling, +$150.00, $0.00); Cancel left nothing saved; Reverse on a sell and a split.
- 3: a reversal dated 2026-10-31 refused with "Reversal date: not earlier than the original (2026-11-01).", the date kept; "2026-" refused at once and the effect hidden.
- 4: Example D on the phone: the reversal of the 2026-10-30 buy refused with "Shares would be −0.66666667 on 2026-11-01. Reverse the sell of 2026-11-01 first, …".
- 5: the sell of 4.66666667 reversed: its three lots back (0.66666667, 3.33333333 and 0.66666667), −$1,200.00 on a "Reversal of sell AAPL" line, realized +$150.00 removed in no color; the sell showed "Reversed on 2026-11-01 ET >" without Reverse, the reversal its link back without Reverse, and Back went one step at a time; AAPL 5.66666667 shares at $1,200.00, net worth $4,041.87.
- 6: Example D continued: dated 2026-10-30 refused, naming 2026-11-01 as the date that works; dated 2026-11-01 saved, 0.66666667 shares removed and +$150.00; AAPL 5 shares at $1,050.00; net worth unchanged.
- 7: the 1 for 2 split of 2026-11-01 reversed: ratio applied 2 for 1, 5 to 10 shares, no cash line; lots at $112.50 and $75.00 per share; net worth unchanged.
- 8: largest font, portrait and landscape, light and dark: the reversal screen and the reversal's detail scroll and wrap, the refusal readable on dark; TalkBack through a whole reversal of the 2026-11-01 buy (2 shares removed, +$150.00), read as section 11; AAPL 8 shares at $900.00.
- 9: closed and reopened: the four reversals, their links and their cash lines kept.
- 10: Logcat cleared at 01:08:33, then the reversal of a 2026-10-31 sell of 1 for $160.00; the app's uid 10454 had 39 lines, all from system tags; no ticker, portfolio name, amount, quantity, reversal word or key; the two lines matching "N for N" were Android's I/Surface buffer lines (checked with the digits masked). Counts only. Afterwards, as worked out: AAPL 9.33333333 shares, Main $1,050.00, net worth $4,031.87.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); a failed save (open item 14: not provoked); a split's reversal refused for a later sell left short, and a sell's reversal refused for cash, on the phone (unit tested only); contrast measurement (by eye on the phone); network traffic (open item 39); the screens in a JVM test (none in the project; the flow and figures are tested in steps 2 and 4).

## 14. Departures from the plan
1. A reversal row also holds its original's portfolio and ticker, because those columns are required; the stored check reads a reversal whose portfolio or ticker differs from its original as unreadable (step 3). Every figure still comes from the original (decision 1).
2. A lot's remainder for a pick is the least it holds at the sell's place or any later place of the holding, as 4.4 says, since a sell's reversal can raise a lot again; 12 used the last figure, which was the least while lots only fell (step 2; tested with a backdated sell before a sell and its reversal).
3. A split's reversal applies its inverse ratio to every lot placed before it, lots bought after the split in post-split shares included (4.6 as written, read with its date rule: the split's effect stays in place until the reversal; your decision A in step 8). The default date, the split's own, reaches only buys entered on that date after the split.
4. On your request steps 1 to 6 were committed together after step 6, each with its own files. AppViewModel.kt (steps 4 and 5) went into step 4's commit, Reversals.kt (steps 2 and 4) into step 2's and TransactionDetailScreen.kt (steps 2 and 5) into step 5's, so the commits of steps 2 to 4 do not build on their own; the tip e9da4fa was built and tested.
5. TransactionDetailScreen.kt had placeholder branches for a reversal in step 2 so the app compiled; step 5 replaced them with the approved wording.
6. A reversal's own trade line shows only the link to the reversal, not "Open the buy or sell to reverse it.", since a reversal cannot be reversed (frame H: a reversal shows the link to the other entry); not shown to you before step 5's build.
7. Reversals.draft builds the unsaved reversal for both the save and the screen's effect, so the two cannot differ (step 4).
