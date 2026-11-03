# plan/iterations/14c-tiny-remainders.md

Stage: Build. Status: approved 2026-11-03.
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
- 2026-11-03: drafted and approved (your A).
