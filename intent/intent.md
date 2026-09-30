# intent.md (v44, awaiting final confirmation)

## Problem
I have no single place to see what I own across bank/cash accounts and investments, how it is split, or how it has changed over time.

## Outcome
A personal financial asset tracker on my Android phone that shows:
- Net-worth snapshot (current total value of all assets)
- Performance over time (how total and per-holding value changed)
- Allocation breakdown (share of net worth by asset type, portfolio, holding, and sector/geography)

## Users
One user: me. No sharing, no multi-user accounts.

## Scope
**In scope**
- One shared cash account (replaces separate bank accounts), tracked through cash transactions
- Multiple stock/ETF portfolios, each with its own holdings and transactions
- Stocks and ETFs on US markets (NYSE, NASDAQ), quoted in USD (no mutual funds)
- Investments recorded as individual events: buys, sells and stock splits (dates, quantities)
- One-time CSV import of my existing investment transactions and direct cash transactions
- Automatic price updates for investments; manual entry for transactions
- Single currency: USD

**Out of scope (for now)**
- Liabilities (loans, mortgages, credit cards)
- Crypto, property, vehicles and other assets
- Notifications, alerts and reminders
- Tax reporting (tax lots, wash sales, tax owed): realized gains are informational only
- Per-holding dividend tracking and separate fee tracking (dividends are recorded only as cash entries; fees are folded into trade totals)
- Multi-currency reporting and currency conversion (only base-currency instruments are held)
- Bank/broker account syncing

## Constraints
- Platform: Android phone
- Data lives on the phone
- Protected by a biometric or PIN lock
- Backup by exporting to a file in storage I control, and restoring from that file
- Works offline, showing last known prices; price refresh needs internet
- Timeline: ASAP
- Installation: directly on my own phone (sideloaded), for me only; no Play Store release
- Price freshness: live or near-real-time (see open question 1)
- Exported backup file may be plain (unencrypted); I store it somewhere safe
- Budget: free tools and free price data only

## Views needed (first version)
What I need to see; layout and navigation are decided in Design.
- Home (first thing after unlocking): net worth plus an allocation summary, and net worth plus each portfolio with its gain/loss.
- Net worth overview, combined and per portfolio.
- Portfolios and their holdings.
- Allocation breakdowns (asset type, portfolio, holding, sector/geography).
- Realized gains (per sale, per holding, overall total).
- Holding detail: lots, cost, gain/loss, and that holding's transactions.
- Cash account: balance and cash entries, including a cash line for each buy and sell, filterable by date range. Deleting a portfolio removes the cash lines its trades produced.
- Transaction history, viewable only within a portfolio (no cross-portfolio history), filterable by ticker, date range and type (buy, sell, split, cash). Cash entries are viewed in the cash account.
- List of closed or hidden holdings.

## Decisions made

### Scope, accounts and first version
- Grouping: a single shared cash account plus multiple stock/ETF portfolios.
- The shared cash account fully replaces per-bank balances.
- The same ticker can be held in several portfolios. Moving holdings between portfolios is not a requirement.
- Base currency is USD.
- Stocks and ETFs are held on US markets (NYSE, NASDAQ) and quoted in USD, so no FX conversion is needed.
- The first version is done when net worth and allocation show from my real data, updates are quick, and backup and restore work end to end.
- First version delivers the net-worth snapshot and allocation breakdown. Performance over time comes after, but transactions are recorded from the start so history is not lost.
- Investments are tracked as individual transactions, which enables performance over time.
- Per-holding cost and unrealized gain/loss (lot-based) are part of the first version.
- Sector and geography allocation is part of the first version (automatic from the data source when available, manual otherwise).
- Adding a transaction should take under 30 seconds.

### Portfolios
- Portfolios can be created, renamed, and deleted even when they have transactions.
- Reasons to delete a portfolio: created by mistake or no longer needed, tidying up, or the real-world account was closed.
- Deleting a portfolio with transactions deletes its transactions and undoes their cash effect; it is blocked if the cash rules would break. Its realized gains disappear with it. Consequence: this is an exception to permanent transactions; the only way back is restoring a backup. (Supersedes the earlier "leave the cash balance as is" decision.)
- Deleting a portfolio needs a typed confirmation plus a summary of what will be removed (no forced backup).
- If undoing the cash effect is blocked, the deletion stays blocked. I fix the cash first (for example with a backdated deposit adjustment that carries a note), then delete.
- The last remaining portfolio can be deleted; I can delete every portfolio.
- After a portfolio is deleted, past net worth history is recalculated as if the portfolio never existed.

### Cash account
- Buys automatically take cash out of the shared cash account and sells put cash in.
- Cash moves on the trade date, immediately, for both buys and sells (no settlement delay).
- The cash account list shows the cash moved by each buy and sell as a cash line.
- The cash account's entry list can be filtered by date range.
- Dividends and interest received are cash-only entries, not linked to any holding.
- Each cash entry has a type label (deposit, withdrawal, interest/dividend, adjustment, opening balance), an amount, a date and an optional note.
- There is only one opening balance entry. It must be the earliest cash entry (nothing dated before it). It is not reversed or replaced; later fixes use adjustments.
- The note is required on adjustment entries and optional on all other cash entries.
- A cash balance that differs from the real balance is corrected with an adjustment deposit or withdrawal carrying a note.
- Direct cash transactions are: deposits, withdrawals, and interest or dividends received.
- A cash withdrawal larger than the cash balance is blocked, same as buys.
- The cash balance starts from a dated opening balance entry.

### Buys
- A buy that costs more than the available cash balance is blocked.
- Buys: I type a ticker and the app checks that it exists (requires internet).
- A buy for a ticker the app has not verified before is blocked until I'm online.
- A buy carries a date only. Same-day entries are ordered by when I entered them.
- For a buy dated before a stock split, I enter the quantity as originally traded and the app applies later splits automatically.
- A buy shows a quick review screen before saving.
- Buys are not sanity-checked against the market price; I trust what I type.

