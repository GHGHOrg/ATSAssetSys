# plan/iterations/15-hide-holdings.md

Stage: Build. Status: approved, built, checked and merged 2026-11-03 (section 12). Decisions 1 to 3 settled (section 10).
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

## 10. Decisions (Build, settled by you 2026-11-03)
1. Hide is offered only at exactly 0 shares counting every entry, later-dated ones included, so a hidden holding is always at 0 as section 8 needs (your A).
2. An unhide is saved in the same database transaction as the save that causes it, so it is never half done (your A).
3. Back from a Holding detail opened from Hidden holdings returns to Hidden holdings (wireframe 06 lists it as a way in) (your A).

## 11. Wording to approve (D9)
Approved in step 5 (your A, 2026-11-03); shown text, then TalkBack:
1. Holding detail's button: "Hide"; "Hide V in Main".
2. The portfolio screen's link: "Hidden holdings" with ">"; "Hidden holdings, opens a list".
3. Hidden holdings' title and portfolio box: "Hidden holdings", "Portfolio" over the name; "Hidden holdings, portfolio Main".
4. With none hidden: "No hidden holdings".
5. A row: the ticker, "0 shares" (existing text), "Last transaction 2026-11-03 ET"; "V, 0 shares, last transaction 2026-11-03 Eastern Time".
6. The row's button: "Unhide"; "Unhide V".
7. A Hide or Unhide not saved: the existing "Not saved. Nothing was changed. Try again."

## 12. Status and notes
Built 2026-11-03 (steps 1 to 6); phone checks 2026-11-03, all yes. Deploy review 2026-11-03: one Low finding (two promised tests missing), the tests added on the branch; no code changed (section 14, items 8 and 9). Pull request #117 merged as d7630a8 (2026-11-03), CI green on the pull request (3 checks) and on main (2m 56s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: d1ce050 Deploy fix (tests), 401c67f step 8, ec2adf1 step 6, fe39eee step 5, a373493 step 4, 92ebb7b step 3, b04dea0 step 2, 13f25a1 step 1 with this file. main was ae4ec04.

### 13.2 Local run
On ec2adf1's code (2026-11-03, all with --rerun-tasks): spotlessCheck green; debug and release builds green ("Unable to strip" as always); 628 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Hiding 7, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 12, ReentriesSplitCash 16, Reentries 27, Reversals 14, Sells 23, Splits 23, TickerCheck 16, Transactions 18, RowMapping 17, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HideFlow 8, HoldingDetailFlow 11, ReenterFlow 31, ReversalFlow 7, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint the 4 listed findings only (report 2026-11-03 16:01:57). Schema: 6.json has the five tables of 5.json with the same SQL, plus hidden_holdings (portfolioId and ticker, both the key). On d1ce050's code (the Deploy fix): the same run, 630 tests (HideFlow 10), 0 failures, lint the 4 listed findings only (report 2026-11-03 16:36:45).

