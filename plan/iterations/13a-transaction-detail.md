# plan/iterations/13a-transaction-detail.md

Stage: Build. Status: approved 2026-11-01, built and checked 2026-11-01 (section 12). Decisions 1 to 3 settled (section 10).
Source: plan/plan.md section 4 (row 13, split into 13a and 13b on 2026-11-01), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
In Holding detail I tap a row on the Transactions tab, or on the Cash tab I tap a buy's or sell's trade line, and the transaction opens: "< Buy AAPL", "< Sell AAPL" or "< Split AAPL", with its portfolio, date, quantity as traded, total, price per share and what it did (the lot it made and what is left of it; the lots a sell used with cost, proceeds and gain; a split's ratio and the shares before and after). A buy or sell links to its cash line, and the cash line links back. Each shows "Not reversed."; Reverse comes in 13b. Back returns to where I came from. Nothing is saved or changed.

## 2. Spec refs
- Section 6: view 6 (a Transactions row opens the transaction), view 7 (tapping a trade line opens the linked transaction, with its lots), "Cash, split and reversal entry" (the detail screens of a transaction and of a cash entry), Number format and color, Accessibility rule 5, the navigation outline.
- Wireframe 10b: frame G (transaction detail, markers 12 to 14 and marker 20) and frame H (a trade line's detail: "Open the buy or sell to reverse it." with a link to it, marker 23).
- spec 3: Transaction (quantity as originally traded), Lot, Sell allocation, the `trade` cash line; 4.4 (lots used, realized gain per piece), 4.5 and 4.10 (split-adjusted figures, exact and rounded only when shown, largest remainder for the lots used).
- Criteria: 29 (a trade line opens the linked transaction), 53 (the detail screens follow wireframe 10b; the reversal parts are 13b), and 9, 35, 39 (TalkBack included), 40, 41, 42.
- spec 14: K3 (lots, pieces, split effect: read only), K5 (the flow), K9.

## 3. Out of scope
- Reverse on a transaction, the reversal screen, reversal rows and the "Reversed on" and "Reversal of" links (13b); every transaction shows "Not reversed." in 13a, which is true until 13b.
- Reverse and re-enter (14); Hide (15); open item 44 (before 15).
- Transaction history and opening a sale from Realized gains (27, 26).
- Prices (16): no unrealized gain on the lot line.
- The phone check of open item 33 (34, 09 decision 9).

## 4. Files
Changed, app/: AppViewModel.kt (the transaction shown, where Back returns to, opening from Holding detail, from a trade line and from a cash line's link); new TransactionDetailScreen.kt and TransactionDetailFlowTest.kt; HoldingDetailScreen.kt (rows open); CashScreen.kt (trade lines open); CashEntryDetailScreen.kt (a trade line's detail links to its transaction); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 13).
core/: nothing new expected; Holdings.lots, Sells.pieces and Splits.effect give the figures. If a helper is needed it comes with its tests.
Documents: plan.md (the split of 13, the status rows), open-items.md, CLAUDE.md if needed.
No new library, no schema change and no new network call.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-13a-transaction-detail from main (c5dad53). This file, the 13b placeholder row and the plan.md lines (the split of 13, with the #107 merge). Baseline run.
2. ViewModel: tests first (a Transactions row opens its transaction and Back returns to the holding; a trade line opens its buy or sell and Back returns to the Cash tab; the cash link opens the trade line's detail and its link returns to the transaction, decision 3; the figures for a buy, a sell and a split; a split has no cash link; an unknown id is ignored; a bottom tab leaves the detail), then the code.
3. Wording questions, then the screens and strings: TransactionDetailScreen (buy, sell, split), rows and trade lines made tappable with their TalkBack role, the trade line's detail.
4. versionCode 13. Full run, release build, install over 12's build with data.
5. Phone checks (section 8).
6. Documents and the pull request draft; the open item 33 reminder.

## 6. Stubs
- "Not reversed." on every transaction and no Reverse button until 13b.
- Lots show no price until 16.

## 7. Risks
- Navigation: three ways in (Holding detail, the Cash tab, a cash line's link) and Back must return to the right place each time without a loop (decision 3).
- A buy's lot line as of today: a lot sold out shows 0, a split since changes it, and a buy dated after today has no lot yet (decision 2).
- A sell's lots used come from its stored allocations, priced on its date with the splits before it (12); they must match what the sell review showed when it was saved (59(b) style largest remainder).
- Rows that open: the touch target and the TalkBack role (button) on Holding detail's rows and the Cash tab's trade lines, at the largest font and in landscape (criterion 39; the CLAUDE.md pitfall from 10 on the fixed header).

## 8. Proof
Commands: as in 12, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 12's data (AAPL in Main: the buys of 2026-10-30, 2026-10-31 and 2026-11-01, the sells of 2026-10-31 and the sell of 4.66666667 for $1,200.00 on 2026-11-01, the splits 2 for 1 of 2026-10-31 and 1 for 3, 2 for 1 and 1 for 2 of 2026-11-01; SPY sold out). Each check names its start (CLAUDE.md pitfall from 11).
1. Install over 12's build (versionCode 12): the data is still there (criterion 35).
2. From AAPL's Transactions, the buy of 10 for $1,500.00 on 2026-10-31 opens "< Buy AAPL": Main, 2026-10-31 ET, quantity as traded 10, $1,500.00, $150.00 per share, its lot (decision 2), "Cash −$1,500.00" with a link, "Not reversed."; "<" and Back return to AAPL.
3. The sell of 4.66666667 for $1,200.00 opens "< Sell AAPL" with its three lots used (proceeds $171.43, $857.14 and $171.43; gains +$21.43, +$107.14 and +$21.43 in green), realized +$150.00 and "Cash +$1,200.00" (decision 1).
4. The 1 for 3 split opens "< Split AAPL": 1 for 3, the shares at that point 14 and after 4.66666667, no cash link.
5. From a buy's detail, the cash link opens the trade line's detail with its link back; the link returns to the buy (decision 3).
6. On the Cash tab, a "Sell AAPL" line opens its sell; Back returns to the Cash tab (criterion 29).
7. Largest font and landscape, light and dark, TalkBack: the rows and trade lines are read as buttons, each detail is read in full (criteria 39 to 41).
8. Logcat while opening details (log cleared first): no ticker, amount or quantity in the app's lines (counts only).

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-11-01)
1. A sell's detail shows the lots used as on the sell review (date, shares, cost, then proceeds and gain in color), the cost of lots used, the realized gain/loss in color and the cash link (11 wording 3), so the record matches what was confirmed (option A).
2. A buy's detail shows what is left of its lot as of today, split-adjusted, with its cost per share ("10 left, $75.00 per share", "0 left" once sold out); a buy dated after today shows its quantity as traded with "from" its date (my reading of "Lot: 10 left" in frame G; option A).
3. Back goes one step back: from a detail to where it was opened (Holding detail or the Cash tab); a buy's or sell's cash link opens the trade line's detail, whose link returns to the transaction rather than opening it again, so there is no loop (option A).
Settled by the spec, no question asked: a trade line opens the transaction, not the cash entry detail (view 7, criterion 29); a split has no cash line and so no link (4.5); the quantity shown is as originally traded (frame G, view 8); amounts carry a sign and no color, gains are colored (section 6).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "< Buy AAPL"*, "Portfolio"*, "Date"*, "Quantity, as traded"*, "Total amount"*, "Price per share"*, "Lot"* "10 left"*, "Cash −$1,500.00 >"*, "Not reversed."*.
- A sell: "Lots used"*, the lot lines of the sell review*, "Cost of lots used"*, "Realized gain/loss"*.
- A split: "Split"* "1 for 3"*, "Shares at this point"*, "Shares after split"*.
- A trade line's detail: "Open the buy or sell to reverse it."* with the link (frame H).
- New, asked in step 3: the titles of a sell and a split, the lot line of decision 2, the cash link's TalkBack sentence, and the TalkBack role and sentence of a row that opens.

## 12. Status and notes
Done. Built 2026-11-01 (steps 1 to 4); phone checks 2026-11-01, all yes, no fix needed; pull request #108 merged as 07dd814 (2026-11-01), CI green on the pull request (3 checks) and on main (2m 5s) (your reports).

## 13. Build results
### 13.1 Commits
Newest first: 06fbf79 step 4, 9578526 step 3, 12efbcc step 2, ea55cfe step 1 with this file. main was c5dad53.

### 13.2 Local run
On 06fbf79's code (2026-11-01, all with --rerun-tasks): spotlessCheck green; debug build green, no compiler warnings printed; 503 tests, 0 failures (Balance 11, CashAccount 20, CashEntriesReverse 12, CashEntries 27, Fraction 12, Holdings 15, MarketDate 10, Money 21, OpeningBalance 9, Portfolios 23, Quantity 10, Sells 19, Splits 23, TickerCheck 16, Transactions 18, RowMapping 8, AppLock 12, FinnhubSource 18, TickerVerifier 6, AppViewModel 120, BuyFlow 29, DateDigits 7, HoldingDetailFlow 11, SellFlow 21, SplitFlow 17, TransactionDetailFlow 8); lint: the 4 listed findings, none silenced (AndroidGradlePluginVersion, open item 31; NewerVersionAvailable for kotlinx-coroutines-test and for sqlcipher-android, open item 43; TypographyDashes on date_not_a_date, strings.xml:86), report written 20:52:29. Release build on the same code green (R8 ran; unsigned; "Unable to strip" names libandroidx.graphics.path.so and libsqlcipher.so, as known). Debug APK versionCode 13 (aapt2). Step 3 was committed after step 4's run with your approval, each step with its own files (you were away from the pinentry window).

### 13.3 Phone results (Android 17, debug build versionCode 13, your reports)
Checks 1 to 8 all yes. Notes:
- 1: installed over the 12 build (lastUpdateTime 20:12:36, firstInstallTime 2026-10-29 10:44:04 unchanged); net worth $4,191.87, AAPL at 1 share and $150.00, its Transactions as before (criterion 35).
- 2: the 2026-10-31 buy of 10 opened "< Buy AAPL" with Main, the date, 10, $1,500.00, $150.00 per share, "Lot 0 left", "Cash −$1,500.00 >" and "Not reversed.", no Reverse; the 2026-11-01 buy of 1 showed "Lot 1 left, $150.00 per share"; "<" and Back returned to AAPL.
- 3: the sell of 4.66666667 showed $257.14 per share, its three lots used (proceeds $171.43, $857.14 and $171.43; gains +$21.43, +$107.14 and +$21.43 in green), cost $1,050.00, realized +$150.00 in green and "Cash +$1,200.00 >".
- 4: the 1 for 3 split showed 14 and 4.66666667 with no cash row; the 2 for 1 of 2026-10-31 showed 6 and 12.
- 5: the buy's cash link opened the trade line's detail ("Back", "Buy AAPL", "For portfolio Main", the card, "Open the buy or sell to reverse it." and the link); the link, the Back button and Android Back each went one step back, with no loop (decision 3).
- 6: on the Cash tab, "Sell AAPL" +$1,200.00 and "Buy AAPL" −$150.00 opened their transactions and "<" and Back returned to the Cash tab (criterion 29); the opening balance still opened its 05 detail.
- 7: largest font, portrait and landscape, light and dark: the details and the trade line's detail scroll and wrap, the gains readable on dark; TalkBack read the rows and trade lines as their sentences with "Button", the lot as "Lot, 0 left", the cash link and the trade line's link as section 11.
- 8: Logcat cleared at 20:49:51 while details were opened; the app's uid 10454 had 59 lines, all from system tags (WindowOnBackDispatcher, Surface and single lines). No ticker, portfolio name, amount, quantity, detail word, key or token. Counts only.
- No data was changed.

### 13.4 Not run or not checked
CI and the pull request; open item 33 (34); the release APK on a device (unsigned, open item 4); contrast measurement (by eye on the phone); network traffic (open item 39); a buy dated after today on the phone ("1 from ‹date› ET"; unit tested only, no such buy on the phone); the screens in a JVM test (none in the project; the navigation and figures are tested in step 2).

## 14. Departures from the plan
1. The trade line's detail is titled with its buy or sell ("Buy AAPL") and "For portfolio Main" instead of 05's "Trade", and keeps 05's "Back" button above the title rather than frame H's "<" title row (your wording 3, header option A). A trade line's detail shows frame H's "Open the buy or sell to reverse it." with the link, in place of "Not reversed."; Reverse itself comes in 13b.
2. A shared Modifier.opensWith gives every row that opens (Holding detail's rows, the Cash tab's trade lines, the cash link) one tap target, its spoken sentence and the button role; Sell's PieceRows and a transactionTitle function are shared with the detail (step 3).
3. Phone check 2 opened a sold-out buy and a buy with shares, so both forms of the lot line were seen (your note on the 2026-10-30 buy, step 3 wording).
