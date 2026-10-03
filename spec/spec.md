# spec.md (DRAFT v2, awaiting approval)

Source: intent/intent.md v51 (final). Stage: Design.
Nothing in this file has been built, run or verified against real data. Items marked **[UNVERIFIED]** rest on my memory or assumptions, not on checks done in this session.

## 1. Summary
Single-user Android app (sideloaded). Local-only data. Tracks one shared cash account plus multiple US stock/ETF portfolios in USD. First version delivers net worth, allocation, lot-based cost and gains, transaction entry, backup/restore (the only way data enters or leaves the app) and app lock. Performance over time comes after v1, but all transactions are stored from day one.

## 2. Concerns and conflicts (read first)

| # | Severity | Concern | Proposed handling (needs your approval) | Status |
|---|----------|---------|------------------------------------------|--------|
| C1 | High | **"Live or near-real-time" price freshness vs "free data only".** Free sources are usually delayed, rate-limited, or unofficial and can break. Constraints and Decisions already differ: Decisions says near-real-time is best effort. | Spec treats freshness as best effort. Every price shows its age and a stale marker. The price source sits behind an interface so it can be swapped, and you can switch it in settings (section 7). Source choice happens in plan.md after a real check **[UNVERIFIED: no free source has been tested]**. | Approved |
| C2 | High | **First version is large for "ASAP"**: 9 views plus many interacting rules. | Build in the slices in section 12. Decision D2: realized gains and sector/geography stay in v1 (you accepted the schedule risk). They are built last, so the rest can be used first. | Approved |
| C3 | High | **Permanent transactions plus a one-time load of a hand-prepared file.** A bad load cannot be edited away. Only restoring a backup undoes it. | The whole file is validated before anything is deleted, and every error is listed with its row and reason. If validation fails, nothing changes. If the app holds data, it shows a count summary, offers a backup export first and requires a typed confirmation (section 8). | Revised, needs approval |
| C4 | Medium | **Cash lines made by buys and sells have no type among the five cash types.** | Add a system-only type `trade`. It cannot be entered by hand. It appears in the cash list and filters. | Approved |
| C5 | Medium | **How a reversal is represented is not defined.** A mistaken buy is offset by a sell-like entry, which could create a realized gain/loss and must pick lots. | A reversal is a normal entry carrying a link "reverses #id" and follows all normal blocking rules. Full rules for reversing a buy, sell, split or cash entry, plus the date and amount rules, are in 4.6. | Approved |
| C6 | Medium | **Withdrawn (v52).** There is no CSV import, so no per-import portfolio target and no cross-file double counting. Cash entries are records in the restore file; trade cash lines are rebuilt by replay. ID kept so references stay valid. | n/a | Withdrawn |
| C7 | Medium | **Error reports can cascade.** In a hand-made file, one bad buy also makes later sells of those shares fail. | The restore is all-or-nothing, so nothing is saved either way. Records are replayed in date order, then file order. The report lists every error, and dependent ones say "depends on row N" so you fix root causes first. | Revised, needs approval |
| C8 | Medium | **Ticker check needs internet.** A hand-prepared file cannot be fully validated offline. | The restore needs internet. Every ticker with shares left at the end of the file is verified before anything is deleted; one that fails blocks the restore. Sold-out tickers stay unverified, and a later manual buy of one is blocked until verified online (4.3). Whether verification must use the selected price source is open (D6). | Revised, needs approval |
| C9 | Medium | **Split handling is manual.** A forgotten split silently distorts holdings, cost per share and gains. | No automation (per intent). The holding detail shows the split history so omissions are easy to spot. No price-based warning is needed. | Approved |
| C10 | Low | **Realized gains can differ from broker/tax figures** (wash sales not modelled). | Label realized gains "informational" in the UI. Decision: No label is added. | Closed, not a concern |
| C11 | Low | **"Performance" moves with deposits/withdrawals**, so it is not investment return. | Label it "Total value change" in the later version. | Approved |
| C12 | Low | **"Allocation by holding" is ambiguous across portfolios.** | Per-portfolio view: one slice per holding in that portfolio. Combined view: same ticker in several portfolios is merged into one slice. | Approved |
| C13 | Low | **Dates and time zones.** You are in Tokyo, markets are in New York. | Entries carry a calendar date only, no time. All dates (buy, sell, split and cash entries) use the US market time zone (New York), not the device's. The app does no time zone conversion: the date is stored as picked. The date field defaults to today's date in New York. Entry order is the tie-breaker within a day. | Approved |
| C14 | Low | **Sector/geography for ETFs** is often missing from free sources **[UNVERIFIED]**. | Manual override per holding. Unclassified holdings show as "Unclassified". | Approved |

