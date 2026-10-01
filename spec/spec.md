# spec.md (DRAFT v1, awaiting approval)

Source: intent/intent.md v51 (final). Stage: Design.
Nothing in this file has been built, run or verified against real data. Items marked **[UNVERIFIED]** rest on my memory or assumptions, not on checks done in this session.

## 1. Summary
Single-user Android app (sideloaded). Local-only data. Tracks one shared cash account plus multiple US stock/ETF portfolios in USD. First version delivers net worth, allocation, lot-based cost and gains, transaction entry, CSV import, backup/restore and app lock. Performance over time comes after v1, but all transactions are stored from day one.

## 2. Concerns and conflicts (read first)

| # | Severity | Concern | Proposed handling (needs your approval) |
|---|----------|---------|------------------------------------------|
| C1 | High | **"Live or near-real-time" price freshness vs "free data only".** Free sources are usually delayed, rate-limited, or unofficial and can break. Constraints and Decisions already differ: Decisions says near-real-time is best effort. | Spec treats freshness as best effort. Every price shows its age and a stale marker. The price source sits behind an interface so it can be swapped. Source choice happens in plan.md after a real check **[UNVERIFIED: no free source has been tested]**. |
| C2 | High | **First version is large for "ASAP"**: 9 views plus many interacting rules. | Build in the slices in section 12. Decision D2: realized gains and sector/geography stay in v1 (you accepted the schedule risk). They are built last, so the rest can be used first. |
| C3 | High | **Permanent transactions plus one-time import.** A bad import cannot be edited away. Only restoring a backup undoes it. | The preview is the main guard. Proposal: before an import is committed, the app offers (not forces) to export a backup first. |
| C4 | Medium | **Cash lines made by buys and sells have no type among the five cash types.** | Add a system-only type `trade`. It cannot be entered by hand. It appears in the cash list and filters. |
| C5 | Medium | **How a reversal is represented is not defined.** A mistaken buy is offset by a sell-like entry, which could create a realized gain/loss and must pick lots. | A reversal is a normal entry carrying a link "reverses #id" and follows all normal blocking rules. Full rules for reversing a buy, sell, split or cash entry, plus the date and amount rules, are in 4.6. |
| C6 | Medium | **CSV import targets one portfolio, but cash rows belong to the shared cash account.** | Trade rows go to the chosen portfolio. Direct cash rows go to the shared cash account and are not removed if that portfolio is later deleted. Only the cash lines of its trades are removed. |
| C7 | Medium | **Row rejection can cascade.** If a buy is rejected, later sells of those shares are also rejected. | Rows are processed in date order, then file order. Each rejection lists its reason, and cascaded rejections say "depends on rejected row N". |
| C8 | Medium | **Ticker check needs internet.** A CSV with unseen tickers cannot be validated offline. | Import with unverified tickers is blocked until online, same as a manual buy. |
| C9 | Medium | **Split handling is manual.** A forgotten split silently distorts holdings, cost per share and gains. | No automation (per intent). The holding detail shows the split history so omissions are easy to spot. Optional later: warn when the latest price differs sharply from the cost per share. |
| C10 | Low | **Realized gains can differ from broker/tax figures** (wash sales not modelled). | Label realized gains "informational" in the UI. |
| C11 | Low | **"Performance" moves with deposits/withdrawals**, so it is not investment return. | Label it "Total value change" in the later version. |
| C12 | Low | **"Allocation by holding" is ambiguous across portfolios.** | Per-portfolio view: one slice per holding in that portfolio. Combined view: same ticker in several portfolios is merged into one slice. |
| C13 | Low | **Dates and time zones.** You are in Tokyo, markets are in New York. | Transactions carry a calendar date you pick. No time zone conversion. Entry order is the tie-breaker within a day. |
| C14 | Low | **Sector/geography for ETFs** is often missing from free sources **[UNVERIFIED]**. | Manual override per holding. Unclassified holdings show as "Unclassified". |

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
  - Optional `reverses_id`.
- **Lot**: created by a buy. `portfolio`, `ticker`, `buy_date`, `original_qty`, `remaining_qty`, `cost_per_share`. Splits adjust quantity and cost per share, never total cost.
- **Sell allocation**: sell id, lot id, quantity taken from that lot.
- Quantities are stored as originally traded, with splits applied by the splits recorded. No stored value snapshots.

## 4. Business rules

### 4.1 Ordering and checking
- Global order is `date`, then `entered_seq` (order of entry).
- On any save (including reversals and CSV rows), the app replays cash and share balances from that date through all later dates. Any date where cash < 0 or shares < 0 blocks the save.
- Future-dated entries count in net worth only from their date.

