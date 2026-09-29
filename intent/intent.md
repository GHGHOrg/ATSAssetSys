# intent.md (v8, awaiting final confirmation)

## Problem
I have no single place to see what I own across bank/cash accounts and investments, how it is split, or how it has changed over time.

## Outcome
A personal financial asset tracker on my Android phone that shows:
- Net-worth snapshot (current total value of all assets)
- Performance over time (how total and per-holding value changed)
- Allocation breakdown (share of net worth by asset type and holding)

## Users
One user: me. No sharing, no multi-user accounts.

## Scope
**In scope**
- One shared cash account (replaces separate bank accounts), tracked through cash transactions
- Multiple stock/ETF portfolios, each with its own holdings and transactions
- Stocks, ETFs and funds on US markets (NYSE, NASDAQ), quoted in USD
- Investments recorded as individual events: buys, sells and stock splits (dates, quantities)
- One-time CSV import of my existing investment transactions and direct cash transactions
- Automatic price updates for investments; manual entry for transactions
- Single currency: USD

**Out of scope (for now)**
- Liabilities (loans, mortgages, credit cards)
- Crypto, property, vehicles and other assets
- Notifications, alerts and reminders
- Dividends and fees (so performance figures will exclude them)
- Multi-currency reporting and currency conversion (only base-currency instruments are held)
- Bank/broker account syncing

## Constraints
- Platform: Android phone
- Data lives on the phone
- Protected by a biometric or PIN lock
- Backup by exporting to a file in storage I control, and restoring from that file
- Works offline, showing last known prices; price refresh needs internet
- Timeline: ASAP
- Budget: free tools and free price data only

## Decisions made
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
None remaining.