## 3. Domain model

- **Cash account** (exactly one): ordered list of cash entries. Balance on a date = sum of entries dated on or before that date.
- **Cash entry**: `id`, `date`, `amount` (signed), `type` in {opening balance, deposit, withdrawal, interest/dividend, adjustment, trade}, `note`, `entered_seq`, optional `reverses_id` (set on a cash-entry reversal), optional `transaction_id` (set only on `trade` entries; points to the buy or sell, including reversals, that created it).
  - Note is required for adjustments, optional for all others.
  - `trade` entries are created only by buys/sells: exactly one per buy or sell (reversals included), none for splits.
- **Portfolio**: `id`, `name`.
- **Holding**: a ticker within a portfolio. Has `hidden` flag. Derived from transactions.
- **Instrument**: ticker, name, asset type (stock or ETF), sector, geography, verified flag, latest price, price timestamp, stale flag.
- **Transaction** (per portfolio): `id`, `date`, `entered_seq`, `type` in {buy, sell, split}, ticker.
  - Buy/sell: `quantity` (up to 8 decimals), `total_amount` (fees included, derived price per share = total / quantity).
  - Split: ratio (e.g. 2:1, 1:10), fractional shares allowed.
  - Optional `reverses_id`. An entry with `reverses_id` is a reversal of a buy, sell or split and is shown as such. Its effect is defined by 4.6, not by the normal buy and sell input rules in 4.3 and 4.4 (for example, it has no lot picking), including split adjustment for the dates between original and reversal. It still follows the blocking rules in 4.1.
- **Lot**: created by a buy. `portfolio`, `ticker`, `buy_date`, `original_qty`, `remaining_qty`, `cost_per_share`. Splits adjust quantity and cost per share, never total cost.
- **Sell allocation**: sell id, lot id, quantity taken from that lot.
- Quantities are stored as originally traded, with splits applied by the splits recorded. No stored value snapshots.

## 4. Business rules

### 4.1 Ordering and checking
- Global order is `date`, then `entered_seq` (order of entry).
- Dates are calendar dates with no time. All dates (buys, sells, splits and cash entries) are US market dates (New York). "Today" (the default date in entry forms, and the line between past and future-dated entries) is the current date in New York.
- On any save (including reversals) and on every restore replay, the app replays cash and share balances from that date through all later dates. Any date where cash < 0 or shares < 0 blocks the save.
- Future-dated entries count in net worth only from their date.

### 4.2 Cash
- Buy: cash out on the trade date, by the total amount. Sell: cash in on the trade date.
- Withdrawal larger than the balance on that date or any later date is blocked.
- Exactly one opening balance. It must be the earliest cash entry. It is never reversed or replaced.
- A cash difference vs reality is fixed with an adjustment deposit/withdrawal with a note.

### 4.3 Buys
- Input: ticker, date, quantity, total amount. Quick review screen before saving.
- The total amount (fees included) must be more than 0. Zero is allowed on sells only (4.4).
- Blocked if total exceeds cash (checked per 4.1).
- Blocked if the ticker has never been verified and the phone is offline. When online, the ticker is checked before saving, and a ticker that does not exist is blocked. The entry flow (K5) enforces this using the verified flag that the price service (K4) maintains. The rules engine (K1) does not know about the network.
- No check against market price.
- A buy dated before a recorded split: quantity entered as originally traded, later splits applied automatically.

