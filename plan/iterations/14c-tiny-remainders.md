# plan/iterations/14c-tiny-remainders.md

Stage: Build. Status: approved, built and checked on the phone 2026-11-03 (section 12). Decisions 1 and 2 settled (section 10).
Source: plan/plan.md section 4 (row 14c), intent/intent-cr-01-tiny-remainders.md (CR1), spec/spec.md v5 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
After a reverse split leaves a lot a leftover too small to show, its lot and holding show 0.00000001, and I can sell it with one sell of 0.00000001 that leaves exactly 0 shares. When I reverse and re-enter a buy after a same-date split and leave the quantity as filled in, the new lot has exactly the original's shares.

## 2. Spec refs
- CR1; spec 4.4 (the pick limit, Sell all, "0 only when exact"), 4.6 (Reverse and re-enter of a buy; the reversal using the ratio), 4.10 (stored ratio; the shown quantity), 4.5, 4.3.
- Spec 3: Transaction (`quantity_ratio`).
- Section 5: Examples H and I. Section 8: the `buy` row (spec only; built in 31 and 33).
- Criteria: 59(e) and 59(f) (their backup-and-restore parts in 31 to 34), and the standing criteria 9, 35, 39 to 42.
- Spec 14: K1, K2, K5, K9.

## 3. Out of scope
- Hide and unhide (15): "offers Hide" in 59(e) is checked in 15; here, "exactly 0 shares".
- Backup and restore of the ratio and the leftover (31 to 35).
- Open item 45 (performance); open item 32 (the cash date field).
- The phone check of open item 33 (34).

