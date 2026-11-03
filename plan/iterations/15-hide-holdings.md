# plan/iterations/15-hide-holdings.md

Stage: Build. Status: approved 2026-11-03.
Source: plan/plan.md section 4 (row 15), intent/intent.md v52 (final), intent/intent-cr-01-tiny-remainders.md (CR1), spec/spec.md v5 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a holding's detail at 0 shares I tap Hide. The holding leaves the portfolio's holdings list and its count, and appears in that portfolio's Hidden holdings, reached from the portfolio's screen, where Unhide puts it back at 0 shares. A new buy of that ticker, or the reversal of a sell that gives it shares again, unhides it on its own. Selling everything never hides a holding by itself (intent v52; spec 4.4).

## 2. Spec refs
- Spec 4.4 (Hide and Unhide, "0 only when exact", a save that leaves a hidden holding with shares unhides it), 4.6 (reversing a sell unhides), 4.8 (counts and values), view 3 (the holdings list, the count, the link), view 6 (the Hide button), view 9 (Hidden holdings), section 8's `hidden` row (spec only; built in 31).
- Wireframes: 09 frames A and B, markers 1 to 4; 06 marker 9; 03 markers 1, 5 and 7.
- Criteria: 27 (its backup-and-restore part in 31 to 34), 51 (with the sample file in 34; here with your data), 60(a) on K1, 59(e)'s "offers Hide", and the standing criteria 9, 35, 39 to 42.
- Spec 14: K1, K2 (a new stored record), K5, K9.

## 3. Out of scope
- The `hidden` row of the backup file (31 to 34).
- Allocation and Transaction history links on the portfolio's screen (18, 19, 27): only Hidden holdings is added now.
- Deleting a portfolio's hidden rows (29).
- Open items 45, 47 and 32; the phone check of open item 33 (34).