### 4.4 Sells
- Input: ticker, date, quantity, total amount (zero allowed), and lot picks. Quick review screen before saving.
- Lot rules: same portfolio only; lot bought on or before the sell date; same-date lot must have been entered earlier. Each lot pick has its own quantity, prefilled with the lot's remaining quantity.
- The lot picker lists each lot of that ticker in the portfolio that still has shares, with: purchase date, quantity remaining, cost per share, and the lot's unrealized gain/loss. Quantity and cost per share include splits recorded so far. Unrealized gain/loss = remaining quantity × latest price − remaining cost of that lot (4.8). If the price is stale it carries the stale marker. If the holding has no price at all, the lot shows "no price" instead of a gain/loss. A lot that fails the lot rules for the sell date you entered is shown greyed out with the reason (for example "bought after the sell date") and cannot be picked. Changing the date updates the list. Lots are listed oldest first by purchase date (lots bought on the same date in the order they were entered), and greyed-out lots keep their place in that order.
- The picked quantities must add up to the sell quantity. "Sell all" fills the exact remaining quantity.
- Selling more than the portfolio holds is blocked.
- Realized gain per lot piece = proceeds share minus cost share. Proceeds are split across lots in proportion to quantity.
- A total loss is a sell with zero proceeds (realized loss = cost of the lots consumed).
- A holding with zero shares stays visible until you hide it. Only fully sold holdings can be hidden. A new buy of a hidden ticker unhides it.

### 4.5 Splits
- Entered by hand: date, ticker, ratio. Applies to all lots of that ticker in that portfolio held on that date. Total cost per lot is unchanged.

### 4.6 Corrections
- Saved transactions cannot be edited or deleted. A mistake is offset by a reversal (C5).
- A reversal is all-or-nothing and links to the original ("reverses #id"). A typo is fixed by reversing the whole entry and entering a correct one.
- Reversals follow the same blocking rules (4.1).
- A reversal cannot itself be reversed. If you reversed something by mistake, re-enter the original as a new entry.
- **Amount:** fixed to the original; not typed.
- **Date:** defaults to the original date. You may pick another date (for example today); the original's effect then stays in place between the two dates.
- **Splits between the original and the reversal:** a reversal's quantity is the original's quantity as originally traded, and any split dated after the original and on or before the reversal is applied to it, the same as-traded rule as 4.3. The reversal therefore removes or restores the same economic shares, and total cost and cash amounts are unchanged. Reversing a buy removes the original lot's whole remaining piece from that buy, now split-adjusted. Reversing a sell restores the exact lots it consumed, with quantities and cost per share adjusted by the splits since the sell date. Reversing a cash entry or a split is not affected.
- **Reversing a buy:** takes the shares back out of the original lot (no lot picking). Realized gain/loss is 0. Its cash line returns the original total. Blocked if any of those shares were sold; reverse the later sells first (Example D).
- **Reversing a sell:** puts back the exact lots and quantities the sell consumed, at their original cost per share. Removes the proceeds from cash and cancels the sell's realized gain/loss. Blocked if cash would go negative on any date (4.1).
- **Reversing a split:** applies the inverse ratio for that ticker and portfolio, dated per the date rule above.
- **Reversing a cash entry:** adds an opposite-amount entry linked to the original, with type `adjustment` and the note pre-filled "Reversal of #id". The opening balance is never reversed (4.2). `trade` lines cannot be reversed directly; reverse the buy or sell instead.

### 4.7 Portfolios
- Create, rename, delete (including the last one, and all of them).
- Delete requires typing a confirmation after a summary of what will be removed: transaction count, trade cash lines, realized gains.
- Deleting removes the portfolio's transactions and their cash lines, then recomputes history as if it never existed.
- If removing those cash lines would make cash negative on any date, deletion is blocked. The message states the first date it goes negative, the largest shortfall, and the fix: a backdated deposit adjustment (with a note) dated on or before that first date for at least the shortfall.
- This is the only exception to permanent transactions. The way back is a backup restore.

### 4.8 Calculations
- Net worth = cash balance + sum over open holdings of (shares × latest price).
- Cost of holding = sum of remaining lot cost. Unrealized gain/loss = market value − cost.
- Allocation: by asset type (cash, stocks, ETFs), portfolio, holding (C12), sector/geography. Combined and per portfolio.
- A price without a fresh quote uses the last known price and is marked stale. If no price has ever been fetched, that holding is valued at cost and flagged "no price".

## 5. Worked examples (check my reading)

**Example A: lots and gains.** Opening balance $10,000 on Jan 1. Portfolio P:
1. Jan 2 buy 10 AAPL, total $1,000 → lot 1, $100/share. Cash $9,000.
2. Feb 1 buy 10 AAPL, total $1,500 → lot 2, $150/share. Cash $7,500.
3. Mar 1 sell 12 AAPL, total $2,100 ($175/share), picks lot 1 × 10 and lot 2 × 2. Cash $9,600.