### 13.3 Phone results (Android 17, debug build versionCode 19, your reports)
Checks 1 to 8 all yes. Each check's figures were worked out from the phone's data first; 投資信託's SPY at 0 shares and no lots confirmed by you before the install.
- 1: installed over the 14c build (lastUpdateTime 2026-11-03 16:05:02; firstInstallTime 2026-10-29 10:44:04 unchanged); the migration 5 to 6 ran with no unreadable-data screen; net worth $4,155.32, cash $3,005.32, Main AAPL 15, V 0, MA 1, the Hidden holdings link shown; Main 3 holdings (criterion 35).
- 2: no Hide on AAPL or MA; Hide on V (0 shares); tapped with no confirmation, the button went; Main listed AAPL and MA, 2 holdings; cash $3,005.32 (59(e)'s "offers Hide").
- 3: Main's Hidden holdings: the title, "Portfolio" over Main, one row V, "0 shares", "Last transaction 2026-11-03 ET", Unhide; the row opened V's detail with neither Hide nor Unhide; Back returned to Hidden holdings (criterion 51; decision 3).
- 4: Unhide with no confirmation: the row went, "No hidden holdings"; V back in Main at 0 shares, 3 holdings.
- 5: V hidden again, then its 2026-11-03 sell of 0.00000001 for $0.00 reversed from the hidden V's detail: "Reversed on 2026-11-03"; V back in Main at 0.00000001 shares, 3 holdings, Hidden holdings empty, no Hide on V, cash $3,005.32 (60(a)'s rule with your data).
- 6: 投資信託's Hidden holdings showed "No hidden holdings"; SPY (0 shares) offered Hide; hidden, it left the list and 投資信託 showed 0 holdings.
- 7: after closing the app from Recents and reopening it: SPY still hidden, one row "SPY, 0 shares, Last transaction 2026-10-31 ET, Unhide"; Main's Hidden holdings empty.
- 8: largest font and landscape (Hidden holdings scrolls as one; V's Holding detail header gives way), light and dark (the link, Hidden holdings, the plain line), TalkBack read "Hidden holdings, opens a list", "Hidden holdings, portfolio 投資信託", "SPY, 0 shares, last transaction 2026-10-31 Eastern Time" and "Unhide SPY" (criteria 9, 39 to 42, 51).
Afterwards: cash $3,005.32, net worth $4,155.32; Main AAPL 15, V 0.00000001, MA 1; 投資信託 SPY hidden.

### 13.4 Not run or not checked
CI and the pull request; TalkBack on the Hide button (no holding was at 0 and unhidden during check 8); unhiding by a new buy and by a buy pair on the phone (unit tested); a failed Hide or Unhide and a rollback of an unhiding save (unit tested for the state; Room's @Transaction needs a device test, none in the project; open item 14); the backup-and-restore parts of criteria 27 and 51 and the sample file's OLDCO (31 to 34); the release APK on a device (unsigned, open item 4); a Logcat check (not in section 8); open item 33 (34); the screens in a JVM test (open item 47).

## 14. Departures from the plan
1. The RecordStore interface additions (load, hide, unhide; the unhide list on saveBuy, saveReversal and savePair) were made in step 3 with the data layer, not in step 2, so the app compiled at every step.
2. Section 4 and the change log of 2026-11-03 call the new table "the first schema change since 09"; the last one was in 12 (version 5, the split columns). Corrected here; the earlier text is left as written.
3. Before each red run, the new names were added with no behaviour so the tests compiled: Hiding.kt returning false, empty or null (step 2); the entity, its mapping and checkHidden returning its input (step 3); the screen, the state fields and empty actions in AppViewModel (step 4).
4. Tests that passed on their first run: step 2's later-dated buy and pair-at-zero tests (the stub's "no" matched them; real guards once the code was in); step 3's row mapping (written with the stub).
5. Steps 2 to 4 were built before their commits on your request ("continue now"); each was committed on its own, in order, step 2 from the index.
6. Hidden rows sit in the transactions DAO, not a DAO of their own, so a buy, a reversal or a pair and its unhiding run in one database transaction (decision 2).
7. The store also refuses, by require, an unhide with a split's reversal (no cash line); not in section 4.
8. Deploy finding 1 (Low), fixed before the pull request (your decision A): step 4 named tests for unhiding by a pair and for a failed Unhide that were not written. Added in d1ce050, after the code: Reverse and re-enter of the hidden AAPL's 10-share sell as 4 shares unhides it in the pair's save; a failed Unhide keeps the row and says "Not saved". Both passed on their first run; no code changed.
9. To show the two tests check something, AppViewModel.kt was changed for one run only (the pair passed an empty unhide list; a failed save set no message): the pair test, the failed-Unhide test and the failed-Hide test failed; the file was then restored (git diff empty) and the full run was green. Not reviewed further: openHolding does not clear holdingFromHidden, but no path leaves Holding detail without closeHolding or a tab, both of which clear it; left as is.