### 4.2 Cash
- Buy: cash out on the trade date, by the total amount. Sell: cash in on the trade date.
- Withdrawal larger than the balance on that date or any later date is blocked.
- Exactly one opening balance. It must be the earliest cash entry. It is never reversed or replaced.
- A cash difference vs reality is fixed with an adjustment deposit/withdrawal with a note.

### 4.3 Buys
- Input: ticker, date, quantity, total amount. Quick review screen before saving.
- Blocked if total exceeds cash (checked per 4.1) or if the ticker has never been verified and the phone is offline.
- No check against market price.
- A buy dated before a recorded split: quantity entered as originally traded, later splits applied automatically.

### 4.4 Sells
- Input: ticker, date, quantity, total amount (zero allowed), and lot picks. Quick review screen before saving.
- Lot rules: same portfolio only; lot bought on or before the sell date; same-date lot must have been entered earlier. Each lot pick has its own quantity, prefilled with the lot's remaining quantity.
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

**Example B: split.** Jan 2 buy 10 shares for $1,000. Mar 1 split 2:1 recorded. Holding becomes 20 shares at $50/share, cost still $1,000. A later entry of a buy dated Feb 15 for 5 shares, $500, is typed as 5 shares, and is stored as 5 shares which the Mar 1 split turns into 10 shares at $50/share.

**Example C: backdated block.** Opening balance $1,000 on Jan 1. Buy for $800 on Jan 10. You try a withdrawal of $300 dated Jan 5. On Jan 5 the balance would be $700 (fine), but on Jan 10 it would be 700 − 800 = **−$100**, so the withdrawal is **blocked**.

**Example D: buried mistake.** Buy 10 shares on Jan 2, sell all 10 on Jan 3. Reversing the Jan 2 buy is blocked (shares would be −10 on Jan 3). You must first reverse the Jan 3 sell, then the buy.

**Example E: blocked portfolio deletion.** Opening $1,000 Jan 1. Portfolio Q sells shares for $500 on Feb 1. Direct withdrawal of $1,200 on Feb 10 (allowed, balance $300 after). Deleting Q removes the $500, so on Feb 10 the balance would be −$200. Deletion is blocked: "Cash would be negative from Feb 10 by $200. Add a deposit adjustment of at least $200 dated on or before Feb 10."

## 6. Views (first version)
Layout and navigation are decided by the screens listed below. Exact navigation is a plan.md item.
1. **Home**: net worth + allocation summary; net worth + each portfolio with gain/loss.
2. **Net worth overview**
3. **Portfolios and holdings**
4. **Allocation** (asset type, portfolio, holding, sector/geography)
5. **Realized gains** (per sale, per holding, total)
6. **Holding detail**: lots, cost, gain/loss, split history, its transactions
7. **Cash account**: balance, entries incl. trade lines, date-range filter. Each `trade` line shows buy or sell, ticker and portfolio name besides date and amount. Tapping it opens the linked transaction (with its lots and any reversal link).
8. **Transaction history** (per portfolio only): filters ticker, date range, type (buy, sell, split, cash). Each buy or sell shows its cash effect and can open its cash line. A reversal links to the original, and its cash line points to the reversal, so the full chain is traceable.
9. **Closed/hidden holdings**

Plus entry flows: buy, sell (lot picker), split, cash entry, CSV import, backup/restore, settings (auto-lock).

Targets: adding a buy or sell takes under 30 seconds, with lots shown ready to tap.

## 7. Prices and market data
- Automatic refresh when online, plus pull-to-refresh. Manual entry is for transactions only.
- Each price shows its timestamp. Failure keeps the last price, marked stale.
- Sending tickers to an outside source is accepted. Free tier only.
- The price source is behind an interface so it can be replaced. **[UNVERIFIED]** Source, limits and whether it provides sector/geography are to be checked in the plan stage.
- Historical prices are needed only for the later performance feature. **[UNVERIFIED]** Availability, limits and split adjustment are unchecked. Because splits are user-entered, any split-adjusted history would have to be converted back to as-traded values before use. Verify before the performance milestone, not before v1.

## 8. CSV import (PARTIAL, blocked on sample file)
Fixed by intent:
- One portfolio chosen per import; one file holds trades and cash rows.
- Preview and confirmation before saving; valid rows load, rejected rows are listed with reasons (C7).
- Sells identify lots by buy date or lot id; if the date alone is ambiguous, match by date + cost per share; otherwise reject.
- Possible duplicates are flagged and you decide.
- Import follows all blocking rules in 4.1 and the opening-balance rule.