Cost consumed = $1,000 + 2 × $150 = $1,300. Realized gain = $2,100 − $1,300 = **$800**. Remaining: lot 2 with 8 shares, cost $1,200. If AAPL is $160, unrealized = 8 × $160 − $1,200 = **$80**.

**Example B: split.** Assume there is enough cash throughout.
1. Jan 2: buy 10 shares, total $1,000 → 10 shares at $100/share.
2. Mar 1: you record a 2:1 split. The holding becomes 20 shares at $50/share. Total cost is still $1,000.
3. Afterwards you enter a buy you forgot, dated Feb 15: 5 shares, total $500. You type 5, the quantity as originally traded. It is stored as 5 shares, and the Mar 1 split is applied automatically, so it counts as 10 shares at $50/share with cost $500.

Result: the holding is **30 shares** with total cost **$1,500** (20 shares from step 2 plus 10 from step 3).

**Example C: backdated block.** Opening balance $1,000 on Jan 1. Buy for $800 on Jan 10. You try a withdrawal of $300 dated Jan 5. On Jan 5 the balance would be $700 (fine), but on Jan 10 it would be 700 − 800 = **−$100**, so the withdrawal is **blocked**.

**Example D: buried mistake.** Buy 10 shares on Jan 2, sell all 10 on Jan 3. Reversing the Jan 2 buy is blocked (shares would be −10 on Jan 3). You must first reverse the Jan 3 sell, then the buy.

**Example E: blocked portfolio deletion.** Opening $1,000 Jan 1. Portfolio Q buys shares for $400 on Jan 5 (cash $600) and sells them for $900 on Feb 1 (cash $1,500). Direct withdrawal of $1,200 on Feb 10 (allowed, balance $300 after). Deleting Q removes both cash lines (−$400 and +$900), so on Feb 10 the balance would be $1,000 − $1,200 = −$200. Deletion is blocked: "Cash would be negative from Feb 10 by $200. Add a deposit adjustment of at least $200 dated on or before Feb 10."

**Example F: reversal after a split.** Opening balance $10,000 on Jan 1.
1. Jan 2: buy 10 shares, total $1,000 → lot of 10 shares at $100/share. Cash $9,000.
2. Mar 1: you record a 2:1 split. The lot becomes 20 shares at $50/share. Total cost is still $1,000.
3. Apr 1: you reverse the Jan 2 buy. The reversal's quantity is the original 10, with the Mar 1 split applied, so it removes **20 shares**. Its cash line returns **$1,000**.

Result: the lot has 0 shares, cash is **$10,000**, and realized gain/loss is **0**. Without the split rule, 10 shares would remain while the cash came back.

## 6. Views (first version)
Layout and navigation are decided by the screens listed below and the navigation outline after them.
1. **Overall**: net worth + allocation summary; net worth + each portfolio with gain/loss.
2. **Net Worth (Total)**
3. **Portfolios and holdings**
4. **Allocation** (asset type, portfolio, holding, sector/geography)
5. **Realized gains** (per sale, per holding, total)
6. **Holding detail**: lots, cost, gain/loss, split history, its transactions
7. **Cash account**: balance, entries incl. trade lines, date-range filter. Each `trade` line shows buy or sell, ticker and portfolio name besides date and amount. Tapping it opens the linked transaction (with its lots and any reversal link).
8. **Transaction history** (per portfolio only): filters ticker, date range, type (buy, sell, split, cash). Cash means the cash lines of that portfolio's trades; direct cash entries are viewed in the cash account. Each buy or sell shows its cash effect and can open its cash line. A reversal links to the original, and its cash line points to the reversal, so the full chain is traceable.
9. **Closed/hidden holdings**

Naming note: intent.md v51 calls view 1 "Home" and view 2 "Net worth overview". This spec renames them "Overall" and "Net Worth (Total)". Intent.md is left at v51, so use these spec names from here on.

Plus entry flows: buy, sell (lot picker), split, cash entry, backup/restore (restore needs internet), settings (auto-lock, price source, tab order).

