# spec.md (DRAFT v3, awaiting approval)

Source: intent/intent.md v52 (final). Stage: Design.
Nothing in this file has been built, run or verified against real data. Items marked **[UNVERIFIED]** rest on my memory or assumptions, not on checks done in this session.

## 1. Summary
Single-user Android app (sideloaded). Local-only data. Tracks one shared cash account plus multiple US stock/ETF portfolios in USD. First version delivers net worth, allocation, lot-based cost and gains, transaction entry, backup/restore (the only way data enters or leaves the app) and app lock. Performance over time comes after v1, but all transactions are stored from day one.

## 2. Concerns and conflicts (read first)

| # | Severity | Concern | Proposed handling (needs your approval) | Status |
|---|----------|---------|------------------------------------------|--------|
| C1 | High | **"Live or near-real-time" price freshness vs "free data only".** Free sources are usually delayed, rate-limited, or unofficial and can break. Constraints and Decisions already differ: Decisions says near-real-time is best effort. | Spec treats freshness as best effort. Every price shows its age and a stale marker. The price source sits behind an interface so it can be swapped, and you can switch it in settings (section 7). Source choice happens in plan.md after a real check **[UNVERIFIED: no free source has been tested]**. | Approved |
| C2 | High | **First version is large for "ASAP"**: 9 views plus many interacting rules. | Build in the slices in section 12. Decision D2: realized gains and sector/geography stay in v1 (you accepted the schedule risk). They are built last, so the rest can be used first. | Approved |
| C3 | High | **Permanent transactions plus a one-time load of a hand-prepared file.** A bad load cannot be edited away. Only restoring a backup undoes it. | The whole file is validated before anything is deleted, and every error is listed with its line and reason. If validation fails, nothing changes. If the app holds data, it shows a count summary, offers a backup export first and requires a typed confirmation (section 8). | Approved |
| C4 | Medium | **Cash lines made by buys and sells have no type among the five cash types.** | Add a system-only type `trade`. It cannot be entered by hand. It appears in the cash list and filters. | Approved |
| C5 | Medium | **How a reversal is represented is not defined.** A mistaken buy is offset by a sell-like entry, which could create a realized gain/loss and must pick lots. | A reversal is a normal entry carrying a link "reverses #id" and follows all normal blocking rules. Full rules for reversing a buy, sell, split or cash entry, plus the date and amount rules, are in 4.6. | Approved |
| C6 | Medium | **Withdrawn (v52).** There is no CSV import, so no per-import portfolio target and no cross-file double counting. Cash entries are records in the restore file; trade cash lines are rebuilt by replay. ID kept so references stay valid. | n/a | Withdrawn |
| C7 | Medium | **Error reports can cascade.** In a hand-made file, one bad buy also makes later sells of those shares fail. | The restore is all-or-nothing, so nothing is saved either way. Records are replayed in date order, then file order. The report lists every error, and dependent ones say "depends on line N" so you fix root causes first. | Approved |
| C8 | Medium | **Ticker check needs internet.** A hand-prepared file cannot be fully validated offline. | The restore needs internet. Every ticker with shares left at the end of the file is verified before anything is deleted; one that fails blocks the restore. Sold-out tickers stay unverified, and a later manual buy of one is blocked until verified online (4.3). Verification uses the price source selected at that moment (D6). A ticker that source cannot verify blocks the restore; you can switch the source in Settings and try again. | Approved |
| C9 | Medium | **Split handling is manual.** A forgotten split silently distorts holdings, cost per share and gains. | No automation (per intent). The holding detail shows the split history so omissions are easy to spot. No price-based warning is needed. | Approved |
| C10 | Low | **Realized gains can differ from broker/tax figures** (wash sales not modelled). | Label realized gains "informational" in the UI. Decision: No label is added. | Closed, not a concern |
| C11 | Low | **"Performance" moves with deposits/withdrawals**, so it is not investment return. | Label it "Total value change" in the later version. | Approved |
| C12 | Low | **"Allocation by holding" is ambiguous across portfolios.** | Per-portfolio view: one slice per holding in that portfolio. Combined view: same ticker in several portfolios is merged into one slice. | Approved |
| C13 | Low | **Dates and time zones.** You are in Tokyo, markets are in New York. | Entries carry a calendar date only, no time. All dates (buy, sell, split and cash entries) use the US market time zone (New York), not the device's. The app does no time zone conversion: the date is stored as picked. The date field defaults to today's date in New York. Entry order is the tie-breaker within a day. | Approved |
| C14 | Low | **Sector/geography for ETFs** is often missing from free sources **[UNVERIFIED]**. | Manual override per holding. Unclassified holdings show as "Unclassified". | Approved |
| C15 | Low | **The last-backup date has a time zone.** It is when you exported, not a market date, so the "ET" rule for market dates (C13) does not fit it, and the 7-day check must not depend on a time zone. | The app stores the exact moment of export. The 7-day check uses elapsed time. The shown date is that moment in the device's time zone, followed by the zone's abbreviation (for example "JST" in Tokyo). It is the only shown date not in New York time. The zone is the device's current one when the date is shown, and no zone is stored, so after travelling the same backup can show a different date and abbreviation. **[UNVERIFIED]** Some zones have no short name on Android and show an offset instead; the app shows what the platform gives. Check on the phone. | Approved |
| C16 | Low | **The restore error report has a Copy report button, but intent says the backup file is the only way data leaves the app.** | Design exception (approved): one "Copy report" button on the restore error report only. It copies the text shown (the count line, then each error with its line, column and reason), never file rows or app data. It is not a backup and does not change the last-backup state. No other screen gets copy, share or export. **[UNVERIFIED]** Android clipboard behaviour (other apps in the foreground can read it; newer versions show a preview that the app can hide); the minimum Android version (plan.md) decides this. | Approved |