### Sells and holdings after selling
- Selling more than a portfolio holds is blocked.
- A sell shows the same quick review screen as a buy before saving.
- Sells are not sanity-checked against the market price; I trust what I type.
- A total loss (worthless or delisted stock) is recorded as a sell with zero proceeds. Zero proceeds are allowed on sells only, and the realized loss equals the cost of the lots consumed.
- A sell has a "sell all" shortcut that fills the exact remaining quantity.
- Only fully sold holdings can be hidden. I can unhide them, and a new buy of a hidden ticker shows it again.
- A holding stays visible after all its shares are sold, until I hide it.

### Trade entry (buys and sells)
- Trading fees are included in the total cost of a trade; there is no separate fee field.
- When entering a buy or sell, I type the quantity and the total amount (fees included). Price per share is derived, and cash moves by the total amount.
- Share quantities may have up to 8 decimal places.
- Forward and reverse stock splits are supported, and fractional share quantities are allowed.
- I enter every stock split myself (no automatic split data). Consequence: a forgotten split leaves holdings wrong until I enter it.

### Lots
- Tapping a lot prefills its full remaining quantity; I edit the quantity only for a partial lot.
- A sell can only use lots in its own portfolio, never lots from another portfolio.
- A sell may consume only lots bought on or before the sell's date. For same-date lots, the buy must have been entered before the sell that uses it (consistent with entry-order checking).
- When picking lots, I type a quantity for each lot I pick.
- When I pick a lot, it shows purchase date, quantity remaining, cost per share, and the lot's unrealized gain/loss.
- My broker uses specific-lot identification, so the app has one lot method for the whole app: I pick the lots on every sell. There is no default method and no per-portfolio setting.
- Lot picking is wanted so the app's gains match how my broker reports them. Lots are used everywhere: realized and unrealized gain both come from lots.
- Lots are tracked per portfolio; the combined view adds portfolios up.
- The under-30-seconds goal applies to sells too: lots are shown ready to tap, and there is a "sell all" shortcut.

### Dates, ordering and corrections
- Blocking rules for same-date entries are checked strictly in the order I entered them.
- A future-dated transaction affects net worth only from its date onward.
- For a backdated transaction, cash and share rules are checked at that date and every later date.
- Manually entered transactions may be backdated or future-dated.
- Reversal transactions follow the same blocking rules (enough cash, enough shares). Consequence: a mistake buried under later dependent transactions can only be undone by reversing the later ones first.
- Mistakes are corrected by adding an offsetting (reversal) transaction.
- Saved transactions are permanent (no edit or delete).

### Gains, cost, tax and performance
- Realized gains are shown per sale, as a total per holding, and as an overall total.
- Realized gains from sells are shown as well, computed from the lots each sell consumes.
- Each holding shows cost paid and unrealized gain/loss, computed from tax lots (supersedes the earlier average-cost decision).
- Tax reporting is out of scope; realized gains are informational only and may differ from tax figures (for example broker wash-sale adjustments are not modelled).
- Performance (later version) means total value change over time. Note: this figure moves with deposits and withdrawals, so it does not isolate investment returns.
- For the later performance-over-time feature, past values are rebuilt from my transactions and historical prices, back to my first transaction (no stored snapshots).

### Views and allocation
- Allocation is shown by asset type (cash, stocks, ETFs), by portfolio, by individual holding, and by sector or geography.
- Net worth and allocation are viewable both combined and per portfolio.

### Prices and market data
- Prices: free data only. Near-real-time is best effort, and the app shows how old each price is.
- If a price cannot be fetched, the last known price is shown and marked as stale.
- Sending ticker symbols to an outside price source is acceptable.
- The app works offline with last known prices.
- Sector and geography come automatically from the data source when available, and are entered manually otherwise.

### CSV import
- My existing CSV covers one portfolio, so a single import loads its investment rows and all its cash rows.
- If rows look like duplicates of existing transactions, the import warns me and lets me decide.
- Each CSV import targets one portfolio that I choose when importing.
- My existing data is a single CSV containing both investment and cash transactions.
- CSV import shows a preview and asks for confirmation before saving.
- CSV import loads valid rows and lists rejected rows.
- In the CSV import, a lot is matched by date and cost per share when the buy date alone is ambiguous; rows that still cannot be matched are rejected and listed in the preview.
- My CSV identifies the lots each sell consumed, by buy date or lot id.

### Backup, restore and security
- Restoring from a backup asks me each time whether to replace or merge.
- Backup means export plus restore from the exported file.
- No data export beyond the backup file.
- The auto-lock setting offers four choices: lock every time I leave the app, after 1 minute idle, after 5 minutes idle, or only when the phone locks or the app restarts. The default is the strictest (lock every time I leave the app).
- Auto-lock timing is a setting I choose myself.

## Open questions
None remaining in Plan.

## Deferred to Design (carried forward, not blocking)
- CSV format: columns, date and number formats, and how rows are recognized as buys, sells, splits and cash movements. To be settled with a sample file when the spec is written.
- Opening balance vs CSV: the opening balance must be the earliest cash entry, so the CSV history and the opening balance must line up. Whether the CSV contains an opening-balance row is unknown; check against the sample file.
- Blocked deletion: the app should tell me why deletion is blocked and how much cash (and from which date) is missing, so I can fix it. Wording and presentation are Design.
- Historical prices: rebuilding past values needs historical prices back to my first transaction from a free source. Availability, limits and split adjustment are unverified; check in Design. Because I enter splits myself, price history that is already split-adjusted must be reconciled with my as-traded lots.