**Navigation outline.**
- After unlock, the app opens on the Overall tab (view 1). A bottom bar with four tabs is always visible. The default order, left to right, is: More, Cash, Portfolios, Overall. You can reorder the tabs in settings.
- **Overall** (1): tap the net worth figure for Net Worth (Total) (2); tap the allocation summary for Allocation (4); tap a portfolio for its holdings (3).
- **Portfolios** (3): the list of portfolios. Tapping one shows its holdings. A holding opens Holding detail (6). From a portfolio you reach its Transaction history (8) and its Closed/hidden holdings (9). Portfolio create, rename and delete (4.7) are here.
- **Cash** (7): the cash account, with its entries and date-range filter. A `trade` line opens the linked transaction (section 6, item 7). Cash entry (deposit, withdrawal, interest/dividend, adjustment) starts here.
- **More:** Allocation (4), Realized gains (5), Backup/restore, Settings (auto-lock, price source, tab order). Net Worth (Total) (2) is also reachable here.
- **Buy/Sell button:** Overall, Portfolios and Cash show a Buy/Sell button that opens a short menu: Buy, Sell. Holding detail also offers Buy and Sell with the ticker already filled in, and starts a split. A reversal starts from the transaction or cash entry it reverses (4.6).
- Android's back button returns to the previous screen. Auto-lock (section 9) can cover any screen.

Date fields: every screen that asks for a date shows the note "Enter date in US market time (New York)" next to the input. This covers buy, sell, split and cash entry, the reversal date, and the date-range filters in the cash account and transaction history. The restore screen shows the same note next to its date format help (section 8). Dates that are only shown (lists, detail screens, review screens, restore summary) carry no note. They show the time zone suffix "ET" after the date, for example "2026-03-01 ET".

Targets: adding a buy or sell takes under 30 seconds, with lots shown ready to tap.

## 7. Prices and market data
- Automatic refresh when online, plus pull-to-refresh. Manual entry is for transactions only.
- Each price shows its timestamp. Failure keeps the last price, marked stale.
- Sending tickers to an outside source is accepted. Free tier only.
- The price source is behind an interface so it can be replaced. **[UNVERIFIED]** Source, limits and whether it provides sector/geography are to be checked in the plan stage.
- **Price source setting.** In settings you choose the price source from the sources the app ships with (the list is decided in plan.md). The app starts on a default source.
- **Sweep on change.** After you confirm a change of source while online, the app runs a price sweep: it fetches the latest price for every instrument the app holds (hidden holdings included) from the new source, and updates all prices. Net worth, gains and allocation then show the new market values.
- **Changing the source while offline is allowed.** The app shows a warning that prices were not refreshed, selects the new source, and skips the sweep. Prices stay as they are until the next refresh (automatic when online, or pull-to-refresh), which uses the new source.
- **Failures during the sweep.** A ticker the new source cannot price keeps its last price, marked stale (the rule above). When the sweep ends, the app lists the tickers it could not price. The new source stays selected.
- **What a source change never touches:** transactions, cash entries, lots, the verified flag, and manually entered sector/geography. Source-supplied sector/geography is refreshed from the new source where it provides them.
- Historical prices are needed only for the later performance feature. **[UNVERIFIED]** Availability, limits and split adjustment are unchecked. Because splits are user-entered, any split-adjusted history would have to be converted back to as-traded values before use. Verify before the performance milestone, not before v1.

## 8. Backup file and restore (App initialization)
Fixed by intent v52:
- The backup file is the only way data enters or leaves the app. No CSV import, no CSV export, no merge.
- App initialization = delete all settings and data, then load the file, as one confirmed action. The file has no other use.
- The app's own backups and your hand-prepared file use one format. Preparing the file from broker statements happens outside the app.
- The restore is all-or-nothing.

**File contents**
- In the file: portfolios; all transactions (buys, sells, splits, and reversals with their links); all cash entries (reversals with their links); hidden holdings; manual sector/geography overrides; the sector and geography lists; all settings (auto-lock, price source, tab order).
- Not in the file: lots, trade cash lines, holdings, prices, the phone's PIN or biometric, the screenshot window. The app rebuilds lots, trade cash lines and holdings by replay.
- Lot labels in the file tie each sell to its buys. They mean nothing outside the file. After a successful restore the app generates its own unique lot IDs.
- Layout, date and number formats, how reversals and lot labels are written, and a format version: open (D5).