## 4. Files
New, core/: Hiding.kt (K1, rules only: Hide offered at 0 shares, decision 1; which hidden holdings a save leaves with shares and so unhides; the last transaction date for a row) and its tests.
Changed, core/: RecordStore.kt (load, hide, unhide; each save takes the holdings it unhides, decision 2); Holdings.kt if a helper for a holding's shares counting every entry is needed.
New, app/: HiddenHoldingsScreen.kt; app/schemas/.../6.json (exported by Room); HideFlowTest.kt.
Changed, app/: AtsDatabase.kt (the table hidden_holdings, portfolio id and ticker; version 6; AutoMigration 5 to 6); RoomRecordStore.kt (hide, unhide, load; saves that unhide in the same database transaction); AppViewModel.kt (hidden holdings in the state; Hide and Unhide; the lists and counts without hidden holdings; Screen.HIDDEN_HOLDINGS; unhiding after a buy, a sell's reversal and a pair); HoldingDetailScreen.kt (Hide); PortfolioScreen.kt (the link; the list without hidden holdings); PortfoliosScreen.kt (the count); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 19); RowMappingTest.kt; the fake stores.
Documents: plan.md, open-items.md if needed, CLAUDE.md if needed.
No new library and no new network call. One schema change, the first since 09.

## 5. Order of work
Each step ends with build, tests and lint (all with --rerun-tasks) and the report; then I stop.
1. Branch iter-15-hide-holdings from main (ae4ec04). This file and the plan.md lines (15's status row; the 14c status row with the #116 merge record; the change log), and the 14c file's status. Baseline run (611 tests).
2. core/: tests first, shown red, then the code. Hide offered only at exactly 0 shares counting every entry (decision 1), not at 0.00000001 (CR1); 60(a) (opening $1,000, buy 10 AAPL for $100 on Jan 2, sell all 10 for $120 on Jan 3, hide, reverse the sell: 10 shares, no longer hidden); a new buy of a hidden ticker unhides it; Reverse and re-enter of a sell at 0 keeps it hidden (60(b): the pair ends at 0); a buy of the same ticker in another portfolio does not unhide; the row's last transaction date includes a reversal.
3. Data layer: the table and the migration 5 to 6 with the exported schema 6.json; hide and unhide; each save that unhides does it in its own database transaction (decision 2); row-mapping tests; a loaded hidden row with shares is unreadable, nothing guessed (as 13b for damaged links).
4. ViewModel: tests first (Hide offered at 0 and not otherwise; Hide removes the holding from the list and the count; Hidden holdings per portfolio in ticker order; Unhide; Holding detail from a Hidden holdings row with neither button, Back to Hidden holdings, decision 3; a buy, a sell's reversal and a buy pair each unhide; a failed hide or unhide leaves the state as it was), then the code.
5. Wording questions, then the screens and strings: Hide on Holding detail; the Hidden holdings link on the portfolio's screen; the Hidden holdings screen (frames A and B).
6. versionCode 19. Full run, release build, install over 14c's build (the migration runs on the phone).
7. Phone checks (section 8).
8. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- None.

## 7. Risks
- The first schema change since the encrypted database holds your data: AutoMigration 5 to 6 runs on the phone's database. Room 3.0.3 has no allowDataLossOnRecovery and the code calls no destructive fallback (CLAUDE.md), so a failed migration shows the unreadable-data screen, not a quiet data loss. Check 1 proves it on the phone; the migrations 1 to 5 ran the same way.
- "0 shares" as of today vs counting every entry: 4.8 shows holdings as of today, but section 8 needs a hidden holding at 0 after every entry; a holding at 0 today with a later-dated buy would break that (decision 1).
- Saves that unhide: a buy, a sell's reversal, a buy pair, and a sell pair whose new sell takes fewer shares must all unhide in the same database transaction; a split pair, a split or a cash entry never moves a holding from 0. Missing one leaves a hidden holding with shares; the load check of step 3 then stops at the unreadable-data screen rather than showing it wrongly.
- Screen text is checked only on the phone (open item 47; CLAUDE.md pitfall from 14c).
- The holding count on the Portfolios tab and the portfolio's list must leave out hidden holdings everywhere they are worked out; step 4 tests each.

## 8. Proof
Commands: as in 14c, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 14c's data (Main: AAPL 15 shares, V 0 shares, MA 1 share; 投資信託: SPY at 0 shares, to be confirmed by your photo; cash $3,005.32; net worth $4,155.32). Each check names its start and the control; the figures are worked out from the phone's data first.
1. Install over 14c's build: the migration ran and the data is still there (criterion 35).
2. Main, V: Hide shown; AAPL and MA show no Hide. Tap Hide: V leaves Main's list and Main's count on the Portfolios tab drops by 1 (59(e)'s "offers Hide").
3. Main, Hidden holdings: one row, V, "0 shares", "Last transaction 2026-11-03 ET", Unhide; tapping the row opens Holding detail with neither Hide nor Unhide; Back returns to Hidden holdings.
4. Unhide: the row goes, the plain line shows, V is back in Main's list at 0 shares and in the count.
5. Unhidden by a save: hide V again, then reverse V's last sell (0.00000001 for $0.00, no cash change): V has 0.00000001 shares, is back in the list, and Hidden holdings shows the plain line (60(a)'s rule, with your data).
6. 投資信託, Hidden holdings with none hidden: the plain line.
7. Close and reopen the app: a hidden holding stays hidden.
8. Largest font and landscape, light and dark, TalkBack on a Hidden holdings row (ticker, shares, date and Unhide in full; criterion 51) (criteria 9, 39 to 42).

## 9. Research
- None expected: Room's AutoMigration for a new table is used as in 1 to 5; the exported schema shows the SQL.

## 10. Decisions needed from you (Build; asked one at a time)
1. Hide is offered at exactly 0 shares counting every entry, later-dated ones included, so a hidden holding is always at 0 as section 8 needs; or at 0 as of today only.
2. Unhiding is saved in the same database transaction as the save that causes it, so it is never half done; or right after the save.
3. Back from a Holding detail opened from Hidden holdings returns to Hidden holdings (wireframe 06 lists it as a way in); or to the portfolio's screen.

## 11. Wording to approve (D9)
- Asked in step 5: Hide; Unhide; the "Hidden holdings" link and title; "No hidden holdings"; "0 shares"; "Last transaction %s ET"; the TalkBack sentences for a row and the buttons. The wireframes give placeholders only.

## 12. Status and notes
- 2026-11-03: drafted and approved (your A).
