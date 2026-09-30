# intent.md (v28, awaiting final confirmation)

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

## Decisions made
- Per-holding cost and unrealized gain/loss (average cost) are part of the first version.
- Average cost is computed per portfolio; the combined view adds portfolios up.
- Realized gains from sells are shown as well, computed with the same average cost.
- Blocking rules for same-date entries are checked strictly in the order I entered them.
- Buys are not sanity-checked against the market price; I trust what I type.
- Each holding shows cost paid and unrealized gain/loss using average cost.
- A buy for a ticker the app has not verified before is blocked until I'm online.
- I enter every stock split myself (no automatic split data). Consequence: a forgotten split leaves holdings wrong until I enter it.
- A buy shows a quick review screen before saving.
- Buys: I type a ticker and the app checks that it exists (requires internet).
- A buy carries a date only. Same-day entries are ordered by when I entered them.
- For a buy dated before a stock split, I enter the quantity as originally traded and the app applies later splits automatically.
- Share quantities may have up to 8 decimal places.
- Dividends and interest received are cash-only entries, not linked to any holding.
- Trading fees are included in the total cost of a trade; there is no separate fee field.
- When entering a buy or sell, I type the quantity and the total amount (fees included). Price per share is derived, and cash moves by the total amount.
- Each cash entry has a type label (deposit, withdrawal, interest/dividend, adjustment, opening balance), an amount, a date and an optional note.
- There is only one opening balance entry. It must be the earliest cash entry (nothing dated before it). It is not reversed or replaced; later fixes use adjustments.
- The note is required on adjustment entries and optional on all other cash entries.
- A cash balance that differs from the real balance is corrected with an adjustment deposit or withdrawal carrying a note.
- Direct cash transactions are: deposits, withdrawals, and interest or dividends received.
- A cash withdrawal larger than the cash balance is blocked, same as buys.
- The cash balance starts from a dated opening balance entry.
- My existing CSV covers one portfolio, so a single import loads its investment rows and all its cash rows.
- If rows look like duplicates of existing transactions, the import warns me and lets me decide.
- The auto-lock setting offers four choices: lock every time I leave the app, after 1 minute idle, after 5 minutes idle, or only when the phone locks or the app restarts. The default is the strictest (lock every time I leave the app).
- Prices: free data only. Near-real-time is best effort, and the app shows how old each price is.
- Sector and geography allocation is part of the first version (automatic from the data source when available, manual otherwise).
- Each CSV import targets one portfolio that I choose when importing.
- My existing data is a single CSV containing both investment and cash transactions.
- Adding a transaction should take under 30 seconds.
- Performance (later version) means total value change over time. Note: this figure moves with deposits and withdrawals, so it does not isolate investment returns.
- Sector and geography come automatically from the data source when available, and are entered manually otherwise.
- A future-dated transaction affects net worth only from its date onward.
- For a backdated transaction, cash and share rules are checked at that date and every later date.
- Allocation is shown by asset type (cash, stocks, ETFs), by portfolio, by individual holding, and by sector or geography.
- Auto-lock timing is a setting I choose myself.
- Manually entered transactions may be backdated or future-dated.
- Reversal transactions follow the same blocking rules (enough cash, enough shares). Consequence: a mistake buried under later dependent transactions can only be undone by reversing the later ones first.
- Forward and reverse stock splits are supported, and fractional share quantities are allowed.
- Restoring from a backup asks me each time whether to replace or merge.
- Mistakes are corrected by adding an offsetting (reversal) transaction.
- CSV import shows a preview and asks for confirmation before saving.
- If a price cannot be fetched, the last known price is shown and marked as stale.
- Selling more than a portfolio holds is blocked.
- CSV import loads valid rows and lists rejected rows.
- Saved transactions are permanent (no edit or delete).
- A buy that costs more than the available cash balance is blocked.
- The same ticker can be held in several portfolios. Moving holdings between portfolios is not a requirement.
- Buys automatically take cash out of the shared cash account and sells put cash in.
- The shared cash account fully replaces per-bank balances.
- Net worth and allocation are viewable both combined and per portfolio.
- Grouping: a single shared cash account plus multiple stock/ETF portfolios.
- The first version is done when net worth and allocation show from my real data, updates are quick, and backup and restore work end to end.
- Base currency is USD.
- Stocks and ETFs are held on US markets (NYSE, NASDAQ) and quoted in USD, so no FX conversion is needed.
- First version delivers the net-worth snapshot and allocation breakdown. Performance over time comes after, but transactions are recorded from the start so history is not lost.
- Investments are tracked as individual transactions, which enables performance over time.
- The app works offline with last known prices.
- Backup means export plus restore from the exported file.
- Sending ticker symbols to an outside price source is acceptable.

## Open questions
None remaining in Plan.

## Deferred to Design (carried forward, not blocking)
- CSV format: columns, date and number formats, and how rows are recognized as buys, sells, splits and cash movements. To be settled with a sample file when the spec is written.
- Opening balance vs CSV: the opening balance must be the earliest cash entry, so the CSV history and the opening balance must line up. Whether the CSV contains an opening-balance row is unknown; check against the sample file.