**Restore steps**
1. Online only. Offline, the restore cannot be started (C8).
2. The whole file is read and validated. Records are replayed in date order, then file order (4.1), each checked against 4.1 to 4.6 as if entered by hand. Every error is collected with its row and reason (C7). Nothing is deleted yet.
3. Every ticker with shares left at the end of the file is verified through K4. One that cannot be verified blocks the restore. Sold-out tickers are not verified (C8).
4. If any check fails, nothing changes and the report is shown.
5. A summary shows counts of portfolios, transactions and cash entries, now versus in the file. [PROPOSED ADDITION, not in intent: also the resulting cash balance and number of holdings, to compare with real life.]
6. If the app holds data, it offers a backup export first, then requires a typed confirmation.
7. Delete and load happen as one action, so the app is never left empty in between. The app then generates its own IDs. Holdings show "no price" until the first successful price refresh (4.8).

**Rules carried over from the CSV design** [needs your confirmation]
- At most one opening balance, none required. If present, it must be the earliest cash entry (4.2).
- Reasons a file is rejected include: wrong format version or layout, unknown record type, bad date or number, missing required value, too many decimals, wrong sign, zero total on a buy, lot label not found or picks not summing to the sell quantity, blocked by 4.1 to 4.6, bad reversal link, adjustment without a note, a held ticker that cannot be verified, and "depends on row N".

**Known limits**
- Depends on a free third-party source (C1, unverified), may hit rate limits on a large file, and is slower.
- A restored lock setting can be weaker than the one on the new install.
- A bad file that loads cleanly can be undone only by restoring a backup (C3).

## 9. Backup, restore, security
- Data stored on the phone only, in app-private storage.
- Lock: biometric or PIN. Auto-lock choices: every time you leave the app (default), after 1 min idle, after 5 min idle, only when the phone locks or the app restarts.
- Backup: export one file, in the format of section 8, to a location you choose. Unencrypted allowed. It is the only way data leaves the app.
- Restore: replaces all current data from one file, per section 8. There is no merge. It needs internet.
- Works offline with last known prices. Price refresh and restore need internet.

## 10. Out of scope
As in intent.md: liabilities, other asset classes, notifications, tax reporting, per-holding dividends, separate fees, multi-currency, account syncing, Play Store release, CSV import, CSV export and merging data.

## 11. Acceptance criteria (first version)
1. Net worth and allocation show correctly from your real data (after restoring your prepared file).
2. Examples A to F behave as written, as automated tests.
3. A buy or sell can be entered in under 30 seconds.
4. Backup then restore on a clean install reproduces identical portfolios, transactions, cash entries, lots, cash balance and holdings. Net worth matches after the first successful price refresh (holdings show "no price" until then).
5. Offline use shows last prices marked with their age.
6. App locks per the chosen auto-lock setting.
7. Changing the price source in settings, while online, fetches all prices again and updates market values; tickers the new source cannot price keep the last price, marked stale. While offline, the change shows a warning and skips the sweep.
8. Restore cannot be started while the phone is offline. Online, every ticker still held at the end of the file is verified before anything is deleted, and one that fails blocks the restore.
9. Every screen that asks for a date shows the note that dates are US market (New York) dates next to the input. Every date that is only shown has the time zone suffix "ET" and no note.
10. In a portfolio's transaction history, the cash filter shows only the cash lines of that portfolio's buys and sells, never direct cash entries.
11. The realized gains view shows the gain per sale, per holding and as an overall total. For Example A, the sale shows a realized gain of $800.
12. Each holding shows its sector and geography. The source supplies them when it can, and you can override them by hand. A holding with neither shows "Unclassified". Allocation by sector/geography uses these values.
13. Every view and entry flow in section 6 can be reached by the navigation outline. A buy or sell can be started from Overall in two taps (Buy/Sell button, then Buy or Sell). The tab order can be changed in settings.
14. The sell lot picker lists the lots of the ticker in that portfolio, oldest first, each with purchase date, quantity remaining, cost per share and unrealized gain/loss. Lots that fail the lot rules for the entered sell date are greyed out with a reason and cannot be picked. For Example A, before the Mar 1 sale at a price of $160, the picker shows a gain of $600 for lot 1 and $100 for lot 2.
15. A manual buy of a never-verified ticker is blocked while the phone is offline. Online, a ticker that does not exist is rejected, and a valid one is saved.
16. A file with errors changes nothing, and the report lists every error with its row and reason.
17. Restoring onto an app that holds data shows the count summary, offers a backup export first and requires a typed confirmation.