## 4. Files
Changed, core/: Quantity.kt (one shown-figure function: a quantity above 0 that rounds to 0 gives 0.00000001; format uses it); Sells.kt (shown, the pick limit, Sell all and the picks' sum use it); Transactions.kt (a buy's optional quantityRatio and its exact bought quantity); Holdings.kt, Reversals.kt and Sells.kt (their cost and quantity lines read the exact bought quantity); Reentries.kt (startingQuantity also gives the exact figure; reenterBuy takes the ratio when the quantity is left as filled in, and chains an original's own ratio); their tests.
Changed, app/: RoomRecordStore.kt (a buy's ratio in the existing splitTo and splitFrom columns; no schema change, the database stays at version 5); AppViewModel.kt (remembers the filled-in quantity; an unchanged quantity sends the ratio); app/build.gradle.kts (versionCode 17); RowMappingTest.kt; ReenterFlowTest.kt; the fake stores. Screens: none expected, since every quantity shown goes through Quantity.format (my reading).
Documents: this file, plan.md, open-items.md (close 44 and 46), CLAUDE.md (the CR intent files convention; pitfalls if any).
No new library, no new network call, no new wording expected (decision 1 may add one).

## 5. Order of work
Each step ends with build, tests and lint (all with --rerun-tasks) and the report; then I stop.
1. This file and the plan.md lines (the 14c row with the CR1 link, row 15 depending on 14c, the examples line, the status row, the change log with the #115 merge record). Baseline run (594 tests).
2. core/, item 44: tests first, shown red, then the code. Quantity.format of 0.00000003 / 7 shows 0.00000001; Example H end to end (the lot listed at 0.00000001, the pick takes the exact leftover, the holding exactly 0); Sell all includes the leftover; a pick of 0.00000002 on it is refused; a sell quantity of 0.00000001 above the exact holding is not refused as more than held; unchanged behaviour with round-up and round-down cases (2/3, 5/3, 3/7, 1/7) and several lots (CLAUDE.md pitfall from 12).
3. core/, item 46: tests first, shown red, then the code. Example I (the re-entered buy stores quantity 1 and ratio 1 to 3; exactly 1/3 at $300.00 per share; exactly 1 share after the 3 for 1); a typed 0.33333333 gives an ordinary buy and 0.99999999 after the split; reversing the re-entered buy removes exactly 1/3; re-entering the re-entered buy chains the ratio; with no same-date split no ratio is stored.
4. Data layer: a buy's ratio saved and read back through splitTo and splitFrom; a buy without one unchanged; the exported Room schema file unchanged.
5. ViewModel: tests first (left as filled in sends the ratio; a changed quantity sends none; the same digits typed again count as unchanged, decision 2), then the code.
6. versionCode 17. Full run, release build, install over 14b's build.
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- None.

## 7. Risks
- The shown figure: the picker, the pick limit, Sell all and later the restore must all use one function, or a leftover shows one figure and is judged by another (CLAUDE.md pitfall from 12).
- More than held: a typed sell of 0.00000001 is above the exact leftover; the existing check must compare as it does for round-up cases (tested in step 2).
- Reusing splitTo and splitFrom on a buy row: the core keeps a buy's ratio out of Transaction.split. Holdings.kt reads splits by type, and the screens read split only on split screens (checked by a search of the code).
- Ratio size: the columns hold 32-bit whole numbers; several same-date splits could in theory multiply past 2,147,483,647 (decision 1).
- Data already on the phone: no leftover and no affected buy expected (unverified); no migration is needed either way.

## 8. Proof
Commands: as in 14b, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 14b's data. Each check names its start and the control; the exact figures are worked out from the phone's data before each check. Two tickers not used before, picked at step 7.
1. Install over 14b's build: the data is still there (criterion 35).
2. Example H in Main: buy 1 share for $10.00, sell 0.99999997 for $10.00, a 1 for 7 split; the holding and the lot show 0.00000001; sell 0.00000001 for $0.00; the holding shows 0 shares and the lot is gone from the Sell list; cash unchanged overall.
3. Example I in Main: buy 1 share for $100.00, a same-date 1 for 3 split, Reverse and re-enter the buy with the quantity unchanged, then a later-dated 3 for 1; the holding shows exactly 1 share; cash down $100.00 overall.
4. AAPL's lots and the Cash tab as before.
5. Largest font and landscape, light and dark, TalkBack on the 0.00000001 figure (criteria 9, 39 to 42).

## 9. Research
- None needed: no new library or platform behaviour.

## 10. Decisions (Build, settled by you 2026-11-03)
1. A ratio that does not fit the columns refuses the pair, with a message to approve (D9) (your A). To save anyway, type a different quantity: the same digits count as unchanged (decision 2).
2. "Left as filled in" means the quantity text equals the filled-in text, so typing the same digits again counts as unchanged (your A).

## 11. Wording to approve (D9)
1. Decision 1's refusal, on the buy screen while replacing: "Not saved. Nothing was changed. The splits since the original are too large to keep its exact shares. To save, type a different quantity." (your A, 2026-11-03)

## 12. Status and notes
Built 2026-11-03 (steps 1 to 6); phone checks 2026-11-03, all yes. Deploy review 2026-11-03: one Medium and one Low finding, fixed on the branch and rechecked on the phone (section 14, items 9 and 10). Not yet merged.

## 13. Build results
### 13.1 Commits
Newest first: 4d9a05a Deploy fix, 5baf5ad step 8, de9bb63 step 6, 6d5c196 step 5, 83ba721 step 4, b70732b step 3, f38c709 step 2, 4aabba4 step 1 with this file, 762d9a5 Design (spec.md v5), 574c680 Plan (intent-cr-01-tiny-remainders.md). main was 071d490.

### 13.2 Local run
On de9bb63's code (2026-11-03, all with --rerun-tasks): spotlessCheck green; debug and release builds green ("Unable to strip" as always); 611 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 12, ReentriesSplitCash 16, Reentries 27, Reversals 14, Sells 23, Splits 23, TickerCheck 16, Transactions 18, RowMapping 15, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, ReenterFlow 31, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint the 4 listed findings only (report 2026-11-03 13:52:33). No schema change: nothing under app/schemas changed; the database stays at version 5. On 4d9a05a's code (the Deploy fix): the same run, debug and release builds green, 611 tests, 0 failures, lint the 4 listed findings only (report 2026-11-03 14:40:08).

### 13.3 Phone results (Android 17, debug build versionCode 17, your reports)
Checks 1 to 5 all yes. All entries in Main, dated 2026-11-03.
- 1: installed over the 14b build (lastUpdateTime 2026-11-03 13:54:04; firstInstallTime 2026-10-29 10:44:04 unchanged); the data kept: net worth $4,155.32, cash $3,105.32, AAPL 15 shares at $1,050.00 (criterion 35).
- 2 (Example H, V): buy 1 for $10.00 (cash $3,095.32); sell 0.99999997 for $10.00 (V 0.00000003 shares, cash $3,105.32, realized $0.00); a 1 for 7 split: the review showed 0.00000003 to 0.00000001, the header and the lot 0.00000001 (0 before 14c); Sell all filled 0.00000001, total $0.00, the review cost $0.00 and realized $0.00; saved: V 0 shares, no lot, Sell offers no lot; cash $3,105.32, net worth $4,155.32 (criterion 59(e)). Confirmed by eye; no photos.
- 3 (Example I, MA): buy 1 for $100.00 (cash $3,005.32); a 1 for 3 split the same date: 0.33333333 shares, the lot at $300.00 per share; Reverse and re-enter on the buy with nothing changed: the banner, the quantity 0.33333333 with "Original: 1"; the review: part 1 0.33333333 removed and $100.00 back, part 2 0.33333333 at $300.00, combined $0.00; saved, "Reversed on 2026-11-03"; a 3 for 1 split: the review 0.33333333 to 1, the header 1 share (0.99999999 before 14c), the lot 1 at $100.00; the new buy's detail 0.33333333, $100.00, $300.00; cash $3,005.32, net worth $4,155.32 (criterion 59(f)).
- 4: AAPL 15 shares at $1,050.00, lots 12 at $75.00 and 3 at $50.00, unchanged.
- 5: largest font and landscape, light and dark, on MA's Holding detail and the new buy's detail; TalkBack read 0.33333333 and $300.00 in full (criteria 9, 39 to 42).
- Recheck after the Deploy fix (versionCode 18, lastUpdateTime 2026-11-03 14:43:49): MA's Transactions tab shows the new buy's row as 0.33333333 shares and the reversed original as 1 share; TalkBack on Reverse of the new buy read 0.33333333 shares on its card.

### 13.4 Not run or not checked
CI and the pull request; TalkBack on the figure 0.00000001 itself (V was sold out before check 5; it is formatted by the same Quantity.format as 0.33333333); "offers Hide" in 59(e) (15); the backup-and-restore parts of 59(e) and 59(f) (31 to 34); the "ratio too large" refusal and its message on the phone (unit tested only; it needs same-date splits whose ratio passes 2,147,483,647); a Logcat check (not in section 8); the release APK on a device (unsigned, open item 4); open item 33 (34); the screens in a JVM test (none in the project).

## 14. Departures from the plan
1. Screens changed, against section 4's "none expected": a buy with a ratio stores the original's quantity, so TransactionDetailScreen.kt (quantity, price, lot "from" row), ReplaceReviewScreen.kt (the new buy's quantity and price, the banner, the "Original" hint) and ReversalScreen.kt now show a buy's exact shares, and core's Transactions.pricePerShare(transaction) divides by them. Found while building step 3; built in step 5; checked on the phone in check 3.
2. RowMappingTest: the existing test of unreadable rows had a buy with a ratio as unreadable; CR1 makes it valid (spec v5, section 8 buy row), so that row was replaced by seven rows that stay unreadable (a buy with half a ratio, 0, a negative number or two equal numbers; a sell with any ratio). Not weakened; it passed before and after the code (step 4).
3. Before each red run, the new names were added with no behaviour so the tests compiled: Quantity.shown with the old rounding (step 2); Transaction.quantityRatio, the asFilledIn parameter and ReentryRefusal.QuantityRatioTooLarge (step 3). Step 5 needed none.
4. Tests that passed on their first run: in step 2 the round-up and round-down guards (2 tests); in step 3 the typed quantity, no ratio when none is needed, and a buy with a ratio as an original (3 tests; the last runs the new path only after the code). In step 4 the changed unreadable-rows test (item 2).
5. One of my new ViewModel tests was wrong when first written: it typed 0.333333330, 9 decimals, which the field refuses. Changed to 00.33333333, the same number in other text, which decision 2 needs. No existing test changed (step 5).
6. Lot gained a boughtQuantity field (with a default), not in section 4's list, so a lot's cost per share uses a buy's exact quantity (step 3).
7. Steps 2 to 4 were built before their commits on your request ("continue first, commit later"); each step was committed on its own, in order, step 2 from the index.
8. Wording 1 was asked in step 5, after the code, before its string was added.
9. Deploy finding 1 (Medium), fixed before the pull request (your decision A): Holding detail's Transactions row showed a buy's stored quantity, so MA's re-entered buy read "Buy 1 share" while its detail showed 0.33333333 (your report on the phone, versionCode 17). Step 5's search looked for one call pattern and missed this one (item 1). Fixed in 4d9a05a with boughtQuantity; versionCode 18; rechecked on the phone (section 13.3). No failing test first: no test draws a screen (open item 47).
10. Deploy finding 2 (Low), fixed in the same commit: TalkBack on the plain reversal's card read a buy's stored quantity ("1 share") while the visible row showed 0.33333333. Rechecked with TalkBack on the phone.
11. From the findings (your decision A): a CLAUDE.md pitfall on searching every read of a field whose meaning changes, and open item 47 on screen tests.