**Not specified until I see your sample file:** columns, date/number formats, row-type recognition, lot id format, and whether the file contains an opening-balance row.

## 9. Backup, restore, security
- Data stored on the phone only, in app-private storage.
- Lock: biometric or PIN. Auto-lock choices: every time you leave the app (default), after 1 min idle, after 5 min idle, only when the phone locks or the app restarts.
- Backup: export one file to a location you choose. Unencrypted allowed. No other export.
- Restore: asks every time to replace or merge.
  - **Replace:** the backup file replaces all current data.
  - **Merge:** adds only records not already present (matched by ID). It never alters or removes existing records. The whole merge is rejected if the merged result would break a cash or share rule (4.1); the message names the failing date and amount.
  - Known limit: a backup from a different install may give the same real-world trade a different ID, so merge would add it as a duplicate. Merge has no duplicate detection.
- Works offline with last known prices.

## 10. Out of scope
As in intent.md: liabilities, other asset classes, notifications, tax reporting, per-holding dividends, separate fees, multi-currency, account syncing, Play Store release.

## 11. Acceptance criteria (first version)
1. Net worth and allocation show correctly from your real data (after import).
2. Examples A to E behave as written, as automated tests.
3. A buy or sell can be entered in under 30 seconds.
4. Backup then restore on a clean install reproduces identical net worth and holdings.
5. Offline use shows last prices marked with their age.
6. App locks per the chosen auto-lock setting.

## 12. Proposed slices (input to plan.md)
1. Data model, cash account, rules engine with tests (Examples A to E).
2. Buy/sell/split entry, lot picker, holdings, cost and gains.
3. Prices, net worth, home and allocation.
4. Portfolio management and deletion rules.
5. Lock, backup and restore.
6. CSV import (after sample file).
7. Realized gains view, sector/geography (kept in v1 per D2; built last).

## 13. Open design points
- D1 CSV format and opening balance row (needs sample file).
- D2 RESOLVED: realized gains and sector/geography stay in v1.
- D3 RESOLVED: meaning of "merge" on restore (section 9).
- D4 RESOLVED: a reversal cannot itself be reversed (4.6).
- Blocked-deletion message: RESOLVED. Wording in 4.7 and Example E approved.
- Historical prices: deferred past v1 (section 7).

## 14. Components and responsibilities (technology-neutral)
Added as section 14 so existing section numbers stay stable. Language, framework, database and price source are plan.md decisions. This section only says what each part must do and what it may depend on.

| # | Component | Responsibility | Spec refs | Slices |
|---|-----------|----------------|-----------|--------|
| K1 | **Rules engine** | Holds the ordering and replay checks (4.1), cash rules (4.2), lots and gains (4.4), splits (4.5), reversals (4.6) and the portfolio deletion check (4.7). Pure logic: no screens, no network. Answers "is this save allowed, and if not, which date and amount fail?" | 3, 4, 5 | 1, 2, 4 |
| K2 | **Storage** | Keeps all records in app-private storage on the phone. A save and its linked cash line are written together or not at all. Loads data at start. | 3, 9 | 1 |
| K3 | **Valuation and reporting** | Net worth, unrealized and realized gains, allocation (combined and per portfolio). Reads records and prices; never writes them. | 4.8, 6 | 3, 7 |
| K4 | **Price service** | Fetches latest prices through a replaceable source, verifies tickers, marks prices stale, supplies sector/geography when the source has it. Only updates instrument data, never transactions. | 7, C1, C8, C14 | 3, 7 |
| K5 | **Entry flows** | Buy, sell (with lot picker), split, cash entry, reversal and portfolio management. Shows the review screen, then asks K1 to validate and save. | 4.3 to 4.7, 6 | 2, 4 |
| K6 | **CSV importer** | Reads the file, maps rows, builds the preview, flags duplicates, lists rejected rows. Commits only through K1. | 8, C6, C7, C8 | 6 |
| K7 | **Backup and restore** | Exports one file; restores by replace or merge. Merge validation goes through K1. | 9 | 5 |
| K8 | **App lock** | Biometric or PIN and the auto-lock policy. Guards every screen. | 9 | 5 |
| K9 | **Screens** | The nine views and the settings screen. Call K3 to read and K5 to change data; never write directly. | 6 | 2, 3, 7 |

**Dependency rules**
1. Every write to transactions or cash entries goes through K1: entry flows, CSV import, restore merge and portfolio deletion. No component bypasses it.
2. K3 and K9 only read data. K4 only writes prices and instrument data.
3. K1 has no knowledge of screens, network or file formats, so Examples A to E can be tested on K1 alone.