## 12. Proposed slices (input to plan.md)
1. Data model, cash account, rules engine with tests (Examples A to F).
2. Buy/sell/split entry, lot picker, holdings, cost and gains.
3. Prices (including the price source setting and sweep), net worth, the Overall view and allocation.
4. Portfolio management and deletion rules.
5. Lock, backup and restore (format in section 8; the only way your real data gets in).
6. Realized gains view, sector/geography (kept in v1 per D2; built last).

## 13. Open design points
- D1 SUPERSEDED by v52: no CSV import. The backup-file format is D5.
- D2 RESOLVED: realized gains and sector/geography stay in v1.
- D3 SUPERSEDED by v52: no merge.
- D4 RESOLVED: a reversal cannot itself be reversed (4.6).
- D5 OPEN: backup-file format: layout, date and number formats, how reversals and lot labels are written, a format version, where remembered source texts are kept, whether the last-backup date is in the file, and a sample file. The v2 CSV layout may be a starting point, but it covers only trades and cash.
- D6 OPEN: must ticker verification use the currently selected price source (K4)?
- Blocked-deletion message: RESOLVED. Wording in 4.7 and Example E approved.
- Historical prices: deferred past v1 (section 7).

## 14. Components and responsibilities (technology-neutral)
Added as section 14 so existing section numbers stay stable. Language, framework, database and price source are plan.md decisions. This section only says what each part must do and what it may depend on.

| # | Component | Responsibility | Spec refs | Slices |
|---|-----------|----------------|-----------|--------|
| K1 | **Rules engine** | Holds the ordering and replay checks (4.1), cash rules (4.2), lots and gains (4.4), splits (4.5), reversals (4.6) and the portfolio deletion check (4.7). Pure logic: no screens, no network. Answers "is this save allowed, and if not, which date and amount fail?" | 3, 4, 5 | 1, 2, 4 |
| K2 | **Storage** | Keeps all records in app-private storage on the phone. A save and its linked cash line are written together or not at all. A restore (delete and load) is also all or nothing. Loads data at start. | 3, 9 | 1 |
| K3 | **Valuation and reporting** | Net worth, unrealized and realized gains, allocation (combined and per portfolio). Reads records and prices; never writes them. | 4.8, 6 | 3, 6 |
| K4 | **Price service** | Fetches latest prices through a replaceable source, verifies tickers, marks prices stale, supplies sector/geography when the source has it. Only updates instrument data, never transactions. Owns the selected-source setting and runs the sweep when it changes. K9 asks K4 to switch the source. | 7, C1, C8, C14 | 3, 6 |
| K5 | **Entry flows** | Buy, sell (with lot picker), split, cash entry, reversal and portfolio management. Shows the review screen, enforces the ticker check of 4.3 using K4's verified flag, then asks K1 to validate and save. | 4.3 to 4.7, 6 | 2, 4 |
| K6 | **Backup and restore** | Exports one file. Restore refuses to start offline, validates the whole file by replaying it through K1 and collecting every error, verifies held tickers through K4, shows the summary, offers an export and requires typed confirmation when data exists, then deletes and loads in one action. | 8, 9, C3, C7, C8 | 5 |
| K7 | (Withdrawn: merged into K6; ID kept stable.) | | | |
| K8 | **App lock** | Biometric or PIN and the auto-lock policy. Guards every screen. | 9 | 5 |
| K9 | **Screens** | The nine views and the settings screen. Call K3 to read and K5 to change data; never write directly. Shows the date-field note on every date input and the time zone suffix on every shown date (section 6). | 6 | 2, 3, 6 |

**Dependency rules**
1. Every write to transactions or cash entries goes through K1: entry flows, restore and portfolio deletion. No component bypasses it.
2. K3 and K9 only read data. K4 only writes prices and instrument data.
3. K1 has no knowledge of screens, network or file formats, so Examples A to F can be tested on K1 alone.
4. Checks that need the network, such as ticker verification, are enforced by K5 (manual entry) and K6 (restore) using K4. They are not part of K1.