## 3. Domain model

- **Cash account** (exactly one): ordered list of cash entries. Balance on a date = sum of entries dated on or before that date.
- **Cash entry**: `id`, `date`, `amount` (signed), `type` in {opening balance, deposit, withdrawal, interest/dividend, adjustment, trade}, `note`, `entered_seq`, optional `reverses_id` (set on a cash-entry reversal), optional `transaction_id` (set only on `trade` entries; points to the buy or sell, including reversals, that created it).
  - Note is required for adjustments, optional for all others.
  - `trade` entries are created only by buys/sells: exactly one per buy or sell (reversals included), none for splits.
- **Portfolio**: `id`, `name`. Names are unique, compared ignoring case and spaces as in 4.9. Portfolios are listed in the order they were created.
- **Holding**: a ticker within a portfolio. Its shares, lots and cost are derived from transactions. Two fields are stored: the `hidden` flag and the optional manual sector/geography override (see Holding classification below).
- **Instrument**: ticker, name, asset type (stock or ETF), source sector, source geography (as the source supplies them, may be missing), verified flag, latest price, price timestamp, stale flag.
- **Holding classification**: optional manual `sector` and `geography` override on a holding (a ticker within a portfolio), each a value from the matching list. Effective value = override, else the source value (via the list, 4.9), else "Unclassified".
- **Lists**: a sector list and a geography list. Each value has a name and may carry remembered source texts (the source's original text from before a rename, 4.9).
- **Settings**: auto-lock, price source, tab order, and the two lists with their remembered source texts. Not settings: the screenshot window (never saved, section 9) and the last-backup moment (app state, set only by a completed export).
- **"App holds data"** means at least one portfolio or cash entry exists. Settings and lists alone do not count. Used by the backup reminder (section 9), the typed confirmation on restore (section 8) and the guided empty state (section 6). Intent says "if the app holds data" without defining it, so this is the spec's definition. Consequence: restoring onto an app that holds only changed settings (no portfolio, no cash entry) needs no typed confirmation.
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
- Amounts have at most 2 decimals, in entry screens and in files. Quantities have up to 8 decimals (4.3).
- A cash entry is never 0. In a file the amount is signed: positive for a deposit, interest/dividend and opening balance, negative for a withdrawal, either sign for an adjustment.

### 4.3 Buys
- Input: ticker, date, quantity, total amount. Quick review screen before saving.
- The total amount (fees included) must be more than 0. Zero is allowed on sells only (4.4).
- Blocked if total exceeds cash (checked per 4.1).
- Blocked if the ticker has never been verified and the phone is offline. When online, the ticker is checked before saving, and a ticker that does not exist is blocked. The entry flow (K5) enforces this using the verified flag that the price service (K4) maintains. The check uses the price source selected at that moment (D6). The rules engine (K1) does not know about the network.
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
- A reversal cannot itself be reversed. If you reversed something by mistake, re-enter the original as a new entry. An entry can be reversed at most once; Reverse is not offered on an entry that already has a reversal.
- **Amount:** fixed to the original; not typed.
- **Date:** defaults to the original date. You may pick a later date (for example today); the original's effect then stays in place between the two dates. A reversal cannot be dated earlier than the original.
- **Splits between the original and the reversal:** a reversal's quantity is the original's quantity as originally traded, and any split dated after the original and on or before the reversal is applied to it, the same as-traded rule as 4.3. The reversal therefore removes or restores the same economic shares, and total cost and cash amounts are unchanged. Reversing a buy removes the original lot's whole remaining piece from that buy, now split-adjusted. Reversing a sell restores the exact lots it consumed, with quantities and cost per share adjusted by the splits since the sell date. Reversing a cash entry or a split is not affected.
- **Reversing a buy:** takes the shares back out of the original lot (no lot picking). Realized gain/loss is 0. Its cash line returns the original total. Blocked if any of those shares were sold; reverse the later sells first (Example D).
- **Reversing a sell:** puts back the exact lots and quantities the sell consumed, at their original cost per share. Removes the proceeds from cash and cancels the sell's realized gain/loss. Blocked if cash would go negative on any date (4.1).
- **Reversing a split:** applies the inverse ratio for that ticker and portfolio, dated per the date rule above.
- **Reversing a cash entry:** adds an opposite-amount entry linked to the original, with type `adjustment` and the note pre-filled "Reversal of #id". The opening balance is never reversed (4.2). `trade` lines cannot be reversed directly; reverse the buy or sell instead.
- **Reverse and re-enter** (shortcut):
  - Starts from a transaction or cash entry, opens a new entry prefilled with the original values, and saves the reversal and the new entry together as one confirmed action. If either part is blocked, neither is saved and the message says why.
  - The original and the reversal stay as separate permanent records, so "no edit or delete" does not change.
  - The pair check: the replay in 4.1 runs once with both entries applied, so the pair is checked as a unit. A block that comes from the original itself still applies (a buy whose shares were sold cannot be reversed, Example D).
  - The reversal follows the date, amount and split rules above. The new entry starts with the original's values, including its date, gets the next entry order after the reversal, and is a normal entry under 4.3, 4.4 or 4.5, including the ticker check (K5).
  - For a sell, the lot picks start from the original picks. The new sell must still pass the lot rules (4.4).
  - Not offered on the opening balance, on a reversal entry, or on a `trade` cash line (start from the buy or sell).
  - It saves typing, not rules: a mistake buried under later dependent transactions is still blocked until those are reversed.

### 4.7 Portfolios
- Create, rename, delete (including the last one, and all of them).
- A name that matches another portfolio's name (ignoring case and spaces, D7) is refused on create and rename.
- Delete requires typing a confirmation after a summary of what will be removed: transaction count, trade cash lines, realized gains.
- Deleting removes the portfolio's transactions and their cash lines, then recomputes history as if it never existed.
- If removing those cash lines would make cash negative on any date, deletion is blocked. The message states the first date it goes negative, the largest shortfall, and the fix: a backdated deposit adjustment (with a note) dated on or before that first date for at least the shortfall.
- This is the only exception to permanent transactions. The way back is a backup restore.

### 4.8 Calculations
- Net worth = cash balance + sum over open holdings of (shares × latest price).
- Cost of holding = sum of remaining lot cost. Unrealized gain/loss = market value − cost.
- Allocation: by asset type (cash, stocks, ETFs), portfolio, holding (C12), sector/geography. Combined and per portfolio.
- A price without a fresh quote uses the last known price and is marked stale. If no price has ever been fetched, that holding is valued at cost and flagged "no price".

### 4.9 Sector and geography
- Every holding has an effective sector and an effective geography: the manual override if set, else the source's value, else "Unclassified" (C14).
- Override: edited on Holding detail in two fields. The value is picked from the matching list, not typed. A manual value wins over the source's until you clear it. Clearing returns to the source's value, or "Unclassified" if there is none.
- Two separate lists (sectors, geographies) are kept in settings. You can add, rename and delete values. A list cannot hold two values with the same name (compared as in the matching rule below).
- Source values: the app maps a source text to a list value through the remembered source texts, then by name. If neither matches, the text is added to the list as a new value.
- A remembered source text belongs to one value in a list. Two remembered texts that match (D7) in the same list are not allowed.
- Rename: updates every holding that uses the value, and remembers the source's original text, so a refresh maps to the renamed value instead of adding the old text again.
- Delete: blocked while any holding uses the value, through an override or through the source. Hidden holdings count as using it. Deleting a value also removes its remembered source texts.
- No ETF look-through in v1: one sector and one geography per holding. A broad ETF can take a value such as "Diversified" or "Global" from your lists.
- Matching: a source text matches a list value or a remembered text when they are equal after ignoring case, leading and trailing spaces, and repeated inner spaces. The same comparison applies to the duplicate-name rule. A value keeps the casing it was first given. Near-matches such as "Tech" and "Technology" stay separate; fix them by renaming (a rename remembers the source text). Remembered texts are checked before names, so after "Tech" is renamed "Technology", the source text "Tech" still maps to "Technology" even if a new value named "Tech" is created later.

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
1. **Overall**: net worth + allocation summary; net worth + each portfolio with gain/loss. Also the backup reminder banner when due (section 9) and, when the app holds no data, the guided empty state (below).
2. **Net Worth (Total)**
3. **Portfolios and holdings**
4. **Allocation** (asset type, portfolio, holding, sector/geography)
5. **Realized gains** (per sale, per holding, total), filterable by date range (the sale date). Informational only. A sale cancelled by a reversal (4.6) is left out of the figures whatever the range; transaction history still shows both entries.
6. **Holding detail**: lots, cost, gain/loss, split history, its transactions, and sector and geography with the manual override (4.9)
7. **Cash account**: balance, entries incl. trade lines, date-range filter. Each `trade` line shows buy or sell, ticker and portfolio name besides date and amount. Tapping it opens the linked transaction (with its lots and any reversal link).
8. **Transaction history** (per portfolio only): filters ticker, date range, type (buy, sell, split, cash). Cash means the cash lines of that portfolio's trades; direct cash entries are viewed in the cash account. Each buy or sell shows its cash effect and can open its cash line. A reversal links to the original, and its cash line points to the reversal, so the full chain is traceable.
9. **Closed/hidden holdings**

Plus entry flows: buy, sell (lot picker), split, cash entry, backup/restore (restore needs internet), settings (auto-lock, price source, tab order, sector and geography lists, screenshot window).

**First launch and empty state.**
- After the lock is confirmed (biometric or phone PIN, section 9), if the app holds no data (section 3), Overall shows three steps instead of zeros: set the opening balance, create a portfolio, enter a buy. Each step opens its entry flow.
- Restore is offered only in More, not here.
- The guide ends as soon as the app holds data, and Overall then shows the normal screens. With cash but no portfolio (also after deleting every portfolio, 4.7), net worth equals the cash balance, the allocation shows 100% cash, and the portfolio section says "No portfolios yet" with a Create a portfolio button.

**Navigation outline.**
- After unlock, the app opens on the Overall tab (view 1). A bottom bar with four tabs is always visible. The default order, left to right, is: More, Cash, Portfolios, Overall. You can reorder the tabs in settings.
- **Overall** (1): tap the net worth figure for Net Worth (Total) (2); tap the allocation summary for Allocation (4); tap a portfolio for its holdings (3). The backup reminder banner is shown here; its button starts a backup export.
- **Portfolios** (3): the list of portfolios, in the order they were created. Tapping one shows its holdings. A holding opens Holding detail (6). From a portfolio you reach its Transaction history (8) and its Closed/hidden holdings (9). Portfolio create, rename and delete (4.7) are here.
- **Cash** (7): the cash account, with its entries and date-range filter. A `trade` line opens the linked transaction (section 6, item 7). Cash entry (deposit, withdrawal, interest/dividend, adjustment) starts here.
- **More:** a line "Last backup: <date> <zone>" (the zone abbreviation, C15) or "never backed up", Allocation (4), Realized gains (5), Backup/restore, Settings (auto-lock, price source, tab order, sector and geography lists, screenshot window). Net Worth (Total) (2) is also reachable here.
- **Buy/Sell button:** Overall, Portfolios and Cash show a Buy/Sell button that opens a short menu: Buy, Sell. Holding detail also offers Buy and Sell with the ticker already filled in, and starts a split. A reversal, and Reverse and re-enter, start from the transaction or cash entry they apply to (4.6). With no portfolio, Buy says "Create a portfolio first" with a button, and after the portfolio is created the app returns to the Buy entry. Sell says "Nothing to sell yet", with no button.
- Android's back button returns to the previous screen. Auto-lock (section 9) can cover any screen.

Date fields: every screen that asks for a date shows the note "Enter date in US market time (New York)" next to the input. This covers buy, sell, split and cash entry, the reversal date, and the date-range filters in the cash account, transaction history and realized gains. The restore screen shows the same note next to its date format help (section 8). Dates that are only shown (lists, detail screens, review screens, restore summary) carry no note. They show the time zone suffix "ET" after the date, for example "2026-03-01 ET". The one exception is the last-backup date, which shows the device's time zone abbreviation, for example "2026-10-04 JST" (C15).

Targets: adding a buy or sell takes under 30 seconds, with lots shown ready to tap.

## 7. Prices and market data
- Automatic refresh when online, plus pull-to-refresh. Manual entry is for transactions only.
- Each price shows its timestamp. Failure keeps the last price, marked stale.
- Sending tickers to an outside source is accepted. Free tier only.
- The price source is behind an interface so it can be replaced. **[UNVERIFIED]** Source, limits, whether it provides sector/geography and whether it tells a stock from an ETF are to be checked in the plan stage.
- **Price source setting.** In settings you choose the price source from the sources the app ships with (the list is decided in plan.md). The app starts on a default source.
- **Sweep on change.** After you confirm a change of source while online, the app runs a price sweep: it fetches the latest price for every instrument the app holds (hidden holdings included) from the new source, and updates all prices. Net worth, gains and allocation then show the new market values.
- **Changing the source while offline is allowed.** The app shows a warning that prices were not refreshed, selects the new source, and skips the sweep. Prices stay as they are until the next refresh (automatic when online, or pull-to-refresh), which uses the new source.
- **Failures during the sweep.** A ticker the new source cannot price keeps its last price, marked stale (the rule above). When the sweep ends, the app lists the tickers it could not price. The new source stays selected.
- **What a source change never touches:** transactions, cash entries, lots, the verified flag, and manually entered sector/geography. Source-supplied sector/geography is refreshed from the new source where it provides them; a text not on a list is added to it (4.9).
- **Ticker verification** uses the source selected at that moment (D6). A ticker that is already verified is not checked again after a source change.
- Historical prices are needed only for the later performance feature. **[UNVERIFIED]** Availability, limits and split adjustment are unchecked. Because splits are user-entered, any split-adjusted history would have to be converted back to as-traded values before use. Verify before the performance milestone, not before v1.

## 8. Backup file and restore (App initialization)
Fixed by intent v52:
- The backup file is the only way data enters or leaves the app. No CSV import, no CSV export, no merge.
- App initialization = delete all settings and data, then load the file, as one confirmed action. The file has no other use.
- The app's own backups and your hand-prepared file use one format. Preparing the file from broker statements happens outside the app.
- The restore is all-or-nothing.

**File contents**
- In the file: portfolios; all transactions (buys, sells, splits, and reversals with their links); all cash entries (reversals with their links); hidden holdings; manual sector/geography overrides; the sector and geography lists with their remembered source texts (so a restored install does not re-add renamed texts); all settings (auto-lock, price source, tab order).
- Not in the file: lots, trade cash lines, holdings, prices, the phone's PIN or biometric, the screenshot window. The app rebuilds lots, trade cash lines and holdings by replay.
- Lot labels are the ids of buys (below). They mean nothing outside the file. After a successful restore the app generates its own unique ids.
- Not in the file either: the last-backup date. After a restore More shows "never backed up" until the next export, so the Overall banner shows after every restore.

**File format**
- One CSV text file, UTF-8, comma-separated, with one header row and one record per line. A line break inside a cell is not allowed. The app writes no byte-order mark and LF line endings. It reads files with or without a byte-order mark, and with LF or CRLF. A file that is not valid UTF-8 is rejected.
- Columns are matched by name, so their order is free. The header must contain all 19 columns: `row_type`, `id`, `portfolio`, `date`, `ticker`, `quantity`, `amount`, `lot_picks`, `split_to`, `split_from`, `cash_type`, `note`, `reverses`, `sector`, `geography`, `list`, `key`, `value`, `source_text`. An unknown or repeated named column rejects the file. A column with an empty header whose cells are all empty is ignored. A row whose cells are all empty is ignored; it still counts in line numbers. Leading and trailing spaces are trimmed in every cell except `note`.
- A cell in a column that the row type does not use must be empty. This catches a comma that shifted cells during editing.
- **Format version:** the first row after the header has `row_type` `format` and the version, a whole number, in `value`. The app writes 1. A missing, non-numeric or unknown version rejects the file with a message such as "this file is version 2; this app reads up to version 1". A newer app reads older versions. A later version adds a field as an optional column or a new row type (for example per-holding dividends, or ETF look-through weights).
- **Ids:** an `id` is 1 to 32 letters, digits, hyphens or underscores, starts with a letter (so Excel cannot read it as a number or time), and is compared ignoring case. One id space covers cash entries, buys, sells and splits. A duplicate is rejected. The app's export writes an id on every cash entry, buy, sell and split. A hand-made file needs one only where something points to it.
- **Order:** one shared order for cash entries and transactions; file order breaks same-date ties (4.1). A reversal must come after the entry it reverses. Other row types may be anywhere. The export writes portfolios in creation order, entries in entry order, and list values alphabetically.
- **Dates and numbers:** a date is `YYYY-MM-DD` (a US market date, C13). A number uses a dot as the decimal point and only digits and an optional leading minus: no thousands separators, currency or percent signs, parentheses or scientific notation. Quantities are above 0 with up to 8 decimals. Amounts have up to 2 decimals (4.2).
- **Tickers:** read trimmed and in capitals; letters, digits, dot and hyphen only, 1 to 12 characters. Name and asset type are not in the file.
- **Portfolios** are referred to by name (unique, 4.7).

**Records**

| row_type | Columns it uses (the others stay empty) | Rules |
|---|---|---|
| `format` | `value` | See Format version. |
| `portfolio` | `portfolio` | One row per portfolio. The file needs at least one `portfolio` or `cash` row. |
| `cash` | `id` (optional), `date`, `cash_type`, `amount`, `note` | `cash_type`: `opening_balance`, `deposit`, `withdrawal`, `interest_dividend`, `adjustment` (case ignored); never `trade`. The note is required on an adjustment. At most one opening balance, and it is the earliest cash entry (4.2). Signs per 4.2. |
| `buy` | `id` (optional), `portfolio`, `date`, `ticker`, `quantity`, `amount` | `amount` is the total, fees included, above 0. The `id` is the lot label; it is needed only if a sell picks the buy or a reversal points to it. |
| `sell` | `id` (optional), `portfolio`, `date`, `ticker`, `quantity`, `amount`, `lot_picks` | `amount` may be 0. `lot_picks` is pieces of `id:quantity` separated by semicolons, for example `T1:10;T2:2.5`, with quantities as the lot picker shows them on the sell's date (4.4). They must add up to the sell quantity and pass the lot rules (4.4). |
| `split` | `id` (optional), `portfolio`, `date`, `ticker`, `split_to`, `split_from` | Whole numbers above 0, not equal. A 2:1 split is 2 and 1; a 1:10 reverse split is 1 and 10. |
| `reversal` | `reverses`, `date`, `note` (cash reversals only, optional) | `reverses` is the id of the original. Nothing else is stored: amount, quantity, portfolio, ticker and split adjustment come from the original (4.6). A reversal needs no id. If a cash reversal has no note, the note of 4.6 is used. The original cannot be the opening balance or another reversal, cannot already be reversed, and cannot be dated after the reversal. |
| `hidden` | `portfolio`, `ticker` | One row per holding. After the replay the holding must exist (a buy or sell for it is in the file) and have 0 shares. |
| `override` | `portfolio`, `ticker`, `sector` and/or `geography` | One row per holding. The holding must exist. At least one of the two values is given, and each must be on its list (matched as in 4.9; the app stores the list's own spelling). |
| `list_value` | `list`, `value` | `list` is `sector` or `geography`. A list may be empty. No two values match (4.9). |
| `source_text` | `list`, `value`, `source_text` | Maps a remembered text to a value on that list. A text maps to one value only. |
| `setting` | `key`, `value` | One row per key, below. |

**Settings keys** (a missing row is never an error)

| `key` | Values (case ignored) | If missing |
|---|---|---|
| `auto_lock` | `every_exit`, `1_min`, `5_min`, `phone_lock_only` | `every_exit` |
| `price_source` | one of the sources the app ships (decided in plan.md) | the app's default source |
| `tab_order` | the four tabs separated by semicolons, each once, for example `more;cash;portfolios;overall` | `more;cash;portfolios;overall` |

**Restore steps**
1. Online only. Offline, the restore cannot be started (C8).
2. The whole file is read and validated. Records are replayed in date order, then file order (4.1), each checked against 4.1 to 4.6 as if entered by hand. Every error is collected with its line and reason (C7). Nothing is deleted yet. A file-level failure (not UTF-8, header, version) stops here.
3. Only if steps 1 and 2 found no errors, every ticker with shares left at the end of the file is verified through K4, using the price source selected before the restore, not the one named in the file (D6). One that cannot be verified blocks the restore. Sold-out tickers are not verified (C8). A first round of fixes can therefore be followed by a second round naming unverifiable tickers.
4. If any check fails, nothing changes and the report is shown, with a Copy report button (C16).
5. A summary shows counts of portfolios, transactions and cash entries, now versus in the file. Also shown: the resulting cash balance and number of holdings, to compare with real life (an addition to intent v52, approved in Design).
6. If the app holds data (section 3), it offers a backup export first, then requires a typed confirmation.
7. Delete and load happen as one action, so the app is never left empty in between. The app then generates its own IDs. The price source in the file becomes the selected source, and the first refresh uses it. Holdings show "no price" until the first successful price refresh (4.8). The last-backup state is reset: More shows "never backed up" until the next export.

**Rejection reasons and the report**
Every error is listed, with its line (the header is line 1, as Notepad shows it), its column when one applies, and the reason in plain words.
- **File:** not valid UTF-8; no header; a missing, unknown or repeated column; a row with more or fewer cells than the header; a line break inside a cell; no `format` row first, or a version that is not a whole number or not known; no `portfolio` row and no `cash` row.
- **Every row:** a missing or unknown `row_type`; a required value missing; a non-empty cell in a column the row type does not use.
- **Dates and numbers:** a date not in `YYYY-MM-DD` or not a real date; a number in a form not allowed; a quantity of 0 or less or with more than 8 decimals; an amount with more than 2 decimals; a wrong sign; a zero cash amount; a buy with a zero total.
- **Ids and portfolios:** a badly formed or duplicate id; an empty or duplicate portfolio name; a portfolio not found.
- **Cash, tickers and splits:** an unknown cash type, or `trade`; an adjustment without a note; more than one opening balance, or one that is not the earliest cash entry; a ticker with bad characters; a held ticker that cannot be verified (D6); a split number that is not a whole number above 0, or two equal numbers.
- **Sells and picks:** missing or badly written picks; a pick of an id that is not found, not a buy, or in another portfolio; a lot bought after the sell date or entered after the sell on the same date; a pick of more than the lot holds; picks that do not add up to the sell quantity; picks on a row that is not a sell.
- **Reversals:** the target is not found, is the opening balance or another reversal, is listed after the reversal, or is already reversed; the reversal is dated earlier than the original.
- **State rows:** a hidden holding not found or with shares left; an override for a holding not found, with both values empty or with a value not on its list; a duplicate list value, remembered text, hidden row or override; an unknown list name; a remembered text pointing to a value not on the list; an unknown setting key or value, a key twice, or a tab order that is not each tab once.
- **Rules (4.1 to 4.6):** cash would go negative on a date (with the date and the shortfall); shares would go negative; a sell of more than is held.
- **Dependent:** a row that depends on a failed row says "depends on line N" and is not checked further.
- The report starts with "N errors in M lines". It stays on screen, and only the Copy report button (C16) copies it.

**Preparing the file** (CSV cannot hold comments, so this note is here)
- In Excel, format date columns as `yyyy-mm-dd` and ticker columns as Text, then save as CSV UTF-8. Excel may rewrite dates, numbers and text like `2:1` **[UNVERIFIED, from memory]**; that is why splits use two columns.
- Open the file in Notepad to check it, with Word Wrap off and the status bar on, so line numbers match the report. Keep one record per line. Save as UTF-8; the file name and extension do not matter to the restore.
- The app's own backups are named `backup-YYYY-MM-DD.csv` (the date in the device's time zone, C15). Samples: `spec/example-backup.csv` (valid, with its expected results) and `spec/template-backup.csv` (header and `format` row only).
- Test the first real file with the restore report before relying on it.

**Known limits**
- Depends on a free third-party source (C1, unverified), may hit rate limits on a large file, and is slower.
- A restored lock setting can be weaker than the one on the new install.
- A bad file that loads cleanly can be undone only by restoring a backup (C3).

## 9. Backup, restore, security
- Data stored on the phone only, in app-private storage.
- Lock: biometric or PIN. Auto-lock choices: every time you leave the app (default), after 1 min idle, after 5 min idle, only when the phone locks or the app restarts.
- First launch: the app asks you to confirm your biometric or phone PIN before showing anything (section 6).
- **Privacy by default.** Screenshots and screen recording are blocked, and the recent-apps preview hides the content.
- **Screenshot window.** A setting turns on a window of a fixed 3 minutes (not adjustable), after a biometric or phone PIN check (asked each time it is turned on). The recent-apps preview stays hidden. The window ends after 3 minutes or when you leave the app. It never survives a restart and is not in the backup file. It is not selective: during it, any screen recorder or screen sharing can capture the app. The default auto-lock relocks the app when you leave it. This protects against casual exposure only.
- **[UNVERIFIED]** Android behaviour behind this, from memory: the secure-window flag blocks screenshots, recording and the preview together; Android 13 and newer have a separate switch for the preview only; older versions need the flag set again when the app goes to the background; using biometric and phone PIN in one prompt has restrictions before Android 11. The minimum Android version is decided in plan.md.
- Backup: export one file, in the format of section 8, to a location you choose. Unencrypted allowed. It is the only way data leaves the app.
- Restore: replaces all current data from one file, per section 8. There is no merge. It needs internet. It resets the last-backup state ("never backed up" until the next export).
- **Last backup.** More shows "Last backup: <date> <zone>" or "never backed up". The date is the export moment in the device's time zone, with that zone's abbreviation (C15). Only a completed export sets it. A cancelled or failed export does not.
- **Backup reminder.** An in-app banner on Overall (not an Android notification) when the last backup is older than 7 days (elapsed time), or when there is no backup and the app holds data. Dismissing it hides it only until the app is next opened: each return to the app after leaving it, or a restart, shows it again while a backup is still due. The dismissal is not saved. Known limit: the app knows only when it last exported, not whether the file is safe.
- Works offline with last known prices. Price refresh and restore need internet.

## 10. Out of scope
As in intent.md: liabilities, other asset classes, system notifications and alerts (the in-app backup reminder is in scope), tax reporting, manual price entry, per-holding dividends, separate fees, multi-currency, account syncing, Play Store release, CSV import, CSV export and merging data.
Planned for later versions: performance over time, per-holding dividends (a dividend entry that optionally names a ticker and portfolio), ETF look-through.

## 11. Acceptance criteria (first version)
1. Net worth and allocation show correctly from your real data (after restoring your prepared file).
2. Examples A to F behave as written, as automated tests.
3. A buy or sell can be entered in under 30 seconds.
4. Backup then restore on a clean install reproduces identical portfolios, transactions, cash entries, lots, cash balance and holdings. Net worth matches after the first successful price refresh (holdings show "no price" until then). After the restore, More shows "never backed up".
5. Offline use shows last prices marked with their age.
6. App locks per the chosen auto-lock setting.
7. Changing the price source in settings, while online, fetches all prices again and updates market values; tickers the new source cannot price keep the last price, marked stale. While offline, the change shows a warning and skips the sweep.
8. Restore cannot be started while the phone is offline. Online, every ticker still held at the end of the file is verified before anything is deleted, and one that fails blocks the restore. The check uses the price source selected before the restore; the file's source takes effect after loading.
9. Every screen that asks for a date shows the note that dates are US market (New York) dates next to the input. Every date that is only shown has the time zone suffix "ET" and no note, except the last-backup date, which shows the device's time zone abbreviation (C15).
10. In a portfolio's transaction history, the cash filter shows only the cash lines of that portfolio's buys and sells, never direct cash entries.
11. The realized gains view shows the gain per sale, per holding and as an overall total. For Example A, the sale shows a realized gain of $800.
12. Each holding shows its sector and geography. The source supplies them when it can, and you can override them by hand. A holding with neither shows "Unclassified". Allocation by sector/geography uses these values.
13. Every view and entry flow in section 6 can be reached by the navigation outline. A buy or sell can be started from Overall in two taps (Buy/Sell button, then Buy or Sell). The tab order can be changed in settings.
14. The sell lot picker lists the lots of the ticker in that portfolio, oldest first, each with purchase date, quantity remaining, cost per share and unrealized gain/loss. Lots that fail the lot rules for the entered sell date are greyed out with a reason and cannot be picked. For Example A, before the Mar 1 sale at a price of $160, the picker shows a gain of $600 for lot 1 and $100 for lot 2.
15. A manual buy of a never-verified ticker is blocked while the phone is offline. Online, a ticker that does not exist is rejected, and a valid one is saved. The check uses the selected price source.
16. A file with errors changes nothing. The report starts with "N errors in M lines" and lists every error with its line, its column when one applies, and the reason, covering every reason in section 8. A Copy report button copies that text; no other screen offers copy, share or export of data.
17. Restoring onto an app that holds data shows the count summary, offers a backup export first and requires a typed confirmation.
18. More shows "Last backup: <date> <zone>" or "never backed up". A completed export updates it, a cancelled or failed export does not. The Overall banner appears when the last backup is older than 7 days, or there is none and the app holds data. Dismissing it hides it until the app is next opened; the next return to the app shows it again while a backup is still due. Checked on the phone, because the abbreviation Android gives for some zones is unverified (C15). After a restore the banner shows, because the last-backup state is reset.
19. With no data, after the lock is confirmed, Overall shows the three steps. Restore is offered only in More. Once a portfolio or cash entry exists, the normal screens replace the steps.
20. By default, screenshots and recording are blocked and the recent-apps preview is hidden. The screenshot window needs a biometric or phone PIN check, lasts 3 minutes, ends early when you leave the app, is gone after a restart, and is not in the backup file. Checked by hand on the target phone, because the platform behaviour is unverified (section 9).
21. Reverse and re-enter saves the reversal and the new entry together or neither. A blocked pair saves nothing and says why. It is not offered on the opening balance, a reversal, a `trade` line, or an entry that already has a reversal. For a buy with a wrong quantity, the result is the corrected buy only.
22. Sector and geography: an override is picked from the list, and clearing it returns the source value or "Unclassified". A source text not on the list is added. Renaming a value updates the holdings and a refresh does not add the old text again. Deleting a value in use is blocked. Values and source texts that differ only in case or spaces count as the same.
23. The realized gains view filters by date range. For Example A, a range containing Mar 1 shows $800, and a range of Apr 1 to Apr 30 shows no sale.
24. With cash and no portfolio, Overall shows the normal screens with 100% cash and a Create a portfolio button. Buy says "Create a portfolio first" and returns to Buy after the portfolio is created. Sell says "Nothing to sell yet".
25. spec/example-backup.csv restores with the results listed beside it (cash balance, holdings, lots, realized gain). The same file saved as CSV UTF-8 from Excel and re-saved from Notepad (with or without a byte-order mark, LF or CRLF) restores the same way.
26. A portfolio name that matches an existing one (ignoring case and spaces) is refused on create and rename. An amount with more than 2 decimals is refused in the entry screens.

## 12. Proposed slices (input to plan.md)
1. Data model (including classification fields, lists and remembered texts), cash account, rules engine with tests (Examples A to F).
2. Buy/sell/split entry, lot picker, holdings, cost and gains, Reverse and re-enter.
3. Prices (including the price source setting and sweep), net worth, the Overall view and allocation.
4. Portfolio management and deletion rules, guided empty state (needs the entry flows of slice 2).
5. Lock (including screenshot protection and the window), backup and restore (format in section 8; the only way your real data gets in), last-backup line and reminder banner.
6. Realized gains view with date filter, sector/geography override, lists and source-text rules (kept in v1 per D2; built last). Backup and restore carry these fields from slice 5, using the data model of slice 1, so the restore file is not blocked by this slice.

## 13. Open design points
- D1 SUPERSEDED by v52: no CSV import. The backup-file format is D5.
- D2 RESOLVED: realized gains and sector/geography stay in v1.
- D3 SUPERSEDED by v52: no merge.
- D4 RESOLVED: a reversal cannot itself be reversed (4.6).
- D5 RESOLVED: the backup-file format is in section 8 (flat CSV, `row_type` column, 19 columns, format version 1). Sample and template files: `spec/example-backup.csv`, `spec/template-backup.csv`. The old `spec/example-import.csv` is no longer referenced.
- D6 RESOLVED: ticker verification uses the price source selected at that moment (4.3, sections 7 and 8). The restore uses the source selected before it, not the file's.
- D7 RESOLVED: a source text is matched ignoring case and spaces (4.9).
- D8 RESOLVED: with cash but no portfolio Overall keeps the normal screens; the Buy and Sell messages are in section 6.
- Blocked-deletion message: RESOLVED. Wording in 4.7 and Example E approved.
- Historical prices: deferred past v1 (section 7).

## 14. Components and responsibilities (technology-neutral)
Added as section 14 so existing section numbers stay stable. Language, framework, database and price source are plan.md decisions. This section only says what each part must do and what it may depend on.

| # | Component | Responsibility | Spec refs | Slices |
|---|-----------|----------------|-----------|--------|
| K1 | **Rules engine** | Holds the ordering and replay checks (4.1), cash rules (4.2), lots and gains (4.4), splits (4.5), reversals (4.6) and the portfolio deletion check (4.7). Also validates a save of several entries as a unit (Reverse and re-enter, restore). Pure logic: no screens, no network. Answers "is this save allowed, and if not, which date and amount fail?" | 3, 4, 5 | 1, 2, 4 |
| K2 | **Storage** | Keeps all records in app-private storage on the phone. A save and its linked cash line, and a Reverse and re-enter pair, are written together or not at all. A restore (delete and load) is also all or nothing. Loads data at start. | 3, 9 | 1 |
| K3 | **Valuation and reporting** | Net worth, unrealized and realized gains (also for a date range), allocation (combined and per portfolio, sector/geography by effective value from K10). Reads records and prices; never writes them. | 4.8, 6 | 3, 6 |
| K4 | **Price service** | Fetches latest prices through a replaceable source, verifies tickers, marks prices stale, supplies source sector/geography texts when the source has them and passes them to K10. Only updates instrument data, never transactions. Owns the selected-source setting and runs the sweep when it changes. K9 asks K4 to switch the source. Ticker verification uses the selected source. | 7, C1, C8, C14 | 3, 6 |
| K5 | **Entry flows** | Buy, sell (with lot picker), split, cash entry, reversal, Reverse and re-enter and portfolio management. Shows the review screen, enforces the ticker check of 4.3 using K4's verified flag, then asks K1 to validate and save (the pair as a unit). | 4.3 to 4.7, 6 | 2, 4 |
| K6 | **Backup and restore** | Exports one file and records the moment of each completed export. Supplies the reminder state (older than 7 days, or none and the app holds data). Restore refuses to start offline, validates the whole file by replaying it through K1 and collecting every error, verifies held tickers through K4, shows the summary, offers an export and requires typed confirmation when data exists, then deletes and loads in one action. Shows the error report with its Copy report button (C16) and resets the last-backup state after a restore. | 8, 9, C3, C7, C8 | 5 |
| K7 | (Withdrawn: merged into K6; ID kept stable.) | | | |
| K8 | **App lock** | Biometric or PIN, the auto-lock policy, first-launch lock confirmation, screenshot/recording/preview protection and the 3-minute window. Guards every screen. | 9 | 5 |
| K9 | **Screens** | The nine views, the guided empty state, the banner and the settings screens. Call K3 to read and K5, K4, K6 or K10 to change data; never write directly. Shows the date-field note on every date input and the time zone suffix on every shown date (section 6). | 6 | 2, 3, 6 |
| K10 | **Classification** | Owns the two lists, the remembered source texts and the manual overrides. Resolves a holding's effective sector and geography. Enforces add, rename (updates holdings, remembers the source text) and delete (blocked while in use). Adds an unlisted source text when K4 passes it. Pure logic: no screens, no network. | 3, 4.9, 6, C14 | 1, 6 |

**Dependency rules**
1. Every write to transactions or cash entries goes through K1: entry flows, restore and portfolio deletion. No component bypasses it.
2. K3 and K9 only read data. K4 only writes prices and instrument data. K10 is the only writer of the lists, remembered source texts and overrides.
3. K1 has no knowledge of screens, network or file formats, so Examples A to F can be tested on K1 alone.
4. Checks that need the network, such as ticker verification, are enforced by K5 (manual entry) and K6 (restore) using K4. They are not part of K1.
