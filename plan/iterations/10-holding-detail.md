# plan/iterations/10-holding-detail.md

Stage: Build. Status: approved 2026-10-31 with decisions 1 to 5 (section 10); not built. Branch iter-10-holding-detail.
Source: plan/plan.md section 4 (row 10), intent/intent.md v52 (final), spec/spec.md v4 (final). "My reading" is an interpretation, not a test.

## 1. Outcome
On a portfolio's screen I tap a holding and its detail opens. A header stays in view: the portfolio, the value, the shares, the cost and "no price, at cost", with Buy. Below it two tabs: Transactions lists this holding's buys, newest first; Lots lists each lot with its date, shares left, cost per share and lot cost. Buy opens with the ticker and portfolio filled in and returns to the holding. "<" or Back returns to the portfolio.

## 2. Spec refs
- spec section 6, view 6 (the header, the tabs, Transactions, Lots, the flags as in view 3); 4.8 (valued at cost, "no price"); 4.4 (the lots listed: those with shares, oldest first, same-date lots in entry order); 4.10 (cost per share exact, rounded only when shown, 2 decimals or 4 under $1).
- Wireframe 06: frames A and B, markers 1 to 5; frame D and markers 9 and 10 are format only (no holding reaches 0 shares before 11).
- Navigation outline: Holding detail offers Buy with the ticker filled in; it is reached from a portfolio's holdings list.
- Criteria: 48 (the parts without prices, sells, splits and classification), 9, 35, 39 to 42.
- spec 14: K3 (lots, holdings), K5 (Buy from the holding), K9.

## 3. Out of scope
- Prices, the age line and gain/loss figures (16). Sell (11). Split and the split history (12). Opening a transaction and Reverse (13), from Transactions and from the Cash buy lines. Hide and Hidden holdings (15). The Classification tab (23, 24). Reaching Holding detail from Transaction history (27).

## 4. Files
Changed, core/ (if needed): Holdings.kt (a holding's own transactions, newest first); its tests.
New, app/: HoldingDetailScreen.kt.
Changed, app/: AppViewModel.kt (a HOLDING screen with the portfolio and ticker, the tab, open and back; Buy remembers it was opened from the holding); PortfolioScreen.kt (holdings rows become tappable); AppContent.kt; strings.xml; app/build.gradle.kts (versionCode 10); BuyFlowTest.kt or a new ViewModel test file.
Documents: plan.md, open-items.md, CLAUDE.md if needed.
No schema change and no network.

## 5. Order of work
Each step ends with build, tests and lint and the report; then I stop.
1. Branch iter-10-holding-detail from main (c372de8). This file and the plan.md lines (with the #101 merge). Baseline run.
2. core/ if needed: a holding's transactions, newest first; its lots, oldest first. Tests first.
3. ViewModel: tests first (open from a holdings row; "<" and Back return to the portfolio; the tab starts on Transactions each time and is not saved; a tab of the bottom bar leaves the holding; an unlock keeps it; Buy from the header fills in the ticker and portfolio and returns to the holding after Back or a save; a saved buy shows its transaction and lot), then the code.
4. Wording questions, then the screens and strings.
5. versionCode 10. Full run, release build, install over 09's build with data.
6. Phone checks (section 8).
7. Documents and the pull request draft.

## 6. Stubs
- The header shows "no price, at cost", the value at cost and no gain/loss until 16.
- Transactions rows and the Cash buy lines open nothing until 13 (decisions 4 and 5).
- No Sell or Split in the header until 11 and 12 (decision 3); no Classification tab until 23 (decision 1).

## 7. Risks
- Holdings rows become tap targets: TalkBack reads each as one button, as the Portfolios rows since 08.
- The header stays in place while the tab content scrolls: at the largest font it could take most of the screen (criterion 39; checked on the phone).
- Cost per share is an exact fraction (for example $1,000.00 for 3 shares) and is rounded only when shown (4.10).
- The Buy screen must return to where it was opened from: a tab (09) or the holding (decision 2).

## 8. Proof
Commands: as in 09, all with --rerun-tasks. CI green on the pull request.
By hand on the phone, with 09's data (AAPL in Main, SPY):
1. Install over 09's build (versionCode 9): the data, the buys and both portfolios are still there (criterion 35).
2. Tap AAPL in Main: the header shows Main, $1,500.00, 10 shares, cost $1,500.00 and "no price, at cost"; it opens on Transactions with the buy of 2026-10-31.
3. Lots: one lot, 2026-10-31 ET, 10 left, $150.00 per share, $1,500.00, "no price".
4. Buy in the header: AAPL and Main are filled in; Back returns to the holding; a small buy saved returns to the holding with two transactions (newest first) and two lots (oldest first). Optional, as it adds data.
5. "<" and Back return to Main; a bottom tab leaves the holding; reopening starts on Transactions.
6. Largest font, light and dark, TalkBack.

## 9. Research
None planned; no new library and no network.

## 10. Decisions (all Build, settled by you 2026-10-31)
1. Only the Transactions and Lots tabs until 23 adds Classification (option A).
2. Buy in the header opens the Buy screen with this holding's ticker and portfolio filled in; the date, quantity and total stay as last left. "<", Back and a saved buy return to the holding (option A).
3. Only Buy in the header; Sell joins in 11 and Split in 12 (option A).
4. Transactions rows are not tappable until 13 adds the transaction screen (option A).
5. The Cash tab's buy lines stay untappable until 13, which opens the transaction from them; 09's stub target moves from "10 or 27" to 13 (option A).

## 11. Wording to approve (D9; * = given by the wireframes or earlier iterations)
- "Shares"*, "Cost"*, "Unrealized gain/loss"* (existing), "no price, at cost"* (09), "Buy"* (09), "Transactions"*, "Lots"*, "No lots with shares"* (format only in 10).
- Transaction row as wireframe 06 frame A: "Buy, 2026-10-31 ET", "10 shares", "$1,500.00"*.
- Lot row as frame B: "2026-10-31 ET", "10 left, $150.00 per share", the lot cost, and "no price" in place of the gain/loss*.
- Approved in step 4 (2026-10-31):
  - Wording 1 (option A): the header shows the portfolio, the value, "Shares", "Cost" and "Unrealized gain/loss" with "no price, at cost" as its value; TalkBack "AAPL in Main, value 1,500.00 dollars, 10 shares, cost 1,500.00 dollars, unrealized gain/loss: no price, at cost".
  - Wording 2 (your change, option B): a transaction is one line, date first, separated by spaces only: "2026-10-31 ET   Buy   10 shares   $1,500.00"; the amount wraps below at the largest font; TalkBack "2026-10-31 Eastern Time, buy, 10 shares, 1,500.00 dollars". The tabs read as Android's own tabs. A lot: "2026-10-31 ET", "10 left, $150.00 per share", the lot cost and "no price"; TalkBack "Lot of 2026-10-31 Eastern Time, 10 left, 150.00 dollars per share, cost 1,500.00 dollars, no price".
  - Wording 3 (option A): a holdings row on a portfolio's screen reads as in 09, now as a button; Buy in the header reads "Buy AAPL in Main" for TalkBack.

## 12. Status and notes
Approved 2026-10-31. Not built.
